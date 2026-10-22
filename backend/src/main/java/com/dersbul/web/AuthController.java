package com.dersbul.web;

import com.dersbul.domain.User;
import com.dersbul.dto.Dtos.*;
import com.dersbul.realtime.WsTicketService;
import com.dersbul.service.*;
import java.util.Map;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService auth;
    private final Mapper mapper;
    private final WsTicketService wsTickets;

    @PostMapping("/register") public AuthRes register(@Valid @RequestBody RegisterReq r) { return auth.register(r); }
    @PostMapping("/login") public AuthRes login(@Valid @RequestBody LoginReq r) { return auth.login(r); }
    /** WebSocket bağlantısı için 30 sn geçerli, tek kullanımlık bilet. */
    @PostMapping("/ws-ticket") public Map<String, String> ticket(@AuthenticationPrincipal User me) { return Map.of("ticket", wsTickets.issue(me.getId())); }
    @GetMapping("/me") public UserRes me(@AuthenticationPrincipal User me) { return mapper.user(me); }
}
