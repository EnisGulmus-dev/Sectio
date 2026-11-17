import { useEffect, useState, type FormEvent } from "react";
import { isApiClientError } from "../../shared/api/apiError";
import { InlineAlert, LoadingState } from "../../shared/ui/Feedback";
import { ConfirmDialog } from "../../shared/ui/ConfirmDialog";
import { ModalPanel } from "../../shared/ui/ModalPanel";
import { useToast } from "../../shared/ui/ToastProvider";
import { useAuth } from "../auth/useAuth";
import { usersApi } from "../users/api/usersApi";
import { organizationApi, type CompanyTeam, type OrganizationPerson, type Relationship } from "./organizationApi";
import styles from "./OrganizationPage.module.css";

export function OrganizationPage() {
  const { user } = useAuth(); const admin = user?.role === "ADMIN";
  const [teams, setTeams] = useState<CompanyTeam[]>([]); const [people, setPeople] = useState<OrganizationPerson[]>([]); const [managers, setManagers] = useState<OrganizationPerson[]>([]);
  const [selected, setSelected] = useState<Relationship | null>(null); const [managerId, setManagerId] = useState(""); const [selectedTeams, setSelectedTeams] = useState<number[]>([]);
  const [teamName, setTeamName] = useState(""); const [renaming, setRenaming] = useState<number | null>(null); const [deleteTeam, setDeleteTeam] = useState<CompanyTeam | null>(null);
  const [teamDialog, setTeamDialog] = useState(false); const [teamMembers, setTeamMembers] = useState<number[]>([]); const [memberQuery, setMemberQuery] = useState("");
  const [loading, setLoading] = useState(true); const [busy, setBusy] = useState(false); const [error, setError] = useState<string | null>(null);
  const { showToast } = useToast();
  function fail(e: unknown) { setError(isApiClientError(e) ? e.message : "İşlem tamamlanamadı."); }
  async function load() {
    const [t, m, p] = await Promise.all([organizationApi.teams(), organizationApi.managers(), admin ? usersApi.list(false) : organizationApi.reports()]);
    setTeams(t); setManagers(m); setPeople(p.filter((person) => !("active" in person) || person.active).map((person) => ({id:person.id, name:person.name, jobTitle:person.jobTitle})));
  }
  useEffect(() => { let live = true; setLoading(true); load().catch((e) => { if (live) fail(e); }).finally(() => { if (live) setLoading(false); }); return () => { live = false; }; }, [user?.id, admin]);
  async function run(action: () => Promise<void>) { if (busy) return; setBusy(true); setError(null); try { await action(); } catch (e) { fail(e); } finally { setBusy(false); } }
  async function pick(id:number) { await run(async () => { const value = await organizationApi.get(id); setSelected(value); setManagerId(value.managerId ? String(value.managerId) : ""); setSelectedTeams(value.teams.map((t) => t.id)); }); }
  async function save(event: FormEvent) { event.preventDefault(); if (!selected) return; await run(async () => { const result = await organizationApi.assign(selected.userId, managerId ? Number(managerId) : null, selectedTeams); setSelected(result); showToast("Organizasyon bilgileri güncellendi."); }); }
  async function saveTeam(event: FormEvent) { event.preventDefault(); await run(async () => { if (renaming) await organizationApi.renameTeam(renaming, teamName); else await organizationApi.createTeam(teamName, teamMembers); setTeamName(""); setRenaming(null); setTeamMembers([]); setMemberQuery(""); setTeamDialog(false); await load(); showToast(renaming ? "Takım güncellendi." : "Takım oluşturuldu."); }); }
  if (loading) return <LoadingState />;
  return <div className="page-stack"><header className="page-header"><div><p className="eyebrow">Şirket</p><h1>Organizasyon</h1><p>Şirket takımları ve doğrudan bağlılıklar.</p></div></header>{error && <InlineAlert message={error} />}
    <div className={styles.columns}>{admin && <section className="panel"><div className={styles.sectionHead}><h2>Şirket takımları</h2><button className="primary-button compact-button" type="button" onClick={() => { setRenaming(null); setTeamName(""); setTeamMembers([]); setMemberQuery(""); setTeamDialog(true); }}>+ Takım oluştur</button></div>{teams.length === 0 && <p>Henüz şirket takımı yok.</p>}{teams.map((team) => <div key={team.id} className={styles.teamCard}><strong>{team.name}</strong><div className={styles.teamActions}><button className="secondary-button" type="button" onClick={() => {setRenaming(team.id);setTeamName(team.name);setTeamDialog(true);}}>Düzenle</button><button className={styles.deleteAction} type="button" onClick={() => setDeleteTeam(team)}>Sil</button></div></div>)}</section>}
      <section className="panel"><h2>{admin ? "Çalışanlar" : "Doğrudan bağlı çalışanlar"}</h2>{people.length === 0 && <p>Gösterilecek çalışan yok.</p>}{people.map((person) => <button type="button" className={styles.person} key={person.id} onClick={() => void pick(person.id)}>{person.name}</button>)}</section>
      {selected && <section className="panel"><h2>{people.find((p) => p.id === selected.userId)?.name ?? "Çalışan"}</h2><form onSubmit={save} className={styles.form}><label>Bağlı yönetici<select value={managerId} disabled={!admin || busy} onChange={(e) => setManagerId(e.target.value)}><option value="">Belirlenmemiş</option>{managers.filter((p) => p.id !== selected.userId).map((person) => <option key={person.id} value={person.id}>{person.name}</option>)}</select></label><fieldset className={styles.teams}><legend>Şirket takımları</legend>{teams.map((team) => <label key={team.id}><input type="checkbox" checked={selectedTeams.includes(team.id)} onChange={() => setSelectedTeams((current) => current.includes(team.id) ? current.filter((id) => id !== team.id) : [...current, team.id])} />{team.name}</label>)}</fieldset><button className="primary-button compact-button" disabled={busy}>Kaydet</button></form></section>}</div>
    <ModalPanel open={teamDialog} title={renaming ? "Takımı düzenle" : "Şirket takımı oluştur"} onClose={() => { if (!busy) setTeamDialog(false); }}><form onSubmit={saveTeam} className={styles.form}><label>Takım adı<input required maxLength={100} value={teamName} onChange={(e) => setTeamName(e.target.value)} /></label>{!renaming && <fieldset className={styles.memberPicker}><legend>Takım üyeleri</legend><input aria-label="Çalışan ara" placeholder="Çalışan ara" value={memberQuery} onChange={(e) => setMemberQuery(e.target.value)} /><p>{teamMembers.length} kişi seçildi</p><div>{people.filter((person) => person.name.toLocaleLowerCase("tr-TR").includes(memberQuery.trim().toLocaleLowerCase("tr-TR"))).map((person) => <label key={person.id}><input type="checkbox" checked={teamMembers.includes(person.id)} onChange={() => setTeamMembers((current) => current.includes(person.id) ? current.filter((id) => id !== person.id) : [...current, person.id])} /><span>{person.name}</span></label>)}</div></fieldset>}{error && <InlineAlert message={error} />}<button className="primary-button compact-button" disabled={busy}>{renaming ? "Değişiklikleri kaydet" : "Takımı oluştur"}</button></form></ModalPanel>
    <ConfirmDialog open={!!deleteTeam} title="Şirket takımı silinsin mi?" description={`${deleteTeam?.name ?? "Takım"} üyelikleri kaldırılacak; çalışanlar ve proje görevleri korunacak.`} confirmLabel="Takımı sil" busy={busy} onCancel={() => setDeleteTeam(null)} onConfirm={() => deleteTeam && void run(async () => { await organizationApi.deleteTeam(deleteTeam.id); setDeleteTeam(null); setSelected(null); await load(); showToast("Takım silindi."); })} />
  </div>;
}
