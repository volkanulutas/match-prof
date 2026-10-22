package com.dersbul.web;

import com.dersbul.domain.*;
import com.dersbul.dto.Dtos.*;
import com.dersbul.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {
    private final AdminService admin;

    @GetMapping("/stats") public StatsRes stats() { return admin.stats(); }

    @GetMapping("/reports")
    public List<ReportRes> reports(@RequestParam(defaultValue = "OPEN") ReportStatus status) { return admin.reports(status); }

    @PatchMapping("/reports/{id}")
    public ReportRes resolve(@PathVariable Long id, @Valid @RequestBody ResolveReq r) { return admin.resolve(id, r); }

    @GetMapping("/listings") public List<ListingRes> listings() { return admin.allListings(); }

    @PatchMapping("/listings/{id}/status")
    public ListingRes listingStatus(@PathVariable Long id, @Valid @RequestBody StatusReq r) { return admin.setListingStatus(id, r.status()); }

    @GetMapping("/qualifications")
    public List<AdminQualificationRes> quals(@RequestParam(defaultValue = "UNVERIFIED") QualificationStatus status) { return admin.qualifications(status); }

    @PatchMapping("/qualifications/{id}")
    public AdminQualificationRes qualStatus(@PathVariable Long id, @Valid @RequestBody StatusReq r) { return admin.setQualificationStatus(id, r.status()); }
}
