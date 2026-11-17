# Profil backend doğrulaması

İlk backend adımı: `GET/PATCH /api/v1/profile` kişisel metin alanları;
`PUT /api/v1/profile/avatar` hazır avatarı veya `null` ile baş harfini seçer;
`POST /api/v1/profile/photo` multipart `file` ile fotoğraf yükler;
`DELETE /api/v1/profile/photo` fotoğrafı kaldırır. Giriş yapmış kullanıcılar,
aktif başka kullanıcıların fotoğrafını `GET /api/v1/profile/{userId}/photo`
ile okuyabilir. Profil güncellemeleri yalnızca oturum sahibi hesabı etkiler.

PNG/JPEG gerçek dosya içeriği doğrulanır ve PNG olarak yeniden kodlanır; 2 MB,
4096x4096 ve 12 milyon piksel sınırları uygulanır. Avatar seçimi yüklenmiş
fotoğrafı, yeni fotoğraf seçimi hazır avatarı kaldırır. Yeni fotoğraf işlemi
geri alınırsa yeni dosya silinir; değiştirilmiş/eski dosya veya kalıcı silinen
hesabın fotoğrafı başarılı transaction sonrasında silinir.

Docker `sectio-profile-photos` volume'u yeni bir kalıcı veri alanıdır; konteyner
yeniden oluşturulunca korunur. Yedeklerde PostgreSQL ile birlikte saklanmalıdır.
Veritabanı ve dosya sistemi ayrı olduğu için işlem sonrasında disk hatasıyla
silinemeyen dosyalar için işletimsel temizlik ihtiyacı kalabilir.

Hazırlanan testler gerçek PostgreSQL üzerinde profil kaydı, avatar ve hatalı
seçim, saat dilimi ve hesap temizliğini; birim düzeyinde fotoğraf doğrulama ve
silme davranışını kapsar. Bunları ajan çalıştırmadı.

Docker Desktop açıkken `backend` dizininde:

```powershell
.\mvnw.cmd -B -ntp clean verify
```

Başarıdan sonra profil ekranı henüz geliştirilmediği için görsel kontrol frontend
adımında yapılır. Uygulama güncellenirse repo kökünden `docker compose -p
sectio-first-unit up -d --build --no-deps --force-recreate backend --wait
--wait-timeout 240` kullanılır; volume silinmez.
