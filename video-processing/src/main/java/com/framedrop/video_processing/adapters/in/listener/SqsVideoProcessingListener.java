package com.framedrop.video_processing.adapters.in.listener;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.framedrop.video_processing.adapters.in.listener.dto.VideoMessageDTO;
import com.framedrop.video_processing.core.domain.ports.in.ProcessVideoInputPort;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import java.util.List;
@Component
@RequiredArgsConstructor
public class SqsVideoProcessingListener {
    private static final Logger log = LoggerFactory.getLogger(SqsVideoProcessingListener.class);
    private final SqsClient sqsClient;
    private final ProcessVideoInputPort processVideoInputPort;
    private final ObjectMapper objectMapper;
    private final String queueUrl;
    @Scheduled(fixedDelay = 5000)
    public void pollMessages() {
        try {
            ReceiveMessageRequest receiveRequest = ReceiveMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .maxNumberOfMessages(5)
                    .waitTimeSeconds(10)
                    .build();
            List<Message> messages = sqsClient.receiveMessage(receiveRequest).messages();
            for (Message message : messages) {
                try {
                    log.info("Received SQS message: {}", message.messageId());
                    VideoMessageDTO videoMessage = objectMapper.readValue(message.body(), VideoMessageDTO.class);
                    processVideoInputPort.processVideo(
                            videoMessage.videoId(),
                            videoMessage.userId(),
                            videoMessage.filePath(),
                            videoMessage.status());
                    deleteMessage(message);
                    log.info("Successfully processed and deleted message: {}", message.messageId());
                } catch (Exception e) {
                    log.error("Error processing SQS message {}: {}", message.messageId(), e.getMessage(), e);
                }
            }
        } catch (Exception e) {
            log.error("Error polling SQS messages: {}", e.getMessage(), e);
        }
    }
    private void deleteMessage(Message message) {
        DeleteMessageRequest deleteRequest = DeleteMessageRequest.builder()
                .queueUrl(queueUrl)
                .receiptHandle(message.receiptHandle())
                .build();
        sqsClient.deleteMessage(deleteRequest);
    }
}
