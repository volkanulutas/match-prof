import { useEffect, useState, useCallback } from 'react';
import { Link } from 'react-router-dom';
import { api, dateTime, money } from '../api.js';
import { Alert, Empty } from '../components/Bits.jsx';

export default function Admin() {
  const [stats, setStats] = useState(null);
  const [reports, setReports] = useState([]);
  const [quals, setQuals] = useState([]);
  const [listings, setListings] = useState([]);
  const [err, setErr] = useState('');

  const load = useCallback(() => {
    api('/admin/stats').then(setStats).catch((e) => setErr(e.message));
    api('/admin/reports?status=OPEN').then(setReports);
    api('/admin/qualifications?status=UNVERIFIED').then(setQuals);
    api('/admin/listings').then(setListings);
  }, []);
  useEffect(() => { load(); }, [load]);

  const run = async (path, body) => {
    setErr('');
    try { await api(path, { method: 'PATCH', body }); load(); } catch (e) { setErr(e.message); }
  };

  const S = stats && [
    ['Kullanıcı', stats.users], ['Öğretmen', stats.teachers], ['Öğrenci', stats.students],
    ['Yayındaki ilan', stats.activeListings], ['Bekleyen talep', stats.pendingRequests],
    ['Açık şikâyet', stats.openReports], ['Doğrulanacak belge', stats.pendingQualifications],
  ];

  return (
    <div className="wrap">
      <h1 className="h1s">Yönetim</h1>
      <Alert>{err}</Alert>
      <div className="stats">{S?.map(([k, v]) => <div key={k} className="card stat"><div className="num">{v}</div><div className="muted small">{k}</div></div>)}</div>

      <h2>Açık şikâyetler</h2>
      {reports.length === 0 && <Empty>Açık şikâyet yok.</Empty>}
      <div className="stackl">
        {reports.map((r) => (
          <div key={r.id} className="card">
            <Link to={`/ilan/${r.listingId}`}><strong>{r.listingTitle}</strong></Link>
            <div className="muted small">{r.reporterName} · {dateTime(r.createdAt)}</div>
            <p>{r.reason}</p>
            <div className="row">
              <button className="btn primary" onClick={() => run(`/admin/reports/${r.id}`, { action: 'SUSPEND', note: 'İlan askıya alındı' })}>İlanı askıya al</button>
              <button className="btn ghost" onClick={() => run(`/admin/reports/${r.id}`, { action: 'DISMISS', note: 'Kural ihlali bulunmadı' })}>Şikâyeti reddet</button>
            </div>
          </div>
        ))}
      </div>

      <h2>Doğrulama bekleyen belgeler</h2>
      {quals.length === 0 && <Empty>Bekleyen belge yok.</Empty>}
      <ul className="plain">
        {quals.map((q) => (
          <li key={q.id} className="row between card">
            <span><strong>{q.teacherName}</strong>: {q.title}</span>
            <span className="row">
              <button className="btn primary" onClick={() => run(`/admin/qualifications/${q.id}`, { status: 'VERIFIED' })}>Doğrula</button>
              <button className="btn ghost" onClick={() => run(`/admin/qualifications/${q.id}`, { status: 'REJECTED' })}>Reddet</button>
            </span>
          </li>
        ))}
      </ul>

      <h2>Tüm ilanlar</h2>
      <div className="stackl">
        {listings.map((l) => (
          <div key={l.id} className="card row between">
            <div><Link to={`/ilan/${l.id}`}>{l.title}</Link>
              <div className="muted small">{l.teacherName} · {money(l.hourlyRate)} · {l.status}</div></div>
            {l.status === 'SUSPENDED'
              ? <button className="btn ghost" onClick={() => run(`/admin/listings/${l.id}/status`, { status: 'ACTIVE' })}>Yeniden yayınla</button>
              : <button className="btn ghost" onClick={() => run(`/admin/listings/${l.id}/status`, { status: 'SUSPENDED' })}>Askıya al</button>}
          </div>
        ))}
      </div>
    </div>
  );
}
