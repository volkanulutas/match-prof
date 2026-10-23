import { useEffect, useRef, useState, useCallback } from 'react';
import { Link, useParams } from 'react-router-dom';
import { api, dateTime, MODES, REQUEST_STATUS } from '../api.js';
import { useAuth } from '../auth.jsx';
import { useRealtime } from '../realtime.jsx';
import { Alert, Empty, Status } from '../components/Bits.jsx';

function ScheduleForm({ requestId, onDone, onError }) {
  const [start, setStart] = useState('');
  const [minutes, setMinutes] = useState(60);
  const submit = async (e) => {
    e.preventDefault();
    try {
      const s = new Date(start);
      const end = new Date(s.getTime() + minutes * 60000);
      await api('/sessions', { method: 'POST', body: { requestId, startsAt: s.toISOString(), endsAt: end.toISOString() } });
      onDone();
    } catch (e2) { onError(e2.message); }
  };
  return (
    <form className="row wrapr" onSubmit={submit}>
      <label>Ders zamanı<input type="datetime-local" required value={start} onChange={(e) => setStart(e.target.value)} /></label>
      <label>Süre
        <select value={minutes} onChange={(e) => setMinutes(Number(e.target.value))}>
          <option value={30}>30 dk</option><option value={60}>60 dk</option><option value={90}>90 dk</option><option value={120}>120 dk</option>
        </select>
      </label>
      <button className="btn primary">Dersi planla</button>
    </form>
  );
}

