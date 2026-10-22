package com.dersbul.service;

import com.dersbul.domain.Notification;
import com.dersbul.domain.User;
import com.dersbul.dto.Dtos.NotificationRes;
import com.dersbul.realtime.PushEvent;
import com.dersbul.repo.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationService {
    private final NotificationRepository repo;
    private final ApplicationEventPublisher events;

    /** link: tıklanınca açılacak uygulama içi yol (ör. /mesajlar/12); yoksa null. */
    public void send(User to, String content, String link) {
        Notification n = new Notification();
        n.setUser(to);
        n.setContent(content);
        n.setLink(link);
        repo.save(n);
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "notification");
        payload.put("id", n.getId());
        payload.put("content", content);
        payload.put("link", link == null ? "" : link);
        payload.put("createdAt", n.getCreatedAt().toString());
        events.publishEvent(new PushEvent(to.getId(), payload));
        // Üretimde burada e-posta / SMS / push sağlayıcısı çağrılır.
    }

    public void send(User to, String content) { send(to, content, null); }

    @Transactional(readOnly = true)
    public List<NotificationRes> list(User me) {
        return repo.findTop50ByUserIdOrderByCreatedAtDesc(me.getId()).stream()
            .map(n -> new NotificationRes(n.getId(), n.getContent(), n.getLink(), n.isSeen(), n.getCreatedAt())).toList();
    }

    @Transactional(readOnly = true)
    public long unread(User me) { return repo.countByUserIdAndSeenFalse(me.getId()); }

    public void markAllSeen(User me) { repo.markAllSeen(me.getId()); }

    public void markSeen(User me, Long id) {
        repo.findById(id).filter(n -> n.getUser().getId().equals(me.getId())).ifPresent(n -> n.setSeen(true));
    }
}
