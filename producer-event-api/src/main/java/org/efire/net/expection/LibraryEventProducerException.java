package org.efire.net.expection;

public class LibraryEventProducerException extends RuntimeException {

    public LibraryEventProducerException() {
        super();
    }

    public LibraryEventProducerException(String message) {
        super(message);
    }

    public LibraryEventProducerException(String message, Throwable cause) {
        super(message, cause);
    }
}
