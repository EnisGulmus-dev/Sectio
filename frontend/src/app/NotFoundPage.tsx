import { Link } from "react-router-dom";

export function NotFoundPage() {
  return (
    <section className="not-found panel">
      <p className="eyebrow">404</p>
      <h1>Sayfa bulunamadı</h1>
      <p>Aradığınız adres taşınmış veya henüz oluşturulmamış olabilir.</p>
      <Link className="primary-link" to="/">Genel bakışa dön</Link>
    </section>
  );
}
