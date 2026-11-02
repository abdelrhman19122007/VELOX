// VELOX web dashboard: the admin (stores, menus, areas, promo codes, users, orders, complaints, numbers) and
// store staff (live orders board, menu, numbers). One page; views switch with the address hash.
import { api, get, post, put, patch, esc, money, when, errorText, toast, attempt, session, STATUS, PAY, TYPES } from './api.js';
import { parseCsv, MENU_TEMPLATE } from './csv.js';

const $ = (s, root = document) => root.querySelector(s);
const view = $('#view');
const SECTION_RE = /^[A-Z_]{2,20}$/;
const SIZES = { SMALL: 'صغير', MEDIUM: 'وسط', LARGE: 'كبير (توكتوك)' };
const SECTIONS = ['MEALS', 'SANDWICHES', 'SOUPS', 'SALADS', 'APPETIZERS', 'GRILL_PAN', 'TAGINES', 'TRAYS', 'PASTA_RICE', 'PIZZA', 'FATEER', 'GRILLS', 'SIDES', 'DRINKS', 'DESSERTS', 'BREAKFAST', 'OFFERS',
  'MEDICINE', 'CARE', 'BABY', 'DAIRY', 'BAKERY', 'VEGETABLES', 'CLEANING', 'GAS', 'OTHER'];

const ADMIN_NAV = [
  ['dashboard', '📊', 'الرئيسية'], ['orders', '🧾', 'الطلبات'], ['stores', '🏪', 'المحلات والمنيو'],
  ['zones', '🗺️', 'المناطق والأسعار'], ['promos', '🏷️', 'أكواد الخصم'], ['users', '👥', 'المستخدمين'], ['complaints', '🎧', 'الشكاوى'],
];
const STORE_NAV = [['board', '🧾', 'الطلبات'], ['menu', '📋', 'المنيو'], ['stats', '📊', 'النهارده']];

// ---------- start ----------
(async function start() {
  applyTheme();
  $('#login-form').addEventListener('submit', login);
  $('#logout').addEventListener('click', () => { post('/auth/logout-everywhere').catch(() => {}); session.token = null; location.reload(); });
  $('#theme').addEventListener('click', toggleTheme);
  $('#menu-btn').addEventListener('click', () => $('#side').classList.toggle('open'));
  window.addEventListener('hashchange', route);
  if (!session.token) return showLogin();
  try {
    session.user = await get('/auth/me');
  } catch {
    session.token = null;
    return showLogin();
  }
  if (!['ADMIN', 'RESTAURANT'].includes(session.user.role)) {
    session.token = null;
    showLogin();
    return toast('اللوحة دي للإدارة وأصحاب المحلات بس. العملاء والمناديب يستخدموا التطبيق.', true);
  }
  $('#login').classList.add('hidden');
  $('#app').classList.remove('hidden');
  $('#who').textContent = `${session.user.name} · ${session.user.role === 'ADMIN' ? 'الإدارة' : 'المحل'}`;
  live();
  route();
})();

function showLogin() {
  $('#app').classList.add('hidden');
  $('#login').classList.remove('hidden');
}

async function login(e) {
  e.preventDefault();
  const r = await attempt(() => post('/auth/login', { email: $('#email').value.trim(), password: $('#password').value }));
  if (!r) return;
  session.token = r.token;
  location.reload();
}

function applyTheme() {
  let t = null;
  try { t = localStorage.getItem('velox.theme'); } catch { /* storage blocked */ }
  if (t) document.documentElement.dataset.theme = t;
}
function toggleTheme() {
  const dark = getComputedStyle(document.documentElement).colorScheme === 'dark';
  document.documentElement.dataset.theme = dark ? 'light' : 'dark';
  try { localStorage.setItem('velox.theme', document.documentElement.dataset.theme); } catch { /* ignore */ }
}

// ---------- routing ----------
const routes = {
  dashboard: adminDashboard, orders: adminOrders, stores: adminStores, store: adminStoreMenu, zones: adminZones,
  promos: adminPromos, users: adminUsers, complaints: adminComplaints,
  board: storeBoard, menu: storeMenu, stats: storeStats,
};

function route() {
  const isAdmin = session.user.role === 'ADMIN';
  const nav = isAdmin ? ADMIN_NAV : STORE_NAV;
  const [name, arg] = (location.hash.slice(1) || nav[0][0]).split('/');
  const allowed = isAdmin ? [...ADMIN_NAV.map((n) => n[0]), 'store'] : STORE_NAV.map((n) => n[0]);
  const page = allowed.includes(name) ? name : nav[0][0];
  $('#nav').innerHTML = nav.map(([k, icon, label]) =>
    `<a href="#${k}" class="${k === page || (page === 'store' && k === 'stores') ? 'active' : ''}">${icon} ${esc(label)}</a>`).join('');
  $('#side').classList.remove('open');
  $('#actions').innerHTML = '';
  view.innerHTML = '<p class="muted">ثواني…</p>';
  routes[page](arg).catch((e) => { view.innerHTML = `<div class="card empty">${esc(errorText(e))}</div>`; });
}

