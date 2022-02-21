package org.efire.net.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.efire.net.domain.LibraryEvent;
import org.springframework.kafka.core.KafkaProducerException;
import org.springframework.kafka.core.KafkaSendCallback;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import org.springframework.util.concurrent.ListenableFuture;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.TimeZone;

@Service
@Slf4j
public class LibraryProducerServiceV2 {

    private KafkaTemplate<Integer, String> kafkaTemplate;
    private ObjectMapper objectMapper;

    public LibraryProducerServiceV2(KafkaTemplate<Integer, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public void sendLibraryEvent(LibraryEvent libraryEvent) throws JsonProcessingException {
        String payload = objectMapper.writeValueAsString(libraryEvent);

        ListenableFuture<SendResult<Integer, String>> futureResult = kafkaTemplate.sendDefault(libraryEvent.getEventId(), payload);
        /**
         * Instead of using ListenableFutureCallback, we can use KafkaSendCallback,
         * making it easier to extract the failed ProducerRecord, avoiding the need to cast the Throwable
         */
        futureResult.addCallback(new KafkaSendCallback<>() {

            @Override
            public void onSuccess(SendResult<Integer, String> result) {
                handleSuccess(libraryEvent.getEventId(), payload, result);
            }

            @Override
            public void onFailure(KafkaProducerException ex) {
                ProducerRecord<Integer, String> failed = ex.getFailedProducerRecord();
                handleFailure(failed, ex);
            }
        });
    }

    private void handleSuccess(Integer eventId, String payload, SendResult<Integer, String> result) {
        log.info("Message Sent SuccessFully for the key : {} and the value is {} , partition is {}", eventId, payload, result.getRecordMetadata().partition());
    }

    private void handleFailure(ProducerRecord<Integer, String> failed , Throwable ex) {
        LocalDateTime triggerTime =
                LocalDateTime.ofInstant(Instant.ofEpochSecond(failed.timestamp()),
                        TimeZone.getDefault().toZoneId());
        log.error("Error Sending the Message to topic {} and the exception is {} \n" +
                "Payload: \n {} \n" +
                        "Triggered Date/Time: {} \n"
                , failed.topic(), ex.getMessage(), failed.value(), triggerTime);;
        try {
            throw ex;
        } catch (Throwable throwable) {
            log.error("Error in OnFailure: {}", throwable.getMessage());
        }
    }
}
