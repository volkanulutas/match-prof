package com.dersbul.web;

import com.dersbul.dto.Dtos.*;
import com.dersbul.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.List;

/** Herkese açık uçlar: kategoriler, ilan arama, ilan detayı, yorumlar, müsaitlik. */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CatalogController {
    private final ListingService listings;
    private final ReviewService reviews;
    private final TeacherProfileService profile;

    @GetMapping("/categories")
    public List<CategoryRes> categories() { return listings.categories(); }

    @GetMapping("/listings")
    public PageRes<ListingRes> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) Boolean online,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Boolean firstLessonFree,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        return listings.search(q, category, city, online, maxPrice, firstLessonFree, sort, page, size);
    }

    @GetMapping("/listings/{id}")
    public ListingRes get(@PathVariable Long id) { return listings.get(id); }

    @GetMapping("/listings/{id}/reviews")
    public List<ReviewRes> reviews(@PathVariable Long id) { return reviews.forListing(id); }

    @GetMapping("/teachers/{teacherId}/availability")
    public List<AvailabilityRes> availability(@PathVariable Long teacherId) { return profile.availabilityOf(teacherId); }
}
