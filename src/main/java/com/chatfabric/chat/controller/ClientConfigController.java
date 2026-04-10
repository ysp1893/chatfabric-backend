package com.chatfabric.chat.controller;

import com.chatfabric.chat.config.properties.E2eeProperties;
import com.chatfabric.chat.dto.config.ClientConfigResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/client-config")
public class ClientConfigController {

    private final E2eeProperties e2eeProperties;

    public ClientConfigController(E2eeProperties e2eeProperties) {
        this.e2eeProperties = e2eeProperties;
    }

    @GetMapping
    public ClientConfigResponse getClientConfig() {
        return ClientConfigResponse.builder()
                .e2eeRequired(e2eeProperties.isRequired())
                .build();
    }
}
