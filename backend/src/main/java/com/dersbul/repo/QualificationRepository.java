package com.dersbul.repo;

import com.dersbul.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.*;

public interface QualificationRepository extends JpaRepository<Qualification, Long> {
    List<Qualification> findByTeacherId(Long teacherId);
    List<Qualification> findByStatusOrderByIdAsc(QualificationStatus status);
    long countByStatus(QualificationStatus status);
}
