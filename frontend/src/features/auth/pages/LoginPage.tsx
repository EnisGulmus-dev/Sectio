import { type FormEvent, useState } from "react";
import { useNavigate } from "react-router-dom";

import { isApiClientError } from "../../../shared/api/apiError";
import { useAuth } from "../useAuth";
import { PasswordInput } from "../../../shared/ui/PasswordInput";
import { useTheme } from "../../../shared/ui/useTheme";

export function LoginPage() {
  const navigate = useNavigate();
  const { login, startupError } = useAuth();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const { theme, toggleTheme } = useTheme();

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
    <main className="auth-layout" data-theme={theme}>
      <button className="login-theme-toggle" type="button" onClick={toggleTheme} aria-label={theme === "midnight" ? "Açık temaya geç" : "Koyu temaya geç"}>
        <span aria-hidden="true">{theme === "midnight" ? "☼" : "◐"}</span>
        {theme === "midnight" ? "Light" : "Dark"}
      </button>
      <section className="auth-story" aria-label="Sectio tanıtımı">
        <div className="login-brand-animation" key={theme} aria-label="Sectio">
          <img className="brand-wave-mark" src="/brand/04_Sadece_S_Amblemi.png" alt="" aria-hidden="true" />
          <img className="brand-word-reveal" src={theme === "midnight" ? "/brand/03_Beyaz_Negatif_Yatay.png" : "/brand/01_Ana_Renkli_Yatay.png"} alt="Sectio" />
        </div>

        <div className="auth-story-copy">
          <p className="eyebrow">Plan. Align. Deliver.</p>
          <h1>Work, made visible.</h1>
          <p>
            Keep projects, people, and progress moving in one focused workspace.
          </p>
        </div>

        <div className="story-metric" aria-label="Uygulama durumu">
          <span className="status-dot" />
          <span>Workspace ready</span>
        </div>
      </section>

      <section className="auth-panel">
        <div className="auth-card">
          <header>
            <p className="eyebrow">Welcome back</p>
            <h2>Sign in to Sectio</h2>
            <p>Bring your team’s next move into focus.</p>
          </header>

          <form onSubmit={handleSubmit} className="auth-form">
            <label>
              <span>Email address</span>
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
              <span>Password</span>
              <PasswordInput
                name="password"
                autoComplete="current-password"
                value={password}
                onChange={(event) => setPassword(event.target.value)}
                placeholder="Enter your password"
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
              {submitting ? "Signing in…" : "Sign in"}
            </button>
          </form>

          <footer className="auth-footer">
            <span className="lock-icon" aria-hidden="true">●</span>
            Protected by secure HTTP-only cookies.
          </footer>
        </div>
      </section>
    </main>
  );
}
