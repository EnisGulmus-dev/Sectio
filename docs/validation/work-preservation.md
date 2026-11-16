# B01 — Kullanıcı/üye kaldırmada görev koruması

Güncel düzeltme: pasifleştirme/silme ayrımı ve atanansız görev kısayolu için
`account-deletion-and-unassigned.md` esas alınır. Aşağıdaki ilk sürüm sonuçları
bu son değişikliği doğrulamaz; yeni test sonucu bekleniyor.

Durum: uygulama ve regresyon testleri hazır; ajan testleri veya container
kontrollerini çalıştırmadı. Yalnız aşağıdaki derleme/statik kontroller yapıldı.
Önceki birimlerin yeşil sonuçları bu değişikliğin
kanıtı sayılmaz. Commit/push/deploy yapılmadı.

## Değişen davranış

- Pasifleştir geçicidir ve üyelik/atamaları korur. Sil normal listeden çıkarır,
  üyelik/atamaları kaldırır; görev oluşturucu kimliği ve işler korunur.
  Silinen hesap salt okunur geçmiş listesinde görülür; aktifleştirilemez.
- Son atananın kaldırılması görev silmez. Proje listesi, sayfa toplamı ve genel
  görev sayaçları işi kaybetmez. Atanansız görev UI'da Atanmamış görünür.
- Proje üyesi kaldırma yalnız ilgili projeyi etkiler. Açık ve bitmiş işler kalır.
- SessionVersion pasifleştirmede artar; kullanıcı yeniden açılırsa bile eski
  oturum geçersizdir. userId/sürüm denetimi auth/me dahil authenticated API'lere
  uygulanır. Eski principal biçimi de oturumdan çıkarılır. Yeni giriş gerekir.
  Rol değişimi ve admin parola sıfırlaması da eski oturumları geçersiz kılar.
- Proje sahibi hesabı kapatılabilir; proje/sahiplik kaydı kalır. Mevcut admin
  gözlem/yönetim davranışı bu birimde yeniden tasarlanmadı; hedef ADMIN yazma
  yasağı merkezi yetki biriminde uygulanacaktır.

## 1. İlk çalıştırılacak kontrol

Docker Desktop açıkken backend dizininde:

```powershell
.\mvnw.cmd -B -ntp clean verify
```

Maven frontend test/build ve backend/PostgreSQL testlerini kapsar. Beklenen:
BUILD SUCCESS; failures/errors/skipped sıfır. Başarısızsa ilk FAIL veya ERROR
ayrıntısını ve son özeti paylaş. Gerekmedikçe ayrı frontend kontrolünü tekrarlama.

Hazırlanan anlamlı kontroller:

- WorkPreservationPostgreSqlTest: gerçek PostgreSQL + servislerle üye/kullanıcı
  kaldırma; son/çoklu atama, oluşturan kimliği, DONE geçmişi, proje sahipliği,
  takım üyeliği, başka projede atanansız görev, sayfalama count, filtreler ve
  yeniden atama. Gerçek HTTP login ile iki oturum; kapatma/yeniden açmadan sonra
  eski oturumlar 401, yeni giriş başarılı.
- CioJobTitlePostgreSqlTest: önceki V8→V9 testi hedef V9 ile sınırlandı;
  ardından V10 backfill'de mevcut üç kullanıcı/parola ve sıfır sessionVersion.
- AdminUserServiceTest: fiziksel user silmeme, erişim kapatma, son admin koruması.
- ProjectServiceTest: yalnız projeye ait atamaların kaldırılması.
- PasswordChangeRequiredFilterTest: auth/me ve change-password kaçışlarının
  kapanması; reaktivasyonun eski oturumu canlandırmaması.
- UsersPage/ProjectDetailPage testleri: kullanıcı satırının pasif olarak kalması,
  doğru onay metni, atanansız görevin görünmesi ve yeniden atama eylemi.

