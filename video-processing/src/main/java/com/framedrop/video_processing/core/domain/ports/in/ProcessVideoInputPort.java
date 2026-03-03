package com.framedrop.video_processing.core.domain.ports.in;
public interface ProcessVideoInputPort {
    void processVideo(String videoId, String userId, String videoPath, String status);
}
