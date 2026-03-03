package com.framedrop.video_processing.adapters.out;

import com.framedrop.video_processing.adapters.out.dto.VideoStatusDTO;
import com.framedrop.video_processing.core.domain.ports.out.UpdateVideoStatusOutputPort;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClient;

@RequiredArgsConstructor
public class UpdateVideoStatusApiAdapter implements UpdateVideoStatusOutputPort {

    private static final Logger log = LoggerFactory.getLogger(UpdateVideoStatusApiAdapter.class);

    private final RestClient restClient;

    @Override
    public void updateStatus(String videoId, String status) {
        log.info("Updating video status for videoId={} to {}", videoId, status);

        restClient.patch()
                .uri("/api/videos/{id}", videoId)
                .body(new VideoStatusDTO(status))
                .retrieve()
                .toBodilessEntity();

        log.info("Video status updated successfully for videoId={}", videoId);
    }
}
