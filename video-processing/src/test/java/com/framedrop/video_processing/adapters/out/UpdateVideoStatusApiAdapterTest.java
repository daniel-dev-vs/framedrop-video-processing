package com.framedrop.video_processing.adapters.out;

import com.framedrop.video_processing.adapters.out.dto.VideoStatusDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UpdateVideoStatusApiAdapterTest {

    @Mock
    private RestClient restClient;

    @Mock
    private RestClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private RestClient.RequestBodySpec requestBodySpec;

    @Mock
    private RestClient.ResponseSpec responseSpec;

    private UpdateVideoStatusApiAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new UpdateVideoStatusApiAdapter(restClient);
    }

    @Test
    void shouldUpdateStatusSuccessfully() {
        when(restClient.patch()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(eq("/api/videos/{id}"), eq("vid-123"))).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any(VideoStatusDTO.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toBodilessEntity()).thenReturn(null);

        assertDoesNotThrow(() -> adapter.updateStatus("vid-123", "PROCESSING"));

        verify(restClient).patch();
        verify(requestBodyUriSpec).uri("/api/videos/{id}", "vid-123");
    }

    @Test
    void shouldSendCorrectStatusInBody() {
        when(restClient.patch()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(eq("/api/videos/{id}"), eq("vid-1"))).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any(VideoStatusDTO.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toBodilessEntity()).thenReturn(null);

        adapter.updateStatus("vid-1", "COMPLETED");

        ArgumentCaptor<VideoStatusDTO> captor = ArgumentCaptor.forClass(VideoStatusDTO.class);
        verify(requestBodySpec).body(captor.capture());

        assertEquals("COMPLETED", captor.getValue().status());
    }

    @Test
    void shouldThrowExceptionWhenApiCallFails() {
        when(restClient.patch()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(eq("/api/videos/{id}"), eq("vid-1"))).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any(VideoStatusDTO.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenThrow(new RuntimeException("API error"));

        assertThrows(RuntimeException.class,
                () -> adapter.updateStatus("vid-1", "FAILED"));
    }

    @Test
    void shouldCallPatchWithCorrectUri() {
        when(restClient.patch()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(eq("/api/videos/{id}"), eq("video-abc"))).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any(VideoStatusDTO.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toBodilessEntity()).thenReturn(null);

        adapter.updateStatus("video-abc", "PROCESSING");

        verify(requestBodyUriSpec).uri("/api/videos/{id}", "video-abc");
    }
}
