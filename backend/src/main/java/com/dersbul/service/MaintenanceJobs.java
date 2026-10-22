package com.dersbul.service;

import com.dersbul.domain.*;
import com.dersbul.repo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
@RequiredArgsConstructor
public class MaintenanceJobs {
    private final LessonRequestRepository requests;
    private final ListingRepository listings;
    private final SubscriptionRepository subs;
    private final NotificationService notifications;

    /** 7 gündür yanıtlanmayan talepler süresi dolmuş sayılır; öğretmen ve öğrenci bilgilendirilir. */
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void expireStaleRequests() {
        Instant cutoff = Instant.now().minus(7, ChronoUnit.DAYS);
        for (LessonRequest r : requests.findByStatusAndCreatedAtBefore(RequestStatus.PENDING, cutoff)) {
            r.setStatus(RequestStatus.EXPIRED);
            notifications.send(r.getListing().getTeacher(), "Yanıtlamadığınız bir ders talebinin süresi doldu. Hızlı yanıt vermek görünürlüğünüzü korur.");
            notifications.send(r.getStudent(), "Talebiniz yanıtlanmadığı için sona erdi. Başka öğretmenlere de başvurabilirsiniz.");
        }
    }

    /** Professional Pass süresi biten öğretmenlerin ilanları öne çıkarılmaz. */
    @Scheduled(cron = "0 5 * * * *")
    @Transactional
    public void expireBoosts() {
        for (TeacherListing l : listings.findByBoostedTrue()) {
            boolean active = subs.existsByUserIdAndPlanAndEndsAtAfter(l.getTeacher().getId(), Plan.TEACHER_PASS, Instant.now());
            if (!active) l.setBoosted(false);
        }
    }
}
