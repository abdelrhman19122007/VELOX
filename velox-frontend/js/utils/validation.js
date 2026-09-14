/**
 * VELOX — Form validation helpers (pure functions, no DOM access).
 * Client-side validation only smooths the experience; the backend
 * must always re-validate (see security notes in authService.js).
 */
(function () {
  const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  // Egyptian mobile numbers: 01 + [0,1,2,5] + 8 digits = 11 digits total.
  const EG_PHONE_RE = /^01[0125][0-9]{8}$/;

  function isRequired(value) {
    return typeof value === 'string' && value.trim().length > 0;
  }

  function isValidEmail(value) {
    return EMAIL_RE.test(String(value).trim());
  }

  function isValidEgyptPhone(value) {
    return EG_PHONE_RE.test(String(value).trim());
  }

  /**
   * Returns a strength score from 0-4 based on length and character
   * variety. Intentionally simple — real strength policy lives server-side.
   */
  function passwordStrength(value) {
    value = value || '';
    let score = 0;
    if (value.length >= 8) score++;
    if (value.length >= 12) score++;
    if (/[A-Z]/.test(value) && /[a-z]/.test(value)) score++;
    if (/[0-9]/.test(value) && /[^A-Za-z0-9]/.test(value)) score++;
    return Math.min(score, 4);
  }

  function hasMinLength(value, min) {
    return typeof value === 'string' && value.trim().length >= min;
  }

  window.VeloxValidation = {
    isRequired,
    isValidEmail,
    isValidEgyptPhone,
    passwordStrength,
    hasMinLength,
  };
})();
