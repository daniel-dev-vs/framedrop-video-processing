package com.framedrop.video_processing.adapters.out;
import com.framedrop.video_processing.core.domain.ports.out.ExtractFramesOutputPort;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
@Component
public class ExtractFramesAdapter implements ExtractFramesOutputPort {
    private static final Logger log = LoggerFactory.getLogger(ExtractFramesAdapter.class);
    @Override
    public List<File> extractFrames(File videoFile, Path framesDir) {
        List<File> frames = new ArrayList<>();
        try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(videoFile);
             Java2DFrameConverter converter = new Java2DFrameConverter()) {
            grabber.start();
            int totalFrames = grabber.getLengthInFrames();
            log.info("Video has {} total frames", totalFrames);
            int frameNumber = 0;
            Frame frame;
            while ((frame = grabber.grabImage()) != null) {
                BufferedImage image = converter.convert(frame);
                if (image != null) {
                    String frameName = String.format("frame_%06d.png", frameNumber);
                    File frameFile = framesDir.resolve(frameName).toFile();
                    ImageIO.write(image, "png", frameFile);
                    frames.add(frameFile);
                    frameNumber++;
                }
            }
            grabber.stop();
            log.info("Extracted {} image frames", frameNumber);
        } catch (Exception e) {
            throw new RuntimeException("Failed to extract frames from video: " + videoFile.getName(), e);
        }
        return frames;
    }
}
