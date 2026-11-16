# ADR 0004 — Ayrılan çalışanın sonlandırılmış atama geçmişi

Tarih: 2026-11-16. Durum: Uygulama hazır, kullanıcı testleri bekleniyor.

Güncelleme: genel çalışan gözlemi ve UI ADR 0005 ile genişletildi. Bu kayıttaki
sonlandırılmış atama saklama ve eski work-history API kararı korunur; yeni İş
takibi ekranı mevcut atamaları da kullanıcı silinmeden gösterir.

Görevin kendisini korumak, ayrılan kişinin o göreve atanmış olduğunu korumaya
yetmez. Önceki kaldırma sorguları task_assignees bağlantısını kaldırınca bu
ilişki kayboluyordu. Kullanıcı geçmiş katkıları gözlemleyebilmek istedi.

V1–V11 korunur. V12 task_assignment_history ekler. Hesap silme ve proje üyesi
kaldırmada yalnız gerçekten kaldırılan atamalar aynı SQL işlemi içinde arşive
aktarılır (DELETE RETURNING → INSERT). Kayıt yazılamazsa atama kaldırma ve üst
transaction içindeki kullanıcı silme de geri alınır. Aynı atamayı ikinci kez
kaldırmak fazladan tarihçe üretmez; yeniden atanıp kaldırılma ayrı kayıttır.
Bu yaklaşım [PostgreSQL veri değiştiren WITH sorgularına](https://www.postgresql.org/docs/18/queries-with.html#QUERIES-WITH-MODIFYING)
dayanır; çalışma zamanı doğrulaması PostgreSQL testlerine bırakılmıştır.

Her kayıtta userId, mevcutsa taskId/projectId; görev/proje adları, atama sonundaki
durum, tarih ve kaldırma nedeni tutulur. Başlangıç tarihi mevcut ilişkide yoktur;
uydurulmaz. Görev/proje ayrıca silinirse ID'ler NULL olur ama metin snapshot'ları
kalır. Kullanıcı kimliği iş geçmişi için saklanır. Bu kapsamlı audit veya bütün
atama değişikliklerinin tarihçesi değildir; yalnız işten ayrılma/üyelik kaldırma
akışındaki sonlandırmaları kapsar. Önceden kaybolmuş bağlantılar geri doldurulmaz.

ADMIN-only paginated GET /api/v1/admin/users/{id}/work-history mevcut /api/v1,
PageResponse ve session yetkisini kullanır. UI'da Kullanıcılar → Silinen
kullanıcıları göster → İş geçmişi, salt okunur gözlem sunar. Aktif/pasif
kullanıcıların önceki proje üyelik sonlandırmaları da aynı eylemden görülebilir.
Özel sohbet veya yeni yazma yetkisi verilmez. Workspace geçişinde aynı kurumsal
sınır bu endpoint'e de uygulanmalıdır.

Migration yeni, boş geçmiş tablosu oluşturur; mevcut işleri taşımaz veya silmez.
Verili ortamda önce yedek/restore planı gerekir. Geri dönüş için tabloyu düşürme:
eski imaj çalışsa da yeni geçmişi kaydetmez. Veri kaybetmeyen uyumlu ileri
düzeltme veya doğrulanmış yedekten dönüş planlanır.
