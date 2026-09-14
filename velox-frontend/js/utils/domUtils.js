/**
 * VELOX — Small DOM helpers shared by page scripts.
 */
(function () {
  function qs(selector, scope) {
    return (scope || document).querySelector(selector);
  }

  function setFieldError(fieldEl, message) {
    fieldEl.classList.add('has-error');
    const hint = fieldEl.querySelector('.field-hint');
    if (hint) hint.textContent = message || '';
  }

  function clearFieldError(fieldEl) {
    fieldEl.classList.remove('has-error');
    const hint = fieldEl.querySelector('.field-hint');
    if (hint) hint.textContent = '';
  }

  function setButtonLoading(buttonEl, isLoading) {
    buttonEl.disabled = isLoading;
    buttonEl.classList.toggle('is-loading', isLoading);
  }

  function showAlert(containerEl, type, message) {
    containerEl.innerHTML = '';
    if (!message) return;
    const el = document.createElement('div');
    el.className = `alert alert-${type}`;
    el.setAttribute('role', type === 'error' ? 'alert' : 'status');
    el.textContent = message;
    containerEl.appendChild(el);
  }

  function clearAlert(containerEl) {
    containerEl.innerHTML = '';
  }

  function setupPasswordToggle(inputEl, buttonEl) {
    buttonEl.addEventListener('click', () => {
      const isPassword = inputEl.type === 'password';
      inputEl.type = isPassword ? 'text' : 'password';
      const key = isPassword ? 'form.password.hide' : 'form.password.show';
      const label = window.VeloxI18n.translate(key, window.VeloxI18n.getLang());
      buttonEl.setAttribute('aria-label', label);
      buttonEl.innerHTML = isPassword ? EYE_OFF_ICON : EYE_ICON;
    });
  }

  const EYE_ICON = '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M1 12s4-7 11-7 11 7 11 7-4 7-11 7-11-7-11-7Z"/><circle cx="12" cy="12" r="3"/></svg>';
  const EYE_OFF_ICON = '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M17.94 17.94A10.94 10.94 0 0 1 12 19c-7 0-11-7-11-7a21.6 21.6 0 0 1 5.06-5.94M9.9 4.24A10.4 10.4 0 0 1 12 4c7 0 11 7 11 7a21.6 21.6 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"/><line x1="1" y1="1" x2="23" y2="23"/></svg>';

  window.VeloxDom = {
    qs,
    setFieldError,
    clearFieldError,
    setButtonLoading,
    showAlert,
    clearAlert,
    setupPasswordToggle,
    EYE_ICON,
    EYE_OFF_ICON,
  };
})();
