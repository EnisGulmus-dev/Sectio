import { avatarGroups, avatarUrl } from "./avatarCatalog";
import styles from "./AvatarPicker.module.css";

export function AvatarPicker({ value, onChange }: { value: string | null; onChange: (id: string | null) => void }) {
  return <fieldset className={styles.picker}>
    <legend>Hazır avatar seç</legend>
    <button type="button" className={styles.initials} aria-pressed={value === null} onClick={() => onChange(null)}>İsim baş harfi</button>
    {avatarGroups.map((group) => <section key={group.label}>
      <h3>{group.label}</h3><div className={styles.grid}>{group.items.map((id, index) => <button type="button" key={id} className={styles.option} aria-label={`${group.label} avatar ${index + 1}`} aria-pressed={value === id} onClick={() => onChange(id)}><img src={avatarUrl(id)!} alt="" loading="lazy" /></button>)}</div>
    </section>)}
  </fieldset>;
}
