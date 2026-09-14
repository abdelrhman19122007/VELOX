/**
 * VELOX — Generic API client.
 * All real network calls funnel through here, kept deliberately
 * separate from UI rendering (see project brief §20). Once the
 * backend exposes real endpoints, only config.js and this file
 * need to know about it — page scripts never call fetch() directly.
 */
(function () {
  class ApiError extends Error {
    constructor(message, status) {
      super(message);
      this.status = status;
    }
  }

  async function request(path, { method = 'GET', body, timeoutMs = 10000 } = {}) {
    const controller = new AbortController();
    const timeout = setTimeout(() => controller.abort(), timeoutMs);

    // Token issued by POST /api/auth/login|register (stored in session by authService).
    // No cookies are used, so credentials stay 'omit' (works from file:// too).
    let authHeader = {};
    try {
      const token = window.VeloxAuthService && window.VeloxAuthService.getSession
        ? window.VeloxAuthService.getSession()?.token
        : null;
      if (token) authHeader = { Authorization: `Bearer ${token}` };
    } catch (_) {}

    try {
      const response = await fetch(`${window.VELOX_CONFIG.API_BASE_URL}${path}`, {
        method,
        headers: { 'Content-Type': 'application/json', ...authHeader },
        credentials: 'omit', // token-based auth; no cookies involved
        cache: method === 'GET' ? 'no-store' : 'default',
        body: body ? JSON.stringify(body) : undefined,
        signal: controller.signal,
      });

      let data = null;
      try { data = await response.json(); } catch (_) { /* empty body */ }

      if (!response.ok) {
        throw new ApiError((data && data.message) || `Request failed (${response.status})`, response.status);
      }
      return data;
    } catch (err) {
      if (err.name === 'AbortError') {
        throw new ApiError('The request took too long. Please try again.', 0);
      }
      throw err;
    } finally {
      clearTimeout(timeout);
    }
  }

  window.VeloxApiClient = { request, ApiError };
})();
