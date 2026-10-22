package com.dersbul.config;

import com.dersbul.domain.*;
import com.dersbul.repo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.*;

/** Boş veritabanına demo veri yükler. Giriş: admin@dersbul.local / Admin12345!, diğer demo hesaplar / Demo12345! */
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {
    private final UserRepository users;
    private final CategoryRepository categories;
    private final ListingRepository listings;
    private final AvailabilityRepository availability;
    private final QualificationRepository quals;
    private final PasswordEncoder encoder;

    @Value("${app.seed-demo:true}") private boolean seedDemo;
    @Value("${app.admin.email:}") private String adminEmail;
    @Value("${app.admin.password:}") private String adminPassword;

    @Override
    @Transactional
    public void run(String... args) {
        // Eski kayıtlar için arama metnini üret (sütun sonradan eklendi).
        listings.findAll().forEach(l -> {
            if (l.getSearchText() == null) l.setSearchText(com.dersbul.service.TextUtil.listingText(l));
        });
        if (categories.count() == 0) seedCategories();
        if (!adminEmail.isBlank() && adminPassword.length() >= 8 && !users.existsByEmailIgnoreCase(adminEmail))
            mk(adminEmail.trim().toLowerCase(), adminPassword, "Yönetici", Role.ADMIN, "");
        if (!seedDemo || users.existsByEmailIgnoreCase("ogrenci@dersbul.local")) return;

        Map<String, Category> cat = new LinkedHashMap<>();
        categories.findAll().forEach(c -> cat.put(c.getSlug(), c));

        mk("admin@dersbul.local", "Admin12345!", "Yönetici", Role.ADMIN, "İstanbul");
        User student = mk("ogrenci@dersbul.local", "Demo12345!", "Deniz Öğrenci", Role.STUDENT, "İstanbul");

        User t1 = mk("melih@dersbul.local", "Demo12345!", "Meliha K.", Role.TEACHER, "Denizli");
        User t2 = mk("burcu@dersbul.local", "Demo12345!", "Burcu A.", Role.TEACHER, "Kayseri");
        User t3 = mk("gorkem@dersbul.local", "Demo12345!", "Görkem T.", Role.TEACHER, "İstanbul");
        User t4 = mk("damla@dersbul.local", "Demo12345!", "Damla S.", Role.TEACHER, "Ankara");

        listing(t1, cat.get("matematik"), "TYT-AYT-LGS matematik: eksik konuları birlikte kapatalım",
            "12 yıllık deneyimle ortaokul ve lise öğrencilerine matematik dersi veriyorum. İlk derste seviyeni belirliyor, haftalık plan çıkarıyorum. Soru çözümü ve deneme analizi derslerin parçası.",
            "600", "Denizli", true, false, true, true, 12, "Matematik Öğretmenliği", "Türkçe", 4.9, 117, true);
        listing(t2, cat.get("diksiyon"), "Diksiyon ve etkili konuşma: sesini ve hitabetini geliştir",
            "Sunum, mülakat ve günlük iletişimde daha net ve güvenli konuşmak isteyenler için bire bir online çalışma. Nefes, artikülasyon ve vurgu üzerine uygulamalı dersler.",
            "400", "Kayseri", true, false, false, true, 8, "Radyo ve Televizyon", "Türkçe", 4.8, 89, false);
        listing(t3, cat.get("tenis"), "Birebir tenis: modern antrenman yöntemleriyle her seviyeye",
            "Başlangıçtan ileri seviyeye kadar teknik, ayak çalışması ve maç taktiği. Kort ve ekipman planı ilk derste birlikte netleşir.",
            "750", "İstanbul", false, true, true, false, 10, "Spor Bilimleri", "Türkçe, İngilizce", 4.7, 132, false);
        listing(t4, cat.get("piyano"), "Piyano: hobi ya da konservatuvar hazırlığı, her yaşa uygun",
            "Nota okumadan repertuvar çalışmasına kadar kişiye özel program. ABRSM sınavlarına ve müzik okulu yetenek sınavlarına hazırlık dersleri veriyorum.",
            "800", "Ankara", true, true, false, true, 21, "Konservatuvar, Piyano", "Türkçe, İngilizce", 4.9, 27, true);
        listing(t4, cat.get("ingilizce"), "Konuşma odaklı İngilizce: akıcılığı dersle değil pratikle kazan",
            "Günlük konuşma, iş İngilizcesi ve IELTS speaking için bire bir çalışma. Her ders sonunda kişisel geri bildirim ve kısa ödev.",
            "500", "Ankara", true, false, false, true, 6, "İngiliz Dili ve Edebiyatı", "Türkçe, İngilizce", 0, 0, false);

        for (User t : List.of(t1, t2, t3, t4)) {
            avail(t, DayOfWeek.MONDAY, 18, 21); avail(t, DayOfWeek.WEDNESDAY, 18, 21); avail(t, DayOfWeek.SATURDAY, 10, 16);
        }
        Qualification q = new Qualification();
        q.setTeacher(t1); q.setTitle("Matematik Öğretmenliği Lisans Diploması"); q.setStatus(QualificationStatus.VERIFIED);
        quals.save(q);
    }

    private void seedCategories() {
        Map<String, Category> cat = new LinkedHashMap<>();
        String[][] data = {
            {"Okul ve Sınavlar", "Matematik", "Fizik", "Kimya", "Biyoloji", "Türkçe", "Tarih", "Edebiyat", "Okul derslerine destek", "Ödevlere yardım", "Eğitim koçluğu"},
            {"Yabancı diller", "İngilizce", "Almanca", "Fransızca", "İspanyolca", "Rusça", "Arapça", "İtalyanca", "Japonca", "Korece", "Çince", "IELTS", "Yabancılar için Türkçe"},
            {"Müzik", "Piyano", "Gitar", "Keman", "Bateri", "Şan", "Bağlama"},
            {"Spor ve Dans", "Tenis", "Yüzme", "Futbol", "Boks", "Fitness", "Personal Trainer", "Dans"},
            {"Sağlık ve Meditasyon", "Yoga", "Pilates"},
            {"Sanat ve Hobi", "Resim", "Satranç", "Direksiyon", "Diksiyon"},
            {"Bilişim", "Programlama", "Web tasarım", "Veri bilimi"},
        };
        for (String[] g : data) {
            for (int i = 1; i < g.length; i++) {
                Category c = new Category();
                c.setGroupName(g[0]); c.setName(g[i]); c.setSlug(slug(g[i]));
                cat.put(c.getSlug(), categories.save(c));
            }
        }

    }

    private User mk(String email, String pw, String name, Role role, String city) {
        User u = new User();
        u.setEmail(email); u.setPasswordHash(encoder.encode(pw)); u.setFullName(name); u.setRole(role); u.setCity(city);
        return users.save(u);
    }

    private void listing(User t, Category c, String title, String desc, String price, String city, boolean online,
                         boolean teacherHome, boolean studentHome, boolean free, int exp, String edu, String langs,
                         double rating, int reviews, boolean boosted) {
        TeacherListing l = new TeacherListing();
        l.setTeacher(t); l.setCategory(c); l.setTitle(title); l.setDescription(desc);
        l.setHourlyRate(new BigDecimal(price)); l.setCity(city); l.setOnline(online);
        l.setTeacherHome(teacherHome); l.setStudentHome(studentHome); l.setFirstLessonFree(free);
        l.setExperienceYears(exp); l.setEducation(edu); l.setLanguages(langs);
        l.setAvgRating(rating); l.setReviewCount(reviews); l.setBoosted(boosted);
        l.setSearchText(com.dersbul.service.TextUtil.listingText(l));
        listings.save(l);
    }

    private void avail(User t, DayOfWeek d, int from, int to) {
        Availability a = new Availability();
        a.setTeacher(t); a.setWeekday(d); a.setStartTime(LocalTime.of(from, 0)); a.setEndTime(LocalTime.of(to, 0));
        availability.save(a);
    }

    static String slug(String s) {
        return s.toLowerCase(new Locale("tr", "TR"))
            .replace('ı', 'i').replace('ö', 'o').replace('ü', 'u').replace('ş', 's').replace('ç', 'c').replace('ğ', 'g')
            .replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
    }
}