/** Talep özeti + öğretmen onayı + sohbet: bildirimler doğrudan bu ekrana açılır. */
export default function Thread() {
  const { requestId } = useParams();
  const { user } = useAuth();
  const { subscribe, connected } = useRealtime();
  const [req, setReq] = useState(null);
  const [msgs, setMsgs] = useState([]);
  const [body, setBody] = useState('');
  const [err, setErr] = useState('');
  const [ok, setOk] = useState('');
  const [needPass, setNeedPass] = useState(false);
  const end = useRef(null);

  const add = (m) => setMsgs((list) => (list.some((x) => x.id === m.id) ? list : [...list, m]));
  const loadReq = useCallback(() => api(`/requests/${requestId}`).then(setReq).catch((e) => setErr(e.message)), [requestId]);
  const loadMsgs = useCallback(() => api(`/requests/${requestId}/messages`).then(setMsgs).catch((e) => setErr(e.message)), [requestId]);

  useEffect(() => { loadReq(); }, [loadReq]);
  useEffect(() => {
    loadMsgs();
    if (connected) return undefined;
    const t = setInterval(loadMsgs, 8000);
    return () => clearInterval(t);
  }, [loadMsgs, connected]);
  useEffect(() => subscribe((ev) => {
    if (ev.type === 'message' && String(ev.requestId) === String(requestId)) add(ev.message);
    if (ev.type === 'notification' && ev.link === `/mesajlar/${requestId}`) loadReq();
  }), [subscribe, requestId, loadReq]);
  useEffect(() => { end.current?.scrollIntoView({ block: 'end' }); }, [msgs.length]);

  const send = async (e) => {
    e.preventDefault(); setErr(''); setNeedPass(false);
    try { const m = await api(`/requests/${requestId}/messages`, { method: 'POST', body: { body } }); setBody(''); add(m); }
    catch (e2) { if (e2.status === 402) setNeedPass(true); else setErr(e2.message); }
  };

  const run = async (path, options, success) => {
    setErr(''); setOk('');
    try { await api(path, options); setOk(success); loadReq(); } catch (e) { setErr(e.message); }
  };

  if (!req) return <div className="wrap narrowish"><Alert>{err}</Alert>{!err && <p className="muted">Yükleniyor…</p>}</div>;

  const teacher = user.role === 'TEACHER';
  const open = req.status === 'PENDING' || req.status === 'ACCEPTED';
  const chosen = req.slots.find((s) => s.chosen);

  return (
    <div className="wrap narrowish">
      <p><Link to="/talepler">← Taleplere dön</Link></p>

      <section className="card stack">
        <div className="row between">
          <div>
            <Link to={`/ilan/${req.listingId}`}><strong>{req.listingTitle}</strong></Link>
            <div className="muted small">{teacher ? `Öğrenci: ${req.studentName}` : `Öğretmen: ${req.teacherName}`} · {MODES[req.mode]}{req.studentLevel ? ` · ${req.studentLevel}` : ''} · {dateTime(req.createdAt)}</div>
          </div>
          <Status value={req.status} labels={REQUEST_STATUS} />
        </div>

        {req.slots.length > 0 && (
          <div>
            <p className="small muted">{teacher ? 'Öğrencinin önerdiği zamanlar' : 'Önerdiğiniz zamanlar'}</p>
            <ul className="plain">
              {req.slots.map((s) => (
                <li key={s.id} className="row between slotrow">
                  <span>{dateTime(s.startsAt)} – {new Date(s.endsAt).toLocaleTimeString('tr-TR', { timeStyle: 'short' })}
                    {s.chosen && <span className="badge st-accepted"> Onaylandı</span>}</span>
                  {teacher && req.status === 'PENDING' && (
                    <button className="btn primary" onClick={() => run(`/requests/${req.id}/accept`, { method: 'POST', body: { slotId: s.id } }, 'Zaman onaylandı ve ders oluşturuldu.')}>Bu zamanı onayla</button>
                  )}
                </li>
              ))}
            </ul>
          </div>
        )}

        <Alert>{err}</Alert>
        <Alert kind="ok">{ok}</Alert>

        <div className="row wrapr">
          {teacher && req.status === 'PENDING' && (
            <>
              {req.slots.length === 0 && <button className="btn primary" onClick={() => run(`/requests/${req.id}/accept`, { method: 'POST', body: {} }, 'Talep kabul edildi.')}>Talebi kabul et</button>}
              {req.slots.length > 0 && <button className="btn ghost" onClick={() => run(`/requests/${req.id}/accept`, { method: 'POST', body: {} }, 'Talep kabul edildi; zamanı sohbetten netleştirebilirsiniz.')}>Zaman seçmeden kabul et</button>}
              <button className="btn ghost" onClick={() => run(`/requests/${req.id}/status`, { method: 'PATCH', body: { status: 'DECLINED' } }, 'Talep reddedildi.')}>Reddet</button>
            </>
          )}
          {!teacher && open && (
            <button className="btn ghost" onClick={() => run(`/requests/${req.id}/status`, { method: 'PATCH', body: { status: 'CANCELLED' } }, 'Talep iptal edildi.')}>Talebi iptal et</button>
          )}
          {chosen && <Link className="btn ghost" to="/dersler">Derslerime git</Link>}
        </div>

        {teacher && req.status === 'ACCEPTED' && !chosen && (
          <div>
            <p className="small muted">Henüz ders planlanmadı. Öğrenciyle konuştuğunuz zamanı girin:</p>
            <ScheduleForm requestId={req.id} onError={setErr} onDone={() => { setOk('Ders planlandı.'); loadReq(); }} />
          </div>
        )}
      </section>

      <h2>Sohbet</h2>
      <p className="muted small">{connected ? 'Canlı bağlantı açık' : 'Bağlanıyor… mesajlar birkaç saniyede bir güncellenir'}</p>
      <div className="thread card">
        {msgs.length === 0 && <Empty>Henüz mesaj yok. İlk mesajı siz yazın.</Empty>}
        {msgs.map((m) => (
          <div key={m.id} className={'bubble' + (m.senderId === user.id ? ' mine' : '')}>
            <div className="small muted">{m.senderName} · {dateTime(m.createdAt)}</div>
            <div>{m.body}</div>
          </div>
        ))}
        <div ref={end} />
      </div>
      {needPass && <Alert>Mesaj göndermek için Öğrenci Pass gerekli. <Link to="/pass">Pass satın al</Link></Alert>}
      {open ? (
        <form className="row" onSubmit={send}>
          <input required value={body} onChange={(e) => setBody(e.target.value)} placeholder="Mesajınızı yazın" maxLength={2000} />
          <button className="btn primary">Gönder</button>
        </form>
      ) : <p className="muted small">Bu talep kapandığı için yeni mesaj yazılamaz.</p>}
    </div>
  );
}
