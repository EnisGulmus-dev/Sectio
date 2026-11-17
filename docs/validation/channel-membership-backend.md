# Kanal ve üyelik backend doğrulaması

V19 kurumsal kapsamlı açık/özel kanal ve üyelik tablolarını ekler. Aktif ADMIN,
TEAM_LEADER, PROJECT_MANAGER ve PROJECT_OWNER kanal oluşturabilir. Kanal sahibi
otomatik üye olur ve yalnız sahip aktif kişileri üye ekleyebilir/çıkarabilir.
Açık kanallara kullanıcı kendisi katılabilir ve kendisi ayrılabilir. Özel kanal
üye olmayan hiç kimseye, ADMIN dahil listede veya detayda görünmez. Bir üye
ayrıldığında üye gerektiren erişimleri sona erer. Pasif/silinmiş hesaplar
erişemez; geçici pasifleştirmede üyelik saklanır.

Sahip kanaldan ayrılamaz. Sahipliği aktif bir mevcut üyeye devredebilir veya
kanalı silebilir. Kanal sahibi bir kullanıcı silinmeden önce bu işlem gereklidir;
böylece özel kanallar erişilemez duruma düşmez. Bu backend adımında kanal
ekranı, mesaj veya WebSocket henüz bulunmaz.

Hazırlanan PostgreSQL testleri: açık/özel görünürlük, üye olmayan yönetici,
yetkisiz oluşturma, ekleme/çıkarma, sahiplik devri, hesap silme engeli,
pasif üyeyi reddetme, açık kanala katılma/ayrılma, oturum ve CSRF koruması.
Testleri ajan çalıştırmadı.

Docker Desktop açıkken `backend` klasöründe çalıştır:

```powershell
.\mvnw.cmd -B -ntp clean verify
```

`BUILD SUCCESS` sonrasında mesaj API'si adımına geçilebilir.
