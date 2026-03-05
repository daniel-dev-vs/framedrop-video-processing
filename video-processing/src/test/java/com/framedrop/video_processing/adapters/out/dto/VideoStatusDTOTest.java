package com.framedrop.video_processing.adapters.out.dto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VideoStatusDTOTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldCreateVideoStatusDTO() {
        VideoStatusDTO dto = new VideoStatusDTO("PROCESSING");
        assertEquals("PROCESSING", dto.status());
    }

    @Test
    void shouldSerializeToJson() throws JsonProcessingException {
        VideoStatusDTO dto = new VideoStatusDTO("COMPLETED");
        String json = objectMapper.writeValueAsString(dto);

        assertTrue(json.contains("\"status\":\"COMPLETED\""));
    }

    @Test
    void shouldDeserializeFromJson() throws JsonProcessingException {
        String json = """
                {"status": "FAILED"}
                """;
        VideoStatusDTO dto = objectMapper.readValue(json, VideoStatusDTO.class);

        assertEquals("FAILED", dto.status());
    }

    @Test
    void shouldHandleNullStatus() {
        VideoStatusDTO dto = new VideoStatusDTO(null);
        assertNull(dto.status());
    }

    @Test
    void shouldImplementEquality() {
        VideoStatusDTO dto1 = new VideoStatusDTO("PROCESSING");
        VideoStatusDTO dto2 = new VideoStatusDTO("PROCESSING");

        assertEquals(dto1, dto2);
        assertEquals(dto1.hashCode(), dto2.hashCode());
    }

    @Test
    void shouldNotBeEqualWithDifferentStatus() {
        VideoStatusDTO dto1 = new VideoStatusDTO("PROCESSING");
        VideoStatusDTO dto2 = new VideoStatusDTO("COMPLETED");

        assertNotEquals(dto1, dto2);
    }
}
