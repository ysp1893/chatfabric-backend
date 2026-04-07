package com.chatfabric.chat.dto.chat;

import com.chatfabric.chat.entity.UserStatus;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ChatParticipantResponse {

    private final Long userId;
    private final String username;
    private final UserStatus status;
}
