# Secure Video Streaming Project

Welcome to the Secure Video Streaming project. This repository contains a full-stack proof-of-concept application designed to stream video files securely to a web client. It generates expiring presigned URLs using HMAC SHA-256 signatures, ensuring that video files can only be accessed by authorized users for a limited timeframe.

This repository consists of three main projects:

## 1. Frontend (`/frontend`)

The frontend client responsible for authenticating the user and rendering the video player.

- **Tech Stack:** Vue 3 (Composition API), Vite, Axios, Vue Router.
- **Key Features:**
  - Login interface to get the JWT access token.
  - Video player interface to select a video and request a presigned streaming URL.
  - Demonstrates how to pass JWT tokens in API calls and handle temporary presigned video URLs in HTML5 `<video>` tags.
- **Run Instructions:**
  ```bash
  cd frontend
  npm install
  npm run dev
  ```

## 2. Backend - Java Spring Boot (`/backend-java`)

The primary backend implementation for generating presigned URLs and streaming videos.

- **Tech Stack:** Java 17, Spring Boot 3.4.1, Spring Security, OpenAPI (Swagger), JWT.
- **Key Features:**
  - `AuthController`: Provides a demo `/auth/login` endpoint that returns a signed JWT.
  - `VideoController`:
    - `/video/list`: Lists available video files in the `backend/videos/` directory.
    - `/video/presign/{id}`: Generates a temporary URL with an HMAC-SHA256 signature and expiration timestamp.
    - `/video/stream/{id}`: Validates the token and expiration. If valid, securely streams the video using Spring's native `Resource` byte-range handling (HTTP 206 Partial Content).
  - **Swagger UI:** Available at `http://localhost:8080/swagger-ui/index.html`.
- **Run Instructions:**
  ```bash
  cd backend-java
  mvn spring-boot:run
  ```

## 3. Backend - NestJS (`/backend-nest`)

The original backend implementation, providing the same exact API contracts as the Java version. This serves as a reference implementation.

- **Tech Stack:** Node.js, NestJS 10, Passport JWT.
- **Key Features:**
  - NestJS video streaming using Express `res.download` / byte-range streaming logic.
  - HMAC-SHA256 token generation and expiration validation.
  - Demonstrates equivalent JWT Authentication strategies in the Node.js ecosystem.
- **Run Instructions:**
  ```bash
  cd backend-nest
  npm install
  npm run start:dev
  ```

## Architecture Pattern - Security Flow

The following sequence diagram illustrates the detailed end-to-end interactions between the user, frontend, and backend services.

<!-- ![Video Streaming Flow Architecture Diagram](<./diagram\ flow\ stream.png>) -->

### Flow Description

1. **Initialization:**
   - The User opens the Video Player page.
   - The Frontend sends a `GET /video/list` request using a Bearer Token.
   - The Backend (`AuthGuard`) validates the JWT.
   - The `VideoController` calls `VideoService.listAvailableVideos()`.
   - The Backend returns a `200 OK` with a list of available assets containing `[{id, label, streamId, mime}]`.

2. **Presigning Process:**
   - The User selects a video file.
   - The Frontend requests access to the specific video via `GET /video/presign/{id}` using the Bearer Token.
   - `AuthGuard` validates the JWT.
   - `VideoController` calls `generateSignedUrl(id)` on `PresignService`.
   - `PresignService` calls `getPublicStreamId(id)` on `VideoService` to resolve the actual stream identifier.
   - `PresignService` returns a presigned URL containing `{url, expiresAt, streamId}`.
   - The Backend returns a `200 OK` with the Presigned URL payload to the Frontend.

3. **Streaming Initialization:**
   - The Frontend binds the HTML `<video>` element's `src` attribute to the presigned `url`.
   - The Browser automatically sends a `GET /video/stream/{streamId}?token={token}&exp={exp}` request.
   - `VideoController` calls `validateSignature(streamId, exp, token)` on `PresignService`.
   - If the signature is valid, `VideoController` calls `streamVideo(streamId, req, res)` on `VideoService`.
   - `VideoService` calls `resolveVideoAsset(streamId)` to locate the file on disk.

4. **Response Delivery (Byte-Range Streaming):**
   - **Scenario A (No Range Header):** If the browser does not request a specific range, the backend responds with `200 OK` and streams the full file.
   - **Scenario B (Has Range Header):** If the browser requests a specific chunk (e.g., `Range: bytes=0-1024`), the backend responds with `206 Partial Content` and streams the requested chunk (`Content-Range: bytes START-END/SIZE`).
   - The loop continues as the browser requests subsequent chunks while the video plays.
