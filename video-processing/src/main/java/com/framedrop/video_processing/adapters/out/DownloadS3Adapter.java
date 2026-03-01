package com.framedrop.video_processing.adapters.out;
import com.framedrop.video_processing.core.domain.ports.out.DownloadVideoOutputPort;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
@RequiredArgsConstructor
public class DownloadS3Adapter implements DownloadVideoOutputPort {
    private final S3Client s3Client;
    @Value("${aws.s3.bucket-name}")
    private String bucketName;
    @Override
    public File downloadVideo(String videoPath, Path destinationDir) {
        String fileName = videoPath.substring(videoPath.lastIndexOf('/') + 1);
        File destinationFile = destinationDir.resolve(fileName).toFile();
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(videoPath)
                .build();
        try (ResponseInputStream<GetObjectResponse> s3Object = s3Client.getObject(getObjectRequest);
             FileOutputStream outputStream = new FileOutputStream(destinationFile)) {
            s3Object.transferTo(outputStream);
        } catch (IOException e) {
            throw new RuntimeException("Failed to download video from S3: " + videoPath, e);
        }
        return destinationFile;
    }
}
