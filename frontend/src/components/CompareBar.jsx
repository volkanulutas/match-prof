import { Link, useLocation } from 'react-router-dom';
import { useCompare } from '../compare.jsx';

/** Seçili ilanlar varken ekranın altında görünen karşılaştırma çubuğu. */
export default function CompareBar() {
  const { ids, clear } = useCompare();
  const { pathname } = useLocation();
  if (ids.length === 0 || pathname === '/karsilastir') return null;
  return (
    <div className="comparebar" role="region" aria-label="Karşılaştırma">
      <span><strong>{ids.length}</strong> ilan seçildi {ids.length < 2 && <span className="muted">(karşılaştırmak için en az 2 seçin)</span>}</span>
      <span className="row">
        <button className="btn ghost" onClick={clear}>Temizle</button>
        {ids.length >= 2
          ? <Link className="btn primary" to={`/karsilastir?ids=${ids.join(',')}`}>Karşılaştır</Link>
          : <button className="btn primary" disabled>Karşılaştır</button>}
      </span>
    </div>
  );
}
