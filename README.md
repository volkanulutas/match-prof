# Dersbul — özel ders platformu (Spring Boot + React)

Superprof benzeri iki taraflı bir özel ders pazaryeri: öğretmen ilanı, arama, ders talebi, mesajlaşma,
ders planlama, yorum, abonelik (Pass), şikâyet/moderasyon ve yönetici paneli.

## Çalıştırma

**Backend** (Java 17+, Maven):
```bash
cd backend
mvn spring-boot:run          # http://localhost:8080  (H2 dosya veritabanı, ./data)
```
PostgreSQL için: `docker compose up -d` ardından `mvn spring-boot:run -Dspring-boot.run.profiles=postgres`.

**Frontend** (Node 18+):
```bash
cd frontend
npm install
npm run dev                  # http://localhost:5173  (/api isteklerini 8080'e yönlendirir)
```

**Her şey Docker ile:** `docker compose up --build` → http://localhost:8080.
**Render'da yayınlama:** `DEPLOY.md` (depoyu GitHub'a yükleyin → Render'da *New → Blueprint*).

## Anlık mesajlaşma
Mesaj gönderme REST ile yapılır (doğrulama ve Öğrenci Pass kuralı tek yerde); sunucu mesajı ve bildirimleri
WebSocket (`/ws`) ile iki tarafa anında iletir. Bağlantı, `POST /api/auth/ws-ticket` ile alınan 30 sn geçerli,
tek kullanımlık biletle kurulur (JWT URL'de görünmez). İstemci kopunca üstel bekleme ile yeniden bağlanır;
bağlantı yokken eski yöntemle (8 sn'de bir sorgu) çalışmaya devam eder. Bağlantı bellek içinde tutulur,
yani tek sunucu örneği içindir; birden fazla örnek için Redis pub/sub eklenmelidir.

## Demo hesaplar (ilk açılışta otomatik yüklenir)
| Rol | E-posta | Şifre |
|---|---|---|
| Yönetici | admin@dersbul.local | Admin12345! |
| Öğrenci | ogrenci@dersbul.local | Demo12345! |
| Öğretmen | melih@dersbul.local, burcu@, gorkem@, damla@dersbul.local | Demo12345! |

H2 konsolu: http://localhost:8080/h2-console (JDBC: `jdbc:h2:file:./data/dersbul`, kullanıcı `sa`, şifre boş).

## Uygulanan iş kuralları
- **Talep akışı:** PENDING → ACCEPTED / DECLINED (öğretmen), iptal (öğrenci); 7 gün yanıtsız talep otomatik EXPIRED olur.
- **Mesajlaşma:** talep kabul edilince sohbet açılır; öğrencinin yazabilmesi için aktif **Öğrenci Pass** gerekir (HTTP 402).
- **Ders planlama:** yalnızca ilan sahibi, kabul edilmiş talep için; geçmişe ders yok, en fazla 4 saat, öğretmenin kendi dersleriyle çakışma kontrolü.
- **Yorum:** yalnızca tamamlanmış derse, ders başına bir kez; ilanın puan ortalaması güncellenir.
- **İlan kuralları:** metinde telefon / e-posta / web adresi yasak; en az bir ders biçimi zorunlu.
- **Öne çıkarma:** Professional Pass'li öğretmenlerin ilanları arama sıralamasında öne alınır; süre bitince saatlik iş kaldırır.
- **Moderasyon:** şikâyet → yönetici ilanı askıya alır veya reddeder; belge doğrulaması ayrı durum (UNVERIFIED / VERIFIED / REJECTED).

## Bilinen sınırlar (üretime çıkmadan önce)
- Ödeme **demo**dur (`SubscriptionService`); iyzico / PayTR / Stripe entegre edilmeli. Fiyatlar `application.yml` içinde örnek değerlerdir.
- WebSocket tek sunucu örneği içindir (bellek içi oturum ve bilet deposu).
- "Yazıyor…" göstergesi ve okundu bilgisi yok.
- Ders çakışma kontrolü yalnızca öğretmen için; planlanan ders müsaitlik aralığına zorlanmaz.
- Bildirimler uygulama içi; e-posta/SMS/push sağlayıcısı `NotificationService` içine eklenmeli.
- JWT gizli anahtarı için `JWT_SECRET` ortam değişkenini ayarlayın. Şifre sıfırlama, e-posta doğrulama, KVKK onayı, dosya yükleme (profil fotoğrafı) yok.
- Testler yazılmadı; `ddl-auto: update` yerine Flyway/Liquibase önerilir.
