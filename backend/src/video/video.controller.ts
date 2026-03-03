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
import {
  ApiBearerAuth,
  ApiOkResponse,
  ApiOperation,
  ApiParam,
  ApiProperty,
  ApiQuery,
  ApiTags,
} from '@nestjs/swagger';

class VideoFileDto {
  @ApiProperty({ example: 'sample.mp4' })
  id: string;

  @ApiProperty({ example: 'sample.mp4' })
  label: string;

  @ApiProperty({ example: 'video/mp4' })
  mime: string;

  @ApiProperty({ example: 'sample.mp4' })
  streamId: string;
}

class VideoListResponseDto {
  @ApiProperty({ type: [VideoFileDto] })
  files: VideoFileDto[];
}

class PresignedUrlResponseDto {
  @ApiProperty({
    example:
      'http://localhost:3000/video/stream/sample.mp4?token=abc&exp=12345678',
  })
  url: string;

  @ApiProperty({ example: 1717171717 })
  expiresAt: number;

  @ApiProperty({ example: 'sample.mp4' })
  streamId: string;
}

@ApiTags('video')
@Controller('video')
export class VideoController {
  constructor(
    private readonly videoService: VideoService,
    private readonly presignService: PresignService,
  ) {}

  @UseGuards(JwtAuthGuard)
  @Get('presign/:id')
  @ApiBearerAuth()
  @ApiOperation({
    summary: 'Generate presigned streaming URL for a given asset identifier',
  })
  @ApiParam({ name: 'id', description: 'Requested video id or filename' })
  @ApiOkResponse({ type: PresignedUrlResponseDto })
  generatePresignedUrl(@Param('id') id: string): PresignedUrlResponseDto {
    return this.presignService.generateSignedUrl(id);
  }

  @UseGuards(JwtAuthGuard)
  @Get('list')
  @ApiBearerAuth()
  @ApiOperation({ summary: 'List available video files' })
  @ApiOkResponse({ type: VideoListResponseDto })
  listVideos(): VideoListResponseDto {
    return { files: this.videoService.listAvailableVideos() };
  }

  @Get('stream/:id(*)')
  @ApiOperation({ summary: 'Stream video content using presigned URL' })
  @ApiParam({
    name: 'id',
    description: 'Resolved stream identifier (often includes path segments)',
  })
  @ApiQuery({
    name: 'token',
    description: 'HMAC token produced by presign endpoint',
    required: true,
  })
  @ApiQuery({
    name: 'exp',
    description: 'Expiration timestamp from presign endpoint',
    required: true,
  })
  @ApiOkResponse({
    description: 'Byte-range video stream',
    schema: { type: 'string', format: 'binary' },
  })
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
