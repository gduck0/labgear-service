import { useEffect, useState } from 'react';
import {
  ArrowRight, CalendarDays, CheckCircle2, CircleAlert,
  ClipboardList, Hash, PackagePlus, PencilLine, Search,
  ShieldAlert, Trash2,
} from 'lucide-react';
import './OperationsPanel.css';

const API_ROOT = (import.meta.env.VITE_API_BASE_URL || '/api').replace(/\/$/, '');

async function requestApi(path, { method = 'GET', body } = {}) {
  const response = await fetch(API_ROOT + path, {
    method,
    headers: {
      Accept: 'application/json',
      ...(body === undefined ? {} : { 'Content-Type': 'application/json' }),
    },
    ...(body === undefined ? {} : { body: JSON.stringify(body) }),
  });

  const raw = await response.text();
  let result = null;
  if (raw) {
    try {
      result = JSON.parse(raw);
    } catch {
      if (response.ok) throw new Error('서버 응답을 해석할 수 없습니다.');
    }
  }
  if (!response.ok) {
    const error = new Error(
      typeof result?.message === 'string' && result.message.trim()
        ? result.message
        : '요청을 처리하지 못했습니다.'
    );
    error.status = response.status;
    throw error;
  }
  return result;
}

function describeError(error) {
  if (!error?.status) return error instanceof TypeError
    ? '서버에 연결할 수 없습니다. 백엔드 실행 상태를 확인해 주세요.'
    : error?.message || '알 수 없는 오류가 발생했습니다.';
  if (error.status === 400) return error.message === '요청을 처리하지 못했습니다.'
    ? '입력한 값을 다시 확인해 주세요.'
    : error.message;
  if (error.status === 404) return error.message === '요청을 처리하지 못했습니다.'
    ? '해당 데이터를 찾을 수 없습니다.'
    : error.message;
  if (error.status === 409) return error.message === '요청을 처리하지 못했습니다.'
    ? '중복되거나 충돌하는 데이터입니다.'
    : error.message;
  return error.status >= 500
    ? '서버 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.'
    : error.message;
}

function toPositiveId(raw) {
  if (!/^[1-9]\d*$/.test(String(raw))) return null;
  const value = Number(raw);
  return Number.isSafeInteger(value) && value <= 2147483647 ? value : null;
}

function Notice({ notice }) {
  if (!notice) return null;
  const success = notice.type === 'success';
  return (
    <div className={'workspace-notice ' + (success ? 'is-success' : 'is-error')} role={success ? 'status' : 'alert'}>
      {success ? <CheckCircle2 size={18} /> : <CircleAlert size={18} />}
      <span>{notice.message}</span>
    </div>
  );
}

