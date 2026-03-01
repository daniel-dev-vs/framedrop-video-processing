package com.framedrop.video_processing.core.domain.ports.out;

import java.io.File;

public interface UploadZipOutputPort {
    void uploadZip(String destinationPath, File zipFile);
}
