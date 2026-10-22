package com.dersbul.repo;

import com.dersbul.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.*;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {
    Optional<Conversation> findByRequestId(Long requestId);
}
