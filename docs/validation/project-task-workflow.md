# Proje ve görev akışı doğrulaması

Hazırlanma: 2026-11-17. Ajan testleri çalıştırmadı. Frontend üretim derlemesi ve
testleri çalıştırmadan backend `test-compile` kontrolü başarılı.

## 1. Testler — kullanıcı çalıştıracak

Docker Desktop açık ve engine hazırken backend dizininde:

```powershell
.\mvnw.cmd -B -ntp clean verify
```

Bu komut backend birim/PostgreSQL Testcontainers testlerini, frontend testlerini
ve üretim derlemesini kapsar. BUILD SUCCESS görülmeden sonraki adıma geçme.
Hata varsa ilk başarısız testin ayrıntısını paylaş; yalnız son Maven özeti yeterli
olmayabilir. Docker volume silme veya testleri atlayarak geçme gerekmez.

Hazırlanan regresyonlar:

- ProjectServiceTest: sahip/seçilen kişiler, tekrar eden ID, pasif seçimde hiçbir
  kayıt oluşturmama ve sıradan çalışanın aday listesini okuyamaması.
- WorkPreservationPostgreSqlTest: V13 ile aynı takıma iki görev, takımın görev
  bitince kullanılabilmesi, başka projenin takımını reddetme, takım silinince iş
  ve kişi atamalarını koruma; eski göreve takım uydurmama.
- Aynı PostgreSQL sınıfı: proje/kişisel ACTIVE, COMPLETED, CANCELLED, ALL içerik ve
  sayfa toplamları; HTTP varsayılan aktif görünümü ve eski status=DONE filtresi;
  durum geçişi; ilk proje üyeliklerinin birlikte kaydedilmesi.
- Frontend: yeni proje üye seçim payload'ı; takımın ikinci görevde yeniden seçimi;
  tek tek kişi seçiminin takım diye yorumlanmaması; tamamlanan kişisel görevin
  aktif görünümden çıkması ve grup değişimi; proje sekmelerinde grubun korunması.

## 2. Başarılı sonuçtan sonra uygulamayı güncelle

Repo kökünde sırayla:

```powershell
docker compose -p sectio-first-unit start db
docker compose -p sectio-first-unit up -d --build --no-deps --force-recreate backend --wait --wait-timeout 240
```

V13 otomatik uygulanır. Mevcut görevler ve takımlar korunur. Geçmişte yalnız
kişiye açılmış atamalara takım bağlantısı tahmin edilerek eklenmez; gerekiyorsa
görevi düzenleyip mevcut takımı seçerek kaydet.

## 3. Tarayıcıda kontrol

1. Ctrl+F5 ile yenile. Yeni proje formunda kişileri ara/seç; kaydettikten sonra
   projedeki Takımlar ve üyeler bölümünde seçilenleri ve proje sahibini gör.
2. Burada bir takım oluştur. Görevler bölümünde aynı kayıtlı takımı iki farklı
   görev için seç; takım listede kalmalı, iki görev de takım adını göstermeli.
3. Bir görevi tamamla, birini iptal et. Varsayılan Aktif görevler görünümünden
   çıkmalılar; Tamamlanan görevler / İptal edilen görevler altında görünmeliler.
   Devam ediyor ve Yapılacak görevleri aktif listede kalmalı. Görevlerim ekranında
   da kendi atamalarınla aynı geçişi kontrol et.
4. Takımlı görevi düzenle: önceden seçilmiş takım korunmalı. Deneme takımını
   silersen görevler ve kişilere atamalar kalmalı, takım adı kalkmalı.
5. Açık/koyu temada dört önceliği kontrol et: düşük gri, normal mavi, yüksek
   kehribar, kritik kırmızı. Proje ve kişisel görevlerde metin okunabilmeli.
6. Menüyü daralt; koyu modda beyaz S, açık modda renkli S görünmeli. Dar ekranda
   yeni proje kişi listesi ve görev grup düğmeleri taşmamalı.
