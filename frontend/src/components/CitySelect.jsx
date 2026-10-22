import { ILLER_ALFABETIK } from '../lib/iller.js';

/** İl seçici. allowEmpty ise ilk seçenek boş değerdir ("Tüm şehirler"). */
export default function CitySelect({ value, onChange, allowEmpty = false, emptyLabel = 'Tüm şehirler', required = false, id }) {
  return (
    <select id={id} value={value} required={required} onChange={(e) => onChange(e.target.value)} autoComplete="address-level1">
      {allowEmpty ? <option value="">{emptyLabel}</option> : <option value="" disabled>İl seçin</option>}
      {ILLER_ALFABETIK.map((il) => <option key={il} value={il}>{il}</option>)}
    </select>
  );
}

/** Kategori seçici: gruplara ayrılmış (optgroup). */
export function CategorySelect({ cats, value, onChange, allowEmpty = true, emptyLabel = 'Tüm dersler', required = false, valueKey = 'slug' }) {
  const groups = cats.reduce((m, c) => ((m[c.groupName] ||= []).push(c), m), {});
  return (
    <select value={value} required={required} onChange={(e) => onChange(e.target.value)}>
      {allowEmpty ? <option value="">{emptyLabel}</option> : <option value="" disabled>Ders alanı seçin</option>}
      {Object.entries(groups).map(([g, items]) => (
        <optgroup key={g} label={g}>
          {items.map((c) => <option key={c.id} value={c[valueKey]}>{c.name}</option>)}
        </optgroup>
      ))}
    </select>
  );
}
