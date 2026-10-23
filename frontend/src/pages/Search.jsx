import { useEffect, useMemo, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { api, qs } from '../api.js';
import { ListingCard, Empty, Alert } from '../components/Bits.jsx';
import CitySelect, { CategorySelect } from '../components/CitySelect.jsx';

/** Sonuç yoksa: filtreleri gevşeterek sonuç veren alternatifleri ve aynı gruptaki dolu kategorileri önerir. */
function Suggestions({ f, cats, setParams }) {
  const [alts, setAlts] = useState([]);

  const variants = useMemo(() => {
    const v = [];
    if (f.city && !f.online) v.push({ label: 'Online ders olarak ara', p: { ...f, city: '', online: true } });
    if (f.city) v.push({ label: 'Tüm şehirlerde ara', p: { ...f, city: '' } });
    if (!f.online && !f.city) v.push({ label: 'Online ders olarak ara', p: { ...f, online: true } });
    if (f.maxPrice) v.push({ label: 'Fiyat sınırını kaldır', p: { ...f, maxPrice: '' } });
    if (f.firstLessonFree) v.push({ label: 'İlk ders ücretsiz şartını kaldır', p: { ...f, firstLessonFree: false } });
    if (f.q && f.category) v.push({ label: 'Anahtar kelimeyi kaldır', p: { ...f, q: '' } });
    if (f.category) v.push({ label: 'Tüm derslerde ara', p: { ...f, category: '' } });
    return v;
  }, [f.q, f.category, f.city, f.online, f.maxPrice, f.firstLessonFree]);  // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    let alive = true;
    Promise.all(variants.map((v) => api('/listings' + qs({ ...v.p, size: 1 })).then((r) => ({ ...v, n: r.totalElements })).catch(() => ({ ...v, n: 0 }))))
      .then((r) => alive && setAlts(r.filter((x) => x.n > 0)));
    return () => { alive = false; };
  }, [variants]);

  const current = cats.find((c) => c.slug === f.category);
  const related = current ? cats.filter((c) => c.groupName === current.groupName && c.slug !== current.slug && c.listingCount > 0) : [];
  const popular = !current ? [...cats].filter((c) => c.listingCount > 0).sort((a, b) => b.listingCount - a.listingCount).slice(0, 6) : [];

  const apply = (p) => {
    const clean = {};
    Object.entries(p).forEach(([k, v]) => { if (v !== '' && v !== false && !(k === 'sort' && v === 'best') && k !== 'page') clean[k] = v; });
    setParams(clean);
  };

  return (
    <div className="card suggest">
      <h3>Bu aramayla sonuç bulamadık. Şunları deneyebilirsiniz:</h3>
      {alts.length > 0 && (
        <ul className="plain">
          {alts.map((a) => (
            <li key={a.label}><button className="linkbtn" onClick={() => apply(a.p)}>{a.label}</button> <span className="muted small">({a.n} ilan)</span></li>
          ))}
        </ul>
      )}
      {related.length > 0 && (
        <>
          <p className="muted small">Benzer dersler</p>
          <div className="chips">{related.map((c) => <Link key={c.id} className="chip link" to={`/ara?category=${c.slug}`}>{c.name} ({c.listingCount})</Link>)}</div>
        </>
      )}
      {popular.length > 0 && (
        <>
          <p className="muted small">Popüler dersler</p>
          <div className="chips">{popular.map((c) => <Link key={c.id} className="chip link" to={`/ara?category=${c.slug}`}>{c.name} ({c.listingCount})</Link>)}</div>
        </>
      )}
      {alts.length === 0 && related.length === 0 && popular.length === 0 && <p className="muted">Filtreleri temizleyip yeniden deneyin.</p>}
    </div>
  );
}

export default function Search() {
  const [params, setParams] = useSearchParams();
  const [cats, setCats] = useState([]);
  const [res, setRes] = useState(null);
  const [err, setErr] = useState('');

  const f = {
    q: params.get('q') || '', category: params.get('category') || '', city: params.get('city') || '',
    online: params.get('online') === 'true', firstLessonFree: params.get('firstLessonFree') === 'true',
    maxPrice: params.get('maxPrice') || '', sort: params.get('sort') || 'best', page: Number(params.get('page') || 0),
  };

  useEffect(() => { api('/categories').then(setCats).catch(() => {}); }, []);
  useEffect(() => {
    setRes(null); setErr('');
    api('/listings' + qs({ ...f, size: 12 })).then(setRes).catch((e) => setErr(e.message));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [params]);

  const set = (patch) => {
    const next = { ...f, page: 0, ...patch };
    const clean = {};
    Object.entries(next).forEach(([k, v]) => { if (v !== '' && v !== false && !(k === 'sort' && v === 'best') && !(k === 'page' && v === 0)) clean[k] = v; });
    setParams(clean);
  };

  return (
    <div className="wrap search">
      <aside className="filters card">
        <h3>Filtreler</h3>
        <label>Ders alanı
          <CategorySelect cats={cats} value={f.category} onChange={(v) => set({ category: v })} />
        </label>
        <label>Şehir
          <CitySelect value={f.city} onChange={(v) => set({ city: v })} allowEmpty />
        </label>
        <label className="check"><input type="checkbox" checked={f.online} onChange={(e) => set({ online: e.target.checked })} /> Online ders</label>
        <label>Anahtar kelime
          <input defaultValue={f.q} key={'q' + f.q} onKeyDown={(e) => e.key === 'Enter' && set({ q: e.target.value })}
                 onBlur={(e) => e.target.value !== f.q && set({ q: e.target.value })} placeholder="ör. TYT, konuşma, ogretmen adı" />
        </label>
        <label>En yüksek saatlik ücret (₺)
          <input type="number" min="0" defaultValue={f.maxPrice} key={'p' + f.maxPrice}
                 onBlur={(e) => e.target.value !== f.maxPrice && set({ maxPrice: e.target.value })} />
        </label>
        <label className="check"><input type="checkbox" checked={f.firstLessonFree} onChange={(e) => set({ firstLessonFree: e.target.checked })} /> İlk ders ücretsiz</label>
        <label>Sıralama
          <select value={f.sort} onChange={(e) => set({ sort: e.target.value })}>
            <option value="best">Önerilen</option>
            <option value="priceAsc">Fiyat: düşükten yükseğe</option>
            <option value="priceDesc">Fiyat: yüksekten düşüğe</option>
            <option value="newest">En yeni</option>
          </select>
        </label>
        <button className="btn ghost" onClick={() => setParams({})}>Filtreleri temizle</button>
      </aside>

      <section>
        <Alert>{err}</Alert>
        {!res && !err && <p className="muted">Öğretmenler yükleniyor… (site uykudaysa ilk açılış bir dakika sürebilir)</p>}
        {res && <p className="muted">{res.totalElements} öğretmen bulundu. Kartlardaki “Karşılaştır” kutusuyla en fazla 3 ilanı yan yana görebilirsiniz.</p>}
        {res && res.content.length === 0 && <Suggestions f={f} cats={cats} setParams={setParams} />}
        <div className="grid two">{res?.content.map((l) => <ListingCard key={l.id} l={l} />)}</div>
        {res && res.totalPages > 1 && (
          <div className="pager">
            <button className="btn ghost" disabled={f.page === 0} onClick={() => set({ page: f.page - 1 })}>Önceki</button>
            <span className="muted">Sayfa {f.page + 1} / {res.totalPages}</span>
            <button className="btn ghost" disabled={f.page + 1 >= res.totalPages} onClick={() => set({ page: f.page + 1 })}>Sonraki</button>
          </div>
        )}
      </section>
    </div>
  );
}
