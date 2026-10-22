package com.dersbul.domain;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.*;

@Entity @Getter @Setter @NoArgsConstructor
public class TeacherListing {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false) private User teacher;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) private Category category;
    @Column(nullable = false, length = 140) private String title;
    @Column(nullable = false, length = 4000) private String description;
    @Column(nullable = false, precision = 10, scale = 2) private BigDecimal hourlyRate;
    private String city;
    private boolean online;
    private boolean teacherHome;
    private boolean studentHome;
    private boolean firstLessonFree;
    private int experienceYears;
    private String education;
    private String languages;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private ListingStatus status = ListingStatus.ACTIVE;
    private boolean boosted;
    private double avgRating;
    private int reviewCount;
    private Instant createdAt = Instant.now();
    /** Aramada kullanılan, Türkçe karakterleri sadeleştirilmiş küçük harfli metin (başlık, açıklama, kategori, öğretmen, şehir). */
    @Column(length = 6000) private String searchText;
}
