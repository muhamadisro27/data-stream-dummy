# Java Spring Boot Backend Research Log

## Objective

Replicate the existing NestJS video streaming backend exactly using Java 17 and Spring Boot 3.4.1. The code will reside in the `backend-java` directory.

## Core Features & Equivalents

1. **Framework**: Spring Boot 3.4.1 (Web, Security, Validation).
2. **OpenAPI/Swagger**: `springdoc-openapi-starter-webmvc-ui` for endpoint documentation.
3. **CORS**: Spring Web MVC CORS configuration (`@CrossOrigin` or `WebMvcConfigurer`).
4. **JWT Authentication**: Spring Security with a custom `OncePerRequestFilter` to validate `Authorization: Bearer <token>` (mocking or implementing the same JWT secret format, usually we will just skip deep JWT generation and focus on validation of the incoming JWT token, since it seems the frontend might bring it or the backend provisions it—wait, the NestJS backend didn't have login logic in the snippet we saw, just a JWT guard. We will implement a simplified JWT filter or copy the approach).
5. **Presigned URLs (HMAC)**:
   - Java `Mac` class with `HmacSHA256`.
   - Payload: `streamId:expiresAt`.
6. **Video Streaming (Range Requests)**:
   - Spring supports `ResourceHttpRequestHandler` or returning `ResponseEntity<Resource>` which automatically handles `Range` headers. This is a huge win for Spring Boot built-in capabilities compared to manual calculation. Alternatively, manally computing the `206 Partial Content` as done in NestJS using `RandomAccessFile` or `InputStream` with skipping. But `FileSystemResource` + `ResponseEntity<Resource>` with `HttpStatus.PARTIAL_CONTENT` supported natively by Spring is standard.
7. **FFmpeg Transcoding**:
   - `ProcessBuilder` or `Runtime.getRuntime().exec()`.
   - Command: `ffmpeg -y -i <source> -c:v libx264 -c:a aac <output>`.

## Architectural Structure (Vertical Slices)

Based on `project-structure.md` and Clean Architecture principles:

```
backend-java/
  pom.xml
  src/main/java/com/streaming/
    StreamingApplication.java
    platform/
      config/        # CORS, Swagger Config
      security/      # JWT Filter
    features/
      video/
        VideoController.java
        VideoService.java
        PresignService.java
        models/      # DTOs
```

## Action Plan

Proceed to Implement phase.

1. Scaffold `pom.xml`.
2. Create absolute minimum boilerplate.
3. Implement `PresignService` and `VideoService`.
4. Implement `VideoController`.
