package com.framedrop.video_processing.core.domain.ports.out;

import java.io.File;
import java.nio.file.Path;

public interface DownloadVideoOutputPort {
    File downloadVideo(String videoPath, Path destinationDir);
}
