package com.dersbul.domain;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.*;

@Entity @Getter @Setter @NoArgsConstructor
public class Subscription {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false) private User user;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private Plan plan;
    @Column(nullable = false) private BigDecimal amountTry;
    private Instant startsAt = Instant.now();
    @Column(nullable = false) private Instant endsAt;
    private String paymentRef;
}
