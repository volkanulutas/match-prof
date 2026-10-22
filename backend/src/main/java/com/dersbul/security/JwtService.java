package com.dersbul.security;

import com.dersbul.domain.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {
    private final SecretKey key;
    private final long hours;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.expiration-hours}") long hours) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.hours = hours;
    }

    public String generate(User u) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
            .subject(u.getEmail())
            .claim("role", u.getRole().name())
            .issuedAt(new Date(now))
            .expiration(new Date(now + hours * 3_600_000L))
            .signWith(key)
            .compact();
    }

    public String emailFrom(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload().getSubject();
    }
}
