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
    let verifyingOtp = false;

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
      if (verifyingOtp) {
        const codeInput = qs('#login-otp-code');
        if (!/^\d{6}$/.test((codeInput?.value || '').trim())) {
          showAlert(alertBox, 'error', t('otp.error.invalid'));
          return;
        }
        setButtonLoading(submitBtn, true);
        try {
          const user = await window.VeloxAuthService.verifyOtp({
            email: emailInput.value.trim(),
            code: codeInput.value.trim(),
          });
          showAlert(alertBox, 'success', t('otp.success'));
          setTimeout(() => { window.location.href = `index.html?welcome=${encodeURIComponent(user.full_name)}`; }, 700);
        } catch (err) {
          showAlert(alertBox, 'error', t('otp.error.invalid'));
          setButtonLoading(submitBtn, false);
        }
        return;
      }
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
        if (err.code === 'ACCOUNT_NOT_VERIFIED' || /not verified|غير مفعل/i.test(err.message || '')) {
          try {
            const result = await window.VeloxAuthService.resendOtp(emailInput.value.trim());
            verifyingOtp = true;
            passwordField.hidden = true;
            emailInput.readOnly = true;
            const otpField = document.createElement('div');
            otpField.className = 'field';
            otpField.id = 'field-login-otp';
            otpField.innerHTML = `<label for="login-otp-code">${t('otp.label')}</label><input type="text" id="login-otp-code" inputmode="numeric" maxlength="6" autocomplete="one-time-code" placeholder="••••••"><div class="field-hint"></div>`;
            passwordField.after(otpField);
            submitBtn.querySelector('.btn-label').textContent = t('otp.submit');
            const prefix = window.VeloxI18n.getLang() === 'ar' ? 'الحساب محتاج تفعيل. اكتب الكود التالي: ' : 'Your account needs verification. Enter this code: ';
            showAlert(alertBox, 'success', prefix + (result.otp || ''));
            qs('#login-otp-code').focus();
          } catch (resendError) {
            showAlert(alertBox, 'error', resendError.message || t('login.error.unverified'));
          }
          setButtonLoading(submitBtn, false);
          return;
        }
        const msg = err.status === 401 ? t('login.error.invalid') : t(err.i18nKey || 'login.error.generic');
        setButtonLoading(submitBtn, false);
        showAlert(alertBox, 'error', msg);
      }
    });
  });
})();
