package com.dersbul.domain;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.*;

@Entity @Getter @Setter @NoArgsConstructor
public class LessonRequest {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false) private User student;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) private TeacherListing listing;
    @Column(nullable = false, length = 2000) private String message;
    private String studentLevel;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private LessonMode mode;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private RequestStatus status = RequestStatus.PENDING;
    private Instant createdAt = Instant.now();
    private Instant respondedAt;
}
