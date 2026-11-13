import { Component, type ErrorInfo, type ReactNode } from "react";

type State = { failed: boolean };

export class ErrorBoundary extends Component<{ children: ReactNode }, State> {
  state: State = { failed: false };

  static getDerivedStateFromError(): State {
    return { failed: true };
  }

  componentDidCatch(error: Error, info: ErrorInfo) {
    console.error("Unhandled application error", error, info.componentStack);
  }

  render() {
    if (this.state.failed) {
      return (
        <main className="centered-page">
          <section className="error-card">
            <p className="eyebrow">Beklenmeyen hata</p>
            <h1>Sayfa görüntülenemedi</h1>
            <p>Uygulamayı yenileyin. Sorun devam ederse backend kayıtlarını kontrol edin.</p>
            <button className="primary-button" type="button" onClick={() => window.location.assign("/")}>
              Ana sayfaya dön
            </button>
          </section>
        </main>
      );
    }

    return this.props.children;
  }
}
