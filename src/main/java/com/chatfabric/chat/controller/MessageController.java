package com.chatfabric.chat.controller;

import com.chatfabric.chat.dto.message.MessageResponse;
import com.chatfabric.chat.dto.message.SendMessageRequest;
import com.chatfabric.chat.security.SecurityUserPrincipal;
import com.chatfabric.chat.service.MessageService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

@Validated
@RestController
@RequestMapping("/api/messages")
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MessageResponse sendMessage(@Valid @RequestBody SendMessageRequest request,
                                       @AuthenticationPrincipal SecurityUserPrincipal principal) {
        return messageService.sendMessage(request, principal.getId());
    }

    @GetMapping("/{chatId}")
    public List<MessageResponse> getMessagesByChatId(@PathVariable Long chatId,
                                                     @AuthenticationPrincipal SecurityUserPrincipal principal) {
        return messageService.getMessagesByChatId(chatId, principal.getId());
    }
}
