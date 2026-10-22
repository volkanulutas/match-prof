package com.dersbul.service;

import com.dersbul.domain.*;
import com.dersbul.dto.Dtos.*;
import com.dersbul.repo.*;
import com.dersbul.web.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

/** Yorum yalnızca tamamlanmış derse, öğrenci tarafından ve ders başına bir kez yazılabilir. */
@Service
@RequiredArgsConstructor
@Transactional
public class ReviewService {
    private final ReviewRepository reviews;
    private final LessonSessionRepository sessions;
    private final ListingRepository listings;
    private final NotificationService notifications;

    @Transactional(readOnly = true)
    public List<ReviewRes> forListing(Long listingId) {
        return reviews.findByListingIdOrderByCreatedAtDesc(listingId).stream()
            .map(r -> new ReviewRes(r.getId(), r.getStudent().getFullName(), r.getRating(), r.getBody(), r.getCreatedAt())).toList();
    }

    public ReviewRes create(User student, ReviewReq r) {
        LessonSession s = sessions.findById(r.sessionId()).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Ders bulunamadı"));
        if (!s.getRequest().getStudent().getId().equals(student.getId()))
            throw new ApiException(HttpStatus.FORBIDDEN, "Yalnızca dersi alan öğrenci yorum yazabilir");
        if (s.getStatus() != SessionStatus.COMPLETED)
            throw new ApiException(HttpStatus.CONFLICT, "Yorum, ders tamamlandıktan sonra yazılabilir");
        if (reviews.existsBySessionId(s.getId()))
            throw new ApiException(HttpStatus.CONFLICT, "Bu ders için zaten yorum yazdınız");

        TeacherListing l = s.getRequest().getListing();
        Review rv = new Review();
        rv.setListing(l); rv.setStudent(student); rv.setSession(s);
        rv.setRating(r.rating()); rv.setBody(r.body());
        rv = reviews.save(rv);
        reviews.flush();
        l.setAvgRating(Math.round(reviews.averageRating(l.getId()) * 10.0) / 10.0);
        l.setReviewCount((int) reviews.countByListingId(l.getId()));
        notifications.send(l.getTeacher(), student.getFullName() + " size " + r.rating() + " yıldız verdi.");
        return new ReviewRes(rv.getId(), student.getFullName(), rv.getRating(), rv.getBody(), rv.getCreatedAt());
    }
}
