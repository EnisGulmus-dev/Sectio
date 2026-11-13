import { useEffect, useState } from "react";

import { apiRequest } from "../../../shared/api/httpClient";
import { InlineAlert, LoadingState } from "../../../shared/ui/Feedback";
import { useAuth } from "../useAuth";

type DashboardSummary = { openTasks: number; completedTasks: number; overdueTasks: number };

export function DashboardPage() {
  const { user } = useAuth();
  const [summary, setSummary] = useState<DashboardSummary | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    apiRequest<DashboardSummary>("/api/v1/dashboard/summary")
      .then(setSummary)
      .catch(() => setError("Görev özeti yüklenemedi."));
  }, []);

  return (
    <div className="page-stack">
      <header className="dashboard-header">
        <div>
          <p className="eyebrow">Çalışma alanı</p>
          <h1>Günaydın, {user?.name.split(" ")[0]}</h1>
          <p>{user?.role === "ADMIN" ? "Sistem genelindeki" : "Size atanmış"} görevlerin güncel özeti.</p>
        </div>
        <span className="role-badge">{user?.role}</span>
      </header>

      {error && <InlineAlert message={error} />}
      {!summary && !error ? <LoadingState /> : summary && (
        <section className="status-grid" aria-label="Görev özeti">
          <article className="status-card accent"><span className="card-index">01</span><h2>Açık görev</h2><strong className="metric-value">{summary.openTasks}</strong></article>
          <article className="status-card"><span className="card-index">02</span><h2>Tamamlanan</h2><strong className="metric-value">{summary.completedTasks}</strong></article>
          <article className="status-card"><span className="card-index">03</span><h2>Geciken</h2><strong className="metric-value">{summary.overdueTasks}</strong></article>
        </section>
      )}

      <section className="next-step-card">
        <div><p className="eyebrow">Aktif modüller</p><h2>Projeler ve görevler</h2><p>Proje üyeliği, görev atama ve durum takibi kullanıma hazır.</p></div>
        <div className="progress-ring" aria-label="Temel iş akışları tamamlandı"><strong>Hazır</strong><span>temel akış</span></div>
      </section>
    </div>
  );
}
