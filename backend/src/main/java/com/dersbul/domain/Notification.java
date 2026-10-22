package com.dersbul.domain;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.*;

@Entity @Getter @Setter @NoArgsConstructor
public class Notification {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false) private User user;
    @Column(nullable = false, length = 500) private String content;
    @Column(length = 300) private String link;
    private boolean seen;
    private Instant createdAt = Instant.now();
}
