package com.dersbul.repo;

import com.dersbul.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.*;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    boolean existsByUserIdAndPlanAndEndsAtAfter(Long userId, Plan plan, Instant now);
    List<Subscription> findByUserIdOrderByEndsAtDesc(Long userId);
}
