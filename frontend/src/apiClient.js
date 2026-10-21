const API_ROOT = (import.meta.env.VITE_API_BASE_URL || '/api').replace(/\/$/, '');
let csrfPromise = null;

export function resetCsrf() {
  csrfPromise = null;
}

export async function getCsrf() {
  if (!csrfPromise) {
    csrfPromise = fetch(API_ROOT + '/csrf', { credentials: 'same-origin' })
      .then(async (response) => {
        if (!response.ok) throw new Error('보안 토큰을 가져오지 못했습니다.');
        const value = await response.json();
        if (!value.token || !value.headerName) throw new Error('보안 토큰 응답이 올바르지 않습니다.');
        return value;
      })
      .catch((error) => {
        resetCsrf();
        throw error;
      });
  }
  return csrfPromise;
}

export async function apiRequest(path, { method = 'GET', body } = {}) {
  const headers = { Accept: 'application/json' };
  if (body !== undefined) headers['Content-Type'] = 'application/json';
  if (!['GET', 'HEAD', 'OPTIONS'].includes(method.toUpperCase())) {
    const csrf = await getCsrf();
    headers[csrf.headerName] = csrf.token;
  }
  const response = await fetch(API_ROOT + path, {
    method,
    headers,
    credentials: 'same-origin',
    ...(body === undefined ? {} : { body: JSON.stringify(body) }),
  });
  const raw = await response.text();
  let result = null;
  if (raw) {
    try {
      result = JSON.parse(raw);
    } catch {
      if (response.ok) throw new Error('서버 응답을 해석할 수 없습니다.');
    }
  }
  if (!response.ok) {
    const error = new Error(
      typeof result?.message === 'string' && result.message.trim()
        ? result.message
        : '요청을 처리하지 못했습니다.'
    );
    error.status = response.status;
    throw error;
  }
  return result;
}

export async function login(email, password) {
  const csrf = await getCsrf();
  const response = await fetch(API_ROOT + '/login', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded',
      [csrf.headerName]: csrf.token,
    },
    body: new URLSearchParams({ email, password }),
    credentials: 'same-origin',
  });
  if (!response.ok) {
    const error = new Error(response.status === 401
      ? '이메일 또는 비밀번호를 확인해 주세요.'
      : '로그인 요청을 처리하지 못했습니다.');
    error.status = response.status;
    throw error;
  }
  resetCsrf();
  await getCsrf();
}

export async function logout() {
  const csrf = await getCsrf();
  const response = await fetch(API_ROOT + '/logout', {
    method: 'POST',
    headers: { [csrf.headerName]: csrf.token },
    credentials: 'same-origin',
  });
  if (!response.ok) {
    const error = new Error('로그아웃 요청을 처리하지 못했습니다.');
    error.status = response.status;
    throw error;
  }
  resetCsrf();
}
