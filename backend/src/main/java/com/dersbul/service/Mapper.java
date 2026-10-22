package com.dersbul.service;

import com.dersbul.domain.*;
import com.dersbul.dto.Dtos.*;
import com.dersbul.repo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
public class Mapper {
    private final QualificationRepository quals;
    private final SubscriptionRepository subs;
    private final ConversationRepository conversations;
    private final ReviewRepository reviews;
    private final RequestSlotRepository requestSlots;

    public UserRes user(User u) {
        boolean pass = false;
        if (u.getRole() != Role.ADMIN) {
            Plan plan = u.getRole() == Role.TEACHER ? Plan.TEACHER_PASS : Plan.STUDENT_PASS;
            pass = subs.existsByUserIdAndPlanAndEndsAtAfter(u.getId(), plan, Instant.now());
        }
        return new UserRes(u.getId(), u.getEmail(), u.getFullName(), u.getRole(), u.getCity(), pass);
    }

    public QualificationRes qualification(Qualification q) {
        return new QualificationRes(q.getId(), q.getTitle(), q.getStatus());
    }

    public ListingRes listing(TeacherListing l) {
        List<QualificationRes> qs = quals.findByTeacherId(l.getTeacher().getId()).stream()
            .filter(q -> q.getStatus() != QualificationStatus.REJECTED)
            .map(this::qualification).toList();
        return new ListingRes(l.getId(), l.getTeacher().getId(), l.getTeacher().getFullName(),
            l.getCategory().getId(), l.getCategory().getName(), l.getTitle(), l.getDescription(),
            l.getHourlyRate(), l.getCity(), l.isOnline(), l.isTeacherHome(), l.isStudentHome(),
            l.isFirstLessonFree(), l.getExperienceYears(), l.getEducation(), l.getLanguages(),
            l.getStatus(), l.isBoosted(), l.getAvgRating(), l.getReviewCount(), qs);
    }

    public RequestRes request(LessonRequest r) {
        Long convId = conversations.findByRequestId(r.getId()).map(Conversation::getId).orElse(null);
        User t = r.getListing().getTeacher();
        return new RequestRes(r.getId(), r.getListing().getId(), r.getListing().getTitle(),
            r.getStudent().getId(), r.getStudent().getFullName(), t.getId(), t.getFullName(),
            r.getMessage(), r.getStudentLevel(), r.getMode(), r.getStatus(), r.getCreatedAt(), convId,
            requestSlots.findByRequestIdOrderByStartsAtAsc(r.getId()).stream()
                .map(s -> new SlotRes(s.getId(), s.getStartsAt(), s.getEndsAt(), s.isChosen())).toList());
    }

    public SessionRes session(LessonSession s) {
        LessonRequest r = s.getRequest();
        return new SessionRes(s.getId(), r.getId(), r.getListing().getId(), r.getListing().getTitle(),
            r.getListing().getTeacher().getFullName(), r.getStudent().getFullName(),
            s.getStartsAt(), s.getEndsAt(), s.getStatus(), reviews.existsBySessionId(s.getId()));
    }

    public ReportRes report(Report r) {
        return new ReportRes(r.getId(), r.getListing().getId(), r.getListing().getTitle(),
            r.getReporter().getFullName(), r.getReason(), r.getStatus(), r.getResolutionNote(), r.getCreatedAt());
    }
}