function setTitle(t) { $('#title').textContent = t; document.title = `${t} · ڤيلوكس`; }
function action(label, fn, cls = '') {
  const b = document.createElement('button');
  b.className = `btn sm ${cls}`;
  b.textContent = label;
  b.addEventListener('click', fn);
  $('#actions').append(b);
}

// ---------- live updates (long polling; the token stays in the Authorization header) ----------
const liveListeners = new Set();
async function live() {
  let after = null;
  for (;;) {
    try {
      const r = await get(`/events/poll${after == null ? '' : `?after=${after}`}`);
      after = r.last;
      for (const e of r.events) liveListeners.forEach((fn) => fn(e.event, e.data));
    } catch {
      await new Promise((ok) => setTimeout(ok, 5000));
    }
  }
}
function onLive(fn) {
  liveListeners.clear();
  liveListeners.add(fn);
}

// ---------- dialog ----------
function dialog(title, fields, onSave, saveLabel = 'حفظ') {
  const d = $('#dialog');
  const f = $('#dialog-form');
  f.innerHTML = `<h2>${esc(title)}</h2>${fields}<div class="dialog-actions">
      <button class="btn ghost" value="cancel" formnovalidate>إلغاء</button><button class="btn" value="ok">${esc(saveLabel)}</button></div>`;
  f.onsubmit = async (e) => {
    if (e.submitter?.value !== 'ok') return;
    e.preventDefault();
    const data = Object.fromEntries(new FormData(f).entries());
    const ok = await attempt(() => onSave(data, f));
    if (ok !== undefined) d.close();
  };
  d.showModal();
  return f;
}
const field = (name, label, attrs = '', value = '') =>
  `<label for="f-${name}">${esc(label)}</label><input id="f-${name}" name="${name}" value="${esc(value)}" ${attrs}>`;
const select = (name, label, options, value = '') =>
  `<label for="f-${name}">${esc(label)}</label><select id="f-${name}" name="${name}">${Object.entries(options)
    .map(([k, v]) => `<option value="${esc(k)}" ${k === value ? 'selected' : ''}>${esc(v)}</option>`).join('')}</select>`;
const num = (v) => (v === '' || v == null ? null : Number(v));
const text = (v) => (v == null || String(v).trim() === '' ? null : String(v).trim());

function chip(status) { return `<span class="chip st-${esc(status)}">${esc(STATUS[status] || status)}</span>`; }
function orderStore(o) { return o.store ? o.store.nameAr : 'هاتلي أي حاجة'; }
function itemsHtml(o) {
  if (o.kind === 'ERRAND') return `<div>🛒 ${esc(o.errandDetails)}</div>`;
  return o.items.map((i) => `<div>${i.qty}× ${esc(i.nameAr)}${i.note ? ` <span class="muted">“${esc(i.note)}”</span>` : ''}</div>`).join('');
}

// ================= ADMIN =================
async function adminDashboard() {
  setTitle('الرئيسية');
  const [s, orders] = await Promise.all([get('/admin/stats'), get('/admin/orders')]);
  const tiles = [
    ['🧾', s.ordersToday, 'طلبات النهارده'], ['✅', s.deliveredToday, 'وصلت'], ['❌', s.cancelledToday, 'اتلغت / اترفضت'],
    ['💰', money(s.salesToday), 'المبيعات'], ['📈', money(s.commissionToday), 'عمولة المنصة'], ['🛵', s.driversOnline, 'مناديب أونلاين'],
    ['🎧', s.openComplaints, 'شكاوى مفتوحة'],
  ];
  view.innerHTML = `<div class="grid stats">${tiles.map(([i, v, l]) => `<div class="card stat"><span>${i}</span><b>${esc(v)}</b><span class="muted">${esc(l)}</span></div>`).join('')}</div>
    <h2 style="margin-top:24px">آخر الطلبات</h2>${ordersTable(orders.slice(0, 10), false)}`;
  onLive((ev) => { if (ev === 'order' && location.hash.startsWith('#dashboard')) route(); });
}

function ordersTable(list, withActions) {
  if (!list.length) return '<div class="card empty">مفيش طلبات</div>';
  return `<div class="table-wrap"><table><thead><tr><th>الطلب</th><th>المحل</th><th>العميل</th><th>المنطقة</th><th>الإجمالي</th><th>الدفع</th><th>الحالة</th><th>الوقت</th>${withActions ? '<th></th>' : ''}</tr></thead><tbody>
    ${list.map((o) => `<tr><td><b>${esc(o.code)}</b></td><td>${esc(orderStore(o))}</td><td>${esc(o.customerName)}<div class="small muted">${esc(o.phone)}</div></td>
      <td>${esc(o.zone)}</td><td>${money(o.total)}</td><td>${esc(PAY[o.paymentMethod] || o.paymentMethod)}</td><td>${chip(o.status)}</td><td class="small">${when(o.createdAt)}</td>
      ${withActions ? `<td>${['PENDING_PAYMENT', 'PLACED', 'ACCEPTED', 'READY', 'PICKED_UP'].includes(o.status) ? `<button class="btn danger sm" data-cancel="${esc(o.code)}">إلغاء</button>` : ''}</td>` : ''}</tr>`).join('')}
    </tbody></table></div>`;
}

