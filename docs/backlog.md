# Bağımlılık sıralı backlog

Her satır ayrı incelenebilir bir çalışma birimidir. Amaç/kapsam ve kabul ölçütleri
hedef davranıştır; tamamlandı anlamına gelmez. R numaraları requirements.md'ye aittir.
Testler kullanıcı tarafından çalıştırılır. V1–V8 hiçbir işte değiştirilmez.

| İş / bağımlılık | Amaç ve kapsam | Kabul ölçütü | Risk | Doğrulama |
| --- | --- | --- | --- | --- |
| B00 Başlangıç / yok | Gereksinim, ADR, matris, tek Tomcat Docker, CI | Temiz kurulum health UP; frontend/backend/PG testleri geçer, atlama yok | Docker/CI henüz çalıştırılmadı; eski volume yerleşimi | validation/first-unit.md; durum: uygulama hazır, doğrulama bekliyor |
| B01 Görev kaybını durdur / B00 | Kullanıcı erişimi kapatma ve üye çıkarma akışlarından görev silmeyi kaldır; açık görevlerin yeniden atanmasını koru; UI etki/onay metinlerini düzelt | Oluşturan ayrılırsa geçmiş durur; son atanan çıkınca görev durur; proje listesi/sayaç atanansız görevi kaybetmez; mevcut oturum erişemez | Global deleteTasksWithoutAssignees, FK cascade, INNER JOIN; silmeyi kapatmak tek başına yetmez | Gerçek PG kullanıcı/üye/son atanan/çoklu atanan/oluşturan senaryoları; oturum iptali ve UI regresyonu |
| B02 Kimlik / B01 | Session principal ve iç sorgularda değişmez userId; API uyumluluğunu koru | E-posta değişse de doğru aktör; kapalı hesap istekleri reddedilir | Eski oturum ve e-posta bazlı sorgu artıkları | Auth/service/web testleri; eski oturum politikası |
| B03 Sürüm ve audit / B02 | Optimistic locking, API conflict ve UI yenileme; audit/aktivite ayrımı | Eşzamanlı yazmada sessiz kayıp yok; audit hassas değer içermez | Bulk update sürüm atlayabilir; log hacmi | PG eşzamanlı transaction ve UI conflict testi |
| B04 Yedek ve işletim / B01 | Restore provası, health, yapılandırılmış hata/istek gözlemi | İzole DB'ye restore sayım ve ilişki doğrulaması; sırlar redakte | Yedeğin çalıştığı varsayımı | Restore tatbikatı, log incelemesi; migration ön koşulu |
| B05 Hesap şeması / B02,B04 | Ayrı bireysel/kurumsal kimlik; eski kullanıcıları kurumsal backfill | Aynı e-posta iki kimliği bağlamaz; eski satırlar korunur | Benzersizlik ve login belirsizliği | PG migration/backfill/restore; hesap ayrımı negatif test |
| B06 Workspace şeması / B05 | Workspace türleri, üyelikler, görev workspace backfill; oda kavramını ayır | Her mevcut görev doğru kuruma bağlı; ilişkiler tutarlı | Yanlış kurum atama/veri sızıntısı | PG ilişki/satır sayımı, kısıt testleri; numara o an seçilir |
| B07 Merkezi yetki / B06 | Kararları matristen uygula; ADMIN yazma yasağı ve lider–takım bağı | Liste/detay/sayaç aynı sınırı uygular; başka workspace ID reddedilir | Proje politikası ve rol birleşimi kararı gerekli | Rol/kaynak matrisi parametrik negatif testler; OpenAPI güncelle |
| B08 UI temeli / B07 | Ortak HTTP/UI, auth'tan dashboard ayırma, i18n/token/CSS Modules; workspace gezinme | TR varsayılan/EN; çıkış/alan değişiminde eski veri görünmez | Önbellek ve büyük ekran geçişi | RTL/MSW, klavye ve hesap geçiş E2E; Query ekran bazlı |
| B09 Görev ilişkileri / B07 | Opsiyonel proje/atama; kalıcı takım hedefi; projesiz görünürlük | Oluşturan/atanan/yetkili takım/ilgili lider/ADMIN dışında kapalı; alanlar arası ilişki reddi | Doğrudan atama lider kapsamı kararı gerekli | PG görünürlük ve boş atama JOIN/sayaç testleri |
| B10 Görev etkileşimi / B08,B09 | Özel durum, görev anahtarı, liste/board/sıralama, URL detay paneli | Yenileme/deep link çalışır; board hatası geri alınır; klavye destekli | Sıralama yarışları | PG benzersizlik/sıralama, UI/E2E |
| B11 Kişisel akış / B10 | Gelen kutusu ve Bugün; bireysel kayıt | Projesiz/atanansız kişisel görev; kurumsal içerik karışmaz | Saat dilimi/gün sınırı | Hesap ayrımı, gün sınırı ve kritik E2E |
| B12 Topluluk / B08,B09 | Topluluk, üyelik ve paylaşımlı projeler | Kişisel içerik açılmaz; üyelikten ayrılma erişimi kaldırır | Topluluk rolleri kararı | Üyelik/davet/erişim negatif testleri |
| B13 Görev ayrıntıları / B10 | Yorum, etiket, alt görev, durum geçmişi ve aktiviteyi küçük alt değişikliklere böl | İlişkiler aynı workspace; geçmiş kaybolmaz | Döngüsel alt görev ve audit karışması | PG ilişki, API ve UI testleri |
| B14 Dosya altyapısı / B07,B04 | Depolama, boyut/tür, yetkili indirme ve yaşam döngüsü | Başka workspace dosyası okunamaz; yedek geri yükler | Yetkisiz URL ve yetim dosya | Depolama entegrasyonu; erişim negatif testleri |
| B15 Dosya eki / B13,B14 | Görev/yorum dosya ilişkilendirme | Dosya ve kaynak workspace tutarlı | Silme/geri yükleme ilişkileri | PG ilişki ve indirme E2E |
| B16 Olay ve işler / B03,B07 | Kalıcı outbox, idempotent worker ve zamanlayıcı | Tekrarlanan iş çift etki yaratmaz; çöküşten devam | Yeniden deneme ve yarış | PG outbox ve worker hata/tekrar testleri |
| B17 E-posta / B16 | Gönderim altyapısı, sonra e-posta daveti | Süreli/tek kullanımlık davet; doğru hesap ve workspace | E-posta adresi üzerinden hesap bağlama | Sahte sağlayıcı; token/yeniden kullanım testleri |
| B18 Bildirim ve realtime / B16 | Bildirim, yetkili abonelik; sonra tekrar/hatırlatma | Oturum kapanınca abonelik kesilir; saat dilimi doğru | Yayınla veri sızıntısı | Yetkisiz abonelik/tekrar/bağlantı E2E |
| B19 Sohbet / B12,B18 | Oda/DM/thread/okunmamış; ayrı alt birimler | Yalnız katılımcı erişir; ADMIN gözlemi özel sohbet açmaz | Üyelik değişiminde erişim | API/realtime ve okunmamış yarış testleri |
| B20 Talep ve ofis / B16,B18 | Ortak talep/onay motoru, sonra modüller | Onay geçişi yetkili ve auditli | Modüller arası politika farkı | Durum makinesi/negatif yetki testleri |
| B21 Arama / B07,B13 | Workspace filtreli arama ve dışa aktarma | Listeyle aynı görünürlük, sayaç sızdırmaz | İndeks gecikmesi | Negatif arama/dışa aktarma ve ölçüm |
| B22 Rapor/entegrasyon / B13,B16,B21 | Durum geçmişiyle cycle time; dar kapsamlı entegrasyonlar | Ölçüm tanımı açık; entegrasyon yetkili ve idempotent | Eksik geçmiş/harici sırlar | Tarihsel örnek, sözleşme ve sorgu sayımı |
| B23 Yayın doğrulaması / ilgili özellikler | Kapsamlı yük, güvenlik, erişilebilirlik, yayın/geri dönüş | Kritik E2E, restore, performans bütçeleri geçer | Test/üretim farkı | Yük ölçümü, erişilebilirlik ve yayın provası |

