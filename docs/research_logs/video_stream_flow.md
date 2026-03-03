# Video Streaming Flow (Frontend & Backend)

The following sequence diagram illustrates the detailed flow of securely streaming a video asset from the backend to the frontend using JWT authentication, short-lived presigned URLs, and HTTP range requests.

```mermaid
sequenceDiagram
    participant User
    participant Frontend as Vue Frontend (VideoPlayer.vue)
    participant AuthGuard as Backend AuthGuard (JWT)
    participant VideoCtrl as Backend Controller (VideoController)
    participant PresignSvc as Backend Service (PresignService)
    participant VideoSvc as Backend Service (VideoService)

    Note over User,VideoSvc: 1. Loading Asset Catalog
    User->>Frontend: Opens Video Player page
    Frontend->>VideoCtrl: GET /video/list (Bearer Token)
    VideoCtrl->>AuthGuard: Validate JWT
    AuthGuard-->>VideoCtrl: Token Valid (User context)
    VideoCtrl->>VideoSvc: listAvailableVideos()
    Note over VideoSvc: Reads `videos/` directory.<br/>If .flv, marks requiresTranscode=true.
    VideoSvc-->>VideoCtrl: Returns list of assets <br/>(id, label, streamId, mime)
    VideoCtrl-->>Frontend: 200 OK: { files: [...] }

    Note over User,VideoSvc: 2. Requesting Presigned URL
    User->>Frontend: Selects a video file
    Frontend->>VideoCtrl: GET /video/presign/{id} (Bearer Token)
    VideoCtrl->>AuthGuard: Validate JWT
    AuthGuard-->>VideoCtrl: Token Valid
    VideoCtrl->>PresignSvc: generateSignedUrl(id)
    PresignSvc->>VideoSvc: getPublicStreamId(id)
    Note over VideoSvc: Resolves asset name <br/>(e.g., sample.mp4 or converted/sample.mp4)
    VideoSvc-->>PresignSvc: streamId
    Note over PresignSvc: Generates `expiresAt` (now + 60s)<br/>Computes HMAC-SHA256 of `streamId:expiresAt`<br/>Constructs presigned URL
    PresignSvc-->>VideoCtrl: { url, expiresAt, streamId }
    VideoCtrl-->>Frontend: 200 OK: Presigned URL

    Note over User,VideoSvc: 3. Streaming Video via HTML5 <video>
    Frontend->>Frontend: Binds `<video src="url">`
    Frontend->>VideoCtrl: GET /video/stream/{streamId}?token={token}&exp={exp}
    Note over Frontend,VideoCtrl: The browser sends Range headers for partial content (e.g., `Range: bytes=0-`)
    VideoCtrl->>PresignSvc: validateSignature(streamId, exp, token)
    Note over PresignSvc: Checks if `now > exp`<br/>Verifies HMAC-SHA256 signature
    PresignSvc-->>VideoCtrl: Signature Valid
    VideoCtrl->>VideoSvc: streamVideo(streamId, req, res)
    VideoSvc->>VideoSvc: resolveVideoAsset(streamId)
    alt Requires Transcoding (e.g., FLV)
        Note over VideoSvc: Checks if `videos/converted/{name}.mp4` exists.
        Note over VideoSvc: Spawns ffmpeg process to convert (if missing or outdated).
    end
    Note over VideoSvc: Reads file size (`stat`)
    alt No Range Header
        VideoSvc-->>Frontend: 200 OK (Full File Stream)
    else Has Range Header
        Note over VideoSvc: Computes start, end, chunkSize
        VideoSvc-->>Frontend: 206 Partial Content<br/>(Content-Range: bytes START-END/SIZE)<br/>(Stream chunk from `start` to `end`)
    end

    Frontend-->>User: Video starts playing in browser
```

## Key Technical Details

1. **Presigned URLs**: To avoid sending JWT tokens in HTML5 `<video>` requests (which by default don't support `Authorization` headers well across browsers without XHR blob buffering), we use temporary presigned URLs.
2. **HMAC Signature**: The backend generates a secure token for the URL using `crypto.createHmac('sha256', secret).update('streamId:expiresAt').digest('hex')`.
3. **HTTP 206 Partial Content**: The Fast/Seekable streaming is achieved through HTTP Range requests and byte-range fs streams (`fs.createReadStream(path, { start, end })`).
4. **On-the-fly Transcoding**: Unsupported web formats like `.flv` are detected and processed via `ffmpeg` into `.mp4` transparently before being streamed to the user.
