# Sectio

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

Beklenen ana sürümler `v24` ve `11`'dir. Maven başlangıcında `npm install`
idempotent olarak çalışır; ardından frontend test/build adımları kullanılabilir.

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

Hızlı birim testleri Docker gerektirmez. PostgreSQL Testcontainers entegrasyon
testi Docker uyumlu bir container runtime bulunduğunda otomatik çalışır; runtime
yoksa test atlanır.

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
   `EMPLOYEE` hesabı oluşturun.
5. Yeni kaydın listede `Aktif` ve `Değişiklik bekliyor` olarak göründüğünü
   doğrulayın.
6. **Düzenle** ile adı veya rolü değiştirin.
7. **Pasifleştir** işleminden sonra kullanıcının giriş yapamadığını doğrulayın;
   ardından tekrar aktifleştirin.
8. **Parola** işlemiyle yeni bir geçici parola belirleyin. Kullanıcı bu parolayla
   giriş yaptığında `/change-password` sayfasına yönlendirilmelidir.

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
