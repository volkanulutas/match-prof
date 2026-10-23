package com.dersbul.service;

import com.dersbul.domain.*;
import com.dersbul.repo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
@Lazy(false)   // zamanlayıcılar tembel başlatmada da kaydolsun
@RequiredArgsConstructor
public class MaintenanceJobs {
    private final LessonRequestRepository requests;
    private final ListingRepository listings;
    private final SubscriptionRepository subs;
    private final LessonSessionRepository sessions;
    private final NotificationService notifications;

    /** 7 gündür yanıtlanmayan talepler süresi dolmuş sayılır; öğretmen ve öğrenci bilgilendirilir. */
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void expireStaleRequests() {
        Instant cutoff = Instant.now().minus(7, ChronoUnit.DAYS);
        for (LessonRequest r : requests.findByStatusAndCreatedAtBefore(RequestStatus.PENDING, cutoff)) {
            r.setStatus(RequestStatus.EXPIRED);
            notifications.send(r.getListing().getTeacher(), "Yanıtlamadığınız bir ders talebinin süresi doldu. Hızlı yanıt vermek görünürlüğünüzü korur.", "/talepler");
            notifications.send(r.getStudent(), "Talebiniz yanıtlanmadığı için sona erdi. Başka öğretmenlere de başvurabilirsiniz.", "/talepler");
        }
    }

    /** Dersten 24 saat önce her iki tarafa uygulama içi hatırlatma gönderir. */
    @Scheduled(cron = "0 */10 * * * *")
    @Transactional
    public void sendLessonReminders() {
        Instant now = Instant.now();
        for (LessonSession s : sessions.findByStatusAndReminderSentFalseAndStartsAtBefore(SessionStatus.SCHEDULED, now.plus(24, ChronoUnit.HOURS))) {
            s.setReminderSent(true);
            if (s.getStartsAt().isBefore(now)) continue;
            String text = "Hatırlatma: \"" + s.getRequest().getListing().getTitle() + "\" dersiniz " + TextUtil.dateTime(s.getStartsAt()) + " tarihinde.";
            notifications.send(s.getRequest().getStudent(), text, "/dersler");
            notifications.send(s.getRequest().getListing().getTeacher(), text, "/dersler");
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
