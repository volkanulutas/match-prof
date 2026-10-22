package com.dersbul.service;

import com.dersbul.domain.*;
import com.dersbul.dto.Dtos.*;
import com.dersbul.repo.*;
import com.dersbul.web.ApiException;
import jakarta.persistence.criteria.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class ListingService {
    private final ListingRepository listings;
    private final CategoryRepository categories;
    private final SubscriptionRepository subs;
    private final Mapper mapper;

    @Transactional(readOnly = true)
    public List<CategoryRes> categories() {
        Map<Long, Long> counts = new HashMap<>();
        for (Object[] row : listings.countActiveByCategory()) counts.put((Long) row[0], (Long) row[1]);
        return categories.findAllByOrderByGroupNameAscNameAsc().stream()
            .map(c -> new CategoryRes(c.getId(), c.getName(), c.getSlug(), c.getGroupName(), counts.getOrDefault(c.getId(), 0L))).toList();
    }

    @Transactional(readOnly = true)
    public PageRes<ListingRes> search(String q, String category, String city, Boolean online,
                                      BigDecimal maxPrice, Boolean firstFree, String sort, int page, int size) {
        Specification<TeacherListing> spec = (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            p.add(cb.equal(root.get("status"), ListingStatus.ACTIVE));
            Join<TeacherListing, Category> cat = root.join("category");
            if (category != null && !category.isBlank()) p.add(cb.equal(cat.get("slug"), category));
            if (city != null && !city.isBlank()) p.add(cb.equal(root.<String>get("city"), city.trim()));
            if (Boolean.TRUE.equals(online)) p.add(cb.isTrue(root.<Boolean>get("online")));
            if (maxPrice != null) p.add(cb.lessThanOrEqualTo(root.<BigDecimal>get("hourlyRate"), maxPrice));
            if (Boolean.TRUE.equals(firstFree)) p.add(cb.isTrue(root.<Boolean>get("firstLessonFree")));
            if (q != null && !q.isBlank()) {
                // Her sözcük ayrı aranır (VE); Türkçe karakter ve büyük/küçük harf farkı yok sayılır.
                for (String tok : TextUtil.fold(q).trim().split("\\s+")) {
                    if (!tok.isBlank()) p.add(cb.like(root.<String>get("searchText"), "%" + tok + "%"));
                }
            }
            return cb.and(p.toArray(new Predicate[0]));
        };
        Sort s = switch (sort == null ? "best" : sort) {
            case "priceAsc" -> Sort.by(Sort.Order.asc("hourlyRate"));
            case "priceDesc" -> Sort.by(Sort.Order.desc("hourlyRate"));
            case "newest" -> Sort.by(Sort.Order.desc("createdAt"));
            default -> Sort.by(Sort.Order.desc("boosted"), Sort.Order.desc("avgRating"), Sort.Order.desc("reviewCount"));
        };
        Page<TeacherListing> res = listings.findAll(spec, PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50), s));
        return new PageRes<>(res.getContent().stream().map(mapper::listing).toList(),
            res.getNumber(), res.getTotalPages(), res.getTotalElements());
    }

    @Transactional(readOnly = true)
    public ListingRes get(Long id) {
        TeacherListing l = find(id);
        if (l.getStatus() == ListingStatus.SUSPENDED) throw new ApiException(HttpStatus.NOT_FOUND, "İlan bulunamadı");
        return mapper.listing(l);
    }

    @Transactional(readOnly = true)
    public List<ListingRes> mine(User teacher) {
        return listings.findByTeacherIdOrderByCreatedAtDesc(teacher.getId()).stream().map(mapper::listing).toList();
    }

    public ListingRes create(User teacher, ListingReq r) {
        requireTeacher(teacher);
        TeacherListing l = new TeacherListing();
        l.setTeacher(teacher);
        apply(l, r);
        l.setBoosted(subs.existsByUserIdAndPlanAndEndsAtAfter(teacher.getId(), Plan.TEACHER_PASS, Instant.now()));
        return mapper.listing(listings.save(l));
    }

    public ListingRes update(User teacher, Long id, ListingReq r) {
        TeacherListing l = owned(teacher, id);
        if (l.getStatus() == ListingStatus.SUSPENDED)
            throw new ApiException(HttpStatus.FORBIDDEN, "Askıya alınan ilan düzenlenemez");
        apply(l, r);
        return mapper.listing(l);
    }

    public ListingRes setStatus(User teacher, Long id, String status) {
        TeacherListing l = owned(teacher, id);
        if (l.getStatus() == ListingStatus.SUSPENDED)
            throw new ApiException(HttpStatus.FORBIDDEN, "Askıya alınan ilanı yalnızca yönetici açabilir");
        ListingStatus s = "ACTIVE".equals(status) ? ListingStatus.ACTIVE
            : "PAUSED".equals(status) ? ListingStatus.PAUSED : null;
        if (s == null) throw new ApiException(HttpStatus.BAD_REQUEST, "Geçersiz durum");
        l.setStatus(s);
        return mapper.listing(l);
    }

    // ---- helpers
    private void apply(TeacherListing l, ListingReq r) {
        ContentRules.requireNoContactInfo(r.title(), r.description());
        if (!r.online() && !r.teacherHome() && !r.studentHome())
            throw new ApiException(HttpStatus.BAD_REQUEST, "En az bir ders biçimi seçin");
        Category c = categories.findById(r.categoryId())
            .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Ders alanı bulunamadı"));
        l.setCategory(c);
        l.setTitle(r.title().trim());
        l.setDescription(r.description().trim());
        l.setHourlyRate(r.hourlyRate());
        l.setCity(r.city().trim());
        l.setOnline(r.online());
        l.setTeacherHome(r.teacherHome());
        l.setStudentHome(r.studentHome());
        l.setFirstLessonFree(r.firstLessonFree());
        l.setExperienceYears(r.experienceYears());
        l.setEducation(r.education());
        l.setLanguages(r.languages());
        l.setSearchText(TextUtil.listingText(l));
    }

    private TeacherListing find(Long id) {
        return listings.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "İlan bulunamadı"));
    }

    private TeacherListing owned(User teacher, Long id) {
        TeacherListing l = find(id);
        if (!l.getTeacher().getId().equals(teacher.getId()))
            throw new ApiException(HttpStatus.FORBIDDEN, "Bu ilan size ait değil");
        return l;
    }

    private void requireTeacher(User u) {
        if (u.getRole() != Role.TEACHER) throw new ApiException(HttpStatus.FORBIDDEN, "Yalnızca öğretmen hesapları ilan verebilir");
    }
}
