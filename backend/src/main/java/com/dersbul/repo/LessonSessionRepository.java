package com.dersbul.repo;

import com.dersbul.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.*;

public interface LessonSessionRepository extends JpaRepository<LessonSession, Long> {
    List<LessonSession> findByRequestStudentIdOrderByStartsAtDesc(Long studentId);
    List<LessonSession> findByRequestListingTeacherIdOrderByStartsAtDesc(Long teacherId);

    @Query("select count(s) > 0 from LessonSession s where s.request.listing.teacher.id = :tid "
         + "and s.status = com.dersbul.domain.SessionStatus.SCHEDULED and s.startsAt < :end and s.endsAt > :start")
    boolean hasConflict(@Param("tid") Long teacherId, @Param("start") Instant start, @Param("end") Instant end);
}
