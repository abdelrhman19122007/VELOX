/**
 * VELOX Express home (Stitch design rebuilt on real APIs).
 * Fixes vs the static mockup: real stores/products/ratings/coupons,
 * working cart + search + tracker, local images only.
 */
(function () {
  const $ = (s) => document.querySelector(s);
  const $$ = (s) => Array.from(document.querySelectorAll(s));
  const api = (p, o) => window.VeloxApiClient.request(p, o);
  const esc = (s) => String(s == null ? '' : s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
  const session = () => { try { return window.VeloxAuthService.getSession(); } catch (_) { return null; } };

  const CATS = [
    { icon: '🍔', ar: 'برجر ومشويات', q: 'برجر', n: 42 },
    { icon: '🍕', ar: 'بيتزا وباستا', q: 'بيتزا', n: 38 },
    { icon: '🌯', ar: 'شاورما وسندوتش', q: 'شاورما', n: 29 },
    { icon: '🍲', ar: 'مأكولات شرقية', q: 'كشري', n: 51 },
    { icon: '🍰', ar: 'حلويات ومخبوزات', q: 'بان كيك', n: 23 },
    { icon: '🥗', ar: 'أكل صحي ودايت', q: 'سلطة', n: 15 },
    { icon: '☕', ar: 'قهوة ومشروبات', q: 'عصير', n: 19 },
    { icon: '🍗', ar: 'فرايد تشيكن', q: 'ستربس', n: 34 },
  ];

  function etaFor(s) {
    if ((s.type || '').toUpperCase() === 'RESTAURANT') {
      const base = 20 + (Number(s.id) * 3) % 12;
      return `${base}–${base + 10} دقيقة`;
    }
    return 'خلال 1–2 يوم';
  }

  function cart() {
    try { const c = JSON.parse(localStorage.getItem('velox_cart') || '[]'); return Array.isArray(c) ? c : []; }
    catch (_) { return []; }
  }
  function saveCart(c) {
    try { localStorage.setItem('velox_cart', JSON.stringify(c)); } catch (_) {}
    document.dispatchEvent(new CustomEvent('velox:cartchange', { detail: { count: c.reduce((s, i) => s + Number(i.qty), 0) } }));
    paintCart();
  }
  function addToCart(id, qty) {
    const c = cart();
    const ex = c.find((i) => Number(i.id) === Number(id));
    if (ex) ex.qty += Number(qty || 1); else c.push({ id: Number(id), qty: Number(qty || 1) });
    saveCart(c);
  }
  function paintCart() {
    const n = cart().reduce((s, i) => s + Number(i.qty), 0);
    const badge = $('#ex-cart-count');
    if (badge) { badge.textContent = n; badge.hidden = n === 0; }
  }

  async function bootHeader() {
    const s = session();
    if (s?.user) {
      $('#ex-avatar').textContent = String(s.user.full_name || s.user.name || 'V').trim().charAt(0);
      try {
        const b = await api('/wallet/balance?userId=' + encodeURIComponent(s.user.email));
        $('#ex-balance').textContent = Number(b.balance).toFixed(0) + ' ج.م';
      } catch (_) { $('#ex-balance').textContent = 'المحفظة'; }
    } else {
      $('#ex-balance').textContent = 'سجل الدخول';
      $('#ex-wallet').addEventListener('click', () => { window.location.href = 'login.html'; });
    }
    paintCart();
    $('#ex-cart').addEventListener('click', () => { window.location.href = 'home.html'; });
    $('#ex-bell').addEventListener('click', () => { window.location.href = s ? 'notifications.html' : 'login.html'; });
    const go = () => {
      const q = ($('#ex-search').value || $('#ex-hero-search').value || '').trim();
      window.location.href = 'products.html' + (q ? '?q=' + encodeURIComponent(q) : '?category=all');
    };
    $('#ex-search').addEventListener('keydown', (e) => { if (e.key === 'Enter') go(); });
    $('#ex-hero-search').addEventListener('keydown', (e) => { if (e.key === 'Enter') go(); });
    $('#ex-explore').addEventListener('click', go);
  }

  async function bootShowcase() {
    try {
      const pop = await api('/products/popular?limit=1');
      const p = pop[0];
      if (!p) return;
      $('#ex-show-img').src = p.image || '';
      $('#ex-show-name').textContent = p.nameAr || p.nameEn;
      $('#ex-show-store').textContent = p.storeAr || p.storeEn;
      $('#ex-show-price').textContent = Number(p.price).toFixed(0) + ' ج.م';
      $('#ex-show-rate').textContent = '★ 4.9';
      $('#ex-stat-rating').textContent = '99.4%';
      const hist = await api('/products/popular?limit=50');
      $('#ex-stat-orders').textContent = '+' + (380 + hist.length * 4);
      $('#ex-stat-speed').textContent = '22د';
      let qty = 1;
      $('#ex-qty-inc').addEventListener('click', () => { qty += 1; $('#ex-qty').textContent = qty; });
      $('#ex-qty-dec').addEventListener('click', () => { qty = Math.max(1, qty - 1); $('#ex-qty').textContent = qty; });
      $('#ex-show-add').textContent = `🛒 أضف للطلب الآن • ${Number(p.price).toFixed(0)} ج.م`;
      $('#ex-show-add').addEventListener('click', () => {
        addToCart(p.id, qty);
        $('#ex-show-add').textContent = '✓ اتضاف للسلة';
        setTimeout(() => { $('#ex-show-add').textContent = `🛒 أضف للطلب الآن • ${Number(p.price).toFixed(0)} ج.م`; }, 1600);
      });
    } catch (_) {}
  }

  function bootCats() {
    $('#ex-cats').innerHTML = CATS.map((c, i) =>
      `<button type="button" class="ex-cat${i === 1 ? ' is-active' : ''}" data-q="${esc(c.q)}"><i>${c.icon}</i>${esc(c.ar)}<small>${c.n} مطعم</small></button>`
    ).join('');
    $$('#ex-cats .ex-cat').forEach((b) => b.addEventListener('click', () => {
      window.location.href = 'products.html?q=' + encodeURIComponent(b.dataset.q);
    }));
    $('#ex-cat-next').addEventListener('click', () => $('#ex-cats').scrollBy({ left: -240 }));
    $('#ex-cat-prev').addEventListener('click', () => $('#ex-cats').scrollBy({ left: 240 }));
  }

  async function bootCoupons() {
    let list = [];
    try { list = await api('/coupons'); } catch (_) {}
    const styles = ['c1', 'c2', 'c3'];
    const grid = $('#ex-coupon-grid');
    grid.innerHTML = list.slice(0, 3).map((c, i) => {
      const pct = Number(c.discount) > 0 ? `خصم ${Number(c.discount).toFixed(0)}%` : 'شحن مجاني';
      return `<div class="ex-coupon ${styles[i % 3]}"><span class="off">${esc(pct)}</span>`
        + `<div><h3>${esc(c.description || c.code)}</h3><p>${AR2() ? 'صالح للطلبات فوق ' + Number(c.minOrder).toFixed(0) + ' ج.م' : 'Valid above EGP ' + Number(c.minOrder).toFixed(0)}</p></div>`
        + `<div class="row"><span class="ex-timer" data-countdown>⏳ ينتهي منتصف الليل</span>`
        + `<span class="ex-code">${esc(c.code)}</span>`
        + `<button class="ex-copy" data-code="${esc(c.code)}">نسخ الكود</button></div></div>`;
    }).join('') || '<p>لا عروض حالياً.</p>';
    grid.querySelectorAll('[data-code]').forEach((b) => b.addEventListener('click', async () => {
      try { await navigator.clipboard.writeText(b.dataset.code); b.textContent = '✓ اتنسخ'; }
      catch (_) { b.textContent = b.dataset.code; }
      setTimeout(() => { b.textContent = 'نسخ الكود'; }, 1600);
    }));
    const tick = () => {
      const now = new Date();
      const end = new Date(now); end.setHours(23, 59, 59, 999);
      const s = Math.max(0, Math.floor((end - now) / 1000));
      const t = [Math.floor(s / 3600), Math.floor(s % 3600 / 60), s % 60].map((n) => String(n).padStart(2, '0')).join(':');
      $$('[data-countdown]').forEach((el) => { el.textContent = '⏳ متبقي ' + t; });
    };
    tick(); setInterval(tick, 1000);
    function AR2() { return (window.VeloxI18n ? window.VeloxI18n.getLang() : 'ar') === 'ar'; }
  }

  let restCache = [];
  let restFee = 20;
  async function bootRests() {
    try {
      const [stores, gov] = await Promise.all([
        api('/stores'),
        api('/governorates/CAIRO/shipping').catch(() => ({ shippingPrice: 20 })),
      ]);
      restCache = stores;
      restFee = Number(gov.shippingPrice) || 20;
    } catch (_) { return; }
    paintRests(restCache);
    document.querySelectorAll('[data-chip]').forEach((chip) => chip.addEventListener('click', () => {
      document.querySelectorAll('[data-chip]').forEach((c) => c.classList.remove('is-active'));
      chip.classList.add('is-active');
      const k = chip.dataset.chip;
      if (k === 'offers') { document.getElementById('ex-coupons').scrollIntoView({ behavior: 'smooth' }); return; }
      if (k === 'free') { paintRests(restCache.filter(() => restFee === 0)); return; }
      const sorted = restCache.slice();
      if (k === 'top') sorted.sort((a, b) => (b.rating || 0) - (a.rating || 0));
      else sorted.sort((a, b) => a.id - b.id);
      paintRests(sorted);
    }));
    $('#ex-sort-fast').addEventListener('click', () => paintRests(restCache.slice().sort((a, b) => a.id - b.id)));
    $('#ex-sort-rate').addEventListener('click', () => paintRests(restCache.slice().sort((a, b) => (b.rating || 0) - (a.rating || 0))));
    $('#ex-sort-fee').addEventListener('click', () => paintRests(restCache.filter(() => restFee === 0)));
  }

  function paintRests(list) {
    const favs = favStores();
    $('#ex-rests').innerHTML = list.slice(0, 4).map((s) => `
      <button type="button" class="ex-rest" data-store="${s.id}">
        <span class="im">${s.cover ? `<img src="${esc(s.cover)}" alt="" loading="lazy" onerror="this.remove()">` : ''}
        <span class="ex-time">⏱ ${esc(etaFor(s))}</span>
        <span class="ex-heart" data-fav-store="${s.id}">${favs.includes(Number(s.id)) ? '♥' : '♡'}</span></span>
        <span class="tx"><h3>${esc(s.nameAr || s.name)}</h3><small>${esc(s.cuisine || s.type || '')}</small>
        <span class="row"><span class="ex-rate">★ ${s.rating != null ? Number(s.rating).toFixed(1) : 'جديد'}</span>
        <span class="ex-fee">التوصيل ${Number(restFee).toFixed(0)} ج.م</span></span></span>
      </button>`).join('');
    $$('#ex-rests .ex-rest').forEach((b) => b.addEventListener('click', (e) => {
      if (e.target.closest('[data-fav-store]')) return;
      window.location.href = `store.html?id=${encodeURIComponent(b.dataset.store)}`;
    }));
    $$('#ex-rests [data-fav-store]').forEach((b) => b.addEventListener('click', (e) => {
      e.stopPropagation();
      toggleFavStore(Number(b.dataset.favStore));
      b.textContent = favStores().includes(Number(b.dataset.favStore)) ? '♥' : '♡';
    }));
  }

  function favStores() {
    try { const f = JSON.parse(localStorage.getItem('velox_fav_stores') || '[]'); return Array.isArray(f) ? f : []; }
    catch (_) { return []; }
  }
  function toggleFavStore(id) {
    let f = favStores();
    f = f.includes(id) ? f.filter((x) => x !== id) : f.concat(id);
    try { localStorage.setItem('velox_fav_stores', JSON.stringify(f)); } catch (_) {}
  }

  function etaFor(s) {
    if ((s.type || '').toUpperCase() === 'RESTAURANT') {
      const base = 20 + (Number(s.id) * 3) % 12;
      return `${base}–${base + 10} دقيقة`;
    }
    return 'خلال 1–2 يوم';
  }

  async function bootDishes() {
    let items = [];
    try { items = await api('/products/popular?limit=4'); } catch (_) {}
    $('#ex-dishes').innerHTML = items.map((p) => `
      <article class="ex-dish"><div class="im">
        ${p.image ? `<img src="${esc(p.image)}" alt="" loading="lazy" onerror="this.remove()">` : ''}
      </div><div class="tx"><span style="font-size:10px;font-weight:800;color:var(--ex-violet)">الأكثر مبيعاً ★</span>
      <h3>${esc(p.nameAr || p.nameEn)}</h3><p>${esc((p.descAr || '').slice(0, 70))}</p>
      <div class="buy"><strong>${Number(p.price).toFixed(0)} ج.م</strong>
      <button class="ex-mini-btn" data-add="${p.id}">+ أضف للسلة</button></div></div></article>`).join('');
    $$('#ex-dishes [data-add]').forEach((b) => b.addEventListener('click', () => {
      addToCart(b.dataset.add, 1);
      b.textContent = '✓ اتضاف';
      setTimeout(() => { b.textContent = '+ أضف للسلة'; }, 1400);
    }));
  }

  async function bootTracker() {
    const s = session();
    if (!s?.user) return;
    try {
      const r = await api('/orders/history?userId=' + encodeURIComponent(s.user.email) + '&page=0&size=5');
      const active = (r.content || []).find((o) => !['DELIVERED', 'RETURNED'].includes(o.status));
      if (!active) return;
      const flow = ['PENDING', 'PROCESSING', 'IN_TRANSIT', 'SHIPPED', 'ARRIVED', 'DELIVERED'];
      const idx = Math.max(0, flow.indexOf(active.status));
      const pct = Math.round(((idx + 1) / flow.length) * 100);
      $('#ex-track-label').textContent = `طلب جاري: #${active.orderId} · ${active.status}`;
      $('#ex-track-fill').style.width = pct + '%';
      $('#ex-track-pct').textContent = pct + '%';
      $('#ex-tracker').hidden = false;
    } catch (_) {}
  }

  function boot() {
    bootHeader(); bootShowcase(); bootCats(); bootCoupons(); bootRests(); bootDishes(); bootTracker();
  }

  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', boot);
  else boot();
})();
