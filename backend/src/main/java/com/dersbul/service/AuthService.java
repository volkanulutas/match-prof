package com.dersbul.service;

import com.dersbul.domain.*;
import com.dersbul.dto.Dtos.*;
import com.dersbul.repo.UserRepository;
import com.dersbul.security.JwtService;
import com.dersbul.web.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final Mapper mapper;

    public AuthRes register(RegisterReq r) {
        if (r.role() == Role.ADMIN) throw new ApiException(HttpStatus.BAD_REQUEST, "Geçersiz hesap türü");
        if (users.existsByEmailIgnoreCase(r.email()))
            throw new ApiException(HttpStatus.CONFLICT, "Bu e-posta ile zaten bir hesap var");
        User u = new User();
        u.setEmail(r.email().trim().toLowerCase());
        u.setPasswordHash(encoder.encode(r.password()));
        u.setFullName(r.fullName().trim());
        u.setRole(r.role());
        u.setCity(r.city());
        users.save(u);
        return new AuthRes(jwt.generate(u), mapper.user(u));
    }

    @Transactional(readOnly = true)
    public AuthRes login(LoginReq r) {
        User u = users.findByEmailIgnoreCase(r.email().trim())
            .filter(x -> encoder.matches(r.password(), x.getPasswordHash()))
            .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "E-posta veya şifre hatalı"));
        if (!u.isActive()) throw new ApiException(HttpStatus.FORBIDDEN, "Hesap askıya alınmış");
        return new AuthRes(jwt.generate(u), mapper.user(u));
    }
}
