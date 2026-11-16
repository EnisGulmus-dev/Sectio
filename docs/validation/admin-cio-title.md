# ADMIN varsayılan şirket unvanı: CIO

Kullanıcı talebiyle ilk ADMIN hesabının şirket içi unvanı CIO olarak tanımlandı.
UI etiketi “Bilgi teknolojileri yöneticisi (CIO)” olur. Güvenlik rolü ADMIN kalır;
CIO unvanı kendi başına proje oluşturma veya ADMIN yetkisi vermez. Kullanıcı
oluşturma formunda açıkça seçilen unvan korunur. Bootstrap, modelin ADMIN
varsayılanını kullanır; mevcut adminin özel unvanını yeniden başlatmada değiştirmez.
Yeni kullanıcı formunda Yönetici rolü seçilince CIO varsayılan olarak seçilir;
kullanıcı başka unvan seçebilir. Düzenleme ekranında mevcut unvan korunur.

## Veri geçişi

Mevcut son migration V8 olduğundan V9 eklendi; V1–V8 değiştirilmedi. Yeni
constraint CIO kabul eder. Yalnız ADMIN + SOFTWARE_DEVELOPER kayıtları CIO olur;
diğer unvanlar ve çalışanlar korunur. Eski SOFTWARE_DEVELOPER değerinin
varsayılan mı elle seçilmiş mi olduğu şemada ayırt edilemez; bu koşula uyan tüm
adminler geçiş kapsamındadır. Parola, rol, aktiflik ve zorunlu parola değişimi
durumu değişmez. Workspace migration'ı eklenmedi.

Önce izole sectio-first-unit veritabanında doğrula. Başka verili ortamda önce
yedek/restore provası gerekir. CIO verisi eski enum ile okunamaz: geri dönüşte
eski imajı doğrudan açma; doğrulanmış yedekten restore veya eski sürümle uyumlu
unvanlara kontrollü veri geçişi planla. Uygulanmış V9 dosyasını değiştirme.

## Sıralı doğrulama — ajan çalıştırmadı

1. Backend içinde `.\mvnw.cmd -B -ntp clean verify` çalıştır. Bootstrap,
   unvan/yetki ve frontend testleriyle yeni CioJobTitlePostgreSqlTest çalışır.
   Yeni PG testi V8 → V9, tekrar migrate ve kullanıcı alanlarının korunmasını sınar.
2. Başarıdan sonra repo kökünde Docker imajını yeniden derle:
   `docker build -f backend/Dockerfile -t sectio-first-unit-backend .`
3. Sonra aynı izole proje/volume ile backend'i yeniden oluştur:
   `docker compose -p sectio-first-unit up -d --force-recreate backend --wait --wait-timeout 240`
   Önceki POSTGRES_PORT=55432 ve DB parolası korunmalı. Volume silinmez;
   admin parolası tekrar ayarlanmaz.
4. Kullanıcılar tablosunda varsayılan unvanlı adminin CIO olduğunu, parolanın
   çalıştığını ve çalışan unvanlarının değişmediğini kontrol et.

Önceki test/build başarısı bu değişiklikten öncedir. Yeni test/build ve çalışma
zamanı sonuçları bekleniyor.

## İlk doğrulama sonucu

2026-11-16 15:32:49: kullanıcı BUILD FAILURE paylaştı; hata `test-frontend`
aşamasında exit code 1. Ayrıntılı Vitest hatası paylaşılmadı. Kaynak incelemesinde
yeni UsersPage testinin tam etiket aramasının açıklama metni içeren etikete
uymadığı görüldü; combobox rolü ve etiket başlangıcıyla seçim olarak düzeltildi.
Bu düzeltme çalıştırılmadı; tek hata nedeni olduğu henüz doğrulanmadı.

Önce backend dizininden yalnız frontend testlerini çalıştır:
`npm --prefix ../frontend run test -- --reporter=verbose`
Başarısızsa FAIL/AssertionError/TestingLibraryElementError bölümünü paylaş.
Geçerse birleşik Maven verify adımına geri dön; bu ara kontrol Docker veya
backend testlerinin doğrulaması yerine geçmez.
