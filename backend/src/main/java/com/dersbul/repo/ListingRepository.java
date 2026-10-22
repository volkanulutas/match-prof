package com.dersbul.repo;

import com.dersbul.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.*;

public interface ListingRepository extends JpaRepository<TeacherListing, Long>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<TeacherListing> {
    List<TeacherListing> findByTeacherIdOrderByCreatedAtDesc(Long teacherId);
    List<TeacherListing> findByBoostedTrue();
    List<TeacherListing> findAllByOrderByCreatedAtDesc();
    long countByStatus(ListingStatus status);

    @Query("select l.category.id, count(l) from TeacherListing l where l.status = com.dersbul.domain.ListingStatus.ACTIVE group by l.category.id")
    List<Object[]> countActiveByCategory();

    @Modifying
    @Query("update TeacherListing l set l.boosted = :b where l.teacher.id = :tid")
    void setBoosted(@Param("tid") Long teacherId, @Param("b") boolean boosted);
}
