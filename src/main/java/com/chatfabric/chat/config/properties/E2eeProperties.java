package com.chatfabric.chat.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.e2ee")
public class E2eeProperties {

    private boolean required = true;
}
