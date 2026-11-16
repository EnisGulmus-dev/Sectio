# İşten ayrılma: kullanıcıyı kaldır, geçmişi gözlemle

Güncel arayüz ve silmeden iş takibi için `employee-work-tracking.md` esas alınır.
Bu belgedeki eski checkbox/İş geçmişi etiketleri, kompakt Silinenler liste seçimi
ve tüm kullanıcılarda İş takibi eylemiyle değiştirildi. Saklama kuralları korunur.

## Çalışan sürüm teşhisi

2026-11-16 read-only Docker kontrolünde sectio-first-unit-backend imajı 16:05
yerel oluşturma zamanını taşıyordu. Sil ayrımı/V11 dosyaları 16:26 sonrasıydı.
Bu yüzden ekranda Erişimi kapat görülmesi güncel Sil akışının çalıştığını
göstermiyordu. Kaynakta Sil ve ayrı silinen hesap görünümü vardır.

## Yeni gözlem akışı

- Pasifleştir geçici; Sil normal listeden kaldırır, üyelik/atamaları sonlandırır.
- Oluşturulmuş işler ve sonlandırılan atama bağlantıları saklanır. Silinen
  kullanıcıları göster → ilgili kişide İş geçmişi üzerinden, hangi proje/görevde
  ataması olduğu ve o anda görev durumunun ne olduğu gözlemlenir.
- Durum tarihsel snapshot'tır; güncel iş için Projeye git kullanılabilir.
- Geçmiş, yalnız bu sürümden sonra kullanıcı silme/üye kaldırma ile sonlanan
  atamaları kaydeder. Önceden kaldırılmış atamalar mevcut veriden çıkarılamaz.
  Bu ekran bütün düzenlemelerin audit'i veya çalışma süresi raporu değildir.

## Doğrulama sırası — testleri kullanıcı çalıştırır

1. Docker Desktop açıkken backend dizininde:

```powershell
.\mvnw.cmd -B -ntp clean verify
```

PG testleri: üç durumdaki atamanın (TODO/IN_PROGRESS/DONE) üyelik kaldırma/silme
ile arşivlenmesi, listeden çıkan kişide okunabilmesi, sayfa toplamı, ADMIN HTTP
erişimi ve çalışana 403; görev ayrıca silinse de snapshot'ın kalması. Zorla
geçmiş yazım hatasında kullanıcı ve atamaların silinmemesi de hazırlanmıştır.
Frontend testi silinen kullanıcıdan İş geçmişi penceresini açmayı doğrular.

2. Başarılı testten sonra repo kökünde imajı derle. Sonraki komuta yalnız build
başarılıysa geç:

```powershell
docker compose -p sectio-first-unit start db
docker compose -p sectio-first-unit up -d --build --no-deps --force-recreate backend --wait --wait-timeout 240
docker compose -p sectio-first-unit ps
```

`--no-deps` mevcut DB container'ının port/volume ayarlarını değiştirmez. DB
zaten çalışıyor olmalıdır. Parolalar ve volume korunur; V11/V12 uygulanır.
Tarayıcıda http://localhost:8080/users aç, Ctrl+F5 yap; gerekiyorsa tekrar giriş.
Sil ve Silinen kullanıcıları göster görülmeli; Erişimi kapat eski sürüm etiketidir.

3. Yeni deneme çalışanına proje içinde bir TODO, bir IN_PROGRESS ve bir DONE
görevi ata. Kullanıcıyı Sil. Normal listeden çıkmalı, görevler korunmalı.
4. Silinen kullanıcıları göster → İş geçmişi: üç görevin ad/proje/durum/tarih
kayıtları görünmeli. Düzenleme/aktifleştirme eylemi olmamalı. Görevler sekmesinde
Atanmamış görevler düğmesiyle açık görevi başka üyeye ata; eski kişinin geçmiş
kaydı değişmemeli.

Hiçbir test ajan tarafından çalıştırılmadı. Derleme/statik kontrol sonuçları son
ajan raporunda; bu yeni kapsam için kullanıcı çalışma zamanı sonucu bekleniyor.

## Kullanıcı çalıştırması — 2026-11-16 21:23

BUILD FAILURE: 71 test, 0 failures, 4 errors, 0 skipped. Dört PostgreSQL sınıfı
Docker keşfi sırasında başlayamadı; bu sonuç yeni iş geçmişi davranışını
doğrulamaz. Ajan mevcut Surefire kaydını ve Docker durumunu salt okunur inceledi.
`docker version` aktif desktop-linux context'inde dockerDesktopLinuxEngine
pipe'ını bulamadı ve Server bilgisi dönmedi. Engine'e erişim yoktur.

Sonraki adım: Docker Desktop'ı aç, Engine running durumunu bekle; aynı terminalde
`docker version` ile Server bölümünü doğrula. Sonra backend dizininde tam Maven
verify komutunu yeni çalıştırmada tekrarla. Testcontainers'ın aynı JVM'deki
başarısız keşif önbelleği nedeniyle testler devam eden eski JVM'de sürdürülmez.
Testleri sessiz atlama veya Docker ayarlarını körlemesine değiştirme uygulanmadı.

Kullanıcı Docker Desktop'ın kapalı olduğunu fark edip açtığını ve yeni build'in
hatasız geçtiğini bildirdi. Yeni ayrıntılı test sayıları paylaşılmadı. Sıradaki
adım mevcut DB'yi başlatıp backend'i --build ile yenilemek ve yeni Sil/İş geçmişi
arayüzünü doğrulamaktır. Ajan testleri veya container güncellemesini çalıştırmadı.
