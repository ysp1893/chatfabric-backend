package com.chatfabric.chat.security;

import com.chatfabric.chat.entity.User;
import com.chatfabric.chat.exception.ResourceNotFoundException;
import com.chatfabric.chat.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(new java.util.function.Supplier<UsernameNotFoundException>() {
                    @Override
                    public UsernameNotFoundException get() {
                        return new UsernameNotFoundException("User not found with username=" + username);
                    }
                });
        return createPrincipal(user);
    }

    public SecurityUserPrincipal loadPrincipalByUserId(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(new java.util.function.Supplier<ResourceNotFoundException>() {
                    @Override
                    public ResourceNotFoundException get() {
                        return new ResourceNotFoundException("User not found with id=" + userId);
                    }
                });
        return createPrincipal(user);
    }

    private SecurityUserPrincipal createPrincipal(User user) {
        return new SecurityUserPrincipal(user.getId(), user.getUsername(), user.getPassword());
    }
}
