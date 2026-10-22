import { useEffect, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { api, money } from '../api.js';
import { useCompare } from '../compare.jsx';
import { Alert, Empty, Stars } from '../components/Bits.jsx';

const yes = <span className="yes">Var</span>;
const no = <span className="muted">Yok</span>;

export default function Compare() {
  const [params] = useSearchParams();
  const { toggle } = useCompare();
  const ids = (params.get('ids') || '').split(',').map(Number).filter(Boolean).slice(0, 3);
  const [items, setItems] = useState(null);
  const [err, setErr] = useState('');

  useEffect(() => {
    setItems(null);
    Promise.all(ids.map((id) => api(`/listings/${id}`).catch(() => null)))
      .then((r) => setItems(r.filter(Boolean)))
      .catch((e) => setErr(e.message));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [params.get('ids')]);

  if (ids.length === 0) return <div className="wrap"><Empty>Karşılaştırılacak ilan seçilmedi. <Link to="/ara">Öğretmen arayın</Link> ve kartlardaki “Karşılaştır” kutusunu işaretleyin.</Empty></div>;

  const minPrice = items ? Math.min(...items.map((i) => Number(i.hourlyRate))) : 0;
  const maxRating = items ? Math.max(...items.map((i) => i.avgRating)) : 0;
  const maxExp = items ? Math.max(...items.map((i) => i.experienceYears)) : 0;

  const rows = [
    ['Saatlik ücret', (i) => <span className={Number(i.hourlyRate) === minPrice && items.length > 1 ? 'best' : ''}>{money(i.hourlyRate)}</span>],
    ['Puan', (i) => <span className={i.avgRating === maxRating && maxRating > 0 && items.length > 1 ? 'best' : ''}><Stars value={i.avgRating} count={i.reviewCount} /></span>],
    ['Deneyim', (i) => <span className={i.experienceYears === maxExp && maxExp > 0 && items.length > 1 ? 'best' : ''}>{i.experienceYears} yıl</span>],
    ['Şehir', (i) => i.city],
    ['Online ders', (i) => (i.online ? yes : no)],
    ['Öğretmenin yerinde', (i) => (i.teacherHome ? yes : no)],
    ['Öğrencinin evinde', (i) => (i.studentHome ? yes : no)],
    ['İlk ders ücretsiz', (i) => (i.firstLessonFree ? yes : no)],
    ['Doğrulanmış belge', (i) => (i.qualifications.some((q) => q.status === 'VERIFIED') ? yes : no)],
    ['Eğitim', (i) => i.education || '—'],
    ['Diller', (i) => i.languages || '—'],
  ];

  return (
    <div className="wrap">
      <p><Link to="/ara">← Aramaya dön</Link></p>
      <h1 className="h1s">İlanları karşılaştır</h1>
      <Alert>{err}</Alert>
      {!items && !err && <p className="muted">Yükleniyor…</p>}
      {items && items.length === 0 && <Empty>Seçilen ilanlar artık yayında değil.</Empty>}
      {items && items.length > 0 && (
        <div className="tablewrap">
          <table className="cmp-table">
            <thead>
              <tr>
                <th />
                {items.map((i) => (
                  <th key={i.id}>
                    <Link to={`/ilan/${i.id}`}>{i.teacherName}</Link>
                    <div className="muted small normal">{i.categoryName}</div>
                    <button className="linkbtn small" onClick={() => { toggle(i.id); setItems(items.filter((x) => x.id !== i.id)); }}>Listeden çıkar</button>
                  </th>
                ))}
              </tr>
            </thead>
            <tbody>
              {rows.map(([label, fn]) => (
                <tr key={label}><th scope="row">{label}</th>{items.map((i) => <td key={i.id}>{fn(i)}</td>)}</tr>
              ))}
              <tr><th scope="row" />{items.map((i) => <td key={i.id}><Link className="btn primary" to={`/ilan/${i.id}`}>İlana git ve talep gönder</Link></td>)}</tr>
            </tbody>
          </table>
        </div>
      )}
      <p className="muted small">Yeşil işaretli değerler karşılaştırılan ilanlar arasındaki en iyi değerdir.</p>
    </div>
  );
}
