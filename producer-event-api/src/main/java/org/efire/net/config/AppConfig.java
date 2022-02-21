package org.efire.net.config;

import org.efire.net.service.DefaultLibraryEventProducer;
import org.efire.net.service.LibraryEventProducer;
import org.efire.net.service.TopicLibraryEventProducer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;

@Configuration
public class AppConfig {

    @Bean
    public List<LibraryEventProducer> producerList(DefaultLibraryEventProducer defaultProducer,
                                                           TopicLibraryEventProducer topicProducer) {
        return Arrays.asList(defaultProducer, topicProducer);
    }

    @Bean
    public RestTemplate restTemplate() {
        RestTemplate rt = new RestTemplate();
        return rt;
    }
}
