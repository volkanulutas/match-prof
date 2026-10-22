import { useEffect, useState } from 'react';
import { Link, useParams, useNavigate } from 'react-router-dom';
import { api, money, MODES, DAYS } from '../api.js';
import { useAuth } from '../auth.jsx';
import { Stars, Alert, Empty, CompareToggle } from '../components/Bits.jsx';
import SlotPicker from '../components/SlotPicker.jsx';

export default function ListingDetail() {
  const { id } = useParams();
  const { user } = useAuth();
  const nav = useNavigate();
  const [l, setL] = useState(null);
  const [reviews, setReviews] = useState([]);
  const [avail, setAvail] = useState([]);
  const [err, setErr] = useState('');
  const [msg, setMsg] = useState('');
  const [form, setForm] = useState({ message: '', studentLevel: '', mode: '' });
  const [slots, setSlots] = useState([]);
  const [report, setReport] = useState(null);

  useEffect(() => {
    api(`/listings/${id}`).then((x) => {
      setL(x);
      setForm((f) => ({ ...f, mode: x.online ? 'ONLINE' : x.teacherHome ? 'TEACHER_HOME' : 'STUDENT_HOME' }));
      api(`/teachers/${x.teacherId}/availability`).then(setAvail).catch(() => {});
    }).catch((e) => setErr(e.message));
    api(`/listings/${id}/reviews`).then(setReviews).catch(() => {});
  }, [id]);

  if (err && !l) return <div className="wrap"><Alert>{err}</Alert></div>;
  if (!l) return <p className="muted pad">Yükleniyor…</p>;

  const modes = [['ONLINE', l.online], ['TEACHER_HOME', l.teacherHome], ['STUDENT_HOME', l.studentHome]].filter(([, on]) => on);

  const send = async (e) => {
    e.preventDefault(); setErr(''); setMsg('');
    try {
      const r = await api('/requests', { method: 'POST', body: { listingId: l.id, ...form, slots } });
      setMsg('Talebiniz gönderildi. Sohbet ekranına yönlendiriliyorsunuz…');
      setTimeout(() => nav(`/mesajlar/${r.id}`), 700);
    } catch (e2) { setErr(e2.message); }
  };

  const sendReport = async (e) => {
    e.preventDefault();
    try { await api(`/listings/${l.id}/reports`, { method: 'POST', body: { reason: report } }); setReport(null); setMsg('Şikâyetiniz iletildi.'); }
    catch (e2) { setErr(e2.message); }
  };

  return (
    <div className="wrap detail">
      <article>
        <p className="muted small">{l.categoryName} · {l.city}</p>
        <h1 className="h1s">{l.title}</h1>
        <div className="who-row">
          <div className="avatar big" aria-hidden>{l.teacherName.charAt(0)}</div>
          <div>
            <strong>{l.teacherName}</strong>
            <div><Stars value={l.avgRating} count={l.reviewCount} /></div>
          </div>
          {l.boosted && <span className="badge pro">Öne çıkan</span>}
        </div>

        <div className="chips">
          {modes.map(([m]) => <span key={m} className="chip">{MODES[m]}</span>)}
          {l.firstLessonFree && <span className="chip good">İlk ders ücretsiz</span>}
          <span className="chip">{l.experienceYears} yıl deneyim</span>
          {l.languages && <span className="chip">{l.languages}</span>}
        </div>

        <h2>Hakkında</h2>
        <p className="prose">{l.description}</p>
        {l.education && <p><strong>Eğitim:</strong> {l.education}</p>}

        {l.qualifications.length > 0 && (
          <>
            <h2>Belgeler</h2>
            <ul className="plain">
              {l.qualifications.map((q) => (
                <li key={q.id}>{q.title} {q.status === 'VERIFIED'
                  ? <span className="badge st-accepted">Doğrulandı</span>
                  : <span className="badge st-pending">Öğretmen beyanı</span>}</li>
              ))}
            </ul>
          </>
        )}

        <h2>Haftalık müsaitlik</h2>
        {avail.length === 0 ? <Empty>Öğretmen henüz müsaitlik girmemiş. Talepte uygun saatlerinizi yazın.</Empty> : (
          <ul className="plain">
            {avail.map((a) => <li key={a.id}>{DAYS[a.weekday]}: {a.startTime.slice(0, 5)} – {a.endTime.slice(0, 5)}</li>)}
          </ul>
        )}

        <h2>Yorumlar</h2>
        {reviews.length === 0 ? <Empty>Bu ilan için henüz yorum yok.</Empty> : reviews.map((r) => (
          <div key={r.id} className="review">
            <strong>{r.studentName}</strong> <span className="star">{'★'.repeat(r.rating)}</span>
            {r.body && <p>{r.body}</p>}
          </div>
        ))}

        {user && user.id !== l.teacherId && (
          <div className="small">
            {report === null
              ? <button className="linkbtn" onClick={() => setReport('')}>Bu ilanı şikâyet et</button>
              : (
                <form onSubmit={sendReport} className="stack">
                  <textarea required value={report} onChange={(e) => setReport(e.target.value)} placeholder="Şikâyet nedeniniz" />
                  <div className="row"><button className="btn ghost">Şikâyeti gönder</button>
                    <button type="button" className="linkbtn" onClick={() => setReport(null)}>Vazgeç</button></div>
                </form>
              )}
          </div>
        )}
      </article>

      <aside className="card sticky">
        <div className="price big">{money(l.hourlyRate)}<span className="muted small"> / saat</span></div>
        <CompareToggle id={l.id} />
        <Alert>{err}</Alert>
        <Alert kind="ok">{msg}</Alert>

        {!user && <p>Talep göndermek için <Link to="/giris" state={{ from: `/ilan/${l.id}` }}>giriş yapın</Link> ya da <Link to="/kayit">kayıt olun</Link>.</p>}
        {user?.role === 'TEACHER' && <p className="muted">Öğretmen hesabıyla talep gönderilemez.</p>}
        {user?.role === 'ADMIN' && <p className="muted">Yönetici hesabıyla talep gönderilemez.</p>}
        {user?.role === 'STUDENT' && (
          <form onSubmit={send} className="stack">
            <h3>Ders talebi gönder</h3>
            <label>Ders biçimi
              <select value={form.mode} onChange={(e) => setForm({ ...form, mode: e.target.value })}>
                {modes.map(([m]) => <option key={m} value={m}>{MODES[m]}</option>)}
              </select>
            </label>
            <label>Seviyeniz
              <input value={form.studentLevel} onChange={(e) => setForm({ ...form, studentLevel: e.target.value })} placeholder="ör. 11. sınıf, başlangıç" />
            </label>
            <SlotPicker teacherId={l.teacherId} selected={slots} onChange={setSlots} />
            <label>Mesajınız
              <textarea required value={form.message} onChange={(e) => setForm({ ...form, message: e.target.value })}
                        placeholder="Hedefiniz ve öğretmene iletmek istedikleriniz" />
            </label>
            <button className="btn primary big">Talep gönder</button>
            <p className="muted small">Talebiniz gönderilince öğretmenle sohbet açılır. Öğretmen seçtiğiniz zamanlardan birini onaylayınca ders oluşur.</p>
          </form>
        )}
      </aside>
    </div>
  );
}
