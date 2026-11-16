# ADR 0002 — Erişim kapatma ve iş geçmişini koruma

Tarih: 2026-11-16. Durum: Uygulama hazır; kullanıcı doğrulaması bekleniyor.

Güncelleme: hesap kapatma/silme ayrımı için bu kaydın yaşam döngüsü kararları
ADR 0003 ile değiştirildi. Görev koruması ve oturum sürümü kararı geçerlidir.

## Bağlam ve karar

Kullanıcı silme, oluşturduğu görevleri; proje üyesi çıkarma ise global atanansız
görevleri silebiliyordu. Bu davranış veri korunması kararına aykırıdır.

- Mevcut DELETE /api/v1/admin/users/{id} yolunu HTTP 204 sözleşmesiyle koru;
  işlemi erişim kapatma olarak uygula. User satırı, görev oluşturucu FK'si ve
  proje sahipliği tutulur. Hesap pasifleştirilir, üyelikler ve doğrudan atamalar
  kaldırılır. Kalıcı silme ve anonimleştirme ayrıca tasarlanacak işlemlerdir.
- Yalnız pasifleştirme üyelik/atamaları korur. Erişim kapatma bunları kaldırır;
  aktifleştirme geri getirmez. Kendi hesabını ve son aktif admini kapatma koruması
  iki akışta da geçerlidir. Proje sahibi olmak erişim kapatmayı engellemez.
- Proje üyesi kaldırma yalnız belirtilen projedeki üyelik, takım üyeliği ve
  atamayı kaldırır. Açık/tamamlanmış görevler ve başka projeler korunur. Sahip
  üyelikten çıkarılamaz; hesabının pasifleştirilmesi proje sahipliğini silmez.
- Atanansız kalmış görev proje listesinde LEFT JOIN ve uyumlu count ile yer alır;
  proje yöneticisi yeniden atayabilir. Projesiz/atanansız oluşturma çekirdeği
  bu değişikliğin kapsamı değildir. Kişisel atanmış görev listesine atanansız
  işler eklenmez; genel görünürlük genişletilmez.
- Session principal giriş anında userId ve sessionVersion taşır. Aktif hesap
  sürümü her authenticated API isteğinde, auth/me ve parola değişimi dahil,
  doğrulanır. Deactivate sürümü artırır. Böylece pasiflik sırasında istek yapmamış
  eski oturum da reaktivasyondan sonra geçersiz kalır. Session ve CSRF korunur.
  Rol değişimi ve admin parola sıfırlaması da sürümü artırır; eski oturumlar
  önceki ADMIN authority'sini veya eski parola erişimini sürdüremez.

## Geçiş ve sınırlamalar

V1–V9 korunur. V10 kullanıcıya BIGINT session_version ekleyip mevcut satırları 0
ile doldurur. Veri/ilişki silmez. Eski principal biçimine ait oturumlar yeni
uygulamada tekrar giriş gerektirir. Şema eklemesi eski sürüm tarafından göz ardı
edilebilir; ancak eski sürüme dönmek görev silme davranışını geri getirir.
İşlem öncesi yedek/restore provası gerekir; eski sürüme otomatik dönüş önerilmez.

Tam userId geçişi, optimistic locking ve audit altyapısı sonraki birimlerdir.
Bu işlem mevcut olmayan yorum/aktivite geçmişi oluşturmaz; var olan görev ve
kimlik kayıtlarını korur. Önceden silinmiş veriyi geri getirmez. Devam eden
istekleri geriye dönük iptal etmez; sonraki API erişimini engeller. Önceden
tarayıcıya indirilmiş içerik için gerçek zamanlı temizleme bu birimde yoktur.

Doğrulama planı: `../validation/work-preservation.md`. Testler çalıştırılmadı.
