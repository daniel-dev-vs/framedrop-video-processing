package com.framedrop.video_processing.core.application.usecases;
import com.framedrop.video_processing.core.domain.model.Video;
import com.framedrop.video_processing.core.domain.model.enums.StatusProcess;
import com.framedrop.video_processing.core.domain.ports.in.ProcessVideoInputPort;
import com.framedrop.video_processing.core.domain.ports.out.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
public class ProcessVideoUseCase implements ProcessVideoInputPort {
    private static final Logger log = LoggerFactory.getLogger(ProcessVideoUseCase.class);
    private final DownloadVideoOutputPort downloadVideoOutputPort;
    private final ValidateVideoOutputPort validateVideoOutputPort;
    private final ExtractFramesOutputPort extractFramesOutputPort;
    private final ZipFramesOutputPort zipFramesOutputPort;
    private final UploadZipOutputPort uploadZipOutputPort;
    private final UpdateVideoStatusOutputPort updateVideoStatusOutputPort;
    private final String baseDir;
    public ProcessVideoUseCase(DownloadVideoOutputPort downloadVideoOutputPort,
                               ValidateVideoOutputPort validateVideoOutputPort,
                               ExtractFramesOutputPort extractFramesOutputPort,
                               ZipFramesOutputPort zipFramesOutputPort,
                               UploadZipOutputPort uploadZipOutputPort,
                               UpdateVideoStatusOutputPort updateVideoStatusOutputPort,
                               String baseDir) {
        this.downloadVideoOutputPort = downloadVideoOutputPort;
        this.validateVideoOutputPort = validateVideoOutputPort;
        this.extractFramesOutputPort = extractFramesOutputPort;
        this.zipFramesOutputPort = zipFramesOutputPort;
        this.uploadZipOutputPort = uploadZipOutputPort;
        this.updateVideoStatusOutputPort = updateVideoStatusOutputPort;
        this.baseDir = baseDir;
    }
    @Override
    public void processVideo(String videoId, String userId, String videoPath, String status) {
        Path processDir = Path.of(baseDir, videoId);
        try {
            String fileName = extractFileName(videoPath);
            Video video = new Video(videoId, userId, videoPath, fileName, StatusProcess.PROCESSING);

            updateVideoStatusOutputPort.updateStatus(videoId, StatusProcess.PROCESSING.name());
            log.info("Starting video processing for videoId={}", videoId);
            Path videoDir = processDir.resolve("video");
            Path framesDir = processDir.resolve("frames");
            Path zipDir = processDir.resolve("zip");
            Files.createDirectories(videoDir);
            Files.createDirectories(framesDir);
            Files.createDirectories(zipDir);
            log.info("Downloading video from S3: {}", videoPath);
            File videoFile = downloadVideoOutputPort.downloadVideo(videoPath, videoDir);
            log.info("Validating video format: {}", videoFile.getName());
            if (!validateVideoOutputPort.isValidFormatVideo(videoFile)) {
                throw new IllegalArgumentException("Invalid video format for file: " + videoFile.getName());
            }
            log.info("Extracting frames from video: {}", videoFile.getName());
            List<File> frames = extractFramesOutputPort.extractFrames(videoFile, framesDir);
            log.info("Extracted {} frames", frames.size());
            String zipFileName = buildZipFileName(fileName);
            log.info("Zipping frames into: {}", zipFileName);
            File zipFile = zipFramesOutputPort.zipFrames(frames, zipDir, zipFileName);
            String zipDestinationPath = "processed/" + userId + "/" + videoId + "_" + zipFileName;
            log.info("Uploading zip to S3: {}", zipDestinationPath);
            uploadZipOutputPort.uploadZip(zipDestinationPath, zipFile);
            video.setStatusProcess(StatusProcess.COMPLETED);

            updateVideoStatusOutputPort.updateStatus(videoId, StatusProcess.COMPLETED.name());
            log.info("Video processing completed for videoId={}", videoId);
        } catch (Exception e) {
            log.error("Failed to process video videoId={}: {}", videoId, e.getMessage(), e);
            updateVideoStatusOutputPort.updateStatus(videoId, StatusProcess.FAILED.name());
            throw new RuntimeException("Failed to process video: " + videoId, e);
        } finally {
            cleanUpProcessDir(processDir);
        }
    }
    private String extractFileName(String videoPath) {
        int lastSlash = videoPath.lastIndexOf('/');
        return lastSlash >= 0 ? videoPath.substring(lastSlash + 1) : videoPath;
    }
    private String buildZipFileName(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        String baseName = lastDot > 0 ? fileName.substring(0, lastDot) : fileName;
        return baseName + "_frames.zip";
    }
    private void cleanUpProcessDir(Path processDir) {
        try {
            if (Files.exists(processDir)) {
                Files.walk(processDir)
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
                log.info("Cleaned up processing directory: {}", processDir);
            }
        } catch (Exception e) {
            log.warn("Failed to clean up processing directory: {}", processDir, e);
        }
    }
}
