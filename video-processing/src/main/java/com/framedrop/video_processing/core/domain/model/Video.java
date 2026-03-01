package com.framedrop.video_processing.core.domain.model;
import com.framedrop.video_processing.core.domain.model.enums.StatusProcess;
import java.util.Set;
public class Video {
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".mp4", ".mkv", ".webm", ".mov", ".avi");
    private String videoId;
    private String userId;
    private String videoPath;
    private String fileName;
    private String fileExtension;
    private StatusProcess statusProcess;
    public Video(String videoId, String userId, String videoPath, String fileName, StatusProcess statusProcess) {
        this.videoId = videoId;
        this.userId = userId;
        this.videoPath = videoPath;
        this.fileName = fileName;
        this.fileExtension = getFileExtensionFromFileName(fileName);
        this.statusProcess = statusProcess;
        this.validate();
    }
    public String getVideoId() { return videoId; }
    public String getUserId() { return userId; }
    public String getVideoPath() { return videoPath; }
    public String getFileName() { return fileName; }
    public String getFileExtension() { return fileExtension; }
    public StatusProcess getStatusProcess() { return statusProcess; }
    public void setStatusProcess(StatusProcess statusProcess) {
        this.statusProcess = statusProcess;
        validateStatusProcess();
    }
    private void validate() {
        if (videoId == null || videoId.isEmpty()) {
            throw new IllegalArgumentException("Video ID cannot be null or empty");
        }
        if (userId == null || userId.isEmpty()) {
            throw new IllegalArgumentException("User ID cannot be null or empty");
        }
        if (videoPath == null || videoPath.isEmpty()) {
            throw new IllegalArgumentException("Video path cannot be null or empty");
        }
        validateFileName();
        validateFileExtension();
        validateStatusProcess();
    }
    private void validateFileName() {
        if (fileName == null || fileName.isEmpty()) {
            throw new IllegalArgumentException("File name cannot be null or empty");
        }
    }
    private void validateFileExtension() {
        if (fileExtension == null || fileExtension.isEmpty()) {
            throw new IllegalArgumentException("File extension cannot be null or empty");
        }
        if (!ALLOWED_EXTENSIONS.contains(fileExtension.toLowerCase())) {
            throw new IllegalArgumentException("File extension not supported. Allowed extensions are: .mp4, .mkv, .webm, .mov, .avi");
        }
    }
    private void validateStatusProcess() {
        if (statusProcess == null) {
            throw new IllegalArgumentException("Status process cannot be null");
        }
    }
    private String getFileExtensionFromFileName(String fileName) {
        validateFileName();
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == fileName.length() - 1) {
            throw new IllegalArgumentException("File name must contain an extension");
        }
        return fileName.substring(lastDotIndex);
    }
}
