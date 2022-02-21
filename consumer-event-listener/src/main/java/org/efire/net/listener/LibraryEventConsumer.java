package org.efire.net.listener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.efire.net.entity.LibraryEvent;
import org.efire.net.service.LibraryEventService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class LibraryEventConsumer {

    private LibraryEventService libraryEventService;
    private ObjectMapper objectMapper;

    public LibraryEventConsumer(LibraryEventService libraryEventService, ObjectMapper objectMapper) {
        this.libraryEventService = libraryEventService;
        this.objectMapper = objectMapper;
    }

    //@KafkaListener(topics = {"LIBRARY-TOPIC", "LIBRARY-TOPIC-ONE", "LIBRARY-TOPIC-TWO"})
    @KafkaListener(topics = {"LIBRARY-SECURED-TOPIC"})
    public void onMessage(ConsumerRecord<Integer, String> consumerRecord) throws JsonProcessingException {
        log.info(">>> Received Payload : {}", consumerRecord);
        LibraryEvent libraryEvent = objectMapper.readValue(consumerRecord.value(), LibraryEvent.class);
        libraryEventService.processLibraryEvent(libraryEvent);
    }

}
