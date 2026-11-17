# Sectio gereksinimleri

Ek kullanıcı kararı: ilk ADMIN hesabının varsayılan şirket içi unvanı CIO olur.
Bu unvan güvenlik rolünden ayrıdır; unvana yeni yetki eklenmez. Geçiş ve
doğrulama: `validation/admin-cio-title.md`.

Son kullanıcı düzeltmesi: Pasifleştir geçici erişim kapatmadır ve atamaları
korur. Sil ayrı bir durumdur; normal kullanıcı listesinden kaldırır, üyelik ve
atamaları sonlandırır, geçmiş işleri korur. Silinen hesap geçmiş
görünümünde yer alır; normal aktifleştirme ile geri açılamaz. Ayrı Geri yükle
işlemi yeni geçici parola ister; eski atamaları otomatik açmaz. Silinenler'den
Kalıcı sil, kişisel hesap alanlarını anonimleştirir ve listeden kaldırır;
iş kayıtları korunur, hesap geri yüklenemez, e-posta yeniden kullanılabilir.
Yeni ve düzenlenen e-posta adresleri ASCII karakterlerle sınırlandırılır; form içinde hata gösterilir.
Doğrulama: `validation/user-recovery-and-purge.md`. Atanansız görevler proje
Görevler sekmesinde açık bir filtre ve kısayolla bulunabilir. Ayrıntı:
`validation/account-deletion-and-unassigned.md`.

İşten ayrılan kişi normal listeden çıkarılırken geçmişte hangi görevleri aldığı
gözlemlenebilmelidir. Kaldırılan atama bağlantılarının bundan sonrası için
saklanması ve ADMIN salt okunur İş geçmişi ekranı ADR 0004 ile tanımlandı.

Son kullanıcı düzeltmesi: iş takibi silme işlemine bağlı olamaz. Aktif, pasif ve
silinmiş çalışanlar mevcut/tamamlanmış işleriyle izlenir. UI büyük checkbox
yerine kompakt Kullanıcılar/Silinenler seçimi ve temaya uygun İş takibi paneli
kullanır. Güncel kapsam ADR 0005 ve `validation/employee-work-tracking.md` içindedir.

2026-11-16 tarihli kullanıcı kararları esas alınır. Bu belge hedef ürünü tanımlar;
uygulama durumu `backlog.md` ve `api/authorization-matrix.md` içinde ayrıdır.

## Hesaplar ve çalışma alanları

- R1: Bireysel kullanıcı kayıt olabilir; kişisel workspace, projeler ve projesiz,
  atanansız görevler kullanabilir. Topluluk kurabilir ve kişileri davet edebilir.
  Topluluk hesap türü değildir; paylaşımlı proje, görev ve oda alanıdır.
  Kişisel içerik topluluğa kendiliğinden açılmaz.
- R2: Kurumsal hesabı yalnız yetkili IT/ADMIN oluşturur. Kurum e-postası veya
  kullanıcı adı/parola ile giriş ve zorunlu ilk parola değişimi desteklenir.
  Kurumsal kimlik bireysel kimlikten ayrıdır; oturumdan kişisel alana geçilemez.
  Aynı e-posta hesapları bağlamaz; kurumsal kapatma bireysel hesabı etkilemez.
  Mevcut kullanıcılar güvenli geçişle kurumsal kabul edilir.
- R3: Kişisel/topluluk/kurumsal workspace ayrılır; oda workspace içindedir.
  Her görev tam bir workspace'e aittir. Alanlar arası erişim varsayılan kapalıdır.
  Bireysel ve kurumsal içerik ortak görev/Bugün görünümünde birleştirilmez.

## Yetkiler ve görevler

- R4: Kurumsal ADMIN kullanıcı erişimi, geçici parola/sıfırlama ve takım/oda
  üyeliklerini yönetir; mevcut parolaları göremez. Kurumun iş içeriğini gözlemler,
  proje/görev oluşturamaz, değiştiremez, silemez, atama/durum değiştiremez.
  Özel sohbetleri gözlem hakkı yoktur. Platform yöneticisi ayrı kavramdır.
- R5: Çalışan görev oluşturabilir; takım lideri yetkili olduğu takıma görev
  atayabilir. Unvan ve yetki rolü ayrılır. Proje yönetimi ve rol birleşimleri
  için açık politika gerekir; belirsizlikler yetki matrisinde tutulur.
- R6: Kurumsal dahil bütün görevlerde proje ve atama isteğe bağlıdır. Kişi,
  birden fazla kişi veya takım hedeflenebilir. Takım ataması, o anki kişilere
  açılan atama anlık görüntüsünden ayrı ve kalıcı anlam taşır.
- R7: Projesiz kurumsal görev yalnız oluşturana, atanan kişilere, atanan takımın
  yetkili üyelerine, ilgili takım liderine ve gözlemci ADMIN'e görünür. İlgili
  lider açık lider–takım ilişkisiyle belirlenir; unvanı lider olan herkese açılmaz.
  Görev/proje/takım/atanan/dosya ilişkilerinde workspace tutarlılığı doğrulanır.
- R8: Ayrılan kişinin kurumsal erişimi ve mevcut oturumları sonlanır; görev,
  yorum ve iş geçmişi korunur. Son atananın kaldırılması görevi silmez; açık işler
  yeniden atanabilir. Erişim kapatma, üyelik kaldırma, anonimleştirme ve kalıcı
  silme ayrı işlemlerdir.

## Mimari ve kalite

- R9: Controller HTTP, service iş kuralı/yetki/transaction, repository veri erişimi
  taşır. `/api/v1`, `ApiErrorResponse`, `PageResponse`, session ve CSRF korunur.
  İç işlemler userId kullanır. İstek ID'lerine güvenilmez; tüm okuma/yazma yüzeyleri
  aynı yetki politikasıyla denetlenir.
