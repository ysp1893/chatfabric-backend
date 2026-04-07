package com.chatfabric.chat.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "messages",
        indexes = {
                @Index(name = "idx_message_chat_timestamp", columnList = "chat_id,timestamp")
        }
)
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chat_id", nullable = false)
    private Chat chat;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @Column(length = 2000)
    private String content;

    @Lob
    @Column(name = "ciphertext")
    private String ciphertext;

    @Column(name = "nonce", length = 1024)
    private String nonce;

    @Column(name = "algorithm", length = 100)
    private String algorithm;

    @Lob
    @Column(name = "encrypted_message_key")
    private String encryptedMessageKey;

    @Lob
    @Column(name = "signature")
    private String signature;

    @Column(name = "key_version")
    private Integer keyVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "message_format", length = 30)
    private MessageFormat messageFormat;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime timestamp;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MessageStatus status;
}
