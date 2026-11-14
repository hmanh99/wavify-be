package com.example.wavifybe.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Getter
public enum ErrorCode {
    UNCATEGORIZED_EXCEPTION(1000, "Uncategorized error", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_KEY(400, "Uncategorized error", HttpStatus.BAD_REQUEST),
    USER_EXISTED(409, "User existed", HttpStatus.CONFLICT),
    INVALID_USERNAME(400, "Username must be at least {min} characters", HttpStatus.BAD_REQUEST),
    INVALID_PASSWORD(401, "Password must be at least {min} characters", HttpStatus.BAD_REQUEST),
    USER_NOT_EXISTED(404, "User not existed", HttpStatus.NOT_FOUND),
    UNAUTHENTICATED(401, "Unauthenticated", HttpStatus.UNAUTHORIZED),
    UNAUTHORIZED(403, "You do not have permission", HttpStatus.FORBIDDEN),
    INVALID_DOB(400, "Your age must be at least {min}", HttpStatus.BAD_REQUEST),
    SONG_NOT_EXISTED(404, "Song not existed", HttpStatus.NOT_FOUND),
    PLAYLIST_NOT_EXISTED(404, "Playlist not existed", HttpStatus.NOT_FOUND),
    SONG_ALREADY_IN_PLAYLIST(409, "Song already existed in playlist", HttpStatus.CONFLICT),
    SONG_NOT_IN_PLAYLIST(404, "Song not existed in playlist", HttpStatus.NOT_FOUND),
    INVALID_PLAYLIST_REORDER(400, "Invalid song list for reordering playlist", HttpStatus.BAD_REQUEST),
    INVALID_FILE(400, "Invalid or empty file", HttpStatus.BAD_REQUEST),
    FILE_UPLOAD_FAILED(500, "Upload file to Cloudinary failed", HttpStatus.INTERNAL_SERVER_ERROR),
    FILE_DELETE_FAILED(500, "Delete file from Cloudinary failed", HttpStatus.INTERNAL_SERVER_ERROR),
    ;

    ErrorCode(int code, String message, HttpStatusCode statusCode) {
        this.code = code;
        this.message = message;
        this.statusCode = statusCode;
    }

    private final int code;
    private final String message;
    private final HttpStatusCode statusCode;
}
