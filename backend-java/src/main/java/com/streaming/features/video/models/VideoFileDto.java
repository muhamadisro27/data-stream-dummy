package com.streaming.features.video.models;

public class VideoFileDto {
    private String id;
    private String label;
    private String mime;
    private String streamId;

    public VideoFileDto(String id, String label, String mime, String streamId) {
        this.id = id;
        this.label = label;
        this.mime = mime;
        this.streamId = streamId;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public String getMime() { return mime; }
    public void setMime(String mime) { this.mime = mime; }
    public String getStreamId() { return streamId; }
    public void setStreamId(String streamId) { this.streamId = streamId; }
}
