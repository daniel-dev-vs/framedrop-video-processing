package com.framedrop.video_processing.core.domain.model.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StatusProcessTest {

    @Test
    void shouldHaveFourValues() {
        assertEquals(4, StatusProcess.values().length);
    }

    @Test
    void shouldContainPending() {
        assertEquals(StatusProcess.PENDING, StatusProcess.valueOf("PENDING"));
    }

    @Test
    void shouldContainProcessing() {
        assertEquals(StatusProcess.PROCESSING, StatusProcess.valueOf("PROCESSING"));
    }

    @Test
    void shouldContainCompleted() {
        assertEquals(StatusProcess.COMPLETED, StatusProcess.valueOf("COMPLETED"));
    }

    @Test
    void shouldContainFailed() {
        assertEquals(StatusProcess.FAILED, StatusProcess.valueOf("FAILED"));
    }

    @Test
    void shouldThrowExceptionForInvalidValue() {
        assertThrows(IllegalArgumentException.class, () -> StatusProcess.valueOf("INVALID"));
    }

    @Test
    void shouldReturnCorrectName() {
        assertEquals("PENDING", StatusProcess.PENDING.name());
        assertEquals("PROCESSING", StatusProcess.PROCESSING.name());
        assertEquals("COMPLETED", StatusProcess.COMPLETED.name());
        assertEquals("FAILED", StatusProcess.FAILED.name());
    }
}
