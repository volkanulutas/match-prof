import { useEffect, useRef, useState } from 'react';
import { Link, NavLink, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth.jsx';
import { useRealtime } from '../realtime.jsx';
import { api, dateTime } from '../api.js';

function Bell({ unread, setUnread }) {
  const nav = useNavigate();
  const [open, setOpen] = useState(false);
  const [items, setItems] = useState([]);
  const box = useRef(null);
  const { subscribe } = useRealtime();

  const load = () => api('/notifications').then((l) => setItems(l.slice(0, 6))).catch(() => {});

  useEffect(() => { if (open) load(); }, [open]);
  useEffect(() => subscribe((ev) => { if (ev.type === 'notification' && open) load(); }), [subscribe, open]);
  useEffect(() => {
    const close = (e) => { if (box.current && !box.current.contains(e.target)) setOpen(false); };
    document.addEventListener('mousedown', close);
    return () => document.removeEventListener('mousedown', close);
  }, []);

  const go = (n) => {
    setOpen(false);
    if (!n.seen) {
      api(`/notifications/${n.id}/read`, { method: 'POST' }).catch(() => {});
      setUnread((u) => Math.max(0, u - 1));
    }
    if (n.link) nav(n.link);
  };

  return (
    <div className="bellbox" ref={box}>
      <button className="bell" onClick={() => setOpen(!open)} aria-expanded={open} aria-haspopup="true">
        Bildirimler{unread > 0 && <span className="dot">{unread}</span>}
      </button>
      {open && (
        <div className="dropdown" role="menu">
          {items.length === 0 && <p className="muted small pad-s">Bildirim yok.</p>}
          {items.map((n) => (
            <button key={n.id} role="menuitem" className={'dd-item' + (n.seen ? '' : ' unseen')} onClick={() => go(n)}>
              <span>{n.content}</span>
              <span className="muted small">{dateTime(n.createdAt)}</span>
            </button>
          ))}
          <Link to="/bildirimler" className="dd-all" onClick={() => setOpen(false)}>Tüm bildirimler</Link>
        </div>
      )}
    </div>
  );
}

export default function Header() {
  const { user, logout } = useAuth();
  const nav = useNavigate();
  const [unread, setUnread] = useState(0);
  const { subscribe, connected } = useRealtime();
  const { pathname } = useLocation();

  useEffect(() => {
    if (!user) { setUnread(0); return undefined; }
    let alive = true;
    const load = () => api('/notifications/unread-count').then((r) => alive && setUnread(r.count)).catch(() => {});
    load();
    const t = connected ? null : setInterval(load, 30000);
    return () => { alive = false; if (t) clearInterval(t); };
  }, [user, connected]);

  useEffect(() => subscribe((ev) => { if (ev.type === 'notification') setUnread((u) => u + 1); }), [subscribe]);
  useEffect(() => { if (pathname === '/bildirimler') setUnread(0); }, [pathname]);

  return (
    <header className="header">
      <div className="header-in">
        <Link to="/" className="brand">dersbul</Link>
        <nav className="nav">
          <NavLink to="/ara">Öğretmen bul</NavLink>
          {user?.role === 'TEACHER' && <NavLink to="/ogretmen">İlanlarım</NavLink>}
          {(user?.role === 'STUDENT' || user?.role === 'TEACHER') && (
            <>
              <NavLink to="/talepler">Talepler</NavLink>
              <NavLink to="/dersler">Derslerim</NavLink>
              <NavLink to="/pass">{user.role === 'TEACHER' ? 'Professional Pass' : 'Öğrenci Pass'}</NavLink>
            </>
          )}
          {user?.role === 'ADMIN' && <NavLink to="/yonetim">Yönetim</NavLink>}
        </nav>
        <div className="nav-right">
          {user ? (
            <>
              <Bell unread={unread} setUnread={setUnread} />
              <span className="who">{user.fullName}</span>
              <button className="btn ghost" onClick={() => { logout(); nav('/'); }}>Çıkış yap</button>
            </>
          ) : (
            <>
              <Link to="/giris" className="btn ghost">Giriş yap</Link>
              <Link to="/kayit" className="btn primary">Kayıt ol</Link>
            </>
          )}
        </div>
      </div>
    </header>
  );
}
