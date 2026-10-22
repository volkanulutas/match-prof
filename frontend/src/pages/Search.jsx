import { useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { api, qs } from '../api.js';
import { ListingCard, Empty, Alert } from '../components/Bits.jsx';

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
        <label>Ders veya konu
          <input defaultValue={f.q} key={'q' + f.q} onKeyDown={(e) => e.key === 'Enter' && set({ q: e.target.value })}
                 onBlur={(e) => e.target.value !== f.q && set({ q: e.target.value })} placeholder="ör. İngilizce" />
        </label>
        <label>Kategori
          <select value={f.category} onChange={(e) => set({ category: e.target.value })}>
            <option value="">Tümü</option>
            {cats.map((c) => <option key={c.id} value={c.slug}>{c.name}</option>)}
          </select>
        </label>
        <label>Şehir
          <input defaultValue={f.city} key={'c' + f.city} onBlur={(e) => e.target.value !== f.city && set({ city: e.target.value })}
                 onKeyDown={(e) => e.key === 'Enter' && set({ city: e.target.value })} placeholder="ör. Ankara" />
        </label>
        <label>En yüksek saatlik ücret (₺)
          <input type="number" min="0" defaultValue={f.maxPrice} key={'p' + f.maxPrice}
                 onBlur={(e) => e.target.value !== f.maxPrice && set({ maxPrice: e.target.value })} />
        </label>
        <label className="check"><input type="checkbox" checked={f.online} onChange={(e) => set({ online: e.target.checked })} /> Online ders</label>
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
        {!res && !err && <p className="muted">Öğretmenler yükleniyor…</p>}
        {res && <p className="muted">{res.totalElements} öğretmen bulundu</p>}
        {res && res.content.length === 0 && <Empty>Bu filtrelerle sonuç yok. Şehri veya ücret sınırını kaldırmayı deneyin.</Empty>}
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