async function adminOrders() {
  setTitle('الطلبات');
  const status = new URLSearchParams(location.hash.split('?')[1] || '').get('status') || '';
  const list = await get(`/admin/orders${status ? `?status=${encodeURIComponent(status)}` : ''}`);
  view.innerHTML = `<div class="row" style="margin-bottom:14px;max-width:320px">${select('status', 'الحالة', { '': 'الكل', ...STATUS }, status)}</div>${ordersTable(list, true)}`;
  $('#f-status').addEventListener('change', (e) => { location.hash = `orders?status=${e.target.value}`; });
  view.querySelectorAll('[data-cancel]').forEach((b) => b.addEventListener('click', () =>
    dialog(`إلغاء ${b.dataset.cancel}`, field('reason', 'السبب (هيظهر للعميل)', 'required maxlength="200"'),
      (d) => post(`/admin/orders/${b.dataset.cancel}/cancel`, { reason: d.reason.trim() }).then(() => { toast('اتلغى'); route(); }), 'إلغاء الطلب')));
  onLive((ev) => { if (ev === 'order' && location.hash.startsWith('#orders')) route(); });
}

async function adminStores() {
  setTitle('المحلات والمنيو');
  action('+ محل جديد', () => storeDialog());
  const stores = await get('/stores');
  view.innerHTML = stores.length ? `<div class="table-wrap"><table><thead><tr><th>المحل</th><th>النوع</th><th>المواعيد</th><th>التقييم</th><th>الحالة</th><th></th></tr></thead><tbody>
    ${stores.map((s) => `<tr><td><b>${esc(s.nameAr)}</b><div class="small muted">${esc(s.name)} · ${esc(s.id)}</div></td><td>${esc(TYPES[s.type] || s.type)}</td>
      <td>${s.opens ? `${esc(s.opens)} – ${esc(s.closes)}` : 'طول اليوم'}</td><td>${s.rating ? `⭐ ${Number(s.rating).toFixed(1)} (${s.ratingCount})` : '—'}</td>
      <td>${s.accepting ? (s.openNow ? '<span class="chip ok">مفتوح</span>' : '<span class="chip">مقفول دلوقتي</span>') : '<span class="chip bad">موقف الطلبات</span>'}</td>
      <td><a class="btn outline sm" href="#store/${esc(s.id)}">المنيو والتعديل</a></td></tr>`).join('')}
    </tbody></table></div>` : `<div class="card empty">لسه مفيش محلات. اضغط "محل جديد" وضيف أول مطعم.</div>`;
}

function storeDialog(existing) {
  const s = existing || {};
  const edit = Boolean(existing);
  const f = dialog(edit ? `تعديل ${s.nameAr}` : 'محل جديد', `
    ${edit ? '' : select('type', 'النوع', TYPES, 'FOOD')}
    <div class="row">${field('nameAr', 'الاسم بالعربي', 'required maxlength="80"', s.nameAr)}${field('name', 'الاسم بالإنجليزي', 'required maxlength="80"', s.name)}</div>
    ${field('cuisine', 'التصنيف (بالإنجليزي حروف كبيرة، مثلاً KOSHARY أو PIZZA أو PHARMACY)', 'required pattern="[A-Z_]{2,20}"', s.cuisine || '')}
    <div class="row">${field('taglineAr', 'وصف قصير بالعربي', 'maxlength="120"', s.taglineAr)}${field('tagline', 'وصف قصير بالإنجليزي', 'maxlength="120"', s.tagline)}</div>
    <div class="row">${field('phone', 'تليفون المحل', 'pattern="(01[0125][0-9]{8}|0[0-9]{8,9})"', s.phone)}${field('address', 'العنوان', 'maxlength="300"', s.address)}</div>
    <div class="row">${field('opens', 'بيفتح (مثلاً 10:00)', 'type="time"', s.opens || '')}${field('closes', 'بيقفل (مثلاً 02:00)', 'type="time"', s.closes || '')}</div>
    <p class="small muted">سيب المواعيد فاضية لو المحل شغال طول اليوم. بعد نص الليل عادي (10:00 لـ 02:00).</p>
    ${field('commission', 'عمولة المنصة ٪', 'type="number" min="0" max="50" step="0.5"', s.commission ?? 10)}
    ${imageField(s.photoUrl)}
    ${edit ? '' : `<div class="row">${field('lat', 'خط العرض (اختياري)', 'type="number" step="any" min="22" max="32"')}${field('lng', 'خط الطول (اختياري)', 'type="number" step="any" min="24" max="37"')}</div>
      <h3 style="margin-top:14px">حساب صاحب المحل</h3>
      <div class="row">${field('accountEmail', 'إيميل الدخول', 'type="email" required')}${field('accountPassword', 'كلمة سر (٨ حروف على الأقل)', 'type="text" minlength="8" required')}</div>`}`,
  async (d) => {
    if (!SECTION_RE.test(d.cuisine)) throw new Error('التصنيف لازم يكون حروف إنجليزي كبيرة');
    const common = { cuisine: d.cuisine, name: d.name.trim(), nameAr: d.nameAr.trim(), taglineAr: text(d.taglineAr), tagline: text(d.tagline), phone: text(d.phone) ?? '', address: text(d.address) ?? '', opens: d.opens || '', closes: d.closes || '', commission: num(d.commission), photoUrl: text(d.photoUrl) };
    if (edit) {
      await patch(`/admin/stores/${s.id}`, common);
      toast('اتحفظ');
    } else {
      const r = await post('/admin/stores', { ...common, type: d.type, lat: num(d.lat), lng: num(d.lng), accountEmail: d.accountEmail.trim(), accountPassword: d.accountPassword });
      toast(`اتضاف المحل ${r.id} — صاحب المحل يدخل بالإيميل وكلمة السر دي`);
    }
    route();
  });
  wireImageField(f);
}

