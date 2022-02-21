package org.efire.net.service;

import lombok.extern.slf4j.Slf4j;
import org.efire.net.domain.LibraryEvent;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static java.util.stream.Collectors.toMap;

@Service
@Slf4j
public class EventProducerExecutor {

    private Map<String, LibraryEventProducer> producers;

    public EventProducerExecutor(List<LibraryEventProducer> producerList) {
        this.producers = producerList.stream().collect(toMap(k -> apply(k), Function.identity()));
    }

    private static String apply(LibraryEventProducer k) {
        return k.getClass().getDeclaredAnnotation(Qualifier.class).value();
    }

    public void publishEvent(LibraryEvent libraryEvent) {
        if (StringUtils.isEmpty(libraryEvent.getTopic())) {
            log.info("Default publish event");
            producers.get("defaultProducer").sendLibraryEvent(libraryEvent);
        } else {
            log.info("Topic publish event");
            producers.get("topicProducer").sendLibraryEvent(libraryEvent);
        }
    }
}
