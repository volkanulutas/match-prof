import { useEffect, useState, useCallback } from 'react';
import { Link } from 'react-router-dom';
import { api, dateTime } from '../api.js';
import { useRealtime } from '../realtime.jsx';
import { Empty } from '../components/Bits.jsx';

export default function Notifications() {
  const [list, setList] = useState(null);
  const load = useCallback(() => api('/notifications').then(setList), []);
  useEffect(() => { load(); }, [load]);
  const { subscribe } = useRealtime();
  useEffect(() => subscribe((ev) => { if (ev.type === 'notification') load(); }), [subscribe, load]);

  const open = (n) => { if (!n.seen) api(`/notifications/${n.id}/read`, { method: 'POST' }).catch(() => {}); };
  const readAll = async () => { await api('/notifications/read-all', { method: 'POST' }); load(); };

  return (
    <div className="wrap narrowish">
      <div className="row between">
        <h1 className="h1s">Bildirimler</h1>
        {list?.some((n) => !n.seen) && <button className="btn ghost" onClick={readAll}>Tümünü okundu işaretle</button>}
      </div>
      {list?.length === 0 && <Empty>Yeni bildirim yok.</Empty>}
      <ul className="plain notes">
        {list?.map((n) => (
          <li key={n.id} className={n.seen ? '' : 'unseen'}>
            {n.link
              ? <Link to={n.link} onClick={() => open(n)} className="notelink">{n.content}</Link>
              : <div onClick={() => open(n)}>{n.content}</div>}
            <div className="muted small">{dateTime(n.createdAt)}</div>
          </li>
        ))}
      </ul>
    </div>
  );
}
