# Görev ilerleme kaydı ve sonraki aşamaya devir — uygulama planı

Durum: İlk uygulama hazır; testler kullanıcı tarafından çalıştırılacak.

Görev detayında mevcut bilgiler ve yeni durum değişimlerinin zaman çizelgesi
gösterilir. Her durum geçişinde eski/yeni durum, işlemi yapan kimlik, zaman ve
isteğe bağlı açıklama tek işlemde kaydedilir. Tarihsel değişiklikler bilinmediği
için uydurulmaz. Değişiklik bir pencereden yapılır; mevcut proje ve kişisel görev
ekranları aynı bileşeni kullanır.

Tamamlanmış işin sonraki aşaması aynı proje içinde yeni, ilişkili bir görev olarak
oluşturulur. Önceki görev tamamlanmış kalır; yeni iş seçilen aktif proje üyelerine
veya kayıtlı takımlara atanır. Kaynak ve hedef, her iki görevde de gösterilir.
Görevi görme yetkisi proje erişimiyle, devir oluşturma yetkisi mevcut proje
yönetimi yetkisiyle sınırlandırılır; iş unvanı tek başına geniş erişim vermez.

Geçiş ve devir işlemlerinde eşzamanlı güncellemeler kontrol edilir; işlem, hedef
atamalar ve geçmiş kaydı birlikte başarılı olur veya tamamı geri alınır. Kişi
silindiğinde veya anonimleştirildiğinde görev ve geçmiş korunur. Kaynak görev
silindiğinde hedefin ilişki açıklaması korunacak şekilde tasarım doğrulanır.

V16 ile yeni devirlerde kaynak/hedef adları, atanan kişiler ve seçili takım
adları, işlemi yapan kullanıcı ve zaman saklanır. Görevlerden biri silinince
ilgili bağlantı boşalır, diğer görev üzerinden devir kaydı görüntülenebilir.
Projenin tamamı silinirse proje içindeki devir kayıtları da silinir. V16 öncesi
devirlerin o andaki ad ve alıcı bilgileri güvenilir biçimde çıkarılamadığından
geriye dönük kayıt uydurulmaz. Bu kayıtlardaki kişi adları devir zamanı anlık
görüntüsüdür; hesap anonimleştirmesi bu tarihsel metinleri değiştirmez.

Oturumlar değişmez kullanıcı kimliğini kontrol eder; servislerin tümünde e-posta
yerine ID ile aktör çözümlemesi bu değişiklikte tamamlanmadı. PostgreSQL ve
arayüz testleri hazırlandı; kullanıcı testleri kendisi çalıştırır.