CI dört PostgreSQL sınıfının raporunu zorunlu kılar; eksik/sıfır/atlanmış raporla
geçmez. Bu CI değişikliği de henüz çalıştırılmadı.

## 2. Başarılı testten sonra izole ortamı güncelle

Bu adımı test sonucu değerlendirildikten sonra uygula. Gerçek veri için önce
yedek/restore provası gerekir. V10 yalnız alan ekler; V1–V9'a dokunmaz. Volume'u
silme veya eski migration'ları düzenleme. Kullanılan DB parolasını koru.

Repo kökünde:

```powershell
docker build -f backend/Dockerfile -t sectio-first-unit-backend .
$env:POSTGRES_PORT = '55432'
docker compose -p sectio-first-unit up -d --force-recreate backend --wait --wait-timeout 240
docker compose -p sectio-first-unit ps
Invoke-RestMethod http://localhost:8080/actuator/health
```

Beklenen: backend/db healthy, health UP. Eski oturumların yeni principal
biçimine geçmesi için yeniden giriş yap. Bootstrap parolasını tekrar ayarlama.

## 3. Manuel regresyon — yalnız deneme kayıtlarıyla

1. İki deneme çalışanı, iki proje ve ilk projede bir takım oluştur. Ayrılacak
   çalışanı iki projeye de ekle. İlk projede yalnız ona atanmış TODO görevi,
   iki çalışana atanmış IN_PROGRESS görevi ve tamamlanmış görev hazırla.
2. Üyeyi yalnız ilk projeden çıkar. Tüm görevler/sayaçlar korunmalı; yalnız ilk
   projenin atama/takım üyelikleri kalkmalı. Diğer projenin ataması kalmalı.
   Atanmamış görevi kalan çalışana yeniden ata; aynı görev kaydı kullanılmalı.
3. Deneme çalışanıyla iki ayrı oturum aç. ADMIN'den Pasifleştir uygula;
   üyelik/atamaları kalmalı. Aktifleştirince yeni giriş çalışmalı; eski iki
   oturum sonraki istekte 401 olmalı.
4. Başka deneme çalışanında Sil uygula. Normal listeden kalkmalı; oluşturduğu
   işler/projeler kalmalı; üyelik/atamalar kaldırılmalı. Silinen kullanıcılar
   görünümünde salt okunur bulunmalı; aktifleştirilememeli. Boşta kalan görevi
   projenin Görevler → Atanmamış görevler düğmesiyle bulup yeniden ata.
5. Kendi hesabını ve son aktif admini kapatma girişimleri reddedilmeli. Bunları
   yalnız izole test ortamında doğrula. Kalıcı silme/anonimleştirme sunulmuyor.

Kısıtlar: bu birim önceden silinmiş işleri geri getirmez; yeni görevde boş atama
ve projesiz oluşturma hâlâ sonraki görev çekirdeği işidir. İşlemin başladığı anda
zaten yürüyen isteklerin geri alınması veya açık tarayıcıdaki indirilen içeriğin
anlık temizlenmesi yoktur. Sonraki kimlik/sürüm/audit birimi servislerin tamamını
userId'ye geçirecek ve eşzamanlı iş düzenleme kurallarını tamamlayacaktır.

## Dosya grupları ve sonuç kaydı

Backend: AdminUserService, ProjectService, TaskRepository, User,
SessionUserDetails, DatabaseUserDetailsService, PasswordChangeRequiredFilter;
V10 migration. Testler yukarıdaki sınıflardır; mevcut MockMvc principal fixture'ı
yeni kimliğe uyarlandı. UI: UsersPage, ProjectDetailPage ve testleri.
Sözleşme/belge: OpenAPI, yetki matrisi, README, feature-testing, backlog,
ADR 0002 ve bu rehber. Workflow yeni PG raporunu da zorunlu kılar.

