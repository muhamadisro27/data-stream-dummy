import { Module } from '@nestjs/common';
import { VideoController } from './video.controller';
import { VideoService } from './video.service';
import { PresignService } from './presign.service';

@Module({
  controllers: [VideoController],
  providers: [VideoService, PresignService],
  exports: [PresignService],
})
export class VideoModule {}
