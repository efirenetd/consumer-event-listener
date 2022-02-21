package org.efire.net.handler;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;

@Component
@Slf4j
public class FailedRecordHandler {

    private KafkaTemplate kafkaTemplate;

    public FailedRecordHandler(KafkaTemplate kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void handleRecord(ConsumerRecord<Integer, String> record) {
        log.info("Inside handleRecord");
        ListenableFuture<SendResult<Integer, String>> futureResult = kafkaTemplate
                .sendDefault(record.key(), record.value());
        futureResult.addCallback(new ListenableFutureCallback<>() {
            @Override
            public void onFailure(Throwable throwable) {
                handleFailure(record.key(), record.value(), throwable);
            }

            @Override
            public void onSuccess(SendResult<Integer, String> result) {
                handleSuccess(record.key(), record.value(), result);
            }
        });
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
