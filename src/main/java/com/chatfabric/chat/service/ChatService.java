package com.chatfabric.chat.service;

import com.chatfabric.chat.dto.chat.ChatResponse;
import com.chatfabric.chat.dto.chat.CreateChatRequest;
import com.chatfabric.chat.entity.Chat;
import com.chatfabric.chat.entity.ChatParticipant;
import com.chatfabric.chat.entity.ChatType;
import com.chatfabric.chat.entity.User;
import com.chatfabric.chat.exception.BadRequestException;
import com.chatfabric.chat.exception.ResourceNotFoundException;
import com.chatfabric.chat.repository.ChatParticipantRepository;
import com.chatfabric.chat.repository.ChatRepository;
import com.chatfabric.chat.util.EntityMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class ChatService {

    private final ChatRepository chatRepository;
    private final ChatParticipantRepository chatParticipantRepository;
    private final UserService userService;

    public ChatService(ChatRepository chatRepository,
                       ChatParticipantRepository chatParticipantRepository,
                       UserService userService) {
        this.chatRepository = chatRepository;
        this.chatParticipantRepository = chatParticipantRepository;
        this.userService = userService;
    }

    @Transactional
    public ChatResponse createPrivateChat(CreateChatRequest request, Long authenticatedUserId) {
        validateParticipants(request);

        Long firstUserId = request.getFirstUserId();
        Long secondUserId = request.getSecondUserId();

        if (!authenticatedUserId.equals(firstUserId) && !authenticatedUserId.equals(secondUserId)) {
            throw new AccessDeniedException("You can only create private chats that include your own user");
        }

        Chat existingChat = chatRepository.findPrivateChatBetweenUsers(ChatType.PRIVATE, firstUserId, secondUserId)
                .orElse(null);

        if (existingChat != null) {
            log.info("Private chat already exists for users {} and {}", firstUserId, secondUserId);
            return EntityMapper.toChatResponse(existingChat);
        }

        User firstUser = userService.getEntityById(firstUserId);
        User secondUser = userService.getEntityById(secondUserId);

        Chat chat = Chat.builder()
                .type(ChatType.PRIVATE)
                .build();

        ChatParticipant firstParticipant = ChatParticipant.builder()
                .chat(chat)
                .user(firstUser)
                .build();

        ChatParticipant secondParticipant = ChatParticipant.builder()
                .chat(chat)
                .user(secondUser)
                .build();

        List<ChatParticipant> participants = new ArrayList<ChatParticipant>();
        participants.add(firstParticipant);
        participants.add(secondParticipant);
        chat.setParticipants(participants);

        Chat savedChat = chatRepository.save(chat);
        log.info("Created private chat id={} between users {} and {}", savedChat.getId(), firstUserId, secondUserId);
        return EntityMapper.toChatResponse(savedChat);
    }

    @Transactional(readOnly = true)
    public List<ChatResponse> getChatsForUser(Long userId, Long authenticatedUserId) {
        userService.validateSelfAccess(userId, authenticatedUserId);
        userService.getEntityById(userId);
        List<Chat> chats = chatRepository.findAllByParticipantUserId(userId);
        List<ChatResponse> responses = new ArrayList<ChatResponse>();
        for (Chat chat : chats) {
            responses.add(EntityMapper.toChatResponse(chat));
        }
        return responses;
    }

    @Transactional(readOnly = true)
    public Chat getChatEntityById(Long chatId) {
        return chatRepository.findById(chatId)
                .orElseThrow(new java.util.function.Supplier<ResourceNotFoundException>() {
                    @Override
                    public ResourceNotFoundException get() {
                        return new ResourceNotFoundException("Chat not found with id=" + chatId);
                    }
                });
    }

    @Transactional(readOnly = true)
    public void validateUserInChat(Long chatId, Long userId) {
        if (!chatParticipantRepository.existsByChatIdAndUserId(chatId, userId)) {
            throw new AccessDeniedException("Authenticated user is not a participant in chat id=" + chatId);
        }
    }

    private void validateParticipants(CreateChatRequest request) {
        if (request.getFirstUserId().equals(request.getSecondUserId())) {
            throw new BadRequestException("Private chat requires two distinct users");
        }
    }
}