function imageField(url) {
  return `<label>صورة (JPG / PNG / WEBP، أقل من ٢ ميجا)</label>
    <div class="row"><input type="file" accept="image/jpeg,image/png,image/webp" data-upload><input name="photoUrl" placeholder="أو رابط الصورة" value="${esc(url || '')}"></div>`;
}
function wireImageField(form) {
  const input = form.querySelector('[data-upload]');
  if (!input) return;
  input.addEventListener('change', async () => {
    const file = input.files[0];
    if (!file) return;
    const body = new FormData();
    body.append('file', file);
    const r = await attempt(() => api('POST', '/uploads', body, { form: true }));
    if (r) { form.querySelector('[name=photoUrl]').value = r.url; toast('الصورة اترفعت'); }
  });
}

async function adminStoreMenu(id) {
  const d = await get(`/stores/${encodeURIComponent(id)}`);
  const s = d.store;
  setTitle(s.nameAr);
  action('تعديل بيانات المحل', () => storeDialog({ ...s, phone: d.phone, address: d.address }), 'outline');
  action(s.accepting ? 'إيقاف الطلبات' : 'تشغيل الطلبات', async () => {
    await attempt(() => patch(`/admin/stores/${s.id}`, { accepting: !s.accepting }));
    route();
  }, 'outline');
  action('+ صنف', () => productDialog(s.id));
  action('⬆ استيراد منيو (Excel / CSV)', () => importDialog(s.id), 'green');
  const products = d.sections.flatMap((sec) => sec.products);
  view.innerHTML = `<div class="card" style="margin-bottom:14px"><div class="row">
      <div><b>${esc(TYPES[s.type] || s.type)}</b> · ${esc(s.cuisine)}</div>
      <div>${s.opens ? `🕒 ${esc(s.opens)} – ${esc(s.closes)}` : '🕒 طول اليوم'}</div>
      <div>${d.phone ? `📞 ${esc(d.phone)}` : ''}</div><div>${d.address ? `📍 ${esc(d.address)}` : ''}</div>
      <div>${s.accepting ? '<span class="chip ok">بيستقبل طلبات</span>' : '<span class="chip bad">موقف الطلبات</span>'}</div></div></div>
    ${products.length ? `<div class="table-wrap"><table><thead><tr><th>الصنف</th><th>القسم</th><th>الحجم</th><th>السعر</th><th>متاح</th><th></th></tr></thead><tbody>
      ${products.map((p) => `<tr><td><b>${esc(p.nameAr)}</b><div class="small muted">${esc(p.name)}${p.description ? ` — ${esc(p.description)}` : ''}</div></td>
        <td>${esc(p.section)}</td><td>${esc(SIZES[p.size] || p.size)}</td><td>${money(p.price)}</td><td>${p.available ? '✅' : '⛔'}</td>
        <td><button class="btn outline sm" data-edit="${esc(p.id)}">تعديل</button></td></tr>`).join('')}
      </tbody></table></div>` : '<div class="card empty">المنيو فاضي. ضيف أصناف واحد واحد، أو استورد المنيو كله من ملف Excel.</div>'}`;
  view.querySelectorAll('[data-edit]').forEach((b) => b.addEventListener('click', () => productDialog(s.id, products.find((p) => p.id === b.dataset.edit))));
}

function productDialog(storeId, p) {
  const edit = Boolean(p);
  const v = p || { size: 'MEDIUM', section: 'MEALS' };
  const f = dialog(edit ? `تعديل ${v.nameAr}` : 'صنف جديد', `
    <div class="row">${field('nameAr', 'الاسم بالعربي', 'required maxlength="120"', v.nameAr)}${field('name', 'الاسم بالإنجليزي', 'required maxlength="120"', v.name)}</div>
    <div class="row">${field('description', 'الوصف بالعربي (اختياري)', 'maxlength="500"', v.description)}${field('descriptionEn', 'الوصف بالإنجليزي (اختياري)', 'maxlength="500"', v.descriptionEn)}</div>
    <div class="row">${field('price', 'السعر (ج.م)', 'type="number" min="1" step="0.5" required', v.price)}
      ${select('size', 'الحجم', SIZES, v.size)}
      <div><label for="f-section">القسم</label><input id="f-section" name="section" list="sections" required pattern="[A-Z_]{2,20}" value="${esc(v.section)}"></div></div>
    <datalist id="sections">${SECTIONS.map((x) => `<option value="${x}">`).join('')}</datalist>
    ${imageField(v.photoUrl)}`,
  async (d) => {
    const body = { name: d.name.trim(), nameAr: d.nameAr.trim(), description: text(d.description), descriptionEn: text(d.descriptionEn), price: Number(d.price), size: d.size, section: d.section.trim().toUpperCase(), photoUrl: text(d.photoUrl) };
    if (edit) await put(`/admin/products/${p.id}`, body); else await post(`/admin/stores/${storeId}/products`, body);
    toast('اتحفظ');
    route();
  });
  wireImageField(f);
}

