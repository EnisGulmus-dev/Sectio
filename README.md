# Sectio

İlk ADMIN hesabının varsayılan şirket unvanı **Bilgi teknolojileri yöneticisi
(CIO)** olur. CIO unvanı güvenlik rolünden ayrıdır. Mevcut varsayılan admin
unvanlarının geçişi ve test sırası: [CIO doğrulaması](docs/validation/admin-cio-title.md).

## Java standardı

Backend geliştirme, test ve çalıştırma ortamlarında **JDK 21 LTS** kullanır. Projenin
Maven derleme hedefi ve `.java-version` dosyası bu sürüme sabitlenmiştir.

Windows'ta kullanıcı düzeyindeki `JAVA_HOME` değerinin JDK 21 dizinini göstermesi
gerekir. Yeni bir terminal açtıktan sonra aşağıdaki komutlarla doğrulayın:

```powershell
$env:JAVA_HOME
& "$env:JAVA_HOME\bin\java.exe" -version
mvn -version
```

IntelliJ IDEA'da aşağıdaki iki ayar JDK 21 olmalıdır:

- `File > Project Structure > Project SDK`: JDK 21
- `Settings > Build, Execution, Deployment > Build Tools > Maven > Runner > JRE`:
  `Use Project JDK (21)`

## Node standardı

Frontend geliştirme ve birleşik Maven derlemesi **Node.js 24** ve **npm 11**
kullanır. Kök dizindeki `.nvmrc` dosyası doğrulanan yerel sürümü sabitler.

```powershell
node --version
npm --version
```

Beklenen ana sürümler `v24` ve `11`'dir. Maven başlangıcında `npm ci`
lockfile üzerinden kurulum yapar; ardından frontend test/build adımları çalışır.

## Backend - yerel geliştirme

Yerel PostgreSQL veritabanını başlatın ve `.env.example` dosyasını `.env` olarak
kopyalayıp kendi değerlerinizle doldurun. `ADMIN_BOOTSTRAP_*` değerleri yalnızca
veritabanında hiç admin yokken zorunludur. İlk admin parolası en az 12 karakter
olmalıdır. Admin oluşturulduktan sonra bootstrap parolası `.env` dosyasından
kaldırılabilir; yeniden başlatma mevcut admini korur ve ikinci bir admin oluşturmaz.

IntelliJ IDEA'da `backend/pom.xml` dosyasını Maven projesi olarak açın. Maven
`generate-resources` aşaması React frontend'ini derler ve çıktıyı Spring Boot
statik kaynaklarına kopyalar. Ardından `SectioApplication` sınıfı çalıştığında
Tomcat hem frontend'i hem backend API'sini `http://localhost:8080` üzerinden
sunar. Uygulama API endpoint'leri `/api/v1` ön ekini taşır; Actuator health
endpoint'i `/actuator/health` altında kalır.

Komut satırından çalıştırmak için:

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

Testleri çalıştırmak için:

```powershell
cd backend
.\mvnw.cmd test
```

Tam test komutu çalışan Docker gerektirir; PostgreSQL Testcontainers testleri
runtime yoksa başarısız olur. Kullanıcının sırayla uygulayacağı ortam, test,
temiz Docker kurulum ve health adımları [doğrulama rehberindedir](docs/validation/first-unit.md).

## Docker ve CI

`.env.example` dosyasını `.env` olarak kopyala; boş parola değerlerini doldur.
Compose DB ve backend için aynı `POSTGRES_PASSWORD` değerini kullanır. Yerel
JVM için `DB_PASSWORD` da bu değere eşit olmalıdır. Bootstrap değerleri ilk
başlangıçta zorunlu, admin oluştuktan sonra boştur; ilk parola değişimi korunur.

Compose yalnız PostgreSQL ve tek Tomcat uygulaması çalıştırır. Kök dizinden
`docker compose build backend` frontend'i Node/npm bulunan build aşamasında
derler ve JAR içine koyar. Runtime imajı yalnız JRE ve uygulamayı içerir.
`.env` Docker bağlamından dışlanır; bootstrap sırları imaja yazılmaz.
Uygulama `http://localhost:8080` üzerinde sunulur. `frontend/Dockerfile` eski
ayrı Nginx dağıtımına aittir; desteklenen Compose akışında kullanılmaz.

