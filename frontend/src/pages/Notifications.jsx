import { useEffect, useState } from 'react';
import { api, dateTime } from '../api.js';
import { Empty } from '../components/Bits.jsx';

export default function Notifications() {
  const [list, setList] = useState(null);
  useEffect(() => {
    api('/notifications').then(async (l) => { setList(l); if (l.some((n) => !n.seen)) await api('/notifications/read-all', { method: 'POST' }); });
  }, []);
  return (
    <div className="wrap narrowish">
      <h1 className="h1s">Bildirimler</h1>
      {list?.length === 0 && <Empty>Yeni bildirim yok.</Empty>}
      <ul className="plain notes">
        {list?.map((n) => (
          <li key={n.id} className={n.seen ? '' : 'unseen'}>
            <div>{n.content}</div><div className="muted small">{dateTime(n.createdAt)}</div>
          </li>
        ))}
      </ul>
    </div>
  );
}