function importDialog(storeId) {
  const f = dialog('استيراد المنيو من ملف', `
    <p>احفظ شيت Excel بصيغة <b>CSV UTF-8</b> وفيه الأعمدة دي (أول سطر أسماء الأعمدة):</p>
    <p class="small"><code>name_ar, name_en, price, section, size, description</code> — الحجم: SMALL أو MEDIUM أو LARGE (الأنبوبة والكرتونة LARGE).</p>
    <p><a href="#" id="tpl">⬇ نزّل ملف مثال</a></p>
    <input type="file" accept=".csv,text/csv" id="csv-file" required>
    <div id="csv-preview" class="small muted" style="margin-top:10px"></div>`,
  async () => {
    const rows = f._rows || [];
    if (!rows.length) throw new Error('اختار ملف فيه أصناف');
    let ok = 0;
    const failed = [];
    for (const [i, r] of rows.entries()) {
      try {
        await post(`/admin/stores/${storeId}/products`, r);
        ok++;
      } catch (e) {
        failed.push(`سطر ${i + 2}: ${errorText(e)}`);
      }
    }
    toast(`اتضاف ${ok} صنف${failed.length ? ` — ${failed.length} فيهم مشكلة` : ''}`, failed.length > 0);
    if (failed.length) console.warn(failed.join('\n'));
    route();
  }, 'استيراد');
  $('#tpl', f).addEventListener('click', (e) => {
    e.preventDefault();
    const blob = new Blob(['﻿' + MENU_TEMPLATE], { type: 'text/csv;charset=utf-8' });
    const a = Object.assign(document.createElement('a'), { href: URL.createObjectURL(blob), download: 'velox-menu-template.csv' });
    a.click();
    URL.revokeObjectURL(a.href);
  });
  $('#csv-file', f).addEventListener('change', async (e) => {
    const { rows, errors } = parseCsv(await e.target.files[0].text());
    f._rows = rows;
    $('#csv-preview', f).innerHTML = `${rows.length} صنف جاهز${errors.length ? `<br>⚠ ${errors.map(esc).join('<br>')}` : ''}
      ${rows.slice(0, 5).map((r) => `<div>• ${esc(r.nameAr)} — ${esc(r.price)} ج.م (${esc(r.section)})</div>`).join('')}`;
  });
}

async function adminZones() {
  setTitle('المناطق والأسعار');
  action('+ منطقة / قرية', () => zoneDialog());
  const zones = await get('/admin/zones');
  view.innerHTML = `<p class="muted">سعر التوصيل والوقت المتوقع لكل منطقة. القرى اللي مش متاحة متظهرش للعملاء.</p>
    <div class="table-wrap"><table><thead><tr><th>المنطقة</th><th>الكود</th><th>النوع</th><th>التوصيل</th><th>الوقت</th><th>متاحة</th><th></th></tr></thead><tbody>
    ${zones.map((z) => `<tr><td><b>${esc(z.nameAr)}</b><div class="small muted">${esc(z.nameEn)}</div></td><td>${esc(z.code)}</td><td>${z.kind === 'CITY' ? 'مدينة' : 'قرية'}</td>
      <td>${money(z.fee)}</td><td>${esc(z.etaMinutes)} د</td><td>${z.active ? '✅' : '⛔'}</td><td><button class="btn outline sm" data-z="${esc(z.code)}">تعديل</button></td></tr>`).join('')}
    </tbody></table></div>`;
  view.querySelectorAll('[data-z]').forEach((b) => b.addEventListener('click', () => zoneDialog(zones.find((z) => z.code === b.dataset.z))));
}

function zoneDialog(z) {
  const edit = Boolean(z);
  const v = z || { kind: 'VILLAGE', fee: 25, etaMinutes: 35, active: true };
  dialog(edit ? `تعديل ${v.nameAr}` : 'منطقة جديدة', `
    ${field('code', 'الكود (إنجليزي كبير، مثلاً MIT_ASSAS)', `required pattern="[A-Z][A-Z0-9_]{1,29}" ${edit ? 'readonly' : ''}`, v.code)}
    <div class="row">${field('nameAr', 'الاسم بالعربي', 'required maxlength="60"', v.nameAr)}${field('nameEn', 'الاسم بالإنجليزي', 'required maxlength="60"', v.nameEn)}</div>
    <div class="row">${select('kind', 'النوع', { CITY: 'مدينة', VILLAGE: 'قرية' }, v.kind)}${field('fee', 'سعر التوصيل', 'type="number" min="0" max="500" required', v.fee)}${field('etaMinutes', 'الوقت (دقايق)', 'type="number" min="5" max="240" required', v.etaMinutes)}</div>
    <div class="row">${select('active', 'متاحة؟', { true: 'أيوه', false: 'لأ' }, String(v.active))}${field('sortOrder', 'الترتيب', 'type="number"', v.sortOrder ?? 0)}</div>`,
  async (d) => {
    const body = { code: d.code.trim(), nameAr: d.nameAr.trim(), nameEn: d.nameEn.trim(), kind: d.kind, fee: Number(d.fee), etaMinutes: Number(d.etaMinutes), active: d.active === 'true', sortOrder: Number(d.sortOrder || 0) };
    if (edit) await put(`/admin/zones/${v.code}`, body); else await post('/admin/zones', body);
    toast('اتحفظ');
    route();
  });
}

