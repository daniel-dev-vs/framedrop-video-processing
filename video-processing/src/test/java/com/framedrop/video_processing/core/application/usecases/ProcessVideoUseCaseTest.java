package com.framedrop.video_processing.core.application.usecases;

import com.framedrop.video_processing.core.domain.ports.out.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProcessVideoUseCaseTest {

    @Mock
    private DownloadVideoOutputPort downloadVideoOutputPort;
    @Mock
    private ValidateVideoOutputPort validateVideoOutputPort;
    @Mock
    private ExtractFramesOutputPort extractFramesOutputPort;
    @Mock
    private ZipFramesOutputPort zipFramesOutputPort;
    @Mock
    private UploadZipOutputPort uploadZipOutputPort;
    @Mock
    private UpdateVideoStatusOutputPort updateVideoStatusOutputPort;

    @TempDir
    Path tempDir;

    private ProcessVideoUseCase processVideoUseCase;

    @BeforeEach
    void setUp() {
        processVideoUseCase = new ProcessVideoUseCase(
                downloadVideoOutputPort,
                validateVideoOutputPort,
                extractFramesOutputPort,
                zipFramesOutputPort,
                uploadZipOutputPort,
                updateVideoStatusOutputPort,
                tempDir.toString()
        );
    }

    @Test
    void shouldProcessVideoSuccessfully() throws IOException {
        String videoId = "vid-123";
        String userId = "user-456";
        String videoPath = "uploads/user-456/video.mp4";
        String status = "PENDING";

        File videoFile = Files.createTempFile(tempDir, "video", ".mp4").toFile();
        File frame1 = Files.createTempFile(tempDir, "frame1", ".png").toFile();
        File frame2 = Files.createTempFile(tempDir, "frame2", ".png").toFile();
        File zipFile = Files.createTempFile(tempDir, "frames", ".zip").toFile();

        when(downloadVideoOutputPort.downloadVideo(eq(videoPath), any(Path.class))).thenReturn(videoFile);
        when(validateVideoOutputPort.isValidFormatVideo(videoFile)).thenReturn(true);
        when(extractFramesOutputPort.extractFrames(eq(videoFile), any(Path.class))).thenReturn(List.of(frame1, frame2));
        when(zipFramesOutputPort.zipFrames(anyList(), any(Path.class), eq("video_frames.zip"))).thenReturn(zipFile);

        processVideoUseCase.processVideo(videoId, userId, videoPath, status);

        verify(updateVideoStatusOutputPort).updateStatus(videoId, "PROCESSING");
        verify(downloadVideoOutputPort).downloadVideo(eq(videoPath), any(Path.class));
        verify(validateVideoOutputPort).isValidFormatVideo(videoFile);
        verify(extractFramesOutputPort).extractFrames(eq(videoFile), any(Path.class));
        verify(zipFramesOutputPort).zipFrames(anyList(), any(Path.class), eq("video_frames.zip"));
        verify(uploadZipOutputPort).uploadZip(eq("processed/user-456/vid-123_video_frames.zip"), eq(zipFile));
        verify(updateVideoStatusOutputPort).updateStatus(videoId, "COMPLETED");
    }

    @Test
    void shouldSetStatusToFailedWhenVideoFormatIsInvalid() throws IOException {
        String videoId = "vid-123";
        String userId = "user-456";
        String videoPath = "uploads/user-456/video.mp4";

        File videoFile = Files.createTempFile(tempDir, "video", ".mp4").toFile();

        when(downloadVideoOutputPort.downloadVideo(eq(videoPath), any(Path.class))).thenReturn(videoFile);
        when(validateVideoOutputPort.isValidFormatVideo(videoFile)).thenReturn(false);

        assertThrows(RuntimeException.class,
                () -> processVideoUseCase.processVideo(videoId, userId, videoPath, "PENDING"));

        verify(updateVideoStatusOutputPort).updateStatus(videoId, "PROCESSING");
        verify(updateVideoStatusOutputPort).updateStatus(videoId, "FAILED");
        verify(extractFramesOutputPort, never()).extractFrames(any(), any());
    }

    @Test
    void shouldSetStatusToFailedWhenDownloadFails() {
        String videoId = "vid-123";
        String userId = "user-456";
        String videoPath = "uploads/user-456/video.mp4";

        when(downloadVideoOutputPort.downloadVideo(eq(videoPath), any(Path.class)))
                .thenThrow(new RuntimeException("S3 connection error"));

        assertThrows(RuntimeException.class,
                () -> processVideoUseCase.processVideo(videoId, userId, videoPath, "PENDING"));

        verify(updateVideoStatusOutputPort).updateStatus(videoId, "PROCESSING");
        verify(updateVideoStatusOutputPort).updateStatus(videoId, "FAILED");
    }

    @Test
    void shouldSetStatusToFailedWhenFrameExtractionFails() throws IOException {
        String videoId = "vid-123";
        String userId = "user-456";
        String videoPath = "uploads/user-456/video.mp4";

        File videoFile = Files.createTempFile(tempDir, "video", ".mp4").toFile();

        when(downloadVideoOutputPort.downloadVideo(eq(videoPath), any(Path.class))).thenReturn(videoFile);
        when(validateVideoOutputPort.isValidFormatVideo(videoFile)).thenReturn(true);
        when(extractFramesOutputPort.extractFrames(eq(videoFile), any(Path.class)))
                .thenThrow(new RuntimeException("Frame extraction error"));

        assertThrows(RuntimeException.class,
                () -> processVideoUseCase.processVideo(videoId, userId, videoPath, "PENDING"));

        verify(updateVideoStatusOutputPort).updateStatus(videoId, "FAILED");
    }

    @Test
    void shouldSetStatusToFailedWhenZipCreationFails() throws IOException {
        String videoId = "vid-123";
        String userId = "user-456";
        String videoPath = "uploads/user-456/video.mp4";

        File videoFile = Files.createTempFile(tempDir, "video", ".mp4").toFile();
        File frame1 = Files.createTempFile(tempDir, "frame1", ".png").toFile();

        when(downloadVideoOutputPort.downloadVideo(eq(videoPath), any(Path.class))).thenReturn(videoFile);
        when(validateVideoOutputPort.isValidFormatVideo(videoFile)).thenReturn(true);
        when(extractFramesOutputPort.extractFrames(eq(videoFile), any(Path.class))).thenReturn(List.of(frame1));
        when(zipFramesOutputPort.zipFrames(anyList(), any(Path.class), anyString()))
                .thenThrow(new RuntimeException("Zip creation error"));

        assertThrows(RuntimeException.class,
                () -> processVideoUseCase.processVideo(videoId, userId, videoPath, "PENDING"));

        verify(updateVideoStatusOutputPort).updateStatus(videoId, "FAILED");
    }

    @Test
    void shouldSetStatusToFailedWhenUploadFails() throws IOException {
        String videoId = "vid-123";
        String userId = "user-456";
        String videoPath = "uploads/user-456/video.mp4";

        File videoFile = Files.createTempFile(tempDir, "video", ".mp4").toFile();
        File frame1 = Files.createTempFile(tempDir, "frame1", ".png").toFile();
        File zipFile = Files.createTempFile(tempDir, "frames", ".zip").toFile();

        when(downloadVideoOutputPort.downloadVideo(eq(videoPath), any(Path.class))).thenReturn(videoFile);
        when(validateVideoOutputPort.isValidFormatVideo(videoFile)).thenReturn(true);
        when(extractFramesOutputPort.extractFrames(eq(videoFile), any(Path.class))).thenReturn(List.of(frame1));
        when(zipFramesOutputPort.zipFrames(anyList(), any(Path.class), anyString())).thenReturn(zipFile);
        doThrow(new RuntimeException("Upload error")).when(uploadZipOutputPort).uploadZip(anyString(), any(File.class));

        assertThrows(RuntimeException.class,
                () -> processVideoUseCase.processVideo(videoId, userId, videoPath, "PENDING"));

        verify(updateVideoStatusOutputPort).updateStatus(videoId, "FAILED");
    }

    @Test
    void shouldExtractFileNameFromPathWithSlashes() throws IOException {
        String videoPath = "uploads/user/subfolder/my_video.mp4";
        File videoFile = Files.createTempFile(tempDir, "video", ".mp4").toFile();
        File zipFile = Files.createTempFile(tempDir, "frames", ".zip").toFile();

        when(downloadVideoOutputPort.downloadVideo(eq(videoPath), any(Path.class))).thenReturn(videoFile);
        when(validateVideoOutputPort.isValidFormatVideo(videoFile)).thenReturn(true);
        when(extractFramesOutputPort.extractFrames(eq(videoFile), any(Path.class))).thenReturn(List.of());
        when(zipFramesOutputPort.zipFrames(anyList(), any(Path.class), eq("my_video_frames.zip"))).thenReturn(zipFile);

        processVideoUseCase.processVideo("vid-1", "user-1", videoPath, "PENDING");

        verify(zipFramesOutputPort).zipFrames(anyList(), any(Path.class), eq("my_video_frames.zip"));
    }

    @Test
    void shouldExtractFileNameWhenNoSlashInPath() throws IOException {
        String videoPath = "video.mp4";
        File videoFile = Files.createTempFile(tempDir, "video", ".mp4").toFile();
        File zipFile = Files.createTempFile(tempDir, "frames", ".zip").toFile();

        when(downloadVideoOutputPort.downloadVideo(eq(videoPath), any(Path.class))).thenReturn(videoFile);
        when(validateVideoOutputPort.isValidFormatVideo(videoFile)).thenReturn(true);
        when(extractFramesOutputPort.extractFrames(eq(videoFile), any(Path.class))).thenReturn(List.of());
        when(zipFramesOutputPort.zipFrames(anyList(), any(Path.class), eq("video_frames.zip"))).thenReturn(zipFile);

        processVideoUseCase.processVideo("vid-1", "user-1", videoPath, "PENDING");

        verify(zipFramesOutputPort).zipFrames(anyList(), any(Path.class), eq("video_frames.zip"));
    }

    @Test
    void shouldBuildCorrectZipDestinationPath() throws IOException {
        String videoId = "vid-abc";
        String userId = "user-xyz";
        String videoPath = "uploads/video.mp4";

        File videoFile = Files.createTempFile(tempDir, "video", ".mp4").toFile();
        File zipFile = Files.createTempFile(tempDir, "frames", ".zip").toFile();

        when(downloadVideoOutputPort.downloadVideo(eq(videoPath), any(Path.class))).thenReturn(videoFile);
        when(validateVideoOutputPort.isValidFormatVideo(videoFile)).thenReturn(true);
        when(extractFramesOutputPort.extractFrames(eq(videoFile), any(Path.class))).thenReturn(List.of());
        when(zipFramesOutputPort.zipFrames(anyList(), any(Path.class), anyString())).thenReturn(zipFile);

        processVideoUseCase.processVideo(videoId, userId, videoPath, "PENDING");

        verify(uploadZipOutputPort).uploadZip(eq("processed/user-xyz/vid-abc_video_frames.zip"), eq(zipFile));
    }

    @Test
    void shouldCleanUpProcessDirectoryAfterSuccess() throws IOException {
        String videoId = "vid-cleanup";
        String videoPath = "uploads/video.mp4";

        File videoFile = Files.createTempFile(tempDir, "video", ".mp4").toFile();
        File zipFile = Files.createTempFile(tempDir, "frames", ".zip").toFile();

        when(downloadVideoOutputPort.downloadVideo(eq(videoPath), any(Path.class))).thenReturn(videoFile);
        when(validateVideoOutputPort.isValidFormatVideo(videoFile)).thenReturn(true);
        when(extractFramesOutputPort.extractFrames(eq(videoFile), any(Path.class))).thenReturn(List.of());
        when(zipFramesOutputPort.zipFrames(anyList(), any(Path.class), anyString())).thenReturn(zipFile);

        processVideoUseCase.processVideo(videoId, "user-1", videoPath, "PENDING");

        Path processDir = tempDir.resolve(videoId);
        assertFalse(Files.exists(processDir), "Process directory should be cleaned up after success");
    }

    @Test
    void shouldCleanUpProcessDirectoryAfterFailure() {
        String videoId = "vid-cleanup-fail";
        String videoPath = "uploads/video.mp4";

        when(downloadVideoOutputPort.downloadVideo(eq(videoPath), any(Path.class)))
                .thenThrow(new RuntimeException("Download failed"));

        assertThrows(RuntimeException.class,
                () -> processVideoUseCase.processVideo(videoId, "user-1", videoPath, "PENDING"));

        Path processDir = tempDir.resolve(videoId);
        assertFalse(Files.exists(processDir), "Process directory should be cleaned up after failure");
    }

    @Test
    void shouldThrowRuntimeExceptionWithCorrectMessage() {
        String videoId = "vid-err";
        String videoPath = "uploads/video.mp4";

        when(downloadVideoOutputPort.downloadVideo(eq(videoPath), any(Path.class)))
                .thenThrow(new RuntimeException("Some error"));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> processVideoUseCase.processVideo(videoId, "user-1", videoPath, "PENDING"));

        assertTrue(exception.getMessage().contains("Failed to process video: vid-err"));
    }
}
