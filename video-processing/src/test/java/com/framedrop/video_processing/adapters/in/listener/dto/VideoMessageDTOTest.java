package com.framedrop.video_processing.adapters.in.listener.dto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VideoMessageDTOTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldCreateVideoMessageDTO() {
        VideoMessageDTO dto = new VideoMessageDTO("vid-1", "user-1", "test@email.com", "uploads/video.mp4", "PENDING");

        assertEquals("vid-1", dto.videoId());
        assertEquals("user-1", dto.userId());
        assertEquals("test@email.com", dto.email());
        assertEquals("uploads/video.mp4", dto.filePath());
        assertEquals("PENDING", dto.status());
    }

    @Test
    void shouldSerializeToJson() throws JsonProcessingException {
        VideoMessageDTO dto = new VideoMessageDTO("vid-1", "user-1", "test@email.com", "uploads/video.mp4", "PENDING");

        String json = objectMapper.writeValueAsString(dto);

        assertTrue(json.contains("\"videoId\":\"vid-1\""));
        assertTrue(json.contains("\"userId\":\"user-1\""));
        assertTrue(json.contains("\"email\":\"test@email.com\""));
        assertTrue(json.contains("\"filePath\":\"uploads/video.mp4\""));
        assertTrue(json.contains("\"status\":\"PENDING\""));
    }

    @Test
    void shouldDeserializeFromJson() throws JsonProcessingException {
        String json = """
                {
                    "videoId": "vid-1",
                    "userId": "user-1",
                    "email": "test@email.com",
                    "filePath": "uploads/video.mp4",
                    "status": "PENDING"
                }
                """;

        VideoMessageDTO dto = objectMapper.readValue(json, VideoMessageDTO.class);

        assertEquals("vid-1", dto.videoId());
        assertEquals("user-1", dto.userId());
        assertEquals("test@email.com", dto.email());
        assertEquals("uploads/video.mp4", dto.filePath());
        assertEquals("PENDING", dto.status());
    }

    @Test
    void shouldHandleNullValues() {
        VideoMessageDTO dto = new VideoMessageDTO(null, null, null, null, null);

        assertNull(dto.videoId());
        assertNull(dto.userId());
        assertNull(dto.email());
        assertNull(dto.filePath());
        assertNull(dto.status());
    }

    @Test
    void shouldImplementEquality() {
        VideoMessageDTO dto1 = new VideoMessageDTO("vid-1", "user-1", "test@email.com", "uploads/video.mp4", "PENDING");
        VideoMessageDTO dto2 = new VideoMessageDTO("vid-1", "user-1", "test@email.com", "uploads/video.mp4", "PENDING");

        assertEquals(dto1, dto2);
        assertEquals(dto1.hashCode(), dto2.hashCode());
    }

    @Test
    void shouldNotBeEqualWithDifferentValues() {
        VideoMessageDTO dto1 = new VideoMessageDTO("vid-1", "user-1", "test@email.com", "uploads/video.mp4", "PENDING");
        VideoMessageDTO dto2 = new VideoMessageDTO("vid-2", "user-2", "other@email.com", "uploads/other.mp4", "COMPLETED");

        assertNotEquals(dto1, dto2);
    }

    @Test
    void shouldHaveToString() {
        VideoMessageDTO dto = new VideoMessageDTO("vid-1", "user-1", "test@email.com", "uploads/video.mp4", "PENDING");

        String toString = dto.toString();
        assertNotNull(toString);
        assertTrue(toString.contains("vid-1"));
        assertTrue(toString.contains("user-1"));
    }
}
