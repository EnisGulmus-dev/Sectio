# ADR 0001 — Hesap sınırları ve tek uygulama dağıtımı

Tarih: 2026-11-16. Durum: Kabul edildi; hesap/workspace modeli henüz uygulanmadı.

## Bağlam

Mevcut kaynaklar tek kullanıcı kimliği, global ADMIN ve proje üyelikleri kullanıyor.
`DashboardWorkspaceController` adı workspace veri modeli bulunduğu anlamına gelmez.
Kurumsal teknik yönetim ile iş içeriği yönetimi bugün ayrılmamış durumda.

## Karar

- Java 21 / Spring Boot modüler monolit korunur. React çıktısı aynı JAR ve
  embedded Tomcat üzerinden sunulur; Compose dağıtımında Nginx servisi kullanılmaz.
- Bireysel ve kurumsal hesaplar ayrı kimliklerdir. Aynı e-posta eşleştirme veya
  otomatik birleştirme gerekçesi değildir. İç kimlik userId olur.
- Kişisel, topluluk ve kurumsal workspace ayrı erişim sınırlarıdır. Oda workspace
  altında yer alır. Her görev workspace taşır; proje ve atama isteğe bağlıdır.
- Kurumsal ADMIN teknik yönetici/gözlemcidir; ADMIN rolü iş içeriğine yazma hakkı
  vermez. Unvan yetki kaynağı olmaktan çıkarılır.
- Yetkilendirme service katmanında merkezi politikayla uygulanır; liste, sayaç,
  dosya, arama ve abonelikler aynı sınırı kullanır. UI/cache erişim kontrolü değildir.
- Hesap kapatma ile veri silme ayrılır; iş geçmişi korunur. İlk öncelik görev
  kaybını durdurmaktır. Audit ve kullanıcı aktivitesi farklı amaçlarla tutulur.

## Sonuçlar ve geçiş

Bu çalışma birimi yalnızca belge ve dağıtım/test altyapısını değiştirir.
V1–V8 değişmez; yeni migration yoktur. Gelecek migration numarası o anki son
migration incelenerek seçilir. Mevcut kullanıcılar kurumsal olarak backfill edilir;
kişisel workspace otomatik yaratılarak eski veri paylaşılmaz. Her backfill için
önce yedek/geri yükleme provası, satır sayısı ve ilişki doğrulaması gerekir.
Yeni veri yazıldıktan sonra eski uygulamaya körlemesine dönülmez; doğrulanmış
geri yükleme veya ileri düzeltme planı hazırlanır.

Rol birleşimleri ve proje yönetim politikası uygulamadan önce ayrı karar ister;
ADMIN'e başka rol üzerinden dolaylı yazma hakkı tanınacağı varsayılmaz.

Dağıtımda mevcut PostgreSQL 18 imajı korunur. Volume hedefi resmi imajın
18+ düzenine göre `/var/lib/postgresql` olur. Eski volume içerikleri otomatik
taşınmaz: yedek ve restore doğrulanmadan mevcut kurulum bu değişiklikle açılmaz.
Kaynak: [PostgreSQL resmi Docker imajı](https://hub.docker.com/_/postgres).
