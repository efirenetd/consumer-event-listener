package org.efire.net.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.efire.net.domain.Book;
import org.efire.net.domain.LibraryEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.SettableListenableFuture;

import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DefaultLibraryEventProducerTests {

    @Mock
    private KafkaTemplate<Integer, String> kafkaTemplate;
    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private DefaultLibraryEventProducer libraryEventProducer;

    @Test
    public void test_sendLibraryEvent_expect_success() throws JsonProcessingException, ExecutionException, InterruptedException {
        Book aBook = Book.builder()
                .id(null)
                .author("Deriq Tenerife")
                .name("Animaniac")
                .build();
        LibraryEvent libraryEvent = LibraryEvent.builder()
                .eventId(null)
                .book(aBook)
                .build();
        String payload = objectMapper.writeValueAsString(libraryEvent);
        ProducerRecord<Integer, String> producerRecord = new ProducerRecord<Integer, String>("LIBRARY-TOPIC", 1, null, payload);
        RecordMetadata recordMetadata = new RecordMetadata(new TopicPartition("LIBRARY-TOPIC", 1),0, 1, System.currentTimeMillis(), 0L, 0,0);
        SendResult<Integer, String> sendResult = new SendResult<Integer, String>(producerRecord, recordMetadata);
        SettableListenableFuture listenableFuture = new SettableListenableFuture();
        listenableFuture.set(sendResult);

        when(kafkaTemplate.sendDefault(null, payload)).thenReturn(listenableFuture);
        ListenableFuture<SendResult<Integer, String>> resultListenableFuture = libraryEventProducer.sendLibraryEvent(libraryEvent);

        assert resultListenableFuture.isDone();
        assert resultListenableFuture.get().getProducerRecord().topic().equals("LIBRARY-TOPIC");
        assert resultListenableFuture.get().getProducerRecord().partition() == 1;
    }

    @Test
    public void test_sendLibraryEvent_expect_failure() throws JsonProcessingException {
        Book aBook = Book.builder()
                .id(null)
                .author("Jong Tenerife")
                .name("The Book")
                .build();
        LibraryEvent libraryEvent = LibraryEvent.builder()
                .eventId(null)
                .book(aBook)
                .build();

        String payload = objectMapper.writeValueAsString(libraryEvent);

        SettableListenableFuture listenableFuture = new SettableListenableFuture();
        listenableFuture.setException(new RuntimeException("Exception thrown to fail"));

        when(kafkaTemplate.sendDefault(null, payload)).thenReturn(listenableFuture);
        assertThrows(Exception.class, () -> libraryEventProducer.sendLibraryEvent(libraryEvent).get());
    }
}
