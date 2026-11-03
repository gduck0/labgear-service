export function formatDate(value) {
  if (!value) return '—';
  const [year, month, day] = value.split('-');
  return `${year}.${month}.${day}`;
}

function todayLocalDate() {
  const now = new Date();
  const year = now.getFullYear();
  const month = String(now.getMonth() + 1).padStart(2, '0');
  const day = String(now.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

export function getReservationDisplay(reservation) {
  switch (reservation.status) {
    case 'ACTIVE':
      return { label: '예약 확정', className: 'active' };
    case 'RENTED':
      return reservation.endDate && reservation.endDate < todayLocalDate()
        ? { label: '연체 중', className: 'overdue' }
        : { label: '대여 중', className: 'rented' };
    case 'RETURNED':
      return { label: '반납 완료', className: 'returned' };
    case 'CANCELED':
      return { label: '취소됨', className: 'canceled' };
    default:
      return { label: '상태 확인 필요', className: 'canceled' };
  }
}
