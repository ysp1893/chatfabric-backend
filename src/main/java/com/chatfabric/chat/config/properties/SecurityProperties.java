package com.chatfabric.chat.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.security")
public class SecurityProperties {

    private boolean requireSsl;
    private boolean trustForwardHeaders = true;
    private List<String> allowedOrigins = new ArrayList<String>();
    private final RateLimit rateLimit = new RateLimit();

    @Getter
    @Setter
    public static class RateLimit {

        private boolean enabled = true;
        private int requestsPerMinute = 120;
        private List<String> excludedPaths = new ArrayList<String>();
    }
}
