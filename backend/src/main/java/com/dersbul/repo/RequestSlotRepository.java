package com.dersbul.repo;

import com.dersbul.domain.RequestSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RequestSlotRepository extends JpaRepository<RequestSlot, Long> {
    List<RequestSlot> findByRequestIdOrderByStartsAtAsc(Long requestId);
}
