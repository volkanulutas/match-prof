package com.dersbul.service;

import com.dersbul.domain.*;
import com.dersbul.dto.Dtos.*;
import com.dersbul.repo.*;
import com.dersbul.web.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminService {
    private final UserRepository users;
    private final ListingRepository listings;
    private final LessonRequestRepository requests;
    private final ReportRepository reports;
    private final QualificationRepository quals;
    private final NotificationService notifications;
    private final Mapper mapper;

    @Transactional(readOnly = true)
    public StatsRes stats() {
        return new StatsRes(users.count(), users.countByRole(Role.TEACHER), users.countByRole(Role.STUDENT),
            listings.countByStatus(ListingStatus.ACTIVE), requests.countByStatus(RequestStatus.PENDING),
            reports.countByStatus(ReportStatus.OPEN), quals.countByStatus(QualificationStatus.UNVERIFIED));
    }

    // ---- şikâyetler
    public ReportRes report(User reporter, Long listingId, String reason) {
        TeacherListing l = listings.findById(listingId).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "İlan bulunamadı"));
        Report r = new Report();
        r.setListing(l); r.setReporter(reporter); r.setReason(reason.trim());
        return mapper.report(reports.save(r));
    }

    @Transactional(readOnly = true)
    public List<ReportRes> reports(ReportStatus status) {
        return reports.findByStatusOrderByCreatedAtDesc(status).stream().map(mapper::report).toList();
    }

    public ReportRes resolve(Long id, ResolveReq req) {
        Report r = reports.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Şikâyet bulunamadı"));
        if (r.getStatus() != ReportStatus.OPEN) throw new ApiException(HttpStatus.CONFLICT, "Şikâyet zaten sonuçlandırılmış");
        switch (req.action()) {
            case "SUSPEND" -> {
                r.setStatus(ReportStatus.RESOLVED);
                TeacherListing l = r.getListing();
                l.setStatus(ListingStatus.SUSPENDED);
                notifications.send(l.getTeacher(), "\"" + l.getTitle() + "\" ilanınız kurallara aykırı bulunduğu için askıya alındı.", "/ogretmen");
            }
            case "DISMISS" -> r.setStatus(ReportStatus.DISMISSED);
            default -> throw new ApiException(HttpStatus.BAD_REQUEST, "Geçersiz işlem");
        }
        r.setResolutionNote(req.note());
        return mapper.report(r);
    }

    // ---- ilanlar
    @Transactional(readOnly = true)
    public List<ListingRes> allListings() {
        return listings.findAllByOrderByCreatedAtDesc().stream().map(mapper::listing).toList();
    }

    public ListingRes setListingStatus(Long id, String status) {
        TeacherListing l = listings.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "İlan bulunamadı"));
        ListingStatus s;
        try { s = ListingStatus.valueOf(status); } catch (IllegalArgumentException e) { throw new ApiException(HttpStatus.BAD_REQUEST, "Geçersiz durum"); }
        l.setStatus(s);
        return mapper.listing(l);
    }

    // ---- sertifika doğrulama
    @Transactional(readOnly = true)
    public List<AdminQualificationRes> qualifications(QualificationStatus status) {
        return quals.findByStatusOrderByIdAsc(status).stream()
            .map(q -> new AdminQualificationRes(q.getId(), q.getTitle(), q.getStatus(), q.getTeacher().getId(), q.getTeacher().getFullName()))
            .toList();
    }

    public AdminQualificationRes setQualificationStatus(Long id, String status) {
        Qualification q = quals.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Kayıt bulunamadı"));
        QualificationStatus s;
        try { s = QualificationStatus.valueOf(status); } catch (IllegalArgumentException e) { throw new ApiException(HttpStatus.BAD_REQUEST, "Geçersiz durum"); }
        q.setStatus(s);
        notifications.send(q.getTeacher(), "\"" + q.getTitle() + "\" belgeniz " + (s == QualificationStatus.VERIFIED ? "doğrulandı." : "doğrulanamadı."), "/ogretmen");
        return new AdminQualificationRes(q.getId(), q.getTitle(), q.getStatus(), q.getTeacher().getId(), q.getTeacher().getFullName());
    }
}
