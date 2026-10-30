import { useEffect, useRef, useState } from 'react';
import { ArrowRight, LockKeyhole, Mail, X } from 'lucide-react';
import { apiRequest, login } from './apiClient.js';

export default function AuthDialog({ onClose, onAuthenticated }) {
  const [mode, setMode] = useState('login');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const emailRef = useRef(null);

  useEffect(() => {
    emailRef.current?.focus();
    function onKeyDown(event) {
      if (event.key === 'Escape') onClose();
    }
    document.addEventListener('keydown', onKeyDown);
    return () => document.removeEventListener('keydown', onKeyDown);
  }, [onClose]);

  async function submit(event) {
    event.preventDefault();
    if (busy) return;
    setBusy(true);
    setError('');
    setNotice('');
    try {
      if (mode === 'register') {
        await apiRequest('/accounts', {
          method: 'POST',
          body: { email: email.trim(), password },
        });
        setMode('login');
        setNotice('회원가입이 완료됐습니다. 같은 정보로 로그인해 주세요.');
      } else {
        const account = await login(email.trim(), password);
        onAuthenticated(account);
      }
    } catch (caught) {
      setError(caught.message || '요청에 실패했습니다.');
    } finally {
      setBusy(false);
    }
  }

  function switchMode(next) {
    if (busy) return;
    setMode(next);
    setError('');
    setNotice('');
  }

  return (
    <div className="modal-backdrop" onMouseDown={(event) => {
      if (event.target === event.currentTarget) onClose();
    }}>
      <section className="modal auth-dialog" role="dialog" aria-modal="true" aria-labelledby="auth-title">
        <button className="icon-button modal-close" onClick={onClose} aria-label="닫기"><X size={20} /></button>
        <div className="modal-symbol"><LockKeyhole size={24} /></div>
        <h2 id="auth-title">{mode === 'login' ? 'LabGear에 로그인' : '새 계정 만들기'}</h2>
        <p className="muted">{mode === 'login' ? '로그인하고 필요한 기자재를 예약하세요.' : '이메일과 비밀번호만으로 시작할 수 있어요.'}</p>
        <div className="auth-switch" role="group" aria-label="인증 방식">
          <button type="button" className={mode === 'login' ? 'active' : ''} onClick={() => switchMode('login')}>로그인</button>
          <button type="button" className={mode === 'register' ? 'active' : ''} onClick={() => switchMode('register')}>회원가입</button>
        </div>
        <form className="stack-form" onSubmit={submit}>
          <label htmlFor="auth-email">이메일</label>
          <div className="input-icon"><Mail size={17} /><input id="auth-email" ref={emailRef} type="email" autoComplete="username" value={email} onChange={(event) => setEmail(event.target.value)} placeholder="name@example.com" required /></div>
          <label htmlFor="auth-password">비밀번호</label>
          <div className="input-icon"><LockKeyhole size={17} /><input id="auth-password" type="password" minLength={8} autoComplete={mode === 'login' ? 'current-password' : 'new-password'} value={password} onChange={(event) => setPassword(event.target.value)} placeholder="8자 이상 입력" required /></div>
          {notice && <p className="inline-notice success" role="status">{notice}</p>}
          {error && <p className="inline-notice error" role="alert">{error}</p>}
          <button className="button primary full" type="submit" disabled={busy}>{busy ? '처리 중...' : mode === 'login' ? '로그인' : '회원가입'}<ArrowRight size={17}/></button>
        </form>
        <p className="form-tip">기자재 목록은 로그인 없이 살펴볼 수 있어요.</p>
      </section>
    </div>
  );
}
