package org.efire.net.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;

@Getter
@Setter
@Builder
public class LibraryEvent {
    private Integer eventId;
    private Book book;
    private LibraryEventType libraryEventType;
    @JsonIgnore
    private String topic;
}

