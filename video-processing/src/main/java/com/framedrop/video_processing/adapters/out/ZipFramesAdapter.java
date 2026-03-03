package com.framedrop.video_processing.adapters.out;

import com.framedrop.video_processing.core.domain.ports.out.ZipFramesOutputPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Component
public class ZipFramesAdapter implements ZipFramesOutputPort {

    private static final Logger log = LoggerFactory.getLogger(ZipFramesAdapter.class);

    @Override
    public File zipFrames(List<File> frames, Path zipDir, String zipFileName) {
        File zipFile = zipDir.resolve(zipFileName).toFile();

        try (FileOutputStream fos = new FileOutputStream(zipFile);
             ZipOutputStream zos = new ZipOutputStream(fos)) {

            for (File frame : frames) {
                ZipEntry entry = new ZipEntry(frame.getName());
                zos.putNextEntry(entry);

                try (FileInputStream fis = new FileInputStream(frame)) {
                    fis.transferTo(zos);
                }

                zos.closeEntry();
            }

            log.info("Created zip file with {} frames: {}", frames.size(), zipFile.getAbsolutePath());

        } catch (IOException e) {
            throw new RuntimeException("Failed to create zip file: " + zipFileName, e);
        }

        return zipFile;
    }
}
