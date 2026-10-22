package com.dersbul.domain;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.*;

@Entity @Getter @Setter @NoArgsConstructor
public class Message {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false) private Conversation conversation;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) private User sender;
    @Column(nullable = false, length = 2000) private String body;
    private Instant createdAt = Instant.now();
}
