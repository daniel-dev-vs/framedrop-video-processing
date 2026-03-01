package com.framedrop.video_processing.core.domain.ports.out;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

public interface ExtractFramesOutputPort {
    List<File> extractFrames(File videoFile, Path framesDir);
}
