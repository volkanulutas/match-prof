import { createContext, useContext, useEffect, useState, useCallback } from 'react';
import { api } from './api.js';

const Ctx = createContext(null);
export const useAuth = () => useContext(Ctx);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [ready, setReady] = useState(false);

  const refresh = useCallback(async () => {
    if (!localStorage.getItem('token')) { setUser(null); setReady(true); return; }
    try { setUser(await api('/auth/me')); }
    catch { localStorage.removeItem('token'); setUser(null); }
    setReady(true);
  }, []);

  useEffect(() => { refresh(); }, [refresh]);

  const login = async (email, password) => {
    const r = await api('/auth/login', { method: 'POST', body: { email, password } });
    localStorage.setItem('token', r.token); setUser(r.user); return r.user;
  };
  const register = async (body) => {
    const r = await api('/auth/register', { method: 'POST', body });
    localStorage.setItem('token', r.token); setUser(r.user); return r.user;
  };
  const logout = () => { localStorage.removeItem('token'); setUser(null); };

  return <Ctx.Provider value={{ user, ready, login, register, logout, refresh }}>{children}</Ctx.Provider>;
}
