package com.chatfabric.chat.repository;

import com.chatfabric.chat.entity.UserKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserKeyRepository extends JpaRepository<UserKey, Long> {

    Optional<UserKey> findByUserIdAndActiveTrue(Long userId);

    Optional<UserKey> findByUserIdAndKeyVersion(Long userId, Integer keyVersion);

    boolean existsByUserIdAndKeyVersion(Long userId, Integer keyVersion);

    List<UserKey> findAllByUserIdAndActiveTrue(Long userId);

    List<UserKey> findAllByUserIdOrderByKeyVersionDesc(Long userId);
}
