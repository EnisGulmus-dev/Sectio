import { type FormEvent, useState } from "react";
import { useNavigate } from "react-router-dom";

import { isApiClientError } from "../../../shared/api/apiError";
import { useAuth } from "../useAuth";

export function LoginPage() {
  const navigate = useNavigate();
  const { login, startupError } = useAuth();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError(null);
    setSubmitting(true);

    try {
      const user = await login({ email, password });
      navigate(user.mustChangePassword ? "/change-password" : "/", {
        replace: true,
      });
    } catch (caught) {
      if (isApiClientError(caught)) {
        setError(caught.message);
      } else {
        setError("Giriş yapılamadı. Bağlantıyı kontrol edip tekrar deneyin.");
      }
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <main className="auth-layout">
      <section className="auth-story" aria-label="Sectio tanıtımı">
        <div className="brand-lockup">
          <span className="brand-mark" aria-hidden="true">
            S
          </span>
          <span>Sectio</span>
        </div>

        <div className="auth-story-copy">
          <p className="eyebrow">Odaklı ekipler için</p>
          <h1>İşi görünür, ilerlemeyi anlaşılır kılın.</h1>
          <p>
            Projeleri, sorumlulukları ve teslim tarihlerini tek bir sakin çalışma
            alanında yönetin.
          </p>
        </div>

        <div className="story-metric" aria-label="Uygulama durumu">
          <span className="status-dot" />
          <span>Yerel çalışma alanı hazır</span>
        </div>
      </section>

      <section className="auth-panel">
        <div className="auth-card">
          <header>
            <p className="eyebrow">Tekrar hoş geldiniz</p>
            <h2>Hesabınıza giriş yapın</h2>
            <p>Devam etmek için yönetici veya çalışan hesabınızı kullanın.</p>
          </header>

          <form onSubmit={handleSubmit} className="auth-form">
            <label>
              <span>Email adresi</span>
              <input
                type="email"
                name="email"
                autoComplete="email"
                value={email}
                onChange={(event) => setEmail(event.target.value)}
                placeholder="ad@sectio.local"
                required
                autoFocus
              />
            </label>

            <label>
              <span>Parola</span>
              <input
                type="password"
                name="password"
                autoComplete="current-password"
                value={password}
                onChange={(event) => setPassword(event.target.value)}
                placeholder="Parolanızı girin"
                maxLength={72}
                required
              />
            </label>

            {(error ?? startupError) && (
              <div className="form-alert" role="alert">
                {error ?? startupError}
              </div>
            )}

            <button className="primary-button" type="submit" disabled={submitting}>
              {submitting ? "Giriş yapılıyor…" : "Giriş yap"}
            </button>
          </form>

          <footer className="auth-footer">
            <span className="lock-icon" aria-hidden="true">●</span>
            Oturumunuz güvenli HTTP cookie ile korunur.
          </footer>
        </div>
      </section>
    </main>
  );
}
