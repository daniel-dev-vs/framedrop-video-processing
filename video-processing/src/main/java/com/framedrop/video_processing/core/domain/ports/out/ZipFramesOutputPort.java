package com.framedrop.video_processing.core.domain.ports.out;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

public interface ZipFramesOutputPort {
    File zipFrames(List<File> frames, Path zipDir, String zipFileName);
}
