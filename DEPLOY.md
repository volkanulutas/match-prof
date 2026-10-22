# Render'a yayınlama (ücretsiz)

Uygulama tek Docker imajıdır (React derlemesi Spring Boot içine gömülü): tek adres, CORS yok, WebSocket aynı adresten çalışır.

## 1) GitHub'a yükleyin
Zip'i açın; `dersbul` klasörünün **içeriği** deponun kökünde olmalı (`Dockerfile` ve `render.yaml` kökte).
```bash
cd dersbul
git init -b main
git add .
git commit -m "İlk sürüm"
git remote add origin https://github.com/KULLANICI/dersbul.git
git push -u origin main
```
Push'tan sonra GitHub'daki **Actions** sekmesinde CI çalışır; backend'in derlendiğini orada görürsünüz.
Kırmızı çıkarsa Render'a geçmeden önce hatayı düzeltin.

## 2) Render'da tek tıkla kurulum
1. render.com → **New → Blueprint** → GitHub deponuzu seçin. `render.yaml` okunur ve şunlar oluşur:
   - `dersbul` adlı ücretsiz Docker web servisi
   - `dersbul-db` adlı ücretsiz Postgres (bağlantı `DATABASE_URL` olarak servise otomatik verilir)
   - `JWT_SECRET` otomatik üretilir
2. **Apply**'a basın. İlk derleme ve açılış birkaç dakika sürebilir (ücretsiz katmanda 0,1 CPU var). Logları *Logs* sekmesinden izleyin.
3. Adresiniz `https://dersbul-xxxx.onrender.com` olur. Demo hesaplar README'de.

## Ücretsiz katman davranışları (Ekim 2026'da Render belgelerinden kontrol edildi, değişebilir)
- Web servisi 15 dk trafik olmazsa uykuya geçer, ilk istekte uyanması yaklaşık 1 dk sürer. Açık bir WebSocket bağlantısından
  gelen mesajlar uykuya geçişi geciktirir (istemci 25 sn'de bir `ping` yollar).
- **Ücretsiz Render Postgres oluşturulduktan 30 gün sonra sona erer ve veriler silinir; yedek de yoktur.** Deneme için uygundur.
  Kalıcı ücretsiz veritabanı isterseniz Neon kullanın (aşağıda).
- Bir çalışma alanında aynı anda yalnızca bir ücretsiz Render Postgres olabilir.
- Dosya sistemi geçicidir; uygulama dosyaya veri yazmaz (veritabanı kullanır).

## Veritabanını Neon'a almak (kalıcı ücretsiz)
1. neon.com'da ücretsiz proje açın, bağlantı dizesini kopyalayın (`postgresql://kullanici:sifre@host/db?sslmode=require`).
2. Render'da servis → **Environment** → `DATABASE_URL` değerini bu dizeyle değiştirin (Blueprint'teki `fromDatabase` bağlantısını elle üzerine yazarsınız) ve `dersbul-db`'yi silin.
Neon hareketsizken hesaplamayı kapatır; ilk bağlantı birkaç saniye gecikebilir.

## Herkese açmadan önce
- `SEED_DEMO` değerini `false` yapın (demo hesapların şifreleri herkesçe bilinir).
- Kendi yöneticiniz için Render → Environment'a `ADMIN_EMAIL` ve `ADMIN_PASSWORD` (en az 8 karakter) ekleyin; yeniden dağıtınca hesap oluşur.
- Ödeme demodur; gerçek para almayın.

## Yerelde Docker ile
```bash
docker compose up --build        # http://localhost:8080
```
İnternete geçici açmak için: `cloudflared tunnel --url http://localhost:8080` (Cloudflare hızlı tünel).
