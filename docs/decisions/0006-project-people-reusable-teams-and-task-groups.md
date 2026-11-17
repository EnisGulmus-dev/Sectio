# 0006 — Proje kişileri, kalıcı takım hedefleri ve görev görünümleri

Tarih: 2026-11-17. Durum: uygulandı; test çalıştırma kullanıcıya ait.

Yeni proje formu aktif kişileri seçtirir. Proje, otomatik sahip üyeliği ve seçilen
üyelikler tek transaction içinde oluşturulur. Yinelenen ID ve sahip ID'si ikinci
üyelik oluşturmaz. Geçersiz/pasif/silinmiş kişiyle proje kaydedilmez. Aday okuma
yetkisi proje oluşturma yetkisiyle aynıdır. Sonradan çalışan ve takım yönetimi
projenin Takımlar ve üyeler bölümünde devam eder.

V13, görev ile kayıtlı takım arasındaki çoktan çoğa ilişkiyi `task_teams` içinde
saklar. Takım görev oluşturulunca tüketilmez; aynı takım birçok göreve atanabilir.
Yanıtta `teams: [{id, name}]` yer alır. Kişi atamaları o işlemdeki aktif proje
üyelerinin snapshot'ıdır. Sonradan takım üyelerinin değişmesi eski görevleri
kendiliğinden yeniden atamaz; görev düzenlemesi seçilen takımın güncel üyelerini
çözer. Aynı kişileri tek tek seçmek takım seçimi sayılmaz. Önceki görevlerin hangi
takımla oluşturulduğu bilinmediğinden migration bu ilişkiyi tahmin etmez.

Takım silme, yalnız takım ve görev-takım bağlantılarını kaldırır; görevleri ve
kişi atamalarını korur. Görev silme takımı silmez. Kişilerin hepsi projeden
çıkarıldığında kayıtlı takım bağlantısı varsa görev takım hedefli kalır. Atanmamış
filtresi hem kişi hem takım hedefi olmayan görevleri kapsar. Kişisel liste mevcut
kişi atamalarını izler; erişim kapsamı genişletilmez.

Proje ve kişisel görev listeleri `bucket` parametresiyle ACTIVE (TODO,
IN_PROGRESS), COMPLETED (DONE), CANCELLED ve ALL gruplarını sunar. Bitirilmiş ve
tamamlanmış aynı DONE durumudur; ayrı bir durum eklenmez. REST varsayılanı ACTIVE;
eski istemcinin yalnız `status` göndermesi mevcut durum filtresini korur. İkisi
birlikte gönderilirse kesişimleri alınır. Filtreler sayfalama ve count sorgusunda
aynıdır. Arayüz seçimi URL'de korur; durum geçişinden sonra listeyi yeniden okur.

Öncelik rozetleri ortak bileşenden, cloud/midnight için ayrı renklerle çizilir.
Renge ek olarak metin bulunur. Koyu temada dar menüdeki saydam S amblemi CSS
filtresiyle beyaz gösterilir; açık temada mevcut renkli amblem kullanılır.
