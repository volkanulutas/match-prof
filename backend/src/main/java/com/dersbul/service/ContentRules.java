package com.dersbul.service;

import com.dersbul.web.ApiException;
import org.springframework.http.HttpStatus;
import java.util.regex.Pattern;

/** İlan metinlerinde iletişim bilgisi paylaşımını engeller (platform dışına çıkışı önlemek için). */
public final class ContentRules {
    private static final Pattern CONTACT = Pattern.compile(
        "([\\w.+-]+@[\\w-]+\\.[\\w.]+)|(\\+?\\d[\\d\\s().-]{8,}\\d)|(https?://|www\\.)",
        Pattern.CASE_INSENSITIVE);

    private ContentRules() {}

    public static void requireNoContactInfo(String... texts) {
        for (String t : texts) {
            if (t != null && CONTACT.matcher(t).find()) {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                    "İlan metninde telefon, e-posta veya web adresi paylaşılamaz. İletişim platform üzerinden kurulur.");
            }
        }
    }
}
