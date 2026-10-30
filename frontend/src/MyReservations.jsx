import { useEffect, useState } from 'react';
import { CalendarDays, ClipboardList, Package, RefreshCw } from 'lucide-react';
import { apiRequest } from './apiClient.js';

function formatDate(value) {
  if (!value) return '—';
  const [year, month, day] = value.split('-');
  return `${year}.${month}.${day}`;
}

export default function MyReservations({ revision, onBrowse, equipment }) {
  const [reservations, setReservations] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [reload, setReload] = useState(0);

  useEffect(() => {
    const controller = new AbortController();
    setLoading(true);
    setError('');
    apiRequest('/reservations/my', { signal: controller.signal })
      .then((data) => {
        if (!Array.isArray(data)) throw new Error('예약 목록 응답을 확인할 수 없습니다.');
        setReservations(data);
      })
      .catch((caught) => { if (!controller.signal.aborted) setError(caught.message); })
      .finally(() => { if (!controller.signal.aborted) setLoading(false); });
    return () => controller.abort();
  }, [revision, reload]);

  return (
    <section className="page-section" aria-labelledby="my-reservations-title">
      <div className="section-top">
        <div><p className="eyebrow">MY RESERVATIONS</p><h1 id="my-reservations-title">내 예약</h1><p className="muted">로그인한 계정으로 예약한 기자재를 확인하세요.</p></div>
        <button className="button subtle" onClick={() => setReload((value) => value + 1)} disabled={loading}><RefreshCw size={17}/> 새로고침</button>
      </div>
      {loading ? (
        <div className="state-panel"><div className="spinner"/><p>예약을 불러오고 있어요.</p></div>
      ) : error ? (
        <div className="state-panel"><p className="state-heading">예약을 불러오지 못했어요.</p><p className="muted">{error}</p><button className="button outline" onClick={() => setReload((value) => value + 1)}>다시 시도</button></div>
      ) : reservations.length === 0 ? (
        <div className="state-panel"><span className="empty-icon"><ClipboardList size={30}/></span><p className="state-heading">아직 예약한 기자재가 없어요.</p><p className="muted">필요한 기자재를 찾아 첫 예약을 시작해 보세요.</p><button className="button primary" onClick={onBrowse}>기자재 둘러보기</button></div>
      ) : (
        <>
          <p className="result-count">총 <strong>{reservations.length}</strong>건의 예약</p>
          <div className="booking-list">
            {reservations.map((reservation) => (
              <article className="booking-row" key={reservation.id}>
                <div className="booking-device-icon"><Package size={23}/></div>
                <div className="booking-row-main"><strong>{equipment.find((item) => item.id === reservation.equipmentId)?.name || `기자재 #${reservation.equipmentId}`}</strong><span className="muted">기자재 #{reservation.equipmentId} · 예약 #{reservation.id}</span></div>
                <div className="booking-period"><CalendarDays size={16}/><span>{formatDate(reservation.startDate)} – {formatDate(reservation.endDate)}</span></div>
              </article>
            ))}
          </div>
          <p className="list-footnote">현재는 예약 조회만 지원해요. 예약 취소 기능은 아직 제공되지 않습니다.</p>
        </>
      )}
    </section>
  );
}
