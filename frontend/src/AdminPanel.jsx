import { useEffect, useState } from 'react';
import { AlertCircle, Pencil, Plus, ShieldCheck, Trash2 } from 'lucide-react';
import { apiRequest } from './apiClient.js';

export default function AdminPanel({ equipment, onEquipmentChanged }) {
  const [newId, setNewId] = useState('');
  const [newName, setNewName] = useState('');
  const [selectedId, setSelectedId] = useState('');
  const [rename, setRename] = useState('');
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState(null);
  const selected = equipment.find((item) => String(item.id) === selectedId);

  useEffect(() => {
    setRename(selected?.name || '');
  }, [selected?.id, selected?.name]);

  async function run(action, task) {
    setBusy(true);
    setMessage(null);
    try {
      await task();
      setMessage({ type: 'success', text: `${action} 작업을 완료했어요.` });
      onEquipmentChanged();
    } catch (caught) {
      setMessage({ type: 'error', text: caught.message || '요청에 실패했습니다.' });
    } finally {
      setBusy(false);
    }
  }

  async function createEquipment(event) {
    event.preventDefault();
    const id = Number(newId);
    const name = newName.trim();
    if (!Number.isSafeInteger(id) || id < 1 || id > 2147483647 || !name || name.length > 100) {
      setMessage({ type: 'error', text: '기자재 번호와 이름을 확인해 주세요.' });
      return;
    }
    await run('기자재 등록', async () => {
      await apiRequest('/equipment', { method: 'POST', body: { id, name } });
      setNewId('');
      setNewName('');
    });
  }

  async function renameEquipment(event) {
    event.preventDefault();
    if (!selected || !rename.trim() || rename.trim().length > 100) return;
    await run('이름 변경', () => apiRequest(`/equipment/${selected.id}/name`, {
      method: 'PATCH', body: { name: rename.trim() },
    }));
  }

  async function deleteEquipment() {
    if (!selected || busy) return;
    if (!window.confirm(`'${selected.name}' 기자재를 삭제할까요?\n연결된 예약이 있으면 서버에서 삭제를 거부합니다.`)) return;
    await run('기자재 삭제', async () => {
      await apiRequest(`/equipment/${selected.id}`, { method: 'DELETE' });
      setSelectedId('');
    });
  }

  return (
    <section className="page-section" aria-labelledby="admin-title">
      <div className="section-top"><div><p className="eyebrow">ADMINISTRATION</p><h1 id="admin-title">기자재 관리</h1><p className="muted">관리자만 이용할 수 있는 기자재 등록·수정·삭제 화면입니다.</p></div><span className="admin-tag"><ShieldCheck size={16}/> 관리자 전용</span></div>
      {message && <div className={`inline-notice ${message.type}`} role={message.type === 'error' ? 'alert' : 'status'}>{message.text}</div>}
      <div className="admin-grid">
        <article className="form-card"><div className="form-card-head"><span className="panel-icon"><Plus size={20}/></span><h2>새 기자재 등록</h2></div>
          <form className="stack-form" onSubmit={createEquipment}>
            <label htmlFor="admin-id">기자재 번호</label><input id="admin-id" type="number" min="1" max="2147483647" required value={newId} onChange={(event) => setNewId(event.target.value)} placeholder="예: 1001"/>
            <label htmlFor="admin-name">기자재 이름</label><input id="admin-name" type="text" maxLength="100" required value={newName} onChange={(event) => setNewName(event.target.value)} placeholder="예: 디지털 오실로스코프"/>
            <button className="button primary full" type="submit" disabled={busy}><Plus size={17}/> 등록하기</button>
          </form>
        </article>
        <article className="form-card"><div className="form-card-head"><span className="panel-icon"><Pencil size={20}/></span><h2>기자재 정보 관리</h2></div>
          <div className="stack-form">
            <label htmlFor="admin-select">관리할 기자재 선택</label>
            <select id="admin-select" value={selectedId} onChange={(event) => setSelectedId(event.target.value)}><option value="">기자재를 선택해 주세요</option>{equipment.map((item) => <option value={item.id} key={item.id}>#{item.id} · {item.name}</option>)}</select>
            <form className="stack-form" onSubmit={renameEquipment}><label htmlFor="admin-rename">변경할 이름</label><input id="admin-rename" type="text" maxLength="100" required value={rename} onChange={(event) => setRename(event.target.value)} disabled={!selected || busy}/><button className="button outline full" type="submit" disabled={!selected || busy || rename.trim() === selected.name}><Pencil size={17}/> 이름 변경</button></form>
            <button className="button danger-outline full" type="button" disabled={!selected || busy} onClick={deleteEquipment}><Trash2 size={17}/> 기자재 삭제</button>
          </div>
          <p className="form-tip"><AlertCircle size={14}/> 예약이 연결된 기자재는 삭제할 수 없습니다.</p>
        </article>
      </div>
    </section>
  );
}
