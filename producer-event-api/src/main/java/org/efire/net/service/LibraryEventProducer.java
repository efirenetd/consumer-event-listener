package org.efire.net.service;

import org.efire.net.domain.LibraryEvent;
import org.springframework.kafka.support.SendResult;
import org.springframework.util.concurrent.ListenableFuture;

public interface LibraryEventProducer {

    ListenableFuture<SendResult<Integer, String>> sendLibraryEvent(LibraryEvent libraryEvent);
}
