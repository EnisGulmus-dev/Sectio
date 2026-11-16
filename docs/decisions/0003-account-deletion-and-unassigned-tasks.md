# ADR 0003 — Pasifleştirme ve silme ayrımı

Tarih: 2026-11-16. Durum: Uygulama hazır; kullanıcı testleri bekleniyor.

Kullanıcı, Pasifleştir ile Sil'in farklı işlemler olmasını ve atanansız görevleri
frontend'de bulabilmeyi istedi. ADR 0002'deki yalnız erişim kapatma davranışı
silme için yeterli bir ürün modeli değildi.

## Karar

- Pasifleştir active=false yapar ve oturumları geçersiz kılar; üyelik ve atamalar
  kalır. Geçici durumdur; Aktifleştir ile geri alınabilir, eski oturumlar dönmez.
- Sil deletedAt kaydeder, normal listeden çıkarır, üyelik ve atamaları kaldırır.
  Görev oluşturucu kimliği ve proje sahipliği saklanır. Silinen hesap için
  aktifleştirme, düzenleme ve parola sıfırlama reddedilir. Tekrar silme idempotenttir.
- ADMIN, silinen hesapları ayrı salt okunur görünümde görebilir. Bu sürümde geri
  yükleme sunulmaz; kalıcı veri temizliği ve anonimleştirme ayrı iş olarak kalır.
  E-posta benzersizliği silinen kimlikler dahil korunur; aynı adresle hesap
  oluşturma eski kimliği canlandırmaz veya geçmiş işleri yeni hesaba bağlamaz.
- Projeler → ilgili proje → Görevler içinde Atanmamış görevler kısayolu ve
  atanan filtresi yer alır. Backend `unassigned=true` ile filtreler; sayfa toplamı
  aynı kuralı kullanır. Proje yetkisi önce denetlenir. Görevlerim yalnız kişiye
  atanmış işleri içerir; atanansız işler bu görünümde kurum geneline açılmaz.

## Veri geçişi

V1–V10 korunur; V11 nullable deleted_at alanı ve silinen hesap active olamaz
kısıtı ekler. Mevcut pasif hesapların hangi nedenle kapatıldığı bilinmediğinden
otomatik silinmiş kabul edilmezler. Önceki Erişimi kapat işlemiyle pasif kalan
hesaplar normal listede kalır; yeni Sil işlemiyle ayrıca kaldırılabilirler.

Normal kullanıcı listesi ve dashboard kullanıcı sayıları silinmiş kayıtları
hariç tutar. Login/aday listeleme active ve deleted durumuyla tutarlıdır. Bootstrap
silinmiş ADMIN'i mevcut yönetici kabul etmez. İş geçmişi FK'leri korunur.

Hesap yönetimi yazıları kullanıcı satırını kilitleyerek silme/aktifleştirme
yarışını sıralar. User güncellemeleri yalnız değişen kolonları yazar; devam eden
parola değişimi silme durumunu eski bir entity snapshot'ıyla geri alamaz.
Tam iş içeriği optimistic locking/sürüm deneyimi ayrı çalışma birimidir.

Geri dönüşte eski uygulama deleted_at alanını göz ardı edip silinen hesabı
aktifleştirebilir; DB kısıtı bu yazmayı reddeder. Eski sürüme körlemesine dönme:
yedek/restore provası veya silme durumunu bilen uyumlu imaj gerekir. Uygulanmış
migration'lar düzenlenmez. Test sırası `validation/account-deletion-and-unassigned.md`.
