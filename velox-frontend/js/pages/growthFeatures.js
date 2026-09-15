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

  let feeCache = null;
  let plusCache = null;

  async function deliveryFee() {
    if (feeCache != null) return feeCache;
    try {
      const g = await api('/governorates/CAIRO/shipping');
      feeCache = Number(g.shippingPrice);
    } catch (_) { feeCache = 20; }
    return feeCache;
  }

  async function plusActive() {
    if (plusCache != null) return plusCache;
    plusCache = false;
    try {
      const e = email();
      if (e) {
        const st = await api('/loyalty/subscription?userId=' + encodeURIComponent(e));
        plusCache = !!st.active;
      }
    } catch (_) {}
    return plusCache;
  }

  function etaFor(store) {
    if ((store.type || '').toUpperCase() === 'RESTAURANT') {
      const base = 20 + (Number(store.id) * 3) % 12;
      return AR() ? `${base}–${base + 10} دقيقة` : `${base}-${base + 10} min`;
    }
    return AR() ? 'خلال 1–2 يوم' : 'In 1-2 days';
  }

  function storeCard(s, fee, plus) {
    const name = AR() ? (s.nameAr || s.name) : (s.name || s.nameAr);
    return `<button type="button" class="store-card" data-store="${s.id}">`
      + `<span class="store-cover">${s.cover ? `<img src="${esc(s.cover)}" alt="" loading="lazy" onerror="this.remove()">` : '🏪'}</span>`
      + `<span class="store-body"><strong>${esc(name)}</strong>`
      + `<small>${esc(s.type || '')}</small>`
      + `<span class="store-meta-row">${stars(s.rating)}<span class="store-eta">⏱ ${esc(etaFor(s))}</span></span>`
      + `<span class="store-meta-row"><span class="store-fee">🚚 ${Number(fee).toFixed(0)} EGP</span>`
      + (plus ? `<span class="plus-badge">👑 ${AR() ? 'مجاني مع Plus' : 'Free with Plus'}</span>` : '')
      + `</span></span></button>`;
  }

  async function renderStoresSection() {
    const catSection = document.querySelector('section.category-section');
    if (!catSection || $('#stores-section')) return;
    const [stores, fee, plus] = await Promise.all([loadStores(), deliveryFee(), plusActive()]);
    if (!stores.length) return;
    const rest = stores.filter((s) => (s.type || '').toUpperCase() === 'RESTAURANT');
    const general = stores.filter((s) => (s.type || '').toUpperCase() !== 'RESTAURANT');
    const group = (title, list) => list.length ? (title ? `<h3 class="stores-sub">${title}</h3>` : '')
      + `<div class="stores-grid">` + list.map((s) => storeCard(s, fee, plus)).join('') + `</div>` : '';
    const sec = document.createElement('section');
    sec.id = 'stores-section';
    sec.innerHTML = `<div class="container section-shell">`
      + `<div class="section-heading"><div><span class="eyebrow">${AR() ? 'تسوق حسب المتجر' : 'Shop by store'}</span>`
      + `<h2>${AR() ? 'مطاعم قريبة منك' : 'Restaurants near you'}</h2></div>`
      + `<a class="text-btn view-all-link" href="restaurants.html">${AR() ? 'عرض الكل ←' : 'View all →'}</a></div>`
      + group(AR() ? '🍽️ مطاعم' : '🍽️ Restaurants', rest)
      + (general.length ? `<div class="section-heading" style="margin-top:26px"><div><h2>${AR() ? 'توصيل عام: أزياء وإلكترونيات' : 'General delivery: fashion & tech'}</h2></div></div>` : '')
      + group('', general)
      + `</div>`;
    catSection.after(sec);
    sec.querySelectorAll('[data-store]').forEach((btn) => btn.addEventListener('click', () => {
      window.location.href = `store.html?id=${encodeURIComponent(btn.dataset.store)}`;
    }));
  }

  /* ---------- animated promo banner (rotating offers) ---------- */
  const PROMOS = [
    { icon: '🛍️', ar: 'سلة موحدة من كل المحلات بتوصيلة واحدة', en: 'One cart across all stores, one delivery' },
    { icon: '💸', ar: 'تعويض فوري على المحفظة لو طلبك اتأخر', en: 'Instant wallet refund if your order is late' },
    { icon: '👑', ar: 'اشترك في Plus: توصيل مجاني 30 يوم بـ 50 جنيه', en: 'Plus: free delivery for 30 days at EGP 50' },
  ];
  function renderPromoStrip() {
    const catSection = document.querySelector('section.category-section');
    if (!catSection || $('#promo-strip')) return;
    const strip = document.createElement('div');
    strip.id = 'promo-strip';
    strip.innerHTML = `<div class="container"><div class="promo-slider">`
      + PROMOS.map((p, i) => `<div class="promo-slide${i === 0 ? ' is-active' : ''}"><span>${p.icon}</span><p>${AR() ? p.ar : p.en}</p></div>`).join('')
      + `</div></div>`;
    catSection.before(strip);
    let idx = 0;
    const slides = strip.querySelectorAll('.promo-slide');
    setInterval(() => {
      slides[idx].classList.remove('is-active');
      idx = (idx + 1) % slides.length;
      slides[idx].classList.add('is-active');
    }, 4000);
    document.addEventListener('velox:langchange', () => {
      strip.querySelectorAll('.promo-slide p').forEach((el, k) => { el.textContent = AR() ? PROMOS[k].ar : PROMOS[k].en; });
    });
  }

  /* ---------- most-ordered rail (real sales data) ---------- */
  async function renderPopularSection() {
    const prodSection = document.querySelector('section.products-section');
    if (!prodSection || $('#popular-section')) return;
    let items = [];
    try {
      const r = await fetch(apiBase() + '/products/popular?limit=8');
      items = await r.json();
    } catch (_) {}
    if (!items.length) return;
    const sec = document.createElement('section');
    sec.id = 'popular-section';
    sec.innerHTML = `<div class="container section-shell">`
      + `<div class="section-heading"><div><span class="eyebrow">${AR() ? 'الأكثر طلباً' : 'Most ordered'}</span>`
      + `<h2>${AR() ? 'الناس بتشتري إيه؟' : 'What people order'}</h2></div></div>`
      + `<div class="popular-rail">` + items.map((p) => {
        const nm = AR() ? (p.nameAr || p.nameEn) : (p.nameEn || p.nameAr);
        return `<article class="product-card popular-card"><div class="product-media">`
          + (p.image ? `<img class="product-image" src="${esc(p.image)}" alt="" loading="lazy" onerror="this.style.display='none'">` : `<span>🛍️</span>`)
          + `</div><div class="product-body"><h3>${esc(nm)}</h3>`
          + `<div class="product-bottom"><div class="price">${Number(p.price).toFixed(0)} <small>EGP</small></div>`
          + `<button type="button" class="add-btn" data-pop-add="${p.id}" aria-label="+">+</button></div></div></article>`;
      }).join('') + `</div></div>`;
    prodSection.before(sec);
    sec.querySelectorAll('[data-pop-add]').forEach((b) => b.addEventListener('click', () => {
      let cart = [];
      try { cart = JSON.parse(localStorage.getItem('velox_cart') || '[]'); } catch (_) {}
      if (!Array.isArray(cart)) cart = [];
      const ex = cart.find((i) => Number(i.id) === Number(b.dataset.popAdd));
      if (ex) ex.qty += 1; else cart.push({ id: Number(b.dataset.popAdd), qty: 1 });
      try { localStorage.setItem('velox_cart', JSON.stringify(cart)); } catch (_) {}
      document.dispatchEvent(new CustomEvent('velox:cartchange', { detail: { count: cart.reduce((s, i) => s + Number(i.qty), 0) } }));
      toast(AR() ? 'تمت الإضافة للسلة' : 'Added to cart');
    }));
  }

  /* ---------- one-click reorder from past orders ---------- */
  function bindReorder() {
    const list = $('#orders-list');
    if (!list) return;
    const obs = new MutationObserver(() => {
      $$('#orders-list .order-card').forEach((card) => {
        if (card.dataset.reorderBound) return;
        const details = card.querySelector('.order-details');
        if (!details) return;
        card.dataset.reorderBound = '1';
        const btn = document.createElement('button');
        btn.type = 'button';
        btn.className = 'btn-app secondary';
        btn.style.cssText = 'margin-top:8px';
        btn.textContent = AR() ? '↻ اطلب نفس الطلب' : '↻ Reorder';
        btn.addEventListener('click', async (ev) => {
          ev.stopPropagation();
          const code = (card.dataset.select || '').trim();
          if (!code) return;
          btn.disabled = true;
          try {
            const lines = await api('/orders/' + encodeURIComponent(code) + '/items');
            if (!lines.length) throw new Error('empty');
            let cart = [];
            try { cart = JSON.parse(localStorage.getItem('velox_cart') || '[]'); } catch (_) {}
            if (!Array.isArray(cart)) cart = [];
            for (const l of lines) {
              const ex = cart.find((i) => Number(i.id) === Number(l.product_id));
              if (ex) ex.qty += Number(l.quantity);
              else cart.push({ id: Number(l.product_id), qty: Number(l.quantity) });
            }
            try { localStorage.setItem('velox_cart', JSON.stringify(cart)); } catch (_) {}
            document.dispatchEvent(new CustomEvent('velox:cartchange', { detail: { count: cart.reduce((s, i) => s + Number(i.qty), 0) } }));
            toast(AR() ? 'اتضافت أصناف طلبك للسلة' : 'Past order added to cart');
          } catch (_) { toast(AR() ? 'تعذر إعادة الطلب.' : 'Could not reorder.', 'error'); }
          finally { btn.disabled = false; }
        });
        details.appendChild(btn);
      });
    });
    obs.observe(list, { childList: true, subtree: true });
  }

  /* ---------- live courier map (offline SVG, no tiles needed) ---------- */
  function quadPoint(p0, p1, p2, t) {
    const x = (1 - t) * (1 - t) * p0[0] + 2 * (1 - t) * t * p1[0] + t * t * p2[0];
    const y = (1 - t) * (1 - t) * p0[1] + 2 * (1 - t) * t * p1[1] + t * t * p2[1];
    return [x, y];
  }

  function bindLiveMap() {
    const box = $('#tracking-box');
    const fill = $('#tracking-fill');
    if (!box || !fill) return;
    const P0 = [30, 100], P1 = [150, 15], P2 = [270, 95];
    let map = $('#live-map');
    if (!map) {
      map = document.createElement('div');
      map.id = 'live-map';
      map.innerHTML = `<svg viewBox="0 0 300 125" class="live-map-svg">`
        + `<path d="M ${P0[0]},${P0[1]} Q ${P1[0]},${P1[1]} ${P2[0]},${P2[1]}" class="live-route"/>`
        + `<circle cx="${P0[0]}" cy="${P0[1]}" r="7" class="live-point store"/>`
        + `<circle cx="${P2[0]}" cy="${P2[1]}" r="7" class="live-point home"/>`
        + `<g id="live-courier"><circle r="8" class="live-courier"/><text y="4" text-anchor="middle" class="live-emoji">🛵</text></g>`
        + `<text x="${P0[0]}" y="120" text-anchor="middle" class="live-label">${AR() ? 'المتجر' : 'Store'}</text>`
        + `<text x="${P2[0]}" y="120" text-anchor="middle" class="live-label">${AR() ? 'بيتك' : 'Home'}</text>`
        + `</svg>`;
      box.prepend(map);
    }
    const move = () => {
      const pct = parseFloat(fill.style.width) || 0;
      const [x, y] = quadPoint(P0, P1, P2, Math.min(1, Math.max(0, pct / 100)));
      const g = $('#live-courier');
      if (g) g.setAttribute('transform', `translate(${x},${y})`);
    };
    new MutationObserver(move).observe(fill, { attributes: true, attributeFilter: ['style'] });
    move();
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
      renderPopularSection();
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
      bindReorder();
      if (page === 'orders.html') bindLiveMap();
      if (page === 'account.html') {
        renderPlusPanel();
        setTimeout(renderPlusPanel, 1500);
      }
    }
  }

  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', boot);
  else boot();
})();
