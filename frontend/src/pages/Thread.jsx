import { useEffect, useRef, useState, useCallback } from 'react';
import { Link, useParams } from 'react-router-dom';
import { api, dateTime } from '../api.js';
import { useAuth } from '../auth.jsx';
import { useRealtime } from '../realtime.jsx';
import { Alert, Empty } from '../components/Bits.jsx';

export default function Thread() {
  const { requestId } = useParams();
  const { user } = useAuth();
  const [msgs, setMsgs] = useState([]);
  const [body, setBody] = useState('');
  const [err, setErr] = useState('');
  const [needPass, setNeedPass] = useState(false);
  const end = useRef(null);
  const { subscribe, connected } = useRealtime();
  const add = (m) => setMsgs((list) => (list.some((x) => x.id === m.id) ? list : [...list, m]));

  const load = useCallback(() => api(`/requests/${requestId}/messages`).then(setMsgs).catch((e) => setErr(e.message)), [requestId]);
  // Bağlantı kurulunca (veya yeniden kurulunca) kaçan mesajları yükle; bağlantı yoksa 8 sn'de bir sorgula.
  useEffect(() => {
    load();
    if (connected) return undefined;
    const t = setInterval(load, 8000);
    return () => clearInterval(t);
  }, [load, connected]);
  useEffect(() => subscribe((ev) => {
    if (ev.type === 'message' && String(ev.requestId) === String(requestId)) add(ev.message);
  }), [subscribe, requestId]);
  useEffect(() => { end.current?.scrollIntoView({ block: 'end' }); }, [msgs.length]);

  const send = async (e) => {
    e.preventDefault(); setErr(''); setNeedPass(false);
    try { const m = await api(`/requests/${requestId}/messages`, { method: 'POST', body: { body } }); setBody(''); add(m); }
    catch (e2) { if (e2.status === 402) setNeedPass(true); else setErr(e2.message); }
  };

  return (
    <div className="wrap narrowish">
      <p><Link to="/talepler">← Taleplere dön</Link></p>
      <h1 className="h1s">Mesajlar</h1>
      <p className="muted small">{connected ? 'Canlı bağlantı açık' : 'Bağlanıyor… mesajlar birkaç saniyede bir güncellenir'}</p>
      <Alert>{err}</Alert>
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
      {needPass && (
        <Alert>Mesaj göndermek için Öğrenci Pass gerekli. <Link to="/pass">Pass satın al</Link></Alert>
      )}
      <form className="row" onSubmit={send}>
        <input required value={body} onChange={(e) => setBody(e.target.value)} placeholder="Mesajınızı yazın" maxLength={2000} />
        <button className="btn primary">Gönder</button>
      </form>
    </div>
  );
}