async function adminPromos() {
  setTitle('أكواد الخصم');
  action('+ كود جديد', promoDialog);
  const list = await get('/admin/promos');
  const kind = { PERCENT: 'نسبة ٪', FIXED: 'مبلغ ثابت', FREE_DELIVERY: 'توصيل مجاني' };
  view.innerHTML = `<div class="table-wrap"><table><thead><tr><th>الكود</th><th>النوع</th><th>القيمة</th><th>أقل طلب</th><th>الاستخدام</th><th>ينتهي</th><th>لعميل معيّن</th></tr></thead><tbody>
    ${list.map((p) => `<tr><td><b>${esc(p.code)}</b></td><td>${esc(kind[p.kind] || p.kind)}</td><td>${p.kind === 'PERCENT' ? `${esc(p.value)}٪` : p.kind === 'FIXED' ? money(p.value) : '—'}</td>
      <td>${money(p.minOrder)}</td><td>${esc(p.usedCount)}${p.maxUses ? ` / ${esc(p.maxUses)}` : ''}</td><td class="small">${p.expiresAt ? when(p.expiresAt) : 'مفيش'}</td><td>${p.userId ? 'تعويض' : '—'}</td></tr>`).join('')}
    </tbody></table></div>`;
}

function promoDialog() {
  dialog('كود خصم جديد', `
    ${field('code', 'الكود (إنجليزي كبير وأرقام)', 'required pattern="[A-Z0-9]{3,20}"')}
    <div class="row">${select('kind', 'النوع', { PERCENT: 'نسبة ٪', FIXED: 'مبلغ ثابت', FREE_DELIVERY: 'توصيل مجاني' })}${field('value', 'القيمة', 'type="number" min="0" max="1000" required', 10)}</div>
    <div class="row">${field('minOrder', 'أقل قيمة طلب', 'type="number" min="0"', 0)}${field('maxUses', 'أقصى عدد استخدام (اختياري)', 'type="number" min="1"')}</div>
    ${field('expiresAt', 'ينتهي في (اختياري)', 'type="datetime-local"')}
    <p class="small muted">كل عميل يستخدم الكود مرة واحدة بس.</p>`,
  async (d) => {
    await post('/admin/promos', { code: d.code.trim().toUpperCase(), kind: d.kind, value: Number(d.value), minOrder: num(d.minOrder), maxUses: num(d.maxUses), expiresAt: d.expiresAt ? new Date(d.expiresAt).toISOString() : null });
    toast('اتضاف الكود');
    route();
  });
}

async function adminUsers() {
  setTitle('المستخدمين');
  const role = new URLSearchParams(location.hash.split('?')[1] || '').get('role') || '';
  const list = await get(`/admin/users${role ? `?role=${role}` : ''}`);
  const roles = { '': 'الكل', CUSTOMER: 'عملاء', DRIVER: 'مناديب', RESTAURANT: 'محلات', ADMIN: 'إدارة' };
  view.innerHTML = `<div class="row" style="margin-bottom:14px;max-width:320px">${select('role', 'النوع', roles, role)}</div>
    <div class="table-wrap"><table><thead><tr><th>الاسم</th><th>الإيميل</th><th>الموبايل</th><th>النوع</th><th>الحالة</th><th></th></tr></thead><tbody>
    ${list.map((u) => `<tr><td><b>${esc(u.name)}</b></td><td>${esc(u.email)}</td><td>${esc(u.phone)}</td><td>${esc(roles[u.role] || u.role)}</td>
      <td>${u.blocked ? '<span class="chip bad">موقوف</span>' : '<span class="chip ok">شغال</span>'}</td>
      <td class="row" style="flex-wrap:nowrap">${u.id === session.user.id ? '' : `<button class="btn outline sm" data-block="${esc(u.id)}" data-v="${!u.blocked}">${u.blocked ? 'تفعيل' : 'إيقاف'}</button>
        <button class="btn ghost sm" data-reset="${esc(u.id)}">كلمة سر مؤقتة</button>`}</td></tr>`).join('')}
    </tbody></table></div>`;
  $('#f-role').addEventListener('change', (e) => { location.hash = `users?role=${e.target.value}`; });
  view.querySelectorAll('[data-block]').forEach((b) => b.addEventListener('click', async () => {
    if (await attempt(() => patch(`/admin/users/${b.dataset.block}`, { blocked: b.dataset.v === 'true' })) !== undefined) route();
  }));
  view.querySelectorAll('[data-reset]').forEach((b) => b.addEventListener('click', async () => {
    if (!confirm('تعمل كلمة سر مؤقتة؟ الأجهزة المفتوحة على الحساب هتقفل.')) return;
    const r = await attempt(() => post(`/admin/users/${b.dataset.reset}/reset-password`));
    if (r) dialog('كلمة السر المؤقتة', `<p>ادّيها لصاحب الحساب وخليه يغيّرها من الإعدادات. مش هتظهر تاني.</p><p style="font-size:24px;font-weight:900;direction:ltr;text-align:center">${esc(r.password)}</p>`, async () => true, 'تمام');
  }));
  action('+ حساب إدارة أو مندوب', staffDialog);
}

