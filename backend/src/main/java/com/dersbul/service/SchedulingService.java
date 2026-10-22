package com.dersbul.service;

import com.dersbul.domain.Availability;
import com.dersbul.dto.Dtos.SlotRes;
import com.dersbul.domain.LessonSession;
import com.dersbul.repo.AvailabilityRepository;
import com.dersbul.repo.LessonSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/** Öğretmenin haftalık müsaitliğinden somut, boş zaman dilimleri üretir ve bir dilimin hâlâ boş olduğunu doğrular. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SchedulingService {
    private static final long MIN_LEAD_HOURS = 2;

    private final AvailabilityRepository availability;
    private final LessonSessionRepository sessions;

    public List<SlotRes> freeSlots(Long teacherId, int days, int minutes) {
        List<Availability> windows = availability.findByTeacherIdOrderByWeekdayAscStartTimeAsc(teacherId);
        Instant now = Instant.now();
        LocalDate today = LocalDate.now(TextUtil.ZONE);
        Instant rangeEnd = today.plusDays(days + 1L).atStartOfDay(TextUtil.ZONE).toInstant();
        List<LessonSession> busy = sessions.findScheduled(teacherId, now, rangeEnd);

        List<SlotRes> out = new ArrayList<>();
        for (int d = 0; d < days; d++) {
            LocalDate date = today.plusDays(d);
            for (Availability a : windows) {
                if (a.getWeekday() != date.getDayOfWeek()) continue;
                LocalTime t = a.getStartTime();
                while (!t.plusMinutes(minutes).isAfter(a.getEndTime()) && !t.plusMinutes(minutes).isBefore(t)) {
                    Instant s = date.atTime(t).atZone(TextUtil.ZONE).toInstant();
                    Instant e = s.plus(minutes, ChronoUnit.MINUTES);
                    boolean taken = busy.stream().anyMatch(b -> b.getStartsAt().isBefore(e) && b.getEndsAt().isAfter(s));
                    if (s.isAfter(now.plus(MIN_LEAD_HOURS, ChronoUnit.HOURS)) && !taken) out.add(new SlotRes(null, s, e, false));
                    t = t.plusMinutes(minutes);
                }
            }
        }
        return out;
    }

    /** Dilim, öğretmenin müsaitlik aralıklarından birinin içinde mi ve başka bir dersle çakışmıyor mu? */
    public boolean isFree(Long teacherId, Instant start, Instant end) {
        ZonedDateTime s = start.atZone(TextUtil.ZONE);
        ZonedDateTime e = end.atZone(TextUtil.ZONE);
        if (!s.toLocalDate().equals(e.toLocalDate())) return false;
        boolean inWindow = availability.findByTeacherIdOrderByWeekdayAscStartTimeAsc(teacherId).stream().anyMatch(a ->
            a.getWeekday() == s.getDayOfWeek()
                && !a.getStartTime().isAfter(s.toLocalTime()) && !a.getEndTime().isBefore(e.toLocalTime()));
        return inWindow && !sessions.hasConflict(teacherId, start, end);
    }
}
