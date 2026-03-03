package com.streaming.features.video;

import com.streaming.features.video.models.VideoFileDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@Service
public class VideoService {

    private static final Logger log = LoggerFactory.getLogger(VideoService.class);
    private final Path videoDir = Paths.get("..", "videos").toAbsolutePath().normalize();
    private final Path convertedDir = videoDir.resolve("converted");

    private static class VideoConfig {
        String mime;
        boolean requiresTranscode;

        VideoConfig(String mime, boolean requiresTranscode) {
            this.mime = mime;
            this.requiresTranscode = requiresTranscode;
        }
    }

    private final Map<String, VideoConfig> supportedTypes = Map.of(
            ".mp4", new VideoConfig("video/mp4", false),
            ".mov", new VideoConfig("video/quicktime", false),
            ".flv", new VideoConfig("video/mp4", true)
    );

    public List<VideoFileDto> listAvailableVideos() {
        File dir = videoDir.toFile();
        if (!dir.exists() || !dir.isDirectory()) {
            return Collections.emptyList();
        }

        List<VideoFileDto> files = new ArrayList<>();
        File[] listFiles = dir.listFiles(File::isFile);
        if (listFiles == null) return files;

        for (File file : listFiles) {
            String name = file.getName();
            String ext = getExtension(name).toLowerCase();
            
            if (supportedTypes.containsKey(ext)) {
                VideoConfig config = supportedTypes.get(ext);
                String publicId = config.requiresTranscode ? getConvertedPublicId(name) : name;
                String label = config.requiresTranscode ? name + " (auto-converted)" : name;
                
                files.add(new VideoFileDto(name, label, config.mime, publicId));
            }
        }
        return files;
    }

    public String getPublicStreamId(String id) {
        return resolveVideoAsset(id).publicId;
    }

    private static class AssetResult {
        Path path;
        String mime;
        String publicId;

        AssetResult(Path path, String mime, String publicId) {
            this.path = path;
            this.mime = mime;
            this.publicId = publicId;
        }
    }

    private AssetResult resolveVideoAsset(String id) {
        List<AssetCandidate> lookupOrder = buildLookupList(id);
        for (AssetCandidate candidate : lookupOrder) {
            if (Files.exists(candidate.path)) {
                return prepareAsset(candidate.path, candidate.config, candidate.candidateId);
            }
        }

        if (!"sample".equals(id)) {
            List<AssetCandidate> fallbackOrder = buildLookupList("sample");
            for (AssetCandidate fallback : fallbackOrder) {
                if (Files.exists(fallback.path)) {
                    return prepareAsset(fallback.path, fallback.config, fallback.candidateId);
                }
            }
        }

        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Video asset missing. Provide videos/<name>.mp4|.mov|.flv or place sample.*");
    }

    private static class AssetCandidate {
        Path path;
        VideoConfig config;
        String candidateId;

        AssetCandidate(Path path, VideoConfig config, String candidateId) {
            this.path = path;
            this.config = config;
            this.candidateId = candidateId;
        }
    }

    private List<AssetCandidate> buildLookupList(String id) {
        List<AssetCandidate> attempts = new ArrayList<>();
        String providedExt = getExtension(id);

        if (!providedExt.isEmpty()) {
            String normalizedExt = "." + providedExt.toLowerCase();
            VideoConfig config = supportedTypes.get(normalizedExt);
            if (config != null) {
                attempts.add(new AssetCandidate(videoDir.resolve(id), config, id));
            }
            return attempts;
        }

        for (Map.Entry<String, VideoConfig> entry : supportedTypes.entrySet()) {
            String candidateId = id + entry.getKey();
            attempts.add(new AssetCandidate(videoDir.resolve(candidateId), entry.getValue(), candidateId));
        }

        return attempts;
    }

