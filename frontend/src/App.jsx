import { useEffect, useMemo, useState } from 'react';
import OperationsPanel from './OperationsPanel.jsx';
import {
  ArrowDownAZ,
  ArrowUpRight,
  Box,
  Boxes,
  ChevronRight,
  CircleAlert,
  Clock3,
  Hash,
  Layers3,
  LoaderCircle,
  Package,
  RefreshCw,
  Search,
  SlidersHorizontal,
  Wifi,
  WifiOff,
  X,
} from 'lucide-react';

const API_ROOT = (import.meta.env.VITE_API_BASE_URL || '/api').replace(/\/$/, '');
async function readApi(path, signal) {
  const response = await fetch(API_ROOT + path, {
    headers: { Accept: 'application/json' },
    signal,
  });

  if (!response.ok) {
    const error = new Error('HTTP ' + response.status);
    error.status = response.status;
    throw error;
  }

  const text = await response.text();
  // The current Spring API returns Optional.empty() without explicit 404 handling.
  if (!text || text.trim() === 'null') return null;

  try {
    return JSON.parse(text);
  } catch {
    throw new Error('서버 응답이 올바른 JSON이 아닙니다.');
  }
}

function explainError(error) {
  if (error?.status === 404) return '해당 기자재를 찾을 수 없습니다.';
  if (error?.status >= 500) return '일시적인 서버 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.';
  if (error?.status) return '요청을 처리하지 못했습니다. (HTTP ' + error.status + ')';
  if (error instanceof TypeError) return '서비스에 연결할 수 없습니다. 잠시 후 다시 시도해 주세요.';
  return error?.message || '알 수 없는 오류가 발생했습니다.';
}

function initialEquipmentId() {
  const value = new URLSearchParams(window.location.search).get('equipment');
  if (value === null || !/^\d+$/.test(value)) return null;
  const id = Number(value);
  return Number.isSafeInteger(id) ? id : null;
}

function DeviceArtwork({ variant = 0, large = false }) {
  return (
    <div className={'device-art art-' + (variant % 4) + (large ? ' device-art-large' : '')} aria-hidden="true">
      <div className="device-art-halo" />
      <Box size={large ? 60 : 38} strokeWidth={1.35} />
      <div className="device-art-shine" />
    </div>
  );
}

function InfoEmpty({ searchTerm }) {
  return (
    <div className="list-empty">
      <div className="empty-symbol"><Search size={27} strokeWidth={1.5} /></div>
      <h3>{searchTerm ? '검색 결과가 없어요' : '등록된 기자재가 없어요'}</h3>
      <p>{searchTerm ? '다른 이름이나 기자재 번호로 다시 검색해 보세요.' : '기자재가 등록되면 이곳에 표시됩니다.'}</p>
    </div>
  );
}

