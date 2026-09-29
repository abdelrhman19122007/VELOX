(function () {
  const SESSION_KEY = 'velox_session';
  const USERS_KEY = 'velox_mock_users';
  const seed = {
    id: 1,
    full_name: 'Demo User',
    email: 'demo@example.com',
    phone_number: '01012345678',
    governorate: 'DAMIETTA',
    role: 'CUSTOMER',
    password: 'Velox1234'
  };

  function readUsers() {
    try {
      const users = JSON.parse(localStorage.getItem(USERS_KEY));
      if (Array.isArray(users) && users.length) {
        return users.map((user) => ({
          ...user,
          password: user.password || (user.email === seed.email ? seed.password : '')
        }));
      }
    } catch (_) {}
    return [seed];
  }
  function writeUsers(users) { localStorage.setItem(USERS_KEY, JSON.stringify(users)); }
  function delay(ms) { return new Promise((resolve) => setTimeout(resolve, ms)); }
  function saveSession(token, user) {
    const safeUser = { ...user };
    delete safeUser.password;
    sessionStorage.setItem(SESSION_KEY, JSON.stringify({ token, user: safeUser }));
  }
  function getSession() { try { return JSON.parse(sessionStorage.getItem(SESSION_KEY)); } catch (_) { return null; } }
  function logout() { sessionStorage.removeItem(SESSION_KEY); }

  async function login({ email, password }) {
    const cfg = window.VELOX_CONFIG;
    if (!cfg.USE_MOCK_API) {
      const data = await window.VeloxApiClient.request(cfg.ENDPOINTS.login, { method: 'POST', body: { email, password } });
      if (!data?.token || !data?.user) throw new Error('Invalid login response');
      saveSession(data.token, data.user);
      return data.user;
    }
    await delay(cfg.MOCK_LATENCY_MS);
    const user = readUsers().find((u) => u.email.toLowerCase() === email.trim().toLowerCase());
    if (!user || user.password !== password) {
      const error = new Error('Invalid credentials');
      error.i18nKey = 'login.error.invalid';
      throw error;
    }
    saveSession('mock-token-' + user.id, user);
    return user;
  }

  async function register(payload) {
    const cfg = window.VELOX_CONFIG;
    if (!cfg.USE_MOCK_API) {
      const data = await window.VeloxApiClient.request(cfg.ENDPOINTS.register, { method: 'POST', body: payload });
      // New accounts stay inactive until OTP verification: pass pending through.
      if (data && data.pending) {
        return { pending: true, email: data.email, otp: data.otp || null };
      }
      if (!data?.token || !data?.user) throw new Error('Invalid register response');
      saveSession(data.token, data.user);
      return data.user;
    }
    await delay(cfg.MOCK_LATENCY_MS);
    const users = readUsers();
    if (users.some((u) => u.email.toLowerCase() === payload.email.trim().toLowerCase())) {
      const error = new Error('Email already used');
      error.i18nKey = 'register.error.emailTaken';
      throw error;
    }
    const user = {
      id: Date.now(),
      full_name: payload.full_name.trim(),
      email: payload.email.trim(),
      phone_number: payload.phone_number,
      governorate: payload.governorate,
      role: 'CUSTOMER',
      password: payload.password
    };
    users.push(user);
    writeUsers(users);
    saveSession('mock-token-' + user.id, user);
    return user;
  }

  async function googleLogin(credential) {
    const data = await window.VeloxApiClient.request('/auth/google', { method: 'POST', body: { credential } });
    if (!data?.token || !data?.user) throw new Error('Invalid Google login response');
    saveSession(data.token, data.user);
    return data.user;
  }

  window.VeloxAuthService = { login, googleLogin, register, verifyOtp, resendOtp, logout, getSession };

  async function verifyOtp({ email, code }) {
    const cfg = window.VELOX_CONFIG;
    if (!cfg.USE_MOCK_API) {
      const data = await window.VeloxApiClient.request('/auth/verify-otp', {
        method: 'POST', body: { email, code },
      });
      if (!data?.token || !data?.user) {
        const error = new Error('Invalid OTP');
        error.i18nKey = 'otp.error.invalid';
        throw error;
      }
      saveSession(data.token, data.user);
      return data.user;
    }
    await delay(cfg.MOCK_LATENCY_MS);
    const user = readUsers().find((u) => u.email.toLowerCase() === String(email).trim().toLowerCase());
    if (!user) throw new Error('Account not found');
    saveSession('mock-token-' + user.id, user);
    return user;
  }

  async function resendOtp(email) {
    const cfg = window.VELOX_CONFIG;
    if (cfg.USE_MOCK_API) return { email, otp: null };
    return window.VeloxApiClient.request('/auth/resend-otp', { method: 'POST', body: { email } });
  }
})();
