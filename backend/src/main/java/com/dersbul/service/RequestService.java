package com.dersbul.service;

import com.dersbul.domain.*;
import com.dersbul.dto.Dtos.*;
import com.dersbul.repo.*;
import com.dersbul.web.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Ders talebi yaşam döngüsü:
 * PENDING -> ACCEPTED | DECLINED (öğretmen), PENDING/ACCEPTED -> CANCELLED (öğrenci), PENDING -> EXPIRED (zamanlayıcı).
 * Talep gönderilince sohbet açılır; öğrenci en fazla 3 zaman önerebilir, öğretmen birini seçerek onaylar ve ders oluşur.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class RequestService {
    private final LessonRequestRepository requests;
    private final ListingRepository listings;
    private final ConversationRepository conversations;
    private final MessageRepository messages;
    private final RequestSlotRepository slots;
    private final LessonSessionRepository sessions;
    private final SchedulingService scheduling;
    private final NotificationService notifications;
    private final Mapper mapper;

    public RequestRes create(User student, RequestReq r) {
        if (student.getRole() != Role.STUDENT)
            throw new ApiException(HttpStatus.FORBIDDEN, "Ders talebi yalnızca öğrenci hesaplarıyla gönderilebilir");
        TeacherListing l = listings.findById(r.listingId())
            .filter(x -> x.getStatus() == ListingStatus.ACTIVE)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "İlan bulunamadı veya yayında değil"));
        boolean offered = switch (r.mode()) {
            case ONLINE -> l.isOnline();
            case TEACHER_HOME -> l.isTeacherHome();
            case STUDENT_HOME -> l.isStudentHome();
        };
        if (!offered) throw new ApiException(HttpStatus.BAD_REQUEST, "Öğretmen bu ders biçimini sunmuyor");
        if (requests.existsByStudentIdAndListingIdAndStatus(student.getId(), l.getId(), RequestStatus.PENDING))
            throw new ApiException(HttpStatus.CONFLICT, "Bu ilana bekleyen bir talebiniz zaten var");

        List<SlotReq> proposed = r.slots() == null ? List.of() : r.slots();
        for (SlotReq s : proposed) {
            if (!s.startsAt().isAfter(Instant.now()) || !s.endsAt().isAfter(s.startsAt())
                || Duration.between(s.startsAt(), s.endsAt()).compareTo(Duration.ofHours(4)) > 0)
                throw new ApiException(HttpStatus.BAD_REQUEST, "Önerdiğiniz zaman geçersiz");
            if (!scheduling.isFree(l.getTeacher().getId(), s.startsAt(), s.endsAt()))
                throw new ApiException(HttpStatus.CONFLICT, "Seçtiğiniz saatlerden biri artık müsait değil, listeyi yenileyin");
        }

        LessonRequest req = new LessonRequest();
        req.setStudent(student); req.setListing(l);
        req.setMessage(r.message().trim()); req.setStudentLevel(r.studentLevel()); req.setMode(r.mode());
        req = requests.save(req);
        for (SlotReq s : proposed) {
            RequestSlot rs = new RequestSlot();
            rs.setRequest(req); rs.setStartsAt(s.startsAt()); rs.setEndsAt(s.endsAt());
            slots.save(rs);
        }
        Conversation c = ensureConversation(req);
        Message first = new Message();
        first.setConversation(c); first.setSender(student); first.setBody(req.getMessage());
        messages.save(first);

        String extra = proposed.isEmpty() ? "" : " (" + proposed.size() + " zaman önerdi)";
        notifications.send(l.getTeacher(), student.getFullName() + " \"" + l.getTitle() + "\" ilanınız için ders talebi gönderdi" + extra + ".",
            "/mesajlar/" + req.getId());
        return mapper.request(req);
    }

    @Transactional(readOnly = true)
    public List<RequestRes> mine(User me) {
        List<LessonRequest> list = me.getRole() == Role.TEACHER
            ? requests.findByListingTeacherIdOrderByCreatedAtDesc(me.getId())
            : requests.findByStudentIdOrderByCreatedAtDesc(me.getId());
        return list.stream().map(mapper::request).toList();
    }

    @Transactional(readOnly = true)
    public RequestRes get(User me, Long id) {
        LessonRequest r = find(id);
        boolean ok = r.getStudent().getId().equals(me.getId()) || r.getListing().getTeacher().getId().equals(me.getId());
        if (!ok) throw new ApiException(HttpStatus.FORBIDDEN, "Bu talebe erişiminiz yok");
        return mapper.request(r);
    }

    /** Öğretmen talebi kabul eder; slotId verilirse seçilen saat için ders hemen oluşturulur. */
    public RequestRes accept(User me, Long id, Long slotId) {
        LessonRequest r = find(id);
        User teacher = r.getListing().getTeacher();
        if (!teacher.getId().equals(me.getId())) throw new ApiException(HttpStatus.FORBIDDEN, "Bu talebi yalnızca ilan sahibi yanıtlayabilir");
        if (r.getStatus() != RequestStatus.PENDING) throw new ApiException(HttpStatus.CONFLICT, "Talep zaten yanıtlanmış");

        RequestSlot chosen = null;
        if (slotId != null) {
            chosen = slots.findById(slotId).filter(s -> s.getRequest().getId().equals(id))
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Geçersiz zaman seçimi"));
            if (!chosen.getStartsAt().isAfter(Instant.now()))
                throw new ApiException(HttpStatus.CONFLICT, "Seçilen zaman geçmiş");
            if (!scheduling.isFree(teacher.getId(), chosen.getStartsAt(), chosen.getEndsAt()))
                throw new ApiException(HttpStatus.CONFLICT, "Bu saat artık müsait değil; başka bir zaman seçin");
        }

        r.setStatus(RequestStatus.ACCEPTED);
        r.setRespondedAt(Instant.now());
        ensureConversation(r);

        if (chosen != null) {
            LessonSession s = new LessonSession();
            s.setRequest(r); s.setStartsAt(chosen.getStartsAt()); s.setEndsAt(chosen.getEndsAt());
            sessions.save(s);
            chosen.setChosen(true);
            notifications.send(r.getStudent(), teacher.getFullName() + " talebinizi onayladı. Ders: " + TextUtil.dateTime(chosen.getStartsAt()) + ".",
                "/mesajlar/" + id);
        } else {
            notifications.send(r.getStudent(), teacher.getFullName() + " talebinizi kabul etti. Mesajlaşıp zamanı netleştirebilirsiniz.",
                "/mesajlar/" + id);
        }
        return mapper.request(r);
    }

    public RequestRes changeStatus(User me, Long id, String status) {
        RequestStatus target;
        try { target = RequestStatus.valueOf(status); }
        catch (IllegalArgumentException e) { throw new ApiException(HttpStatus.BAD_REQUEST, "Geçersiz durum"); }
        if (target == RequestStatus.ACCEPTED) return accept(me, id, null);

        LessonRequest r = find(id);
        User teacher = r.getListing().getTeacher();
        switch (target) {
            case DECLINED -> {
                if (!teacher.getId().equals(me.getId())) throw new ApiException(HttpStatus.FORBIDDEN, "Bu talebi yalnızca ilan sahibi yanıtlayabilir");
                if (r.getStatus() != RequestStatus.PENDING) throw new ApiException(HttpStatus.CONFLICT, "Talep zaten yanıtlanmış");
                r.setStatus(RequestStatus.DECLINED);
                r.setRespondedAt(Instant.now());
                notifications.send(r.getStudent(), teacher.getFullName() + " talebinizi şu an karşılayamayacağını bildirdi.", "/mesajlar/" + id);
            }
            case CANCELLED -> {
                if (!r.getStudent().getId().equals(me.getId())) throw new ApiException(HttpStatus.FORBIDDEN, "Bu talebi yalnızca talep sahibi iptal edebilir");
                if (r.getStatus() != RequestStatus.PENDING && r.getStatus() != RequestStatus.ACCEPTED)
                    throw new ApiException(HttpStatus.CONFLICT, "Bu talep iptal edilemez");
                r.setStatus(RequestStatus.CANCELLED);
                notifications.send(teacher, r.getStudent().getFullName() + " ders talebini iptal etti.", "/mesajlar/" + id);
            }
            default -> throw new ApiException(HttpStatus.BAD_REQUEST, "Bu duruma geçiş yapılamaz");
        }
        return mapper.request(r);
    }

    private LessonRequest find(Long id) {
        return requests.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Talep bulunamadı"));
    }

    private Conversation ensureConversation(LessonRequest r) {
        return conversations.findByRequestId(r.getId()).orElseGet(() -> {
            Conversation c = new Conversation();
            c.setRequest(r);
            return conversations.save(c);
        });
    }
}