export default function App() {
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [listError, setListError] = useState('');
  const [detail, setDetail] = useState(null);
  const [detailLoading, setDetailLoading] = useState(false);
  const [detailError, setDetailError] = useState('');
  const [selectedId, setSelectedId] = useState(initialEquipmentId);
  const [search, setSearch] = useState('');
  const [sort, setSort] = useState('id');
  const [refreshKey, setRefreshKey] = useState(0);
  const [lastUpdated, setLastUpdated] = useState(null);

  useEffect(() => {
    const controller = new AbortController();
    setLoading(true);
    setListError('');

    readApi('/equipment', controller.signal)
      .then((data) => {
        if (!Array.isArray(data)) throw new Error('목록 응답 형식이 예상과 다릅니다.');
        setItems(data);
        setLastUpdated(Date.now());
        setSelectedId((previous) => previous ?? (data[0]?.id ?? null));
      })
      .catch((error) => {
        if (!controller.signal.aborted) {
          setItems([]);
          setListError(explainError(error));
        }
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false);
      });

    return () => controller.abort();
  }, [refreshKey]);

  useEffect(() => {
    if (selectedId === null) {
      setDetail(null);
      setDetailError('');
      setDetailLoading(false);
      return;
    }

    const controller = new AbortController();
    setDetail(null);
    setDetailError('');
    setDetailLoading(true);

    readApi('/equipment/' + encodeURIComponent(String(selectedId)), controller.signal)
      .then((data) => {
        if (!data || typeof data !== 'object') {
          setDetailError('해당 ID로 조회된 기자재가 없습니다.');
        } else {
          setDetail(data);
        }
      })
      .catch((error) => {
        if (!controller.signal.aborted) setDetailError(explainError(error));
      })
      .finally(() => {
        if (!controller.signal.aborted) setDetailLoading(false);
      });

    return () => controller.abort();
  }, [selectedId, refreshKey]);

  const filteredItems = useMemo(() => {
    const term = search.trim().toLocaleLowerCase('ko-KR');
    return items
      .filter((item) =>
        !term || String(item.name ?? '').toLocaleLowerCase('ko-KR').includes(term)
          || String(item.id).includes(term)
      )
      .sort((a, b) =>
        sort === 'name'
          ? String(a.name).localeCompare(String(b.name), 'ko-KR')
          : Number(a.id) - Number(b.id)
      );
  }, [items, search, sort]);

  function chooseEquipment(id) {
    setSelectedId(id);
    const next = new URL(window.location.href);
    next.searchParams.set('equipment', String(id));
    window.history.replaceState(null, '', next);
  }

  function refresh() {
    setRefreshKey((key) => key + 1);
  }

  function handleEquipmentChanged(nextId) {
    setSelectedId(nextId);
    setDetail(null);
    setDetailError('');
    const next = new URL(window.location.href);
    if (nextId === null) next.searchParams.delete('equipment');
    else next.searchParams.set('equipment', String(nextId));
    window.history.replaceState(null, '', next);
    refresh();
  }

  const connection = loading ? 'connecting' : listError ? 'error' : 'connected';
  const countLabel = loading ? '—' : listError ? '—' : items.length.toLocaleString('ko-KR');

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <a className="brand" href="#top" aria-label="LabGear 처음으로">
          <div className="brand-symbol"><Boxes size={25} strokeWidth={1.85}/></div>
          <div className="brand-copy"><strong>labgear<span className="brand-dot">.</span></strong><small>EQUIPMENT HUB</small></div>
        </a>

        <div className="sidebar-label">MENU</div>
        <nav className="side-navigation" aria-label="주 메뉴">
          <a className="side-link active" href="#catalog" aria-current="page">
            <Package size={18}/><span>기자재 탐색</span><ChevronRight size={16}/>
          </a>
          <a className="side-link" href="#manage">
            <Layers3 size={18}/><span>기자재 관리</span><ChevronRight size={16}/>
          </a>
          <a className="side-link" href="#reservations">
            <Clock3 size={18}/><span>예약 접수·조회</span><ChevronRight size={16}/>
          </a>
        </nav>

        <div className="sidebar-bottom">
          <p>LabGear · 기자재 관리 시스템</p>
        </div>
      </aside>

      <main className="main-content" id="top">
        <header className="topbar">
          <div className="breadcrumb"><span>LabGear</span><ChevronRight size={14}/><strong>기자재 탐색</strong></div>
          <div className={'connection-pill ' + connection} role="status">
            <span className="status-dot" />
            {connection === 'connecting' ? '연결 중' : connection === 'connected' ? '연결됨' : '연결 오류'}
          </div>
        </header>

        <div className="page-content">
          <section className="hero" aria-labelledby="hero-title">
            <div className="hero-glow" />
            <div className="hero-grid" />
            <div className="hero-text">
              <div className="hero-overline"><span className="mini-spark" /> LABGEAR · EQUIPMENT EXPLORER</div>
              <h1 id="hero-title">등록된 기자재를<br/><span>한눈에.</span></h1>
              <p>학과 실습과 프로젝트에 필요한 기자재를 찾고,<br className="desktop-break"/> 등록과 예약까지 한곳에서 관리하세요.</p>
              <a className="hero-action" href="#catalog">기자재 살펴보기 <ArrowUpRight size={17}/></a>
            </div>
            <div className="hero-visual" aria-hidden="true">
              <div className="orb orb-one"/>
              <div className="orb orb-two"/>
              <div className="hero-device"><Layers3 size={76} strokeWidth={1.1}/></div>
              <span className="hero-tag tag-one">01 / EXPLORE</span>
              <span className="hero-tag tag-two">LAB · 2026</span>
            </div>
          </section>

          <section className="summary-row" aria-label="기자재 요약">
            <div className="summary-card"><span className="summary-icon tone-blue"><Boxes size={21}/></span><div><small>등록된 기자재</small><strong>{countLabel}<span className="summary-unit">개</span></strong></div></div>
            <div className="summary-card"><span className="summary-icon tone-orange"><SlidersHorizontal size={21}/></span><div><small>현재 검색 결과</small><strong>{loading || listError ? '—' : filteredItems.length.toLocaleString('ko-KR')}<span className="summary-unit">개</span></strong></div></div>
            <div className="summary-card"><span className="summary-icon tone-green">{connection === 'connected' ? <Wifi size={21}/> : <WifiOff size={21}/>}</span><div><small>서버 연결 상태</small><strong className="summary-text">{connection === 'connected' ? '정상 연결' : connection === 'connecting' ? '확인 중' : '연결 실패'}</strong></div></div>
          </section>

          <section className="catalog-section" id="catalog" aria-labelledby="catalog-title">
            <div className="section-header"><div><span className="eyebrow">EQUIPMENT CATALOG</span><h2 id="catalog-title">기자재 목록<span className="title-dot">.</span></h2><p>목록에서 기자재를 선택하면 상세 정보를 확인할 수 있어요.</p></div><button className="refresh-button" type="button" disabled={loading} onClick={refresh}><RefreshCw size={16} className={loading ? 'spin' : ''}/> 새로고침</button></div>

            <div className="catalog-layout">
              <div className="catalog-panel">
                <div className="catalog-toolbar">
                  <div className="search-wrap"><Search size={19} aria-hidden="true"/><input type="search" value={search} onChange={(event) => setSearch(event.target.value)} placeholder="이름 또는 기자재 번호 검색" aria-label="기자재 검색"/>{search && <button type="button" className="search-clear" aria-label="검색어 지우기" onClick={() => setSearch('')}><X size={15}/></button>}</div>
                  <label className="sort-wrap"><ArrowDownAZ size={17}/><select value={sort} onChange={(event) => setSort(event.target.value)} aria-label="기자재 정렬"><option value="id">번호순</option><option value="name">이름순</option></select></label>
                </div>

                {loading ? (
                  <div className="equipment-grid" aria-label="기자재 목록 불러오는 중">{[0, 1, 2, 3].map((index) => <div className="skeleton-card" key={index}><div className="skeleton skeleton-image"/><div className="skeleton skeleton-line"/><div className="skeleton skeleton-short"/></div>)}</div>
                ) : listError ? (
                  <div className="list-empty error-state"><div className="empty-symbol error"><CircleAlert size={27}/></div><h3>기자재를 불러오지 못했어요</h3><p>{listError}</p><button type="button" className="inline-retry" onClick={refresh}><RefreshCw size={15}/> 다시 시도</button></div>
                ) : filteredItems.length === 0 ? (
                  <InfoEmpty searchTerm={search}/>
                ) : (
                  <div className="equipment-grid" aria-label="기자재 목록">
                    {filteredItems.map((item, index) => (
                      <button type="button" key={item.id} className={'equipment-card ' + (selectedId === item.id ? 'selected' : '')} onClick={() => chooseEquipment(item.id)} aria-pressed={selectedId === item.id}>
                        <DeviceArtwork variant={index}/>
                        <div className="item-meta"><span className="item-kicker">EQUIPMENT</span><span className="item-id">NO. {item.id}</span></div>
                        <div className="item-bottom"><div className="item-name" title={item.name}>{item.name}</div><span className="item-action"><ArrowUpRight size={17}/></span></div>
                      </button>
                    ))}
                  </div>
                )}
                <div className="catalog-bottom"><span>기자재 목록</span>{lastUpdated && !listError && <span><Clock3 size={13}/> {new Date(lastUpdated).toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit' })} 기준</span>}</div>
              </div>

              <aside className="detail-panel" aria-label="선택 기자재 상세">
                <div className="detail-top"><div><span className="eyebrow">EQUIPMENT DETAIL</span><h3>기자재 상세</h3></div><div className="detail-corner"><ArrowUpRight size={20}/></div></div>

                {detailLoading ? (
                  <div className="detail-wait"><LoaderCircle size={32} className="spin"/><strong>상세 정보를 불러오는 중</strong><span>기자재 정보를 확인하고 있어요</span></div>
                ) : detailError ? (
                  <div className="detail-wait"><CircleAlert size={35} className="error-icon"/><strong>상세 정보를 표시할 수 없어요</strong><span>{detailError}</span><button className="inline-retry" type="button" onClick={refresh}><RefreshCw size={15}/> 다시 시도</button></div>
                ) : detail ? (
                  <>
                    <DeviceArtwork variant={0} large/>
                    <div className="detail-badge"><span className="badge-dot"/> 등록된 기자재 정보</div>
                    <h4 className="detail-name">{detail.name}</h4>
                    <p className="detail-description">학과 실습과 캡스톤 프로젝트에 활용하는 기자재 정보입니다.</p>
                    <div className="detail-divider"/>
                    <div className="detail-row"><span><Hash size={16}/> 기자재 번호</span><strong>{detail.id}</strong></div>
                    <div className="detail-row"><span><Package size={16}/> 기자재 이름</span><strong>{detail.name}</strong></div>
                    <div className="detail-divider"/>
                  </>
                ) : (
                  <div className="detail-wait"><Package size={34}/><strong>기자재를 선택해 주세요</strong><span>목록에서 기자재를 선택해 주세요.</span></div>
                )}
              </aside>
            </div>
          </section>

          <OperationsPanel
            selectedEquipment={detail && detail.id === selectedId ? detail : null}
            selectedEquipmentId={selectedId}
            onEquipmentChanged={handleEquipmentChanged}
          />

          <footer className="footer"><span>© LabGear · Academic Equipment Management</span><span>기자재 관리 시스템</span></footer>
        </div>
      </main>
    </div>
  );
}
