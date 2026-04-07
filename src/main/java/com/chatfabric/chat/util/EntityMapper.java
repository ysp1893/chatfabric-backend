package com.chatfabric.chat.util;

import com.chatfabric.chat.dto.chat.ChatParticipantResponse;
import com.chatfabric.chat.dto.chat.ChatResponse;
import com.chatfabric.chat.dto.message.MessageResponse;
import com.chatfabric.chat.dto.user.UserResponse;
import com.chatfabric.chat.entity.Chat;
import com.chatfabric.chat.entity.ChatParticipant;
import com.chatfabric.chat.entity.Message;
import com.chatfabric.chat.entity.MessageFormat;
import com.chatfabric.chat.entity.User;

import java.util.ArrayList;
import java.util.List;

public final class EntityMapper {

    private EntityMapper() {
    }

    public static UserResponse toUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .status(user.getStatus())
                .createdAt(user.getCreatedAt())
                .build();
    }

    public static ChatResponse toChatResponse(Chat chat) {
        List<ChatParticipantResponse> participantResponses = new ArrayList<ChatParticipantResponse>();
        for (ChatParticipant participant : chat.getParticipants()) {
            participantResponses.add(ChatParticipantResponse.builder()
                    .userId(participant.getUser().getId())
                    .username(participant.getUser().getUsername())
                    .status(participant.getUser().getStatus())
                    .build());
        }

        return ChatResponse.builder()
                .id(chat.getId())
                .type(chat.getType())
                .createdAt(chat.getCreatedAt())
                .participants(participantResponses)
                .build();
    }

    public static MessageResponse toMessageResponse(Message message) {
        MessageFormat messageFormat = message.getMessageFormat();
        if (messageFormat == null) {
            messageFormat = message.getContent() != null ? MessageFormat.PLAINTEXT_V1 : MessageFormat.E2EE_V1;
        }
        return MessageResponse.builder()
                .id(message.getId())
                .chatId(message.getChat().getId())
                .senderId(message.getSender().getId())
                .senderUsername(message.getSender().getUsername())
                .content(message.getContent())
                .ciphertext(message.getCiphertext())
                .nonce(message.getNonce())
                .algorithm(message.getAlgorithm())
                .encryptedMessageKey(message.getEncryptedMessageKey())
                .signature(message.getSignature())
                .keyVersion(message.getKeyVersion())
                .messageFormat(messageFormat)
                .timestamp(message.getTimestamp())
                .status(message.getStatus())
                .build();
    }
}
