package com.streaming.features.video;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;

@Service
public class PresignService {

    @Value("${APP_URL:http://localhost:3000}")
    private String appUrl;

    @Value("${PRESIGN_SECRET:mysecret}")
    private String presignSecret;

    private final VideoService videoService;

    public PresignService(VideoService videoService) {
        this.videoService = videoService;
    }

    public Map<String, Object> generateSignedUrl(String id) {
        String streamId = videoService.getPublicStreamId(id);
        long expiresAt = Instant.now().getEpochSecond() + 60;
        String token = sign(streamId + ":" + expiresAt);

        return Map.of(
                "url", String.format("%s/video/stream/%s?token=%s&exp=%d", appUrl, streamId, token, expiresAt),
                "expiresAt", expiresAt,
                "streamId", streamId
        );
    }

    public void validateSignature(String id, long exp, String token) {
        long now = Instant.now().getEpochSecond();
        if (now > exp) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Presigned URL expired");
        }

        String expected = sign(id + ":" + exp);

        if (!java.security.MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), token.getBytes(StandardCharsets.UTF_8))) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token");
        }
    }

    private String sign(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(presignSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));

            // Convert to hex
            StringBuilder hexString = new StringBuilder(2 * hash.length);
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to calculate HMAC", e);
        }
    }
}