PostgreSQL 18 volume hedefi `/var/lib/postgresql` olarak düzeltildi. Eski verili
volume'a doğrudan uygulamadan önce yedek/restore provası gerekir; temiz kurulum
için rehberdeki ayrı proje adını kullan. Mevcut volume'u silme.

GitHub Actions, JDK 21/Node 24 ile bir kez Maven `verify` çalıştırır; bu komut
frontend kontrollerini ve backend/PostgreSQL testlerini kapsar. Ardından testleri
tekrarlamadan Docker imajı oluşturur ve health/SPA kontrolü yapar. PostgreSQL
test sınıflarının raporları bulunmalı ve sıfır atlama/hata içermelidir. Workflow
hazırlandı; bu çalışma kapsamında çalıştırılmadı.

## Authentication API

Backend token taşımak yerine sunucu taraflı HTTP session kullanır. Tarayıcı
`JSESSIONID` cookie'sini saklar; frontend bu cookie'yi JavaScript ile okumaz.

Yerel authentication akışı:

1. `GET /api/v1/auth/csrf` ile CSRF token'ı alın.
2. Token'ı `X-XSRF-TOKEN` header'ında göndererek
   `POST /api/v1/auth/login` çağrısı yapın.
3. Login session kimliğini ve CSRF token'ını yenilediği için tekrar
   `GET /api/v1/auth/csrf` çağrısı yapın.
4. `GET /api/v1/auth/me` ile session kullanıcısını okuyun.
5. `mustChangePassword=true` ise diğer API işlemlerinden önce
   `POST /api/v1/auth/change-password` çağrısını tamamlayın.
6. `POST /api/v1/auth/logout` ile session'ı ve cookie'leri temizleyin.

CSRF koruması state-changing isteklerde zorunludur. API sözleşmesinin tamamı
`docs/api/openapi.yaml` dosyasındadır.

Hesap pasifleştirme, rol değişimi ve admin parola sıfırlaması mevcut oturum
sürümünü geçersiz kılar. Yeniden aktifleştirme eski oturumu geri getirmez;
yeniden giriş gerekir. Görev koruması ve V10 doğrulaması için
[B01 rehberini](docs/validation/work-preservation.md) izleyin.

## Frontend

Entegre uygulamayı çalıştırmak için ayrı bir frontend süreci gerekmez:

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

Bu komut frontend'i otomatik derler ve aşağıdaki adresleri aynı Tomcat üzerinde
sunar:

- Uygulama: `http://localhost:8080/`
- Giriş: `http://localhost:8080/login`
- Zorunlu parola değişimi: `http://localhost:8080/change-password`
- API: `http://localhost:8080/api/v1/...`
- Sağlık kontrolü: `http://localhost:8080/actuator/health`

Yalnızca frontend üzerinde hızlı geliştirme yapmak istenirse Vite ayrıca
çalıştırılabilir:

```bash
cd frontend
npm install
npm run dev
```

Frontend `http://localhost:5173` adresinde açılır. Frontend içinden `/api/...` biçiminde yapılan istekler, `vite.config.ts` içindeki proxy tarafından `http://localhost:8080` adresindeki backend'e yönlendirilir.

Bu sayede geliştirme sırasında iki ayrı süreç çalışsa da tarayıcı kodunda backend host/port bilgisi yazılmaz.

## Kullanıcı yönetimini kontrol etme

Kullanıcı yönetimi yalnızca `ADMIN` rolüne açıktır. Değişikliklerden sonra açık
uygulamayı durdurup birleşik uygulamayı yeniden başlatın:

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

Ardından aşağıdaki akışı kontrol edin:

1. `http://localhost:8080/login` adresinden admin hesabıyla giriş yapın.
2. İlk girişse zorunlu parola değişimini tamamlayın.
3. Sol menüden **Kullanıcılar** bağlantısını açın veya
   `http://localhost:8080/users` adresine gidin.
