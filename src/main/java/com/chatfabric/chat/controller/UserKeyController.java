package com.chatfabric.chat.controller;

import com.chatfabric.chat.dto.key.UserKeyRegistrationRequest;
import com.chatfabric.chat.dto.key.UserKeyResponse;
import com.chatfabric.chat.security.SecurityUserPrincipal;
import com.chatfabric.chat.service.UserKeyService;
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
@RequestMapping("/api/keys")
public class UserKeyController {

    private final UserKeyService userKeyService;

    public UserKeyController(UserKeyService userKeyService) {
        this.userKeyService = userKeyService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserKeyResponse registerKey(@Valid @RequestBody UserKeyRegistrationRequest request,
                                       @AuthenticationPrincipal SecurityUserPrincipal principal) {
        return userKeyService.registerActiveKey(principal.getId(), request);
    }

    @GetMapping("/{userId}/active")
    public UserKeyResponse getActiveKey(@PathVariable Long userId) {
        return userKeyService.getActiveKey(userId);
    }

    @GetMapping("/{userId}/versions/{keyVersion}")
    public UserKeyResponse getKeyByVersion(@PathVariable Long userId,
                                           @PathVariable Integer keyVersion) {
        return userKeyService.getKeyByVersion(userId, keyVersion);
    }

    @GetMapping("/{userId}")
    public List<UserKeyResponse> getKeyHistory(@PathVariable Long userId) {
        return userKeyService.getKeyHistory(userId);
    }
}
