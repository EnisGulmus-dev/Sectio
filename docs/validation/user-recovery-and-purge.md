# Kullanıcı oluşturma, geri yükleme ve kalıcı silme

2026-11-17. Testler hazırlandı; ajan tarafından çalıştırılmadı.

## Davranış

- Kullanıcı ekleme ve girişteki HTML `type=email`, yerel bölümünde Türkçe
  karakter bulunan adresleri isteği göndermeden engelliyordu. Alanlar artık metin
  alanı + e-posta klavyesi kullanır. Yeni veya güncellenen adreslerde ASCII zorunludur; sunucudaki @Email
  doğrulaması korunur. Adres trim/küçük harf normalizasyonuyla gönderilir.
  Bir uygulama giriş adresinin kabulü gerçek posta kutusunu doğrulamaz.
- Yeni kullanıcı penceresi API hatalarını ve alan hatalarını kendi içinde
  gösterir. Kaydet işlem sırasında kilitlenir. Silinmiş hesaba ayrılmış e-posta
  için Silinenler bölümünden geri yükleme/kalıcı silme yönlendirmesi görünür.
- Silinenler / Geri yükle yeni 12–72 karakter geçici parola ister. Hesap aktif
  olur, ilk girişte parola değiştirilir. Eski parola ve eski oturumlar geçersizdir.
  Önceki proje/takım üyelikleri ve kişi atamaları otomatik açılmaz; mevcut proje
  sahipliği korunur. İş geçmişi aynı kişi kimliğinde kalır.
- Silinenler / Kalıcı sil ayrı onay ister. Hesaptaki ad, e-posta ve parola
  anonimleştirilir, rol/unvan sıfırlanır. V14 `purged_at` ile kaydı iki kullanıcı
  listesinden de çıkarır ve geri yüklemeyi engeller. Görevler, projeler ve atama
  geçmişinin referansları korunur; proje/görev ekranında anonim kullanıcı adı
  görünür. Görev açıklaması gibi serbest metinler yeniden yazılmaz.
- Eski e-posta yeni bir hesapta kullanılabilir; yeni ID eski hesabın görev ve
  proje erişimlerini devralmaz. Yalnız ADMIN işlem yapabilir, CSRF zorunludur.
  Aktif/pasif ama silinmemiş hesap doğrudan kalıcı silinemez.

## Kullanıcının çalıştıracağı sıra

1. Docker Desktop açıkken backend dizininde:

   ```powershell
   .\mvnw.cmd -B -ntp clean verify
   ```

   Regresyonlar Türkçe karakterli e-posta reddi; form içinde hata;
   geri yükleme, geçici parola, oturum iptali; kalıcı silme, iş koruma ve e-posta
   yeniden kullanımı; ADMIN/CSRF ve silinmemiş hesap korumalarını kapsar.

2. BUILD SUCCESS sonrasında repo kökünde sırayla:

   ```powershell
   docker compose -p sectio-first-unit start db
   docker compose -p sectio-first-unit up -d --build --no-deps --force-recreate backend --wait --wait-timeout 240
   ```

3. Ctrl+F5 ile yenile. ASCII karakterli deneme adresiyle kullanıcı oluştur.
   Aynı adresi yeniden dene: hata pencerenin içinde görünmeli. Yeni hesapla giriş
   yap ve zorunlu parola değişimini tamamla.
4. Deneme hesabına görev ata ve hesabı sil. Silinenler'den yeni geçici parolayla
   geri yükle: kullanıcı normal listeye dönmeli, geçmişi durmalı, eski parola
   çalışmamalı. Eski atamaları otomatik açılmamalı.
5. Aynı deneme hesabını tekrar sil; Silinenler'de Kalıcı sil onayını önce iptal et
   (kayıt kalmalı), ardından onayla (listeden kalkmalı). Yalnız deneme hesabında
   uygula; bu ikinci silme geri alınamaz. Proje ve görevler kalmalı.
6. Eski e-postayla yeni hesap aç: farklı kişi kimliği olmalı, eski iş erişimi
   devralınmamalı. Açık/koyu temada aksiyonlar okunabilir olmalı.
