package org.efire.net.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.efire.net.exceptions.LibraryEventException;
import org.springframework.boot.autoconfigure.kafka.ConcurrentKafkaListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.RecoverableDataAccessException;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.SeekToCurrentErrorHandler;
import org.springframework.retry.RecoveryCallback;
import org.springframework.retry.RetryPolicy;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@Configuration
@EnableKafka
@Slf4j
public class ConsumerAppConfig {

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    ConcurrentKafkaListenerContainerFactory<?, ?> kafkaListenerContainerFactory(
            ConcurrentKafkaListenerContainerFactoryConfigurer configurer,
            ConsumerFactory<Object, Object> kafkaConsumerFactory,
            RecoveryCallback recoveryCallback) {

        ConcurrentKafkaListenerContainerFactory<Object, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
        configurer.configure(factory, kafkaConsumerFactory);
        factory.setConcurrency(3);
/*        factory.setErrorHandler((e, consumerRecord) -> {
            log.info("Error in Consumer is \n {} \n and record was \n {}", e.getMessage(), consumerRecord);
        });*/
        factory.setErrorHandler(new SeekToCurrentErrorHandler(new FixedBackOff(1000L, 2L)));
        factory.setRecoveryCallback(recoveryCallback);
        //factory.setRecoveryCallback(recoveryCallback());
        factory.setRecoveryCallback(retryContext -> {
            if (retryContext.getLastThrowable().getCause() instanceof RecoverableDataAccessException) {
                log.info("Recoverable logic here...");
            } else {
                log.info("Non-recoverable logic here...");
                throw new RuntimeException(retryContext.getLastThrowable().getMessage());
            }
            return null;
        });
        //factory.setRetryTemplate(simpleRetryTemplate());
        return factory;
    }

    @Bean
    public DeadLetterPublishingRecoverer publisher(KafkaTemplate<?, ?> stringTemplate) {
                                                   //KafkaTemplate<?, ?> bytesTemplate) {

        Map<Class<?>, KafkaOperations<?, ?>> templates = new LinkedHashMap<>();
        templates.put(String.class, stringTemplate);
        //templates.put(byte[].class, bytesTemplate);
        return new DeadLetterPublishingRecoverer(templates);
    }

    private RetryTemplate simpleRetryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();
        FixedBackOffPolicy backOffPolicy = new FixedBackOffPolicy();
        backOffPolicy.setBackOffPeriod(1400);
        retryTemplate.setBackOffPolicy(backOffPolicy);
        retryTemplate.setRetryPolicy(simpleRetryPolicy());
        return retryTemplate;
    }

    private RetryPolicy simpleRetryPolicy() {
/*        SimpleRetryPolicy simpleRetryPolicy = new SimpleRetryPolicy();
        simpleRetryPolicy.setMaxAttempts(3);*/

        Map<Class<? extends Throwable>, Boolean> retryableExceptions = new HashMap<>();
        retryableExceptions.put(IllegalArgumentException.class, false);
        retryableExceptions.put(RecoverableDataAccessException.class, true);
        retryableExceptions.put(LibraryEventException.class, false);
        SimpleRetryPolicy simpleRetryPolicy = new SimpleRetryPolicy(3, retryableExceptions, true);
        return simpleRetryPolicy;
    }
}
