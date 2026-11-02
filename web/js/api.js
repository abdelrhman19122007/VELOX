// Talks to the VELOX API on the same address (the server serves this site). Errors come back as
// {status, code, message}; t() translates the codes.
const TOKEN_KEY = 'velox.token';

export const session = {
  get token() { try { return sessionStorage.getItem(TOKEN_KEY); } catch { return null; } },
  set token(v) { try { v ? sessionStorage.setItem(TOKEN_KEY, v) : sessionStorage.removeItem(TOKEN_KEY); } catch { /* private mode */ } },
  user: null,
};

export class ApiError extends Error {
  constructor(code, message, status) { super(message); this.code = code; this.status = status; }
}

export async function api(method, path, body, { form } = {}) {
  const headers = {};
  if (session.token) headers.Authorization = `Bearer ${session.token}`;
  if (body !== undefined && !form) headers['Content-Type'] = 'application/json';
  let res;
  try {
    res = await fetch(`/api${path}`, { method, headers, body: form ? body : body === undefined ? undefined : JSON.stringify(body) });
  } catch {
    throw new ApiError('NETWORK', 'Network error', 0);
  }
  const text = await res.text();
  let data = null;
  try { data = text ? JSON.parse(text) : null; } catch { data = null; }
  if (!res.ok) {
    if (res.status === 401 && session.token) { session.token = null; location.reload(); }
    throw new ApiError(data?.code || 'SERVER', data?.message || `HTTP ${res.status}`, res.status);
  }
  return data;
}

export const get = (p) => api('GET', p);
export const post = (p, b) => api('POST', p, b ?? undefined);
export const put = (p, b) => api('PUT', p, b);
export const patch = (p, b) => api('PATCH', p, b);

/** Escapes text for HTML — every value from the server goes through this (stored XSS defence in depth). */
export function esc(v) {
  return String(v ?? '').replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
}

export const money = (v) => `${Number(v ?? 0).toLocaleString('ar-EG', { maximumFractionDigits: 2 })} ج.م`;
export const when = (v) => (v ? new Date(v).toLocaleString('ar-EG', { dateStyle: 'short', timeStyle: 'short' }) : '');

const ERRORS = {
  NETWORK: 'مش قادرين نوصل للسيرفر', SERVER: 'مشكلة في السيرفر — جرّب تاني', SIGN_IN_REQUIRED: 'سجّل دخول الأول',
  FORBIDDEN: 'الحساب ده مش مسموحله', BAD_LOGIN: 'الإيميل أو كلمة السر غلط', TOO_MANY_LOGINS: 'محاولات كتير — استنى ١٠ دقايق',
  BLOCKED: 'الحساب موقوف', EMAIL_TAKEN: 'الإيميل ده عليه حساب', INVALID_REQUEST: 'راجع البيانات', INVALID_HOURS: 'المواعيد مش صحيحة',
  UNKNOWN_STORE: 'المحل مش موجود', UNKNOWN_PRODUCT: 'الصنف مش موجود', ZONE_EXISTS: 'الكود ده موجود', PROMO_EXISTS: 'الكود ده موجود',
  INVALID_PROMO: 'الكود مش صالح', BAD_STATUS: 'حالة الطلب اتغيرت — حدّث الصفحة', ORDER_NOT_FOUND: 'الطلب مش موجود',
  COMPLAINT_CLOSED: 'الشكوى مقفولة بالفعل', NOT_AN_IMAGE: 'الملف مش صورة (JPG أو PNG أو WEBP)', FILE_TOO_LARGE: 'الصورة أكبر من ٢ ميجا',
  EMPTY_FILE: 'الملف فاضي', UNKNOWN_USER: 'الحساب مش موجود',
};
export const errorText = (e) => ERRORS[e?.code] || e?.message || String(e);

export const STATUS = {
  PENDING_PAYMENT: 'مستني الدفع', PLACED: 'جديد', ACCEPTED: 'بيتجهز', READY: 'جاهز', PICKED_UP: 'مع المندوب',
  DELIVERED: 'وصل', REJECTED: 'اترفض', CANCELLED: 'اتلغى', RETURNED: 'مرتجع',
};
export const PAY = { CASH: 'كاش', VODAFONE_CASH: 'فودافون كاش', FAWRY: 'فوري', WALLET: 'المحفظة' };
export const TYPES = { FOOD: 'مطعم', GROCERY: 'سوبر ماركت', PHARMACY: 'صيدلية', CLOTHES: 'ملابس', TECH: 'إلكترونيات', OTHER: 'أخرى' };

export function toast(text, isError = false) {
  const box = document.getElementById('toast');
  box.innerHTML = `<div class="${isError ? 'err' : ''}">${esc(text)}</div>`;
  clearTimeout(toast.timer);
  toast.timer = setTimeout(() => { box.innerHTML = ''; }, 3500);
}

/** Runs an action; shows the translated error if it fails. Returns undefined on failure. */
export async function attempt(fn) {
  try { return await fn(); } catch (e) { toast(errorText(e), true); return undefined; }
}
