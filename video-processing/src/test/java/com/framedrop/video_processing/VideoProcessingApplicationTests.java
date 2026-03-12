package com.framedrop.video_processing;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class VideoProcessingApplicationTests {

	@Test
	void shouldHaveMainMethod() {
		assertDoesNotThrow(() -> VideoProcessingApplication.class.getDeclaredMethod("main", String[].class));
	}

}
