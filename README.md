# Sectio

## Backend - IntelliJ IDEA

IntelliJ IDEA'da `backend/pom.xml` dosyasını Maven projesi olarak açın ve `SectioApplication` sınıfını çalıştırın. Backend varsayılan olarak `http://localhost:8080` adresini kullanır.

## Frontend

```bash
cd frontend
npm install
npm run dev
```

Frontend `http://localhost:5173` adresinde açılır. Frontend içinden `/api/...` biçiminde yapılan istekler, `vite.config.ts` içindeki proxy tarafından `http://localhost:8080` adresindeki backend'e yönlendirilir.

Bu sayede geliştirme sırasında iki ayrı süreç çalışsa da tarayıcı kodunda backend host/port bilgisi yazılmaz.