function EquipmentWorkspace({ selectedEquipment, selectedEquipmentId, onEquipmentChanged }) {
  const [createForm, setCreateForm] = useState({ id: '', name: '' });
  const [editName, setEditName] = useState('');
  const [busy, setBusy] = useState('');
  const [notice, setNotice] = useState(null);

  useEffect(() => {
    setEditName(selectedEquipment?.name ?? '');
  }, [selectedEquipment?.id, selectedEquipment?.name]);

  async function createEquipment(event) {
    event.preventDefault();
    const id = toPositiveId(createForm.id);
    const name = createForm.name.trim();
    if (!id || !name || name.length > 100) {
      setNotice({ type: 'error', message: '기자재 번호는 양의 정수, 이름은 1~100자로 입력해 주세요.' });
      return;
    }
    setBusy('create');
    setNotice(null);
    try {
      await requestApi('/equipment', { method: 'POST', body: { id, name } });
      setCreateForm({ id: '', name: '' });
      setNotice({ type: 'success', message: '기자재 NO. ' + id + ' 등록이 완료됐습니다.' });
      onEquipmentChanged(id);
    } catch (error) {
      setNotice({ type: 'error', message: describeError(error) });
    } finally {
      setBusy('');
    }
  }

  async function renameEquipment(event) {
    event.preventDefault();
    if (!selectedEquipment || busy) return;
    const name = editName.trim();
    if (!name || name.length > 100) {
      setNotice({ type: 'error', message: '변경할 기자재 이름을 1~100자로 입력해 주세요.' });
      return;
    }
    if (name === selectedEquipment.name) {
      setNotice({ type: 'error', message: '현재 이름과 동일합니다.' });
      return;
    }
    setBusy('rename');
    setNotice(null);
    try {
      await requestApi('/equipment/' + selectedEquipment.id + '/name', {
        method: 'PATCH', body: { name },
      });
      setNotice({ type: 'success', message: '기자재 이름을 변경했습니다.' });
      onEquipmentChanged(selectedEquipment.id);
    } catch (error) {
      setNotice({ type: 'error', message: describeError(error) });
    } finally {
      setBusy('');
    }
  }

  async function deleteEquipment() {
    if (!selectedEquipment || busy) return;
    const agreed = window.confirm(
      '기자재 NO. ' + selectedEquipment.id + ' · ' + selectedEquipment.name
      + '\n정말 삭제하시겠습니까?\n예약이 연결되어 있다면 서버에서 삭제를 거부합니다.'
    );
    if (!agreed) return;
    setBusy('delete');
    setNotice(null);
    try {
      await requestApi('/equipment/' + selectedEquipment.id, { method: 'DELETE' });
      setNotice({ type: 'success', message: '기자재 NO. ' + selectedEquipment.id + '을(를) 삭제했습니다.' });
      onEquipmentChanged(null);
    } catch (error) {
      setNotice({ type: 'error', message: describeError(error) });
    } finally {
      setBusy('');
    }
  }

  return (
    <section className="workspace-section" id="manage" aria-labelledby="manage-title">
      <div className="workspace-heading">
        <span className="eyebrow">EQUIPMENT OPERATIONS</span>
        <h2 id="manage-title">기자재 관리<span className="title-dot">.</span></h2>
        <p>실제 Spring API에 연결해 기자재를 등록하고, 선택한 기자재의 정보를 수정할 수 있어요.</p>
      </div>
      <div className="workspace-grid">
        <article className="workspace-panel">
          <div className="workspace-panel-title">
            <span className="workspace-icon"><PackagePlus size={20} /></span>
            <div><h3>새 기자재 등록</h3><p>등록 후 목록과 상세 정보가 새로고침됩니다.</p></div>
          </div>
          <form className="workspace-form" onSubmit={createEquipment}>
            <label htmlFor="equipment-new-id">기자재 번호</label>
            <input
              id="equipment-new-id" type="number" min="1" max="2147483647" step="1"
              placeholder="예: 1001" required value={createForm.id}
              onChange={(event) => setCreateForm((prev) => ({ ...prev, id: event.target.value }))}
            />
            <label htmlFor="equipment-new-name">기자재 이름</label>
            <input
              id="equipment-new-name" type="text" maxLength={100}
              placeholder="예: 디지털 오실로스코프" required value={createForm.name}
              onChange={(event) => setCreateForm((prev) => ({ ...prev, name: event.target.value }))}
            />
            <button className="workspace-button primary" type="submit" disabled={Boolean(busy)}>
              <PackagePlus size={16} />{busy === 'create' ? '등록 중...' : '기자재 등록'}
              {busy !== 'create' && <ArrowRight size={15} />}
            </button>
          </form>
        </article>
        <article className="workspace-panel">
          <div className="workspace-panel-title">
            <span className="workspace-icon subdued"><PencilLine size={20} /></span>
            <div><h3>선택 기자재 수정</h3><p>위 목록에서 기자재를 선택하면 관리할 수 있어요.</p></div>
          </div>
          <div className="workspace-selected" aria-live="polite">
            <Hash size={16} />
            {selectedEquipment
              ? <span><strong>NO. {selectedEquipment.id}</strong> · {selectedEquipment.name}</span>
              : <span>{selectedEquipmentId === null ? '선택된 기자재가 없습니다.' : '선택 기자재 정보를 불러오는 중이거나 조회할 수 없습니다.'}</span>}
          </div>
          <form className="workspace-form" onSubmit={renameEquipment}>
            <label htmlFor="equipment-rename">새 기자재 이름</label>
            <input
              id="equipment-rename" type="text" maxLength={100}
              placeholder="변경할 이름" required disabled={!selectedEquipment || Boolean(busy)}
              value={editName} onChange={(event) => setEditName(event.target.value)}
            />
            <div className="workspace-actions">
              <button className="workspace-button primary" type="submit" disabled={!selectedEquipment || Boolean(busy)}>
                <PencilLine size={16} />{busy === 'rename' ? '수정 중...' : '이름 저장'}
              </button>
              <button className="workspace-button danger" type="button"
                disabled={!selectedEquipment || Boolean(busy)} onClick={deleteEquipment}>
                <Trash2 size={16} />{busy === 'delete' ? '삭제 중...' : '삭제'}
              </button>
            </div>
          </form>
          <p className="workspace-footnote"><ShieldAlert size={14} /> 예약이 있는 기자재는 서버가 삭제를 차단합니다.</p>
        </article>
      </div>
      <Notice notice={notice} />
    </section>
  );
}

