/**
 * VELOX — Shared theme + language toggle wiring.
 * Any page that includes a #theme-toggle and/or #lang-toggle button
 * gets working controls for free, with no per-page duplication.
 */
(function () {
  const SUN_ICON = '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="4"/><path d="M12 2v2M12 20v2M4.93 4.93l1.41 1.41M17.66 17.66l1.41 1.41M2 12h2M20 12h2M6.34 17.66l-1.41 1.41M19.07 4.93l-1.41 1.41"/></svg>';
  const MOON_ICON = '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79Z"/></svg>';

  function syncThemeIcon(btn) {
    if (!btn) return;
    btn.innerHTML = window.VeloxTheme.getTheme() === 'dark' ? SUN_ICON : MOON_ICON;
  }

  document.addEventListener('DOMContentLoaded', () => {
    const sideNav = document.getElementById('side-nav');
    const sideBackdrop = document.getElementById('side-nav-backdrop');
    const openSide = () => { if (!sideNav) return; document.dispatchEvent(new CustomEvent('velox:open-side')); sideNav.classList.add('is-open'); sideNav.setAttribute('aria-hidden','false'); if(sideBackdrop) sideBackdrop.hidden=false; document.body.classList.add('side-nav-open'); };
    const closeSide = () => { if (!sideNav) return; sideNav.classList.remove('is-open'); sideNav.setAttribute('aria-hidden','true'); if(sideBackdrop) sideBackdrop.hidden=true; document.body.classList.remove('side-nav-open'); };
    document.getElementById('side-menu-toggle')?.addEventListener('click', openSide);
    document.getElementById('side-nav-close')?.addEventListener('click', closeSide);
    sideBackdrop?.addEventListener('click', closeSide);
    document.addEventListener('keydown', (e) => { if (e.key === 'Escape') closeSide(); });
    document.querySelectorAll('[data-side-target], .side-nav a[href$=\".html\"]').forEach((link) => link.addEventListener('click', closeSide));
    document.getElementById('side-cart-link')?.addEventListener('click', () => { closeSide(); document.getElementById('cart-toggle')?.click(); });
        document.getElementById('side-location-link')?.addEventListener('click', () => { closeSide(); document.getElementById('location-btn')?.click(); });
    document.getElementById('side-theme-link')?.addEventListener('click', () => document.getElementById('theme-toggle')?.click());
    document.getElementById('side-lang-link')?.addEventListener('click', () => document.getElementById('lang-toggle')?.click());
    const themeBtn = document.getElementById('theme-toggle');
    const langBtn = document.getElementById('lang-toggle');

    if (themeBtn) {
      syncThemeIcon(themeBtn);
      themeBtn.addEventListener('click', () => {
        window.VeloxTheme.toggleTheme();
        syncThemeIcon(themeBtn);
      });
    }

    if (langBtn) {
      langBtn.addEventListener('click', () => window.VeloxI18n.toggleLanguage());
    }
  });

  document.addEventListener('velox:cartchange', (e) => { const count = document.getElementById('side-cart-count'); if (count) count.textContent = e.detail?.count ?? document.getElementById('cart-count')?.textContent ?? '0'; });

  // Panel isolation: opening the cart closes the sidebar (and vice versa),
  // so only one backdrop is ever visible.
  document.addEventListener('velox:open-cart', () => closeSide());
  // Header cart icon toggles the drawer (single handler — pages must NOT
  // bind #cart-toggle themselves). Emits open/close events the drawer listens to.
  function toggleCart(){
    const d=document.getElementById('cart-drawer');
    if(!d) return;
    const willOpen=d.hidden||!d.classList.contains('is-open');
    document.dispatchEvent(new CustomEvent(willOpen?'velox:open-cart':'velox:close-cart'));
  }
  document.getElementById('cart-toggle')?.addEventListener('click',toggleCart);
  window.VeloxNav={toggleCart};

  // Direction mirror: header uses flex (auto-mirrors), while JS-driven widgets
  // read this to position toasts/drawers correctly in RTL vs LTR.
  const syncDirection = () => { document.body.dataset.dir = document.documentElement.dir || 'rtl'; };
  syncDirection();
  document.addEventListener('velox:langchange', syncDirection);

  document.addEventListener('velox:themechange', () => syncThemeIcon(document.getElementById('theme-toggle')));
})();
