import { Routes, Route, Navigate, useLocation } from 'react-router-dom';
import Header from './components/Header.jsx';
import CompareBar from './components/CompareBar.jsx';
import Compare from './pages/Compare.jsx';
import { useAuth } from './auth.jsx';
import Home from './pages/Home.jsx';
import Search from './pages/Search.jsx';
import ListingDetail from './pages/ListingDetail.jsx';
import { Login, Register } from './pages/AuthPages.jsx';
import Requests from './pages/Requests.jsx';
import Thread from './pages/Thread.jsx';
import Sessions from './pages/Sessions.jsx';
import TeacherPanel from './pages/TeacherPanel.jsx';
import Pass from './pages/Pass.jsx';
import Notifications from './pages/Notifications.jsx';
import Admin from './pages/Admin.jsx';

function Guard({ roles, children }) {
  const { user, ready } = useAuth();
  const loc = useLocation();
  if (!ready) return <p className="muted pad">Yükleniyor…</p>;
  if (!user) return <Navigate to="/giris" state={{ from: loc.pathname }} replace />;
  if (roles && !roles.includes(user.role)) return <Navigate to="/" replace />;
  return children;
}

export default function App() {
  return (
    <>
      <Header />
      <main>
        <Routes>
          <Route path="/" element={<Home />} />
          <Route path="/ara" element={<Search />} />
          <Route path="/ilan/:id" element={<ListingDetail />} />
          <Route path="/karsilastir" element={<Compare />} />
          <Route path="/giris" element={<Login />} />
          <Route path="/kayit" element={<Register />} />
          <Route path="/talepler" element={<Guard roles={['STUDENT', 'TEACHER']}><Requests /></Guard>} />
          <Route path="/mesajlar/:requestId" element={<Guard roles={['STUDENT', 'TEACHER']}><Thread /></Guard>} />
          <Route path="/dersler" element={<Guard roles={['STUDENT', 'TEACHER']}><Sessions /></Guard>} />
          <Route path="/ogretmen" element={<Guard roles={['TEACHER']}><TeacherPanel /></Guard>} />
          <Route path="/pass" element={<Guard roles={['STUDENT', 'TEACHER']}><Pass /></Guard>} />
          <Route path="/bildirimler" element={<Guard><Notifications /></Guard>} />
          <Route path="/yonetim" element={<Guard roles={['ADMIN']}><Admin /></Guard>} />
          <Route path="*" element={<p className="pad">Sayfa bulunamadı.</p>} />
        </Routes>
      </main>
      <CompareBar />
      <footer className="footer">
        <span>Dersbul — örnek proje. Fiyatlar ve ödeme akışı demodur.</span>
      </footer>
    </>
  );
}
