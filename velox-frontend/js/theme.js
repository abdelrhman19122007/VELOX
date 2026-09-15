/**
 * VELOX — Theme (light/dark) switcher.
 * Persists the user's choice so it survives reopening the app.
 * A tiny inline snippet in each page's <head> (see login.html /
 * register.html) applies the stored theme before first paint to
 * avoid a flash of the wrong theme; this file owns the toggle logic.
 */
(function () {
  const STORAGE_KEY = 'velox_theme';

  function getTheme() {
    const stored = localStorage.getItem(STORAGE_KEY);
    if (stored) return stored;
    if (window.matchMedia && window.matchMedia('(prefers-color-scheme: light)').matches) return 'light';
    return 'dark';
  }

  function applyTheme(theme) {
    theme = theme === 'dark' ? 'dark' : 'light';
    localStorage.setItem(STORAGE_KEY, theme);
    document.documentElement.setAttribute('data-theme', theme);
    document.dispatchEvent(new CustomEvent('velox:themechange', { detail: { theme } }));
  }

  function toggleTheme() {
    applyTheme(getTheme() === 'dark' ? 'light' : 'dark');
  }

  window.VeloxTheme = { getTheme, applyTheme, toggleTheme };
})();
