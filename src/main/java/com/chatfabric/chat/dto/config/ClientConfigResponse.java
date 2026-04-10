package com.chatfabric.chat.dto.config;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ClientConfigResponse {

    private final boolean e2eeRequired;
}
