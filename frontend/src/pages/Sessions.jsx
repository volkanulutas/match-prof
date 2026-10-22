import { useEffect, useState, useCallback } from 'react';
import { api, dateTime, SESSION_STATUS } from '../api.js';
import { useAuth } from '../auth.jsx';
import { useRealtime } from '../realtime.jsx';
import { Alert, Empty, Status } from '../components/Bits.jsx';

function ReviewForm({ session, onDone, onError }) {
  const [rating, setRating] = useState(5);
  const [body, setBody] = useState('');
  const submit = async (e) => {
    e.preventDefault();
    try { await api('/reviews', { method: 'POST', body: { sessionId: session.id, rating, body } }); onDone(); }
    catch (e2) { onError(e2.message); }
  };
  return (
    <form className="stack" onSubmit={submit}>
      <label>Puanınız
        <select value={rating} onChange={(e) => setRating(Number(e.target.value))}>
          {[5, 4, 3, 2, 1].map((n) => <option key={n} value={n}>{'★'.repeat(n)} ({n})</option>)}
        </select>
      </label>
      <label>Yorumunuz<textarea value={body} onChange={(e) => setBody(e.target.value)} /></label>
      <button className="btn primary">Yorumu yayınla</button>
    </form>
  );
}

export default function Sessions() {
  const { user } = useAuth();
  const [list, setList] = useState(null);
  const [err, setErr] = useState('');
  const [open, setOpen] = useState(null);
  const load = useCallback(() => api('/sessions/mine').then(setList).catch((e) => setErr(e.message)), []);
  useEffect(() => { load(); }, [load]);
  const { subscribe } = useRealtime();
  useEffect(() => subscribe((ev) => { if (ev.type === 'notification') load(); }), [subscribe, load]);

  const act = async (id, status) => {
    setErr('');
    try { await api(`/sessions/${id}/status`, { method: 'PATCH', body: { status } }); load(); }
    catch (e) { setErr(e.message); }
  };
  const teacher = user.role === 'TEACHER';

  return (
    <div className="wrap">
      <h1 className="h1s">Derslerim</h1>
      <Alert>{err}</Alert>
      {list?.length === 0 && <Empty>Planlanmış ders yok. Öğretmen talebinizi kabul edip bir zaman belirlediğinde dersler burada görünür.</Empty>}
      <div className="stackl">
        {list?.map((s) => {
          const started = new Date(s.startsAt) <= new Date();
          return (
            <div key={s.id} className="card">
              <div className="row between">
                <div>
                  <strong>{s.listingTitle}</strong>
                  <div className="muted small">{teacher ? `Öğrenci: ${s.studentName}` : `Öğretmen: ${s.teacherName}`}</div>
                  <div>{dateTime(s.startsAt)} – {new Date(s.endsAt).toLocaleTimeString('tr-TR', { timeStyle: 'short' })}</div>
                </div>
                <Status value={s.status} labels={SESSION_STATUS} />
              </div>
              <div className="row wrapr">
                {s.status === 'SCHEDULED' && teacher && started && <button className="btn primary" onClick={() => act(s.id, 'COMPLETED')}>Tamamlandı olarak işaretle</button>}
                {s.status === 'SCHEDULED' && <button className="btn ghost" onClick={() => act(s.id, 'CANCELLED')}>Dersi iptal et</button>}
                {!teacher && s.status === 'COMPLETED' && !s.reviewed && <button className="btn primary" onClick={() => setOpen(open === s.id ? null : s.id)}>Yorum yaz</button>}
                {s.reviewed && <span className="muted small">Yorum yazıldı</span>}
              </div>
              {open === s.id && <ReviewForm session={s} onError={setErr} onDone={() => { setOpen(null); load(); }} />}
            </div>
          );
        })}
      </div>
    </div>
  );
}
