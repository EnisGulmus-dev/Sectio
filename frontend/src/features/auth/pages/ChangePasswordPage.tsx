import { type FormEvent, useState } from "react";
import { useNavigate } from "react-router-dom";

import { isApiClientError } from "../../../shared/api/apiError";
import { useAuth } from "../useAuth";
import { PasswordInput } from "../../../shared/ui/PasswordInput";

export function ChangePasswordPage() {
  const navigate = useNavigate();
  const { user, changePassword, logout } = useAuth();
  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmation, setConfirmation] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError(null);

    if (newPassword !== confirmation) {
      setError("Yeni parola ve tekrarı aynı olmalıdır.");
      return;
    }

    if (newPassword === currentPassword) {
      setError("Yeni parola mevcut paroladan farklı olmalıdır.");
      return;
    }

    setSubmitting(true);
    try {
      await changePassword({ currentPassword, newPassword });
      navigate("/", { replace: true });
    } catch (caught) {
      if (isApiClientError(caught)) {
        setError(caught.message);
      } else {
        setError("Parola değiştirilemedi. Lütfen tekrar deneyin.");
      }
    } finally {
      setSubmitting(false);
    }
  }

  async function handleLogout() {
    await logout();
    navigate("/login", { replace: true });
  }

  return (
    <main className="centered-page">
      <section className="password-card">
        <div className="brand-lockup compact">
          <span className="brand-mark" aria-hidden="true">S</span>
          <span>Sectio</span>
        </div>

        <div className="security-icon" aria-hidden="true">✓</div>
        <p className="eyebrow">İlk giriş güvenliği</p>
        <h1>Geçici parolanızı değiştirin</h1>
        <p className="page-description">
          {user?.name}, çalışma alanına devam etmeden önce yalnızca sizin bildiğiniz
          kalıcı bir parola belirleyin.
        </p>

        <form onSubmit={handleSubmit} className="auth-form">
          <label>
            <span>Mevcut parola</span>
            <PasswordInput
              autoComplete="current-password"
              value={currentPassword}
              onChange={(event) => setCurrentPassword(event.target.value)}
              required
            />
          </label>

          <label>
            <span>Yeni parola</span>
            <PasswordInput
              autoComplete="new-password"
              value={newPassword}
              onChange={(event) => setNewPassword(event.target.value)}
              minLength={12}
              maxLength={72}
              required
            />
            <small>12–72 karakter; mevcut paroladan farklı olmalı.</small>
          </label>

          <label>
            <span>Yeni parola tekrarı</span>
            <PasswordInput
              autoComplete="new-password"
              value={confirmation}
              onChange={(event) => setConfirmation(event.target.value)}
              minLength={12}
              maxLength={72}
              required
            />
          </label>

          {error && <div className="form-alert" role="alert">{error}</div>}

          <button className="primary-button" type="submit" disabled={submitting}>
            {submitting ? "Kaydediliyor…" : "Parolayı güncelle"}
          </button>
        </form>

        <button className="text-button" type="button" onClick={handleLogout}>
          Farklı hesapla giriş yap
        </button>
      </section>
    </main>
  );
}
