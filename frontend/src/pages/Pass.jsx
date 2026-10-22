import { useEffect, useState, useCallback } from 'react';
import { api, money, dateTime } from '../api.js';
import { useAuth } from '../auth.jsx';
import { Alert } from '../components/Bits.jsx';

export default function Pass() {
  const { user, refresh } = useAuth();
  const [plans, setPlans] = useState([]);
  const [mine, setMine] = useState([]);
  const [err, setErr] = useState('');
  const [ok, setOk] = useState('');
  const load = useCallback(() => api('/subscriptions/me').then(setMine), []);
  useEffect(() => { api('/subscriptions/plans').then(setPlans); load(); }, [load]);

  const plan = plans.find((p) => (user.role === 'TEACHER' ? p.plan === 'TEACHER_PASS' : p.plan === 'STUDENT_PASS'));
  const buy = async (months) => {
    setErr(''); setOk('');
    try { await api('/subscriptions', { method: 'POST', body: { plan: plan.plan, months } }); setOk('Paket etkinleştirildi (demo ödeme).'); load(); refresh(); }
    catch (e) { setErr(e.message); }
  };
  const active = mine.find((s) => s.active);

  return (
    <div className="wrap narrowish">
      <h1 className="h1s">{plan?.title}</h1>
      <Alert>{err}</Alert>
      <Alert kind="ok">{ok}</Alert>
      {plan && (
        <div className="card stack">
          <p>{plan.benefit}</p>
          <div className="row wrapr">
            <button className="btn primary big" onClick={() => buy(1)}>1 ay · {money(plan.monthlyTry)}</button>
            {plan.yearlyTry && <button className="btn ghost big" onClick={() => buy(12)}>12 ay · {money(plan.yearlyTry)}</button>}
          </div>
          <p className="muted small">Bu bir demo: gerçek ödeme alınmaz. Fiyatlar sunucu yapılandırmasından gelir.</p>
        </div>
      )}
      {active && <Alert kind="ok">Aktif paket {dateTime(active.endsAt)} tarihine kadar geçerli.</Alert>}
      <h2>Geçmiş</h2>
      <ul className="plain">
        {mine.map((s) => <li key={s.id}>{dateTime(s.startsAt)} → {dateTime(s.endsAt)} · {money(s.amountTry)} {s.active && <span className="badge st-accepted">Aktif</span>}</li>)}
        {mine.length === 0 && <li className="muted">Henüz satın alma yok.</li>}
      </ul>
    </div>
  );
}
