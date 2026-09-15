/**
 * VELOX — Growth features UI (5 competitive features, MVP).
 * Self-contained: no edits to other page scripts needed.
 * 1) Multi-store cart breakdown via POST /api/orders/quote
 * 2) Instant refund button on delivered orders (POST /api/wallet/refund)
 * 3) Scheduled delivery picker injected in the cart drawer
 * 4) VELOX Plus panel on the account page
 * 5) Price-watch bell on the product modal
 */
(function () {
  const $ = (s, r) => (r || document).querySelector(s);
  const $$ = (s, r) => Array.from((r || document).querySelectorAll(s));
  const lang = () => (window.VeloxI18n ? window.VeloxI18n.getLang() : 'ar');
  const AR = () => lang() === 'ar';
  const esc = (s) => String(s == null ? '' : s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
  const apiBase = () => window.VELOX_CONFIG.API_BASE_URL;
  const token = () => { try { return window.VeloxAuthService.getSession()?.token || null; } catch (_) { return null; } };
  const email = () => { try { return window.VeloxAuthService.getSession()?.user?.email || null; } catch (_) { return null; } };

  async function api(path, opts) {
    const headers = { 'Content-Type': 'application/json' };
    const t = token();
    if (t) headers.Authorization = 'Bearer ' + t;
    const res = await fetch(apiBase() + path, {
      method: (opts && opts.method) || 'GET',
      headers,
      body: opts && opts.body ? JSON.stringify(opts.body) : undefined,
    });
    let data = null;
    try { data = await res.json(); } catch (_) {}
    if (!res.ok) throw new Error((data && data.message) || ('Request failed (' + res.status + ')'));
    return data;
  }

  function toast(msg, type) {
    let el = $('#velox-toast');
    if (!el) { el = document.createElement('div'); el.id = 'velox-toast'; el.className = 'velox-toast'; document.body.appendChild(el); }
    el.className = 'velox-toast is-' + (type || 'success') + ' is-visible';
    el.textContent = msg;
    clearTimeout(toast.t);
    toast.t = setTimeout(() => el.classList.remove('is-visible'), 3500);
  }

  /* ---------- 3) scheduled time: stored + attached to every order ---------- */
  const SCHED_KEY = 'velox_scheduled_for';
  function getScheduled() { try { return localStorage.getItem(SCHED_KEY) || ''; } catch (_) { return ''; } }

  function wrapOrderCreate() {
    if (!window.VeloxOrderService || window.VeloxOrderService.__growthWrapped) return;
    const orig = window.VeloxOrderService.createOrder.bind(window.VeloxOrderService);
    window.VeloxOrderService.createOrder = (payload) => {
      const sched = getScheduled();
      const body = Object.assign({}, payload);
      if (sched) body.scheduled_for = sched;
      return orig(body).then((r) => {
        try { localStorage.removeItem(SCHED_KEY); } catch (_) {}
        return r;
      });
    };
    window.VeloxOrderService.__growthWrapped = true;
  }

  function injectSchedulePicker() {
    const drawer = $('#cart-drawer');
    const footer = $('#cart-footer');
    if (!drawer || !footer || $('#sched-row')) return;
    const row = document.createElement('div');
    row.id = 'sched-row';
    row.style.cssText = 'display:flex;gap:8px;align-items:center;margin:8px 0;flex-wrap:wrap';
    row.innerHTML = `<label for="sched-input" style="font-size:12px">${AR() ? 'التوصيل المجدول (اختياري)' : 'Scheduled delivery (optional)'}</label>`
      + `<input type="datetime-local" id="sched-input" style="flex:1;min-width:180px" value="${esc(getScheduled())}">`;
    footer.before(row);
    $('#sched-input').addEventListener('change', (e) => {
      try {
        if (e.target.value) localStorage.setItem(SCHED_KEY, e.target.value);
        else localStorage.removeItem(SCHED_KEY);
      } catch (_) {}
      renderQuoteBox();
    });
  }

  /* ---------- 1) multi-store breakdown box in the drawer ---------- */
  function readCartFromDrawer() {
    return $$('#cart-items .cart-item').map((el) => {
      const btn = el.querySelector('[data-qty]');
      const qtyEl = el.querySelector('.qty-control span');
      const id = btn ? Number(btn.dataset.qty) : NaN;
      const qty = qtyEl ? Number(qtyEl.textContent.trim()) : NaN;
      if (!id || !qty || qty < 1) return null;
      return { product_id: id, quantity: qty };
    }).filter(Boolean);
  }

  let quoteTimer = null;
  function renderQuoteBox() {
    const footer = $('#cart-footer');
    if (!footer) return;
    let box = $('#quote-box');
    const items = readCartFromDrawer();
    if (!items.length) { if (box) box.remove(); return; }
    clearTimeout(quoteTimer);
    quoteTimer = setTimeout(async () => {
      try {
        const q = await api('/orders/quote', { method: 'POST', body: { items } });
        let b = $('#quote-box');
        if (!b) {
          b = document.createElement('div');
          b.id = 'quote-box';
          b.style.cssText = 'margin:8px 0;padding:8px;border-radius:8px;background:rgba(139,92,246,.08);font-size:12px';
          footer.before(b);
        }
        const stores = (q.stores || []).map((s) =>
          `<div>${esc(s.storeName)} · ${Number(s.subtotal).toFixed(2)} EGP</div>`).join('');
        const sched = getScheduled();
        b.innerHTML = `<strong>${AR() ? 'تفاصيل السلة الموحدة' : 'Unified cart breakdown'}</strong>${stores}`
          + `<div>${AR() ? 'شحن أساسي' : 'Base shipping'}: ${Number(q.deliveryFee).toFixed(2)} EGP</div>`
          + (Number(q.extraStoreFee) > 0 ? `<div>${AR() ? 'محلات إضافية' : 'Extra stores'}: +${Number(q.extraStoreFee).toFixed(2)} EGP</div>` : '')
          + `<div><strong>${AR() ? 'الإجمالي' : 'Total'}: ${Number(q.total).toFixed(2)} EGP</strong></div>`
          + (sched ? `<div>🕒 ${esc(sched.replace('T', ' '))}</div>` : '');
      } catch (_) { /* backend unreachable: drawer still works without preview */ }
    }, 350);
  }

  function bindDrawerHooks() {
    if (!$('#cart-drawer')) return;
    document.addEventListener('velox:open-cart', () => { injectSchedulePicker(); renderQuoteBox(); });
    document.addEventListener('click', (e) => {
      if (e.target.closest && e.target.closest('#cart-items [data-qty]')) {
        setTimeout(renderQuoteBox, 300);
      }
    });
    injectSchedulePicker();
    renderQuoteBox();
  }

  /* ---------- 5) price-watch bell on the product modal ---------- */
  function bindWatchButton() {
    document.addEventListener('click', (e) => {
      const modal = $('#product-modal');
      if (!modal || modal.hidden) return;
      const addBtn = $('#product-modal-add');
      if (!addBtn || $('#watch-btn')) return;
      const pid = Number(addBtn.dataset.productId);
      if (!pid) return;
      const btn = document.createElement('button');
      btn.type = 'button';
      btn.id = 'watch-btn';
      btn.className = 'btn-app secondary';
      btn.style.cssText = 'margin-inline-start:8px';
      btn.textContent = AR() ? '🔔 راقب السعر' : '🔔 Watch price';
      btn.addEventListener('click', async (ev) => {
        ev.stopPropagation();
        if (!token()) { toast(AR() ? 'سجل الدخول أولاً لمراقبة الأسعار.' : 'Log in first to watch prices.', 'error'); return; }
        try {
          const r = await api('/watches', { method: 'POST', body: { product_id: pid } });
          toast((AR() ? 'هنبلغك لو السعر نزل عن ' : 'We will notify you below ') + Number(r.targetPrice).toFixed(2) + ' EGP');
        } catch (err) { toast(err.message, 'error'); }
      });
      addBtn.after(btn);
    });
  }

  /* ---------- 2) instant refund on delivered orders ---------- */
  function bindRefundButtons() {
    const list = $('#orders-list');
    if (!list) return;
    const obs = new MutationObserver(() => {
      $$('#orders-list .order-card').forEach((card) => {
        if (card.dataset.refundBound) return;
        const h3 = card.querySelector('h3');
        const isDelivered = h3 && h3.textContent.includes('DELIVERED');
        if (!isDelivered) return;
        card.dataset.refundBound = '1';
        const details = card.querySelector('.order-details');
        if (!details) return;
        const wrap = document.createElement('div');
        wrap.style.cssText = 'display:flex;gap:8px;margin-top:8px;flex-wrap:wrap';
        wrap.innerHTML = `<select id="refund-reason-${Date.now()}" style="flex:1;min-width:140px">`
          + `<option value="LATE">${AR() ? 'تأخير التوصيل' : 'Late delivery'}</option>`
          + `<option value="WRONG_ITEM">${AR() ? 'صنف غلط' : 'Wrong item'}</option>`
          + `<option value="DAMAGED">${AR() ? 'منتج تالف' : 'Damaged item'}</option></select>`
          + `<button type="button" class="btn-app secondary">${AR() ? 'طلب تعويض فوري' : 'Claim instant refund'}</button>`;
        const btn = wrap.querySelector('button');
        const sel = wrap.querySelector('select');
        btn.addEventListener('click', async (ev) => {
          ev.stopPropagation();
          const code = (card.dataset.select || '').trim();
          if (!code) return;
          btn.disabled = true;
          try {
            const r = await api('/wallet/refund', { method: 'POST', body: { orderId: code, reason: sel.value } });
            toast((AR() ? 'نزل لك تعويض ' : 'Refunded ') + Number(r.amount).toFixed(2) + ' EGP');
          } catch (err) { toast(err.message, 'error'); }
          finally { btn.disabled = false; }
        });
        details.appendChild(wrap);
      });
    });
    obs.observe(list, { childList: true, subtree: true });
  }

  /* ---------- 4) VELOX Plus panel on the account page ---------- */
  async function renderPlusPanel() {
    if (!document.querySelector('.page-grid') || $('#plus-panel') || !email()) return;
    const panel = document.createElement('section');
    panel.className = 'panel';
    panel.id = 'plus-panel';
    panel.innerHTML = `<div class="panel-head"><div><h2>VELOX Plus</h2>`
      + `<p>${AR() ? 'توصيل مجاني لكل طلباتك لمدة 30 يوم بـ 50 جنيه' : 'Free delivery for 30 days for EGP 50'}</p></div>`
      + `<div class="icon-tile">👑</div></div><div style="margin-top:12px" id="plus-body">…</div>`;
    document.querySelector('.page-grid').prepend(panel);
    const body = $('#plus-body');
    try {
      const st = await api('/loyalty/subscription?userId=' + encodeURIComponent(email()));
      if (st.active) {
        body.innerHTML = `<p>✅ ${AR() ? 'اشتراكك شغال حتى' : 'Active until'} ${esc(st.expiresAt || '')}</p>`;
      } else {
        body.innerHTML = `<button type="button" class="btn-app" id="plus-subscribe">`
          + `${AR() ? 'اشترك الآن — 50 جنيه من المحفظة' : 'Subscribe now — EGP 50 from wallet'}</button>`;
        $('#plus-subscribe').addEventListener('click', async () => {
          try {
            const r = await api('/loyalty/subscribe', { method: 'POST', body: {} });
            body.innerHTML = `<p>✅ ${AR() ? 'تم التفعيل حتى' : 'Active until'} ${esc(r.expiresAt || '')}</p>`;
            toast(AR() ? 'أهلاً بيك في Plus! التوصيل بقى مجاني.' : 'Welcome to Plus! Delivery is now free.');
          } catch (err) { toast(err.message, 'error'); }
        });
      }
    } catch (_) { body.innerHTML = `<p class="muted">…</p>`; }
  }

  /* ---------- stores-first: directory with live ratings ---------- */
  let storeCache = null;
  async function loadStores() {
    if (storeCache) return storeCache;
    try {
      const r = await fetch(apiBase() + '/stores');
      storeCache = await r.json();
    } catch (_) { storeCache = []; }
    return storeCache;
  }
  window.VeloxStoreMeta = {
    async ready() { await loadStores(); },
    get(id) { return (storeCache || []).find((s) => Number(s.id) === Number(id)) || null; },
  };

  function stars(rating) {
    if (rating == null) return `<span class="store-rating new">${AR() ? 'جديد' : 'New'}</span>`;
    const full = Math.round(Number(rating));
    return `<span class="store-rating">${'★'.repeat(Math.max(0, Math.min(5, full)))}${'☆'.repeat(Math.max(0, 5 - Math.min(5, full)))} ${Number(rating).toFixed(1)}</span>`;
  }

  async function renderStoresSection() {
    const catSection = document.querySelector('section.category-section');
    if (!catSection || $('#stores-section')) return;
    const stores = await loadStores();
    if (!stores.length) return;
    const sec = document.createElement('section');
    sec.id = 'stores-section';
    sec.innerHTML = `<div class="container section-shell">`
      + `<div class="section-heading"><div><span class="eyebrow">${AR() ? 'تسوق حسب المتجر' : 'Shop by store'}</span>`
      + `<h2>${AR() ? 'اختار المتجر الأول' : 'Pick a store first'}</h2></div></div>`
      + `<div class="category-grid" id="stores-grid">` + stores.map((s) => {
        const name = AR() ? (s.nameAr || s.name) : (s.name || s.nameAr);
        const initial = String(name || 'V').trim().charAt(0);
        return `<button type="button" class="category-card store-card" data-store="${s.id}">`
          + `<span class="category-icon">${esc(initial)}</span>`
          + `<span class="category-copy"><strong>${esc(name)}</strong>`
          + `<small>${esc(s.type || '')} · ${s.productsCount} ${AR() ? 'منتج' : 'products'}</small>`
          + `${stars(s.rating)}</span><span class="category-arrow">↗</span></button>`;
      }).join('') + `</div></div>`;
    catSection.after(sec);
    sec.querySelectorAll('[data-store]').forEach((btn) => btn.addEventListener('click', () => {
      window.location.href = `products.html?store=${encodeURIComponent(btn.dataset.store)}`;
    }));
  }

  /* ---------- my extra: promo strip for the new capabilities ---------- */
  function renderPromoStrip() {
    const catSection = document.querySelector('section.category-section');
    if (!catSection || $('#promo-strip')) return;
    const strip = document.createElement('div');
    strip.id = 'promo-strip';
    strip.innerHTML = `<div class="container"><div class="promo-strip-card">`
      + `<span>🛍️</span><p>${AR()
        ? 'جديد: سلة موحدة من كل المحلات + تعويض فوري + جدولة طلبك + توصيل مجاني مع Plus'
        : 'New: one cart across all stores + instant refunds + scheduled orders + free delivery with Plus'}</p>`
      + `</div></div>`;
    catSection.before(strip);
  }

  /* ---------- boot ---------- */
  function boot() {
    wrapOrderCreate();
    const page = (location.pathname.split('/').pop() || '').toLowerCase();
    if (['index.html', 'home.html', 'products.html', ''].includes(page)) {
      bindDrawerHooks();
      bindWatchButton();
      renderPromoStrip();
      renderStoresSection();
      window.VeloxStoreMeta.ready().then(() => {
        if (new URLSearchParams(window.location.search).get('store')) {
          window.dispatchEvent(new Event('velox:langchange'));
        }
      });
      // storefront scripts may load after us on slow networks
      setTimeout(() => { wrapOrderCreate(); bindDrawerHooks(); }, 1500);
    }
    if (['account.html', 'orders.html'].includes(page)) {
      bindRefundButtons();
      if (page === 'account.html') {
        renderPlusPanel();
        setTimeout(renderPlusPanel, 1500);
      }
    }
  }

  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', boot);
  else boot();
})();
