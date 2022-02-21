package org.efire.net.config;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@ConfigurationProperties(prefix = "library")
@Getter
@Setter
@ToString
public class LibraryEventProperties {

    private Map<String, String> topics;

}
