package org.efire.net.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.efire.net.config.LibraryEventProperties;
import org.efire.net.domain.LibraryEvent;
import org.efire.net.expection.LibraryEventProducerException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.KafkaProducerException;
import org.springframework.kafka.core.KafkaSendCallback;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import org.springframework.util.concurrent.ListenableFuture;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.TimeZone;

@Service
@Slf4j
@Qualifier("topicProducer")
public class TopicLibraryEventProducer implements LibraryEventProducer {

    private KafkaTemplate kafkaTemplate;
    private LibraryEventProperties libraryEventProperties;
    private ObjectMapper objectMapper;

    public TopicLibraryEventProducer(KafkaTemplate kafkaTemplate, LibraryEventProperties libraryEventProperties, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.libraryEventProperties = libraryEventProperties;
        this.objectMapper = objectMapper;
    }

    @Override
    public ListenableFuture<SendResult<Integer, String>> sendLibraryEvent(LibraryEvent libraryEvent) {
        try {
            String payload = objectMapper.writeValueAsString(libraryEvent);
            ProducerRecord<Integer, String> producerRecord = buildProducerRecord(libraryEvent, payload);
            ListenableFuture<SendResult<Integer, String>> futureResult = this.kafkaTemplate.send(producerRecord);
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
            return futureResult;
        } catch (JsonProcessingException ex) {
            throw new LibraryEventProducerException(ex.getMessage());

        }
    }

    private ProducerRecord<Integer, String> buildProducerRecord(LibraryEvent libraryEvent, String payload) {
        List<RecordHeader> recordHeaders = List.of(new RecordHeader("event-source", "scanner".getBytes()));
        return new ProducerRecord(libraryEvent.getTopic(), null, libraryEvent.getEventId(), payload, recordHeaders);
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
