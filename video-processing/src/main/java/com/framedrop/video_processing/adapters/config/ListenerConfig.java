package com.framedrop.video_processing.adapters.config;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
@Configuration
public class ListenerConfig {
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
    @Bean
    public String queueUrl(@Value("${aws.sqs.video-processing-queue-url}") String queueUrl) {
        return queueUrl;
    }
}
