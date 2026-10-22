package com.dersbul.repo;

import com.dersbul.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.*;

public interface LessonRequestRepository extends JpaRepository<LessonRequest, Long> {
    List<LessonRequest> findByStudentIdOrderByCreatedAtDesc(Long studentId);
    List<LessonRequest> findByListingTeacherIdOrderByCreatedAtDesc(Long teacherId);
    boolean existsByStudentIdAndListingIdAndStatus(Long studentId, Long listingId, RequestStatus status);
    List<LessonRequest> findByStatusAndCreatedAtBefore(RequestStatus status, Instant before);
    long countByStatus(RequestStatus status);
}
