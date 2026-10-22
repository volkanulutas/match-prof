import { useState } from 'react';
import { Link, useLocation, useNavigate, useSearchParams } from 'react-router-dom';
import { useAuth } from '../auth.jsx';
import { Alert } from '../components/Bits.jsx';
import CitySelect from '../components/CitySelect.jsx';

export function Login() {
  const { login } = useAuth();
  const nav = useNavigate();
  const loc = useLocation();
  const [f, setF] = useState({ email: '', password: '' });
  const [err, setErr] = useState('');

  const submit = async (e) => {
    e.preventDefault(); setErr('');
    try { const u = await login(f.email, f.password); nav(loc.state?.from || (u.role === 'ADMIN' ? '/yonetim' : '/')); }
    catch (e2) { setErr(e2.message); }
  };

  return (
    <form className="card narrow stack" onSubmit={submit}>
      <h1 className="h1s">Giriş yap</h1>
      <Alert>{err}</Alert>
      <label>E-posta<input type="email" required value={f.email} onChange={(e) => setF({ ...f, email: e.target.value })} autoComplete="email" /></label>
      <label>Şifre<input type="password" required value={f.password} onChange={(e) => setF({ ...f, password: e.target.value })} autoComplete="current-password" /></label>
      <button className="btn primary big">Giriş yap</button>
      <p className="muted small">Hesabın yok mu? <Link to="/kayit">Kayıt ol</Link></p>
      <p className="muted small">Demo: ogrenci@dersbul.local, melih@dersbul.local veya admin@dersbul.local. Şifreler: Demo12345! (yönetici: Admin12345!)</p>
    </form>
  );
}

export function Register() {
  const { register } = useAuth();
  const nav = useNavigate();
  const [params] = useSearchParams();
  const [f, setF] = useState({ fullName: '', email: '', password: '', city: '', role: params.get('rol') === 'TEACHER' ? 'TEACHER' : 'STUDENT' });
  const [err, setErr] = useState('');

  const submit = async (e) => {
    e.preventDefault(); setErr('');
    try { const u = await register(f); nav(u.role === 'TEACHER' ? '/ogretmen' : '/'); }
    catch (e2) { setErr(e2.message); }
  };

  return (
    <form className="card narrow stack" onSubmit={submit}>
      <h1 className="h1s">Hesap oluştur</h1>
      <Alert>{err}</Alert>
      <div className="seg" role="radiogroup" aria-label="Hesap türü">
        <button type="button" className={f.role === 'STUDENT' ? 'on' : ''} onClick={() => setF({ ...f, role: 'STUDENT' })}>Ders almak istiyorum</button>
        <button type="button" className={f.role === 'TEACHER' ? 'on' : ''} onClick={() => setF({ ...f, role: 'TEACHER' })}>Ders vermek istiyorum</button>
      </div>
      <label>Ad soyad<input required value={f.fullName} onChange={(e) => setF({ ...f, fullName: e.target.value })} autoComplete="name" /></label>
      <label>E-posta<input type="email" required value={f.email} onChange={(e) => setF({ ...f, email: e.target.value })} autoComplete="email" /></label>
      <label>İl<CitySelect required value={f.city} onChange={(v) => setF({ ...f, city: v })} /></label>
      <label>Şifre (en az 8 karakter)<input type="password" required minLength={8} value={f.password} onChange={(e) => setF({ ...f, password: e.target.value })} autoComplete="new-password" /></label>
      <button className="btn primary big">Kayıt ol</button>
      <p className="muted small">Zaten hesabın var mı? <Link to="/giris">Giriş yap</Link></p>
    </form>
  );
}
