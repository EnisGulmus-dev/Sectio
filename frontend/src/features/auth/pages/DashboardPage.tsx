import { useAuth } from "../useAuth";

export function DashboardPage() {
  const { user } = useAuth();

  return (
    <div className="page-stack">
        <header className="dashboard-header">
          <div>
            <p className="eyebrow">Çalışma alanı</p>
            <h1>Günaydın, {user?.name.split(" ")[0]}</h1>
            <p>Kullanıcı yönetimi hazır. Proje ve görev modülleri sıradaki aşamada eklenecek.</p>
          </div>
          <span className="role-badge">{user?.role}</span>
        </header>

        <section className="status-grid" aria-label="Sistem durumu">
          <article className="status-card accent">
            <span className="card-index">01</span>
            <h2>Güvenli oturum</h2>
            <p>Session cookie ve CSRF koruması aktif.</p>
            <span className="card-state ready">Hazır</span>
          </article>
          <article className="status-card">
            <span className="card-index">02</span>
            <h2>Kullanıcı yönetimi</h2>
            <p>Çalışan ekleme, rol, erişim ve geçici parola yönetimi aktif.</p>
            <span className="card-state ready">Hazır</span>
          </article>
          <article className="status-card">
            <span className="card-index">03</span>
            <h2>Projeler ve görevler</h2>
            <p>Veri modeli hazır; uygulama servisleri geliştirilecek.</p>
            <span className="card-state">Bekliyor</span>
          </article>
        </section>

        <section className="next-step-card">
          <div>
            <p className="eyebrow">Sonraki kilometre taşı</p>
            <h2>Admin kullanıcı yönetimi</h2>
            <p>
              Proje oluşturma, üye atama ve arşivleme akışlarını kullanıcı yönetiminin
              üzerine ekleyeceğiz.
            </p>
          </div>
          <div className="progress-ring" aria-label="Altyapı yüzde 60 tamamlandı">
            <strong>70%</strong>
            <span>altyapı</span>
          </div>
        </section>
    </div>
  );
}
