# Organizasyon ve profil doğrulaması

V21 kurumsal takımları ve doğrudan yönetici bağını proje takımlarından ayrı
saklar. Eski kullanıcılar için bunlar boş başlar; eski proje üyeliklerinden
ilişki tahmin edilmez. Bir kullanıcı birden fazla şirket takımına girebilir.
Kullanıcı oluşturma isteğinde yönetici ve takım seçimi isteğe bağlıdır; hesap,
başlangıç profili ve seçilen ilişkiler tek transaction ile oluşturulur.

Organizasyon ekranında ADMIN takım oluşturabilir, adını düzenleyebilir veya
üyeliklerle birlikte takımı silebilir. ADMIN çalışan ilişkilerini değiştirir;
bir doğrudan yönetici yalnız kendi çalışanlarının takımlarını yönetebilir ve
yönetici alanını kendisinden başkasına değiştiremez. Döngü ve pasif/yetkisiz
yönetici seçimi engellenir. Bağlı çalışanı olan yönetici pasifleştirilmeden,
silinmeden veya yöneticilik yetkisi kaldırılmadan önce çalışanlar devredilir.
Takım silme çalışanları ve proje görevlerini silmez.

Profilim ekranında görsel ve metinler önce taslak olarak tutulur; en alttaki
Güncelle tüm seçimi tek multipart işleminde kaydeder. Başarıdan sonra sol
menüde görünen ad ve fotoğraf/avatar yenilenir. Çıkış ve tema düğmeleri
erişilebilir isim ve tooltip içeren simgelerdir. Mevcut fotoğraf uçları eski
istemciler için korunur.

Hazırlanan testler PostgreSQL üzerinde çoklu takım, döngü/rol/yetki kontrolleri,
devir zorunluluğu, hesap+profil+ilişki transaction'ı ve profil işleminde hata
durumunda eski bilgilerin korunmasını; UI tarafında çoklu takım seçimi, tek
güncelleme ve sol menü yenilenmesini kapsar. Testler ajan tarafından çalıştırılmadı.

Docker Desktop açıkken backend dizininde:

```powershell
.\mvnw.cmd -B -ntp clean verify
```

Başarıdan sonra repo kökünde:

```powershell
docker compose -p sectio-first-unit up -d --build --no-deps --force-recreate backend --wait --wait-timeout 240
```

Ctrl+F5 ile Organizasyon'da şirket takımları oluşturun; yeni kullanıcıya isteğe
bağlı iki takım ve yönetici seçin. Doğrudan yönetici hesabında yalnız bağlı
çalışanların yönetildiğini kontrol edin. Bağlı yöneticiyi devretmeden
pasifleştirmeyi deneyin; engellenmeli, devirden sonra başarılı olmalıdır.
Profilim'de metin ve avatarı değiştirip kaydetmeden menünün değişmediğini,
Güncelle sonrası menünün yenilendiğini ve iki temada simgeleri kontrol edin.
