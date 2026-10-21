import { useEffect, useState } from 'react';
import { apiRequest, login, logout } from './apiClient.js';
import './AuthPanel.css';

export default function AuthPanel({ onAuthChange }) {
  const [account, setAccount] = useState(null);
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [busy, setBusy] = useState(false);
  const [checking, setChecking] = useState(true);
  const [message, setMessage] = useState('');

  useEffect(() => {
    let mounted = true;
    apiRequest('/accounts/me')
      .then((data) => {
        if (!mounted) return;
        setAccount(data);
        onAuthChange(true);
      })
      .catch(() => {
        if (!mounted) return;
        setAccount(null);
        onAuthChange(false);
      })
      .finally(() => {
        if (mounted) setChecking(false);
      });
    return () => { mounted = false; };
  }, [onAuthChange]);

  async function submitLogin(event) {
    event.preventDefault();
    setBusy(true);
    setMessage('');
    try {
      await login(email.trim(), password);
      const data = await apiRequest('/accounts/me');
      setAccount(data);
      onAuthChange(true);
      setPassword('');
    } catch (error) {
      setMessage(error.message || '로그인에 실패했습니다.');
    } finally {
      setBusy(false);
    }
  }

  async function submitRegister() {
    setBusy(true);
    setMessage('');
    try {
      await apiRequest('/accounts', { method: 'POST', body: { email: email.trim(), password } });
      setMessage('회원가입이 완료됐습니다. 로그인해 주세요.');
    } catch (error) {
      setMessage(error.status === 409 ? '이미 가입된 이메일입니다.'
        : error.status === 400 ? '이메일 형식과 8자 이상 비밀번호를 확인해 주세요.'
          : error.message || '회원가입에 실패했습니다.');
    } finally {
      setBusy(false);
    }
  }

  async function submitLogout() {
    setBusy(true);
    setMessage('');
    try {
      await logout();
      setAccount(null);
      onAuthChange(false);
      setEmail('');
      setPassword('');
    } catch (error) {
      setMessage(error.message || '로그아웃에 실패했습니다.');
    } finally {
      setBusy(false);
    }
  }

  return (
    <section className="auth-panel" id="account" aria-labelledby="account-title">
      <div className="auth-heading">
        <span className="eyebrow">ACCOUNT</span>
        <h2 id="account-title">계정 및 로그인<span className="title-dot">.</span></h2>
      </div>
      {checking ? (
        <p>로그인 상태를 확인하고 있습니다.</p>
      ) : account ? (
        <div className="auth-row">
          <p><strong>{account.email}</strong> 계정으로 로그인했습니다.</p>
          <button type="button" disabled={busy} onClick={submitLogout}>로그아웃</button>
        </div>
      ) : (
        <form className="auth-form" onSubmit={submitLogin}>
          <label htmlFor="auth-email">이메일</label>
          <input id="auth-email" type="email" autoComplete="username" required
            value={email} onChange={(event) => setEmail(event.target.value)} />
          <label htmlFor="auth-password">비밀번호</label>
          <input id="auth-password" type="password" autoComplete="current-password" minLength={8} required
            value={password} onChange={(event) => setPassword(event.target.value)} />
          <div className="auth-actions">
            <button type="submit" disabled={busy}>{busy ? '처리 중...' : '로그인'}</button>
            <button type="button" disabled={busy || !email.trim() || password.length < 8}
              onClick={submitRegister}>회원가입</button>
          </div>
          <p>회원가입 시 이메일과 8자 이상의 비밀번호를 입력한 뒤 회원가입을 누르세요.</p>
        </form>
      )}
      {message && <p role="status" className="auth-message">{message}</p>}
    </section>
  );
}
