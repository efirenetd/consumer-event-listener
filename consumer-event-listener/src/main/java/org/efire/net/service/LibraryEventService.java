package org.efire.net.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.efire.net.entity.LibraryEvent;
import org.efire.net.entity.LibraryEventType;
import org.efire.net.exceptions.LibraryEventException;
import org.efire.net.repository.LibraryEventRepository;
import org.springframework.dao.RecoverableDataAccessException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;

import java.util.Optional;

@Service
@Slf4j
public class LibraryEventService {

    private LibraryEventRepository libraryEventRepository;
    private KafkaTemplate kafkaTemplate;

    public LibraryEventService(LibraryEventRepository libraryEventRepository, KafkaTemplate kafkaTemplate) {
        this.libraryEventRepository = libraryEventRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    public void processLibraryEvent(LibraryEvent libraryEvent) {

        //To demonstrate a recoverable scenario to trigger a retry
        if (libraryEvent.getEventId() != null && libraryEvent.getEventId() == 999) {
            throw new RecoverableDataAccessException("Temporary network issue!!!");
        }

        //To demonstrate a recoverable scenario including libraryEvent payload
        if (libraryEvent.getEventId() != null && libraryEvent.getEventId() == 666) {
            throw new LibraryEventException("Failed payload", libraryEvent );
        }

        if (libraryEvent.getLibraryEventType().equals(LibraryEventType.NEW)) {
            libraryEvent.getBook().setLibraryEvent(libraryEvent);
            libraryEventRepository.save(libraryEvent);
            log.info("Successfully Persisted New Library Event");
        } else {
            if  (libraryEvent.getEventId() == null) {
                throw new IllegalArgumentException("Event id should not be null");
            }
            log.info("Get Library Event by Id: {}", libraryEvent.getEventId());
            Optional<LibraryEvent> byId = libraryEventRepository.findById(libraryEvent.getEventId());
            if(byId.isPresent()) {
                doUpdate(byId.get(), libraryEvent);
            } else {
                throw new IllegalArgumentException("Library Event does not exist");
            }
        }
    }

    private void doUpdate(LibraryEvent libraryEvent, LibraryEvent updatedLibraryEvent) {
        log.info("Modifying Library Event info...");
        libraryEvent.getBook().setAuthor(updatedLibraryEvent.getBook().getAuthor());
        libraryEvent.getBook().setName(updatedLibraryEvent.getBook().getName());
        libraryEvent.setLibraryEventType(updatedLibraryEvent.getLibraryEventType());
        libraryEventRepository.save(libraryEvent);
        log.info("Successfully Persisted UPDATED Library Event");
    }

    public void handleFailedRecord(ConsumerRecord<Integer, String> record)  {
        log.info("Inside handleFailedRecord");
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
