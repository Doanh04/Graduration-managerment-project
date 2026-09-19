import React, { useEffect, useState } from 'react';
import { AlertCircle, Check, ChevronDown, LoaderCircle, Plus, Search, X } from 'lucide-react';
import API_ENDPOINTS from '../../config/endpoints.js';
import ResourceService from '../../services/ResourceService.jsx';
import { useToast } from '../../context/ToastContext.jsx';
import getApiErrorMessage from '../../utils/apiError.js';
import { formatDateTimeDisplay } from '../../utils/dateFormat.js';
import '../../style/WorkflowPages.scss';
import '../../style/Search.scss';

const statusLabels = { ASSIGNED: 'Đã phân công', IN_PROGRESS: 'Đang phản biện', SUBMITTED: 'Đã gửi', APPROVED: 'Đã duyệt', REVISION_REQUIRED: 'Yêu cầu chỉnh sửa', CANCELLED: 'Đã hủy' };
export default function ReviewAssignmentAdminPage() {
  const toast = useToast(); const [topics, setTopics] = useState([]); const [lecturers, setLecturers] = useState([]); const [items, setItems] = useState([]); const [query, setQuery] = useState(''); const [modal, setModal] = useState(false); const [form, setForm] = useState({ topicId: '', lectureId: '', deadline: '', note: '' }); const [loading, setLoading] = useState(true); const [saving, setSaving] = useState(false); const [error, setError] = useState('');
  const load = async () => { setLoading(true); try { const [allTopics, allLecturers] = await Promise.all([ResourceService.getAll(API_ENDPOINTS.topics.list), ResourceService.getAll(API_ENDPOINTS.lecturers.list)]); const eligible = allTopics.filter((topic) => topic.teamId && ['APPROVED', 'REGISTERED', 'IN_PROGRESS'].includes(topic.status)); const grouped = (await Promise.all(eligible.map(async (topic) => (await ResourceService.getAll(API_ENDPOINTS.reviews.byTopic(topic.topicId))).map((item) => ({ ...item, topicTitle: item.topicTitle || topic.title })) ))).flat(); setTopics(eligible); setLecturers(allLecturers); setItems(grouped); } catch (e) { setError(getApiErrorMessage(e, 'Không thể tải dữ liệu phân công phản biện.')); } finally { setLoading(false); } };
  useEffect(() => { load(); }, []); // eslint-disable-line react-hooks/exhaustive-deps
  const create = async (event) => { event.preventDefault(); if (!form.topicId || !form.lectureId || !form.deadline) { setError('Vui lòng chọn đề tài, giảng viên và hạn phản biện.'); return; } setSaving(true); setError(''); try { await ResourceService.create(API_ENDPOINTS.reviews.assign(form.topicId), { lectureId: form.lectureId, deadline: form.deadline, note: form.note || null }); toast.success('Đã phân công giảng viên phản biện.'); setModal(false); setForm({ topicId: '', lectureId: '', deadline: '', note: '' }); await load(); } catch (e) { const message = getApiErrorMessage(e, 'Không thể phân công phản biện.'); setError(message); toast.error(message, { title: 'Phân công thất bại' }); } finally { setSaving(false); } };
  const filtered = items.filter((item) => `${item.topicTitle} ${item.lectureName} ${item.teamName}`.toLowerCase().includes(query.toLowerCase()));
  return <div className="workflow-page">
    <div className="page-heading"><div><span>QUẢN LÝ PHẢN BIỆN</span><h1>Phân công phản biện đề tài</h1><p>Phân công giảng viên và theo dõi tiến độ phản biện.</p></div><button className="primary-button" onClick={() => { setError(''); setModal(true); }}><Plus size={17} /> Phân công mới</button></div>
    {error && !modal && <div className="workflow-error"><AlertCircle size={17} />{error}</div>}
    <section className="workflow-panel"><div className="workflow-toolbar"><label><Search size={17} /><input value={query} onChange={(e) => setQuery(e.target.value)} placeholder="Tìm theo đề tài, nhóm hoặc giảng viên..." /></label></div>
      {loading ? <div className="workflow-state"><LoaderCircle className="spin" /><strong>Đang tải dữ liệu...</strong></div> : <div className="workflow-table"><table><thead><tr><th>Đề tài</th><th>Giảng viên phản biện</th><th>Hạn phản biện</th><th>Trạng thái</th></tr></thead><tbody>{filtered.map((item) => <tr key={item.assignmentId}><td><strong>{item.topicTitle}</strong><small>{item.teamName || 'Chưa có nhóm'}</small></td><td>{item.lectureName || item.lectureCode || '—'}</td><td>{formatDate(item.deadline)}</td><td><span className={`workflow-status ${String(item.status).toLowerCase()}`}>{statusLabels[item.status] || item.status}</span></td></tr>)}</tbody></table>{!filtered.length && <div className="workflow-state"><strong>Chưa có phân công phản biện.</strong></div>}</div>}
    </section>
    {modal && <div className="modal-backdrop" onMouseDown={(event) => event.target === event.currentTarget && !saving && setModal(false)}><section className="workflow-modal compact review-assignment-modal" role="dialog" aria-modal="true">
      <header><div><span>PHÂN CÔNG MỚI</span><h3>Giảng viên phản biện</h3></div><button type="button" disabled={saving} onClick={() => setModal(false)} aria-label="Đóng"><X size={19} /></button></header>
      <form onSubmit={create}>
        <SearchChoice label="Đề tài" required value={form.topicId} items={topics} valueKey="topicId" labelKey="title" secondaryKey="teamName" placeholder="Tìm và chọn đề tài đã có nhóm..." optionEntity="đề tài" emptyText="Không có đề tài đã được gán nhóm và phê duyệt." onChange={(value) => setForm({ ...form, topicId: value })} />
        <SearchChoice label="Giảng viên phản biện" required value={form.lectureId} items={lecturers} valueKey="lectureId" labelKey="fullName" secondaryKey="lecturerCode" placeholder="Tìm theo tên hoặc mã giảng viên..." optionEntity="giảng viên" emptyText="Không có giảng viên phù hợp." onChange={(value) => setForm({ ...form, lectureId: value })} />
        <label>Hạn phản biện <b>*</b><input required type="datetime-local" value={form.deadline} onChange={(e) => setForm({ ...form, deadline: e.target.value })} /></label>
        <label>Ghi chú<textarea value={form.note} onChange={(e) => setForm({ ...form, note: e.target.value })} placeholder="Nội dung cần lưu ý cho phân công này..." maxLength={1000} /></label>
        {error && <div className="workflow-error"><AlertCircle size={16} />{error}</div>}
        <footer><button type="button" className="secondary-button" disabled={saving} onClick={() => setModal(false)}>Hủy</button><button className="primary-button" disabled={saving}>{saving ? 'Đang lưu...' : 'Lưu phân công'}</button></footer>
      </form>
    </section></div>}
  </div>;
}
function SearchChoice({ label, required, value, items, valueKey, labelKey, secondaryKey, placeholder, optionEntity, emptyText, onChange }) {
  const selected = items.find((item) => String(item[valueKey] || item.lectureId || item.assignmentIdentifier) === String(value));
  const [query, setQuery] = useState(selected ? displayValue(selected, labelKey, secondaryKey) : '');
  const [open, setOpen] = useState(false);
  useEffect(() => { setQuery(selected ? displayValue(selected, labelKey, secondaryKey) : ''); }, [value, items.length]); // eslint-disable-line react-hooks/exhaustive-deps
  const normalized = query.trim().toLowerCase();
  const filtered = items.filter((item) => displayValue(item, labelKey, secondaryKey).toLowerCase().includes(normalized));
  const choose = (item) => { const optionValue = item[valueKey] || item.lectureId || item.assignmentIdentifier; onChange(optionValue); setQuery(displayValue(item, labelKey, secondaryKey)); setOpen(false); };
  return <label className="review-choice">{label}{required && <b>*</b>}<div className={`searchable-select ${open ? 'open' : ''}`}><Search size={16} /><input value={query} required={required} placeholder={placeholder} autoComplete="off" onFocus={() => setOpen(true)} onChange={(event) => { setQuery(event.target.value); onChange(''); setOpen(true); }} /><button type="button" className="select-toggle" aria-label={`Mở danh sách ${optionEntity}`} aria-expanded={open} onClick={() => setOpen((current) => !current)}><ChevronDown size={17} /></button>{open && <div className="searchable-options">{filtered.length ? <><span className="options-caption">Danh sách {optionEntity} · {filtered.length} kết quả</span>{filtered.map((item) => { const optionValue = item[valueKey] || item.lectureId || item.assignmentIdentifier; return <button type="button" key={optionValue} className={String(value) === String(optionValue) ? 'selected' : ''} onClick={() => choose(item)}><span><strong>{item[labelKey] || item.fullName || item.lectureName || item.title}</strong><small>{item[secondaryKey] || item.teamName || item.lectureCode || '—'}</small></span>{String(value) === String(optionValue) && <Check size={16} />}</button>; })}</> : <span className="option-message">{items.length ? 'Không tìm thấy dữ liệu phù hợp.' : emptyText}</span>}</div>}</div></label>;
}
function displayValue(item, labelKey, secondaryKey) { return [item[labelKey] || item.fullName || item.lectureName || item.title, item[secondaryKey] || item.teamName || item.lectureCode].filter(Boolean).join(' · '); }
function formatDate(value) { return formatDateTimeDisplay(value); }
