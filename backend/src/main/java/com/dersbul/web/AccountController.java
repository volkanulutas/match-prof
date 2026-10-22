package com.dersbul.web;

import com.dersbul.domain.User;
import com.dersbul.dto.Dtos.*;
import com.dersbul.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

/** Abonelikler ve bildirimler. */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AccountController {
    private final SubscriptionService subscriptions;
    private final NotificationService notifications;

    @GetMapping("/subscriptions/plans") public List<PlanRes> plans() { return subscriptions.plans(); }
    @GetMapping("/subscriptions/me") public List<SubscriptionRes> mine(@AuthenticationPrincipal User me) { return subscriptions.mine(me); }

    @PostMapping("/subscriptions") @ResponseStatus(HttpStatus.CREATED)
    public SubscriptionRes buy(@AuthenticationPrincipal User me, @Valid @RequestBody BuyReq r) { return subscriptions.buy(me, r); }

    @GetMapping("/notifications") public List<NotificationRes> list(@AuthenticationPrincipal User me) { return notifications.list(me); }
    @GetMapping("/notifications/unread-count") public Map<String, Long> unread(@AuthenticationPrincipal User me) { return Map.of("count", notifications.unread(me)); }
    @PostMapping("/notifications/{id}/read") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void readOne(@AuthenticationPrincipal User me, @PathVariable Long id) { notifications.markSeen(me, id); }
    @PostMapping("/notifications/read-all") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void readAll(@AuthenticationPrincipal User me) { notifications.markAllSeen(me); }
}
