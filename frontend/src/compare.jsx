import { createContext, useContext, useState, useCallback } from 'react';

const Ctx = createContext({ ids: [], toggle: () => {}, clear: () => {}, has: () => false });
export const useCompare = () => useContext(Ctx);
export const MAX_COMPARE = 3;

function load() {
  try { return JSON.parse(localStorage.getItem('compare') || '[]').slice(0, MAX_COMPARE); } catch { return []; }
}

/** Karşılaştırma listesi (en fazla 3 ilan); sayfalar arasında ve yenilemede korunur. */
export function CompareProvider({ children }) {
  const [ids, setIds] = useState(load);
  const save = (next) => { setIds(next); try { localStorage.setItem('compare', JSON.stringify(next)); } catch { /* yok say */ } };
  const toggle = useCallback((id) => {
    setIds((cur) => {
      const next = cur.includes(id) ? cur.filter((x) => x !== id) : cur.length < MAX_COMPARE ? [...cur, id] : cur;
      try { localStorage.setItem('compare', JSON.stringify(next)); } catch { /* yok say */ }
      return next;
    });
  }, []);
  const clear = () => save([]);
  const has = (id) => ids.includes(id);
  return <Ctx.Provider value={{ ids, toggle, clear, has }}>{children}</Ctx.Provider>;
}
