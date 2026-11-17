# Kanal mesajlaşması backend doğrulaması

V20 kanal mesajlarını saklar. Üyeler 1–4000 karakterlik metin gönderebilir;
yalnız yazar kendi mesajını düzenler veya silebilir. Silinince mesaj metni
silinir, zaman çizelgesinde silinmiş kayıt kalır. Geçmiş en yeni ID'den eskiye
50 kayıtla başlar; `beforeId` ve 1–100 `limit` ile önceki sayfalar alınır.

Her okuma ve yazmada aktif hesap ve güncel üyelik kontrol edilir. Yazmada hesap
ve üyelik satırları kilitlenir. Üyeliği kaldırılan kişi geçmişi ve mesaj yazmayı
kaybeder. Yeni veya yeniden eklenen üye önceki geçmişi görebilir. Hesap kalıcı
anonimleştirildiğinde yazar bilgisi anonim hesap üzerinden gösterilir, mesaj
metinleri korunur. Kanal silindiğinde mesajlar da silinir.

Hazırlanan PostgreSQL ve HTTP testleri sayfalama, mesaj sahibi kontrolleri,
düzenleme/silme, üyelik kaldırma ve geri ekleme, anonimleştirme ve CSRF
senaryolarını kapsar. Testler ajan tarafından çalıştırılmadı.

Docker Desktop açıkken `backend` dizininde:

```powershell
.\mvnw.cmd -B -ntp clean verify
```

Anlık bağlantı ve mesaj ekranı sonraki aşamalardadır.
