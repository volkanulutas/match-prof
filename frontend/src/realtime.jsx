import { createContext, useContext, useEffect, useRef, useState, useCallback } from 'react';
import { api } from './api.js';
import { useAuth } from './auth.jsx';

const Ctx = createContext({ subscribe: () => () => {}, connected: false });
export const useRealtime = () => useContext(Ctx);

/**
 * Giriş yapmış kullanıcı için tek bir WebSocket bağlantısı tutar.
 * Kopunca üstel bekleme ile yeniden bağlanır; her bağlantı için sunucudan tek kullanımlık bilet alınır.
 */
export function RealtimeProvider({ children }) {
  const { user } = useAuth();
  const [connected, setConnected] = useState(false);
  const listeners = useRef(new Set());
  const subscribe = useCallback((fn) => {
    listeners.current.add(fn);
    return () => listeners.current.delete(fn);
  }, []);

  const uid = user?.id;
  useEffect(() => {
    if (!uid) return undefined;
    let ws; let retryTimer; let pingTimer; let attempt = 0; let stopped = false;

    const retry = () => {
      attempt += 1;
      retryTimer = setTimeout(connect, Math.min(15000, 1000 * 2 ** attempt));
    };

    async function connect() {
      try {
        const { ticket } = await api('/auth/ws-ticket', { method: 'POST' });
        if (stopped) return;
        const proto = window.location.protocol === 'https:' ? 'wss' : 'ws';
        ws = new WebSocket(`${proto}://${window.location.host}/ws?ticket=${ticket}`);
        ws.onopen = () => {
          attempt = 0;
          setConnected(true);
          pingTimer = setInterval(() => { if (ws.readyState === 1) ws.send('ping'); }, 25000);
        };
        ws.onmessage = (e) => {
          if (e.data === 'pong') return;
          try { const ev = JSON.parse(e.data); listeners.current.forEach((fn) => fn(ev)); } catch { /* yok say */ }
        };
        ws.onclose = () => { clearInterval(pingTimer); setConnected(false); if (!stopped) retry(); };
        ws.onerror = () => ws.close();
      } catch {
        if (!stopped) retry();
      }
    }

    connect();
    return () => {
      stopped = true;
      clearTimeout(retryTimer); clearInterval(pingTimer);
      if (ws) ws.close();
      setConnected(false);
    };
  }, [uid]);

  return <Ctx.Provider value={{ subscribe, connected }}>{children}</Ctx.Provider>;
}
