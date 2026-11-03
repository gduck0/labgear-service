import { useEffect, useRef, useState } from 'react';
import { ArrowRight, CalendarDays, Check, ChevronLeft, ChevronRight, Package, RefreshCw, X } from 'lucide-react';
import { apiRequest } from './apiClient.js';
import { useModalFocus } from './useModalFocus.js';

function toLocalISO(date) {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return year + '-' + month + '-' + day;
}

function formatDate(value) {
  return value ? value.replaceAll('-', '.') : '선택 전';
}

function reservedPeriodsPath(equipmentId, startDate, endDate) {
  return '/equipment/' + equipmentId + '/reserved-periods?startDate=' + startDate + '&endDate=' + endDate;
}

function isValidPeriodList(value) {
  return Array.isArray(value) && value.every((period) =>
    period && /^\d{4}-\d{2}-\d{2}$/.test(period.startDate)
      && /^\d{4}-\d{2}-\d{2}$/.test(period.endDate)
      && period.startDate <= period.endDate
  );
}

export default function BookingDialog({ equipment, onClose, onCreated }) {
  const today = toLocalISO(new Date());
  const now = new Date();
  const [monthOffset, setMonthOffset] = useState(0);
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');
  const [availability, setAvailability] = useState(null);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState('');
  const [reload, setReload] = useState(0);
  const [checking, setChecking] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const firstRef = useRef(null);
  const dialogRef = useRef(null);
  useModalFocus(dialogRef, firstRef, onClose);

  const shownMonth = new Date(now.getFullYear(), now.getMonth() + monthOffset, 1);
  const year = shownMonth.getFullYear();
  const month = shownMonth.getMonth();
  const monthKey = year + '-' + String(month + 1).padStart(2, '0');
  const firstDay = new Date(year, month, 1).getDay();
  const daysInMonth = new Date(year, month + 1, 0).getDate();
  const monthStart = toLocalISO(new Date(year, month, 1));
  const monthEnd = toLocalISO(new Date(year, month + 1, 0));
  const days = [
    ...Array.from({ length: firstDay }, () => null),
    ...Array.from({ length: daysInMonth }, (_, index) => index + 1),
  ];
  const visiblePeriods = availability?.monthKey === monthKey ? availability.periods : null;
  const loadingMonth = loading || !visiblePeriods;

  useEffect(() => {
    const controller = new AbortController();
    setLoading(true);
    setLoadError('');

    apiRequest(reservedPeriodsPath(equipment.id, monthStart, monthEnd), { signal: controller.signal })
      .then((data) => {
        if (!isValidPeriodList(data)) throw new Error('예약 현황 응답 형식을 확인할 수 없습니다.');
        if (!controller.signal.aborted) setAvailability({ monthKey, periods: data });
      })
      .catch((caught) => {
        if (!controller.signal.aborted) setLoadError(caught.message || '예약 현황을 가져올 수 없습니다.');
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false);
      });

    return () => controller.abort();
  }, [equipment.id, monthStart, monthEnd, monthKey, reload]);

  async function selectDay(date) {
    if (checking || busy || loadingMonth || loadError) return;
    setError('');

    // 첫 클릭이나 기간 선택 완료 후의 클릭은 시작일을 다시 선택한다.
    if (!startDate || endDate || date < startDate) {
      setStartDate(date);
      setEndDate('');
      return;
    }

    if (date === startDate) {
      setEndDate(date);
      return;
    }

    // 두 달 이상에 걸친 선택도 서버에서 전체 기간을 다시 확인한다.
    setChecking(true);
    try {
      const periods = await apiRequest(reservedPeriodsPath(equipment.id, startDate, date));
      if (!isValidPeriodList(periods)) throw new Error('예약 현황 응답 형식을 확인할 수 없습니다.');
      if (periods.length > 0) {
        setError('선택한 기간에 예약된 날짜가 포함돼 있어요. 종료일을 다시 선택해 주세요.');
        return;
      }
      setEndDate(date);
    } catch (caught) {
      setError(caught.message || '선택한 기간을 확인하지 못했습니다.');
    } finally {
      setChecking(false);
    }
  }

  async function submit(event) {
    event.preventDefault();
    if (busy || checking || loadingMonth || loadError) return;
    if (!startDate || !endDate || startDate > endDate) {
      setError('예약 시작일과 종료일을 확인해 주세요.');
      return;
    }
    setBusy(true);
    setError('');

    try {
      const reservation = await apiRequest('/reservations', {
        method: 'POST',
        body: { equipmentId: equipment.id, startDate, endDate },
      });
      onCreated(reservation);
    } catch (caught) {
      if (caught.status === 409) {
        setStartDate('');
        setEndDate('');
        setReload((value) => value + 1);
        setError('다른 예약이 먼저 등록됐어요. 갱신된 달력에서 날짜를 다시 선택해 주세요.');
      } else {
        setError(caught.message || '예약을 완료하지 못했습니다.');
      }
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="modal-backdrop" onMouseDown={(event) => { if (event.target === event.currentTarget) onClose(); }}>
      <section ref={dialogRef} className="modal booking-dialog booking-calendar-dialog" role="dialog" aria-modal="true" aria-labelledby="booking-title">
        <button ref={firstRef} type="button" className="icon-button modal-close" onClick={onClose} aria-label="닫기"><X size={20}/></button>
        <div className="modal-symbol"><CalendarDays size={24}/></div>
        <p className="eyebrow">기자재 예약</p>
        <h2 id="booking-title">예약 날짜 선택</h2>
        <p className="muted">달력에서 시작일과 종료일을 차례로 눌러 주세요.</p>
        <div className="chosen-device">
          <span className="chosen-icon"><Package size={22}/></span>
          <span><strong>{equipment.name}</strong><small>기자재 번호 {equipment.id}</small></span>
          <Check size={18} className="chosen-check"/>
        </div>

        <div className="booking-calendar">
          <div className="booking-calendar-top">
            <strong>{year}년 {month + 1}월</strong>
            <div className="booking-calendar-controls">
              <button type="button" className="icon-button" disabled={monthOffset === 0 || busy || checking} onClick={() => setMonthOffset((value) => value - 1)} aria-label="이전 달"><ChevronLeft size={20}/></button>
              <button type="button" className="icon-button" disabled={busy || checking || loading} onClick={() => setReload((value) => value + 1)} aria-label="예약 현황 새로고침"><RefreshCw size={17}/></button>
              <button type="button" className="icon-button" disabled={busy || checking} onClick={() => setMonthOffset((value) => value + 1)} aria-label="다음 달"><ChevronRight size={20}/></button>
            </div>
          </div>
          {loadError ? (
            <div className="booking-calendar-error">
              <p className="inline-notice error" role="alert">{loadError}</p>
              <button type="button" className="button outline" onClick={() => setReload((value) => value + 1)}>다시 시도</button>
            </div>
          ) : (
            <>
              <div className="booking-calendar-grid" aria-label={year + '년 ' + (month + 1) + '월 예약 달력'}>
                {['일', '월', '화', '수', '목', '금', '토'].map((weekday) => (
                  <span className="booking-weekday" key={weekday}>{weekday}</span>
                ))}
                {days.map((day, index) => {
                  if (day === null) return <span className="booking-day-empty" key={'empty-' + index}/>;
                  const date = toLocalISO(new Date(year, month, day));
                  const reserved = visiblePeriods?.some((period) => period.startDate <= date && period.endDate >= date) ?? false;
                  const past = date < today;
                  const picked = date === startDate || date === endDate;
                  const inRange = Boolean(startDate && endDate && date >= startDate && date <= endDate);
                  const disabled = loadingMonth || Boolean(loadError) || reserved || past || busy || checking;
                  const className = 'booking-day' +
                    (reserved ? ' is-reserved' : '') +
                    (inRange ? ' is-in-range' : '') +
                    (picked ? ' is-picked' : '');
                  const label = date + ' · ' + (reserved ? '예약됨' : past ? '지난 날짜' : picked ? '선택됨' : '예약 가능');
                  return (
                    <button
                      key={date}
                      type="button"
                      className={className}
                      disabled={disabled}
                      aria-label={label}
                      aria-pressed={picked}
                      onClick={() => selectDay(date)}
                    >
                      {day}
                    </button>
                  );
                })}
              </div>
              {loadingMonth && <p className="booking-calendar-loading" role="status">예약된 날짜를 불러오고 있어요.</p>}
              <div className="booking-calendar-legend">
                <span><i className="booking-legend-dot available"/> 예약 가능</span>
                <span><i className="booking-legend-dot reserved"/> 예약됨</span>
                <span><i className="booking-legend-dot selected"/> 선택한 날짜</span>
              </div>
            </>
          )}
        </div>

        <form className="stack-form booking-calendar-form" onSubmit={submit}>
          <div className="booking-selected-period">
            <div><span>시작일</span><strong>{formatDate(startDate)}</strong></div>
            <span aria-hidden="true">–</span>
            <div><span>종료일</span><strong>{formatDate(endDate)}</strong></div>
            {startDate && (
              <button type="button" className="button subtle" disabled={busy || checking} onClick={() => { setStartDate(''); setEndDate(''); setError(''); }}>
                다시 선택
              </button>
            )}
          </div>
          {checking && <p className="form-tip" role="status">선택한 기간의 예약 여부를 확인하고 있어요.</p>}
          {error && <p className="inline-notice error" role="alert">{error}</p>}
          <button className="button primary full" type="submit" disabled={busy || checking || loadingMonth || Boolean(loadError) || !endDate}>
            {busy ? '예약 확인 중...' : '이 기간으로 예약하기'}<ArrowRight size={17}/>
          </button>
        </form>
      </section>
    </div>
  );
}
