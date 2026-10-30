const API_ROOT = (import.meta.env.VITE_API_BASE_URL || '/api').replace(/\/$/, '');
let csrfPromise = null;

export function resetCsrf() {
  csrfPromise = null;
}

export async function getCsrf() {
  if (!csrfPromise) {
    csrfPromise = fetch(`${API_ROOT}/csrf`, { credentials: 'same-origin' })
      .then(async (response) => {
        if (!response.ok) throw new Error('보안 토큰을 가져오지 못했습니다.');
        const result = await response.json();
        if (!result.token || !result.headerName) throw new Error('보안 토큰 응답이 올바르지 않습니다.');
        return result;
      })
      .catch((error) => {
        resetCsrf();
        throw error;
      });
  }
  return csrfPromise;
}

export async function apiRequest(path, { method = 'GET', body, signal } = {}) {
  const normalizedMethod = method.toUpperCase();
  const headers = { Accept: 'application/json' };
  if (body !== undefined) headers['Content-Type'] = 'application/json';
  if (!['GET', 'HEAD', 'OPTIONS'].includes(normalizedMethod)) {
    const csrf = await getCsrf();
    headers[csrf.headerName] = csrf.token;
  }

  let response;
  try {
    response = await fetch(`${API_ROOT}${path}`, {
      method: normalizedMethod,
      headers,
      signal,
      credentials: 'same-origin',
      ...(body === undefined ? {} : { body: JSON.stringify(body) }),
    });
  } catch (error) {
    if (error?.name === 'AbortError') throw error;
    throw new Error('서버에 연결할 수 없습니다. 백엔드 실행 상태를 확인해 주세요.');
  }

  const text = await response.text();
  let result = null;
  if (text) {
    try {
      result = JSON.parse(text);
    } catch {
      if (response.ok) throw new Error('서버에서 예상하지 않은 응답을 받았습니다. 로그인 상태와 서버 설정을 확인해 주세요.');
    }
  }

  if (!response.ok) {
    const error = new Error(
      typeof result?.message === 'string' && result.message.trim()
        ? result.message
        : response.status === 403 ? '이 작업을 수행할 권한이 없습니다.'
          : response.status === 404 ? '요청한 정보를 찾을 수 없습니다.'
            : response.status === 409 ? '기존 예약이나 정보와 충돌합니다.'
              : response.status === 400 ? '입력값을 확인해 주세요.'
                : `요청에 실패했습니다. (HTTP ${response.status})`
    );
    error.status = response.status;
    throw error;
  }
  return result;
}

export async function login(email, password) {
  const csrf = await getCsrf();
  let response;
  try {
    response = await fetch(`${API_ROOT}/login`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded',
        [csrf.headerName]: csrf.token,
      },
      body: new URLSearchParams({ email, password }),
      credentials: 'same-origin',
    });
  } catch {
    throw new Error('로그인 서버에 연결할 수 없습니다.');
  } finally {
    resetCsrf();
  }
  if (!response.ok) throw new Error('로그인 요청에 실패했습니다.');

  // 기본 Spring Security 폼 로그인은 성공과 실패 모두 리다이렉트를 사용하므로
  // 응답 코드만 믿지 않고 실제 인증된 계정을 다시 조회한다.
  try {
    const account = await apiRequest('/accounts/me');
    if (!account?.email) throw new Error('로그인 상태를 확인할 수 없습니다.');
    return account;
  } catch {
    throw new Error('이메일 또는 비밀번호를 확인해 주세요.');
  }
}

export async function logout() {
  const csrf = await getCsrf();
  let response;
  try {
    response = await fetch(`${API_ROOT}/logout`, {
      method: 'POST',
      headers: { [csrf.headerName]: csrf.token },
      credentials: 'same-origin',
    });
  } catch {
    throw new Error('로그아웃 요청에 실패했습니다.');
  } finally {
    resetCsrf();
  }
  if (!response.ok) throw new Error('로그아웃 요청에 실패했습니다.');
}
