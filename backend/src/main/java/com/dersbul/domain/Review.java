package com.dersbul.domain;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.*;

@Entity @Getter @Setter @NoArgsConstructor
public class Review {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false) private TeacherListing listing;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) private User student;
    @OneToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(unique = true) private LessonSession session;
    private int rating;
    @Column(length = 2000) private String body;
    private Instant createdAt = Instant.now();
}
