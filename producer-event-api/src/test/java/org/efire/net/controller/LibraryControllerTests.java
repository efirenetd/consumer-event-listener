package org.efire.net.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.efire.net.config.LibraryEventProperties;
import org.efire.net.domain.Book;
import org.efire.net.domain.LibraryEvent;
import org.efire.net.service.EventProducerExecutor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

@WebMvcTest(LibraryController.class)
public class LibraryControllerTests {

    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private EventProducerExecutor eventProducerExecutor;
    @MockBean
    private LibraryEventProperties libraryEventProperties;

    @BeforeEach
    void setUp() {
        Map<String, String> topics = new HashMap<>();
        topics.put("eventOne", "LIBRARY-TOPIC-ONE");
        topics.put("eventTwo", "LIBRARY-TOPIC-TWO");
        when(libraryEventProperties.getTopics()).thenReturn(topics);
    }

    @Test
    public void testDefaultLibraryEvent_expectCreated() throws Exception {
        Book aBook = Book.builder()
                .id(null)
                .author("Jong Tenerife")
                .name("The Book")
                .build();
        LibraryEvent libraryEvent = LibraryEvent.builder()
                .eventId(null)
                .book(aBook)
                .build();
        ObjectMapper om = new ObjectMapper();
        String payload = om.writeValueAsString(libraryEvent);
        mockMvc.perform(post("/v1/libraries")
                .content(payload)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isCreated());
    }

    @Test
    public void testUpdateLibraryEvent_expectOk() throws Exception {
        Book aBook = Book.builder()
                .id(null)
                .author("Jong Tenerife")
                .name("Kafka")
                .build();
        LibraryEvent libraryEvent = LibraryEvent.builder()
                .eventId(1)
                .book(aBook)
                .build();
        ObjectMapper om = new ObjectMapper();
        String payload = om.writeValueAsString(libraryEvent);
        mockMvc.perform(put("/v1/libraries")
                .content(payload)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    public void testUpdateLibraryEvent_withNullLibraryEventId_expectBadRequest() throws Exception {
        Book aBook = Book.builder()
                .id(null)
                .author("Jong Tenerife")
                .name("Kafka Null")
                .build();
        LibraryEvent libraryEvent = LibraryEvent.builder()
                .eventId(null)
                .book(aBook)
                .build();
        ObjectMapper om = new ObjectMapper();
        String payload = om.writeValueAsString(libraryEvent);
        mockMvc.perform(put("/v1/libraries")
                .content(payload)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.content().string("Please pass Library EventId"))
                .andExpect(MockMvcResultMatchers.status().isBadRequest());
    }

    @Test
    public void testTopicOneLibraryEvent() throws Exception {
        Book aBook = Book.builder()
                .id(null)
                .author("Deriq Tenerife")
                .name("Animaniac")
                .build();
        LibraryEvent libraryEvent = LibraryEvent.builder()
                .eventId(null)
                .book(aBook)
                .build();
        ObjectMapper om = new ObjectMapper();
        String payload = om.writeValueAsString(libraryEvent);

        mockMvc.perform(post("/v1/libraries/eventOne")
                .content(payload)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isCreated());;
    }

    @Test
    public void testTopicTwoLibraryEvent() throws Exception {
        Book aBook = Book.builder()
                .id(null)
                .author("Racqel Tenerife")
                .name("Grey Anatomy")
                .build();
        LibraryEvent libraryEvent = LibraryEvent.builder()
                .eventId(null)
                .book(aBook)
                .build();
        ObjectMapper om = new ObjectMapper();
        String payload = om.writeValueAsString(libraryEvent);

        mockMvc.perform(post("/v1/libraries/eventTwo")
                .content(payload)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isCreated());
    }

    @Test
    public void testLibraryEvent_4xx() throws Exception {
        ObjectMapper om = new ObjectMapper();
        final LibraryEvent libraryEvent = LibraryEvent.builder().build();
        String payload = om.writeValueAsString(libraryEvent);
        mockMvc.perform(post("/v1/libraries/wrongURI")
                .content(payload)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isBadRequest());
    }

    @Test
    public void testBookValidation() throws Exception {
        LibraryEvent libraryEvent = LibraryEvent.builder()
                .eventId(null)
                .book(null)
                .build();
        ObjectMapper om = new ObjectMapper();
        String payload = om.writeValueAsString(libraryEvent);
        mockMvc.perform(post("/v1/libraries")
                .content(payload)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isCreated());
    }
}
