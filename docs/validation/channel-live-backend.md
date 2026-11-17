# Kanal WebSocket doğrulaması

Spring MVC WebSocket desteği kullanılır. `GET /api/v1/channels/{id}/live`
mevcut oturum ve yalnız aynı origin ile açılır; aktif kullanıcı ve güncel kanal
üyeliği handshake sırasında ve her bildirim öncesinde kontrol edilir. Bu uç
yalnız `connected` ve `messages_changed` bildirimlerini gönderir; mesaj içeriği
ve yazma işlemleri CSRF korumalı HTTP API'dedir. İstemciden sokete metin
gönderilmesi bağlantıyı kapatır.

Mesaj oluşturma/düzenleme/silme commit edildikten sonra bildirim yayınlanır.
Üyelik kaldırıldığında o kanalın bağlantıları kontrol edilir; erişimi bitenler
kapatılır. Oturumu kapanan, parolası sıfırlanan veya hesabı pasif/silinmiş
kullanıcılar her bildirimde ve en geç 10 saniyelik kontrolde ayrılır. Tek
uygulama örneği için bellek içi bağlantı kaydı kullanılır; çoklu backend
örnekleri arasında yayın için daha sonra ortak bir aracı gerekir.

Bağlantı tekrar açılınca istemci HTTP mesaj geçmişini yeniden okumalı, en son
gördüğü kayda ulaşana dek `beforeId` ile sayfalama yapmalı ve ID'ye göre
tekilleştirmelidir. Mesaj düzenleme ve silme de geçmişten yeniden alınır.
Frontend henüz uygulanmadığından bu toparlama davranışı sonraki adımdadır.

Hazırlanan gerçek PostgreSQL/WebSocket testi: özel kanal üyesi olmayanı ve
farklı origin'i reddetme, üye bağlantısı, commit sonrası bildirim ve üyelik
kaldırılınca kapanış. Testler ajan tarafından çalıştırılmadı. Ngrok arkasında
aynı origin kontrolü ayrıca gerçek HTTPS adresiyle kullanıcı tarafından
doğrulanmalıdır; proxy başlıkları otomatik olarak güvenilir kabul edilmez.

Docker Desktop açıkken `backend` klasöründe:

```powershell
.\mvnw.cmd -B -ntp clean verify
```
