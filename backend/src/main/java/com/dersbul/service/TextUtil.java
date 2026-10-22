package com.dersbul.service;

import com.dersbul.domain.TeacherListing;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class TextUtil {
    private TextUtil() {}

    /** Uygulamanın saat dilimi: müsaitlik saatleri ve bildirim metinleri bu dilimde yorumlanır. */
    public static final ZoneId ZONE = ZoneId.of("Europe/Istanbul");
    private static final DateTimeFormatter WHEN =
        DateTimeFormatter.ofPattern("d MMMM yyyy, EEEE HH:mm", new Locale("tr", "TR")).withZone(ZONE);

    public static String dateTime(Instant i) { return WHEN.format(i); }

    /** Küçük harfe çevirir ve Türkçe karakterleri sadeleştirir: "İstanbul" -> "istanbul", "öğretmen" -> "ogretmen". */
    public static String fold(String s) {
        if (s == null) return "";
        String t = s.replace('İ', 'i').replace('I', 'i').replace('ı', 'i')
            .replace('Ş', 's').replace('ş', 's').replace('Ğ', 'g').replace('ğ', 'g')
            .replace('Ü', 'u').replace('ü', 'u').replace('Ö', 'o').replace('ö', 'o')
            .replace('Ç', 'c').replace('ç', 'c')
            .replace('â', 'a').replace('Â', 'a').replace('î', 'i').replace('Î', 'i').replace('û', 'u').replace('Û', 'u');
        return t.toLowerCase(Locale.ROOT);
    }

    public static String listingText(TeacherListing l) {
        return fold(String.join(" ",
            nvl(l.getTitle()), nvl(l.getDescription()), nvl(l.getCategory().getName()), nvl(l.getCity()),
            nvl(l.getTeacher().getFullName()), nvl(l.getEducation())));
    }

    private static String nvl(String s) { return s == null ? "" : s; }
}