function staffDialog() {
  dialog('حساب جديد', `
    <div class="row">${field('name', 'الاسم', 'required minlength="2" maxlength="60"')}${select('role', 'النوع', { DRIVER: 'مندوب', ADMIN: 'إدارة' }, 'DRIVER')}</div>
    <div class="row">${field('email', 'الإيميل', 'type="email" required')}${field('phone', 'الموبايل', 'required pattern="01[0125][0-9]{8}"')}</div>
    <p class="small muted">هيتعمل بكلمة سر مؤقتة تظهرلك مرة واحدة. حساب الإدارة يقدر يعمل كل حاجة، فاعمله للناس اللي واثق فيهم بس.</p>`,
  async (d) => {
    const r = await post('/admin/users', { name: d.name.trim(), email: d.email.trim(), phone: d.phone.trim(), role: d.role });
    route();
    dialog('اتعمل الحساب', `<p>الإيميل: <b style="direction:ltr">${esc(r.email)}</b></p><p>كلمة السر المؤقتة (مش هتظهر تاني):</p><p style="font-size:24px;font-weight:900;direction:ltr;text-align:center">${esc(r.password)}</p>`, async () => true, 'تمام');
  });
}

async function adminComplaints() {
  setTitle('الشكاوى');
  const list = await get('/admin/complaints');
  view.innerHTML = list.length ? `<div class="table-wrap"><table><thead><tr><th>الشكوى</th><th>الطلب</th><th>التفاصيل</th><th>الحالة</th><th>الوقت</th><th></th></tr></thead><tbody>
    ${list.map((c) => `<tr><td><b>${esc(c.code)}</b></td><td>${esc(c.orderCode)}</td><td>${esc(c.details)}${c.adminNote ? `<div class="small muted">💬 ${esc(c.adminNote)}</div>` : ''}${c.compensationCode ? `<div class="small">🎁 ${esc(c.compensationCode)}</div>` : ''}</td>
      <td>${c.status === 'OPEN' ? '<span class="chip st-PENDING_PAYMENT">مفتوحة</span>' : '<span class="chip ok">مقفولة</span>'}</td><td class="small">${when(c.at)}</td>
      <td>${c.status === 'OPEN' ? `<button class="btn sm" data-close="${esc(c.code)}">قفل</button>` : ''}</td></tr>`).join('')}
    </tbody></table></div>` : '<div class="card empty">مفيش شكاوى 🎉</div>';
  view.querySelectorAll('[data-close]').forEach((b) => b.addEventListener('click', () =>
    dialog(`قفل ${b.dataset.close}`, `${field('note', 'ملاحظة للعميل (اختياري)', 'maxlength="500"')}${field('compensation', 'تعويض بكود خصم للعميل ده بس (ج.م، أقصى ٢٠٠، اختياري)', 'type="number" min="0" max="200"')}`,
      async (d) => { await post(`/admin/complaints/${b.dataset.close}/close`, { note: text(d.note), compensation: num(d.compensation) }); toast('اتقفلت'); route(); }, 'قفل الشكوى')));
}

