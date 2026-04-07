package com.chatfabric.chat.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "user_keys",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_user_key_version", columnNames = {"user_id", "key_version"})
        },
        indexes = {
                @Index(name = "idx_user_key_active", columnList = "user_id,active")
        }
)
public class UserKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Lob
    @Column(name = "public_encryption_key", nullable = false)
    private String publicEncryptionKey;

    @Lob
    @Column(name = "public_signing_key", nullable = false)
    private String publicSigningKey;

    @Column(name = "key_version", nullable = false)
    private Integer keyVersion;

    @Column(nullable = false)
    private boolean active;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
