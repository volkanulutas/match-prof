import { useEffect, useState, useCallback } from 'react';
import { Link } from 'react-router-dom';
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

export default function Requests() {
  const { user } = useAuth();
  const [list, setList] = useState(null);
  const [err, setErr] = useState('');
  const [ok, setOk] = useState('');

  const load = useCallback(() => api('/requests/mine').then(setList).catch((e) => setErr(e.message)), []);
  useEffect(() => { load(); }, [load]);
  const { subscribe } = useRealtime();
  useEffect(() => subscribe((ev) => { if (ev.type === 'notification') load(); }), [subscribe, load]);

  const act = async (id, status) => {
    setErr(''); setOk('');
    try { await api(`/requests/${id}/status`, { method: 'PATCH', body: { status } }); load(); }
    catch (e) { setErr(e.message); }
  };

  const teacher = user.role === 'TEACHER';
  return (
    <div className="wrap">
      <h1 className="h1s">{teacher ? 'Gelen talepler' : 'Gönderdiğim talepler'}</h1>
      <Alert>{err}</Alert>
      <Alert kind="ok">{ok}</Alert>
      {!list && !err && <p className="muted">Yükleniyor…</p>}
      {list?.length === 0 && (
        <Empty>{teacher ? 'Henüz talep gelmedi. İlanınızı ayrıntılı yazmak ve müsaitlik eklemek talep almanıza yardımcı olur.'
          : <>Henüz talep göndermediniz. <Link to="/ara">Öğretmen bulun</Link>.</>}</Empty>
      )}
      <div className="stackl">
        {list?.map((r) => (
          <div key={r.id} className="card">
            <div className="row between">
              <div>
                <Link to={`/ilan/${r.listingId}`}><strong>{r.listingTitle}</strong></Link>
                <div className="muted small">{teacher ? `Öğrenci: ${r.studentName}` : `Öğretmen: ${r.teacherName}`} · {MODES[r.mode]} · {dateTime(r.createdAt)}</div>
              </div>
              <Status value={r.status} labels={REQUEST_STATUS} />
            </div>
            <p className="prose">{r.message}</p>
            {r.studentLevel && <p className="muted small">Seviye: {r.studentLevel}</p>}
            <div className="row wrapr">
              {teacher && r.status === 'PENDING' && (
                <>
                  <button className="btn primary" onClick={() => act(r.id, 'ACCEPTED')}>Kabul et</button>
                  <button className="btn ghost" onClick={() => act(r.id, 'DECLINED')}>Reddet</button>
                </>
              )}
              {!teacher && (r.status === 'PENDING' || r.status === 'ACCEPTED') && (
                <button className="btn ghost" onClick={() => act(r.id, 'CANCELLED')}>Talebi iptal et</button>
              )}
              {r.conversationId && <Link className="btn ghost" to={`/mesajlar/${r.id}`}>Mesajlar</Link>}
            </div>
            {teacher && r.status === 'ACCEPTED' && (
              <ScheduleForm requestId={r.id} onError={setErr} onDone={() => { setOk('Ders planlandı.'); load(); }} />
            )}
          </div>
        ))}
      </div>
    </div>
  );
}
