package com.chatfabric.chat;

import com.chatfabric.chat.config.properties.E2eeProperties;
import com.chatfabric.chat.config.properties.SecurityProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({SecurityProperties.class, E2eeProperties.class})
public class ChatFabricApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChatFabricApplication.class, args);
    }
}
