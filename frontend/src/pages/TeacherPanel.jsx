import { useEffect, useState, useCallback } from 'react';
import { Link } from 'react-router-dom';
import { api, money, DAYS } from '../api.js';
import { Alert, Empty } from '../components/Bits.jsx';
import CitySelect, { CategorySelect } from '../components/CitySelect.jsx';

const blank = {
  categoryId: '', title: '', description: '', hourlyRate: '', city: '',
  online: true, teacherHome: false, studentHome: false, firstLessonFree: true,
  experienceYears: 0, education: '', languages: 'Türkçe',
};

function ListingForm({ cats, initial, onSaved, onCancel }) {
  const [f, setF] = useState(initial);
  const [err, setErr] = useState('');
  const set = (k) => (e) => setF({ ...f, [k]: e.target.type === 'checkbox' ? e.target.checked : e.target.value });

  const submit = async (e) => {
    e.preventDefault(); setErr('');
    const body = { ...f, categoryId: Number(f.categoryId), hourlyRate: Number(f.hourlyRate), experienceYears: Number(f.experienceYears) };
    try {
      if (f.id) await api(`/teacher/listings/${f.id}`, { method: 'PUT', body });
      else await api('/teacher/listings', { method: 'POST', body });
      onSaved();
    } catch (e2) { setErr(e2.message); }
  };

  return (
    <form className="card stack" onSubmit={submit}>
      <h3>{f.id ? 'İlanı düzenle' : 'Yeni ilan'}</h3>
      <Alert>{err}</Alert>
      <label>Ders alanı
        <CategorySelect required allowEmpty={false} valueKey="id" cats={cats} value={f.categoryId} onChange={(v) => setF({ ...f, categoryId: v })} />
      </label>
      <label>İlan başlığı<input required maxLength={140} value={f.title} onChange={set('title')} /></label>
      <label>Açıklama (en az 40 karakter)<textarea required minLength={40} rows={5} value={f.description} onChange={set('description')}
        placeholder="Deneyiminiz, yönteminiz ve hangi seviyelere ders verdiğiniz. Telefon, e-posta ve web adresi yazmayın." /></label>
      <div className="row wrapr">
        <label>Saatlik ücret (₺)<input type="number" min="1" required value={f.hourlyRate} onChange={set('hourlyRate')} /></label>
        <label>İl<CitySelect required value={f.city} onChange={(v) => setF({ ...f, city: v })} /></label>
        <label>Deneyim (yıl)<input type="number" min="0" value={f.experienceYears} onChange={set('experienceYears')} /></label>
      </div>
      <label>Eğitim<input value={f.education} onChange={set('education')} /></label>
      <label>Diller<input value={f.languages} onChange={set('languages')} /></label>
      <fieldset>
        <legend>Ders biçimleri</legend>
        <label className="check"><input type="checkbox" checked={f.online} onChange={set('online')} /> Online</label>
        <label className="check"><input type="checkbox" checked={f.teacherHome} onChange={set('teacherHome')} /> Öğretmenin yerinde</label>
        <label className="check"><input type="checkbox" checked={f.studentHome} onChange={set('studentHome')} /> Öğrencinin evinde</label>
      </fieldset>
      <label className="check"><input type="checkbox" checked={f.firstLessonFree} onChange={set('firstLessonFree')} /> İlk ders ücretsiz</label>
      <div className="row">
        <button className="btn primary">{f.id ? 'Değişiklikleri kaydet' : 'İlanı yayınla'}</button>
        {onCancel && <button type="button" className="btn ghost" onClick={onCancel}>Vazgeç</button>}
      </div>
    </form>
  );
}

