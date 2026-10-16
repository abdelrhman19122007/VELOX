/**
 * VELOX — All-restaurants directory (Talabat-style grid + live search).
 * Cards show the logo tile, name + branch, cuisine tags and rating.
 */
(function () {
  const $ = (s) => document.querySelector(s);
  const lang = () => (window.VeloxI18n ? window.VeloxI18n.getLang() : 'ar');
  const AR = () => lang() === 'ar';
  const esc = (s) => String(s == null ? '' : s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
  let all = [];

  function card(s) {
    const name = AR() ? (s.nameAr || s.name) : (s.name || s.nameAr);
    const title = s.branch ? `${name} - ${s.branch}` : name;
    const logo = s.logo
      ? `<img src="${esc(s.logo)}" alt="" loading="lazy" onerror="this.remove()">`
      : `<span class="logo-fallback">${esc(String(name || 'V').trim().charAt(0))}</span>`;
    const rating = s.rating != null
      ? `<span class="rest-rating">⭐ ${Number(s.rating).toFixed(1)} <small>(${s.reviewsCount})</small></span>` : '';
    return `<button type="button" class="rest-card" data-store="${s.id}">`
      + `<span class="rest-logo">${logo}</span>`
      + `<span class="rest-info"><strong>${esc(title)}</strong>`
      + `<small>${esc(s.cuisine || s.type || '')}</small>${rating}</span></button>`;
  }

  function render(filter) {
    const q = (filter || '').trim().toLowerCase();
    const list = !q ? all : all.filter((s) =>
      [s.name, s.nameAr, s.cuisine, s.branch, s.type].some((v) =>
        String(v || '').toLowerCase().includes(q)));
    $('#rest-grid').innerHTML = list.map(card).join('');
    $('#rest-empty').hidden = list.length > 0;
    $('#rest-grid').querySelectorAll('[data-store]').forEach((b) => b.addEventListener('click', () => {
      window.location.href = `store.html?id=${encodeURIComponent(b.dataset.store)}`;
    }));
  }

  async function boot() {
    try {
      all = await window.VeloxApiClient.request('/stores');
    } catch (_) { all = []; }
    render('');
    $('#rest-search').addEventListener('input', (e) => render(e.target.value));
    document.addEventListener('velox:langchange', () => render($('#rest-search').value));
    const themeBtn = $('#rest-theme');
    if (themeBtn) themeBtn.addEventListener('click', () => window.VeloxTheme.toggleTheme());
  }

  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', boot);
  else boot();
})();
