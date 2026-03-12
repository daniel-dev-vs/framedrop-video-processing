package com.framedrop.video_processing.adapters.out;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DownloadS3AdapterTest {

    @Mock
    private S3Client s3Client;

    @InjectMocks
    private DownloadS3Adapter downloadS3Adapter;

    @TempDir
    Path tempDir;

    @Test
    void shouldDownloadVideoSuccessfully() throws IOException {
        String videoPath = "uploads/user-1/video.mp4";
        byte[] fileContent = "fake video content".getBytes();

        ResponseInputStream<GetObjectResponse> responseStream = new ResponseInputStream<>(
                GetObjectResponse.builder().build(),
                new ByteArrayInputStream(fileContent)
        );

        when(s3Client.getObject(any(GetObjectRequest.class))).thenReturn(responseStream);

        File result = downloadS3Adapter.downloadVideo(videoPath, tempDir);

        assertNotNull(result);
        assertEquals("video.mp4", result.getName());
        assertTrue(result.exists());
    }

    @Test
    void shouldExtractFileNameFromPath() throws IOException {
        String videoPath = "uploads/subfolder/my_video.mkv";
        byte[] fileContent = "content".getBytes();

        ResponseInputStream<GetObjectResponse> responseStream = new ResponseInputStream<>(
                GetObjectResponse.builder().build(),
                new ByteArrayInputStream(fileContent)
        );

        when(s3Client.getObject(any(GetObjectRequest.class))).thenReturn(responseStream);

        File result = downloadS3Adapter.downloadVideo(videoPath, tempDir);

        assertEquals("my_video.mkv", result.getName());
    }

    @Test
    void shouldBuildCorrectGetObjectRequest() throws IOException {
        String videoPath = "uploads/video.mp4";
        byte[] fileContent = "content".getBytes();

        ResponseInputStream<GetObjectResponse> responseStream = new ResponseInputStream<>(
                GetObjectResponse.builder().build(),
                new ByteArrayInputStream(fileContent)
        );

        when(s3Client.getObject(any(GetObjectRequest.class))).thenReturn(responseStream);

        downloadS3Adapter.downloadVideo(videoPath, tempDir);

        ArgumentCaptor<GetObjectRequest> captor = ArgumentCaptor.forClass(GetObjectRequest.class);
        verify(s3Client).getObject(captor.capture());

        GetObjectRequest request = captor.getValue();
        assertEquals(videoPath, request.key());
    }

    @Test
    void shouldThrowRuntimeExceptionWhenS3Fails() {
        String videoPath = "uploads/video.mp4";

        when(s3Client.getObject(any(GetObjectRequest.class)))
                .thenThrow(new RuntimeException("S3 error"));

        assertThrows(RuntimeException.class,
                () -> downloadS3Adapter.downloadVideo(videoPath, tempDir));
    }

    @Test
    void shouldHandleFileNameWithoutSlash() throws IOException {
        String videoPath = "video.mp4";
        byte[] fileContent = "content".getBytes();

        ResponseInputStream<GetObjectResponse> responseStream = new ResponseInputStream<>(
                GetObjectResponse.builder().build(),
                new ByteArrayInputStream(fileContent)
        );

        when(s3Client.getObject(any(GetObjectRequest.class))).thenReturn(responseStream);

        File result = downloadS3Adapter.downloadVideo(videoPath, tempDir);

        assertEquals("video.mp4", result.getName());
    }
}
