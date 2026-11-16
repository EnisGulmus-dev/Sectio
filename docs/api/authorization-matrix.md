# Yetki matrisi

Güncel İş takibi UI'sı ADMIN-only work-tracking endpoint'inden aktif/pasif
çalışanın canlı atamalarını (DONE dahil) ve silinmiş/üyelikten çıkarılmış
çalışanın snapshot'larını birlikte okur. Silme ön koşulu yoktur. Durum, kapsam,
arama ve sayfa filtreleri erişim yetkisini genişletmez. Eski work-history
sözleşmesi yalnız snapshot okuması olarak korunur.

Session ve CSRF korunur. Login/CSRF dışındaki API erişimi oturum gerektirir;
zorunlu parola değişimi tamamlanmadan iş API'lerine geçilemez.

## Mevcut uygulama (2026-11-16 kaynak incelemesi)

Bu tablo hedef politika değildir. ProjectService, TaskService,
ProjectTeamService ve SecurityConfiguration esas alınmıştır.

| İşlem | ADMIN | Proje sahibi | Sıradan üye | Üye olmayan |
| --- | --- | --- | --- | --- |
| Proje/görev/üye/takım okuma | Tümü | Kendi projesi | Üyesi olduğu | Hayır |
| Proje oluşturma | Evet | Yetkili unvansa | Yetkili unvansa | Yetkili unvansa |
| Proje düzenle/arşivle/geri aç/sil | Evet | Evet | Hayır | Hayır |
| Üye/takım yönetimi | Evet | Evet | Hayır | Hayır |
| Görev oluştur/düzenle/sil/ata | Evet | Evet | Hayır | Hayır |
| Görev durumu değiştir | Evet | Evet | Kendisine atanmışsa | Hayır |
| Kullanıcı/rol/geçici parola yönetimi | Evet | ADMIN ise | ADMIN ise | ADMIN ise |

Yetkili unvan: TEAM_LEADER, PROJECT_MANAGER veya PROJECT_OWNER.
CIO şirket içi unvandır; kendi başına proje oluşturma yetkisi vermez. İlk ADMIN
hesabının varsayılan unvanıdır; ADMIN'in mevcut yetkileri ve hedef yazma yasağı
ayrıca rol politikasıyla ele alınır.
Atanan kişiler aktif proje üyeleri olmalıdır. Proje sahibi üyelikten çıkarılamaz;
atanmış üye çıkarılırken görevler silinmez, yalnız o projedeki atamaları kaldırılır.
Arşiv okuma sürer; normal değişiklikler
reddedilir; geri açma ve kalıcı silme ayrıca mümkündür. ADMIN dashboard genel,
çalışan dashboard kendisine atanmış görev sayaçlarını kullanır.
DELETE kullanıcı endpoint'i silme durumunu kaydeder; normal listeden çıkarır,
üyelik/atamaları kaldırır. Oluşturduğu işler ve proje sahipliği korunur.
Silinen hesap yalnız ADMIN'in salt okunur geçmiş listesinde bulunur; düzenleme,
parola sıfırlama ve aktifleştirme reddedilir. E-posta rezerve kalır; geri yükleme
ve fiziksel veri temizliği sunulmaz. Kendi hesabı ve son aktif ADMIN korunur.
Pasifleştirme ise geçicidir; üyelik/atamaları ve normal listedeki kaydı korur.
Oturumlarda userId ve sessionVersion doğrulanır; yeniden aktifleştirme eski
oturumları geri getirmez. Atanmamış filtre yalnız zaten erişilebilen projeyi kapsar.
Bu B01 değişikliklerinin testleri hazırdır; kullanıcı doğrulaması bekleniyor.

ADMIN ayrıca kullanıcıların sonlandırılmış atamalarını work-history endpoint'i
ve İş geçmişi penceresinden salt okunur görebilir. EMPLOYEE erişemez. Bu yalnız
hesap/proje üyeliği kaldırmada kaydedilmiş görev atamalarıdır; özel sohbet veya
genel audit erişimi sağlamaz. Yeni V12 ve UI testleri kullanıcı doğrulaması bekler.

## Hedef politika — henüz uygulanmadı

| İşlem | Kurumsal ADMIN rolü | Çalışan | İlgili takım lideri | Bireysel hesap |
| --- | --- | --- | --- | --- |
| Kurumsal hesap aç/kapat, parola sıfırla | Evet; mevcut parola okunamaz | Hayır | Hayır | Hayır |
| Kurum takım/oda/üyelik yönetimi | Evet | Ek yetki tanımlıysa | Ek yetki tanımlıysa | Kuruma erişemez |
| Kurum proje/görev/ilerleme gözlemi | Kurum kapsamında | Erişim politikasıyla | Erişim politikasıyla | Kuruma erişemez |
| Proje/görev oluştur/düzenle/sil | ADMIN rolünden yasak | Görev oluşturabilir; diğerleri proje politikasıyla | Proje politikasıyla | Kendi kişisel alanında; toplulukta üyelik politikasıyla |
| Atama | ADMIN rolünden yasak | Ek atama yetkisi kararı bekliyor | Yetkili takım kapsamında | Kişisel alanında; topluluk politikasıyla |
| Durum değiştir | ADMIN rolünden yasak | Görev/proje politikasıyla | Görev/proje politikasıyla | Kendi alanı/politikasıyla |
| Özel sohbet okuma | Gözlem rolü sağlamaz | Katılımcıysa | Katılımcıysa | Katılımcıysa |
| Kurumsal oturumdan kişisel alana geçme | Hayır | Hayır | Hayır | Ayrı bireysel oturum gerekir |

### Projesiz kurumsal görev görünürlüğü

Önce aktif kurumsal kimlik ve aynı workspace şartı aranır. Ardından yalnız
oluşturan, doğrudan atanan, atanan takımın yetkili üyesi, ilgili lider veya
gözlemci ADMIN erişebilir. Diğer çalışanlara kapalıdır. İlgili lider ilişkisi
hedef modelde açık `leader → team → task team target` bağıyla kurulacaktır;
bir doğrudan atananın takım liderinin erişiminin nasıl belirleneceği ayrıca
karara bağlanmalıdır. Sırf TEAM_LEADER unvanı erişim sağlamaz.

### Uygulamadan önce netleştirilecek noktalar

- Çalışanın hangi proje görevlerini düzenleyebileceği/silebileceği; proje
  oluşturma/yönetim rolü ve proje içi görünürlük: kullanıcı kararında kesin değil.
- ADMIN ile iş rolünün birleşebilmesi: varsayılan yazma yetkisi verilmez;
  açık karar ve regresyon testinden sonra politika tanımlanır.
- Doğrudan kişi atamasında ilgili liderin kapsamı, çok takımlı üyelik ve takım
  değişiminin görünürlüğe etkisi açık ilişkilerle tanımlanmalıdır.
- Topluluk yönetici/moderatör rolleri ve davet yetkileri henüz tanımlanmadı.

Proje/atanan/takım/dosya ID'si workspace sınırını geçemez. Liste, detay, sayaç,
arama, dışa aktarma ve abonelik aynı politika kullanır. Hesap kapatma mevcut
oturumların erişimini de sonlandırır; geçmiş içerik korunur.
