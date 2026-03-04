import { ForbiddenException, Injectable, UnauthorizedException } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import * as crypto from 'crypto';
import { VideoService } from './video.service';

@Injectable()
export class PresignService {
  constructor(
    private readonly configService: ConfigService,
    private readonly videoService: VideoService,
  ) {}

  generateSignedUrl(id: string) {
    const streamId = this.videoService.getPublicStreamId(id);
    const expiresAt = Math.floor(Date.now() / 1000) + 60;
    const token = this.sign(`${streamId}:${expiresAt}`);
    const baseUrl = this.configService.get<string>('APP_URL') || 'http://localhost:3000';

    return {
      url: `${baseUrl}/video/stream/${streamId}?token=${token}&exp=${expiresAt}`,
      expiresAt,
      streamId,
    };
  }

  validateSignature(id: string, exp: number, token: string) {
    const now = Math.floor(Date.now() / 1000);
    if (now > exp) {
      throw new ForbiddenException('Presigned URL expired');
    }

    const expected = this.sign(`${id}:${exp}`);
    const provided = Buffer.from(token, 'hex');
    const actual = Buffer.from(expected, 'hex');

    if (provided.length !== actual.length || !crypto.timingSafeEqual(provided, actual)) {
      throw new UnauthorizedException('Invalid token');
    }
  }

  private sign(payload: string) {
    const secret = this.configService.get<string>('PRESIGN_SECRET') || 'mysecret';
    return crypto.createHmac('sha256', secret).update(payload).digest('hex');
  }
}
