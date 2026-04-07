package com.chatfabric.chat.controller;

import com.chatfabric.chat.dto.chat.ChatResponse;
import com.chatfabric.chat.dto.chat.CreateChatRequest;
import com.chatfabric.chat.security.SecurityUserPrincipal;
import com.chatfabric.chat.service.ChatService;
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
@RequestMapping("/api/chats")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ChatResponse createPrivateChat(@Valid @RequestBody CreateChatRequest request,
                                          @AuthenticationPrincipal SecurityUserPrincipal principal) {
        return chatService.createPrivateChat(request, principal.getId());
    }

    @GetMapping("/{userId}")
    public List<ChatResponse> getChatsForUser(@PathVariable Long userId,
                                              @AuthenticationPrincipal SecurityUserPrincipal principal) {
        return chatService.getChatsForUser(userId, principal.getId());
    }
}
