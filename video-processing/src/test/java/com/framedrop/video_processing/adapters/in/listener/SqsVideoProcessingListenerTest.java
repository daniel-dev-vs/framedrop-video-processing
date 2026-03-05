package com.framedrop.video_processing.adapters.in.listener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.framedrop.video_processing.adapters.in.listener.dto.VideoMessageDTO;
import com.framedrop.video_processing.core.domain.ports.in.ProcessVideoInputPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SqsVideoProcessingListenerTest {

    @Mock
    private SqsClient sqsClient;

    @Mock
    private ProcessVideoInputPort processVideoInputPort;

    private ObjectMapper objectMapper;

    private final String queueUrl = "https://sqs.us-east-1.amazonaws.com/123456789/test-queue";

    private SqsVideoProcessingListener listener;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        listener = new SqsVideoProcessingListener(sqsClient, processVideoInputPort, objectMapper, queueUrl);
    }

    @Test
    void shouldPollAndProcessMessageSuccessfully() throws JsonProcessingException {
        VideoMessageDTO dto = new VideoMessageDTO("vid-1", "user-1", "test@email.com", "uploads/video.mp4", "PENDING");
        String messageBody = objectMapper.writeValueAsString(dto);

        Message message = Message.builder()
                .messageId("msg-1")
                .body(messageBody)
                .receiptHandle("receipt-1")
                .build();

        ReceiveMessageResponse response = ReceiveMessageResponse.builder()
                .messages(List.of(message))
                .build();

        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class))).thenReturn(response);

        listener.pollMessages();

        verify(processVideoInputPort).processVideo("vid-1", "user-1", "uploads/video.mp4", "PENDING");
        verify(sqsClient).deleteMessage(any(DeleteMessageRequest.class));
    }

    @Test
    void shouldDeleteMessageAfterSuccessfulProcessing() throws JsonProcessingException {
        VideoMessageDTO dto = new VideoMessageDTO("vid-1", "user-1", "test@email.com", "uploads/video.mp4", "PENDING");
        String messageBody = objectMapper.writeValueAsString(dto);

        Message message = Message.builder()
                .messageId("msg-1")
                .body(messageBody)
                .receiptHandle("receipt-handle-123")
                .build();

        ReceiveMessageResponse response = ReceiveMessageResponse.builder()
                .messages(List.of(message))
                .build();

        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class))).thenReturn(response);

        listener.pollMessages();

        ArgumentCaptor<DeleteMessageRequest> captor = ArgumentCaptor.forClass(DeleteMessageRequest.class);
        verify(sqsClient).deleteMessage(captor.capture());

        DeleteMessageRequest deleteRequest = captor.getValue();
        assertEquals(queueUrl, deleteRequest.queueUrl());
        assertEquals("receipt-handle-123", deleteRequest.receiptHandle());
    }

    @Test
    void shouldHandleEmptyMessageList() {
        ReceiveMessageResponse response = ReceiveMessageResponse.builder()
                .messages(Collections.emptyList())
                .build();

        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class))).thenReturn(response);

        assertDoesNotThrow(() -> listener.pollMessages());

        verify(processVideoInputPort, never()).processVideo(any(), any(), any(), any());
        verify(sqsClient, never()).deleteMessage(any(DeleteMessageRequest.class));
    }

    @Test
    void shouldNotDeleteMessageWhenProcessingFails() throws JsonProcessingException {
        VideoMessageDTO dto = new VideoMessageDTO("vid-1", "user-1", "test@email.com", "uploads/video.mp4", "PENDING");
        String messageBody = objectMapper.writeValueAsString(dto);

        Message message = Message.builder()
                .messageId("msg-1")
                .body(messageBody)
                .receiptHandle("receipt-1")
                .build();

        ReceiveMessageResponse response = ReceiveMessageResponse.builder()
                .messages(List.of(message))
                .build();

        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class))).thenReturn(response);
        doThrow(new RuntimeException("Processing failed")).when(processVideoInputPort)
                .processVideo(any(), any(), any(), any());

        assertDoesNotThrow(() -> listener.pollMessages());

        verify(sqsClient, never()).deleteMessage(any(DeleteMessageRequest.class));
    }

    @Test
    void shouldHandleInvalidJsonMessage() {
        Message message = Message.builder()
                .messageId("msg-1")
                .body("invalid json body")
                .receiptHandle("receipt-1")
                .build();

        ReceiveMessageResponse response = ReceiveMessageResponse.builder()
                .messages(List.of(message))
                .build();

        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class))).thenReturn(response);

        assertDoesNotThrow(() -> listener.pollMessages());

        verify(processVideoInputPort, never()).processVideo(any(), any(), any(), any());
        verify(sqsClient, never()).deleteMessage(any(DeleteMessageRequest.class));
    }

    @Test
    void shouldHandleSqsClientExceptionOnPoll() {
        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
                .thenThrow(new RuntimeException("SQS connection error"));

        assertDoesNotThrow(() -> listener.pollMessages());

        verify(processVideoInputPort, never()).processVideo(any(), any(), any(), any());
    }

    @Test
    void shouldProcessMultipleMessagesInBatch() throws JsonProcessingException {
        VideoMessageDTO dto1 = new VideoMessageDTO("vid-1", "user-1", "a@b.com", "uploads/v1.mp4", "PENDING");
        VideoMessageDTO dto2 = new VideoMessageDTO("vid-2", "user-2", "c@d.com", "uploads/v2.mp4", "PENDING");

        Message message1 = Message.builder()
                .messageId("msg-1")
                .body(objectMapper.writeValueAsString(dto1))
                .receiptHandle("receipt-1")
                .build();

        Message message2 = Message.builder()
                .messageId("msg-2")
                .body(objectMapper.writeValueAsString(dto2))
                .receiptHandle("receipt-2")
                .build();

        ReceiveMessageResponse response = ReceiveMessageResponse.builder()
                .messages(List.of(message1, message2))
                .build();

        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class))).thenReturn(response);

        listener.pollMessages();

        verify(processVideoInputPort).processVideo("vid-1", "user-1", "uploads/v1.mp4", "PENDING");
        verify(processVideoInputPort).processVideo("vid-2", "user-2", "uploads/v2.mp4", "PENDING");
        verify(sqsClient, times(2)).deleteMessage(any(DeleteMessageRequest.class));
    }

    @Test
    void shouldContinueProcessingAfterOneMessageFails() throws JsonProcessingException {
        VideoMessageDTO dto1 = new VideoMessageDTO("vid-1", "user-1", "a@b.com", "uploads/v1.mp4", "PENDING");
        VideoMessageDTO dto2 = new VideoMessageDTO("vid-2", "user-2", "c@d.com", "uploads/v2.mp4", "PENDING");

        Message message1 = Message.builder()
                .messageId("msg-1")
                .body(objectMapper.writeValueAsString(dto1))
                .receiptHandle("receipt-1")
                .build();

        Message message2 = Message.builder()
                .messageId("msg-2")
                .body(objectMapper.writeValueAsString(dto2))
                .receiptHandle("receipt-2")
                .build();

        ReceiveMessageResponse response = ReceiveMessageResponse.builder()
                .messages(List.of(message1, message2))
                .build();

        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class))).thenReturn(response);
        doThrow(new RuntimeException("Failed")).when(processVideoInputPort)
                .processVideo("vid-1", "user-1", "uploads/v1.mp4", "PENDING");

        listener.pollMessages();

        verify(processVideoInputPort).processVideo("vid-2", "user-2", "uploads/v2.mp4", "PENDING");
        verify(sqsClient, times(1)).deleteMessage(any(DeleteMessageRequest.class));
    }

    @Test
    void shouldSendReceiveRequestWithCorrectParameters() {
        ReceiveMessageResponse response = ReceiveMessageResponse.builder()
                .messages(Collections.emptyList())
                .build();

        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class))).thenReturn(response);

        listener.pollMessages();

        ArgumentCaptor<ReceiveMessageRequest> captor = ArgumentCaptor.forClass(ReceiveMessageRequest.class);
        verify(sqsClient).receiveMessage(captor.capture());

        ReceiveMessageRequest request = captor.getValue();
        assertEquals(queueUrl, request.queueUrl());
        assertEquals(5, request.maxNumberOfMessages());
        assertEquals(10, request.waitTimeSeconds());
    }
}
