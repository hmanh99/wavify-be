package com.example.wavifybe.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.example.wavifybe.dto.request.SongCreationRequest;
import com.example.wavifybe.dto.request.SongUpdateRequest;
import com.example.wavifybe.dto.response.SongResponse;
import com.example.wavifybe.entity.Song;
import com.example.wavifybe.entity.Status;
import com.example.wavifybe.entity.User;
import com.example.wavifybe.entity.Visibility;
import com.example.wavifybe.exception.AppException;
import com.example.wavifybe.exception.ErrorCode;
import com.example.wavifybe.mapper.SongMapper;
import com.example.wavifybe.repository.FavoriteSongRepository;
import com.example.wavifybe.repository.SongRepository;
import com.example.wavifybe.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SongService {
    SongMapper songMapper;
    SongRepository songRepository;
    UserRepository userRepository;
    FavoriteSongRepository favoriteSongRepository;
    Cloudinary cloudinary;

    @Transactional(readOnly = true)
    public Page<SongResponse> getSongs(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return songRepository.findByVisibilityAndStatus(Visibility.PUBLIC, Status.READY, pageable)
                .map(songMapper::toSongResponse);
    }

    @Transactional(readOnly = true)
    public List<SongResponse> getSongsByUploader() {
        var context = SecurityContextHolder.getContext();
        var email = context.getAuthentication().getName();

        User uploader = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        return songRepository.findByUploaderId(uploader.getId()).stream()
                .map(songMapper::toSongResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public SongResponse getSong(UUID songId) {
        Song song = songRepository.findById(songId)
                .orElseThrow(() -> new AppException(ErrorCode.SONG_NOT_EXISTED));

        if (song.getVisibility() == Visibility.PRIVATE) {
            validatePermission(song);
        }

        return songMapper.toSongResponse(song);
    }

    @Transactional
    public SongResponse uploadSong(SongCreationRequest request, MultipartFile audioFile, MultipartFile coverFile) {
        if (audioFile == null || audioFile.isEmpty() || coverFile == null || coverFile.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_FILE);
        }

        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User uploader = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        Song song = songMapper.toSong(request);
        song.setUploader(uploader);
        if (song.getVisibility() == null) {
            song.setVisibility(Visibility.PUBLIC);
        }
        song.setStatus(Status.PROCESSING);

        try {
            Map<?, ?> audioResult = cloudinary.uploader().upload(
                    audioFile.getBytes(),
                    ObjectUtils.asMap(
                            "resource_type", "video",
                            "folder", "wavify/songs"
                    )
            );
            String fileUrl = (String) audioResult.get("secure_url");
            Number duration = (Number) audioResult.get("duration");
            int durationSec = duration != null ? (int) Math.round(duration.doubleValue()) : 0;


            Map<?, ?> coverResult = cloudinary.uploader().upload(
                    coverFile.getBytes(),
                    ObjectUtils.asMap(
                            "resource_type", "image",
                            "folder", "wavify/covers"
                    )
            );
            String coverUrl = (String) coverResult.get("secure_url");

            song.setFileUrl(fileUrl);
            song.setCoverUrl(coverUrl);
            song.setDurationSec(durationSec);
            song.setStatus(Status.READY);

            return songMapper.toSongResponse(songRepository.save(song));
        }catch (Exception e){
            log.info(e.toString());
            throw new AppException(ErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    @Transactional
    public SongResponse updateSong(UUID songId, SongUpdateRequest request) {
        return updateSong(songId, request, null, null);
    }

    @Transactional
    public SongResponse updateSong(UUID songId, SongUpdateRequest request, MultipartFile audioFile, MultipartFile coverFile) {
        Song song = songRepository.findById(songId)
                .orElseThrow(() -> new AppException(ErrorCode.SONG_NOT_EXISTED));

        validatePermission(song);

        if (request != null) {
            songMapper.updateSong(song, request);
        }

        try {
            if (audioFile != null && !audioFile.isEmpty()) {
                deleteFromCloudinary(song.getFileUrl(), "video");
                Map<?, ?> audioResult = cloudinary.uploader().upload(
                        audioFile.getBytes(),
                        ObjectUtils.asMap(
                                "resource_type", "video",
                                "folder", "wavify/songs"
                        )
                );
                song.setFileUrl((String) audioResult.get("secure_url"));
                Number duration = (Number) audioResult.get("duration");
                if (duration != null) {
                    song.setDurationSec((int) Math.round(duration.doubleValue()));
                }
            }

            if (coverFile != null && !coverFile.isEmpty()) {
                deleteFromCloudinary(song.getCoverUrl(), "image");
                Map<?, ?> coverResult = cloudinary.uploader().upload(
                        coverFile.getBytes(),
                        ObjectUtils.asMap(
                                "resource_type", "image",
                                "folder", "wavify/covers"
                        )
                );
                song.setCoverUrl((String) coverResult.get("secure_url"));
            }
        } catch (IOException e) {
            log.error("Failed to update song files on Cloudinary for song {}", songId, e);
            throw new AppException(ErrorCode.FILE_UPLOAD_FAILED);
        }

        return songMapper.toSongResponse(songRepository.save(song));
    }

    @Transactional
    public void deleteSong(UUID songId) {
        Song song = songRepository.findById(songId)
                .orElseThrow(() -> new AppException(ErrorCode.SONG_NOT_EXISTED));

        validatePermission(song);

        deleteFromCloudinary(song.getFileUrl(), "video");
        deleteFromCloudinary(song.getCoverUrl(), "image");

        favoriteSongRepository.deleteAllByIdSongId(songId);
        songRepository.delete(song);
    }

    private void validatePermission(Song song) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        String email = authentication.getName();
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!isAdmin && !song.getUploader().getEmail().equals(email)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
    }

    private void deleteFromCloudinary(String url, String resourceType) {
        String publicId = extractPublicId(url);
        if (publicId == null || publicId.isBlank()) {
            return;
        }
        try {
            cloudinary.uploader().destroy(
                    publicId,
                    ObjectUtils.asMap("resource_type", resourceType)
            );
        } catch (IOException e) {
            log.error("Failed to delete file from Cloudinary: {}", publicId, e);
            throw new AppException(ErrorCode.FILE_DELETE_FAILED);
        }
    }

    private String extractPublicId(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        int uploadIndex = url.indexOf("/upload/");
        if (uploadIndex == -1) {
            return null;
        }
        String path = url.substring(uploadIndex + "/upload/".length());
        path = path.replaceFirst("^v\\d+/", "");
        int dotIndex = path.lastIndexOf('.');
        if (dotIndex != -1) {
            path = path.substring(0, dotIndex);
        }
        return path;
    }
}
