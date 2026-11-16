# İlk çalışma birimi: kullanıcı tarafından doğrulama

2026-11-16: Aşağıdaki testler, derleme ve container kontrolleri ajan tarafından
çalıştırılmadı. Kullanıcının ilk çalıştırma sonucu aşağıda kayıtlıdır. Önceki
target raporları bu değişikliğin kanıtı değildir. CI dosyası hazırlandı; push
yapılmadığından bu çalışma kapsamında CI tetiklenmedi.

## 1. Ortamı kontrol et

Repo kökünde PowerShell kullan. JDK 21, Node 24, npm 11 ve çalışan Docker Desktop
gerekir. İlk adımda yalnız şu komutları çalıştırıp sonuçları paylaşabilirsin:

```powershell
java -version
node --version
npm --version
docker version
```

Beklenen: Java 21, Node v24, npm 11; Docker hem Client hem Server bilgisi döner.
Hata varsa sonraki adıma geçmeden giderilir. Ortam çıktısında sır paylaşma.

### Kullanıcının paylaştığı ortam sonucu — 2026-11-16

- Java: Microsoft OpenJDK 21.0.12.1 — ana sürüm uygun.
- Node: v24.15.0 — uygun.
- npm: 11.12.1 — uygun.
- Docker: Kullanıcı WSL ve Docker Desktop kurulumunu tamamladı. Paylaşılan son
  `docker version` çıktısında Client ve Server 29.9.3; Docker Desktop 4.99.0;
  engine linux/amd64. `desktop-linux` context'i aktif ve bağlantı hatası yok.
- Compose: Kullanıcının paylaştığı çıktı v5.7.0. Ortam ön kontrolü tamamlandı;
  birleşik test/derleme sonucu henüz bekleniyor.

