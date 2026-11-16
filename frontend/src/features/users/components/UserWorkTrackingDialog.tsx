import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { ModalPanel } from "../../../shared/ui/ModalPanel";
import { EmptyState, InlineAlert, LoadingState } from "../../../shared/ui/Feedback";
import { isApiClientError } from "../../../shared/api/apiError";
import { usersApi } from "../api/usersApi";
import { jobTitleLabels } from "../../auth/jobTitles";
import { statusLabels } from "../../projects/projectFormat";
import type { PageResponse, TaskStatus } from "../../projects/types";
import type { ManagedUser, UserWorkTrackingEntry, UserWorkTrackingScope } from "../types";
import styles from "./UserWorkTrackingDialog.module.css";

function dateLabel(value: string) {
  return new Intl.DateTimeFormat("tr-TR", { day: "numeric", month: "short", year: "numeric", hour: "2-digit", minute: "2-digit" }).format(new Date(value));
}

export function UserWorkTrackingDialog({ user, onClose }: { user: ManagedUser | null; onClose: () => void }) {
  const [page, setPage] = useState(0);
  const [scope, setScope] = useState<UserWorkTrackingScope>("ALL");
  const [status, setStatus] = useState<TaskStatus | "">("");
  const [draftQuery, setDraftQuery] = useState("");
  const [query, setQuery] = useState("");
  const [refresh, setRefresh] = useState(0);
  const [result, setResult] = useState<PageResponse<UserWorkTrackingEntry> | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const userId = user?.id;

  useEffect(() => {
    if (userId === undefined) return;
    let current = true;
    setLoading(true);
    setError(null);
    usersApi.workTracking(userId, { page, scope, status, query })
      .then((response) => { if (current) setResult(response); })
      .catch((caught) => { if (current) { setResult(null); setError(isApiClientError(caught) ? caught.message : "İş takibi yüklenemedi"); } })
      .finally(() => { if (current) setLoading(false); });
    return () => { current = false; };
  }, [userId, page, scope, status, query, refresh]);

  const initials = user?.name.trim().split(/\s+/).slice(0, 2).map((part) => part[0]).join("") ?? "";

  return <ModalPanel className={styles.dialog} open={user !== null} title={`${user?.name ?? "Çalışan"} · İş takibi`} onClose={onClose}>
    <div className={styles.identity}>
      <span className={styles.avatar} aria-hidden="true">{initials}</span>
      <div className={styles.identityText}><strong>{user?.name}</strong><span>{user && jobTitleLabels[user.jobTitle]} · {user?.email}</span></div>
      <span className={styles.accountState}>{user?.deletedAt ? "Silinmiş hesap" : user?.active ? "Aktif hesap" : "Pasif hesap"}</span>
    </div>
    <p className={styles.intro}>Devam eden ve tamamlanan işleri, ayrıca geçmiş atamaları tek yerden izleyin.</p>
    <form className={styles.filters} onSubmit={(event) => { event.preventDefault(); setPage(0); setQuery(draftQuery.trim()); }}>
      <label className={styles.searchField}>
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" aria-hidden="true"><circle cx="10.5" cy="10.5" r="6.5" /><path d="m16 16 4.5 4.5" /></svg>
        <input aria-label="Çalışanın işlerinde ara" type="search" placeholder="Görev veya proje ara" value={draftQuery} onChange={(event) => setDraftQuery(event.target.value)} />
      </label>
      <select aria-label="Atama kapsamı" value={scope} onChange={(event) => { setPage(0); setScope(event.target.value as UserWorkTrackingScope); }}>
        <option value="ALL">Tüm atamalar</option><option value="CURRENT">Güncel atamalar</option><option value="ENDED">Geçmiş atamalar</option>
      </select>
      <select aria-label="İş durumuna göre filtrele" value={status} onChange={(event) => { setPage(0); setStatus(event.target.value as TaskStatus | ""); }}>
        <option value="">Tüm durumlar</option>{Object.entries(statusLabels).map(([value, label]) => <option key={value} value={value}>{label}</option>)}
      </select>
      <button className={styles.actionButton} type="submit">Ara</button>
    </form>
    <div className={styles.resultBar}>
      <span role="status">{loading ? "İşler yükleniyor…" : error ? "İşler yüklenemedi" : `${result?.totalElements ?? 0} atama kaydı`}</span>
      <button className={styles.refreshButton} type="button" disabled={loading} onClick={() => setRefresh((value) => value + 1)}>Yenile</button>
    </div>
    <section className={styles.results} aria-label="Çalışanın işleri" aria-busy={loading}>
      {loading ? <LoadingState /> : error ? <InlineAlert message={error} /> : !result?.content.length
        ? <EmptyState title="Bu filtreye uygun iş yok" description="Durum ve atama kapsamını değiştirerek diğer işleri görüntüleyebilirsiniz." />
        : <ul className={styles.workList}>{result.content.map((entry) => <li className={styles.workCard} key={entry.id}>
          <div className={styles.workMain}>
            <div className={styles.overline}><span className={styles.sourceBadge} data-source={entry.assignmentState}>{entry.assignmentState === "CURRENT" ? "Güncel atama" : "Geçmiş atama"}</span><span>{entry.projectName}</span></div>
            <h3>{entry.taskTitle}</h3>
            <p className={styles.activity}>{entry.assignmentState === "ENDED"
              ? `${entry.endReason === "USER_DELETED" ? "Kullanıcı silindi" : "Proje üyeliği kaldırıldı"} · ${dateLabel(entry.activityAt)}`
              : `${entry.completedAt ? "Tamamlandı" : "Son güncelleme"} · ${dateLabel(entry.completedAt ?? entry.activityAt)}`}</p>
          </div>
          <div className={styles.workAside}>
            <span className={styles.statusBadge} data-status={entry.status}>{statusLabels[entry.status]}</span>
            {entry.assignmentState === "ENDED" && <small>Atama sonundaki durum</small>}
            {entry.projectId !== null && <Link className={styles.projectLink} to={`/projects/${entry.projectId}?tab=tasks`}>Projeye git <span aria-hidden="true">↗</span></Link>}
          </div>
        </li>)}</ul>}
    </section>
    <footer className={styles.footer}>
      <span>Geçmiş atamalar, sonlandırıldıkları andaki bilgileri korur.</span>
      {result && result.totalPages > 1 && <nav className={styles.pagination} aria-label="İş takibi sayfaları">
        <button type="button" disabled={loading || result.first} onClick={() => setPage((value) => value - 1)}>Önceki</button>
        <span>{result.page + 1} / {result.totalPages}</span>
        <button type="button" disabled={loading || result.last} onClick={() => setPage((value) => value + 1)}>Sonraki</button>
      </nav>}
    </footer>
  </ModalPanel>;
}
