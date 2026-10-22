package com.dersbul.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

/** Öğrencinin ders talebiyle birlikte önerdiği zaman dilimi. Öğretmen birini seçerek talebi onaylar. */
@Entity @Getter @Setter @NoArgsConstructor
public class RequestSlot {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) private LessonRequest request;
    @Column(nullable = false) private Instant startsAt;
    @Column(nullable = false) private Instant endsAt;
    private boolean chosen;
}
