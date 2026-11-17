export const avatarGroups = [
  { label: "Erkek", items: ["erkek_01_sakalsiz", "erkek_02_hafif_sakal", "erkek_03_biyik_top_sakal", "erkek_04_kel_gur_sakal", "erkek_05_kir_sac_sakal"] },
  { label: "Kadın", items: ["kadin_01_uzun_siyah_sac", "kadin_02_dalgali_kestane", "kadin_03_kisa_kivircik", "kadin_04_sarisin_topuz", "kadin_05_kisa_kizil"] },
] as const;

export function avatarUrl(id: string): string | null {
  return avatarGroups.some((group) => (group.items as readonly string[]).includes(id)) ? `/avatars/${id}.png` : null;
}
