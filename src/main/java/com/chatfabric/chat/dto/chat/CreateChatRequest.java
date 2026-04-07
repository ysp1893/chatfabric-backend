package com.chatfabric.chat.dto.chat;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;

@Getter
@Setter
public class CreateChatRequest {

    @NotNull(message = "First participant id is required")
    private Long firstUserId;

    @NotNull(message = "Second participant id is required")
    private Long secondUserId;
}
