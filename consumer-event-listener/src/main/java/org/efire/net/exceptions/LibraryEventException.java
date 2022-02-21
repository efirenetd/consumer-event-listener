package org.efire.net.exceptions;

import org.efire.net.entity.LibraryEvent;

public class LibraryEventException extends RuntimeException {

    private LibraryEvent libraryEvent;

    public LibraryEventException() {
        super();
    }

    public LibraryEventException(String message,LibraryEvent event) {
        super(message);
        this.libraryEvent = event;
    }

    public LibraryEvent getLibraryEvent() {
        return libraryEvent;
    }
}
