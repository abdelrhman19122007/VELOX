/**
 * VELOX — Login page controller.
 * Owns DOM wiring only; all business logic lives in services/utils.
 */
(function () {
  document.addEventListener('DOMContentLoaded', () => {
    const { qs, setFieldError, clearFieldError, setButtonLoading, showAlert, clearAlert, setupPasswordToggle } = window.VeloxDom;
    const { isRequired, isValidEmail } = window.VeloxValidation;
    const t = (key) => window.VeloxI18n.translate(key, window.VeloxI18n.getLang());

    const form = qs('#login-form');
    const emailField = qs('#field-email');
    const emailInput = qs('#email');
    const passwordField = qs('#field-password');
    const passwordInput = qs('#password');
    const passwordToggleBtn = qs('#password-toggle');
    const submitBtn = qs('#login-submit');
    const alertBox = qs('#login-alert');

    setupPasswordToggle(passwordInput, passwordToggleBtn);

    // If we just arrived from a successful registration, greet the user.
    const params = new URLSearchParams(window.location.search);
    if (params.get('registered') === '1') {
      showAlert(alertBox, 'success', t('register.success'));
    }

    function validate() {
      let valid = true;

      if (!isRequired(emailInput.value)) {
        setFieldError(emailField, t('validation.required'));
        valid = false;
      } else if (!isValidEmail(emailInput.value)) {
        setFieldError(emailField, t('validation.email.invalid'));
        valid = false;
      } else {
        clearFieldError(emailField);
      }

      if (!isRequired(passwordInput.value)) {
        setFieldError(passwordField, t('validation.required'));
        valid = false;
      } else {
        clearFieldError(passwordField);
      }

      return valid;
    }

    [emailInput, passwordInput].forEach((input) => {
      input.addEventListener('input', () => clearFieldError(input.closest('.field')));
    });

    form.addEventListener('submit', async (event) => {
      event.preventDefault();
      clearAlert(alertBox);
      if (!validate()) return;

      setButtonLoading(submitBtn, true);
      try {
        const user = await window.VeloxAuthService.login({
          email: emailInput.value.trim(),
          password: passwordInput.value,
        });
        showAlert(alertBox, 'success', t('login.success'));
        setTimeout(() => {
          window.location.href = `index.html?welcome=${encodeURIComponent(user.full_name)}`;
        }, 700);
      } catch (err) {
        setButtonLoading(submitBtn, false);
        showAlert(alertBox, 'error', t(err.i18nKey || 'login.error.generic'));
      }
    });
  });
})();
