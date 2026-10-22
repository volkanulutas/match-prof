import { useEffect, useState } from 'react';
import { Link, NavLink, useLocation, useNavigate } from 'react-router-dom';
import { useRealtime } from '../realtime.jsx';
import { useAuth } from '../auth.jsx';
import { api } from '../api.js';

export default function Header() {
  const { user, logout } = useAuth();
  const nav = useNavigate();
  const [unread, setUnread] = useState(0);
  const { subscribe, connected } = useRealtime();
  const { pathname } = useLocation();

  useEffect(() => {
    if (!user) { setUnread(0); return; }
    let alive = true;
    const load = () => api('/notifications/unread-count').then((r) => alive && setUnread(r.count)).catch(() => {});
    load();
    // Bağlantı varken anlık olaylar yeter; yokken 30 sn'de bir sorgula.
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
              <Link to="/bildirimler" className="bell" aria-label="Bildirimler">
                Bildirimler{unread > 0 && <span className="dot">{unread}</span>}
              </Link>
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
