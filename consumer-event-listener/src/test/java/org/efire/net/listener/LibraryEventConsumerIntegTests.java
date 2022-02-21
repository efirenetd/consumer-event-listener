package org.efire.net.listener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.efire.net.entity.Book;
import org.efire.net.entity.LibraryEvent;
import org.efire.net.entity.LibraryEventType;
import org.efire.net.repository.LibraryEventRepository;
import org.efire.net.service.LibraryEventService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles({"test"})
@TestPropertySource(properties = {"spring.kafka.producer.bootstrap-servers=${spring.embedded.kafka.brokers}",
                        "spring.kafka.consumer.bootstrap-servers=${spring.embedded.kafka.brokers}"},
                    locations = "classpath:application-test.yml")
@EmbeddedKafka(topics = {"LIBRARY-TOPIC", "LIBRARY-TOPIC-ONE", "LIBRARY-TOPIC-TWO"}, partitions = 3)
@Slf4j
public class LibraryEventConsumerIntegTests {

    @Autowired
    private KafkaTemplate<Integer, String> kafkaTemplate;
    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;
    @Autowired
    private KafkaListenerEndpointRegistry kafkaListenerEndpointRegistry;

    @SpyBean
    private LibraryEventConsumer libraryEventConsumer;
    @SpyBean
    private LibraryEventService libraryEventService;
    @Autowired
    private LibraryEventRepository libraryEventRepository;
    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        for (MessageListenerContainer messageListenerContainer: kafkaListenerEndpointRegistry.getListenerContainers()) {
            System.out.println(messageListenerContainer.getAssignedPartitions());
            ContainerTestUtils.waitForAssignment(messageListenerContainer, 9);
        }
    }

    @AfterEach
    void tearDown() {
        libraryEventRepository.deleteAll();


    }

    @Test
    public void testPublishNewLibraryEvent() throws InterruptedException, JsonProcessingException, ExecutionException {
        var payload = "{\"eventId\":null,\"book\":{\"id\":111,\"name\":\"Kafka Using Spring Boot\",\"author\":\"Jong\"}, \"libraryEventType\":\"NEW\"}";
        kafkaTemplate.sendDefault(payload).get();

        CountDownLatch countDownLatch = new CountDownLatch(1);
        countDownLatch.await(3, TimeUnit.SECONDS);

        verify(libraryEventConsumer, times(1)).onMessage(isA(ConsumerRecord.class));
        verify(libraryEventService, times(1)).processLibraryEvent(isA(LibraryEvent.class));

    }

    @Test
    public void testPublishUpdateLibraryEvent() throws InterruptedException, JsonProcessingException, ExecutionException {
        var payload = "{\"eventId\":null,\"book\":{\"id\":222,\"name\":\"Kafka Using Spring Boot\",\"author\":\"Deriq\"}, \"libraryEventType\":\"NEW\"}";
        LibraryEvent libraryEvent = objectMapper.readValue(payload, LibraryEvent.class);
        libraryEvent.getBook().setLibraryEvent(libraryEvent);
        libraryEventRepository.save(libraryEvent);

        Book aBook = Book.builder()
                .id(222)
                .author("Deriq Tenerife")
                .name("Animaniac")
                .build();
        libraryEvent.setLibraryEventType(LibraryEventType.UPDATED);
        libraryEvent.setBook(aBook);
        String updatedPayload = objectMapper.writeValueAsString(libraryEvent);
        kafkaTemplate.sendDefault(updatedPayload).get();

        CountDownLatch countDownLatch = new CountDownLatch(1);
        countDownLatch.await(3, TimeUnit.SECONDS);

        verify(libraryEventConsumer, times(1)).onMessage(isA(ConsumerRecord.class));
        verify(libraryEventService, times(1)).processLibraryEvent(isA(LibraryEvent.class));

        LibraryEvent actualLibraryEvent = libraryEventRepository.findById(libraryEvent.getEventId()).get();
        assertThat(actualLibraryEvent.getEventId()).isNotNull();
        assertThat(actualLibraryEvent.getLibraryEventType()).isEqualTo(LibraryEventType.UPDATED);
        assertThat(actualLibraryEvent.getBook()).isNotNull();
        assertThat(actualLibraryEvent.getBook().getName()).isEqualTo("Animaniac");
        assertThat(actualLibraryEvent.getBook().getAuthor()).isEqualTo("Deriq Tenerife");
    }

    @Test
    public void testLibraryEventNotValid() throws JsonProcessingException, ExecutionException, InterruptedException {
        var payload = "{\"eventId\":99,\"book\":{\"id\":111,\"name\":\"Baba Yaga\",\"author\":\"John Wick\"}, \"libraryEventType\":\"UPDATED\"}";

        kafkaTemplate.sendDefault(payload).get();

        CountDownLatch countDownLatch = new CountDownLatch(1);
        countDownLatch.await(3, TimeUnit.SECONDS);

        verify(libraryEventConsumer, atLeast(1)).onMessage(isA(ConsumerRecord.class));
        verify(libraryEventService, atLeast(1)).processLibraryEvent(isA(LibraryEvent.class));

        Optional<LibraryEvent> byId = libraryEventRepository.findById(99);
        assertThat(byId.isPresent()).isFalse();

    }

    @Test
    public void testLibraryEventNullExpectRetry() throws JsonProcessingException, ExecutionException, InterruptedException {
        var payload = "{\"eventId\":null,\"book\":{\"id\":111,\"name\":\"Taken\",\"author\":\"Bryan Mills\"}, \"libraryEventType\":\"UPDATED\"}";

        kafkaTemplate.sendDefault(payload).get();

        CountDownLatch countDownLatch = new CountDownLatch(1);
        countDownLatch.await(5, TimeUnit.SECONDS);

        verify(libraryEventConsumer, times(3)).onMessage(isA(ConsumerRecord.class));
        verify(libraryEventService, times(3)).processLibraryEvent(isA(LibraryEvent.class));
    }

    @Test
    public void testLibraryEvent999ExpectRetry() throws JsonProcessingException, ExecutionException, InterruptedException {
        var payload = "{\"eventId\":999,\"book\":{\"id\":111,\"name\":\"Taken\",\"author\":\"Bryan Mills\"}, \"libraryEventType\":\"NEW\"}";

        kafkaTemplate.sendDefault(payload).get();

        CountDownLatch countDownLatch = new CountDownLatch(1);
        countDownLatch.await(6, TimeUnit.SECONDS);

        verify(libraryEventConsumer, times(4)).onMessage(isA(ConsumerRecord.class));
        verify(libraryEventService, times(4)).processLibraryEvent(isA(LibraryEvent.class));
    }

    @Test
    public void testLibraryEvent666ExpectRetryWithFailedMessage() throws JsonProcessingException, ExecutionException, InterruptedException {
        var payload = "{\"eventId\":666,\"book\":{\"id\":111,\"name\":\"Taken\",\"author\":\"Bryan Mills\"}, \"libraryEventType\":\"NEW\"}";

        kafkaTemplate.sendDefault(payload).get();

        CountDownLatch countDownLatch = new CountDownLatch(1);
        countDownLatch.await(5, TimeUnit.SECONDS);

        verify(libraryEventConsumer, times(1)).onMessage(isA(ConsumerRecord.class));
        verify(libraryEventService, times(1)).processLibraryEvent(isA(LibraryEvent.class));
    }
}
