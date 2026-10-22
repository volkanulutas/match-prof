package com.dersbul.service;

import com.dersbul.domain.*;
import com.dersbul.dto.Dtos.*;
import com.dersbul.repo.*;
import com.dersbul.web.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.List;

/**
 * Ders talebi yaşam döngüsü:
 * PENDING -> ACCEPTED | DECLINED (öğretmen), PENDING/ACCEPTED -> CANCELLED (öğrenci), PENDING -> EXPIRED (zamanlayıcı).
 */
@Service
@RequiredArgsConstructor
@Transactional
public class RequestService {
    private final LessonRequestRepository requests;
    private final ListingRepository listings;
    private final ConversationRepository conversations;
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

        LessonRequest req = new LessonRequest();
        req.setStudent(student); req.setListing(l);
        req.setMessage(r.message().trim()); req.setStudentLevel(r.studentLevel()); req.setMode(r.mode());
        req = requests.save(req);
        notifications.send(l.getTeacher(), student.getFullName() + " size \"" + l.getTitle() + "\" ilanı için ders talebi gönderdi.");
        return mapper.request(req);
    }

    @Transactional(readOnly = true)
    public List<RequestRes> mine(User me) {
        List<LessonRequest> list = me.getRole() == Role.TEACHER
            ? requests.findByListingTeacherIdOrderByCreatedAtDesc(me.getId())
            : requests.findByStudentIdOrderByCreatedAtDesc(me.getId());
        return list.stream().map(mapper::request).toList();
    }

    public RequestRes changeStatus(User me, Long id, String status) {
        LessonRequest r = requests.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Talep bulunamadı"));
        RequestStatus target;
        try { target = RequestStatus.valueOf(status); }
        catch (IllegalArgumentException e) { throw new ApiException(HttpStatus.BAD_REQUEST, "Geçersiz durum"); }

        User teacher = r.getListing().getTeacher();
        switch (target) {
            case ACCEPTED, DECLINED -> {
                if (!teacher.getId().equals(me.getId())) throw new ApiException(HttpStatus.FORBIDDEN, "Bu talebi yalnızca ilan sahibi yanıtlayabilir");
                if (r.getStatus() != RequestStatus.PENDING) throw new ApiException(HttpStatus.CONFLICT, "Talep zaten yanıtlanmış");
                r.setStatus(target);
                r.setRespondedAt(Instant.now());
                if (target == RequestStatus.ACCEPTED) {
                    Conversation c = new Conversation();
                    c.setRequest(r);
                    conversations.save(c);
                    notifications.send(r.getStudent(), teacher.getFullName() + " talebinizi kabul etti. Mesajlaşıp ilk dersi planlayabilirsiniz.");
                } else {
                    notifications.send(r.getStudent(), teacher.getFullName() + " talebinizi şu an karşılayamayacağını bildirdi.");
                }
            }
            case CANCELLED -> {
                if (!r.getStudent().getId().equals(me.getId())) throw new ApiException(HttpStatus.FORBIDDEN, "Bu talebi yalnızca talep sahibi iptal edebilir");
                if (r.getStatus() != RequestStatus.PENDING && r.getStatus() != RequestStatus.ACCEPTED)
                    throw new ApiException(HttpStatus.CONFLICT, "Bu talep iptal edilemez");
                r.setStatus(RequestStatus.CANCELLED);
                notifications.send(teacher, r.getStudent().getFullName() + " ders talebini iptal etti.");
            }
            default -> throw new ApiException(HttpStatus.BAD_REQUEST, "Bu duruma geçiş yapılamaz");
        }
        return mapper.request(r);
    }
}
