/**
 * VELOX — Register page controller.
 */
(function () {
  document.addEventListener('DOMContentLoaded', () => {
    const { qs, setFieldError, clearFieldError, setButtonLoading, showAlert, clearAlert, setupPasswordToggle } = window.VeloxDom;
    const { isRequired, isValidEmail, isValidEgyptPhone, hasMinLength, passwordStrength } = window.VeloxValidation;
    const t = (key) => window.VeloxI18n.translate(key, window.VeloxI18n.getLang());

    const form = qs('#register-form');
    const nameField = qs('#field-fullname');
    const nameInput = qs('#fullname');
    const emailField = qs('#field-email');
    const emailInput = qs('#email');
    const phoneField = qs('#field-phone');
    const phoneInput = qs('#phone');
    const govField = qs('#field-governorate');
    const govSelect = qs('#governorate');
    const passwordField = qs('#field-password');
    const passwordInput = qs('#password');
    const passwordToggleBtn = qs('#password-toggle');
    const confirmField = qs('#field-confirm-password');
    const confirmInput = qs('#confirm-password');
    const strengthMeter = qs('#strength-meter');
    const strengthLabel = qs('#strength-label');
    const submitBtn = qs('#register-submit');
    const alertBox = qs('#register-alert');

    setupPasswordToggle(passwordInput, passwordToggleBtn);

    passwordInput.addEventListener('input', () => {
      const score = passwordStrength(passwordInput.value);
      strengthMeter.setAttribute('data-level', passwordInput.value ? score || 1 : 0);
      strengthLabel.textContent = passwordInput.value ? t(`strength.${score}`) : '';
    });

    function validate() {
      let valid = true;

      if (!isRequired(nameInput.value)) {
        setFieldError(nameField, t('validation.required'));
        valid = false;
      } else clearFieldError(nameField);

      if (!isRequired(emailInput.value)) {
        setFieldError(emailField, t('validation.required'));
        valid = false;
      } else if (!isValidEmail(emailInput.value)) {
        setFieldError(emailField, t('validation.email.invalid'));
        valid = false;
      } else clearFieldError(emailField);

      if (!isRequired(phoneInput.value)) {
        setFieldError(phoneField, t('validation.required'));
        valid = false;
      } else if (!isValidEgyptPhone(phoneInput.value)) {
        setFieldError(phoneField, t('validation.phone.invalid'));
        valid = false;
      } else clearFieldError(phoneField);

      if (!govSelect.value) {
        setFieldError(govField, t('validation.required'));
        valid = false;
      } else clearFieldError(govField);

      if (!isRequired(passwordInput.value)) {
        setFieldError(passwordField, t('validation.required'));
        valid = false;
      } else if (!hasMinLength(passwordInput.value, 8)) {
        setFieldError(passwordField, t('validation.password.minLength'));
        valid = false;
      } else clearFieldError(passwordField);

      if (confirmInput.value !== passwordInput.value || !isRequired(confirmInput.value)) {
        setFieldError(confirmField, t('validation.confirmPassword.mismatch'));
        valid = false;
      } else clearFieldError(confirmField);

      return valid;
    }

    [nameInput, emailInput, phoneInput, govSelect, passwordInput, confirmInput].forEach((input) => {
      input.addEventListener('input', () => clearFieldError(input.closest('.field')));
      input.addEventListener('change', () => clearFieldError(input.closest('.field')));
    });

    form.addEventListener('submit', async (event) => {
      event.preventDefault();
      clearAlert(alertBox);
      if (!validate()) return;

      setButtonLoading(submitBtn, true);
      try {
        await window.VeloxAuthService.register({
          full_name: nameInput.value.trim(),
          email: emailInput.value.trim(),
          password: passwordInput.value,
          phone_number: phoneInput.value.trim(),
          governorate: govSelect.value,
        });
        showAlert(alertBox, 'success', t('register.success'));
        setTimeout(() => {
          window.location.href = 'login.html?registered=1';
        }, 900);
      } catch (err) {
        setButtonLoading(submitBtn, false);
        showAlert(alertBox, 'error', t(err.i18nKey || 'register.error.generic'));
      }
    });
  });
})();
