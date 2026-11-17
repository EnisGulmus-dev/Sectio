import { useState } from "react";
import type { ProjectMemberCandidate } from "../types";
import { jobTitleLabels } from "../../auth/jobTitles";
import styles from "./ProjectMembersPicker.module.css";

export function ProjectMembersPicker({ candidates, selected, onChange, disabled = false }: {
  candidates: ProjectMemberCandidate[]; selected: number[]; onChange: (ids: number[]) => void; disabled?: boolean;
}) {
  const [query, setQuery] = useState("");
  const normalized = query.trim().toLocaleLowerCase("tr-TR");
  const visible = candidates.filter((person) => `${person.name} ${person.email}`.toLocaleLowerCase("tr-TR").includes(normalized));
  return <fieldset className={`full-field ${styles.picker}`} disabled={disabled}>
    <legend>Projede çalışacak kişiler</legend>
    <div className={styles.toolbar}><input type="search" aria-label="Projeye eklenecek kişiyi ara" placeholder="Ad veya e-posta ile ara" value={query} onChange={(event) => setQuery(event.target.value)} /><span>{selected.length} kişi seçildi</span></div>
    <div className={styles.people}>{visible.length === 0 ? <p className={styles.empty}>Eşleşen kişi yok.</p> : visible.map((person) => <label className={styles.person} key={person.id}>
      <input type="checkbox" checked={selected.includes(person.id)} onChange={() => onChange(selected.includes(person.id) ? selected.filter((id) => id !== person.id) : [...selected, person.id])} />
      <span><strong>{person.name}</strong><small>{jobTitleLabels[person.jobTitle]} · {person.email}</small></span>
    </label>)}</div>
    <p className={styles.help}>Proje sahibi otomatik eklenir. Kaydedilen kişilerle takımları proje içindeki Takımlar ve üyeler bölümünde oluşturabilirsiniz.</p>
  </fieldset>;
}
