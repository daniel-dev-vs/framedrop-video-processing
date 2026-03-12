package com.framedrop.video_processing.adapters.out;
import com.framedrop.video_processing.core.domain.ports.out.UploadZipOutputPort;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import java.io.File;
@RequiredArgsConstructor
public class UploadZipS3Adapter implements UploadZipOutputPort {
    private final S3Client s3Client;
    @Value("${aws.s3.bucket-name}")
    private String bucketName;
    @Override
    public void uploadZip(String destinationPath, File zipFile) {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(destinationPath)
                .contentType("application/zip")
                .build();
        s3Client.putObject(putObjectRequest,
                RequestBody.fromFile(zipFile));
    }
}
