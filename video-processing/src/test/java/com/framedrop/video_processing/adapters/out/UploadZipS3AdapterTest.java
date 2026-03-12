package com.framedrop.video_processing.adapters.out;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UploadZipS3AdapterTest {

    @Mock
    private S3Client s3Client;

    @InjectMocks
    private UploadZipS3Adapter uploadZipS3Adapter;

    @Test
    void shouldUploadZipSuccessfully() throws IOException {
        File zipFile = Files.createTempFile("test", ".zip").toFile();
        zipFile.deleteOnExit();
        Files.writeString(zipFile.toPath(), "zip content");

        String destinationPath = "processed/user-1/vid-1_frames.zip";

        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        assertDoesNotThrow(() -> uploadZipS3Adapter.uploadZip(destinationPath, zipFile));

        verify(s3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    void shouldBuildCorrectPutObjectRequest() throws IOException {
        File zipFile = Files.createTempFile("test", ".zip").toFile();
        zipFile.deleteOnExit();
        Files.writeString(zipFile.toPath(), "zip content");

        String destinationPath = "processed/user-1/vid-1_frames.zip";

        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        uploadZipS3Adapter.uploadZip(destinationPath, zipFile);

        ArgumentCaptor<PutObjectRequest> captor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(captor.capture(), any(RequestBody.class));

        PutObjectRequest request = captor.getValue();
        assertEquals(destinationPath, request.key());
        assertEquals("application/zip", request.contentType());
    }

    @Test
    void shouldThrowExceptionWhenS3UploadFails() throws IOException {
        File zipFile = Files.createTempFile("test", ".zip").toFile();
        zipFile.deleteOnExit();
        Files.writeString(zipFile.toPath(), "zip content");

        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenThrow(new RuntimeException("S3 upload error"));

        assertThrows(RuntimeException.class,
                () -> uploadZipS3Adapter.uploadZip("path/file.zip", zipFile));
    }
}
