package com.dersbul.service;

import com.dersbul.domain.*;
import com.dersbul.dto.Dtos.*;
import com.dersbul.repo.*;
import com.dersbul.web.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

/**
 * DEMO ödeme: gerçek bir ödeme sağlayıcısı (iyzico, PayTR, Stripe vb.) entegre edilene kadar
 * satın alma doğrudan başarılı sayılır.
 */
@Service
@Transactional
public class SubscriptionService {
    private final SubscriptionRepository subs;
    private final ListingRepository listings;
    private final NotificationService notifications;
    private final BigDecimal studentPass, teacherMonthly, teacherYearly;

    public SubscriptionService(SubscriptionRepository subs, ListingRepository listings, NotificationService notifications,
                               @Value("${app.pricing.student-pass-try}") BigDecimal studentPass,
                               @Value("${app.pricing.teacher-pass-monthly-try}") BigDecimal teacherMonthly,
                               @Value("${app.pricing.teacher-pass-yearly-try}") BigDecimal teacherYearly) {
        this.subs = subs; this.listings = listings; this.notifications = notifications;
        this.studentPass = studentPass; this.teacherMonthly = teacherMonthly; this.teacherYearly = teacherYearly;
    }

    public List<PlanRes> plans() {
        return List.of(
            new PlanRes(Plan.STUDENT_PASS, "Öğrenci Pass", studentPass, null,
                "Kabul eden öğretmenlerle mesajlaşın ve ilk dersi planlayın."),
            new PlanRes(Plan.TEACHER_PASS, "Professional Pass", teacherMonthly, teacherYearly,
                "İlanlarınız arama sonuçlarında öne çıkar."));
    }

    @Transactional(readOnly = true)
    public List<SubscriptionRes> mine(User me) {
        Instant now = Instant.now();
        return subs.findByUserIdOrderByEndsAtDesc(me.getId()).stream()
            .map(s -> new SubscriptionRes(s.getId(), s.getPlan(), s.getAmountTry(), s.getStartsAt(), s.getEndsAt(), s.getEndsAt().isAfter(now)))
            .toList();
    }

    public SubscriptionRes buy(User me, BuyReq r) {
        Plan required = me.getRole() == Role.TEACHER ? Plan.TEACHER_PASS : me.getRole() == Role.STUDENT ? Plan.STUDENT_PASS : null;
        if (required == null || required != r.plan())
            throw new ApiException(HttpStatus.FORBIDDEN, "Bu paket hesap türünüz için geçerli değil");

        int months = r.plan() == Plan.TEACHER_PASS && Integer.valueOf(12).equals(r.months()) ? 12 : 1;
        BigDecimal price = r.plan() == Plan.STUDENT_PASS ? studentPass : (months == 12 ? teacherYearly : teacherMonthly);

        // Mevcut abonelik varsa süre onun bitişinden uzatılır.
        Instant base = subs.findByUserIdOrderByEndsAtDesc(me.getId()).stream()
            .filter(s -> s.getPlan() == r.plan()).map(Subscription::getEndsAt)
            .filter(e -> e.isAfter(Instant.now())).findFirst().orElse(Instant.now());

        Subscription s = new Subscription();
        s.setUser(me); s.setPlan(r.plan()); s.setAmountTry(price);
        s.setStartsAt(Instant.now()); s.setEndsAt(base.plus(30L * months, ChronoUnit.DAYS));
        s.setPaymentRef("DEMO-" + UUID.randomUUID());
        s = subs.save(s);

        if (r.plan() == Plan.TEACHER_PASS) listings.setBoosted(me.getId(), true);
        notifications.send(me, r.plan() == Plan.TEACHER_PASS
            ? "Professional Pass etkin. İlanlarınız öne çıkarılıyor."
            : "Öğrenci Pass etkin.", "/pass");
        return new SubscriptionRes(s.getId(), s.getPlan(), s.getAmountTry(), s.getStartsAt(), s.getEndsAt(), true);
    }
}
