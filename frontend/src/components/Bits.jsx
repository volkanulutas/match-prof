import { Link } from 'react-router-dom';
import { MODES, money } from '../api.js';
import { useCompare, MAX_COMPARE } from '../compare.jsx';

export function Stars({ value, count }) {
  if (!count) return <span className="muted small">Henüz yorum yok</span>;
  return (
    <span className="stars" aria-label={`${value} / 5`}>
      <span className="star">★</span> {value.toFixed(1)} <span className="muted small">({count} yorum)</span>
    </span>
  );
}

export function CompareToggle({ id }) {
  const { ids, toggle, has } = useCompare();
  const on = has(id);
  const full = !on && ids.length >= MAX_COMPARE;
  return (
    <label className={'check cmp' + (on ? ' on' : '')} title={full ? `En fazla ${MAX_COMPARE} ilan karşılaştırılabilir` : ''}>
      <input type="checkbox" checked={on} disabled={full} onChange={() => toggle(id)} /> Karşılaştır
    </label>
  );
}

export function ListingCard({ l }) {
  const modes = [l.online && MODES.ONLINE, l.teacherHome && MODES.TEACHER_HOME, l.studentHome && MODES.STUDENT_HOME].filter(Boolean);
  return (
    <div className="card listing">
      <Link to={`/ilan/${l.id}`} className="listing-link">
        <div className="listing-top">
          <div className="avatar" aria-hidden>{l.teacherName.charAt(0)}</div>
          <div>
            <strong>{l.teacherName}</strong>
            <div className="muted small">{l.city} · {l.categoryName}</div>
          </div>
          {l.boosted && <span className="badge pro">Öne çıkan</span>}
        </div>
        <h3>{l.title}</h3>
        <Stars value={l.avgRating} count={l.reviewCount} />
        <div className="chips">
          {modes.map((m) => <span key={m} className="chip">{m}</span>)}
          {l.firstLessonFree && <span className="chip good">İlk ders ücretsiz</span>}
          {l.qualifications.some((q) => q.status === 'VERIFIED') && <span className="chip good">Doğrulanmış belge</span>}
        </div>
      </Link>
      <div className="listing-foot">
        <div className="price">{money(l.hourlyRate)}<span className="muted small"> / saat</span></div>
        <CompareToggle id={l.id} />
      </div>
    </div>
  );
}

export function Alert({ kind = 'error', children }) {
  if (!children) return null;
  return <div className={`alert ${kind}`} role={kind === 'error' ? 'alert' : 'status'}>{children}</div>;
}

export function Empty({ children }) {
  return <p className="empty">{children}</p>;
}

export function Status({ value, labels }) {
  return <span className={`badge st-${String(value).toLowerCase()}`}>{labels[value] || value}</span>;
}
