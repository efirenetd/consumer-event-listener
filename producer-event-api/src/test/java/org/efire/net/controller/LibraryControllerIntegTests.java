package org.efire.net.controller;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.IntegerDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.efire.net.domain.Book;
import org.efire.net.domain.LibraryEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.TestPropertySource;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {"spring.kafka.consumer.bootstrap-servers=${spring.embedded.kafka.brokers}"})
@EmbeddedKafka(topics = {"LIBRARY-TOPIC", "LIBRARY-TOPIC-ONE", "LIBRARY-TOPIC-TWO"}, partitions = 3)
@Slf4j
public class LibraryControllerIntegTests {

    @Autowired
    private  TestRestTemplate restTemplate;
    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;
    @LocalServerPort
    private int randomServerPort;

    private String baseUrl;
    private Consumer<Integer, String> consumer;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + randomServerPort;
        Map<String, Object> configs = new HashMap<>(KafkaTestUtils.consumerProps("group1","true",embeddedKafkaBroker));
        consumer = new DefaultKafkaConsumerFactory<>(configs, new IntegerDeserializer(), new StringDeserializer())
                .createConsumer();
        embeddedKafkaBroker.consumeFromAllEmbeddedTopics(consumer);
    }

    @AfterEach
    void tearDown() {
        consumer.close();
    }

    @Test
    public void testDefaultLibraryEvent_expectCreated() {
        Book aBook = Book.builder()
                .id(null)
                .author("Jong Tenerife")
                .name("The Book")
                .build();
        LibraryEvent libraryEvent = LibraryEvent.builder()
                .eventId(null)
                .book(aBook)
                .build();

        RequestEntity<LibraryEvent> requestEntity = RequestEntity
                .post(URI.create(baseUrl + "/v1/libraries"))
                .body(libraryEvent);
        ResponseEntity<LibraryEvent> exchange = this.restTemplate.exchange(requestEntity, LibraryEvent.class);
        ConsumerRecord<Integer, String> singleRecord = KafkaTestUtils.getSingleRecord(consumer, "LIBRARY-TOPIC");
        log.info(">>> Payload Received: \n"+ singleRecord.value());
        String expectedValue = "{\"eventId\":null,\"book\":{\"id\":null,\"name\":\"The Book\",\"author\":\"Jong Tenerife\"},\"libraryEventType\":\"NEW\"}";

        assertThat(exchange.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(singleRecord.value()).isEqualTo(expectedValue);
    }

    @Test
    public void testDefaultLibraryEvent_expectOk() {
        Book aBook = Book.builder()
                .id(null)
                .author("Deriq Tenerife")
                .name("Animaniac")
                .build();
        LibraryEvent libraryEvent = LibraryEvent.builder()
                .eventId(1)
                .book(aBook)
                .build();

        RequestEntity<?> requestEntity = RequestEntity
                .put(URI.create(baseUrl + "/v1/libraries"))
                .body(libraryEvent);

        ResponseEntity<LibraryEvent> exchange = this.restTemplate.exchange(requestEntity, LibraryEvent.class);
        ConsumerRecord<Integer, String> singleRecord = KafkaTestUtils.getSingleRecord(consumer, "LIBRARY-TOPIC");

        log.info(">>> Payload Received: \n"+ singleRecord.value());
        String expectedValue = "{\"eventId\":1,\"book\":{\"id\":null,\"name\":\"Animaniac\",\"author\":\"Deriq Tenerife\"},\"libraryEventType\":\"UPDATED\"}";

        assertThat(exchange.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(singleRecord.value()).isEqualTo(expectedValue);
    }

    @Test
    public void testDefaultLibraryEvent_expectBadRequest() {
        Book aBook = Book.builder()
                .id(null)
                .author("Delma Tenerife")
                .name("MilkTea")
                .build();
        LibraryEvent libraryEvent = LibraryEvent.builder()
                .eventId(null)
                .book(aBook)
                .build();

        RequestEntity<LibraryEvent> requestEntity = RequestEntity
                .put(URI.create(baseUrl + "/v1/libraries"))
                .body(libraryEvent);

        ResponseEntity<String> exchange = this.restTemplate.exchange(requestEntity, String.class);

        assertThat(exchange.getBody()).isEqualTo("Please pass Library EventId");
        assertThat(exchange.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    public void testTopicOneLibraryEvent() {
        Book aBook = Book.builder()
                .id(null)
                .author("Deriq Tenerife")
                .name("Animaniac")
                .build();
        LibraryEvent libraryEvent = LibraryEvent.builder()
                .eventId(null)
                .book(aBook)
                .build();

        RequestEntity<LibraryEvent> requestEntity = RequestEntity
                .post(URI.create(baseUrl + "/v1/libraries/eventOne"))
                .body(libraryEvent);
        ResponseEntity<LibraryEvent> exchange = this.restTemplate.exchange(requestEntity, LibraryEvent.class);
        ConsumerRecord<Integer, String> singleRecord = KafkaTestUtils.getSingleRecord(consumer, "LIBRARY-TOPIC-ONE");
        System.out.println(">>> Payload Received: \n"+ singleRecord.value());
        String expectedValue = "{\"eventId\":null,\"book\":{\"id\":null,\"name\":\"Animaniac\",\"author\":\"Deriq Tenerife\"},\"libraryEventType\":\"NEW\"}";

        assertThat(exchange.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(singleRecord.value()).isEqualTo(expectedValue);
    }

    @Test
    public void testTopicTwoLibraryEvent() {
        Book aBook = Book.builder()
                .id(null)
                .author("Racqel Tenerife")
                .name("Grey Anatomy")
                .build();
        LibraryEvent libraryEvent = LibraryEvent.builder()
                .eventId(null)
                .book(aBook)
                .build();

        RequestEntity<LibraryEvent> requestEntity = RequestEntity
                .post(URI.create(baseUrl + "/v1/libraries/eventTwo"))
                .body(libraryEvent);
        ResponseEntity<LibraryEvent> exchange = this.restTemplate.exchange(requestEntity, LibraryEvent.class);
        ConsumerRecord<Integer, String> singleRecord = KafkaTestUtils
                .getSingleRecord(consumer, "LIBRARY-TOPIC-TWO", 10000L);
        System.out.println(">>> Payload Received: \n"+ singleRecord.value());
        String expectedValue = "{\"eventId\":null,\"book\":{\"id\":null,\"name\":\"Grey Anatomy\",\"author\":\"Racqel Tenerife\"},\"libraryEventType\":\"NEW\"}";

        assertThat(exchange.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(singleRecord.value()).isEqualTo(expectedValue);
    }
}