B01 başlangıçtan sonraki çalışma birimidir. Genel görev modeli/workspace dönüşümü B01'e
taşınmaz; ancak veri korumak için gereken atanansız listeleme düzeltmesi ve
oturum kapatma aynı birimde ele alınır. Güvenlik ve işletim işleri B23'e ertelenmez.

## B01 uygulama durumu — 2026-11-16

Hesap kapatma/üyelik kaldırmada görev silen işlemler kaldırıldı. Uyumluluk DELETE
hesabı ve geçmişi korur; proje listesi/count atanansız görevleri içerir. Kalıcı
sessionVersion + userId snapshot ile pasifleştirilen hesabın eski oturumları
yeniden aktifleştirmeyle canlanmaz. Bu dar userId kullanımı oturum güvenliği
içindir; servis/sorguların bütünündeki kimlik geçişi B02'de kalır. V10 eklendi.
PostgreSQL, service/filter ve UI testleri hazırlandı; çalıştırılmadı.
Durum: kullanıcı doğrulaması bekleniyor (`validation/work-preservation.md`).

Kullanıcının son düzeltmesi: Pasifleştir ve Sil farklı durumlar olarak ayrıldı.
V11 silme durumunu ekler; normal liste/adaylar/oturum/benzersizlik ve aktifleştirme
kuralları birlikte ele alındı. Silinen kayıtlar salt okunur görünür, geri yükleme
sunulmaz. Projede Atanmamış görevler kısayolu ve sunucu filtresi eklendi.
Bu yeni sürümün doğrulaması: `validation/account-deletion-and-unassigned.md`.

Kullanıcı ayrılan kişinin geçmiş atamalarını gözlemlemek istediğinden V12 ile
sonlandırılan atama snapshot'ları ve ADMIN İş geçmişi eklendi. Bu dar iş geçmişi
tam audit altyapısının yerine geçmez. Test/çalıştırma sırası:
`validation/employee-offboarding-history.md`; yeni test sonucu bekleniyor.

Son kullanıcı düzeltmesiyle gözlem silmeden bağımsız hale getirildi: CURRENT
görevler (tamamlananlar dahil) ve ENDED snapshot'ları work-tracking okuma
modelinde birleşir. Yeni migration/bağımlılık yoktur. CSS Modules ile kompakt
liste seçimi ve iş takibi kartları hazır; test/görsel kullanıcı doğrulaması:
`validation/employee-work-tracking.md`.
