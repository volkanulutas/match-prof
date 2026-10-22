package com.dersbul.realtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sunucudan istemciye tek yönlü itme kanalı. Mesaj gönderme REST üzerinden yapılır
 * (doğrulama, Pass kuralı ve kayıt tek yerde kalsın diye); WebSocket yalnızca dağıtım içindir.
 */
@Component
@RequiredArgsConstructor
public class ChatSocketHandler extends TextWebSocketHandler {
    private final ObjectMapper mapper;
    private final Map<Long, Set<WebSocketSession>> sessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Long uid = (Long) session.getAttributes().get("userId");
        sessions.computeIfAbsent(uid, k -> ConcurrentHashMap.newKeySet()).add(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long uid = (Long) session.getAttributes().get("userId");
        Set<WebSocketSession> set = sessions.get(uid);
        if (set != null) {
            set.remove(session);
            if (set.isEmpty()) sessions.remove(uid);
        }
    }

    /** İstemci düzenli "ping" yollar: bağlantıyı canlı tutar ve ücretsiz barındırmada uykuya geçişi geciktirir. */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        if ("ping".equals(message.getPayload())) {
            synchronized (session) { session.sendMessage(new TextMessage("pong")); }
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onPush(PushEvent e) {
        Set<WebSocketSession> set = sessions.get(e.userId());
        if (set == null || set.isEmpty()) return;
        final String json;
        try { json = mapper.writeValueAsString(e.payload()); } catch (Exception ex) { return; }
        for (WebSocketSession s : set) {
            try {
                synchronized (s) { if (s.isOpen()) s.sendMessage(new TextMessage(json)); }
            } catch (Exception ignored) {
                // kopan bağlantı afterConnectionClosed ile temizlenir
            }
        }
    }
}
