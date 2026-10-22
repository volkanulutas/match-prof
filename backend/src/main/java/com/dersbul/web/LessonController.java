package com.dersbul.web;

import com.dersbul.domain.User;
import com.dersbul.dto.Dtos.*;
import com.dersbul.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/** Talepler, mesajlar, dersler, yorumlar, şikâyetler. */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class LessonController {
    private final RequestService requests;
    private final MessagingService messaging;
    private final SessionService sessions;
    private final ReviewService reviews;
    private final AdminService admin;

    @PostMapping("/requests") @ResponseStatus(HttpStatus.CREATED)
    public RequestRes createRequest(@AuthenticationPrincipal User me, @Valid @RequestBody RequestReq r) { return requests.create(me, r); }

    @GetMapping("/requests/mine")
    public List<RequestRes> myRequests(@AuthenticationPrincipal User me) { return requests.mine(me); }

    @PatchMapping("/requests/{id}/status")
    public RequestRes requestStatus(@AuthenticationPrincipal User me, @PathVariable Long id, @Valid @RequestBody StatusReq r) {
        return requests.changeStatus(me, id, r.status());
    }

    @GetMapping("/requests/{id}/messages")
    public List<MessageRes> messages(@AuthenticationPrincipal User me, @PathVariable Long id) { return messaging.list(me, id); }

    @PostMapping("/requests/{id}/messages") @ResponseStatus(HttpStatus.CREATED)
    public MessageRes send(@AuthenticationPrincipal User me, @PathVariable Long id, @Valid @RequestBody MessageReq r) {
        return messaging.send(me, id, r.body());
    }

    @PostMapping("/sessions") @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasRole('TEACHER')")
    public SessionRes schedule(@AuthenticationPrincipal User me, @Valid @RequestBody SessionReq r) { return sessions.schedule(me, r); }

    @GetMapping("/sessions/mine")
    public List<SessionRes> mySessions(@AuthenticationPrincipal User me) { return sessions.mine(me); }

    @PatchMapping("/sessions/{id}/status")
    public SessionRes sessionStatus(@AuthenticationPrincipal User me, @PathVariable Long id, @Valid @RequestBody StatusReq r) {
        return sessions.changeStatus(me, id, r.status());
    }

    @PostMapping("/reviews") @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasRole('STUDENT')")
    public ReviewRes review(@AuthenticationPrincipal User me, @Valid @RequestBody ReviewReq r) { return reviews.create(me, r); }

    @PostMapping("/listings/{id}/reports") @ResponseStatus(HttpStatus.CREATED)
    public ReportRes report(@AuthenticationPrincipal User me, @PathVariable Long id, @Valid @RequestBody ReportReq r) {
        return admin.report(me, id, r.reason());
    }
}
