import { type FormEvent, useEffect, useMemo, useState } from "react";

import { isApiClientError } from "../../../shared/api/apiError";
import { ConfirmDialog } from "../../../shared/ui/ConfirmDialog";
import { ModalPanel } from "../../../shared/ui/ModalPanel";
import { PasswordInput } from "../../../shared/ui/PasswordInput";
import { EmptyState, InlineAlert, LoadingState } from "../../../shared/ui/Feedback";
import { useToast } from "../../../shared/ui/ToastProvider";
import type { JobTitle, UserRole } from "../../auth/types";
import { jobTitleLabels, jobTitleOptions } from "../../auth/jobTitles";
import { usersApi } from "../api/usersApi";
import type { CreateUserInput, ManagedUser, UserDeletionImpact } from "../types";
import { UserWorkTrackingDialog } from "../components/UserWorkTrackingDialog";
import styles from "./UsersPage.module.css";
import { organizationApi, type CompanyTeam, type OrganizationPerson } from "../../organization/organizationApi";

const emptyForm: CreateUserInput = {
  name: "",
  email: "",
  temporaryPassword: "",
  role: "EMPLOYEE",
  jobTitle: "SOFTWARE_DEVELOPER",
  managerId: null,
  teamIds: [],
};

function errorMessage(error: unknown) {
  return isApiClientError(error)
    ? error.message
    : "İşlem tamamlanamadı. Lütfen tekrar deneyin.";
}

