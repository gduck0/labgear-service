import { useCallback, useEffect, useMemo, useState } from 'react';
import {
  ArrowRight, BookOpenCheck, CalendarDays, ChevronDown, LogIn, LogOut,
  Package, PackageSearch, RefreshCw, Search, SlidersHorizontal, X,
} from 'lucide-react';
import { apiRequest, logout } from './apiClient.js';
import AuthDialog from './AuthDialog.jsx';
import BookingDialog from './BookingDialog.jsx';
import MyReservations from './MyReservations.jsx';
import AdminPanel from './AdminPanel.jsx';

function EquipmentCard({ item, onReserve }) {
  return (
    <article className="equipment-card">
      <div className="equipment-art"><Package size={37} strokeWidth={1.4}/><span className="equipment-mark">LABGEAR</span></div>
      <div className="equipment-card-content"><span className="equipment-id">EQUIPMENT #{item.id}</span><h3>{item.name}</h3><button className="reserve-link" onClick={() => onReserve(item)} aria-label={`${item.name} 예약하기`}>예약하기 <ArrowRight size={17}/></button></div>
    </article>
  );
}

export default function App() {
  const [page, setPage] = useState('catalog');
  const [account, setAccount] = useState(null);
  const [accountLoading, setAccountLoading] = useState(true);
  const [authOpen, setAuthOpen] = useState(false);
  const [pendingEquipment, setPendingEquipment] = useState(null);
  const [pendingPage, setPendingPage] = useState(null);
  const [bookingEquipment, setBookingEquipment] = useState(null);
  const [equipment, setEquipment] = useState([]);
  const [equipmentLoading, setEquipmentLoading] = useState(true);
  const [equipmentError, setEquipmentError] = useState('');
  const [equipmentRevision, setEquipmentRevision] = useState(0);
  const [bookingRevision, setBookingRevision] = useState(0);
  const [search, setSearch] = useState('');
  const [sort, setSort] = useState('id');
  const [toast, setToast] = useState('');

  useEffect(() => {
    let active = true;
    apiRequest('/accounts/me')
      .then((value) => { if (active) setAccount(value); })
      .catch(() => { if (active) setAccount(null); })
      .finally(() => { if (active) setAccountLoading(false); });
    return () => { active = false; };
  }, []);

  useEffect(() => {
    const controller = new AbortController();
    setEquipmentLoading(true);
    setEquipmentError('');
    apiRequest('/equipment', { signal: controller.signal })
      .then((value) => {
        if (!Array.isArray(value)) throw new Error('기자재 목록 응답 형식이 올바르지 않습니다.');
        setEquipment(value);
      })
      .catch((error) => { if (!controller.signal.aborted) setEquipmentError(error.message); })
      .finally(() => { if (!controller.signal.aborted) setEquipmentLoading(false); });
    return () => controller.abort();
  }, [equipmentRevision]);

  const closeAuth = useCallback(() => {
    setAuthOpen(false);
    setPendingEquipment(null);
    setPendingPage(null);
  }, []);

  const visibleEquipment = useMemo(() => {
    const term = search.trim().toLocaleLowerCase('ko-KR');
    return equipment
      .filter((item) => !term || String(item.name).toLocaleLowerCase('ko-KR').includes(term) || String(item.id).includes(term))
      .sort((a, b) => sort === 'name'
        ? String(a.name).localeCompare(String(b.name), 'ko-KR')
        : a.id - b.id);
  }, [equipment, search, sort]);

  function choosePage(next) {
    setToast('');
    if (next === 'catalog') { setPage(next); return; }
    if (accountLoading) return;
    if (!account) {
      setPendingPage(next);
      setAuthOpen(true);
      return;
    }
    if (next === 'admin' && account.role !== 'ADMIN') return;
    setPage(next);
  }

  function startReservation(item) {
    setToast('');
    if (!account) {
      setPendingEquipment(item);
      setAuthOpen(true);
      return;
    }
    setBookingEquipment(item);
  }

  function onAuthenticated(nextAccount) {
    setAccount(nextAccount);
    setAuthOpen(false);
    if (pendingEquipment) setBookingEquipment(pendingEquipment);
    if (pendingPage && (pendingPage !== 'admin' || nextAccount.role === 'ADMIN')) setPage(pendingPage);
    setPendingEquipment(null);
    setPendingPage(null);
  }

  async function signOut() {
    try {
      await logout();
      setAccount(null);
      setPage('catalog');
      setBookingEquipment(null);
      setToast('로그아웃했어요.');
    } catch (error) {
      setToast(error.message);
    }
  }

  function bookingCreated(reservation) {
    setBookingEquipment(null);
    setBookingRevision((value) => value + 1);
    setPage('reservations');
    setToast(`예약이 접수됐어요. 예약 번호는 #${reservation.id}입니다.`);
  }

  return (
    <div className="app-shell">
      <header className="site-header"><div className="container header-inner">
        <button className="brand" type="button" onClick={() => choosePage('catalog')} aria-label="LabGear 홈"><span className="brand-symbol"><Package size={21}/></span><span>LabGear<span className="brand-period">.</span></span></button>
        <nav className="main-nav" aria-label="주 메뉴">
          <button className={page === 'catalog' ? 'active' : ''} onClick={() => choosePage('catalog')}>기자재 찾기</button>
          <button className={page === 'reservations' ? 'active' : ''} onClick={() => choosePage('reservations')}>내 예약</button>
          {account?.role === 'ADMIN' && <button className={page === 'admin' ? 'active' : ''} onClick={() => choosePage('admin')}>관리자</button>}
        </nav>
        <div className="account-actions">
          {account ? <div className="signed-in"><span className="account-initial">{account.email?.slice(0, 1).toUpperCase()}</span><span className="account-email" title={account.email}>{account.email}</span><button className="icon-button" onClick={signOut} aria-label="로그아웃" title="로그아웃"><LogOut size={18}/></button></div>
            : <button className="button login-button" onClick={() => setAuthOpen(true)} disabled={accountLoading}><LogIn size={17}/>{accountLoading ? '확인 중' : '로그인'}</button>}
        </div>
      </div></header>

      <main>
        {page === 'catalog' && <>
          <section className="hero"><div className="container hero-grid">
            <div className="hero-copy"><span className="eyebrow"><span className="eyebrow-dot"/> 학과 기자재 예약 서비스</span><h1>필요한 기자재,<br/><span>찾고 예약까지.</span></h1><p>실습과 프로젝트에 필요한 기자재를 확인하고<br className="desktop-only"/> 원하는 날짜에 간편하게 예약하세요.</p><a className="button primary hero-action" href="#catalog">기자재 살펴보기 <ArrowRight size={18}/></a></div>
            <div className="hero-steps" aria-label="이용 방법"><div className="hero-steps-head"><span className="step-head-icon"><BookOpenCheck size={21}/></span><div><strong>이용 방법</strong><small>세 단계로 간단하게</small></div></div><div className="step"><span>01</span><div><strong>기자재 찾기</strong><small>목록에서 필요한 기자재 선택</small></div></div><div className="step"><span>02</span><div><strong>날짜 선택하기</strong><small>사용할 시작일과 종료일 입력</small></div></div><div className="step"><span>03</span><div><strong>예약 확인하기</strong><small>내 예약에서 접수 내역 확인</small></div></div></div>
          </div></section>
          <section className="container catalog page-section" id="catalog" aria-labelledby="catalog-title"><div className="section-top"><div><p className="eyebrow">EQUIPMENT CATALOG</p><h2 id="catalog-title">기자재 찾기</h2><p className="muted">목록에서 원하는 기자재를 골라 예약해 보세요.</p></div><button className="button subtle" onClick={() => setEquipmentRevision((value) => value + 1)} disabled={equipmentLoading}><RefreshCw size={17}/> 새로고침</button></div>
            <div className="catalog-toolbar"><label className="search-box"><Search size={20}/><input type="search" placeholder="기자재 이름 또는 번호로 검색" value={search} onChange={(event) => setSearch(event.target.value)} aria-label="기자재 검색"/>{search && <button className="icon-button" aria-label="검색 초기화" onClick={() => setSearch('')}><X size={17}/></button>}</label><label className="sort-box"><SlidersHorizontal size={17}/><select aria-label="정렬 방식" value={sort} onChange={(event) => setSort(event.target.value)}><option value="id">번호순</option><option value="name">이름순</option></select><ChevronDown size={15}/></label></div>
            {equipmentLoading ? <div className="state-panel compact"><div className="spinner"/><p>기자재 목록을 불러오고 있어요.</p></div>
              : equipmentError ? <div className="state-panel"><p className="state-heading">기자재를 불러오지 못했어요.</p><p className="muted">{equipmentError}</p><button className="button outline" onClick={() => setEquipmentRevision((value) => value + 1)}>다시 시도</button></div>
                : visibleEquipment.length === 0 ? <div className="state-panel"><span className="empty-icon"><PackageSearch size={31}/></span><p className="state-heading">{search ? '검색 결과가 없어요.' : '등록된 기자재가 없어요.'}</p><p className="muted">{search ? '다른 이름이나 번호로 검색해 보세요.' : '관리자가 기자재를 등록하면 이곳에 나타나요.'}</p></div>
                  : <><p className="result-count"><strong>{visibleEquipment.length}</strong>개의 기자재 {search && <span>검색 결과</span>}</p><div className="equipment-grid">{visibleEquipment.map((item) => <EquipmentCard key={item.id} item={item} onReserve={startReservation}/>)}</div></>}
          </section>
        </>}
        {page === 'reservations' && account && <div className="container"><MyReservations revision={bookingRevision} equipment={equipment} onBrowse={() => choosePage('catalog')}/></div>}
        {page === 'admin' && account?.role === 'ADMIN' && <div className="container"><AdminPanel equipment={equipment} onEquipmentChanged={() => setEquipmentRevision((value) => value + 1)}/></div>}
      </main>
      <footer className="site-footer"><div className="container footer-inner"><span>LabGear · 학과 기자재 예약 서비스</span><span>Java / Spring Boot / React</span></div></footer>

      {toast && <div className="toast" role="status"><button onClick={() => setToast('')} aria-label="알림 닫기"><X size={16}/></button>{toast}</div>}
      {authOpen && <AuthDialog onClose={closeAuth} onAuthenticated={onAuthenticated}/>}
      {bookingEquipment && <BookingDialog key={bookingEquipment.id} equipment={bookingEquipment} onClose={() => setBookingEquipment(null)} onCreated={bookingCreated}/>}
    </div>
  );
}
