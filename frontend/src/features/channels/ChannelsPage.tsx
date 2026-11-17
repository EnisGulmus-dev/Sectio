import { useEffect, useLayoutEffect, useRef, useState, type FormEvent } from "react";
import { useSearchParams } from "react-router-dom";
import { isApiClientError } from "../../shared/api/apiError";
import { InlineAlert } from "../../shared/ui/Feedback";
import { ConfirmDialog } from "../../shared/ui/ConfirmDialog";
import { ModalPanel } from "../../shared/ui/ModalPanel";
import { useAuth } from "../auth/useAuth";
import { canCreateProjects } from "../auth/jobTitles";
import { avatarUrl } from "../profile/avatarCatalog";
import { channelsApi, type Candidate, type Channel, type ChannelMember, type Message } from "./channelsApi";
import styles from "./ChannelsPage.module.css";

export function ChannelsPage() {
  const { user } = useAuth();
  const [params, setParams] = useSearchParams();
  const selectedId = Number(params.get("channel")) || null;
  const [channels, setChannels] = useState<Channel[]>([]);
  const [messages, setMessages] = useState<Message[]>([]);
  const latest = useRef<Message[]>([]);
  const request = useRef(0);
  const [members, setMembers] = useState<ChannelMember[]>([]);
  const [candidates, setCandidates] = useState<Candidate[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [live, setLive] = useState("Bağlanıyor");
  const [draft, setDraft] = useState("");
  const [editing, setEditing] = useState<number | null>(null);
  const [editText, setEditText] = useState("");
  const [newChannel, setNewChannel] = useState(false);
  const [showMembers, setShowMembers] = useState(false);
  const [deleteChannel, setDeleteChannel] = useState(false);
  const [deleteMessage, setDeleteMessage] = useState<number | null>(null);
  const [form, setForm] = useState<{ name:string; description:string; visibility:"PUBLIC"|"PRIVATE" }>({name:"",description:"",visibility:"PUBLIC"});
  const selected = channels.find((channel) => channel.id === selectedId);
  const owner = !!selected && selected.ownerId === user?.id;
  const canCreate = !!user && canCreateProjects(user.role, user.jobTitle);
  const fail = (e: unknown) => setError(isApiClientError(e) ? e.message : "İşlem tamamlanamadı.");
  async function loadChannels() { try { setChannels(await channelsApi.list()); } catch (e) { fail(e); } }
  useEffect(() => { void loadChannels(); }, [user?.id]);
  function updateMessages(items: Message[]) { latest.current = items; setMessages(items); }
  async function refresh(id: number, full = false) {
    const current = ++request.current;
    try {
      let batch = await channelsApi.messages(id); const all = [...batch];
      const oldest = full && latest.current.length ? Math.min(...latest.current.map((m) => m.id)) : null;
      while (oldest !== null && batch.length === 50 && batch[batch.length - 1].id > oldest) {
        batch = await channelsApi.messages(id, batch[batch.length - 1].id); all.push(...batch);
      }
      if (current === request.current) { updateMessages(all); setError(null); }
    } catch (e) { if (current === request.current) { updateMessages([]); fail(e); } }
  }
  useLayoutEffect(() => {
    request.current++; updateMessages([]); setMembers([]); setCandidates([]); setDraft(""); setEditing(null);
    if (!selectedId || !selected?.member) { setLive(""); return; }
    let active = true; let timer: number | undefined; let socket: WebSocket | undefined; let delay = 1000;
    const connect = () => {
      if (!active) return;
      setLive("Bağlanıyor");
      const scheme = location.protocol === "https:" ? "wss:" : "ws:";
      socket = new WebSocket(`${scheme}//${location.host}/api/v1/channels/${selectedId}/live`);
      socket.onopen = () => { if (active) { delay = 1000; setLive("Canlı"); void refresh(selectedId, true); } };
      socket.onmessage = () => { if (active) void refresh(selectedId, true); };
      socket.onclose = (event) => { if (!active) return; setLive(event.code === 1008 ? "Erişim sonlandırıldı" : "Bağlantı kesildi, yeniden deneniyor"); if (event.code === 1008) { updateMessages([]); void loadChannels(); return; } timer = window.setTimeout(connect, delay); delay = Math.min(delay * 2, 15000); };
      socket.onerror = () => socket?.close();
    };
    void refresh(selectedId); connect();
    return () => { active = false; request.current++; if (timer) clearTimeout(timer); socket?.close(); };
  }, [selectedId, selected?.member, user?.id]);
  async function perform(action: () => Promise<unknown>, after?: () => Promise<void>) { if (busy) return; setBusy(true); setError(null); try { await action(); if (after) await after(); } catch (e) { fail(e); } finally { setBusy(false); } }
  async function loadPeople() { if (!selectedId) return; const [list, options] = await Promise.all([channelsApi.members(selectedId), owner ? channelsApi.candidates(selectedId) : Promise.resolve([])]); setMembers(list); setCandidates(options); }
  async function create(event: FormEvent) { event.preventDefault(); await perform(async () => { const channel = await channelsApi.create(form); setNewChannel(false); setForm({name:"",description:"",visibility:"PUBLIC"}); await loadChannels(); setParams({channel:String(channel.id)}); }); }
  async function send(event: FormEvent) { event.preventDefault(); if (!selectedId || !draft.trim()) return; const body = draft; await perform(async () => { await channelsApi.send(selectedId, body); setDraft(""); await refresh(selectedId, true); }); }
  return <div className="page-stack"><header className="page-header"><div><p className="eyebrow">İletişim</p><h1>Kanallar</h1><p>Ekibinle aynı yerde konuş ve gelişmeleri takip et.</p></div>{canCreate && <button className="primary-button compact-button" onClick={() => setNewChannel(true)}>Kanal oluştur</button>}</header>
    {error && <InlineAlert message={error} />}
    <div className={styles.workspace}><aside className={`panel ${styles.sidebar}`}><h2>Kanallar</h2>{channels.length === 0 && <p>Henüz kanal yok.</p>}{channels.map((channel) => <button key={channel.id} className={styles.channel} aria-current={selectedId === channel.id ? "page" : undefined} onClick={() => setParams({channel:String(channel.id)})}><span>{channel.visibility === "PRIVATE" ? "🔒" : "#"} {channel.name}</span><small>{channel.member ? "Üye" : "Açık"}</small></button>)}</aside>
      <section className={`panel ${styles.conversation}`}>{!selected ? <div className={styles.empty}><h2>Bir kanal seç</h2><p>Sohbete başlamak için soldaki kanallardan birini aç.</p></div> : <><header className={styles.header}><div><h2>{selected.visibility === "PRIVATE" ? "🔒" : "#"} {selected.name}</h2><p>{selected.description || "Kanal açıklaması yok"}</p></div>{selected.member && <div className={styles.actions}><span className={styles.connection}>{live}</span><button className="secondary-button" onClick={() => { setShowMembers(true); void perform(loadPeople); }}>Üyeler</button>{owner && <button className="text-button" onClick={() => setDeleteChannel(true)}>Kanalı sil</button>}{!owner && <button className="text-button" onClick={() => void perform(() => channelsApi.remove(selected.id, user!.id), loadChannels)}>Ayrıl</button>}</div>}</header>
        {!selected.member ? <div className={styles.empty}><p>Bu açık kanaldaki mesajları görmek için katıl.</p><button className="primary-button compact-button" disabled={busy} onClick={() => void perform(() => channelsApi.join(selected.id), loadChannels)}>Kanala katıl</button></div> : <><div className={styles.history} aria-label="Mesaj geçmişi"><button className="text-button neutral" type="button" disabled={busy || messages.length === 0} onClick={() => void perform(async () => { const older = await channelsApi.messages(selected.id, messages[messages.length - 1].id); updateMessages([...latest.current, ...older.filter((item) => !latest.current.some((m) => m.id === item.id))]); })}>Önceki mesajları yükle</button>{messages.length === 0 && <p>Henüz mesaj yok. İlk mesajı sen yaz.</p>}{[...messages].reverse().map((m) => m.eventType ? <div className={styles.systemEvent} key={m.id}>{m.eventType === "MEMBER_LEFT" ? `${m.subjectName ?? "Bir kullanıcı"} kanaldan ayrıldı.` : `${m.subjectName ?? "Bir kullanıcı"}, ${m.authorName} tarafından kanaldan çıkarıldı.`}</div> : <article className={styles.message} key={m.id}><div className={styles.avatar}>{m.hasPhoto ? <img src={`/api/v1/profile/${m.authorId}/photo`} alt="" /> : m.avatarId && avatarUrl(m.avatarId) ? <img src={avatarUrl(m.avatarId)!} alt="" /> : m.authorName.slice(0,1).toLocaleUpperCase("tr-TR")}</div><div className={styles.messageBody}><div className={styles.messageHead}><strong>{m.displayName || m.authorName}</strong><time dateTime={m.createdAt}>{new Date(m.createdAt).toLocaleString("tr-TR")}</time>{m.editedAt && !m.deleted && <small>düzenlendi</small>}</div>{editing === m.id ? <form onSubmit={(e) => { e.preventDefault(); void perform(async () => { await channelsApi.edit(selected.id,m.id,editText); setEditing(null); await refresh(selected.id,true); }); }}><textarea maxLength={4000} value={editText} onChange={(e) => setEditText(e.target.value)} /><div className={styles.actions}><button className="primary-button compact-button" disabled={busy || !editText.trim()}>Kaydet</button><button className="secondary-button" type="button" onClick={() => setEditing(null)}>Vazgeç</button></div></form> : <p>{m.deleted ? <em>Mesaj silindi</em> : m.body}</p>}{!m.deleted && m.authorId === user?.id && editing !== m.id && <div className={styles.actions}><button className="text-button neutral" onClick={() => {setEditing(m.id);setEditText(m.body ?? "");}}>Düzenle</button><button className="text-button" onClick={() => setDeleteMessage(m.id)}>Sil</button></div>}</div></article>)}</div><form className={styles.composer} onSubmit={send}><label className="sr-only" htmlFor="channel-message">Mesaj</label><textarea id="channel-message" rows={2} maxLength={4000} placeholder="Mesaj yaz…" value={draft} onChange={(e) => setDraft(e.target.value)} /><button className="primary-button compact-button" disabled={busy || !draft.trim()}>Gönder</button></form></>}</>}</section></div>
    <ModalPanel open={newChannel} title="Kanal oluştur" onClose={() => setNewChannel(false)}><form className="management-form" onSubmit={create}><label>Kanal adı<input required maxLength={100} value={form.name} onChange={(e) => setForm({...form,name:e.target.value})} /></label><label>Görünürlük<select value={form.visibility} onChange={(e) => setForm({...form,visibility:e.target.value as "PUBLIC"|"PRIVATE"})}><option value="PUBLIC">Açık</option><option value="PRIVATE">Özel</option></select></label><label className="full-field">Açıklama<textarea maxLength={500} value={form.description} onChange={(e) => setForm({...form,description:e.target.value})} /></label><button className="primary-button compact-button" disabled={busy}>Oluştur</button></form></ModalPanel>
    <ModalPanel open={showMembers} title="Kanal üyeleri" onClose={() => setShowMembers(false)}><div className={styles.people}>{members.map((member) => <div key={member.userId}><span>{member.name}{member.userId === selected?.ownerId ? " · Sahip" : ""}</span>{owner && member.userId !== user?.id && <span className={styles.actions}><button className="text-button neutral" disabled={busy} onClick={() => selectedId && void perform(() => channelsApi.transfer(selectedId,member.userId), async () => { await loadChannels(); setShowMembers(false); })}>Sahipliği devret</button><button className="text-button" disabled={busy} onClick={() => selectedId && void perform(() => channelsApi.remove(selectedId,member.userId),loadPeople)}>Çıkar</button></span>}</div>)}{owner && <><h3>Üye ekle</h3>{candidates.map((candidate) => <div key={candidate.userId}><span>{candidate.name}</span><button className="secondary-button" disabled={busy} onClick={() => selectedId && void perform(() => channelsApi.add(selectedId,candidate.userId),loadPeople)}>Ekle</button></div>)}</>}</div></ModalPanel>
    <ConfirmDialog open={deleteChannel} title="Kanal silinsin mi?" description="Kanal ve tüm mesajları kalıcı olarak silinecek." confirmLabel="Kanalı sil" busy={busy} onCancel={() => setDeleteChannel(false)} onConfirm={() => selectedId && void perform(async () => { await channelsApi.delete(selectedId); setDeleteChannel(false); setParams({}); await loadChannels(); })} />
    <ConfirmDialog open={deleteMessage !== null} title="Mesaj silinsin mi?" description="Mesaj içeriği silinecek; geçmişte silinmiş mesaj işareti kalacak." confirmLabel="Mesajı sil" busy={busy} onCancel={() => setDeleteMessage(null)} onConfirm={() => selectedId && deleteMessage && void perform(async () => { await channelsApi.deleteMessage(selectedId,deleteMessage); setDeleteMessage(null); await refresh(selectedId,true); })} />
  </div>;
}
