package com.streaming.features.video;

import com.streaming.features.video.models.PresignedUrlResponseDto;
import com.streaming.features.video.models.VideoListResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;

@RestController
@RequestMapping("/video")
@Tag(name = "video", description = "Video catalog and streaming endpoints")
public class VideoController {

    private final VideoService videoService;
    private final PresignService presignService;

    public VideoController(VideoService videoService, PresignService presignService) {
        this.videoService = videoService;
        this.presignService = presignService;
    }

    @GetMapping("/presign/{id}")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Generate presigned streaming URL for a given asset identifier")
    public PresignedUrlResponseDto generatePresignedUrl(
            @Parameter(description = "Requested video id or filename") @PathVariable String id) {
        var result = presignService.generateSignedUrl(id);
        return new PresignedUrlResponseDto(
                (String) result.get("url"),
                (long) result.get("expiresAt"),
                (String) result.get("streamId")
        );
    }

    @GetMapping("/list")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "List available video files")
    public VideoListResponseDto listVideos() {
        return new VideoListResponseDto(videoService.listAvailableVideos());
    }

    @GetMapping("/stream/{*id}")
    @Operation(summary = "Stream video content using presigned URL",
            responses = {
                    @ApiResponse(responseCode = "206", description = "Byte-range video stream",
                            content = @Content(mediaType = "application/octet-stream", schema = @Schema(type = "string", format = "binary"))),
                    @ApiResponse(responseCode = "200", description = "Full video stream")
            })
    public ResponseEntity<Resource> streamVideo(
            @Parameter(description = "Resolved stream identifier (often includes path segments)") @PathVariable("id") String id,
            @Parameter(description = "HMAC token produced by presign endpoint", required = true) @RequestParam(value = "token", required = false) String token,
            @Parameter(description = "Expiration timestamp from presign endpoint", required = true) @RequestParam(value = "exp", required = false) String exp) {

        // Remove leading slash that Spring Boot adds for wildcard paths depending on version
        if (id != null && id.startsWith("/")) {
            id = id.substring(1);
        }

        if (token == null || exp == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing token or exp");
        }

        long numericExp;
        try {
            numericExp = Long.parseLong(exp);
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "exp must be a UNIX timestamp");
        }

        presignService.validateSignature(id, numericExp, token);

        return videoService.streamVideo(id);
    }
}
