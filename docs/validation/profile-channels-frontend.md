# Profil ve kanallar: arayüz kontrolü

Profilim ekranı görünür ad, açıklama ve saat dilimini kaydeder; baş harf, hazır
avatar veya PNG/JPEG fotoğraf seçilir. Kanallar ekranında açık/özel kanal,
üyelik, sahiplik devri, mesaj gönderme/düzenleme/silme ve geçmiş yer alır.
Bağlantı kesilirse yeniden bağlanır ve mevcut sayfaların geçmişi yeniden okunur.
Üyelik iptalinde ekran temizlenir. WebSocket yalnız değişiklik bildirimi taşır;
mesaj verisi mevcut yetkili HTTP uçlarından okunur.

Hazırlanan frontend testleri avatar seçimi, multipart dosya gönderimi, açık
kanala katılma ve mesaj gönderimini; PostgreSQL testleri aday listesine yalnız
kanal sahibinin erişmesini kapsar. Testler ajan tarafından çalıştırılmadı.

Docker Desktop açıkken backend klasöründe:

```powershell
.\mvnw.cmd -B -ntp clean verify
```

Başarılıysa repo kökünde:

```powershell
docker compose -p sectio-first-unit up -d --build --no-deps --force-recreate backend --wait --wait-timeout 240
```

Sonra Ctrl+F5 ile yenileyin: Profilim'de baş harf, hazır avatar ve fotoğrafı
deneyin. İki farklı hesap ve iki tarayıcı oturumuyla açık kanala katılın, mesaj
gönderin, düzenleyin ve silin. Özel kanala üye olmayan yönetici dahil kimsenin
erişemediğini; çıkarılan üyenin sohbetinin kapandığını kontrol edin. Bağlantıyı
kesip açınca geçmişi ve iki temayı kontrol edin. `ngrok` HTTPS bağlantısında da
canlı durumunu ayrı doğrulayın; proxy origin yapılandırması gerekebilir.
