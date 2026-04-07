package com.chatfabric.chat.dto.message;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Getter
@Setter
public class SendMessageRequest {

    @NotNull(message = "Chat id is required")
    private Long chatId;

    @NotNull(message = "Sender id is required")
    private Long senderId;

    @NotBlank(message = "Message content is required")
    @Size(max = 2000, message = "Message content must not exceed 2000 characters")
    private String content;
}
