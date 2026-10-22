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

@Service
@RequiredArgsConstructor
@Transactional
public class SessionService {
    private final LessonSessionRepository sessions;
    private final LessonRequestRepository requests;
    private final NotificationService notifications;
    private final Mapper mapper;

    public SessionRes schedule(User teacher, SessionReq r) {
        LessonRequest req = requests.findById(r.requestId()).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Talep bulunamadı"));
        User owner = req.getListing().getTeacher();
        if (!owner.getId().equals(teacher.getId())) throw new ApiException(HttpStatus.FORBIDDEN, "Ders yalnızca ilan sahibi tarafından planlanabilir");
        if (req.getStatus() != RequestStatus.ACCEPTED) throw new ApiException(HttpStatus.CONFLICT, "Önce talebi kabul etmelisiniz");
        if (!r.startsAt().isAfter(Instant.now())) throw new ApiException(HttpStatus.BAD_REQUEST, "Ders zamanı gelecekte olmalı");
        if (!r.endsAt().isAfter(r.startsAt())) throw new ApiException(HttpStatus.BAD_REQUEST, "Bitiş, başlangıçtan sonra olmalı");
        if (Duration.between(r.startsAt(), r.endsAt()).compareTo(Duration.ofHours(4)) > 0)
            throw new ApiException(HttpStatus.BAD_REQUEST, "Tek ders en fazla 4 saat olabilir");
        if (sessions.hasConflict(teacher.getId(), r.startsAt(), r.endsAt()))
            throw new ApiException(HttpStatus.CONFLICT, "Bu saatte başka bir dersiniz var");

        LessonSession s = new LessonSession();
        s.setRequest(req); s.setStartsAt(r.startsAt()); s.setEndsAt(r.endsAt());
        s = sessions.save(s);
        notifications.send(req.getStudent(), teacher.getFullName() + " ders planladı: " + TextUtil.dateTime(s.getStartsAt()) + ".", "/dersler");
        return mapper.session(s);
    }

    @Transactional(readOnly = true)
    public List<SessionRes> mine(User me) {
        List<LessonSession> list = me.getRole() == Role.TEACHER
            ? sessions.findByRequestListingTeacherIdOrderByStartsAtDesc(me.getId())
            : sessions.findByRequestStudentIdOrderByStartsAtDesc(me.getId());
        return list.stream().map(mapper::session).toList();
    }

    public SessionRes changeStatus(User me, Long id, String status) {
        LessonSession s = sessions.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Ders bulunamadı"));
        LessonRequest r = s.getRequest();
        boolean isTeacher = r.getListing().getTeacher().getId().equals(me.getId());
        boolean isStudent = r.getStudent().getId().equals(me.getId());
        if (!isTeacher && !isStudent) throw new ApiException(HttpStatus.FORBIDDEN, "Bu derse erişiminiz yok");
        if (s.getStatus() != SessionStatus.SCHEDULED) throw new ApiException(HttpStatus.CONFLICT, "Bu ders artık değiştirilemez");

        switch (status) {
            case "COMPLETED" -> {
                if (!isTeacher) throw new ApiException(HttpStatus.FORBIDDEN, "Dersi yalnızca öğretmen tamamlandı işaretleyebilir");
                if (s.getStartsAt().isAfter(Instant.now())) throw new ApiException(HttpStatus.CONFLICT, "Ders henüz başlamadı");
                s.setStatus(SessionStatus.COMPLETED);
                notifications.send(r.getStudent(), "Ders tamamlandı. Öğretmeninizi değerlendirebilirsiniz.", "/dersler");
            }
            case "CANCELLED" -> {
                s.setStatus(SessionStatus.CANCELLED);
                User other = isTeacher ? r.getStudent() : r.getListing().getTeacher();
                notifications.send(other, me.getFullName() + " planlanan dersi iptal etti.", "/dersler");
            }
            default -> throw new ApiException(HttpStatus.BAD_REQUEST, "Geçersiz durum");
        }
        return mapper.session(s);
    }
}
