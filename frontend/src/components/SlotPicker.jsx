import { useEffect, useMemo, useState } from 'react';
import { api } from '../api.js';

const MAX = 3;
const dayLabel = (iso) => new Date(iso).toLocaleDateString('tr-TR', { weekday: 'long', day: 'numeric', month: 'long' });
const timeLabel = (iso) => new Date(iso).toLocaleTimeString('tr-TR', { hour: '2-digit', minute: '2-digit' });

/**
 * Öğretmenin önümüzdeki 14 gündeki boş zamanlarını gösterir; öğrenci en fazla 3 zaman seçer.
 * Zamanlar öğretmenin haftalık müsaitliğinden ve mevcut derslerinden sunucuda hesaplanır.
 */
export default function SlotPicker({ teacherId, selected, onChange }) {
  const [minutes, setMinutes] = useState(60);
  const [slots, setSlots] = useState(null);

  useEffect(() => {
    let alive = true;
    setSlots(null);
    onChange([]);
    api(`/teachers/${teacherId}/slots?days=14&minutes=${minutes}`)
      .then((r) => alive && setSlots(r)).catch(() => alive && setSlots([]));
    return () => { alive = false; };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [teacherId, minutes]);

  const days = useMemo(() => {
    const m = new Map();
    (slots || []).forEach((s) => { const k = dayLabel(s.startsAt); if (!m.has(k)) m.set(k, []); m.get(k).push(s); });
    return [...m.entries()];
  }, [slots]);

  const isOn = (s) => selected.some((x) => x.startsAt === s.startsAt);
  const toggle = (s) => {
    if (isOn(s)) onChange(selected.filter((x) => x.startsAt !== s.startsAt));
    else if (selected.length < MAX) onChange([...selected, { startsAt: s.startsAt, endsAt: s.endsAt }]);
  };

  return (
    <fieldset className="slotpicker">
      <legend>Uygun olduğunuz zamanlar (en fazla {MAX})</legend>
      <label>Ders süresi
        <select value={minutes} onChange={(e) => setMinutes(Number(e.target.value))}>
          <option value={30}>30 dakika</option><option value={60}>60 dakika</option>
          <option value={90}>90 dakika</option><option value={120}>120 dakika</option>
        </select>
      </label>
      {slots === null && <p className="muted small">Takvim yükleniyor…</p>}
      {slots && slots.length === 0 && (
        <p className="muted small">Öğretmenin önümüzdeki 14 günde boş zamanı görünmüyor. Mesajınızda uygun olduğunuz zamanları yazın, öğretmenle sohbette netleştirirsiniz.</p>
      )}
      <div className="slotdays">
        {days.map(([day, list]) => (
          <div key={day}>
            <div className="slotday">{day}</div>
            <div className="chips">
              {list.map((s) => (
                <button type="button" key={s.startsAt} className={'slot' + (isOn(s) ? ' on' : '')}
                        aria-pressed={isOn(s)} onClick={() => toggle(s)}>{timeLabel(s.startsAt)}</button>
              ))}
            </div>
          </div>
        ))}
      </div>
      {selected.length > 0 && <p className="small">Seçilen: {selected.map((s) => `${dayLabel(s.startsAt)} ${timeLabel(s.startsAt)}`).join(' · ')}</p>}
    </fieldset>
  );
}