function Availability() {
  const [list, setList] = useState([]);
  const [f, setF] = useState({ weekday: 'MONDAY', startTime: '18:00', endTime: '21:00' });
  const [err, setErr] = useState('');
  const load = useCallback(() => api('/teacher/availability').then(setList).catch((e) => setErr(e.message)), []);
  useEffect(() => { load(); }, [load]);

  const add = async (e) => {
    e.preventDefault(); setErr('');
    try { await api('/teacher/availability', { method: 'POST', body: f }); load(); } catch (e2) { setErr(e2.message); }
  };
  const del = async (id) => { await api(`/teacher/availability/${id}`, { method: 'DELETE' }); load(); };

  return (
    <section className="card stack">
      <h3>Müsaitlik</h3>
      <Alert>{err}</Alert>
      {list.length === 0 && <Empty>Müsait saat eklemediniz. Öğrenciler uygun zamanı buradan görür.</Empty>}
      <ul className="plain">
        {list.map((a) => (
          <li key={a.id} className="row between">
            <span>{DAYS[a.weekday]}: {a.startTime.slice(0, 5)} – {a.endTime.slice(0, 5)}</span>
            <button className="linkbtn" onClick={() => del(a.id)}>Sil</button>
          </li>
        ))}
      </ul>
      <form className="row wrapr" onSubmit={add}>
        <label>Gün
          <select value={f.weekday} onChange={(e) => setF({ ...f, weekday: e.target.value })}>
            {Object.entries(DAYS).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
          </select>
        </label>
        <label>Başlangıç<input type="time" value={f.startTime} onChange={(e) => setF({ ...f, startTime: e.target.value })} /></label>
        <label>Bitiş<input type="time" value={f.endTime} onChange={(e) => setF({ ...f, endTime: e.target.value })} /></label>
        <button className="btn ghost">Ekle</button>
      </form>
    </section>
  );
}

const QSTAT = { UNVERIFIED: 'Doğrulama bekliyor', VERIFIED: 'Doğrulandı', REJECTED: 'Reddedildi' };

function Qualifications() {
  const [list, setList] = useState([]);
  const [title, setTitle] = useState('');
  const [err, setErr] = useState('');
  const load = useCallback(() => api('/teacher/qualifications').then(setList).catch((e) => setErr(e.message)), []);
  useEffect(() => { load(); }, [load]);

  const add = async (e) => {
    e.preventDefault(); setErr('');
    try { await api('/teacher/qualifications', { method: 'POST', body: { title } }); setTitle(''); load(); } catch (e2) { setErr(e2.message); }
  };
  const del = async (id) => { await api(`/teacher/qualifications/${id}`, { method: 'DELETE' }); load(); };

  return (
    <section className="card stack">
      <h3>Belgeler ve sertifikalar</h3>
      <p className="muted small">Eklediğiniz belgeler yönetici tarafından doğrulanınca profilinizde "Doğrulandı" olarak görünür.</p>
      <Alert>{err}</Alert>
      <ul className="plain">
        {list.map((q) => (
          <li key={q.id} className="row between"><span>{q.title} <span className={`badge st-${q.status.toLowerCase()}`}>{QSTAT[q.status]}</span></span>
            <button className="linkbtn" onClick={() => del(q.id)}>Sil</button></li>
        ))}
      </ul>
      <form className="row" onSubmit={add}>
        <input required value={title} onChange={(e) => setTitle(e.target.value)} placeholder="ör. ODTÜ Matematik Lisans Diploması" />
        <button className="btn ghost">Ekle</button>
      </form>
    </section>
  );
}

export default function TeacherPanel() {
  const [cats, setCats] = useState([]);
  const [mine, setMine] = useState(null);
  const [editing, setEditing] = useState(null); // null | 'new' | listing
  const [err, setErr] = useState('');

  const load = useCallback(() => api('/teacher/listings').then(setMine).catch((e) => setErr(e.message)), []);
  useEffect(() => { api('/categories').then(setCats); load(); }, [load]);

  const toggle = async (l) => {
    setErr('');
    try { await api(`/teacher/listings/${l.id}/status`, { method: 'PATCH', body: { status: l.status === 'ACTIVE' ? 'PAUSED' : 'ACTIVE' } }); load(); }
    catch (e) { setErr(e.message); }
  };
  const LS = { ACTIVE: 'Yayında', PAUSED: 'Duraklatıldı', SUSPENDED: 'Askıya alındı' };

  return (
    <div className="wrap">
      <div className="row between">
        <h1 className="h1s">İlanlarım</h1>
        {!editing && <button className="btn primary" onClick={() => setEditing('new')}>Yeni ilan</button>}
      </div>
      <Alert>{err}</Alert>
      {editing && (
        <ListingForm cats={cats} onCancel={() => setEditing(null)} onSaved={() => { setEditing(null); load(); }}
          initial={editing === 'new' ? blank : { ...editing, categoryId: editing.categoryId }} />
      )}
      {mine?.length === 0 && !editing && <Empty>Henüz ilanınız yok. İlk ilanınızı oluşturun, öğrenciler sizi bulsun.</Empty>}
      <div className="stackl">
        {mine?.map((l) => (
          <div key={l.id} className="card">
            <div className="row between">
              <div><Link to={`/ilan/${l.id}`}><strong>{l.title}</strong></Link>
                <div className="muted small">{l.categoryName} · {l.city} · {money(l.hourlyRate)} / saat</div></div>
              <span className={`badge st-${l.status === 'ACTIVE' ? 'accepted' : l.status === 'PAUSED' ? 'pending' : 'declined'}`}>{LS[l.status]}</span>
            </div>
            <div className="row">
              <button className="btn ghost" onClick={() => { setEditing(l); window.scrollTo({ top: 0 }); }} disabled={l.status === 'SUSPENDED'}>Düzenle</button>
              {l.status !== 'SUSPENDED' && <button className="btn ghost" onClick={() => toggle(l)}>{l.status === 'ACTIVE' ? 'Yayından kaldır' : 'Yayına al'}</button>}
            </div>
          </div>
        ))}
      </div>
      <div className="grid two">
        <Availability />
        <Qualifications />
      </div>
    </div>
  );
}
