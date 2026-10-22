package com.dersbul.service;

import com.dersbul.domain.*;
import com.dersbul.dto.Dtos.*;
import com.dersbul.realtime.PushEvent;
import com.dersbul.repo.*;
import org.springframework.context.ApplicationEventPublisher;
import com.dersbul.web.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Sohbet, talep kabul edilince açılır. Öğrencinin mesaj gönderebilmesi için aktif Öğrenci Pass gerekir.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class MessagingService {
    private final LessonRequestRepository requests;
    private final ConversationRepository conversations;
    private final MessageRepository messages;
    private final SubscriptionRepository subs;
    private final NotificationService notifications;
    private final ApplicationEventPublisher events;

    @Transactional(readOnly = true)
    public List<MessageRes> list(User me, Long requestId) {
        participantRequest(me, requestId);
        return conversations.findByRequestId(requestId)
            .map(c -> messages.findByConversationIdOrderByCreatedAtAsc(c.getId()).stream().map(this::map).toList())
            .orElse(List.of());
    }

    public MessageRes send(User me, Long requestId, String body) {
        LessonRequest r = participantRequest(me, requestId);
        Conversation c = conversations.findByRequestId(requestId)
            .orElseThrow(() -> new ApiException(HttpStatus.CONFLICT, "Sohbet, talep kabul edildikten sonra açılır"));
        if (me.getRole() == Role.STUDENT
            && !subs.existsByUserIdAndPlanAndEndsAtAfter(me.getId(), Plan.STUDENT_PASS, Instant.now()))
            throw new ApiException(HttpStatus.PAYMENT_REQUIRED, "Mesaj göndermek için Öğrenci Pass gerekli");
        Message m = new Message();
        m.setConversation(c); m.setSender(me); m.setBody(body.trim());
        m = messages.save(m);
        User other = me.getId().equals(r.getStudent().getId()) ? r.getListing().getTeacher() : r.getStudent();
        notifications.send(other, me.getFullName() + " size yeni bir mesaj gönderdi.");
        MessageRes out = map(m);
        Map<String, Object> payload = Map.of("type", "message", "requestId", requestId, "message", out);
        events.publishEvent(new PushEvent(r.getStudent().getId(), payload));
        events.publishEvent(new PushEvent(r.getListing().getTeacher().getId(), payload));
        return out;
    }

    private LessonRequest participantRequest(User me, Long requestId) {
        LessonRequest r = requests.findById(requestId).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Talep bulunamadı"));
        boolean ok = r.getStudent().getId().equals(me.getId()) || r.getListing().getTeacher().getId().equals(me.getId());
        if (!ok) throw new ApiException(HttpStatus.FORBIDDEN, "Bu sohbete erişiminiz yok");
        return r;
    }

    private MessageRes map(Message m) {
        return new MessageRes(m.getId(), m.getSender().getId(), m.getSender().getFullName(), m.getBody(), m.getCreatedAt());
    }
}