// ================= STORE STAFF =================
async function storeBoard() {
  setTitle('الطلبات');
  const [active, done] = await Promise.all([get('/restaurant/orders?active=true'), get('/restaurant/orders?active=false')]);
  const col = (title, list, buttons) => `<div><h3>${esc(title)} (${list.length})</h3>${list.length ? list.map((o) => `
    <div class="card order-card"><div class="row"><b style="flex:0">${esc(o.code)}</b><span class="small muted">${when(o.createdAt)}</span><span style="flex:0">${chip(o.status)}</span></div>
      ${o.scheduledFor ? `<div class="small" style="color:var(--tertiary)">⏰ ميعاد: ${when(o.scheduledFor)}</div>` : ''}
      <div class="items">${itemsHtml(o)}</div>
      <div class="row"><span>${o.paymentMethod === 'CASH' ? '💵 كاش عند الاستلام' : '✅ مدفوع أونلاين'}</span><b style="flex:0">${money(o.total)}</b></div>
      ${o.driver ? `<div class="small">🛵 ${esc(o.driver.name)}</div>` : ''}
      <div class="row">${buttons(o)}</div></div>`).join('') : '<div class="card empty">—</div>'}</div>`;
  view.innerHTML = `<div class="kanban">
    ${col('جديدة', active.filter((o) => o.status === 'PLACED'), (o) => `<button class="btn green" data-act="accept" data-code="${esc(o.code)}">قبول</button><button class="btn outline" data-reject="${esc(o.code)}">رفض</button>`)}
    ${col('بتتجهز', active.filter((o) => o.status === 'ACCEPTED'), (o) => `<button class="btn" data-act="ready" data-code="${esc(o.code)}">جاهز</button>`)}
    ${col('جاهزة / مع المندوب', active.filter((o) => ['READY', 'PICKED_UP'].includes(o.status)), () => '')}
    ${col('خلصت', done.slice(0, 15), () => '')}</div>`;
  view.querySelectorAll('[data-act]').forEach((b) => b.addEventListener('click', async () => {
    b.disabled = true;
    if (await attempt(() => post(`/restaurant/orders/${b.dataset.code}/${b.dataset.act}`)) !== undefined) route(); else b.disabled = false;
  }));
  view.querySelectorAll('[data-reject]').forEach((b) => b.addEventListener('click', () =>
    dialog(`رفض ${b.dataset.reject}`, `${select('preset', 'السبب', { 'الصنف خلص': 'الصنف خلص', 'قافلين': 'قافلين', 'ضغط شغل': 'ضغط شغل', '': 'سبب تاني…' })}${field('other', 'سبب تاني', 'maxlength="200"')}`,
      async (d) => {
        const reason = (d.preset || d.other || '').trim();
        if (!reason) throw new Error('اكتب السبب');
        await post(`/restaurant/orders/${b.dataset.reject}/reject`, { reason });
        route();
      }, 'رفض الطلب')));
  onLive((ev, data) => {
    if (ev !== 'order' || !location.hash.match(/^(#board)?$/)) return;
    if (data?.status === 'PLACED') { beep(); toast(`🔔 طلب جديد ${data.code}`); }
    route();
  });
}

function beep() {
  try {
    const ctx = new AudioContext();
    const o = ctx.createOscillator();
    o.frequency.value = 880;
    o.connect(ctx.destination);
    o.start();
    o.stop(ctx.currentTime + 0.35);
  } catch { /* sound blocked until the page is clicked once */ }
}

async function storeMenu() {
  const d = await get('/restaurant/store');
  setTitle(`المنيو · ${d.store.nameAr}`);
  action(d.store.accepting ? '⏸ إيقاف استقبال الطلبات' : '▶ تشغيل استقبال الطلبات', async () => {
    await attempt(() => patch('/restaurant/store', { accepting: !d.store.accepting }));
    route();
  }, d.store.accepting ? 'outline' : 'green');
  const products = d.sections.flatMap((s) => s.products);
  view.innerHTML = `<p class="muted">تقدر توقف أي صنف خلص أو تغيّر سعره. لإضافة أصناف جديدة كلّم الإدارة.</p>
    <div class="table-wrap"><table><thead><tr><th>الصنف</th><th>القسم</th><th>السعر</th><th>متاح</th></tr></thead><tbody>
    ${products.map((p) => `<tr><td><b>${esc(p.nameAr)}</b></td><td>${esc(p.section)}</td>
      <td><input type="number" min="1" step="0.5" value="${esc(p.price)}" data-price="${esc(p.id)}" style="max-width:120px"></td>
      <td><input type="checkbox" ${p.available ? 'checked' : ''} data-avail="${esc(p.id)}" style="width:22px;min-height:22px"></td></tr>`).join('')}
    </tbody></table></div>`;
  view.querySelectorAll('[data-price]').forEach((i) => i.addEventListener('change', async () => {
    if (await attempt(() => patch(`/restaurant/products/${i.dataset.price}`, { price: Number(i.value) })) !== undefined) toast('السعر اتغير');
  }));
  view.querySelectorAll('[data-avail]').forEach((i) => i.addEventListener('change', async () => {
    if (await attempt(() => patch(`/restaurant/products/${i.dataset.avail}`, { available: i.checked })) !== undefined) toast(i.checked ? 'الصنف متاح' : 'الصنف اتوقف');
  }));
}

async function storeStats() {
  setTitle('النهارده');
  const [s, reviews] = await Promise.all([get('/restaurant/stats'), get('/restaurant/reviews')]);
  view.innerHTML = `<div class="grid stats">
    ${[['🧾', s.ordersToday, 'طلبات'], ['✅', s.deliveredToday, 'وصلت'], ['❌', s.cancelledToday, 'اتلغت'], ['💰', money(s.salesToday), 'المبيعات']]
      .map(([i, v, l]) => `<div class="card stat"><span>${i}</span><b>${esc(v)}</b><span class="muted">${esc(l)}</span></div>`).join('')}</div>
    <h2 style="margin-top:24px">التقييمات</h2>
    ${reviews.length ? reviews.map((r) => `<div class="card" style="margin-bottom:10px"><b style="color:#e8a317">${'★'.repeat(r.storeRating)}${'☆'.repeat(5 - r.storeRating)}</b>
      <span class="small muted">${esc(r.orderCode)} · ${when(r.at)}</span>${r.comment ? `<div>${esc(r.comment)}</div>` : ''}</div>`).join('') : '<div class="card empty">لسه مفيش تقييمات</div>'}`;
}
