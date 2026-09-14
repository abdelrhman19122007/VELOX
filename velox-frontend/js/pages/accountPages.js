/* VELOX — account-area wiring: guards + real API for the 8 app pages. */
(function () {
  const $ = (s) => document.querySelector(s);
  const lang = () => (window.VeloxI18n ? window.VeloxI18n.getLang() : 'ar');
  const t = (k) => (window.VeloxI18n ? window.VeloxI18n.translate(k, lang()) : k);
  const session = () => { try { return window.VeloxAuthService.getSession(); } catch (_) { return null; } };
  const api = (p, o) => window.VeloxApiClient.request(p, o);
  function esc(s) { return String(s == null ? '' : s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;'); }
  function toast(msg) { const el = $('#app-toast'); if (!el) return; el.textContent = msg; el.classList.add('show'); setTimeout(() => el.classList.remove('show'), 2400); }
  function saveSessionObj(s) { try { sessionStorage.setItem('velox_session', JSON.stringify(s)); } catch (_) {} }
  function authHeader() { const s = session(); return (s && s.token) ? { Authorization: 'Bearer ' + s.token } : {}; }

  const page = (location.pathname.split('/').pop() || '').toLowerCase();
  const NEED_LOGIN = ['account.html', 'orders.html', 'rating.html', 'feedback.html', 'support.html'];
  const GOVS = ['CAIRO','GIZA','ALEXANDRIA','DAKAHLIA','RED_SEA','BEHEIRA','FAYOUM','GHARBIA','ISMAILIA','MENOFIA','MINYA','QALYUBIA','NEW_VALLEY','SUEZ','ASWAN','ASYUT','BENI_SUEF','PORT_SAID','DAMIETTA','SHARKIA','SOUTH_SINAI','KAFR_EL_SHEIKH','MATROUH','LUXOR','NORTH_SINAI','QENA','SOHAG'];

  document.addEventListener('DOMContentLoaded', () => {
    if (NEED_LOGIN.includes(page) && !session()) { window.location.href = 'login.html'; return; }
    if (page === 'account.html') initAccount();
    if (page === 'orders.html') initOrders();
    if (page === 'favorites.html') initFavs();
    if (page === 'rating.html') initRating();
    if (page === 'feedback.html') initFeedback();
    if (page === 'support.html') { initSupport(); initMyComplaints(); }
    if (page === 'notifications.html') initNotifications();
    document.addEventListener('velox:langchange', () => {
      if (page === 'orders.html') renderOrdersCached();
      if (page === 'favorites.html') renderFavs();
    });
  });

  function setVal(sel, v) { const el = $(sel); if (el) el.value = v == null ? '' : v; }

  /* ---------------- account ---------------- */
  async function initAccount() {
    const s = session(); if (!s) return;
    const u = s.user || {};
    setVal('#acc-name', u.full_name || u.name || '');
    setVal('#acc-email', u.email || '');
    setVal('#acc-phone', u.phone_number || u.phone || '');
    buildGovSelect(u.governorate);
    const cn = $('#card-name');
    if (cn && !cn.value) cn.value = u.full_name || u.name || '';
    try {
      const p = await api('/auth/profile?userId=' + encodeURIComponent(u.email));
      Object.assign(u, p); saveSessionObj(s);
      setVal('#acc-name', u.full_name || u.name || '');
      setVal('#acc-email', u.email || '');
      setVal('#acc-phone', u.phone_number || u.phone || '');
      buildGovSelect(u.governorate);
    } catch (_) {}
    const save = $('#save-account');
    if (save) save.addEventListener('click', async () => {
      const payload = {
        full_name: ($('#acc-name') || {}).value || '',
        phone_number: ($('#acc-phone') || {}).value || '',
        governorate: ($('#acc-governorate') || {}).value || ''
      };
      try {
        const p = await api('/auth/profile', { method: 'PUT', body: payload });
        Object.assign(s.user, p); saveSessionObj(s);
        toast(t('pg.account.savedOk'));
      } catch (_) { toast(t('pg.account.saveFail')); }
    });
    refreshWallet(); refreshLoyalty();
    renderCards(((s.user) || {}).cards || []);
  }

  function renderCards(cards) {
    const box = $('#saved-cards'); if (!box) return;
    if (!cards.length) { box.innerHTML = `<p class="muted">${t('pg.account.noCards')}</p>`; return; }
    box.innerHTML = cards.map((c) => `<div class="list-item"><span>💳</span><div class="content"><h3>•••• ${c.last_4_digits} (${c.card_brand || ''})</h3><p>${c.cardholder_name || ''}</p></div></div>`).join('');
  }

  {
    const top = $('#topup-btn');
    if (top) top.addEventListener('click', async () => {
      const amount = Number((($('#topup-amount') || {}).value || '').trim());
      const msg = $('#wallet-msg');
      const cardName = (($('#card-name') || {}).value || '').trim();
      const cardNumber = (($('#card-number') || {}).value || '').trim();
      const walletNumber = (($('#wallet-number') || {}).value || '').trim();
      const saveCard = !!($('#save-card') || {}).checked;
      const methodSel = (($('#topup-method') || {}).value || 'CARD').trim().toUpperCase();
      const payload = { amount };
      if (methodSel === 'WALLET') {
        payload.method = 'WALLET';
        payload.provider = 'E_WALLET';
        payload.wallet_number = walletNumber;
        payload.cardholder_name = cardName;
        payload.save_card = saveCard;
      } else {
        payload.method = 'CARD';
        payload.cardholder_name = cardName;
        payload.card_number = cardNumber;
        payload.save_card = saveCard;
      }
      try {
        const r = await api('/wallet/topup', { method: 'POST', body: payload });
        if ($('#wallet-balance')) $('#wallet-balance').textContent = r.balance + ' EGP';
        if (msg) msg.textContent = t('pg.account.topupOk');
        try {
          const s2 = session();
          if (s2) {
            const p = await api('/auth/profile?userId=' + encodeURIComponent(s2.user.email));
            Object.assign(s2.user, p);
            try { sessionStorage.setItem('velox_session', JSON.stringify(s2)); } catch (_) {}
            renderCards(p.cards || []);
          }
        } catch (_) {}
      } catch (_) { if (msg) msg.textContent = t('pg.account.topupFail'); }
    });
  }

  function buildGovSelect(current) {
    const sel = $('#acc-governorate'); if (!sel) return;
    sel.innerHTML = GOVS.map((g) => `<option value="${g}">${g}</option>`).join('');
    if (current) sel.value = current;
  }

  async function refreshWallet() {
    const s = session(); if (!s) return;
    try {
      const r = await api('/wallet/balance?userId=' + encodeURIComponent(s.user.email));
      if ($('#wallet-balance')) $('#wallet-balance').textContent = r.balance + ' EGP';
    } catch (_) {}
  }

  async function refreshLoyalty() {
    const s = session(); if (!s) return;
    try {
      const r = await api('/loyalty/status?userId=' + encodeURIComponent(s.user.email));
      const fill = $('#loyalty-fill'), msg = $('#loyalty-msg');
      if (fill) fill.style.width = Math.round((r.progressInCycle / r.ordersPerReward) * 100) + '%';
      if (msg) msg.textContent = r.message;
    } catch (_) {}
  }

  /* ---------------- orders + tracking + invoice ---------------- */
  let cachedOrders = [];
  let trackTimer = null;
  let trackCode = null;

  async function initOrders() {
    await loadOrders();
    const btn = $('#tracking-refresh');
    if (btn) btn.addEventListener('click', () => { if (trackCode) pollTracking(trackCode); });
    const inv = $('#tracking-invoice');
    if (inv) inv.addEventListener('click', downloadInvoice);
  }

  function fmtDT(iso) {
    try {
      const d = new Date(iso);
      const p = (n) => String(n).padStart(2, '0');
      return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}`;
    } catch (_) { return iso || ''; }
  }

  function withinDays(iso, days) {
    try { return (Date.now() - new Date(iso).getTime()) / 864e5 <= days; } catch (_) { return false; }
  }

  async function loadOrders() {
    const s = session(); if (!s) return;
    try {
      const r = await api('/orders/history?userId=' + encodeURIComponent(s.user.email) + '&page=0&size=50');
      cachedOrders = (r && r.content) || [];
    } catch (_) { cachedOrders = []; }
    try {
      const r = await api('/returns?userId=' + encodeURIComponent(s.user.email));
      cachedReturns = Array.isArray(r) ? r : [];
    } catch (_) { cachedReturns = []; }
    try {
      const r = await api('/complaints?userId=' + encodeURIComponent(s.user.email));
      cachedComplaints = Array.isArray(r) ? r.filter((c) => c.status !== 'RESOLVED') : [];
    } catch (_) { cachedComplaints = []; }
    renderOrdersCached();
  }

  let cachedReturns = [];
  let cachedComplaints = [];

  function activeOrders() {
    return cachedOrders.filter((o) => !['DELIVERED', 'RETURNED'].includes(o.status));
  }

  function bindOrderTabs() {
    const to = $('#tab-orders'), tr = $('#tab-returns');
    const list = $('#orders-list'), rlist = $('#returns-list');
    if (!to || !tr || !list || !rlist || to.dataset.bound) return;
    to.dataset.bound = '1';
    const show = (which) => {
      const showOrders = which === 'orders';
      list.hidden = !showOrders;
      rlist.hidden = showOrders;
      to.classList.toggle('is-active', showOrders);
      tr.classList.toggle('is-active', !showOrders);
    };
    to.addEventListener('click', () => show('orders'));
    tr.addEventListener('click', () => show('returns'));
    show('orders');
  }

  /* Order stages timeline (mirrors the returns style). */
  const ORDER_FLOW = ['PENDING', 'PROCESSING', 'IN_TRANSIT', 'SHIPPED', 'ARRIVED', 'DELIVERED'];
  function flowIdx(st) {
    if (st === 'PAID') st = 'SHIPPED';
    if (st === 'RETURNED') return ORDER_FLOW.length;
    const i = ORDER_FLOW.indexOf(st);
    return i < 0 ? 0 : i;
  }
  function miniTimeline(st) {
    const i = flowIdx(st);
    return ORDER_FLOW.map((s, k) => (k <= i ? '●' : '○')).join('');
  }
  function fullTimeline(st) {
    const i = flowIdx(st);
    return ORDER_FLOW.map((s, k) => `${k <= i ? '●' : '○'} ${s}`).join(' → ');
  }

  function renderOrdersCached() {
    const list = $('#orders-list'); if (!list) return;
    const L = lang();
    // --- orders tab: shipping stages only ---
    let html = '';
    if (!cachedOrders.length) {
      html += `<p>${t('pg.orders.empty')}</p>`;
    } else {
      html += cachedOrders.map((o) => {
        const canReturn = o.status === 'DELIVERED' && withinDays(o.orderDate, 14);
        return `
        <article class="list-item order-card" data-select="${o.orderId}"><span class="dot"></span><div class="content">
        <h3>${o.orderId} · ${o.status}</h3>
        <p>${miniTimeline(o.status)}</p>
        <p>${o.city || ''} · ${o.total} EGP · ${o.itemCount} items · ${fmtDT(o.orderDate)}</p>
        <div class="order-details" hidden>
          <p>${fullTimeline(o.status)}</p>
          <div><button type="button" class="btn-app secondary" data-invoice="${o.orderId}">PDF</button>`
          + (canReturn ? `<button type="button" class="btn-app secondary" data-return="${o.orderId}">${t('pg.orders.returnBtn')}</button>` : '')
          + `</div></div></div>
        </article>`;
      }).join('');
    }
    list.innerHTML = html;
    // --- returns tab: the 4-stage return pipeline only (never mixed above) ---
    const rlist = $('#returns-list');
    if (rlist) {
      const stages = ['RETURN_REQUESTED', 'RETURN_UNDER_REVIEW', 'RETURN_APPROVED', 'REFUND_PROCESSED'];
      rlist.innerHTML = cachedReturns.length ? cachedReturns.map((r) => `
        <article class="list-item"><span class="dot"></span><div class="content">
        <h3>#${r.id} · ${r.orderCode}</h3>
        <p>${stages.map((s) => `${stages.indexOf(s) <= stages.indexOf(r.status) ? '●' : '○'} ${s}`).join(' → ')}</p>
        <p class="muted">${fmtDT(r.updated_at)}</p></div></div>
        </article>`).join('') : `<p>${t('pg.orders.returnsEmpty')}</p>`;
    }
    bindOrderTabs();
    renderTrackDropdown();
    const metrics = document.querySelectorAll('.metric');
    const active = cachedOrders.filter((o) => !['DELIVERED', 'RETURNED'].includes(o.status)).length;
    const done = cachedOrders.filter((o) => o.status === 'DELIVERED').length;
    const ret = cachedOrders.filter((o) => o.status === 'RETURNED').length;
    if (metrics[0]) metrics[0].textContent = active;
    if (metrics[1]) metrics[1].textContent = done;
    if (metrics[2]) metrics[2].textContent = ret;
    list.querySelectorAll('[data-invoice]').forEach((b) => b.addEventListener('click', () => downloadInvoice(b.dataset.invoice)));
    list.querySelectorAll('[data-return]').forEach((b) => b.addEventListener('click', () => requestReturn(b.dataset.return)));
    list.querySelectorAll('[data-select]').forEach((card) => card.addEventListener('click', (e) => {
      if (e.target.closest('button')) return;
      card.classList.toggle('is-selected');
      const d = card.querySelector('.order-details');
      if (d) d.hidden = !card.classList.contains('is-selected');
    }));
    void L;
  }

  async function requestReturn(code) {
    try {
      await api('/returns', { method: 'POST', body: { orderId: code } });
      toast(t('pg.orders.returnOk'));
      await loadOrders();
    } catch (_) { toast(t('pg.orders.returnFail')); }
  }

  /* ---------------- tracking dropdown (orders + open complaints) ---------------- */
  function complaintStepName(st) {
    return st === 'RESOLVED' ? t('pg.support.stRESOLVED')
      : st === 'IN_REVIEW' ? t('pg.support.stIN_REVIEW') : t('pg.support.stPENDING');
  }

  function renderTrackDropdown() {
    const box = $('#tracking-box'); if (!box) return;
    let sel = $('#track-select');
    if (!sel) {
      sel = document.createElement('select');
      sel.id = 'track-select';
      sel.style.cssText = 'width:100%;margin-bottom:10px';
      box.prepend(sel);
      sel.addEventListener('change', () => {
        const v = sel.value; if (!v) return;
        if (v.startsWith('C:')) showComplaintTrack(v.slice(2));
        else openTracking(v);
        sel.value = '';
      });
    }
    const act = activeOrders();
    sel.innerHTML = `<option value="">${t('pg.orders.trackSelect')}</option>`
      + `<optgroup label="${t('pg.orders.trackOrders')}">`
      + act.map((o) => `<option value="${o.orderId}">${o.orderId} · ${o.status}</option>`).join('')
      + `</optgroup><optgroup label="${t('pg.orders.trackComplaints')}">`
      + cachedComplaints.map((c) => `<option value="C:${c.id}">#${c.id} · ${complaintStepName(c.status)}</option>`).join('')
      + `</optgroup>`;
  }

  function showComplaintTrack(id) {
    const c = cachedComplaints.find((x) => String(x.id) === String(id));
    const box = $('#tracking-box'); if (!box || !c) return;
    box.hidden = false;
    if (trackTimer) { clearInterval(trackTimer); trackTimer = null; }
    trackCode = null;
    $('#tracking-code').textContent = '#' + c.id;
    const flow = ['PENDING', 'IN_REVIEW', 'RESOLVED'];
    if ($('#tracking-status')) $('#tracking-status').textContent =
      flow.map((x) => `${flow.indexOf(x) <= flow.indexOf(c.status) ? '●' : '○'} ${complaintStepName(x)}`).join(' → ');
    const fill = $('#tracking-fill');
    if (fill) fill.style.width = Math.round(((flow.indexOf(c.status) + 1) / flow.length) * 100) + '%';
    if ($('#tracking-driver')) $('#tracking-driver').textContent = '';
  }

  async function openTracking(code) {
    trackCode = code;
    const box = $('#tracking-box'); if (box) box.hidden = false;
    await pollTracking(code);
    if (trackTimer) clearInterval(trackTimer);
    trackTimer = setInterval(() => pollTracking(trackCode), 5000);
  }

  async function pollTracking(code) {
    try {
      const r = await api('/orders/' + encodeURIComponent(code) + '/tracking');
      if ($('#tracking-code')) $('#tracking-code').textContent = r.orderId + ' · ' + r.currentStatus;
      if ($('#tracking-status')) $('#tracking-status').textContent = (r.timeline || []).map((s) => `${s.status}${s.reached ? ' ✓' : ''}`).join(' → ');
      if ($('#tracking-driver')) $('#tracking-driver').textContent = r.driverLocation ? t('pg.orders.driver') + ': ' + r.driverLocation : '';
      const fill = $('#tracking-fill');
      if (fill) {
        const steps = (r.timeline || []).filter((s) => s.reached).length;
        const total = (r.timeline || []).length || 1;
        fill.style.width = Math.round((steps / total) * 100) + '%';
      }
    } catch (_) {}
  }

  async function downloadInvoice(code) {
    const id = code || trackCode; if (!id) return;
    const s = session(); if (!s || !s.token) return;
    try {
      const res = await fetch(window.VELOX_CONFIG.API_BASE_URL + '/orders/' + encodeURIComponent(id) + '/invoice', {
        headers: { Authorization: 'Bearer ' + s.token }
      });
      if (!res.ok) throw new Error('failed');
      const blob = await res.blob();
      const a = document.createElement('a');
      a.href = URL.createObjectURL(blob);
      a.download = 'INV-' + id + '.pdf';
      document.body.appendChild(a); a.click();
      setTimeout(() => { URL.revokeObjectURL(a.href); a.remove(); }, 800);
    } catch (_) { toast(t('pg.orders.invoice')); }
  }

  /* ---------------- favorites ---------------- */
  function favIds() { try { const f = JSON.parse(localStorage.getItem('velox_favs') || '[]'); return Array.isArray(f) ? f : []; } catch (_) { return []; } }

  async function allProducts() {
    try {
      const r = await api('/products');
      if (Array.isArray(r) && r.length) return r;
    } catch (_) {}
    try { return window.VeloxCatalogService.getProducts(); } catch (_) { return []; }
  }

  async function initFavs() { renderFavs(); }

  async function renderFavs() {
    const grid = $('#favs-grid'); if (!grid) return;
    const ids = favIds();
    const empty = $('#favs-empty');
    if (!ids.length) { grid.innerHTML = ''; if (empty) empty.hidden = false; return; }
    if (empty) empty.hidden = true;
    const products = await allProducts();
    const L = lang();
    const items = products.filter((p) => ids.some((id) => Number(id) === Number(p.id)));
    grid.innerHTML = items.map((p) => {
      const name = L === 'ar' ? (p.nameAr || p.nameEn) : (p.nameEn || p.nameAr);
      return `<article class="product-card"><div class="product-media">`
        + (p.image ? `<img class="product-image" src="${p.image}" alt="" loading="lazy">` : `<span>🛍️</span>`)
        + `</div><div class="product-body"><h3>${name || ''}</h3><div class="price">${p.price} <small>EGP</small></div></div></article>`;
    }).join('');
  }

  /* ---------------- notifications (persistent) ---------------- */
  async function initNotifications() {
    const list = document.querySelector('.list');
    const s = session();
    if (!list || !s) return; // guests keep the static demo content
    try {
      const r = await api('/notifications?userId=' + encodeURIComponent(s.user.email) + '&limit=30');
      if (Array.isArray(r) && r.length) {
        list.innerHTML = r.map((n) => `
          <article class="list-item notification-item${n.is_read ? '' : ' unread'}"><span class="dot"></span>
          <div class="content"><h3>${esc(n.title)}</h3><p>${esc(n.message)}</p></div>
          <small class="muted">${fmtDT(n.created_at)}</small></article>`).join('');
      }
    } catch (_) {}
    const clear = $('#clear-notifications');
    if (clear && !clear.dataset.bound) {
      clear.dataset.bound = '1';
      clear.addEventListener('click', async () => {
        try {
          await api('/notifications/read-all', { method: 'PUT', body: { userId: s.user.email } });
          document.querySelectorAll('.notification-item').forEach((x) => x.classList.remove('unread'));
        } catch (_) {}
      });
    }
  }

  /* ---------------- my complaints tracker ---------------- */
  async function initMyComplaints() {
    const box = $('#my-complaints'); if (!box) return;
    const s = session(); if (!s) return;
    let items = [];
    try {
      const r = await api('/complaints?userId=' + encodeURIComponent(s.user.email));
      if (Array.isArray(r)) items = r;
    } catch (_) {}
    const stepName = (st) => st === 'RESOLVED' ? t('pg.support.stRESOLVED')
      : st === 'IN_REVIEW' ? t('pg.support.stIN_REVIEW') : t('pg.support.stPENDING');
    if (!items.length) { box.innerHTML = `<p class="muted">${t('pg.support.noComplaints')}</p>`; return; }
    const flow = ['PENDING', 'IN_REVIEW', 'RESOLVED'];
    box.innerHTML = items.map((c) => `
      <article class="list-item"><span class="dot"></span><div class="content">
      <h3>#${c.id}${c.orderCode ? ' · ' + esc(c.orderCode) : ''}</h3>
      <p>${flow.map((x) => `${flow.indexOf(x) <= flow.indexOf(c.status) ? '●' : '○'} ${stepName(x)}`).join(' → ')}</p>
      <p class="muted">${esc(c.details || '').slice(0, 120)} · ${fmtDT(c.created_at)}</p></div></div></article>`).join('');
  }

  /* ---------------- rating / feedback / support ---------------- */
  function initRating() {
    const form = $('#feedback-form'); if (!form) return;
    form.addEventListener('submit', async (e) => {
      e.preventDefault();
      const rating = Number(($('#rating-value') || {}).value || 0);
      const ta = form.querySelector('textarea');
      const orderInput = $('#rating-order');
      try {
        await api('/reviews', { method: 'POST', body: {
          rating, comment: ta ? ta.value : '',
          orderId: orderInput && orderInput.value ? orderInput.value.trim() : null
        }});
        toast(t('pg.rating.sentOk')); form.reset();
      } catch (_) { toast(t('pg.rating.sentFail')); }
    });
  }

  function initFeedback() {
    const form = $('#feedback-form'); if (!form) return;
    form.addEventListener('submit', async (e) => {
      e.preventDefault();
      const type = $('#fb-type') ? $('#fb-type').value : '';
      const order = $('#fb-order') ? $('#fb-order').value.trim() : '';
      const details = $('#fb-details') ? $('#fb-details').value.trim() : '';
      if (!details) return;
      try {
        await api('/complaints', { method: 'POST', body: {
          details: type ? '[' + type + '] ' + details : details,
          orderId: order || null
        }});
        toast(t('pg.feedback.sentOk')); form.reset();
      } catch (_) { toast(t('pg.rating.sentFail')); }
    });
  }

  function initSupport() {
    const form = $('#support-form'); if (!form) return;
    form.addEventListener('submit', async (e) => {
      e.preventDefault();
      const inputs = form.querySelectorAll('input,textarea');
      const name = inputs[0] ? inputs[0].value.trim() : '';
      const email = inputs[1] ? inputs[1].value.trim() : '';
      const msg = form.querySelector('textarea') ? form.querySelector('textarea').value.trim() : '';
      if (!msg) return;
      try {
        await api('/complaints', { method: 'POST', body: { details: name + ' <' + email + '>: ' + msg } });
        toast(t('pg.support.sentOk')); form.reset();
      } catch (_) { toast(t('pg.rating.sentFail')); }
    });
  }
})();
