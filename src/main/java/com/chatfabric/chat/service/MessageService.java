package com.chatfabric.chat.service;

import com.chatfabric.chat.dto.message.MessageResponse;
import com.chatfabric.chat.dto.message.SendMessageRequest;
import com.chatfabric.chat.entity.Chat;
import com.chatfabric.chat.entity.Message;
import com.chatfabric.chat.entity.MessageStatus;
import com.chatfabric.chat.entity.User;
import com.chatfabric.chat.repository.MessageRepository;
import com.chatfabric.chat.util.EntityMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class MessageService {

    private final MessageRepository messageRepository;
    private final ChatService chatService;
    private final UserService userService;

    public MessageService(MessageRepository messageRepository,
                          ChatService chatService,
                          UserService userService) {
        this.messageRepository = messageRepository;
        this.chatService = chatService;
        this.userService = userService;
    }

    @Transactional
    public MessageResponse sendMessage(SendMessageRequest request, Long authenticatedUserId) {
        Chat chat = chatService.getChatEntityById(request.getChatId());
        User sender = userService.getEntityById(authenticatedUserId);

        chatService.validateUserInChat(chat.getId(), sender.getId());

        Message message = Message.builder()
                .chat(chat)
                .sender(sender)
                .content(request.getContent().trim())
                .status(MessageStatus.SENT)
                .build();

        Message savedMessage = messageRepository.save(message);
        log.info("Stored message id={} chatId={} senderId={}",
                savedMessage.getId(),
                savedMessage.getChat().getId(),
                savedMessage.getSender().getId());
        return EntityMapper.toMessageResponse(savedMessage);
    }

    @Transactional(readOnly = true)
    public List<MessageResponse> getMessagesByChatId(Long chatId, Long authenticatedUserId) {
        chatService.getChatEntityById(chatId);
        chatService.validateUserInChat(chatId, authenticatedUserId);
        List<Message> messages = messageRepository.findByChatIdOrderByTimestampAsc(chatId);
        List<MessageResponse> responses = new ArrayList<MessageResponse>();
        for (Message message : messages) {
            responses.add(EntityMapper.toMessageResponse(message));
        }
        return responses;
    }
}
