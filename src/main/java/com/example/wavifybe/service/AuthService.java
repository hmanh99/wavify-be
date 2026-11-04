package com.example.wavifybe.service;

import com.example.wavifybe.dto.request.AuthRequest;
import com.example.wavifybe.dto.request.RefreshRequest;
import com.example.wavifybe.dto.request.RegisterRequest;
import com.example.wavifybe.dto.response.AuthResponse;
import com.example.wavifybe.dto.response.UserResponse;
import com.example.wavifybe.entity.RefreshToken;
import com.example.wavifybe.entity.User;
import com.example.wavifybe.entity.UserRole;
import com.example.wavifybe.exception.AppException;
import com.example.wavifybe.exception.ErrorCode;
import com.example.wavifybe.mapper.UserMapper;
import com.example.wavifybe.repository.RefreshTokenRepository;
import com.example.wavifybe.repository.UserRepository;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HexFormat;
import java.util.StringJoiner;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthService {
    static String TOKEN_TYPE = "Bearer";

    UserRepository userRepository;
    PasswordEncoder passwordEncoder;
    UserMapper userMapper;
    RefreshTokenRepository refreshTokenRepository;

    @NonFinal
    @Value("${jwt.token.signer-key:${jwt.signerKey:}}")
    protected String SIGNER_KEY;

    @NonFinal
    @Value("${jwt.token.expire-length:900}")
    protected long VALID_DURATION;

    @NonFinal
    @Value("${jwt.refresh-token.expire-length:604800}")
    protected long REFRESHABLE_DURATION;



    public String generateAccessToken(User user) {
        JWSHeader header = new JWSHeader(JWSAlgorithm.HS512);

        JWTClaimsSet jwtClaimsSet = new JWTClaimsSet.Builder()
                .subject(user.getEmail())
                .issuer("wavify.com")
                .issueTime(new Date())
                .expirationTime(new Date(
                        Instant.now().plus(VALID_DURATION, ChronoUnit.SECONDS).toEpochMilli()))
                .jwtID(UUID.randomUUID().toString())
                .claim("userId", user.getId() != null ? user.getId().toString() : null)
                .claim("username", user.getUsername())
                .claim("scope", buildScope(user))
                .build();

        Payload payload = new Payload(jwtClaimsSet.toJSONObject());
        JWSObject jwsObject = new JWSObject(header, payload);

        try {
            jwsObject.sign(new MACSigner(getSignerKeyBytes()));
            return jwsObject.serialize();
        } catch (JOSEException e) {
            log.error("Cannot create access token", e);
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
        }
    }


    @Transactional
    public String generateRefreshToken(User user) {
        String rawToken = UUID.randomUUID().toString();
        String tokenHash = hashToken(rawToken);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setTokenHash(tokenHash);
        refreshToken.setExpiryDate(Instant.now().plus(REFRESHABLE_DURATION, ChronoUnit.SECONDS));
        refreshToken.setRevoked(false);

        refreshTokenRepository.save(refreshToken);
        return rawToken;
    }


    public RefreshToken verifyRefreshToken(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        String tokenHash = hashToken(rawRefreshToken);
        RefreshToken refreshToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new AppException(ErrorCode.UNAUTHENTICATED));

        if (refreshToken.isRevoked()) {
            log.warn("Attempted to use revoked refresh token for user: {}", refreshToken.getUser().getEmail());
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        if (refreshToken.isExpired()) {
            refreshToken.setRevoked(true);
            refreshTokenRepository.save(refreshToken);
            log.warn("Attempted to use expired refresh token for user: {}", refreshToken.getUser().getEmail());
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        return refreshToken;
    }


    // Hash token using SHA-256

    public String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedHash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(encodedHash);
        } catch (NoSuchAlgorithmException e) {
            log.error("SHA-256 algorithm not available", e);
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
        }
    }

    // Register
    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AppException(ErrorCode.USER_EXISTED);
        }

        User user = userMapper.toUser(request);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(UserRole.USER);

        return userMapper.toUserResponse(userRepository.save(user));
    }

    // Login and return pair of accessToken and refreshToken
    @Transactional
    public AuthResponse login(AuthRequest request) {
        User user = userRepository.findByEmail(request.getEmail()).orElseThrow(() -> new AppException(ErrorCode.UNAUTHENTICATED));

        String accessToken = generateAccessToken(user);
        String refreshToken = generateRefreshToken(user);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType(TOKEN_TYPE)
                .expiresIn(VALID_DURATION)
                .build();
    }

    // Refresh access token using valid refresh token (with token rotation)
    @Transactional
    public AuthResponse refresh(RefreshRequest request) {
        RefreshToken validRefreshToken = verifyRefreshToken(request.getRefreshToken());
        User user = validRefreshToken.getUser();

        // Revoke used refresh token
        validRefreshToken.setRevoked(true);
        refreshTokenRepository.save(validRefreshToken);

        // Generate new token pair
        String newAccessToken = generateAccessToken(user);
        String newRefreshToken = generateRefreshToken(user);

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .tokenType(TOKEN_TYPE)
                .expiresIn(VALID_DURATION)
                .build();
    }

    //logout and revoked refreshToken
    @Transactional
    public void logout(RefreshRequest request) {
        try {
            RefreshToken refreshToken = verifyRefreshToken(request.getRefreshToken());
            refreshToken.setRevoked(true);
            refreshTokenRepository.save(refreshToken);
        } catch (AppException e) {
            log.warn("Invalid refresh token during logout", e);
        }
    }

    private byte[] getSignerKeyBytes() {
        byte[] keyBytes = SIGNER_KEY.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 64) {
            byte[] padded = new byte[64];
            System.arraycopy(keyBytes, 0, padded, 0, keyBytes.length);
            return padded;
        }
        return keyBytes;
    }

    private String buildScope(User user) {
        StringJoiner stringJoiner = new StringJoiner(" ");
        if (user.getRole() != null) {
            stringJoiner.add("ROLE_" + user.getRole().name());
        }
        return stringJoiner.toString();
    }
}
