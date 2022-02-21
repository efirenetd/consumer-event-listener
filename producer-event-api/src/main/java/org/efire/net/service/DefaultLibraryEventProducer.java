package org.efire.net.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.efire.net.domain.LibraryEvent;
import org.efire.net.expection.LibraryEventProducerException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;

@Service
@Slf4j
@Qualifier("defaultProducer")
public class DefaultLibraryEventProducer implements LibraryEventProducer {

    private KafkaTemplate<Integer, String> kafkaTemplate;
    private ObjectMapper objectMapper;

    public DefaultLibraryEventProducer(KafkaTemplate<Integer, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public ListenableFuture<SendResult<Integer, String>> sendLibraryEvent(LibraryEvent libraryEvent) {
        try {
            String payload = objectMapper.writeValueAsString(libraryEvent);
            ListenableFuture<SendResult<Integer, String>> futureResult = kafkaTemplate
                    .sendDefault(libraryEvent.getEventId(), payload);
            String finalPayload = payload;
            futureResult.addCallback(new ListenableFutureCallback<>() {
                @Override
                public void onFailure(Throwable throwable) {
                    handleFailure(libraryEvent.getEventId(), finalPayload, throwable);
                }

                @Override
                public void onSuccess(SendResult<Integer, String> result) {
                    handleSuccess(libraryEvent.getEventId(), finalPayload, result);
                }
            });
            return futureResult;
        } catch (JsonProcessingException e) {
            throw new LibraryEventProducerException(e.getMessage());
        }
    }

    private void handleSuccess(Integer eventId, String payload, SendResult<Integer, String> result) {
        log.info("Message Sent SuccessFully for the key : {} and the value is {} , partition is {}", eventId, payload, result.getRecordMetadata().partition());
    }

    private void handleFailure(Integer eventId, String payload, Throwable ex) {
        log.error("Error Sending the Message and the exception is {}", ex.getMessage());
        try {
            throw ex;
        } catch (Throwable throwable) {
            log.error("Error in OnFailure: {}", throwable.getMessage());
        }
    }
}
