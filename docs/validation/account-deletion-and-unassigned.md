# Pasifleştirme / silme ayrımı ve atanansız görev görünümü

Ek kapsam: ayrılan kişinin sonlandırılmış atama bağlantılarını tutan V12 ve
İş geçmişi ekranı için `employee-offboarding-history.md` güncel rehberdir.

Bu belge B01'in önceki erişim kapatma davranışını düzelten güncel sürümdür.
Önceki yeşil sonuçlar aşağıdaki yeni değişiklikleri doğrulamaz. Ajan testleri
çalıştırmaz; kullanıcı sırasıyla doğrular.

| İşlem | Kullanıcı listesi | Üyelik/atama | Erişim | Geri dönüş |
| --- | --- | --- | --- | --- |
| Pasifleştir | Normal listede Pasif | Korunur | Kapalı; eski oturum iptal | Aktifleştir ile yeni giriş |
| Sil | Normal listeden çıkar; silinenler görünümünde salt okunur | Kaldırılır | Kapalı | Bu sürümde geri yükleme yok |

Her iki işlemde görevler ve geçmiş korunur. Silinen hesap e-postası hâlâ
rezerve edilir; aynı e-posta yeni kimlikle eski işlerin devralınmasına yol açmaz.
Kendi hesabını ve son aktif admini silme/pasifleştirme koruması sürer.

## Atanmamış görevleri bulma

Projeler → ilgili proje → Görevler → **Atanmamış görevler** düğmesi.
Aynı seçenek **Atanana göre filtrele** açılır alanında da vardır. Görevi
**Düzenle** ile mevcut proje üyelerine yeniden ata. Atama sonrası görev bu
filtreden çıkar; Tüm görevler seçeneğinde görünür. Görevlerim sayfası yalnız
size atanmış işleri içerir. Atanmamış işleri görmek için proje erişimi gerekir.

## 1. Kullanıcının ilk çalıştıracağı kontrol

Docker Desktop açık; backend dizininde:

```powershell
.\mvnw.cmd -B -ntp clean verify
```

Beklenen BUILD SUCCESS; backend ve frontend testleri geçer. Hata varsa ilk
FAIL/ERROR ayrıntısını paylaş. Testler hazırlanmıştır; ajan çalıştırmadı.
Özellikle: pasifleştirmede üyeliklerin kalması, silmede default listeden çıkma,
silinen hesabın aktifleştirilememesi, V11'de eski pasif hesapların korunması,
unassigned filtre/count/yetki ve frontend kısayolu kapsanır.

## 2. Testten sonra güncelleme

Repo kökünde sırayla; bir adım başarısızsa devam etme:

```powershell
docker build -f backend/Dockerfile -t sectio-first-unit-backend .
$env:POSTGRES_PORT = '55432'
docker compose -p sectio-first-unit up -d --force-recreate backend --wait --wait-timeout 240
docker compose -p sectio-first-unit ps
Invoke-RestMethod http://localhost:8080/actuator/health
```

V11 mevcut veriyi silmez; volume/parolalar korunur. Eski pasif hesaplar silinmiş
sayılmaz. Güncellemeden önce verili ortamın yedek/restore planını koru.

## 3. Deneme kayıtlarıyla manuel kontrol

1. Bir çalışanı pasifleştir. Normal listede Pasif kalmalı, proje üyelikleri ve
   atamaları durmalı; aktifleştirince yeni giriş yapabilmeli.
2. Farklı bir çalışanı Sil. Normal listeden kalkmalı. Silinen kullanıcıları
   göster seçilince salt okunur görünmeli; Aktifleştir/Parola/Düzenle olmamalı.
3. İlgili projenin Görevler sekmesine git; Atanmamış görevler düğmesini seç.
   Silinen kullanıcının tek atanan olduğu görev listelenmeli. Atananı kalan
   çoklu görev bu filtrede olmamalı; Tüm görevler içinde durmalı.
4. Boştaki görevi başka üyeye ata. Atanmamış listesi yenilenmeli; görev normal
   listede aynı kayıt olarak görünmeli. Başka projenin görevleri etkilenmemeli.

Bu düzenlemeyle yeni migration V11, User ve user list/service/controller/DTO,
bootstrap sorgusu, dashboard kullanıcı sayısı, task query/service/controller,
frontend kullanıcı ekranı ve proje filtreleri değişti. API/matris/ADR güncellendi.
Güncel sonuçlar son ajan raporunda ve kullanıcı geri bildirimlerinde kaydedilir.

## Hazırlama sırasındaki kontroller

- Java uygulama ve test kaynakları: `mvnw -B -ntp -Dexec.skip=true -DskipTests test-compile`
  BUILD SUCCESS. Frontend npm adımları atlandı; hiçbir test runner çalışmadı.
- TypeScript: yerel `tsc -b` çıkış kodu 0. Vitest/Vite çalıştırılmadı.
- Statik diff kontrolü geçti. Yeni V11 dahil test/DB/arayüz davranışlarının
  çalışma zamanı sonucu hâlâ kullanıcı tarafından doğrulanacak.

## Kullanıcı doğrulaması

2026-11-16: Kullanıcı birleşik Maven build'in hatasız tamamlandığını bildirdi.
Ayrıntılı son test sayıları paylaşılmadığından sayısal sonuç eklenmedi. Ajan
testleri çalıştırmadı. Sıradaki adım aynı izole Compose projesinde Docker imajını
güncellemek, health ve yeni Pasifleştir/Sil/Atanmamış görevler akışını doğrulamaktır.