function ReservationWorkspace({ selectedEquipmentId }) {
  const [form, setForm] = useState({
    id: '', studentId: '', equipmentId: '', startDate: '', endDate: '',
  });
  const [busy, setBusy] = useState(false);
  const [notice, setNotice] = useState(null);
  const [lookupId, setLookupId] = useState('');
  const [lookupBusy, setLookupBusy] = useState(false);
  const [lookupError, setLookupError] = useState('');
  const [reservation, setReservation] = useState(null);

  useEffect(() => {
    if (selectedEquipmentId != null) {
      setForm((prev) => ({ ...prev, equipmentId: String(selectedEquipmentId) }));
    }
  }, [selectedEquipmentId]);

  function editForm(field, value) {
    setForm((prev) => ({ ...prev, [field]: value }));
  }

  async function createReservation(event) {
    event.preventDefault();
    const id = toPositiveId(form.id);
    const studentId = toPositiveId(form.studentId);
    const equipmentId = toPositiveId(form.equipmentId);
    if (!id || !studentId || !equipmentId) {
      setNotice({ type: 'error', message: '예약·학생·기자재 번호는 모두 양의 정수여야 합니다.' });
      return;
    }
    if (!form.startDate || !form.endDate || form.startDate > form.endDate) {
      setNotice({ type: 'error', message: '시작일은 종료일보다 늦을 수 없습니다.' });
      return;
    }
    setBusy(true);
    setNotice(null);
    try {
      await requestApi('/reservations', {
        method: 'POST',
        body: { id, studentId, equipmentId, startDate: form.startDate, endDate: form.endDate },
      });
      setNotice({ type: 'success', message: '예약 NO. ' + id + ' 접수가 완료됐습니다. 아래에서 번호로 조회할 수 있어요.' });
      setLookupId(String(id));
      setReservation(null);
      setLookupError('');
      setForm((prev) => ({ ...prev, id: '', startDate: '', endDate: '' }));
    } catch (error) {
      setNotice({ type: 'error', message: describeError(error) });
    } finally {
      setBusy(false);
    }
  }

  async function findReservation(event) {
    event.preventDefault();
    const id = toPositiveId(lookupId);
    if (!id) {
      setLookupError('올바른 예약 번호를 입력해 주세요.');
      setReservation(null);
      return;
    }
    setLookupBusy(true);
    setLookupError('');
    setReservation(null);
    try {
      const data = await requestApi('/reservations/' + id);
      if (!data || typeof data !== 'object') {
        setLookupError('예약 응답을 확인할 수 없습니다.');
      } else {
        setReservation(data);
      }
    } catch (error) {
      setLookupError(describeError(error));
    } finally {
      setLookupBusy(false);
    }
  }

  return (
    <section className="workspace-section reservation-section" id="reservations" aria-labelledby="reservation-title">
      <div className="workspace-heading">
        <span className="eyebrow">RESERVATION DESK</span>
        <h2 id="reservation-title">예약 접수 및 조회<span className="title-dot">.</span></h2>
        <p>예약 가능한 기간을 요청하고, 서버에 저장된 예약을 번호로 확인할 수 있어요.</p>
      </div>
      <div className="workspace-grid reservation-grid">
        <article className="workspace-panel">
          <div className="workspace-panel-title">
            <span className="workspace-icon"><CalendarDays size={20} /></span>
            <div><h3>새 예약 접수</h3><p>같은 기자재의 겹치는 예약은 409로 거부됩니다.</p></div>
          </div>
          <form className="workspace-form" onSubmit={createReservation}>
            <div className="workspace-field-grid">
              <div><label htmlFor="reservation-id">예약 번호</label>
                <input id="reservation-id" type="number" min="1" max="2147483647" step="1" required
                  placeholder="예: 5001" value={form.id} onChange={(e) => editForm('id', e.target.value)} /></div>
              <div><label htmlFor="reservation-student">학생 번호</label>
                <input id="reservation-student" type="number" min="1" max="2147483647" step="1" required
                  placeholder="등록된 학생 ID" value={form.studentId}
                  onChange={(e) => editForm('studentId', e.target.value)} /></div>
            </div>
            <label htmlFor="reservation-equipment">기자재 번호</label>
            <input id="reservation-equipment" type="number" min="1" max="2147483647" step="1" required
              placeholder="위 목록에서 선택하거나 직접 입력" value={form.equipmentId}
              onChange={(e) => editForm('equipmentId', e.target.value)} />
            <div className="workspace-field-grid">
              <div><label htmlFor="reservation-start">시작일</label>
                <input id="reservation-start" type="date" required value={form.startDate}
                  onChange={(e) => editForm('startDate', e.target.value)} /></div>
              <div><label htmlFor="reservation-end">종료일</label>
                <input id="reservation-end" type="date" required min={form.startDate || undefined}
                  value={form.endDate} onChange={(e) => editForm('endDate', e.target.value)} /></div>
            </div>
            <button className="workspace-button primary" type="submit" disabled={busy}>
              <CalendarDays size={16} />{busy ? '접수 중...' : '예약 접수'}{!busy && <ArrowRight size={15} />}
            </button>
            <p className="workspace-footnote"><CircleAlert size={14} /> 학생은 DB에 미리 등록되어 있어야 합니다. 현재 학생 등록 API는 제공되지 않습니다.</p>
          </form>
          <Notice notice={notice} />
        </article>
        <article className="workspace-panel lookup-panel">
          <div className="workspace-panel-title">
            <span className="workspace-icon subdued"><ClipboardList size={20} /></span>
            <div><h3>예약 번호 조회</h3><p>현재 API는 예약 전체 목록 대신 개별 조회를 지원합니다.</p></div>
          </div>
          <form className="workspace-form" onSubmit={findReservation}>
            <label htmlFor="reservation-lookup">조회할 예약 번호</label>
            <div className="workspace-inline-form">
              <input id="reservation-lookup" type="number" min="1" max="2147483647" step="1"
                value={lookupId} required placeholder="예약 번호 입력"
                onChange={(event) => setLookupId(event.target.value)} />
              <button className="workspace-button secondary" type="submit" disabled={lookupBusy}>
                <Search size={16} />{lookupBusy ? '조회 중' : '조회'}
              </button>
            </div>
          </form>
          {lookupError && <div className="workspace-notice is-error" role="alert"><CircleAlert size={18}/><span>{lookupError}</span></div>}
          {reservation ? (
            <div className="reservation-result" aria-live="polite">
              <div className="reservation-result-heading"><CheckCircle2 size={17}/><strong>예약 정보</strong><span>NO. {reservation.id}</span></div>
              <dl>
                <div><dt>학생 번호</dt><dd>{reservation.studentId}</dd></div>
                <div><dt>기자재 번호</dt><dd>{reservation.equipmentId}</dd></div>
                <div><dt>시작일</dt><dd>{reservation.startDate}</dd></div>
                <div><dt>종료일</dt><dd>{reservation.endDate}</dd></div>
              </dl>
            </div>
          ) : !lookupError && (
            <div className="workspace-placeholder"><ClipboardList size={36}/><strong>예약 정보를 기다리고 있어요</strong><span>예약 번호를 입력하면 서버의 저장 정보를 보여드립니다.</span></div>
          )}
        </article>
      </div>
    </section>
  );
}

export default function OperationsPanel({ selectedEquipment, selectedEquipmentId, onEquipmentChanged }) {
  return (
    <>
      <EquipmentWorkspace
        selectedEquipment={selectedEquipment}
        selectedEquipmentId={selectedEquipmentId}
        onEquipmentChanged={onEquipmentChanged}
      />
      <ReservationWorkspace selectedEquipmentId={selectedEquipmentId} />
    </>
  );
}
