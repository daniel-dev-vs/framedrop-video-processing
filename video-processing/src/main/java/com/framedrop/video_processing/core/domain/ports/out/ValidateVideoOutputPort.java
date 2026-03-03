package com.framedrop.video_processing.core.domain.ports.out;

import java.io.File;

public interface ValidateVideoOutputPort {
    boolean isValidFormatVideo(File file);
}
