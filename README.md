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

## Backend - yerel geliştirme

Yerel PostgreSQL veritabanını başlatın ve `.env.example` dosyasını `.env` olarak
kopyalayıp kendi değerlerinizle doldurun. `ADMIN_BOOTSTRAP_*` değerleri yalnızca
veritabanında hiç admin yokken zorunludur. İlk admin parolası en az 12 karakter
olmalıdır. Admin oluşturulduktan sonra bootstrap parolası `.env` dosyasından
kaldırılabilir; yeniden başlatma mevcut admini korur ve ikinci bir admin oluşturmaz.

IntelliJ IDEA'da `backend/pom.xml` dosyasını Maven projesi olarak açın ve
`SectioApplication` sınıfını çalıştırın. Backend varsayılan olarak
`http://localhost:8080/api/v1` adresini kullanır.

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

## Frontend

```bash
cd frontend
npm install
npm run dev
```

Frontend `http://localhost:5173` adresinde açılır. Frontend içinden `/api/...` biçiminde yapılan istekler, `vite.config.ts` içindeki proxy tarafından `http://localhost:8080` adresindeki backend'e yönlendirilir.

Bu sayede geliştirme sırasında iki ayrı süreç çalışsa da tarayıcı kodunda backend host/port bilgisi yazılmaz.
