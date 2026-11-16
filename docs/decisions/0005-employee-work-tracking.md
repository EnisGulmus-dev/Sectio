# ADR 0005 — Silme işleminden bağımsız çalışan iş takibi

Tarih: 2026-11-16. Durum: Uygulama hazır; kullanıcı testleri bekleniyor.

## Sorun

Önceki İş geçmişi yalnız kaldırılan atamaları okuyordu. Çalışanın tamamladığı
bir iş, çalışan veya üyelik silinmeden bu ekranda görünmüyordu. Kullanıcı iş
takibinin aktif, pasif ve silinmiş hesaplarda yapılmasını istedi. Büyük checkbox
ve standart tablo görünümü de ürünün tasarımıyla uyumsuz bulundu.

## Karar

- GET /api/v1/admin/users/{id}/work-tracking eklenir. Mevcut work-history
  endpoint'i ve tarihsel snapshot sözleşmesi korunur.
- CURRENT kayıtları task_assignees + tasks + projects üzerinden canlı okunur;
  TODO, IN_PROGRESS, DONE ve CANCELLED dahil edilir. Hesap silme koşulu yoktur.
- ENDED kayıtları V12'deki sonlandırılmış atama snapshot'larından gelir. Canlı
  durum ve tarihsel durum UI'da ayırt edilir. Eski snapshot'a tamamlanma tarihi
  uydurulmaz; güncel görevde varsa gerçek completedAt döner.
- UNION ALL okuma modeline scope/status/query filtreleri ve sayfalama uygulanır.
  Aynı filtre count sorgusunda da kullanılır. Aynı göreve tekrar atanma ayrı
  atama kayıtları üretebilir; toplamlar benzersiz görev/çalışma süresi değildir.
- ADMIN gözlem yetkisi korunur; EMPLOYEE API'yi okuyamaz. Silme, pasifleştirme,
  görev atama veya durum değiştirme bu endpoint'in yan etkisi değildir.
- Kullanıcılar/Silinenler kompakt liste seçimi ve her kullanıcıda İş takibi
  eylemi kullanılır. Modal, CSS Modules ile temaya uygun kartlar, sunucu filtreleri,
  arama, yenileme ve sayfalama sunar. Global tasarım sistemi değiştirilmez.

## Sınırlar

Yeni migration veya bağımlılık eklenmez; V1–V12 korunur. Mevcut atamalardaki
tamamlanmış işler hemen okunabilir. Önceden kaldırılmış, V12 öncesi kaybolan
atamalar geri getirilemez. Bu genel audit veya tüm durum geçişlerinin tarihçesi
değildir. Workspace modeli eklendiğinde yeni okuma modeli aynı kurum sınırını
uygulamalıdır; frontend filtreleri yetkilendirmenin yerine geçmez.
