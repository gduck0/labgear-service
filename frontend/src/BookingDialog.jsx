import { useEffect, useRef, useState } from 'react';
import { ArrowRight, CalendarDays, Check, Package, X } from 'lucide-react';
import { apiRequest } from './apiClient.js';

function todayLocal() {
  const now = new Date();
  const year = now.getFullYear();
  const month = String(now.getMonth() + 1).padStart(2, '0');
  const day = String(now.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

export default function BookingDialog({ equipment, onClose, onCreated }) {
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const startRef = useRef(null);
  const today = todayLocal();

  useEffect(() => {
    startRef.current?.focus();
    function handleKey(event) { if (event.key === 'Escape') onClose(); }
    document.addEventListener('keydown', handleKey);
    return () => document.removeEventListener('keydown', handleKey);
  }, [onClose]);

  async function submit(event) {
    event.preventDefault();
    if (busy) return;
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
      setError(caught.status === 409
        ? '선택한 기간에 이미 예약이 있어요. 다른 날짜를 선택해 주세요.'
        : caught.message || '예약을 완료하지 못했습니다.');
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="modal-backdrop" onMouseDown={(event) => { if (event.target === event.currentTarget) onClose(); }}>
      <section className="modal booking-dialog" role="dialog" aria-modal="true" aria-labelledby="booking-title">
        <button className="icon-button modal-close" onClick={onClose} aria-label="닫기"><X size={20}/></button>
        <div className="modal-symbol"><CalendarDays size={24}/></div>
        <p className="eyebrow">기자재 예약</p>
        <h2 id="booking-title">언제 사용하시나요?</h2>
        <p className="muted">사용할 날짜를 선택하면 예약을 접수할 수 있어요.</p>
        <div className="chosen-device"><span className="chosen-icon"><Package size={22}/></span><span><strong>{equipment.name}</strong><small>기자재 번호 {equipment.id}</small></span><Check size={18} className="chosen-check"/></div>
        <form className="stack-form" onSubmit={submit}>
          <div className="date-pair">
            <div><label htmlFor="booking-start">시작일</label><input ref={startRef} id="booking-start" type="date" min={today} required value={startDate} onChange={(event) => { setStartDate(event.target.value); if (endDate && event.target.value > endDate) setEndDate(''); }}/></div>
            <div><label htmlFor="booking-end">종료일</label><input id="booking-end" type="date" min={startDate || today} required value={endDate} onChange={(event) => setEndDate(event.target.value)}/></div>
          </div>
          <p className="form-tip">시작일과 종료일 모두 예약 기간에 포함돼요. 날짜가 겹치면 서버에서 예약을 거부합니다.</p>
          {error && <p className="inline-notice error" role="alert">{error}</p>}
          <button className="button primary full" type="submit" disabled={busy}>{busy ? '예약 확인 중...' : '이 기간으로 예약하기'}<ArrowRight size={17}/></button>
        </form>
      </section>
    </div>
  );
}
