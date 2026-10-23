import { useEffect, useState, useCallback } from 'react';
import { Link } from 'react-router-dom';
import { api, dateTime, MODES, REQUEST_STATUS } from '../api.js';
import { useAuth } from '../auth.jsx';
import { useRealtime } from '../realtime.jsx';
import { Alert, Empty, Status } from '../components/Bits.jsx';

export default function Requests() {
  const { user } = useAuth();
  const [list, setList] = useState(null);
  const [err, setErr] = useState('');
  const load = useCallback(() => api('/requests/mine').then(setList).catch((e) => setErr(e.message)), []);
  useEffect(() => { load(); }, [load]);
  const { subscribe } = useRealtime();
  useEffect(() => subscribe((ev) => { if (ev.type === 'notification') load(); }), [subscribe, load]);

  const teacher = user.role === 'TEACHER';
  return (
    <div className="wrap">
      <h1 className="h1s">{teacher ? 'Gelen talepler' : 'Gönderdiğim talepler'}</h1>
      <Alert>{err}</Alert>
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
                <Link to={`/mesajlar/${r.id}`}><strong>{r.listingTitle}</strong></Link>
                <div className="muted small">{teacher ? `Öğrenci: ${r.studentName}` : `Öğretmen: ${r.teacherName}`} · {MODES[r.mode]} · {dateTime(r.createdAt)}</div>
              </div>
              <Status value={r.status} labels={REQUEST_STATUS} />
            </div>
            <p className="prose clamp">{r.message}</p>
            <div className="row wrapr">
              <Link className="btn primary" to={`/mesajlar/${r.id}`}>
                {teacher && r.status === 'PENDING' ? 'Görüntüle ve yanıtla' : 'Sohbeti aç'}
              </Link>
              {r.slots.length > 0 && <span className="muted small">{r.slots.length} zaman önerisi</span>}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
