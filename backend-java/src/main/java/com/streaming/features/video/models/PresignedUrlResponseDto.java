package com.streaming.features.video.models;

public class PresignedUrlResponseDto {
    private String url;
    private long expiresAt;
    private String streamId;

    public PresignedUrlResponseDto(String url, long expiresAt, String streamId) {
        this.url = url;
        this.expiresAt = expiresAt;
        this.streamId = streamId;
    }

    // Getters
    public String getUrl() { return url; }
    public long getExpiresAt() { return expiresAt; }
    public String getStreamId() { return streamId; }
}