Docker Desktop kuruluysa Başlat menüsünden açıp yeni PowerShell terminalinde
`docker version` çalıştır. Kurulu değilse [resmî Windows kurulum rehberini](https://docs.docker.com/desktop/setup/install/windows-install/)
izleyerek WSL 2 backend ile kur; ardından Docker Desktop'ı aç ve yeni terminalde
`docker version` çalıştır. İstenen Linux container ortamıdır. WSL gereksinimi
veya kurulum hatası çıkarsa o çıktıyı paylaş; tam test adımına henüz geçme.
Yukarıdaki kurulum sorun giderme adımları son paylaşılan çıktıyla tamamlandı;
mevcut sıradaki adım 2'dir.

## 2. Birleşik test ve derleme

Repo kökünden:

```powershell
Push-Location backend
.\mvnw.cmd -B -ntp clean verify
Pop-Location
```

Beklenen: `BUILD SUCCESS`; frontend Vitest, TypeScript/Vite build ve backend
JUnit testleri geçer. `AuthenticationPostgreSqlTest` ve
`AdminBootstrapPostgreSqlTest` gerçek PostgreSQL container açar; Docker yoksa
artık atlanmaz, başarısız olur. Web `ProjectTaskAuthorizationIntegrationTest`
mock repository kullanır; PostgreSQL testinin yerine geçmez.

JUnit sonuçları `backend/target/surefire-reports` altında bulunur. Toplam
tests/failures/errors/skipped sayılarını ve frontend test özetini kaydet.
Frontend ayrıca `npm run check` ile tekrar çalıştırılmak zorunda değildir;
Maven zaten bu kontrolleri kapsar. Sadece Docker gerektirmeyen hızlı tanılama
gerekirse backend içinde `.\mvnw.cmd '-Dtest=!*PostgreSqlTest' test` kullanılır. Bu filtreli
komut tam doğrulama sayılmaz.

### İlk birleşik çalıştırma — 2026-11-16 14:16

Kullanıcının paylaştığı Maven çıktısı: BUILD FAILURE; backend 60 test,
0 failures, 2 errors, 0 skipped. Frontend production derlemesi geçti;
backend test aşamasında durulduğu için frontend Vitest aşamasına ulaşılmadı.
Testcontainers Docker'a bağlandı; PostgreSQL 18.7 üzerinde V1–V8 uygulandı.

İki hata için hazırlanan düzeltmeler (henüz yeniden çalıştırılmadı):

- AuthenticationPostgreSqlTest: uygulama context'ini bean olarak aramak yerine
  gerçek sunucu portu context environment içindeki `local.server.port` değerinden alınır.
- AdminBootstrapPostgreSqlTest: uygulama gerçek kullanımındaki gibi SERVLET
  context ile, rastgele portta başlatılır. NONE context'inde web güvenliği için
  gerekli AuthenticationConfiguration bulunamıyordu. Üretim güvenlik ayarları
  değiştirilmeden test başlangıç biçimi düzeltildi.

Mevcut sıradaki adım: backend dizininde `.\mvnw.cmd -B -ntp clean verify`
komutunu yeniden çalıştır; test özetleri ve nihai build sonucunu paylaş.
Docker kurulum kontrolüne yalnız bu adım geçtikten sonra ilerle.

### İkinci birleşik çalıştırma — 2026-11-16 15:04

Kullanıcının paylaştığı Maven çıktısı: BUILD FAILURE; backend 60 test,
1 failure, 0 errors, 0 skipped. AuthenticationPostgreSqlTest geçti;
önceki context başlatma hataları tekrarlanmadı. Bootstrap testi admin oluşturma
ve bootstrap değeri olmadan yeniden başlatma aşamalarını tamamladı; son negatif
senaryoda doğru `IllegalStateException` doğrudan fırlatıldı. Test ise bunu başka
bir exception'ın kök nedeni olarak aradığından assertion başarısız oldu.

Hazırlanan düzeltme: doğrudan exception türü ve tam hata mesajı doğrulanır;
üretim kodu ve eksik bootstrap ayarını reddetme kuralı değiştirilmedi.
Bu düzeltme henüz çalıştırılmadı. Backend dizininde
`.\mvnw.cmd -B -ntp clean verify` yeniden çalıştırılmalı. Vitest aşaması
bu çalıştırmada da backend başarısızlığı nedeniyle çalışmadı.

### Üçüncü birleşik çalıştırma — 2026-11-16

Kullanıcı tüm testlerin geçtiğini ve `BUILD SUCCESS` sonucunu bildirdi.
Birleşik Maven doğrulama adımı kullanıcı bildirimiyle başarılı olarak kaydedildi.
Bu son çalıştırmanın ayrıntılı test sayıları/raporları paylaşılmadığından burada
yeni sayısal sonuç ileri sürülmez. Ajan testleri çalıştırmadı.
Sıradaki adım Docker imaj derlemesi, ardından izole kurulum ve health kontrolüdür.

## 3. İzole temiz Docker kurulumu

Kullanıcı Docker imaj derlemesinin hatasız tamamlandığını bildirdi.
Sıradaki bekleyen doğrulama izole Compose başlangıcı ve health kontrolüdür.

Önce container başlatmadan imaj derlemesini ayrı doğrula. Repo kökünden:

```powershell
docker build -f backend/Dockerfile -t sectio-first-unit-backend .
```

Bu adım `.env` değerlerini gerektirmez; veritabanı/container başlatmaz. Dockerfile
`-DskipTests` kullanır; frontend production derlemesi ve JAR paketlemesi yapılır,
testler tekrarlanmaz. Sonuç başarılı olduğunda aşağıdaki kuruluma geçilir.

Önce mevcut 8080 uygulamasını durdur. `.env` içeriğini terminale yazdırmadan
düzenle: POSTGRES_PASSWORD güçlü/dolu olsun; yerel JVM için DB_PASSWORD aynı
değer olsun. İlk admin için ADMIN_BOOTSTRAP_NAME, EMAIL ve 12–72 karakter
PASSWORD ayarla. Bunlar build arg değil runtime environment olarak geçer.
Admin oluşunca bootstrap parola değeri boşaltılabilir. `.env` commit edilmez.

PostgreSQL 18 volume hedefi `/var/lib/postgresql` olarak değişti. Eski container
veya volume'u bu adımda kullanma/taşıma/silme. Mevcut verili kurulumda önce
mantıksal yedek ve ayrı hedefe restore provası gerekir. `down -v` kullanma.
Yeni proje adı yeni volume oluşturur; dolu/eski proje adı seçme.

```powershell
$env:POSTGRES_PORT = '55432'
docker compose -p sectio-first-unit config --quiet
docker compose -p sectio-first-unit up -d --build --wait --wait-timeout 240
docker compose -p sectio-first-unit ps
```

Beklenen: yalnız db/backend servisleri sağlıklı, uygulama 8080 portunda.
`config --quiet` genişletilmiş sırları yazdırmaz. İmaj derlemesi frontend build
yapar, testleri çalıştırmaz (`skipTests` iki test runner'ını da atlar); test
kanıtı 2. adımdır. İndirme ve ilk derleme internet gerektirir.

## 4. Health ve temel akış

```powershell
Invoke-RestMethod http://localhost:8080/actuator/health
(Invoke-WebRequest http://localhost:8080/).StatusCode
(Invoke-WebRequest http://localhost:8080/login).StatusCode
```

Beklenen: health `UP`, iki HTTP yanıtı 200 ve sayfa React uygulamasını açar.
Tarayıcıda bootstrap hesabıyla giriş yap: zorunlu parola değişimi görünmeli;
değişimden sonra dashboard açılmalı. Çıkış sonrası iş API'leri oturumsuz 401
dönmeli; CSRF'siz yazma reddedilmeli. AuthenticationPostgreSqlTest bu HTTP
akışının otomatik kontrolünü de içerir.

Bootstrap değerlerini `.env` içinde boşaltıp aşağıdaki işlemi uygula:

```powershell
docker compose -p sectio-first-unit up -d --force-recreate backend --wait --wait-timeout 240
```

Mevcut admin yeni parolayla giriş yapabilmeli; ikinci admin oluşmamalı.
Kullanıcı listesinden ilk kurulumdaki tek adminin aynı kayıt olarak kaldığını
kontrol et. Volume kalıcılığını da doğrulamak için uygulama ve DB container'larını
volume'u koruyarak kaldırıp yeniden oluştur:

```powershell
docker compose -p sectio-first-unit down
docker compose -p sectio-first-unit up -d --wait --wait-timeout 240
```

Bootstrap değerleri hâlâ boşken health UP olmalı; admin değiştirilen parolayla
girebilmeli ve liste aynı kayıtları göstermeli. Bu kontrol PostgreSQL volume
hedefi değişikliğinden sonra verinin gerçekten kalıcı olduğunu sınar.
İşin bitince volume'u koruyarak durdur:

```powershell
docker compose -p sectio-first-unit down
Remove-Item Env:POSTGRES_PORT
```

Health hatasında yalnız ilgili hata satırlarını paylaş; `.env`, parola, cookie,
CSRF token veya genişletilmiş Compose yapılandırması paylaşma.

## Sonuç kaydı

| Kontrol | Sonuç |
| --- | --- |
| Kaynak/belge incelemesi | Tamamlandı; requirements.md bulguları |
| git diff --check | Geçti; yalnız LF/CRLF normalizasyon uyarıları |
| POM XML / frontend package JSON | Okuma/parse başarılı; derleme doğrulaması değildir |
| Maven / Vitest / PostgreSQL | Kullanıcı son çalıştırmada tüm testlerin geçtiğini ve BUILD SUCCESS bildirdi; ayrıntılı son test sayıları paylaşılmadı |
| Docker imaj derlemesi | Kullanıcı hatasız tamamlandığını bildirdi |
| Docker izole kurulum / health / yeniden başlatma | Çalıştırılmadı; kullanıcı sonucu bekleniyor |
| GitHub Actions | Hazırlandı; çalıştırılmadı |

CI ayrıca iki PostgreSQL sınıfının Surefire XML raporunu zorunlu kılar:
rapor eksikse, test sayısı sıfırsa veya skipped/failures/errors varsa geçmez.
Bu rapor kapısı da henüz çalıştırılmadı.

Bu sonuçlar gelmeden ilk birimin çalışma zamanı doğrulaması tamamlandı denmez.
Bugünkü yetki testleri mevcut davranışı test eder; hedef ADMIN yasağı/workspace
izolasyonu bu değişiklikte uygulanmadı. Sıradaki birim B01 görev/veri korumasıdır.

## Değişen dosyalar

- `.dockerignore`: sırlar ve yerel derleme çıktıları Docker bağlamından dışlanır.
- `.env.example`: boş parola alanları, yerel/Compose parola açıklaması.
- `.github/workflows/ci.yml`: birleşik test, PostgreSQL ve Docker smoke akışı.
- `compose.yaml`: kök build bağlamı, tek uygulama, ortak parola, bootstrap, health ve PG18 volume hedefi.
- `backend/Dockerfile`: Node/npm ile frontend içeren JAR derlemesi.
- `backend/pom.xml`: npm ci ve frontend için skipTests desteği.
- `backend/src/test/java/com/enis/sectio/auth/AuthenticationPostgreSqlTest.java`: Docker yokken sessiz atlama kaldırıldı.
- `backend/src/test/java/com/enis/sectio/user/bootstrap/AdminBootstrapPostgreSqlTest.java`: Docker yokken sessiz atlama kaldırıldı.
- `docs/requirements.md`: kararlar, sürümler ve kaynakla doğrulanan farklar.
- `docs/backlog.md`: amaç/kapsam/kabul/risk/doğrulama ve bağımlılıklar.
- `docs/api/authorization-matrix.md`: mevcut yetkiler, hedef kurallar ve açık kararlar.
- `docs/decisions/0001-account-workspace-and-deployment.md`: mimari karar ve geçiş sınırları.
- `docs/architecture.md`, `docs/feature-testing.md`, `README.md`: gerçek kod/dağıtım/test davranışına göre güncellendi.
- `docs/validation/first-unit.md`: bu rehber ve bekleyen sonuçlar.

Migration veya iş modeli değişmedi; commit, push, merge, deploy yapılmadı.
