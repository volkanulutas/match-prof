package com.dersbul.realtime;

import org.springframework.stereotype.Service;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Kısa ömürlü, tek kullanımlık WebSocket bileti. JWT'nin URL'de (log'larda) görünmesini önler.
 * Bellek içi çalışır: birden fazla sunucu örneği için Redis gibi ortak bir depo gerekir.
 */
@Service
public class WsTicketService {
    private record Ticket(Long userId, Instant expires) {}

    private final ConcurrentMap<String, Ticket> tickets = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();

    public String issue(Long userId) {
        tickets.values().removeIf(t -> t.expires().isBefore(Instant.now()));
        byte[] bytes = new byte[24];
        random.nextBytes(bytes);
        String id = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        tickets.put(id, new Ticket(userId, Instant.now().plusSeconds(30)));
        return id;
    }

    public Optional<Long> consume(String id) {
        if (id == null) return Optional.empty();
        Ticket t = tickets.remove(id);
        if (t == null || t.expires().isBefore(Instant.now())) return Optional.empty();
        return Optional.of(t.userId());
    }
}
