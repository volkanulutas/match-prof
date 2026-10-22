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

/** Müsaitlik ve sertifika yönetimi. */
@Service
@RequiredArgsConstructor
@Transactional
public class TeacherProfileService {
    private final AvailabilityRepository availability;
    private final QualificationRepository quals;

    @Transactional(readOnly = true)
    public List<AvailabilityRes> availabilityOf(Long teacherId) {
        return availability.findByTeacherIdOrderByWeekdayAscStartTimeAsc(teacherId).stream()
            .map(a -> new AvailabilityRes(a.getId(), a.getWeekday(), a.getStartTime(), a.getEndTime())).toList();
    }

    public AvailabilityRes addAvailability(User t, AvailabilityReq r) {
        if (!r.endTime().isAfter(r.startTime()))
            throw new ApiException(HttpStatus.BAD_REQUEST, "Bitiş saati başlangıçtan sonra olmalı");
        boolean overlaps = availability.findByTeacherIdOrderByWeekdayAscStartTimeAsc(t.getId()).stream()
            .anyMatch(a -> a.getWeekday() == r.weekday()
                && a.getStartTime().isBefore(r.endTime()) && a.getEndTime().isAfter(r.startTime()));
        if (overlaps) throw new ApiException(HttpStatus.CONFLICT, "Bu aralık mevcut bir müsaitlikle çakışıyor");
        Availability a = new Availability();
        a.setTeacher(t); a.setWeekday(r.weekday()); a.setStartTime(r.startTime()); a.setEndTime(r.endTime());
        a = availability.save(a);
        return new AvailabilityRes(a.getId(), a.getWeekday(), a.getStartTime(), a.getEndTime());
    }

    public void deleteAvailability(User t, Long id) {
        Availability a = availability.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Kayıt bulunamadı"));
        if (!a.getTeacher().getId().equals(t.getId())) throw new ApiException(HttpStatus.FORBIDDEN, "Bu kayıt size ait değil");
        availability.delete(a);
    }

    @Transactional(readOnly = true)
    public List<QualificationRes> myQualifications(User t) {
        return quals.findByTeacherId(t.getId()).stream()
            .map(q -> new QualificationRes(q.getId(), q.getTitle(), q.getStatus())).toList();
    }

    public QualificationRes addQualification(User t, QualificationReq r) {
        ContentRules.requireNoContactInfo(r.title());
        Qualification q = new Qualification();
        q.setTeacher(t); q.setTitle(r.title().trim());
        q = quals.save(q);
        return new QualificationRes(q.getId(), q.getTitle(), q.getStatus());
    }

    public void deleteQualification(User t, Long id) {
        Qualification q = quals.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Kayıt bulunamadı"));
        if (!q.getTeacher().getId().equals(t.getId())) throw new ApiException(HttpStatus.FORBIDDEN, "Bu kayıt size ait değil");
        quals.delete(q);
    }
}
