package com.dersbul.dto;

import com.dersbul.domain.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;

public final class Dtos {
    private Dtos() {}

    public record PageRes<T>(List<T> content, int page, int totalPages, long totalElements) {}

    // ---- auth
    public record RegisterReq(
        @NotBlank(message = "E-posta zorunlu") @Email(message = "Geçerli bir e-posta girin") String email,
        @NotBlank @Size(min = 8, message = "Şifre en az 8 karakter olmalı") String password,
        @NotBlank(message = "Ad soyad zorunlu") String fullName,
        @NotNull(message = "Hesap türü seçin") Role role,
        String city) {}
    public record LoginReq(@NotBlank String email, @NotBlank String password) {}
    public record UserRes(Long id, String email, String fullName, Role role, String city, boolean hasActivePass) {}
    public record AuthRes(String token, UserRes user) {}

    // ---- catalog
    public record CategoryRes(Long id, String name, String slug, String groupName) {}
    public record QualificationReq(@NotBlank(message = "Başlık zorunlu") @Size(max = 200) String title) {}
    public record QualificationRes(Long id, String title, QualificationStatus status) {}

    public record ListingReq(
        @NotNull(message = "Ders alanı seçin") Long categoryId,
        @NotBlank(message = "Başlık zorunlu") @Size(max = 140, message = "Başlık en fazla 140 karakter") String title,
        @NotBlank @Size(min = 40, max = 4000, message = "Açıklama 40-4000 karakter arasında olmalı") String description,
        @NotNull @DecimalMin(value = "1", message = "Ücret 0'dan büyük olmalı") BigDecimal hourlyRate,
        @NotBlank(message = "Şehir zorunlu") String city,
        boolean online, boolean teacherHome, boolean studentHome, boolean firstLessonFree,
        @Min(0) int experienceYears, String education, String languages) {}

    public record ListingRes(
        Long id, Long teacherId, String teacherName, Long categoryId, String categoryName,
        String title, String description, BigDecimal hourlyRate, String city,
        boolean online, boolean teacherHome, boolean studentHome, boolean firstLessonFree,
        int experienceYears, String education, String languages,
        ListingStatus status, boolean boosted, double avgRating, int reviewCount,
        List<QualificationRes> qualifications) {}

    public record AvailabilityReq(@NotNull DayOfWeek weekday, @NotNull LocalTime startTime, @NotNull LocalTime endTime) {}
    public record AvailabilityRes(Long id, DayOfWeek weekday, LocalTime startTime, LocalTime endTime) {}

    // ---- requests, messages, sessions
    public record StatusReq(@NotBlank String status) {}
    public record RequestReq(
        @NotNull Long listingId,
        @NotBlank(message = "Ne öğrenmek istediğinizi kısaca yazın") @Size(max = 2000) String message,
        String studentLevel,
        @NotNull(message = "Ders biçimi seçin") LessonMode mode) {}
    public record RequestRes(
        Long id, Long listingId, String listingTitle, Long studentId, String studentName,
        Long teacherId, String teacherName, String message, String studentLevel, LessonMode mode,
        RequestStatus status, Instant createdAt, Long conversationId) {}

    public record MessageReq(@NotBlank @Size(max = 2000) String body) {}
    public record MessageRes(Long id, Long senderId, String senderName, String body, Instant createdAt) {}

    public record SessionReq(@NotNull Long requestId, @NotNull Instant startsAt, @NotNull Instant endsAt) {}
    public record SessionRes(
        Long id, Long requestId, Long listingId, String listingTitle, String teacherName, String studentName,
        Instant startsAt, Instant endsAt, SessionStatus status, boolean reviewed) {}

    public record ReviewReq(@NotNull Long sessionId, @Min(1) @Max(5) int rating, @Size(max = 2000) String body) {}
    public record ReviewRes(Long id, String studentName, int rating, String body, Instant createdAt) {}

    // ---- subscriptions & notifications
    public record BuyReq(@NotNull Plan plan, Integer months) {}
    public record SubscriptionRes(Long id, Plan plan, BigDecimal amountTry, Instant startsAt, Instant endsAt, boolean active) {}
    public record PlanRes(Plan plan, String title, BigDecimal monthlyTry, BigDecimal yearlyTry, String benefit) {}
    public record NotificationRes(Long id, String content, boolean seen, Instant createdAt) {}

    // ---- moderation / admin
    public record ReportReq(@NotBlank(message = "Şikâyet nedenini yazın") @Size(max = 1000) String reason) {}
    public record ReportRes(Long id, Long listingId, String listingTitle, String reporterName, String reason,
                            ReportStatus status, String resolutionNote, Instant createdAt) {}
    public record ResolveReq(@NotBlank String action, @Size(max = 1000) String note) {}
    public record AdminQualificationRes(Long id, String title, QualificationStatus status, Long teacherId, String teacherName) {}
    public record StatsRes(long users, long teachers, long students, long activeListings,
                           long pendingRequests, long openReports, long pendingQualifications) {}
}
