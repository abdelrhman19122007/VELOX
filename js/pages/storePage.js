/**
 * VELOX — Store details page (?id=).
 * Cover + info + full menu with quick-add + customer reviews.
 * Cart format matches the storefront (localStorage velox_cart).
 */
(function () {
  const $ = (s) => document.querySelector(s);
  const lang = () => (window.VeloxI18n ? window.VeloxI18n.getLang() : 'ar');
  const AR = () => lang() === 'ar';
  const esc = (s) => String(s == null ? '' : s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
  const api = (p) => window.VeloxApiClient.request(p);

  function toast(msg, type) {
    let el = $('#velox-toast');
    if (!el) { el = document.createElement('div'); el.id = 'velox-toast'; el.className = 'velox-toast'; document.body.appendChild(el); }
    el.className = 'velox-toast is-' + (type || 'success') + ' is-visible';
    el.textContent = msg;
    clearTimeout(toast.t);
    toast.t = setTimeout(() => el.classList.remove('is-visible'), 2800);
  }

  function etaFor(store) {
    if ((store.type || '').toUpperCase() === 'RESTAURANT') {
      const base = 20 + (Number(store.id) * 3) % 12;
      return AR() ? `${base}–${base + 10} دقيقة` : `${base}-${base + 10} min`;
    }
    return AR() ? 'خلال 1–2 يوم' : 'In 1-2 days';
  }

  function addToCart(id) {
    let cart = [];
    try { cart = JSON.parse(localStorage.getItem('velox_cart') || '[]'); } catch (_) {}
    if (!Array.isArray(cart)) cart = [];
    const ex = cart.find((i) => Number(i.id) === Number(id));
    if (ex) ex.qty += 1; else cart.push({ id: Number(id), qty: 1 });
    try { localStorage.setItem('velox_cart', JSON.stringify(cart)); } catch (_) {}
    document.dispatchEvent(new CustomEvent('velox:cartchange', { detail: { count: cart.reduce((s, i) => s + Number(i.qty), 0) } }));
    toast(AR() ? 'تمت الإضافة للسلة' : 'Added to cart');
  }

  async function boot() {
    const id = new URLSearchParams(window.location.search).get('id');
    if (!id) { window.location.href = 'home.html'; return; }
    try {
      const stores = await api('/stores');
      const store = stores.find((s) => Number(s.id) === Number(id));
      if (!store) { window.location.href = 'home.html'; return; }
      const name = AR() ? (store.nameAr || store.name) : (store.name || store.nameAr);
      document.title = name + ' | VELOX';
      $('#store-cover').src = store.cover || 'assets/images/velox-logo.jpeg';
      $('#store-type').textContent = store.type || '';
      $('#store-name').textContent = name;
      const rating = store.rating != null ? `★ ${Number(store.rating).toFixed(1)} (${store.reviewsCount})` : (AR() ? 'جديد' : 'New');
      $('#store-meta').textContent = `${rating} · ${store.productsCount} ${AR() ? 'منتج' : 'products'}`;
      let fee = '';
      try {
        const gov = await api('/governorates/CAIRO/shipping');
        fee = `${Number(gov.shippingPrice).toFixed(0)} EGP`;
      } catch (_) { fee = '—'; }
      $('#store-chips').innerHTML = `<span class="info-chip">🚚 ${esc(fee)}</span>`
        + `<span class="info-chip">⏱ ${esc(etaFor(store))}</span>`
        + `<span class="info-chip">${rating}</span>`;

      const products = await api('/products');
      const menu = products.filter((p) => Number(p.store_id) === Number(id));
      $('#menu-title').textContent = `${AR() ? 'القائمة' : 'Menu'} (${menu.length})`;
      $('#menu-grid').innerHTML = menu.map((p) => {
        const nm = AR() ? (p.nameAr || p.nameEn) : (p.nameEn || p.nameAr);
        return `<article class="product-card"><div class="product-media">`
          + (p.image ? `<img class="product-image" src="${esc(p.image)}" alt="" loading="lazy" onerror="this.style.display='none'">` : `<span>🛍️</span>`)
          + `</div><div class="product-body"><h3>${esc(nm)}</h3>`
          + `<div class="product-bottom"><div class="price">${Number(p.price).toFixed(0)} <small>EGP</small></div>`
          + `<button type="button" class="add-btn" data-add="${p.id}" aria-label="+">+</button></div></div></article>`;
      }).join('') || `<p>${AR() ? 'لا أصناف متاحة حالياً.' : 'No items right now.'}</p>`;
      $('#menu-grid').querySelectorAll('[data-add]').forEach((b) =>
        b.addEventListener('click', () => addToCart(b.dataset.add)));

      const reviews = await api('/reviews?storeId=' + encodeURIComponent(id));
      $('#reviews-list').innerHTML = reviews.length ? reviews.map((r) =>
        `<article class="list-item"><span class="dot"></span><div class="content"><h3>${esc(r.author)} · ${'★'.repeat(r.rating)}${'☆'.repeat(5 - r.rating)}</h3><p>${esc(r.comment)}</p></div></article>`
      ).join('') : `<p class="muted">${AR() ? 'كن أول من يقيّم هذا المتجر.' : 'Be the first to review this store.'}</p>`;
    } catch (_) {
      toast(AR() ? 'تعذر تحميل المتجر.' : 'Could not load the store.', 'error');
    }
    const themeBtn = $('#store-theme');
    if (themeBtn) themeBtn.addEventListener('click', () => window.VeloxTheme.toggleTheme());
  }

  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', boot);
  else boot();
})();
