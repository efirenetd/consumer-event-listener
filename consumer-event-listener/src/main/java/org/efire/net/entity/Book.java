package org.efire.net.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class Book {
    @Id
    private Integer id;
    private String name;
    private String author;
    @OneToOne
    @JoinColumn(name = "eventId")
    private LibraryEvent libraryEvent;
}
