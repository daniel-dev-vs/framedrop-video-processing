package com.framedrop.video_processing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class VideoProcessingApplication {

	public static void main(String[] args) {
		SpringApplication.run(VideoProcessingApplication.class, args);
	}

}
