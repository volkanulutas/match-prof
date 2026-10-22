import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { api, qs } from '../api.js';
import { ListingCard } from '../components/Bits.jsx';

export default function Home() {
  const nav = useNavigate();
  const [q, setQ] = useState('');
  const [city, setCity] = useState('');
  const [online, setOnline] = useState(false);
  const [cats, setCats] = useState([]);
  const [top, setTop] = useState([]);

  useEffect(() => {
    api('/categories').then(setCats).catch(() => {});
    api('/listings' + qs({ size: 6 })).then((r) => setTop(r.content)).catch(() => {});
  }, []);

  const groups = cats.reduce((m, c) => ((m[c.groupName] ||= []).push(c), m), {});
  const submit = (e) => { e.preventDefault(); nav('/ara' + qs({ q, city, online })); };

  return (
    <>
      <section className="hero">
        <div className="hero-in">
          <h1>Ne öğrenmek istiyorsun?</h1>
          <p>Matematikten piyanoya, online ya da yüz yüze. Öğretmenin profilini, ücretini ve yorumlarını görüp doğrudan talep gönder.</p>
          <form className="hero-form" onSubmit={submit}>
            <label>
              <span className="sr">Ders</span>
              <input value={q} onChange={(e) => setQ(e.target.value)} placeholder="Ders veya konu (ör. matematik, gitar)" />
            </label>
            <label>
              <span className="sr">Şehir</span>
              <input value={city} onChange={(e) => setCity(e.target.value)} placeholder="Şehir" />
            </label>
            <label className="check light">
              <input type="checkbox" checked={online} onChange={(e) => setOnline(e.target.checked)} /> Sadece online
            </label>
            <button className="btn primary big">Öğretmen bul</button>
          </form>
        </div>
      </section>

      <section className="wrap">
        <h2>Derslere göz at</h2>
        {Object.entries(groups).map(([g, items]) => (
          <div key={g} className="catgroup">
            <h3>{g}</h3>
            <div className="chips">
              {items.map((c) => <Link key={c.id} className="chip link" to={`/ara?category=${c.slug}`}>{c.name}</Link>)}
            </div>
          </div>
        ))}
      </section>

      <section className="wrap">
        <h2>Öne çıkan öğretmenler</h2>
        <div className="grid">{top.map((l) => <ListingCard key={l.id} l={l} />)}</div>
      </section>

      <section className="wrap cta">
        <div>
          <h2>Bilgini ders olarak paylaş</h2>
          <p className="muted">İlan oluştur, ücretini ve saatlerini kendin belirle, öğrencilerden gelen talepleri yönet.</p>
        </div>
        <Link to="/kayit?rol=TEACHER" className="btn primary big">Öğretmen olarak kayıt ol</Link>
      </section>
    </>
  );
}
