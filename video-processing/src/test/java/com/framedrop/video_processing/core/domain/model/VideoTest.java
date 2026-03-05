package com.framedrop.video_processing.core.domain.model;

import com.framedrop.video_processing.core.domain.model.enums.StatusProcess;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class VideoTest {

    @Test
    void shouldCreateVideoWithValidParameters() {
        Video video = new Video("vid-1", "user-1", "/path/to/video.mp4", "video.mp4", StatusProcess.PENDING);

        assertEquals("vid-1", video.getVideoId());
        assertEquals("user-1", video.getUserId());
        assertEquals("/path/to/video.mp4", video.getVideoPath());
        assertEquals("video.mp4", video.getFileName());
        assertEquals(".mp4", video.getFileExtension());
        assertEquals(StatusProcess.PENDING, video.getStatusProcess());
    }

    @ParameterizedTest
    @ValueSource(strings = {"video.mp4", "video.mkv", "video.webm", "video.mov", "video.avi"})
    void shouldCreateVideoWithAllAllowedExtensions(String fileName) {
        Video video = new Video("vid-1", "user-1", "/path/" + fileName, fileName, StatusProcess.PENDING);

        assertNotNull(video);
        assertEquals(fileName, video.getFileName());
    }

    @ParameterizedTest
    @ValueSource(strings = {"video.MP4", "video.MKV", "video.WEBM", "video.MOV", "video.AVI"})
    void shouldCreateVideoWithUppercaseExtensions(String fileName) {
        Video video = new Video("vid-1", "user-1", "/path/" + fileName, fileName, StatusProcess.PENDING);

        assertNotNull(video);
    }

    @Test
    void shouldThrowExceptionWhenVideoIdIsNull() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> new Video(null, "user-1", "/path/video.mp4", "video.mp4", StatusProcess.PENDING));

        assertEquals("Video ID cannot be null or empty", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenVideoIdIsEmpty() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> new Video("", "user-1", "/path/video.mp4", "video.mp4", StatusProcess.PENDING));

        assertEquals("Video ID cannot be null or empty", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenUserIdIsNull() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> new Video("vid-1", null, "/path/video.mp4", "video.mp4", StatusProcess.PENDING));

        assertEquals("User ID cannot be null or empty", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenUserIdIsEmpty() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> new Video("vid-1", "", "/path/video.mp4", "video.mp4", StatusProcess.PENDING));

        assertEquals("User ID cannot be null or empty", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenVideoPathIsNull() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> new Video("vid-1", "user-1", null, "video.mp4", StatusProcess.PENDING));

        assertEquals("Video path cannot be null or empty", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenVideoPathIsEmpty() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> new Video("vid-1", "user-1", "", "video.mp4", StatusProcess.PENDING));

        assertEquals("Video path cannot be null or empty", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenFileNameIsNull() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> new Video("vid-1", "user-1", "/path/video.mp4", null, StatusProcess.PENDING));

        assertEquals("File name cannot be null or empty", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenFileNameIsEmpty() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> new Video("vid-1", "user-1", "/path/video.mp4", "", StatusProcess.PENDING));

        assertEquals("File name cannot be null or empty", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenFileNameHasNoExtension() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> new Video("vid-1", "user-1", "/path/video", "video", StatusProcess.PENDING));

        assertEquals("File name must contain an extension", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenFileNameEndsWithDot() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> new Video("vid-1", "user-1", "/path/video.", "video.", StatusProcess.PENDING));

        assertEquals("File name must contain an extension", exception.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {"video.txt", "video.pdf", "video.jpg", "video.png", "video.gif", "video.exe"})
    void shouldThrowExceptionForUnsupportedExtension(String fileName) {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> new Video("vid-1", "user-1", "/path/" + fileName, fileName, StatusProcess.PENDING));

        assertEquals("File extension not supported. Allowed extensions are: .mp4, .mkv, .webm, .mov, .avi",
                exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenStatusProcessIsNull() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> new Video("vid-1", "user-1", "/path/video.mp4", "video.mp4", null));

        assertEquals("Status process cannot be null", exception.getMessage());
    }

    @Test
    void shouldSetStatusProcessSuccessfully() {
        Video video = new Video("vid-1", "user-1", "/path/video.mp4", "video.mp4", StatusProcess.PENDING);

        video.setStatusProcess(StatusProcess.PROCESSING);
        assertEquals(StatusProcess.PROCESSING, video.getStatusProcess());

        video.setStatusProcess(StatusProcess.COMPLETED);
        assertEquals(StatusProcess.COMPLETED, video.getStatusProcess());

        video.setStatusProcess(StatusProcess.FAILED);
        assertEquals(StatusProcess.FAILED, video.getStatusProcess());
    }

    @Test
    void shouldThrowExceptionWhenSettingNullStatusProcess() {
        Video video = new Video("vid-1", "user-1", "/path/video.mp4", "video.mp4", StatusProcess.PENDING);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> video.setStatusProcess(null));

        assertEquals("Status process cannot be null", exception.getMessage());
    }

    @Test
    void shouldExtractCorrectFileExtension() {
        Video video = new Video("vid-1", "user-1", "/path/my.video.file.mp4", "my.video.file.mp4", StatusProcess.PENDING);

        assertEquals(".mp4", video.getFileExtension());
    }

    @Test
    void shouldCreateVideoWithAllStatusValues() {
        for (StatusProcess status : StatusProcess.values()) {
            Video video = new Video("vid-1", "user-1", "/path/video.mp4", "video.mp4", status);
            assertEquals(status, video.getStatusProcess());
        }
    }
}