- R10: Sürüm kontrolü ve UI çakışma deneyimi, veri korunması ve ayrı audit temeli
  kurulur. Soft delete seçilirse sorgu/benzersizlik/geri yükleme birlikte ele alınır.
  Atanansız görevler JOIN yüzünden kaybolmamalıdır. N+1 iddiaları ölçülür.
  V1–V8 korunur; migration backfill, doğrulama ve geri dönüş planıyla gelir.
- R11: Ortak HTTP istemcisi korunur. Sunucu ve geçici UI durumu ayrılır; TanStack
  Query ekran bazlı ve workspace anahtarlı geçiştir. Çıkış/hesap/alan değişiminde
  eski veri temizlenir. Dashboard auth'tan ayrılır; proje detay sorumluluklara bölünür.
  Token/CSS Modules, ortak form/dialog/drawer/tablo/durum bileşenleri kademeli kurulur.
  Türkçe varsayılan, İngilizce ikinci dil; board klavye ve hata geri alma destekler.
- R12: PostgreSQL entegrasyon, yetkisiz erişim, hesap ayrımı, ADMIN yazma yasağı,
  projesiz görünürlük ve veri korunması regresyonları gerekir. Kritik E2E kademelidir.
  CI entegrasyonu sessizce atlayamaz. Güvenlik/yedekleme/gözlemlenebilirlik erkenden
  ele alınır. Sırlar log/çıktı/commit'e taşınmaz. Yeni bağımlılık resmi kaynakla
  gerekçelendirilir. Gereksinim, backlog, OpenAPI, yetki ve ADR güncel tutulur.

## 2026-11-17 — Proje kişileri ve görev görünümleri

Yeni proje formunda ilk çalışanlar seçilir; proje ve üyelikler atomik kaydedilir.
Takımlar proje içinde bir kez oluşturulur ve birden fazla göreve tekrar atanır.
V13 kalıcı görev-takım ilişkisini ekler; eski kişi atamalarından takım tahmini
yapılmaz. Bitirilmiş/tamamlanmış DONE görevleri ile iptal edilenler ayrı
görünümdedir; varsayılan görev listesi TODO ve IN_PROGRESS içerir. Kişisel
görünümün erişim kapsamı mevcut kişi atamalarıyla sınırlı kalır. Öncelik rozetleri
iki temaya uyarlanır; koyu modun dar menüsünde S amblemi beyaz gösterilir.

Kararlar: `decisions/0006-project-people-reusable-teams-and-task-groups.md`.
Kullanıcının çalıştıracağı kontroller: `validation/project-task-workflow.md`.

## Başlangıç bulguları (kaynak kodu incelemesi)

Bu bölüm ilk incelemenin tarihsel kaydıdır. B01'de görev silme ve oturum
kontrolü için yapılan güncel değişiklikler `validation/work-preservation.md`
ve ADR 0002'de tutulur; aşağıdaki eski bulgular güncel özellik iddiası değildir.

Doğrulanan sürümler: Java 21; Spring Boot 4.1.1 (`backend/pom.xml`);
PostgreSQL 18 (`compose.yaml`); React ^19.3.0, TypeScript ^7.0.2,
Vite ^8.3.4, Vitest ^5.1.0 (`frontend/package.json`); Node 24.15.0 (`.nvmrc`),
npm 11 ana sürümü (`engines`). Bunlar çalışma zamanında doğrulanmış sonuç değildir.

| Bulgu | Kaynak / etkisi |
| --- | --- |
| ADMIN iş içeriğini yazabiliyor | ProjectService, TaskService; R4 henüz uygulanmadı |
| Görev oluşturma/düzenleme/silme ADMIN veya proje sahibine açık | TaskService; eski yetki belgesi sıradan üyeye de izin verdiğini söylüyordu, düzeltildi |
| Proje oluşturma unvana bağlı | JobTitle.canCreateProjects; TEAM_LEADER, PROJECT_MANAGER, PROJECT_OWNER. README'deki MANAGER/EMPLOYEE unvanları eskimişti |
| Kullanıcı silme oluşturduğu ve atanansız kalan görevleri siliyor | AdminUserService.deleteUser; R8 ile çelişiyor |
| Üye kaldırma global atanansız görev temizliği çağırıyor | ProjectService.removeMember, TaskRepository.deleteTasksWithoutAssignees; sonraki işin önceliği |
| Proje zorunlu; servis boş atamayı reddediyor | Task.project, TaskService.resolveAssignees; R6 bekliyor |
| Takım seçimi kişisel atamalara açılıyor | TaskService ve ProjectTeamService; kalıcı takım hedefi yok |
| E-posta principal ve sorgularda kullanılıyor | DatabaseUserDetailsService, ProjectService, TaskRepository; R9 bekliyor |
| Liste sorgusu atananlara INNER JOIN yapıyor | TaskRepository.findProjectTasks; atanansız desteğiyle birlikte değişmeli |
| Workspace/ayrı hesap modeli yok | Mevcut entity/migration incelemesi; controller adında workspace bulunması model değildir |
| Docker backend bağlamında frontend ve npm yoktu | Bu birimde kök build bağlamı ve Node aşaması hazırlandı |

Test başlangıcı: 17 backend `*Test.java`, 6 frontend test dosyası bulundu.
İki PostgreSQL Testcontainers sınıfı vardı; web yetki entegrasyonu mock repository
kullanır ve gerçek DB doğrulaması sayılmaz. Kullanıcı talimatıyla hiçbir test
çalıştırılmadı. Önceki `target` çıktıları güncel başlangıç kanıtı sayılmadı.
Çalıştırma ve sonuç kaydı: `validation/first-unit.md`.
