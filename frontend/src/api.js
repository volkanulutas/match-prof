export async function api(path, { method = 'GET', body } = {}) {
  const token = localStorage.getItem('token');
  const res = await fetch('/api' + path, {
    method,
    headers: {
      ...(body ? { 'Content-Type': 'application/json' } : {}),
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    body: body ? JSON.stringify(body) : undefined,
  });
  if (res.status === 204) return null;
  const data = await res.json().catch(() => null);
  if (!res.ok) {
    if (res.status === 401 && token) {
      localStorage.removeItem('token');
    }
    const err = new Error(
      data?.message || (res.status === 401 ? 'Devam etmek için giriş yapın' : 'Bir hata oluştu, tekrar deneyin')
    );
    err.status = res.status;
    throw err;
  }
  return data;
}

export const qs = (obj) => {
  const p = new URLSearchParams();
  Object.entries(obj).forEach(([k, v]) => {
    if (v !== '' && v !== null && v !== undefined && v !== false) p.set(k, v);
  });
  const s = p.toString();
  return s ? '?' + s : '';
};

export const money = (v) => new Intl.NumberFormat('tr-TR').format(v) + ' ₺';
export const dateTime = (iso) =>
  new Date(iso).toLocaleString('tr-TR', { dateStyle: 'medium', timeStyle: 'short' });

export const MODES = { ONLINE: 'Online', TEACHER_HOME: 'Öğretmenin yerinde', STUDENT_HOME: 'Öğrencinin evinde' };
export const REQUEST_STATUS = {
  PENDING: 'Yanıt bekliyor', ACCEPTED: 'Kabul edildi', DECLINED: 'Reddedildi', CANCELLED: 'İptal edildi', EXPIRED: 'Süresi doldu',
};
export const SESSION_STATUS = { SCHEDULED: 'Planlandı', COMPLETED: 'Tamamlandı', CANCELLED: 'İptal edildi' };
export const DAYS = {
  MONDAY: 'Pazartesi', TUESDAY: 'Salı', WEDNESDAY: 'Çarşamba', THURSDAY: 'Perşembe',
  FRIDAY: 'Cuma', SATURDAY: 'Cumartesi', SUNDAY: 'Pazar',
};
