package com.framedrop.video_processing.adapters.out;
import com.framedrop.video_processing.core.domain.ports.out.ValidateVideoOutputPort;
import org.apache.tika.Tika;
import org.springframework.stereotype.Component;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;
@Component
public class ValidateVideoAdapter implements ValidateVideoOutputPort {
    private static final Tika tika = new Tika();
    private static final Set<String> ALLOWED_VIDEO_MIME_TYPES = Set.of(
            "video/mp4",
            "video/x-msvideo",      // avi
            "video/quicktime",       // mov
            "video/webm",
            "video/x-matroska",      // mkv
            "application/x-matroska" // mkv
    );
    @Override
    public boolean isValidFormatVideo(File file) {
        try {
            String detectedMimeType = tika.detect(file.toPath());
            return ALLOWED_VIDEO_MIME_TYPES.contains(detectedMimeType);
        } catch (IOException e) {
            throw new RuntimeException("Failed to detect MIME type for file: " + file.getName(), e);
        }
    }
}
