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

@RestController
@RequestMapping("/api/teacher")
@PreAuthorize("hasRole('TEACHER')")
@RequiredArgsConstructor
public class TeacherController {
    private final ListingService listings;
    private final TeacherProfileService profile;

    @GetMapping("/listings") public List<ListingRes> mine(@AuthenticationPrincipal User me) { return listings.mine(me); }

    @PostMapping("/listings") @ResponseStatus(HttpStatus.CREATED)
    public ListingRes create(@AuthenticationPrincipal User me, @Valid @RequestBody ListingReq r) { return listings.create(me, r); }

    @PutMapping("/listings/{id}")
    public ListingRes update(@AuthenticationPrincipal User me, @PathVariable Long id, @Valid @RequestBody ListingReq r) { return listings.update(me, id, r); }

    @PatchMapping("/listings/{id}/status")
    public ListingRes status(@AuthenticationPrincipal User me, @PathVariable Long id, @Valid @RequestBody StatusReq r) { return listings.setStatus(me, id, r.status()); }

    @GetMapping("/availability") public List<AvailabilityRes> availability(@AuthenticationPrincipal User me) { return profile.availabilityOf(me.getId()); }

    @PostMapping("/availability") @ResponseStatus(HttpStatus.CREATED)
    public AvailabilityRes addAvailability(@AuthenticationPrincipal User me, @Valid @RequestBody AvailabilityReq r) { return profile.addAvailability(me, r); }

    @DeleteMapping("/availability/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAvailability(@AuthenticationPrincipal User me, @PathVariable Long id) { profile.deleteAvailability(me, id); }

    @GetMapping("/qualifications") public List<QualificationRes> quals(@AuthenticationPrincipal User me) { return profile.myQualifications(me); }

    @PostMapping("/qualifications") @ResponseStatus(HttpStatus.CREATED)
    public QualificationRes addQual(@AuthenticationPrincipal User me, @Valid @RequestBody QualificationReq r) { return profile.addQualification(me, r); }

    @DeleteMapping("/qualifications/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteQual(@AuthenticationPrincipal User me, @PathVariable Long id) { profile.deleteQualification(me, id); }
}
