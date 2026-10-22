package com.dersbul.repo;

import com.dersbul.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.*;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByListingIdOrderByCreatedAtDesc(Long listingId);
    boolean existsBySessionId(Long sessionId);
    long countByListingId(Long listingId);

    @Query("select coalesce(avg(r.rating), 0) from Review r where r.listing.id = :id")
    double averageRating(@Param("id") Long listingId);
}
