import { Injectable, InternalServerErrorException, NotFoundException } from '@nestjs/common';
import { Request, Response } from 'express';
import { createReadStream, existsSync, statSync, readdirSync, mkdirSync } from 'fs';
import { basename, extname, join, posix as posixPath } from 'path';
import { spawnSync } from 'child_process';
import { path as ffmpegPath } from '@ffmpeg-installer/ffmpeg';

@Injectable()
export class VideoService {
  private readonly videoDir = join(__dirname, '..', '..', 'videos');
  private readonly convertedDir = join(this.videoDir, 'converted');
  private readonly supportedTypes = new Map<
    string,
    { mime: string; requiresTranscode?: boolean }
  >([
    ['.mp4', { mime: 'video/mp4' }],
    ['.mov', { mime: 'video/quicktime' }],
    ['.flv', { mime: 'video/mp4', requiresTranscode: true }],
  ]);

  listAvailableVideos() {
    const files =
      readdirSync(this.videoDir, { withFileTypes: true })
        .filter((entry) => entry.isFile())
        .map((entry) => entry.name)
        .filter((name) => this.supportedTypes.has(extname(name).toLowerCase()))
        .map((name) => {
          const config = this.supportedTypes.get(extname(name).toLowerCase());
          return {
            id: name,
            label: config?.requiresTranscode ? `${name} (auto-converted)` : name,
            mime: config?.mime ?? 'video/mp4',
            streamId: config?.requiresTranscode
              ? this.getConvertedPublicId(name)
              : name,
          };
        }) ?? [];

    return files;
  }

  getPublicStreamId(id: string) {
    return this.resolveVideoAsset(id).publicId;
  }

  private buildLookupList(id: string) {
    const attempts: {
      path: string;
      config: { mime: string; requiresTranscode?: boolean };
      candidateId: string;
    }[] = [];
    const providedExt = extname(id);

    if (providedExt) {
      const normalizedExt = providedExt.toLowerCase();
      const config = this.supportedTypes.get(normalizedExt);
      if (config) {
        attempts.push({ path: join(this.videoDir, id), config, candidateId: id });
      }
      return attempts;
    }

    for (const [ext, config] of this.supportedTypes.entries()) {
      const candidateId = `${id}${ext}`;
      attempts.push({ path: join(this.videoDir, candidateId), config, candidateId });
    }

    return attempts;
  }

  private resolveVideoAsset(id: string) {
    const lookupOrder = this.buildLookupList(id);
    for (const candidate of lookupOrder) {
      if (existsSync(candidate.path)) {
        return this.prepareAsset(candidate.path, candidate.config, candidate.candidateId);
      }
    }

    if (id !== 'sample') {
      const fallback = this.buildLookupList('sample').find(({ path }) => existsSync(path));
      if (fallback) {
        return this.prepareAsset(fallback.path, fallback.config, fallback.candidateId);
      }
    }

    throw new NotFoundException(
      'Video asset missing. Provide videos/<name>.mp4|.mov|.flv or place sample.*',
    );
  }

  streamVideo(id: string, req: Request, res: Response) {
    const asset = this.resolveVideoAsset(id);
    const videoStat = statSync(asset.path);
    const fileSize = videoStat.size;
    const range = req.headers.range;

    if (!range) {
      res.status(200);
      res.setHeader('Content-Length', fileSize);
      res.setHeader('Content-Type', asset.mime);
      res.setHeader('Accept-Ranges', 'bytes');
      const stream = createReadStream(asset.path);
      stream.pipe(res);
      return;
    }

    const bytesPrefix = 'bytes=';
    if (!range.startsWith(bytesPrefix)) {
      res.status(416).end();
      return;
    }

    const parts = range.replace(bytesPrefix, '').split('-');
    const start = parseInt(parts[0], 10);
    const end = parts[1] ? parseInt(parts[1], 10) : fileSize - 1;

    if (isNaN(start) || isNaN(end) || start > end || end >= fileSize) {
      res.status(416).end();
      return;
    }

    const chunkSize = end - start + 1;
    res.status(206);
    res.setHeader('Content-Range', `bytes ${start}-${end}/${fileSize}`);
    res.setHeader('Accept-Ranges', 'bytes');
    res.setHeader('Content-Length', chunkSize);
    res.setHeader('Content-Type', asset.mime);

    const stream = createReadStream(asset.path, { start, end });
    stream.pipe(res);
  }

  private prepareAsset(
    sourcePath: string,
    config: { mime: string; requiresTranscode?: boolean },
    publicId: string,
  ) {
    if (config.requiresTranscode) {
      const convertedPublicId = this.getConvertedPublicId(publicId);
      return {
        path: this.ensureConvertedMp4(sourcePath),
        mime: 'video/mp4',
        publicId: convertedPublicId,
      };
    }

    return { path: sourcePath, mime: config.mime, publicId };
  }

  private ensureConvertedMp4(sourcePath: string) {
    if (!existsSync(this.convertedDir)) {
      mkdirSync(this.convertedDir, { recursive: true });
    }

    const outputPath = join(
      this.convertedDir,
      `${basename(sourcePath, extname(sourcePath))}.mp4`,
    );

    const sourceStat = statSync(sourcePath);
    const needsConversion =
      !existsSync(outputPath) || statSync(outputPath).mtimeMs < sourceStat.mtimeMs;

    if (needsConversion) {
      const binary = this.resolveFfmpegBinary();
      if (!binary) {
        throw new InternalServerErrorException(
          'Automatic conversion requires ffmpeg. Install it or set FFMPEG_PATH.',
        );
      }
      const result = spawnSync(binary, [
        '-y',
        '-i',
        sourcePath,
        '-c:v',
        'libx264',
        '-c:a',
        'aac',
        outputPath,
      ]);

      if (result.status !== 0) {
        const stderr = result.stderr?.toString() || result.error?.message || 'Unknown';
        throw new InternalServerErrorException(
          `Failed to convert FLV. ffmpeg error: ${stderr}`,
        );
      }
    }

    return outputPath;
  }

  private resolveFfmpegBinary() {
    return process.env.FFMPEG_PATH || ffmpegPath || null;
  }

  private getConvertedPublicId(fileName: string) {
    const nameWithoutExt = basename(fileName, extname(fileName));
    return posixPath.join('converted', `${nameWithoutExt}.mp4`);
  }
}
