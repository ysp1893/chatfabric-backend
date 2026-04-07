package com.chatfabric.chat.repository;

import com.chatfabric.chat.entity.Chat;
import com.chatfabric.chat.entity.ChatType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChatRepository extends JpaRepository<Chat, Long> {

    @Query("select distinct c from Chat c join c.participants cp left join fetch c.participants p left join fetch p.user where cp.user.id = :userId order by c.createdAt desc")
    List<Chat> findAllByParticipantUserId(@Param("userId") Long userId);

    @Query("select distinct c from Chat c join c.participants p1 join c.participants p2 where c.type = :type and p1.user.id = :userOneId and p2.user.id = :userTwoId and size(c.participants) = 2")
    Optional<Chat> findPrivateChatBetweenUsers(@Param("type") ChatType type,
                                               @Param("userOneId") Long userOneId,
                                               @Param("userTwoId") Long userTwoId);
}
