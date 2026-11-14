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

const emptyForm: CreateUserInput = {
  name: "",
  email: "",
  temporaryPassword: "",
  role: "EMPLOYEE",
  jobTitle: "SOFTWARE_DEVELOPER",
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
  const [busyId, setBusyId] = useState<number | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [showCreate, setShowCreate] = useState(false);
  const [form, setForm] = useState<CreateUserInput>(emptyForm);
  const [editing, setEditing] = useState<ManagedUser | null>(null);
  const [resetting, setResetting] = useState<ManagedUser | null>(null);
  const [temporaryPassword, setTemporaryPassword] = useState("");
  const [statusTarget, setStatusTarget] = useState<ManagedUser | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<ManagedUser | null>(null);
  const [deletionImpact, setDeletionImpact] = useState<UserDeletionImpact | null>(null);
  const { showToast } = useToast();

  async function loadUsers() {
    setLoading(true);
    setError(null);
    try {
      setUsers(await usersApi.list());
    } catch (requestError) {
      setError(errorMessage(requestError));
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    void loadUsers();
  }, []);

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
    setError(null);
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email.trim())) {
      showToast("Geçerli bir e-posta adresi girin. Örnek: ad@firma.com", "error");
      return;
    }
    try {
      const created = await usersApi.create(form);
      setUsers((current) => [...current, created].sort((a, b) => a.name.localeCompare(b.name, "tr")));
      setForm(emptyForm);
      setShowCreate(false);
      showToast(`${created.name} oluşturuldu. İlk girişte parolasını değiştirmesi gerekecek.`);
    } catch (requestError) {
      setError(errorMessage(requestError));
    }
  }

  async function handleEdit(event: FormEvent) {
    event.preventDefault();
    if (!editing) return;
    setBusyId(editing.id);
    setError(null);
    try {
      const updated = await usersApi.update(editing.id, {
        name: editing.name,
        role: editing.role,
        jobTitle: editing.jobTitle,
      });
      replaceUser(updated);
      setEditing(null);
      showToast("Kullanıcı bilgileri güncellendi.");
    } catch (requestError) {
      setError(errorMessage(requestError));
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
      showToast(`${deleteTarget.name} kalıcı olarak silindi.`);
      setDeleteTarget(null);
      setDeletionImpact(null);
    } catch (requestError) {
      setError(errorMessage(requestError));
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
    ? deletionImpact.ownedProjects > 0
      ? `${deleteTarget.name} ${deletionImpact.ownedProjects} projenin sahibi. Bu projeler devredilmeden veya silinmeden kullanıcı silinemez.`
      : `${deleteTarget.name} silinecek; ${deletionImpact.projectMemberships} proje üyeliği ve ${deletionImpact.assignedTasks} görev ataması kaldırılacak. Başka çalışanı kalmayan görevler otomatik silinecek. Emin misiniz?`
    : "";

  return (
    <div className="page-stack">
      <header className="page-header">
        <div>
          <p className="eyebrow">Yönetim</p>
          <h1>Kullanıcılar</h1>
          <p>Çalışan hesaplarını, rollerini ve erişim durumlarını yönetin.</p>
        </div>
        <button className="primary-button compact-button" type="button" aria-haspopup="dialog" onClick={() => setShowCreate(true)}>
          Kullanıcı ekle
        </button>
      </header>

      {error && <InlineAlert message={error} />}

      <ModalPanel open={showCreate} title="Yeni kullanıcı" onClose={() => setShowCreate(false)}>
          <form className="management-form" onSubmit={handleCreate}>
            <label>Ad soyad<input required maxLength={100} value={form.name} onChange={(event) => setForm({ ...form, name: event.target.value })} /></label>
            <label>E-posta<input required type="email" value={form.email} onChange={(event) => setForm({ ...form, email: event.target.value })} /></label>
            <label>Geçici parola<PasswordInput required minLength={12} maxLength={72} value={form.temporaryPassword} onChange={(event) => setForm({ ...form, temporaryPassword: event.target.value })} /><small>12–72 karakter; kullanıcı ilk girişte değiştirecek.</small></label>
            <label>Rol<select value={form.role} onChange={(event) => setForm({ ...form, role: event.target.value as UserRole })}><option value="EMPLOYEE">Çalışan</option><option value="ADMIN">Yönetici</option></select></label>
            <label>İş unvanı<select value={form.jobTitle} onChange={(event) => setForm({ ...form, jobTitle: event.target.value as JobTitle })}>{jobTitleOptions.map(([value, label]) => <option value={value} key={value}>{label}</option>)}</select><small>Takım lideri, proje yöneticisi ve proje sahibi proje açabilir.</small></label>
            <button className="primary-button compact-button" type="submit">Kaydet</button>
          </form>
      </ModalPanel>

      <ModalPanel open={editing !== null} title="Kullanıcıyı düzenle" onClose={() => setEditing(null)}>
        {editing && (
          <form className="management-form" onSubmit={handleEdit}>
            <label>Ad soyad<input required maxLength={100} value={editing.name} onChange={(event) => setEditing({ ...editing, name: event.target.value })} /></label>
            <label>Rol<select value={editing.role} onChange={(event) => setEditing({ ...editing, role: event.target.value as UserRole })}><option value="EMPLOYEE">Çalışan</option><option value="ADMIN">Yönetici</option></select></label>
            <label>İş unvanı<select value={editing.jobTitle} onChange={(event) => setEditing({ ...editing, jobTitle: event.target.value as JobTitle })}>{jobTitleOptions.map(([value, label]) => <option value={value} key={value}>{label}</option>)}</select></label>
            <div className="button-row"><button className="primary-button compact-button" disabled={busyId === editing.id} type="submit">Güncelle</button><button className="secondary-button" type="button" onClick={() => setEditing(null)}>Vazgeç</button></div>
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

      <section className="panel table-panel">
        <div className="table-toolbar">
          <label className="search-field">Kullanıcı ara<input placeholder="Ad, e-posta veya rol" value={query} onChange={(event) => setQuery(event.target.value)} /></label>
          <span>{filteredUsers.length} kayıt</span>
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
                  <td><span className={`status-label ${user.active ? "active" : "inactive"}`}>{user.active ? "Aktif" : "Pasif"}</span></td>
                  <td>{user.mustChangePassword ? "Değişiklik bekliyor" : "Güncel"}</td>
                  <td><div className="row-actions"><button type="button" onClick={() => setEditing(user)}>Düzenle</button><button disabled={busyId === user.id} type="button" onClick={() => setStatusTarget(user)}>{user.active ? "Pasifleştir" : "Aktifleştir"}</button><button type="button" onClick={() => setResetting(user)}>Parola</button><button className="danger-text" disabled={busyId === user.id} type="button" onClick={() => void prepareDelete(user)}>Sil</button></div></td>
                </tr>
              ))}</tbody>
            </table>
          </div>
        )}
      </section>

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
        title="Kullanıcı kalıcı olarak silinsin mi?"
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
