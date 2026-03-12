package com.framedrop.video_processing.core.domain.ports.out;

public interface UpdateVideoStatusOutputPort {
    void updateStatus(String videoId, String status);
}