    private AssetResult prepareAsset(Path sourcePath, VideoConfig config, String publicId) {
        if (config.requiresTranscode) {
            String convertedPublicId = getConvertedPublicId(publicId);
            return new AssetResult(ensureConvertedMp4(sourcePath), "video/mp4", convertedPublicId);
        }
        return new AssetResult(sourcePath, config.mime, publicId);
    }

    private Path ensureConvertedMp4(Path sourcePath) {
        if (!Files.exists(convertedDir)) {
            try {
                Files.createDirectories(convertedDir);
            } catch (IOException e) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to create converted directory");
            }
        }

        String fileName = sourcePath.getFileName().toString();
        String nameWithoutExt = fileName.substring(0, fileName.lastIndexOf('.'));
        Path outputPath = convertedDir.resolve(nameWithoutExt + ".mp4");

        boolean needsConversion = !Files.exists(outputPath);
        if (!needsConversion) {
            try {
                long sourceTime = Files.getLastModifiedTime(sourcePath).toMillis();
                long outputTime = Files.getLastModifiedTime(outputPath).toMillis();
                if (outputTime < sourceTime) {
                    needsConversion = true;
                }
            } catch (IOException e) {
                needsConversion = true;
            }
        }

        if (needsConversion) {
            String ffmpegCmd = resolveFfmpegBinary();
            ProcessBuilder pb = new ProcessBuilder(
                    ffmpegCmd, "-y", "-i", sourcePath.toString(), "-c:v", "libx264", "-c:a", "aac", outputPath.toString()
            );
            pb.redirectErrorStream(true);
            try {
                Process p = pb.start();
                int exitCode = p.waitFor();
                if (exitCode != 0) {
                    throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to convert FLV. ffmpeg exit code: " + exitCode);
                }
            } catch (Exception e) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "FFmpeg processing failed", e);
            }
        }

        return outputPath;
    }

    private String resolveFfmpegBinary() {
        String envPath = System.getenv("FFMPEG_PATH");
        return (envPath != null && !envPath.isEmpty()) ? envPath : "ffmpeg";
    }

    private String getConvertedPublicId(String fileName) {
        String nameWithoutExt = fileName.substring(0, fileName.lastIndexOf('.'));
        return "converted/" + nameWithoutExt + ".mp4";
    }

    private String getExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        return (dotIndex == -1) ? "" : filename.substring(dotIndex + 1);
    }

    // Handles streaming via Spring's ResourceRegion for HTTP 206 Partial Content
    public ResponseEntity<ResourceRegion> streamVideo(String id, String rangeHeader) throws IOException {
        AssetResult asset = resolveVideoAsset(id);
        FileSystemResource videoResource = new FileSystemResource(asset.path);

        if (!videoResource.exists()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Video file not found");
        }

        ResourceRegion region = resourceRegion(videoResource, rangeHeader);
        return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                .contentType(MediaType.parseMediaType(asset.mime))
                .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                .body(region);
    }

    private ResourceRegion resourceRegion(Resource video, String httpHeaders) throws IOException {
        long contentLength = video.contentLength();
        if (httpHeaders != null && httpHeaders.startsWith("bytes=")) {
            String[] ranges = httpHeaders.substring("bytes=".length()).split("-");
            long start = Long.parseLong(ranges[0]);
            long end = ranges.length > 1 ? Long.parseLong(ranges[1]) : contentLength - 1;
            long rangeLength = Math.min(1024 * 1024, end - start + 1); // 1MB chunk max if not specified, though Spring handles it. We'll just return the requested range.
            return new ResourceRegion(video, start, end - start + 1);
        } else {
            long rangeLength = Math.min(1024 * 1024, contentLength);
            return new ResourceRegion(video, 0, rangeLength);
        }
    }
    
    // Fallback if full video is requested (No range header)
    public ResponseEntity<Resource> streamFullVideo(String id) {
        AssetResult asset = resolveVideoAsset(id);
        FileSystemResource videoResource = new FileSystemResource(asset.path);
        
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(asset.mime))
            .header(HttpHeaders.ACCEPT_RANGES, "bytes")
            .body(videoResource);
    }
}
