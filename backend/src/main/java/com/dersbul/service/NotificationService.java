package com.dersbul.service;

import com.dersbul.domain.Notification;
import com.dersbul.domain.User;
import com.dersbul.dto.Dtos.NotificationRes;
import com.dersbul.realtime.PushEvent;
import com.dersbul.repo.NotificationRepository;
import org.springframework.context.ApplicationEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationService {
    private final NotificationRepository repo;
    private final ApplicationEventPublisher events;

    public void send(User to, String content) {
        Notification n = new Notification();
        n.setUser(to);
        n.setContent(content);
        repo.save(n);
        events.publishEvent(new PushEvent(to.getId(), Map.of("type", "notification", "content", content)));
        // Üretimde burada e-posta / SMS / push sağlayıcısı çağrılır.
    }

    @Transactional(readOnly = true)
    public List<NotificationRes> list(User me) {
        return repo.findTop50ByUserIdOrderByCreatedAtDesc(me.getId()).stream()
            .map(n -> new NotificationRes(n.getId(), n.getContent(), n.isSeen(), n.getCreatedAt())).toList();
    }

    @Transactional(readOnly = true)
    public long unread(User me) { return repo.countByUserIdAndSeenFalse(me.getId()); }

    public void markAllSeen(User me) { repo.markAllSeen(me.getId()); }
}
