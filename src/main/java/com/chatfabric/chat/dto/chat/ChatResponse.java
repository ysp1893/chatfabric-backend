package com.chatfabric.chat.dto.chat;

import com.chatfabric.chat.entity.ChatType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class ChatResponse {

    private final Long id;
    private final ChatType type;
    private final LocalDateTime createdAt;
    private final List<ChatParticipantResponse> participants;
}
