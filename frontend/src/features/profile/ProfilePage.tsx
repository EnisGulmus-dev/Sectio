import { useEffect, useState, type FormEvent } from "react";
import { isApiClientError } from "../../shared/api/apiError";
import { InlineAlert, LoadingState } from "../../shared/ui/Feedback";
import { useToast } from "../../shared/ui/ToastProvider";
import { jobTitleLabels } from "../auth/jobTitles";
import { useAuth } from "../auth/useAuth";
import { organizationApi, type Relationship } from "../organization/organizationApi";
import { AvatarPicker } from "./AvatarPicker";
import { avatarUrl } from "./avatarCatalog";
import { profileApi, type Profile } from "./profileApi";
import styles from "./ProfilePage.module.css";

type Mode = "KEEP" | "INITIALS" | "AVATAR" | "PHOTO";
export function ProfilePage() {
  const { user } = useAuth();
  const [profile, setProfile] = useState<Profile | null>(null);
  const [relationship, setRelationship] = useState<Relationship | null>(null);
  const [form, setForm] = useState({ displayName: "", biography: "", timeZone: "" });
  const [mode, setMode] = useState<Mode>("KEEP");
  const [avatar, setAvatar] = useState<string | null>(null);
  const [file, setFile] = useState<File | undefined>();
  const [preview, setPreview] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [photoVersion, setPhotoVersion] = useState(0);
  const { showToast } = useToast();
  useEffect(() => {
    let live = true;
    Promise.all([profileApi.get(), user ? organizationApi.get(user.id) : Promise.resolve(null)]).then(([p, r]) => {
      if (live) { setProfile(p); setRelationship(r); setAvatar(p.avatarId); setForm({ displayName: p.displayName ?? "", biography: p.biography ?? "", timeZone: p.timeZone ?? "" }); }
    }).catch((e) => { if (live) setError(isApiClientError(e) ? e.message : "Profil yüklenemedi."); }).finally(() => { if (live) setLoading(false); });
    return () => { live = false; };
  }, [user?.id]);
  useEffect(() => { if (!file) { setPreview(null); return; } const url = URL.createObjectURL(file); setPreview(url); return () => URL.revokeObjectURL(url); }, [file]);
  async function save(event: FormEvent) {
    event.preventDefault(); if (busy) return; setBusy(true); setError(null);
    try {
      const result = await profileApi.commit({ ...form, imageMode: mode, avatarId: mode === "AVATAR" ? avatar : null }, mode === "PHOTO" ? file : undefined);
      setProfile(result); setAvatar(result.avatarId); setMode("KEEP"); setFile(undefined); setPhotoVersion((v) => v + 1);
      window.dispatchEvent(new Event("sectio-profile-updated")); showToast("Profil güncellendi.");
    } catch (e) { setError(isApiClientError(e) ? e.message : "Profil güncellenemedi."); }
    finally { setBusy(false); }
  }
  if (loading) return <LoadingState />;
  if (!profile) return <InlineAlert message={error ?? "Profil yüklenemedi."} />;
  const picture = mode === "PHOTO" ? preview : mode === "AVATAR" && avatar ? avatarUrl(avatar) : mode === "INITIALS" ? null : profile.hasPhoto ? `/api/v1/profile/${profile.userId}/photo?v=${photoVersion}` : profile.avatarId ? avatarUrl(profile.avatarId) : null;
  const chosen = mode === "INITIALS" ? null : mode === "PHOTO" || (mode === "KEEP" && profile.hasPhoto) ? "photo" : avatar;
  return <div className="page-stack"><header className="page-header"><div><p className="eyebrow">Hesabım</p><h1>Profilim</h1><p>İletişimde görünecek bilgilerini ve profil resmini seç.</p></div></header>
    {error && <InlineAlert message={error} />}
    <form className={`panel ${styles.card}`} onSubmit={save}>
      <div className={styles.identity}><div className={styles.avatar}>{picture ? <img src={picture} alt="" /> : profile.name.slice(0, 1).toLocaleUpperCase("tr-TR")}</div><div><h2>{profile.name}</h2><p>{jobTitleLabels[profile.jobTitle]}</p></div></div>
      <section className={styles.section}><div><h2>Profil bilgileri</h2><p>İletişim alanlarında görünecek bilgilerini düzenle.</p></div><div className="management-form"><label>Görünen ad<input maxLength={100} value={form.displayName} onChange={(e) => setForm({ ...form, displayName: e.target.value })} /></label><label className="full-field">Hakkımda<textarea maxLength={500} rows={3} value={form.biography} onChange={(e) => setForm({ ...form, biography: e.target.value })} /></label></div></section>
      <section className={styles.section}><div><h2>Organizasyon</h2><p>Şirket içindeki rol ve bağlılıkların.</p></div><dl className={styles.details}><div><dt>Rol</dt><dd>{user?.role === "ADMIN" ? "Yönetici" : "Çalışan"}</dd></div><div><dt>Unvan</dt><dd>{jobTitleLabels[profile.jobTitle]}</dd></div><div><dt>Bağlı yönetici</dt><dd>{relationship?.managerName ?? "Belirlenmemiş"}</dd></div><div><dt>Şirket takımları</dt><dd>{relationship?.teams.map((t) => t.name).join(", ") || "Belirlenmemiş"}</dd></div></dl></section>
      <section className={styles.section}><div><h2>Profil resmi</h2><p>PNG veya JPEG, en fazla 2 MB.</p></div><div className={styles.photoActions}><label className={styles.upload}>Fotoğraf seç<input type="file" accept="image/png,image/jpeg" disabled={busy} aria-label="Fotoğraf yükle" onChange={(e) => { const next = e.target.files?.[0]; e.target.value = ""; if (next) { setFile(next); setMode("PHOTO"); } }} /></label>{(profile.hasPhoto || mode === "PHOTO") && <button className="secondary-button" type="button" disabled={busy} onClick={() => { setFile(undefined); setAvatar(null); setMode("INITIALS"); }}>Fotoğrafı kaldır</button>}{file && <span>{file.name}</span>}</div></section>
      <AvatarPicker value={chosen} onChange={(id) => { setAvatar(id); setFile(undefined); setMode(id ? "AVATAR" : "INITIALS"); }} />
      <div><button className="primary-button compact-button" disabled={busy} type="submit">{busy ? "Güncelleniyor…" : "Güncelle"}</button></div>
    </form></div>;
}