export function UsersPage() {
  const [users, setUsers] = useState<ManagedUser[]>([]);
  const [query, setQuery] = useState("");
  const [loading, setLoading] = useState(true);
  const [showDeleted, setShowDeleted] = useState(false);
  const [historyUser, setHistoryUser] = useState<ManagedUser | null>(null);
  const [busyId, setBusyId] = useState<number | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [showCreate, setShowCreate] = useState(false);
  const [companyTeams, setCompanyTeams] = useState<CompanyTeam[]>([]);
  const [managers, setManagers] = useState<OrganizationPerson[]>([]);
  const [organizationLoading, setOrganizationLoading] = useState(false);
  const [organizationError, setOrganizationError] = useState<string | null>(null);
  const [creating, setCreating] = useState(false);
  const [createError, setCreateError] = useState<string | null>(null);
  const [createFieldErrors, setCreateFieldErrors] = useState<Record<string, string>>({});
  const [restoreTarget, setRestoreTarget] = useState<ManagedUser | null>(null);
  const [restorePassword, setRestorePassword] = useState("");
  const [restoreError, setRestoreError] = useState<string | null>(null);
  const [purgeTarget, setPurgeTarget] = useState<ManagedUser | null>(null);
  const [form, setForm] = useState<CreateUserInput>(emptyForm);
  const [editing, setEditing] = useState<ManagedUser | null>(null);
  const [editManagerId, setEditManagerId] = useState<number | null>(null);
  const [editTeamIds, setEditTeamIds] = useState<number[]>([]);
  const [editOrganizationLoading, setEditOrganizationLoading] = useState(false);
  const [editOrganizationError, setEditOrganizationError] = useState<string | null>(null);
  const [editError, setEditError] = useState<string | null>(null);
  const [resetting, setResetting] = useState<ManagedUser | null>(null);
  const [temporaryPassword, setTemporaryPassword] = useState("");
  const [statusTarget, setStatusTarget] = useState<ManagedUser | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<ManagedUser | null>(null);
  const [deletionImpact, setDeletionImpact] = useState<UserDeletionImpact | null>(null);
  const { showToast } = useToast();

  useEffect(() => {
    let current = true;
    setLoading(true);
    setError(null);
    usersApi.list(showDeleted)
      .then((result) => { if (current) setUsers(result); })
      .catch((requestError) => { if (current) setError(errorMessage(requestError)); })
      .finally(() => { if (current) setLoading(false); });
    return () => { current = false; };
  }, [showDeleted]);

  useEffect(() => {
    if (!showCreate) return;
    let live = true; setOrganizationLoading(true); setOrganizationError(null);
    Promise.all([organizationApi.teams(), organizationApi.managers()]).then(([teams, people]) => { if (live) { setCompanyTeams(teams); setManagers(people); } })
      .catch(() => { if (live) setOrganizationError("Organizasyon bilgileri yüklenemedi. Pencereyi kapatıp tekrar açın."); })
      .finally(() => { if (live) setOrganizationLoading(false); });
    return () => { live = false; };
  }, [showCreate]);

  useEffect(() => {
    if (!editing) return;
    let live = true; setEditOrganizationLoading(true); setEditError(null); setEditOrganizationError(null);
    Promise.all([organizationApi.teams(), organizationApi.managers(), organizationApi.get(editing.id)]).then(([teams, people, relationship]) => {
      if (live) { setCompanyTeams(teams); setManagers(people); setEditManagerId(relationship.managerId); setEditTeamIds(relationship.teams.map((team) => team.id)); }
    }).catch(() => { if (live) setEditOrganizationError("Organizasyon bilgileri yüklenemedi. Pencereyi kapatıp tekrar açın."); })
      .finally(() => { if (live) setEditOrganizationLoading(false); });
    return () => { live = false; };
  }, [editing?.id]);

  const filteredUsers = useMemo(() => {
    const normalized = query.trim().toLocaleLowerCase("tr-TR");
    if (!normalized) return users;
    return users.filter((user) =>
      `${user.name} ${user.email} ${user.role} ${jobTitleLabels[user.jobTitle]}`.toLocaleLowerCase("tr-TR").includes(normalized),
    );
  }, [query, users]);

  function replaceUser(updated: ManagedUser) {
    setUsers((current) => current.map((user) => user.id === updated.id ? updated : user));
  }

  async function handleCreate(event: FormEvent) {
    event.preventDefault();
    if (creating) return;
    setError(null);
    setCreateError(null); setCreateFieldErrors({});
    if (!/^[\x21-\x7e]+$/.test(form.email.trim()) || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email.trim())) {
      setCreateError("Geçerli bir e-posta adresi girin; Türkçe karakter kullanmayın. Örnek: ad@firma.com");
      return;
    }
    setCreating(true);
    try {
      const created = await usersApi.create({ ...form, name: form.name.trim(), email: form.email.trim().toLowerCase() });
      setUsers((current) => [...current, created].sort((a, b) => a.name.localeCompare(b.name, "tr")));
      setForm(emptyForm);
      setShowCreate(false);
      showToast(`${created.name} oluşturuldu. İlk girişte parolasını değiştirmesi gerekecek.`);
    } catch (requestError) {
      setCreateError(errorMessage(requestError));
      if (isApiClientError(requestError)) setCreateFieldErrors(requestError.fieldErrors);
    } finally { setCreating(false); }
  }

  async function handleRestore(event: FormEvent) {
    event.preventDefault();
    if (!restoreTarget || busyId !== null) return;
    setBusyId(restoreTarget.id); setRestoreError(null);
    try {
      await usersApi.restore(restoreTarget.id, restorePassword);
      setUsers((current) => current.filter((user) => user.id !== restoreTarget.id));
      setRestoreTarget(null); setRestorePassword("");
      showToast("Kullanıcı geri yüklendi. Kullanıcılar listesinden görebilirsiniz; ilk girişte parolasını değiştirecek.");
    } catch (caught) { setRestoreError(errorMessage(caught)); }
    finally { setBusyId(null); }
  }

  async function handlePurge() {
    if (!purgeTarget || busyId !== null) return;
    setBusyId(purgeTarget.id); setError(null);
    try {
      await usersApi.purge(purgeTarget.id);
      setUsers((current) => current.filter((user) => user.id !== purgeTarget.id));
      setPurgeTarget(null);
      showToast("Hesap kalıcı silindi. Kimlik bilgileri anonimleştirildi; iş kayıtları korundu.");
    } catch (caught) { setError(errorMessage(caught)); setPurgeTarget(null); }
    finally { setBusyId(null); }
  }

  async function handleEdit(event: FormEvent) {
    event.preventDefault();
    if (!editing || editOrganizationLoading || editOrganizationError) return;
    setEditError(null);
    if (!/^[\x21-\x7e]+$/.test(editing.email.trim()) || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(editing.email.trim())) { setEditError("Geçerli bir e-posta girin; Türkçe karakter kullanmayın."); return; }
    setBusyId(editing.id);
    setError(null);
    try {
      const updated = await usersApi.update(editing.id, {
        name: editing.name,
        email: editing.email.trim().toLowerCase(),
        role: editing.role,
        jobTitle: editing.jobTitle,
        managerId: editManagerId,
        teamIds: editTeamIds,
      });
      replaceUser(updated);
      setEditing(null);
      showToast("Kullanıcı bilgileri güncellendi. E-posta değiştiyse kullanıcı yeniden giriş yapmalıdır.");
    } catch (requestError) {
      setEditError(errorMessage(requestError));
    } finally {
      setBusyId(null);
    }
  }

  async function handleStatus(user: ManagedUser) {
    setBusyId(user.id);
    setError(null);
    try {
      const updated = await usersApi.changeStatus(user.id, !user.active);
      replaceUser(updated);
      showToast(`${updated.name} ${updated.active ? "aktifleştirildi" : "pasifleştirildi"}.`);
      setStatusTarget(null);
    } catch (requestError) {
      setError(errorMessage(requestError));
      setStatusTarget(null);
    } finally {
      setBusyId(null);
    }
  }

  async function handleResetPassword(event: FormEvent) {
    event.preventDefault();
    if (!resetting) return;
    setBusyId(resetting.id);
    setError(null);
    try {
      const updated = await usersApi.resetPassword(resetting.id, temporaryPassword);
      replaceUser(updated);
      setResetting(null);
      setTemporaryPassword("");
      showToast(`${updated.name} için geçici parola kaydedildi.`);
    } catch (requestError) {
      setError(errorMessage(requestError));
    } finally {
      setBusyId(null);
    }
  }

  async function handleDelete() {
    if (!deleteTarget) return;
    setBusyId(deleteTarget.id);
    setError(null);
    try {
      await usersApi.deleteUser(deleteTarget.id);
      setUsers((current) => current.filter((user) => user.id !== deleteTarget.id));
      showToast(`${deleteTarget.name} silindi; oluşturduğu işler ve görev geçmişi korundu.`);
      setDeleteTarget(null);
      setDeletionImpact(null);
    } catch (requestError) {
      setError(errorMessage(requestError));
      setDeleteTarget(null); setDeletionImpact(null);
    } finally {
      setBusyId(null);
    }
  }

  async function prepareDelete(user: ManagedUser) {
    setBusyId(user.id);
    setError(null);
    try {
      setDeletionImpact(await usersApi.deletionImpact(user.id));
      setDeleteTarget(user);
    } catch (requestError) {
      setError(errorMessage(requestError));
    } finally {
      setBusyId(null);
    }
  }

  const deleteDescription = deleteTarget && deletionImpact
    ? `${deleteTarget.name} Silinenler bölümüne taşınacak; buradan geri yüklenebilir. Oturumları kapatılacak; ${deletionImpact.projectMemberships} proje üyeliği ve ${deletionImpact.assignedTasks} görev ataması kaldırılacak. Oluşturduğu ${deletionImpact.createdTasks} görev, iş geçmişi ve sahibi olduğu ${deletionImpact.ownedProjects} proje korunacak. Kişi ve takım hedefi kalmayan görevler, ilgili projenin Görevler sekmesindeki Atanmamış görevler alanından yeniden atanabilir.`
    : "";

  return (
    <div className="page-stack">
      <header className="page-header">
        <div>
          <p className="eyebrow">Yönetim</p>
          <h1>Kullanıcılar</h1>
          <p>Çalışan hesaplarını, rollerini ve erişim durumlarını yönetin.</p>
        </div>
        <button className="primary-button compact-button" type="button" disabled={showDeleted} aria-haspopup="dialog" onClick={() => { setCreateError(null); setCreateFieldErrors({}); setShowCreate(true); }}>
          Kullanıcı ekle
        </button>
      </header>

      {error && <InlineAlert message={error} />}

      <ModalPanel open={showCreate} title="Yeni kullanıcı" onClose={() => { if (!creating) setShowCreate(false); }}>
          {createError && <InlineAlert message={createError} />}
          {organizationError && <InlineAlert message={organizationError} />}
          {Object.keys(createFieldErrors).length > 0 && <ul className="field-error">{Object.entries(createFieldErrors).map(([field, message]) => <li key={field}>{({ name: "Ad soyad", email: "E-posta", temporaryPassword: "Geçici parola", role: "Rol", jobTitle: "İş unvanı" } as Record<string, string>)[field] ?? field}: {message}</li>)}</ul>}
          <form className="management-form" onSubmit={handleCreate}>
            <label>Ad soyad<input required maxLength={100} value={form.name} onChange={(event) => setForm({ ...form, name: event.target.value })} /></label>
            <label>E-posta<input required type="text" inputMode="email" aria-label="E-posta" autoCapitalize="none" autoCorrect="off" spellCheck={false} maxLength={255} aria-describedby="create-email-help" value={form.email} onChange={(event) => setForm({ ...form, email: event.target.value })} /><small id="create-email-help">Türkçe karakter kullanmayın. Örnek: ad@firma.com</small></label>
            <label>Geçici parola<PasswordInput required minLength={12} maxLength={72} value={form.temporaryPassword} onChange={(event) => setForm({ ...form, temporaryPassword: event.target.value })} /><small>12–72 karakter; kullanıcı ilk girişte değiştirecek.</small></label>
            <label>Rol<select value={form.role} onChange={(event) => {
              const role = event.target.value as UserRole;
              setForm({ ...form, role, jobTitle: role === "ADMIN" ? "CIO" : "SOFTWARE_DEVELOPER" });
            }}><option value="EMPLOYEE">Çalışan</option><option value="ADMIN">Yönetici</option></select></label>
            <label>İş unvanı<select value={form.jobTitle} onChange={(event) => setForm({ ...form, jobTitle: event.target.value as JobTitle })}>{jobTitleOptions.map(([value, label]) => <option value={value} key={value}>{label}</option>)}</select><small>Takım lideri, proje yöneticisi ve proje sahibi proje açabilir.</small></label>
            <label>Bağlı yönetici<select value={form.managerId ?? ""} onChange={(event) => setForm({ ...form, managerId: event.target.value ? Number(event.target.value) : null })}><option value="">Belirlenmemiş</option>{managers.map((person) => <option key={person.id} value={person.id}>{person.name}</option>)}</select></label>
            <fieldset className={`full-field ${styles.orgTeams}`}><legend>Şirket takımları</legend><p>İsteğe bağlı; birden fazla takım seçilebilir.</p>{companyTeams.length === 0 && <p>Henüz şirket takımı yok.</p>}{companyTeams.map((team) => <label key={team.id}><input type="checkbox" checked={(form.teamIds ?? []).includes(team.id)} onChange={() => setForm({ ...form, teamIds: (form.teamIds ?? []).includes(team.id) ? (form.teamIds ?? []).filter((id) => id !== team.id) : [...(form.teamIds ?? []), team.id] })} /> {team.name}</label>)}</fieldset>
            <button className="primary-button compact-button" disabled={creating || organizationLoading || !!organizationError} type="submit">{creating ? "Kaydediliyor…" : "Kaydet"}</button>
          </form>
      </ModalPanel>

      <ModalPanel open={restoreTarget !== null} title="Kullanıcıyı geri yükle" onClose={() => { if (busyId === null) { setRestoreTarget(null); setRestorePassword(""); } }}>
        <p className="modal-description">{restoreTarget?.name} aktif kullanıcı olarak geri yüklenecek. Eski proje üyelikleri ve görev atamaları otomatik açılmaz; proje sahipliği korunur. Yeni geçici parolayla ilk girişte parola değişimi zorunludur.</p>
        {restoreError && <InlineAlert message={restoreError} />}
        <form className="management-form" onSubmit={handleRestore}>
          <label>Yeni geçici parola<PasswordInput required minLength={12} maxLength={72} autoComplete="new-password" value={restorePassword} onChange={(event) => setRestorePassword(event.target.value)} /></label>
          <button className="primary-button compact-button" disabled={busyId !== null}>Geri yükle</button>
        </form>
      </ModalPanel>

      <ModalPanel open={editing !== null} title="Kullanıcıyı düzenle" onClose={() => setEditing(null)}>
        {editing && (
          <form className="management-form" onSubmit={handleEdit}>
            {editError && <InlineAlert message={editError} />}
            {editOrganizationError && <InlineAlert message={editOrganizationError} />}
            <label>E-posta<input required type="text" inputMode="email" maxLength={255} value={editing.email} onChange={(event) => setEditing({ ...editing, email: event.target.value })} /><small>E-posta değişirse açık oturumlar kapatılır.</small></label>
            <label>Ad soyad<input required maxLength={100} value={editing.name} onChange={(event) => setEditing({ ...editing, name: event.target.value })} /></label>
            <label>Rol<select value={editing.role} onChange={(event) => setEditing({ ...editing, role: event.target.value as UserRole })}><option value="EMPLOYEE">Çalışan</option><option value="ADMIN">Yönetici</option></select></label>
            <label>İş unvanı<select value={editing.jobTitle} onChange={(event) => setEditing({ ...editing, jobTitle: event.target.value as JobTitle })}>{jobTitleOptions.map(([value, label]) => <option value={value} key={value}>{label}</option>)}</select></label>
            <label>Bağlı yönetici<select disabled={editOrganizationLoading || !!editOrganizationError} value={editManagerId ?? ""} onChange={(event) => setEditManagerId(event.target.value ? Number(event.target.value) : null)}><option value="">Belirlenmemiş</option>{managers.filter((person) => person.id !== editing.id).map((person) => <option key={person.id} value={person.id}>{person.name}</option>)}</select></label>
            <fieldset className={`full-field ${styles.orgTeams}`} disabled={editOrganizationLoading || !!editOrganizationError}><legend>Şirket takımları</legend>{companyTeams.length === 0 && <p>Henüz şirket takımı yok.</p>}{companyTeams.map((team) => <label key={team.id}><input type="checkbox" checked={editTeamIds.includes(team.id)} onChange={() => setEditTeamIds((current) => current.includes(team.id) ? current.filter((id) => id !== team.id) : [...current, team.id])} /> {team.name}</label>)}</fieldset>
            <div className="button-row"><button className="primary-button compact-button" disabled={busyId === editing.id || editOrganizationLoading || !!editOrganizationError} type="submit">Güncelle</button><button className="secondary-button" type="button" onClick={() => setEditing(null)}>Vazgeç</button></div>
          </form>
        )}
      </ModalPanel>

      <ModalPanel open={resetting !== null} title={resetting ? `${resetting.name} için geçici parola` : "Geçici parola"} onClose={() => setResetting(null)}>
        {resetting && (
          <form className="management-form" onSubmit={handleResetPassword}>
            <label>Yeni geçici parola<PasswordInput required minLength={12} maxLength={72} value={temporaryPassword} onChange={(event) => setTemporaryPassword(event.target.value)} /></label>
            <div className="button-row"><button className="primary-button compact-button" disabled={busyId === resetting.id} type="submit">Parolayı sıfırla</button><button className="secondary-button" type="button" onClick={() => setResetting(null)}>Vazgeç</button></div>
          </form>
        )}
      </ModalPanel>

      <section className={`panel table-panel ${styles.usersPanel}`}>
        <div className={`table-toolbar ${styles.toolbar}`}>
          <label className={`search-field ${styles.searchField}`}>Kullanıcı ara<input placeholder="Ad, e-posta veya rol" value={query} onChange={(event) => setQuery(event.target.value)} /></label>
          <span className={styles.counter}>{filteredUsers.length} kayıt</span>
          <div className={styles.listSwitch} role="group" aria-label="Kullanıcı listesi">
            <button type="button" aria-pressed={!showDeleted} onClick={() => setShowDeleted(false)}>Kullanıcılar</button>
            <button type="button" aria-pressed={showDeleted} onClick={() => setShowDeleted(true)}>Silinenler</button>
          </div>
        </div>

        {loading ? <LoadingState label="Kullanıcılar yükleniyor…" /> : filteredUsers.length === 0 ? <EmptyState title="Eşleşen kullanıcı bulunamadı" description="Arama ifadenizi değiştirip yeniden deneyin." /> : (
          <div className="table-scroll">
            <table>
              <thead><tr><th>Kullanıcı</th><th>Sistem rolü</th><th>İş unvanı</th><th>Durum</th><th>Parola</th><th><span className="sr-only">İşlemler</span></th></tr></thead>
              <tbody>{filteredUsers.map((user) => (
                <tr key={user.id}>
                  <td><strong>{user.name}</strong><span>{user.email}</span></td>
                  <td>{user.role === "ADMIN" ? "Yönetici" : "Çalışan"}</td>
                  <td>{jobTitleLabels[user.jobTitle]}</td>
                  <td><span className={`status-label ${user.active ? "active" : "inactive"}`}>{user.deletedAt ? "Silindi" : user.active ? "Aktif" : "Pasif"}</span></td>
                  <td>{user.mustChangePassword ? "Değişiklik bekliyor" : "Güncel"}</td>
                  <td>{user.deletedAt ? <div className={styles.archivedActions}><button type="button" disabled={busyId !== null} onClick={() => { setRestoreTarget(user); setRestorePassword(""); setRestoreError(null); }}>Geri yükle</button><button className="danger-text" type="button" disabled={busyId !== null} onClick={() => setPurgeTarget(user)}>Kalıcı sil</button><button className={styles.historyAction} type="button" onClick={() => setHistoryUser(user)}>İş takibi</button></div> : <div className={`row-actions ${styles.userActions}`}><button className={styles.historyAction} type="button" onClick={() => setHistoryUser(user)}>İş takibi</button><button type="button" onClick={() => { setEditError(null); setEditing(user); }}>Düzenle</button><button disabled={busyId === user.id} type="button" onClick={() => setStatusTarget(user)}>{user.active ? "Pasifleştir" : "Aktifleştir"}</button><button type="button" onClick={() => setResetting(user)}>Parola</button><button className="danger-text" disabled={busyId === user.id} type="button" onClick={() => void prepareDelete(user)}>Sil</button></div>}</td>
                </tr>
              ))}</tbody>
            </table>
          </div>
        )}
      </section>

      <UserWorkTrackingDialog key={historyUser?.id ?? "closed"} user={historyUser} onClose={() => setHistoryUser(null)} />
      <ConfirmDialog open={purgeTarget !== null} title="Hesap kalıcı silinsin mi?" description={`${purgeTarget?.name ?? "Seçilen kullanıcı"} Silinenler listesinden de kaldırılacak ve geri yüklenemeyecek. Hesaptaki ad, e-posta ve parola anonimleştirilecek; e-posta yeniden kullanılabilecek. Görevler, projeler ve geçmiş iş kayıtları silinmiş kullanıcı kimliğiyle korunacak.`} confirmLabel="Kalıcı sil" tone="danger" busy={busyId !== null} onCancel={() => setPurgeTarget(null)} onConfirm={() => void handlePurge()} />
      <ConfirmDialog
        open={statusTarget !== null}
        title={statusTarget?.active ? "Kullanıcı pasifleştirilsin mi?" : "Kullanıcı aktifleştirilsin mi?"}
        description={statusTarget ? `${statusTarget.name} hesabının erişim durumu değiştirilecek.` : ""}
        confirmLabel={statusTarget?.active ? "Pasifleştir" : "Aktifleştir"}
        busy={statusTarget ? busyId === statusTarget.id : false}
        onCancel={() => setStatusTarget(null)}
        onConfirm={() => statusTarget && void handleStatus(statusTarget)}
      />
      <ConfirmDialog
        open={deleteTarget !== null}
        title="Kullanıcı silinsin mi?"
        description={deleteDescription}
        confirmLabel="Kullanıcıyı sil"
        tone="danger"
        busy={deleteTarget ? busyId === deleteTarget.id : false}
        onCancel={() => { setDeleteTarget(null); setDeletionImpact(null); }}
        onConfirm={() => void handleDelete()}
      />
    </div>
  );
}
