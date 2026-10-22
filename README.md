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

## Talep, sohbet ve takvim akışı
1. Öğrenci ilan sayfasında öğretmenin **boş zamanlarını** görür (haftalık müsaitlikten ve mevcut derslerden sunucuda üretilir, 14 gün) ve en fazla 3 zaman seçip talep gönderir.
2. Talep gönderilince **sohbet hemen açılır** (talep metni ilk mesajdır); öğretmene bildirim gider.
3. Bildirime tıklayınca doğrudan **talep + sohbet ekranı** (`/mesajlar/:id`) açılır. Öğretmen burada önerilen zamanlardan birini onaylar (ders oluşur), zaman seçmeden kabul eder ya da reddeder.
4. Dersten 24 saat önce her iki tarafa hatırlatma bildirimi gider.
Saat dilimi `Europe/Istanbul` kabul edilir. Öğrenci Pass şartını geri getirmek için `STUDENT_PASS_REQUIRED=true` ayarlayın.

## Arama
Ders alanı (gruplu), il (81 il, sabit liste), online filtresi ve anahtar kelime; Türkçe karakter ve büyük/küçük harf farkı yok sayılır. En fazla 3 ilan yan yana karşılaştırılır (`/karsilastir`). Sonuç yoksa filtreleri gevşeten alternatifler ve aynı gruptaki dolu kategoriler önerilir.

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
- Öğrencinin önerdiği zamanlar müsaitliğe ve çakışmaya karşı doğrulanır; öğretmenin elle planladığı ders (`POST /api/sessions`) müsaitlik aralığına zorlanmaz. Karşı zaman önerisi, takvim görünümü ve iCal henüz yok.
- Bildirimler uygulama içi; e-posta/SMS/push sağlayıcısı `NotificationService` içine eklenmeli.
- JWT gizli anahtarı için `JWT_SECRET` ortam değişkenini ayarlayın. Şifre sıfırlama, e-posta doğrulama, KVKK onayı, dosya yükleme (profil fotoğrafı) yok.
- Testler yazılmadı; `ddl-auto: update` yerine Flyway/Liquibase önerilir.
