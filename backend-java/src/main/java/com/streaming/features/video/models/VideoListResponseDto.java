package com.streaming.features.video.models;

import java.util.List;

public class VideoListResponseDto {
    private List<VideoFileDto> files;

    public VideoListResponseDto(List<VideoFileDto> files) {
        this.files = files;
    }

    public List<VideoFileDto> getFiles() { return files; }
    public void setFiles(List<VideoFileDto> files) { this.files = files; }
}
