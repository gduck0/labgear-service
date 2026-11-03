import { useEffect, useState } from 'react';
import { CalendarDays, ClipboardList, Package, RefreshCw } from 'lucide-react';
import { apiRequest } from './apiClient.js';
import { formatDate, getReservationDisplay } from './reservationStatus.js';

export default function AdminReservations({ equipment }) {
  const [reservations, setReservations] = useState([]);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState('');
  const [notice, setNotice] = useState(null);
  const [revision, setRevision] = useState(0);
  const [pending, setPending] = useState(null);
  const [busyId, setBusyId] = useState(null);

  useEffect(() => {
    const controller = new AbortController();
    setLoading(true);
    setLoadError('');

    apiRequest('/reservations', { signal: controller.signal })
      .then((data) => {
        if (!Array.isArray(data)) throw new Error('전체 예약 목록 응답 형식이 올바르지 않습니다.');
        if (!controller.signal.aborted) setReservations(data);
      })
      .catch((error) => {
        if (!controller.signal.aborted) setLoadError(error.message);
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false);
      });

    return () => controller.abort();
  }, [revision]);

  async function updateReservation(id, action) {
    if (busyId !== null) return;
    setBusyId(id);
    setNotice(null);

    try {
      await apiRequest(`/reservations/${id}/${action}`, { method: 'PATCH' });
      setNotice({
        type: 'success',
        text: action === 'rent' ? '대여 처리가 완료됐어요.' : '반납 처리가 완료됐어요.',
      });
    } catch (error) {
      setNotice({ type: 'error', text: error.message || '상태 변경에 실패했습니다.' });
    } finally {
      setPending(null);
      setBusyId(null);
      // 다른 관리자가 먼저 처리했을 수도 있으므로 성공·실패 모두 최신 상태를 조회한다.
      setRevision((value) => value + 1);
    }
  }

  return (
    <section className="page-section admin-reservations-section" aria-labelledby="admin-reservations-title">
      <div className="section-top">
        <div>
          <p className="eyebrow">RESERVATION MANAGEMENT</p>
          <h2 id="admin-reservations-title">예약·대여 관리</h2>
          <p className="muted">예약자의 기자재 대여와 반납을 확인하고 처리하세요.</p>
        </div>
        <button
          type="button"
          className="button subtle"
          onClick={() => setRevision((value) => value + 1)}
          disabled={loading || busyId !== null}
        >
          <RefreshCw size={17}/> 새로고침
        </button>
      </div>

      {notice && (
        <p className={`inline-notice ${notice.type}`} role={notice.type === 'error' ? 'alert' : 'status'}>
          {notice.text}
        </p>
      )}
      {loading ? (
        <div className="state-panel compact"><div className="spinner"/><p>예약 목록을 불러오고 있어요.</p></div>
      ) : loadError ? (
        <div className="state-panel compact">
          <p className="state-heading">예약 목록을 불러오지 못했어요.</p>
          <p className="muted">{loadError}</p>
          <button className="button outline" onClick={() => setRevision((value) => value + 1)}>
            다시 시도
          </button>
        </div>
      ) : reservations.length === 0 ? (
        <div className="state-panel compact">
          <span className="empty-icon"><ClipboardList size={28}/></span>
          <p className="state-heading">등록된 예약이 없어요.</p>
        </div>
      ) : (
        <>
          <p className="result-count">전체 <strong>{reservations.length}</strong>건 · 반납 완료 후에도 원래 예약 종료일까지는 기간이 유지돼요.</p>
          <div className="booking-list">
            {reservations.map((reservation) => {
              const display = getReservationDisplay(reservation);
              const action = reservation.status === 'ACTIVE'
                ? 'rent'
                : reservation.status === 'RENTED' ? 'return' : null;
              const actionLabel = action === 'rent' ? '대여 처리' : '반납 처리';
              const confirming = pending?.id === reservation.id && pending.action === action;
              const equipmentName = equipment.find((item) => item.id === reservation.equipmentId)?.name;

              return (
                <article className="booking-row admin-reservation-row" key={reservation.id}>
                  <div className="booking-device-icon"><Package size={23}/></div>
                  <div className="booking-row-main">
                    <div className="booking-device-heading">
                      <strong>{equipmentName || `기자재 #${reservation.equipmentId}`}</strong>
                      <span className={`reservation-status ${display.className}`}>{display.label}</span>
                    </div>
                    <span className="muted">예약 #{reservation.id} · 기자재 #{reservation.equipmentId}</span>
                    <span className="admin-reservation-owner">예약자: {reservation.accountEmail || '이메일 정보 없음'}</span>
                    <div className="booking-period">
                      <CalendarDays size={16}/>
                      <span>{formatDate(reservation.startDate)} – {formatDate(reservation.endDate)}</span>
                    </div>
                  </div>
                  <div className="booking-row-actions admin-reservation-actions">
                    {action && !confirming && (
                      <button
                        type="button"
                        className="button outline"
                        disabled={busyId !== null}
                        onClick={() => setPending({ id: reservation.id, action })}
                      >
                        {actionLabel}
                      </button>
                    )}
                    {action && confirming && (
                      <div className="booking-cancel-confirm">
                        <p>{actionLabel}할까요?</p>
                        <div className="booking-confirm-buttons">
                          <button
                            type="button"
                            className="button primary"
                            disabled={busyId !== null}
                            onClick={() => updateReservation(reservation.id, action)}
                          >
                            {busyId === reservation.id ? '처리 중...' : '확인'}
                          </button>
                          <button
                            type="button"
                            className="button subtle"
                            disabled={busyId !== null}
                            onClick={() => setPending(null)}
                          >
                            돌아가기
                          </button>
                        </div>
                      </div>
                    )}
                  </div>
                </article>
              );
            })}
          </div>
        </>
      )}
    </section>
  );
}
