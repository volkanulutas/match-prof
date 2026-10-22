import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { api, qs } from '../api.js';
import { ListingCard } from '../components/Bits.jsx';
import CitySelect, { CategorySelect } from '../components/CitySelect.jsx';

export default function Home() {
  const nav = useNavigate();
  const [category, setCategory] = useState('');
  const [city, setCity] = useState('');
  const [q, setQ] = useState('');
  const [online, setOnline] = useState(false);
  const [cats, setCats] = useState([]);
  const [top, setTop] = useState([]);

  useEffect(() => {
    api('/categories').then(setCats).catch(() => {});
    api('/listings' + qs({ size: 6 })).then((r) => setTop(r.content)).catch(() => {});
  }, []);

  const groups = cats.reduce((m, c) => ((m[c.groupName] ||= []).push(c), m), {});
  const submit = (e) => { e.preventDefault(); nav('/ara' + qs({ q, category, city, online })); };

  return (
    <>
      <section className="hero">
        <div className="hero-in">
          <h1>Doğru öğretmeni bul,<br />hedefine daha hızlı ulaş</h1>
          <p>Ders alanını ve şehrini seç ya da online dersleri ara. Öğretmenleri ücret, puan ve deneyime göre karşılaştır, uygun saati seçip talep gönder.</p>
          <form className="hero-form" onSubmit={submit}>
            <label><span className="sr">Ders alanı</span>
              <CategorySelect cats={cats} value={category} onChange={setCategory} emptyLabel="Ders alanı (tümü)" />
            </label>
            <label><span className="sr">Şehir</span>
              <CitySelect value={city} onChange={setCity} allowEmpty emptyLabel="Şehir (tümü)" />
            </label>
            <label><span className="sr">Anahtar kelime</span>
              <input value={q} onChange={(e) => setQ(e.target.value)} placeholder="Anahtar kelime (isteğe bağlı)" />
            </label>
            <button className="btn primary big">Öğretmen bul</button>
            <label className="check light span-all">
              <input type="checkbox" checked={online} onChange={(e) => setOnline(e.target.checked)} /> Sadece online ders
            </label>
          </form>
        </div>
      </section>

      <section className="wrap">
        <h2>Derslere göz at</h2>
        <div className="catgrid">
          {Object.entries(groups).map(([g, items]) => (
            <div key={g} className="card catcard">
              <h3>{g}</h3>
              <ul className="plain">
                {items.map((c) => (
                  <li key={c.id}>
                    <Link to={`/ara?category=${c.slug}`}>{c.name}</Link>
                    <span className="muted small"> {c.listingCount > 0 ? `${c.listingCount} ilan` : ''}</span>
                  </li>
                ))}
              </ul>
            </div>
          ))}
        </div>
      </section>

      <section className="wrap">
        <h2>Öne çıkan öğretmenler</h2>
        <div className="grid">{top.map((l) => <ListingCard key={l.id} l={l} />)}</div>
      </section>

      <section className="wrap cta">
        <div>
          <h2>Bilgini ders olarak paylaş</h2>
          <p className="muted">İlan oluştur, ücretini ve müsait saatlerini belirle, öğrencilerden gelen talepleri kendi takvimine göre onayla.</p>
        </div>
        <Link to="/kayit?rol=TEACHER" className="btn primary big">Öğretmen olarak kayıt ol</Link>
      </section>
    </>
  );
}
