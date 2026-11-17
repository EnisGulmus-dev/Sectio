# Organizasyon ve profil düzenleme planı

Durum: İlk uygulama hazır; kullanıcı doğrulaması bekleniyor.

## Kullanıcı kararları

- Şirket takımları proje takımlarından ayrıdır; çalışan birden fazla şirket
  takımında yer alabilir. Proje takımları proje içinde yönetilmeye devam eder.
- Kullanıcı oluşturulurken şirket takımları ve doğrudan yönetici seçimi isteğe
  bağlıdır. Sistem rolü ve iş unvanı mevcut ayrı alanlardır.
- Organizasyon ilişkilerini ADMIN veya ilgili doğrudan yönetici yönetebilir.
  Yönetici yalnız kendisine doğrudan bağlı kişileri yönetir ve yönetici alanında
  yalnız kendisini seçebilir; ADMIN başka uygun yöneticiyi seçebilir.
- Bağlı çalışanı olan yönetici pasifleştirilmeden veya silinmeden önce çalışanlar
  başka yöneticiye devredilir. Şirket takımı silinince yalnız o takım üyelikleri
  kaldırılır; çalışanlar ve proje görevleri korunur.
- Çalışan görünen ad, kısa açıklama, saat dilimi ve baş harf/fotoğraf/hazır avatar
  seçimini düzenler. Rol, unvan, şirket takımları ve yönetici profilinde salt
  okunur gösterilir.
- Profilde tek Güncelle düğmesi en altta bulunur. Bir seçim kaydedilmeden sol
  çubuk değişmez; başarılı kayıttan sonra profil görünümü ve sol çubuk birlikte
  güncellenir. Çıkış ve tema değiştirme sol çubuğun altında erişilebilir isimli,
  odak göstergeli iki simge düğmesidir.

## Önerilen uygulama sırası

1. Yeni migration ile şirket takımları, çoklu üyelik ve kullanıcıya bağlı
   doğrudan yönetici ilişkisi ekle. Mevcut kullanıcılar için ilişki boş başlar;
   mevcut proje takımlarından şirket takımı veya yönetici tahmin edilmez.
2. ADMIN için şirket takımı oluşturma, düzenleme ve üye ilişkilerini yönetme;
   çalışan oluşturma/düzenleme ekranında aktif yöneticiler ve şirket takımları
   seçimi. Hesap açılırken profil kaydı ad/unvan gibi mevcut hesap bilgilerine
   bağlanır, kişisel alanlar ve görsel başlangıçta boş/baş harfidir. Bir kişi
   kendisinin yöneticisi olamaz; döngü engellenir. İlişkiler
   kullanıcı kimliğiyle saklanır. Yetki her yazmada sunucuda doğrulanır.
3. Doğrudan yöneticinin yalnız kendi çalışanlarının takım ve yönetici alanına
   erişebilmesini sağlayan ayrı yetkili uçlar ekle. Bu yetki kullanıcı silme,
   parola sıfırlama, sistem rolü değiştirme veya tüm özel kanalları okuma yetkisi
   vermez. Pasif/silinmiş kişi yeni yönetici olarak seçilemez.
4. Profil güncellemesini tek kullanıcı işlemi olarak tasarla: metin ve seçilen
   avatar/fotoğraf değişiklikleri tek onayla uygulanır, hata halinde ekranda
   görünür ve mevcut profil korunur. Dosya geçici alanı/DB commit/temizlik
   işlemleri birlikte tasarlanır; dosya depolama hatasında kısmi değişiklik
   gösterilmez. Mevcut profil API istemcileri için uyumluluk korunur.
5. Profil verisini sol menü ile paylaşan ortak durum ekle. Başarılı kayıttan
   sonra seçilen görsel ve görünen ad hem profilde hem menüde yenilenir; eksik
   fotoğrafta baş harfe dönülür. Oturum değişiminde eski kullanıcının verisi
   temizlenir. Fotoğraf ve avatar önizlemesi açık/koyu temada yuvarlak sunulur.
6. Menü altındaki çıkış ve tema eylemlerini yalnız simgeyle göster; her iki
   düğmeye açık erişilebilir ad ve tooltip, klavye odağı ve dar menü desteği ekle.

## Doğrulama

PostgreSQL testlerinde çoklu takım, isteğe bağlı ilişki, kullanıcı oluşturmanın
atomikliği, yetkisiz/başka yönetici, pasif kullanıcı ve yönetici döngüleri
doğrulanır. UI testlerinde tek kaydetme, iptal/başarısız işlem, görsel seçimi,
dosya hatası, oturum değişimi ve sol menü senkronizasyonu hazırlanır. Kullanıcı
talebi gereği testler ajan tarafından çalıştırılmaz; sırayla çalıştırılacak
komutlar ve beklenen sonuç kullanıcıya verilir.
