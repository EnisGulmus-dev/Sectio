# Çalışan iş takibi ve kullanıcı ekranı düzeltmesi

Güncel davranış: kullanıcı silinmeden mevcut ve tamamlanmış atamaları İş takibi
ekranında görünür. Pasif ve silinmiş hesaplar da izlenebilir. Önceki yalnız
sonlandırılmış atama görünümü eski API'de korunur; yeni ekran birleşik API kullanır.

Kullanıcılar üstündeki büyük checkbox kaldırıldı. **Kullanıcılar / Silinenler**
kompakt seçiminden liste değişir. Her satırda temaya uygun **İş takibi** eylemi
vardır. Pencerede iş kartları, görev/proje araması, durum ve atama kapsamı
filtreleri, Yenile ve sayfalama bulunur. Güncel atama gerçek görev durumunu,
Geçmiş atama sonlandırma anındaki durumu gösterir.

## 1. İlk çalıştırılacak komut

Docker Desktop açıkken backend dizininde:

```powershell
.\mvnw.cmd -B -ntp clean verify
```

Testler hazırlanır; ajan çalıştırmaz. Yeni PG regresyonu aktif/pasif çalışanın
DONE işini hiçbir kullanıcı silinmeden listeler; CURRENT/ENDED, durum, arama ve
sayfa toplamlarını denetler. Silinen hesapta snapshot okunması ve çalışan için
403 de kapsanır. Frontend testi aktif çalışanı silmeden tamamlanan işi açmayı
ve durum filtresini doğrular; silinen kullanıcı akışı yeni liste seçimine uyarlandı.

## 2. Başarıdan sonra uygulama güncellemesi

Repo kökünde, adımlar başarılı oldukça devam et:

```powershell
docker compose -p sectio-first-unit start db
docker compose -p sectio-first-unit up -d --build --no-deps --force-recreate backend --wait --wait-timeout 240
```

Yeni migration yoktur. Volume/parolalar korunur; DB container yeniden oluşturulmaz.

## 3. Kullanıcının yapacağı manuel/görsel kontrol

1. Kullanıcılar sayfasını Ctrl+F5 ile yenile. Büyük checkbox olmamalı; küçük
   Kullanıcılar/Silinenler seçimiyle liste geçişi çalışmalı.
2. Aktif deneme çalışanına görev ata ve tamamla. Kullanıcıyı silmeden İş takibi
   aç; görev Tamamlandı olarak, varsa tamamlanma tarihiyle görünmeli.
3. Tüm durumlar/Tamamlandı/Devam ediyor ve Tüm/Güncel/Geçmiş atama filtrelerini
   dene. Görev/proje araması sonucu ve sayfa toplamı aynı filtreyi izlemeli.
4. Pasifleştirilen çalışan mevcut işlerini kaybetmemeli. Silinenler görünümündeki
   kişinin sonlandırılmış atamaları da aynı pencerede görünmeli. Okuma işlemleri
   hesap veya görev durumunu değiştirmemeli.
5. Cloud/midnight temalarında masaüstü ve dar ekranı kontrol et: kart/metinler
   taşmamalı, kapanış ve sayfalama butonları stilli olmalı, Tab/Escape ile pencere
   kullanılabilmeli. Kısa ekranlarda kontroller kaydırılarak erişilebilir kalmalı.

Bu değişiklik için test ve görsel çalışma zamanı sonuçları kullanıcı tarafından
doğrulanacak. Statik/derleme kontrolleri son ajan raporunda ayrıca belirtilir.

## Ajanın çalıştırdığı kontroller

- `npm run build`: başarılı. TypeScript ve Vite/CSS Modules üretim paketi oluştu;
  Vitest çalışmadı.
- `mvnw -B -ntp -Dexec.skip=true -DskipTests test-compile`: BUILD SUCCESS;
  Java uygulama/test kaynakları derlendi, test runner çalışmadı.
- `git diff --check`: geçti. Yeni migration yok; V1–V12 korunur.
- Yeni SQL'in PostgreSQL çalışma zamanı, frontend etkileşimleri ve temaların
  görsel doğrulaması henüz yapılmadı. Bunlar yukarıdaki kullanıcı adımlarıdır.

## Kullanıcı sonucu — 2026-11-16

Kullanıcı birleşik Maven build'in hatasız tamamlandığını bildirdi. Ayrıntılı son
test sayıları paylaşılmadı; ek sayısal sonuç ileri sürülmez. Önceki maddelerde
bekleyen SQL/etkileşim testleri bu kullanıcı build bildirimiyle başarılı olarak
kaydedildi; ayrıntılı raporlar ayrıca incelenmedi. Yeni uygulamanın Docker'da
çalışması ve gerçek arayüzün görsel/manuel kontrolü hâlâ bekleniyor.

Kullanıcı Docker güncellemesi ardından arayüzün önceki sürüme göre daha iyi
olduğunu bildirdi ve değişikliklerin GitHub'a gönderilmesini istedi. Bu son
bildirim kullanıcı arayüzü kabulü olarak kaydedildi; bağımsız ekran ölçümü veya
ayrıntılı manuel senaryo raporu olduğu ileri sürülmez. Ajan testleri tekrar
çalıştırmadan commit/push hazırlığı yapar.