| Kontrol | Sonuç |
| --- | --- |
| Kaynak ve SQL incelemesi | Tamamlandı; eski görev-silme çağrıları kaldırıldı |
| Statik diff kontrolü | git diff --check geçti |
| Java uygulama ve test kaynakları derlemesi | mvnw -B -ntp -Dexec.skip=true -DskipTests test-compile: BUILD SUCCESS; test runner çalışmadı, npm adımları atlandı |
| TypeScript derlemesi | Yerel tsc -b: çıkış kodu 0; Vitest/Vite çalışmadı |
| Frontend Vitest | Kullanıcının 16:01:12 çalıştırması: 6 dosya, 19 test başarılı; 3.61 saniye |
| Birleşik Maven / PostgreSQL doğrulaması | Kullanıcı frontend tekrarından sonraki birleşik build'in başarılı olduğunu bildirdi; ayrıntılı son backend sayıları paylaşılmadı |
| Docker uygulama güncellemesi | Kullanıcı önerilen adımlardan sonra test ettiğini ve uygulamanın düzgün çalıştığını bildirdi; ayrıntılı ps/health çıktısı paylaşılmadı |
| Manuel görev/oturum koruma senaryoları | Ayrıntılı senaryo doğrulaması bekleniyor |

### İlk kullanıcı çalıştırması — 2026-11-16 15:59

BUILD FAILURE, `test-frontend` exit code 1. Aynı zamanlı Vitest sonuç kaydında
beş dosya başarılı; yalnız UsersPage'in yeni hesap kapatma testi başarısız.
Kaynak incelemesinde jsdom HTMLDialogElement'in showModal/close implementasyonu
olmadığı, ConfirmDialog'un bu metotları çağırdığı doğrulandı. Test setup'ına
yalnız bu metotlar yoksa open attribute durumunu ve close olayını taklit eden
destek eklendi. Gerçek tarayıcıdaki modal/focus davranışı değiştirilmedi;
bu basit destek erişilebilirlik veya focus-trap doğrulaması sayılmaz.

Düzeltme henüz test edilmedi. Backend dizininden önce
`npm --prefix ../frontend run test -- --reporter=verbose` çalıştırılmalı.
Geçerse tam Maven verify adımına dönülür. Hata sürerse FAIL bölümündeki ayrıntı
paylaşılmalı; Maven'in son exit-code özeti tek başına neden kanıtı değildir.

### Frontend tekrar sonucu — 2026-11-16 16:01:12

Kullanıcının paylaştığı özet: Test Files 6 passed (6), Tests 19 passed (19),
Duration 3.61s. Test ortamı düzeltmesinden sonra frontend testleri geçti.
Sıradaki adım backend dizininde `.\mvnw.cmd -B -ntp clean verify` komutuyla
tam birleşik doğrulamadır; Docker uygulama güncellemesi bu sonuçtan sonra gelir.

### Birleşik tekrar sonucu — 2026-11-16

Kullanıcı tam Maven build'in başarılı tamamlandığını bildirdi. Ayrıntılı son
backend test sayıları paylaşılmadı; yeni sayısal sonuç ileri sürülmez. Ajan
testleri çalıştırmadı. Sıradaki bekleyen doğrulamalar Docker imaj güncellemesi,
V10 ile health kontrolü ve deneme kayıtlarıyla görev/oturum korumasıdır.

### Docker güncellemesi sonrası kullanıcı bildirimi — 2026-11-16

Kullanıcı uygulamayı test ettiğini ve düzgün çalıştığını bildirdi. ps/health
çıktısı veya tek tek görev/oturum senaryolarının sonucu paylaşılmadı. Bu bildirim
uygulama çalışırlığı olarak kaydedildi; veri korumasının her manuel kabul
ölçütünü tek başına kanıtladığı varsayılmadı. Sonraki adım deneme kayıtlarıyla
son atananın kaldırılması, yeniden atama ve eski oturumun engellenmesidir.
