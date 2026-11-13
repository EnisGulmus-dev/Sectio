import { type FormEvent, useEffect, useMemo, useState } from "react";

import { isApiClientError } from "../../../shared/api/apiError";
import { ConfirmDialog } from "../../../shared/ui/ConfirmDialog";
import { EmptyState, InlineAlert, LoadingState } from "../../../shared/ui/Feedback";
import { useToast } from "../../../shared/ui/ToastProvider";
import type { UserRole } from "../../auth/types";
import { usersApi } from "../api/usersApi";
import type { CreateUserInput, ManagedUser } from "../types";

const emptyForm: CreateUserInput = {
  name: "",
  email: "",
  temporaryPassword: "",
  role: "EMPLOYEE",
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
      `${user.name} ${user.email} ${user.role}`.toLocaleLowerCase("tr-TR").includes(normalized),
    );
  }, [query, users]);

  function replaceUser(updated: ManagedUser) {
    setUsers((current) => current.map((user) => user.id === updated.id ? updated : user));
  }

  async function handleCreate(event: FormEvent) {
    event.preventDefault();
    setError(null);
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

  return (
    <div className="page-stack">
      <header className="page-header">
        <div>
          <p className="eyebrow">Yönetim</p>
          <h1>Kullanıcılar</h1>
          <p>Çalışan hesaplarını, rollerini ve erişim durumlarını yönetin.</p>
        </div>
        <button className="primary-button compact-button" type="button" onClick={() => setShowCreate((value) => !value)}>
          {showCreate ? "Formu kapat" : "Kullanıcı ekle"}
        </button>
      </header>

      {error && <InlineAlert message={error} />}

      {showCreate && (
        <section className="panel">
          <h2>Yeni kullanıcı</h2>
          <form className="management-form" onSubmit={handleCreate}>
            <label>Ad soyad<input required maxLength={100} value={form.name} onChange={(event) => setForm({ ...form, name: event.target.value })} /></label>
            <label>E-posta<input required type="email" value={form.email} onChange={(event) => setForm({ ...form, email: event.target.value })} /></label>
            <label>Geçici parola<input required minLength={12} maxLength={72} type="password" value={form.temporaryPassword} onChange={(event) => setForm({ ...form, temporaryPassword: event.target.value })} /><small>12–72 karakter; kullanıcı ilk girişte değiştirecek.</small></label>
            <label>Rol<select value={form.role} onChange={(event) => setForm({ ...form, role: event.target.value as UserRole })}><option value="EMPLOYEE">Çalışan</option><option value="ADMIN">Yönetici</option></select></label>
            <button className="primary-button compact-button" type="submit">Kaydet</button>
          </form>
        </section>
      )}

      {editing && (
        <section className="panel">
          <h2>Kullanıcıyı düzenle</h2>
          <form className="management-form" onSubmit={handleEdit}>
            <label>Ad soyad<input required maxLength={100} value={editing.name} onChange={(event) => setEditing({ ...editing, name: event.target.value })} /></label>
            <label>Rol<select value={editing.role} onChange={(event) => setEditing({ ...editing, role: event.target.value as UserRole })}><option value="EMPLOYEE">Çalışan</option><option value="ADMIN">Yönetici</option></select></label>
            <div className="button-row"><button className="primary-button compact-button" disabled={busyId === editing.id} type="submit">Güncelle</button><button className="secondary-button" type="button" onClick={() => setEditing(null)}>Vazgeç</button></div>
          </form>
        </section>
      )}

      {resetting && (
        <section className="panel">
          <h2>{resetting.name} için geçici parola</h2>
          <form className="management-form" onSubmit={handleResetPassword}>
            <label>Yeni geçici parola<input required minLength={12} maxLength={72} type="password" value={temporaryPassword} onChange={(event) => setTemporaryPassword(event.target.value)} /></label>
            <div className="button-row"><button className="primary-button compact-button" disabled={busyId === resetting.id} type="submit">Parolayı sıfırla</button><button className="secondary-button" type="button" onClick={() => setResetting(null)}>Vazgeç</button></div>
          </form>
        </section>
      )}

      <section className="panel table-panel">
        <div className="table-toolbar">
          <label className="search-field">Kullanıcı ara<input placeholder="Ad, e-posta veya rol" value={query} onChange={(event) => setQuery(event.target.value)} /></label>
          <span>{filteredUsers.length} kayıt</span>
        </div>

        {loading ? <LoadingState label="Kullanıcılar yükleniyor…" /> : filteredUsers.length === 0 ? <EmptyState title="Eşleşen kullanıcı bulunamadı" description="Arama ifadenizi değiştirip yeniden deneyin." /> : (
          <div className="table-scroll">
            <table>
              <thead><tr><th>Kullanıcı</th><th>Rol</th><th>Durum</th><th>Parola</th><th><span className="sr-only">İşlemler</span></th></tr></thead>
              <tbody>{filteredUsers.map((user) => (
                <tr key={user.id}>
                  <td><strong>{user.name}</strong><span>{user.email}</span></td>
                  <td>{user.role === "ADMIN" ? "Yönetici" : "Çalışan"}</td>
                  <td><span className={`status-label ${user.active ? "active" : "inactive"}`}>{user.active ? "Aktif" : "Pasif"}</span></td>
                  <td>{user.mustChangePassword ? "Değişiklik bekliyor" : "Güncel"}</td>
                  <td><div className="row-actions"><button type="button" onClick={() => setEditing(user)}>Düzenle</button><button disabled={busyId === user.id} type="button" onClick={() => setStatusTarget(user)}>{user.active ? "Pasifleştir" : "Aktifleştir"}</button><button type="button" onClick={() => setResetting(user)}>Parola</button></div></td>
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
    </div>
  );
}
