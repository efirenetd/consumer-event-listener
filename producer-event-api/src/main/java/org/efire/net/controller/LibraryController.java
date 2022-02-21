package org.efire.net.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.efire.net.config.LibraryEventProperties;
import org.efire.net.domain.LibraryEvent;
import org.efire.net.domain.LibraryEventType;
import org.efire.net.service.EventProducerExecutor;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import static org.springframework.web.servlet.support.ServletUriComponentsBuilder.fromCurrentRequest;

@RestController
@RequestMapping("/v1/libraries")
@Slf4j
public class LibraryController {

    private EventProducerExecutor eventProducerExecutor;
    private LibraryEventProperties libraryEventProperties;

    public LibraryController(EventProducerExecutor eventProducerExecutor, LibraryEventProperties libraryEventProperties) {
        this.eventProducerExecutor = eventProducerExecutor;
        this.libraryEventProperties = libraryEventProperties;
    }

    @PostMapping
    public ResponseEntity<LibraryEvent> libraryEvent(@RequestBody LibraryEvent libraryEvent) throws JsonProcessingException {
        log.info("Before post default libraryEvent");
        libraryEvent.setLibraryEventType(LibraryEventType.NEW);
        //Calls default producer implementation
        eventProducerExecutor.publishEvent(libraryEvent);

        log.info("After post default libraryEvent");
        return ResponseEntity.created(fromCurrentRequest().build().toUri())
                .body(libraryEvent);
    }

    @PutMapping
    public ResponseEntity<?> putLibraryEvent(@RequestBody LibraryEvent libraryEvent) {
        log.info("Before put default libraryEvent");
        if (libraryEvent.getEventId() == null) {
            log.warn("Library event ID should not be null");
            return ResponseEntity.badRequest().body("Please pass Library EventId");
        }
        //Calls default producer implementation
        libraryEvent.setLibraryEventType(LibraryEventType.UPDATED);
        eventProducerExecutor.publishEvent(libraryEvent);

        log.info("After put default libraryEvent");
        return ResponseEntity.ok().body(libraryEvent);
    }

    @PostMapping("/{eventPath}")
    public ResponseEntity<LibraryEvent> libraryEvent(@PathVariable String eventPath,
                                                     @RequestBody LibraryEvent libraryEvent) throws JsonProcessingException {
        log.info("Before sendLibraryEvent");
        log.info("Event: {} ", eventPath);
        String topic = libraryEventProperties.getTopics().get(eventPath);
        if (StringUtils.isEmpty(topic)) {
            log.error("Event URI is not recognized.");
            return ResponseEntity.badRequest().body(libraryEvent);
        }

        log.info("Message will be post to Topic: {}", topic);
        libraryEvent.setTopic(topic);
        libraryEvent.setLibraryEventType(LibraryEventType.NEW);

        eventProducerExecutor.publishEvent(libraryEvent);

        log.info("After sendLibraryEvent");
        return ResponseEntity.created(fromCurrentRequest().build().toUri())
                .body(libraryEvent);
    }


}
