package com.framedrop.video_processing.adapters.config;
import com.framedrop.video_processing.adapters.out.DownloadS3Adapter;
import com.framedrop.video_processing.adapters.out.UploadZipS3Adapter;
import com.framedrop.video_processing.core.application.usecases.ProcessVideoUseCase;
import com.framedrop.video_processing.core.domain.ports.in.ProcessVideoInputPort;
import com.framedrop.video_processing.core.domain.ports.out.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.s3.S3Client;
@Configuration
@RequiredArgsConstructor
public class ProcessVideoConfig {
    @Bean
    public ProcessVideoInputPort createProcessVideoInputPort(DownloadVideoOutputPort downloadVideoOutputPort,
                                                             ValidateVideoOutputPort validateVideoOutputPort,
                                                             ExtractFramesOutputPort extractFramesOutputPort,
                                                             ZipFramesOutputPort zipFramesOutputPort,
                                                             UploadZipOutputPort uploadZipOutputPort,
                                                             @Value("${video.processing.base-dir}") String baseDir) {
        return new ProcessVideoUseCase(
                downloadVideoOutputPort,
                validateVideoOutputPort,
                extractFramesOutputPort,
                zipFramesOutputPort,
                uploadZipOutputPort,
                baseDir);
    }
    @Bean
    public DownloadVideoOutputPort createDownloadVideoOutputPort(S3Client s3Client) {
        return new DownloadS3Adapter(s3Client);
    }
    @Bean
    public UploadZipOutputPort createUploadZipOutputPort(S3Client s3Client) {
        return new UploadZipS3Adapter(s3Client);
    }
}