4. **Kullanıcı ekle** ile en az 12 karakterli geçici parola kullanarak bir
   `EMPLOYEE` hesabı oluşturun ve iş unvanını seçin. Mevcut kodda `TEAM_LEADER`,
   `PROJECT_MANAGER`, `PROJECT_OWNER` unvanları proje açabilir. Görev oluşturma
   ADMIN veya proje sahibine açıktır; sıradan üyeler kendilerine atanmış görevin
   durumunu değiştirebilir. Hedef kurallar ve mevcut farklar yetki matrisindedir.
5. Yeni kaydın listede `Aktif` ve `Değişiklik bekliyor` olarak göründüğünü
   doğrulayın.
6. **Düzenle** ile adı veya rolü değiştirin.
7. **Pasifleştir** işleminden sonra kullanıcının giriş yapamadığını doğrulayın;
   ardından tekrar aktifleştirin.
8. **Parola** işlemiyle yeni bir geçici parola belirleyin. Kullanıcı bu parolayla
   giriş yaptığında `/change-password` sayfasına yönlendirilmelidir.
9. **Pasifleştir** geçici erişim kapatmadır; üyelik ve atamalar korunur.
   **Sil** kullanıcıyı normal listeden çıkarır, üyelik/atamalarını kaldırır;
   oluşturduğu işler ve proje sahipliği korunur. Silinen kullanıcılar ayrı
   salt okunur listede görülebilir; Aktifleştir ile geri açılamaz.
   Son atananı kaldırılan görevleri **Projeler → ilgili proje → Görevler →
   Atanmamış görevler** üzerinden bulup yeniden atayın.

Çalışanın işlerini **Kullanıcılar → ilgili kişi → İş takibi** üzerinden, hesabını
silmeden izleyin. Mevcut atamalardaki tamamlanmış işler de görünür. **Silinenler**
liste seçimiyle ayrılan kişinin aynı iş takibi penceresine ulaşılır. Güncel
atamalar canlı, geçmiş atamalar sonlandırma anındaki bilgilerle gösterilir.
V12 öncesi kaldırılmış atamalar geri getirilemez.
[İş takibi doğrulaması](docs/validation/employee-work-tracking.md).

Proje ayrıntısında **Takım oluştur** ile A Takımı/B Takımı gibi proje kapsamlı
gruplar tanımlanabilir. Yeni görev panelinde birden fazla çalışan, bir veya daha
fazla takım ya da **Herkes** seçilebilir. Atamalar çalışan bazında saklandığı
için aynı kişi birden fazla seçimden gelse bile göreve yalnızca bir kez eklenir.
Sol menü üstündeki okla daraltılabilir; seçim tarayıcıda korunur.

Koruma kontrolleri:

- Admin kendi hesabını pasifleştiremez.
- Admin kendi rolünü değiştiremez.
- Son aktif admin pasifleştirilemez veya çalışan rolüne indirilemez.
- Aynı e-posta adresiyle ikinci kullanıcı oluşturulamaz.
- Geçici parola 12 ile 72 karakter arasında olmalıdır.

Otomatik kontrolleri çalıştırmak için:

```powershell
cd backend
.\mvnw.cmd test
```

Bu tek komut frontend bağımlılık kontrolü, frontend testleri, frontend production
build'i ve backend testlerini birlikte çalıştırır.

Frontend kontrollerini ayrı çalıştırmak için:

```powershell
cd frontend
npm run test
npm run test:watch
npm run check
```

Frontend testlerinde Vitest, React Testing Library, user-event, jsdom ve MSW
kullanılır. Genel proje ve klasör düzeni `docs/architecture.md` dosyasında
açıklanmıştır.

Proje, üyelik, görev ve dashboard özelliklerinin endpoint ve manuel kontrol
adımları `docs/feature-testing.md` dosyasında açıklanmıştır.

Eksiksiz HTTP sözleşmesi `docs/api/openapi.yaml`, rol ve proje kapsamı kuralları
ise `docs/api/authorization-matrix.md` dosyasında tutulur.
