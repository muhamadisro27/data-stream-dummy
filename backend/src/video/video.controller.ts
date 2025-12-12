import {
  Controller,
  Get,
  Param,
  Query,
  Req,
  Res,
  UseGuards,
  BadRequestException,
} from '@nestjs/common';
import { Request, Response } from 'express';
import { VideoService } from './video.service';
import { PresignService } from './presign.service';
import { JwtAuthGuard } from '../auth/jwt-auth.guard';

@Controller('video')
export class VideoController {
  constructor(
    private readonly videoService: VideoService,
    private readonly presignService: PresignService,
  ) {}

  @UseGuards(JwtAuthGuard)
  @Get('presign/:id')
  generatePresignedUrl(@Param('id') id: string) {
    return this.presignService.generateSignedUrl(id);
  }

  @UseGuards(JwtAuthGuard)
  @Get('list')
  listVideos() {
    return { files: this.videoService.listAvailableVideos() };
  }

  @Get('stream/:id(*)')
  streamVideo(
    @Param('id') id: string,
    @Query('token') token: string,
    @Query('exp') exp: string,
    @Req() req: Request,
    @Res() res: Response,
  ) {
    if (!token || !exp) {
      throw new BadRequestException('Missing token or exp');
    }

    const numericExp = Number(exp);
    if (Number.isNaN(numericExp)) {
      throw new BadRequestException('exp must be a UNIX timestamp');
    }

    this.presignService.validateSignature(id, numericExp, token);
    this.videoService.streamVideo(id, req, res);
  }
}
