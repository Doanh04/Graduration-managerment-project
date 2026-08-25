import React, { useEffect, useMemo, useState } from 'react';
import { AlertCircle, BookOpen, Check, ChevronDown, Edit3, Eye, GraduationCap, Inbox, LoaderCircle, Mail, Phone, Plus, Search, UserMinus, UserRoundCheck, Users, X } from 'lucide-react';
import ResourceService from '../../services/ResourceService.jsx';
import API_ENDPOINTS from '../../config/endpoints.js';
import { useToast } from '../../context/ToastContext.jsx';
import getApiErrorMessage from '../../utils/apiError.js';
import ConfirmModal from '../../components/feedback/ConfirmModal.jsx';
import '../../style/Search.scss';

const emptyForm = { topicId: '', lectureId: '', note: '' };

export default function SupervisorAssignmentPage() {
  const toast = useToast();
  const [assignments, setAssignments] = useState([]); const [topics, setTopics] = useState([]); const [lecturers, setLecturers] = useState([]);
  const [loading, setLoading] = useState(true); const [saving, setSaving] = useState(false); const [error, setError] = useState('');
  const [dataWarning, setDataWarning] = useState('');
  const [query, setQuery] = useState(''); const [page, setPage] = useState(0); const [modal, setModal] = useState(null); const [confirm, setConfirm] = useState(null); const [form, setForm] = useState(emptyForm); const [formError, setFormError] = useState('');
  const [detail, setDetail] = useState(null);

  const load = async () => {
    setLoading(true);
    setError('');
    setDataWarning('');

    const [topicResult, lecturerResult] = await Promise.allSettled([
      ResourceService.getAll(API_ENDPOINTS.topics.list),
      ResourceService.getAll(API_ENDPOINTS.lecturers.list),
    ]);

    const topicData = topicResult.status === 'fulfilled' ? topicResult.value : [];
    const assignableTopics = topicData.filter((topic) => ['APPROVED', 'REGISTERED', 'IN_PROGRESS'].includes(topic.status));
    setTopics(assignableTopics);

    const lecturerData = lecturerResult.status === 'fulfilled' ? lecturerResult.value : [];
    const activeLecturers = lecturerData.filter((lecturer) => lecturer.status === 'ACTIVE');
    const assignableLecturers = activeLecturers
      .filter((lecturer) => lecturer.lectureId || lecturer.lecturerCode)
      .map((lecturer) => ({
        ...lecturer,
        // Backend mới dùng ID nội bộ; bản backend cũ vẫn có thể gửi mã giảng viên.
        assignmentIdentifier: lecturer.lectureId || lecturer.lecturerCode,
      }));
    setLecturers(assignableLecturers);

    try {
      setAssignments(await ResourceService.getAll(API_ENDPOINTS.supervisors.list));
    } catch (assignmentError) {
      // Tương thích trong lúc backend cũ chưa có API danh sách tổng: lấy phân công theo từng đề tài.
      const assignmentResults = await Promise.allSettled(
        topicData.map((topic) => ResourceService.getAll(API_ENDPOINTS.supervisors.byTopic(topic.topicId))),
      );
      setAssignments(assignmentResults.flatMap((result) => result.status === 'fulfilled' ? result.value : []));
      if (!topicData.length || assignmentResults.every((result) => result.status === 'rejected')) {
        setDataWarning(getApiErrorMessage(assignmentError, 'Không thể tải danh sách phân công hiện tại.'));
      }
    }

    const warnings = [];
    if (topicResult.status === 'rejected') warnings.push('Không tải được danh sách đề tài.');
    if (lecturerResult.status === 'rejected') warnings.push('Không tải được danh sách giảng viên.');
    else if (activeLecturers.length && !assignableLecturers.length) warnings.push('Danh sách giảng viên chưa có mã hợp lệ để phân công.');
    if (warnings.length) setDataWarning((current) => [current, ...warnings].filter(Boolean).join(' '));

    if (topicResult.status === 'rejected' && lecturerResult.status === 'rejected') {
      setError('Không thể tải đề tài và giảng viên. Vui lòng kiểm tra kết nối backend.');
    }
    setLoading(false);
  };
  useEffect(() => { load(); }, []); // eslint-disable-line react-hooks/exhaustive-deps
  useEffect(() => { setPage(0); }, [query]);

  const filtered = useMemo(() => { const keyword = normalize(query); return assignments.filter((item) => normalize(`${item.topicTitle} ${item.lectureName} ${item.lectureCode} ${statusLabel(item.status)}`).includes(keyword)); }, [assignments, query]);
  const pageSize = 10; const totalPages = Math.max(1, Math.ceil(filtered.length / pageSize)); const visible = filtered.slice(page * pageSize, page * pageSize + pageSize);
  const openCreate = () => { setForm(emptyForm); setFormError(''); setModal({ mode: 'create' }); };
  const requestEdit = (item) => setConfirm({ type: 'edit', item });
  const openEdit = (item) => { setForm({ topicId: item.topicId, lectureId: item.lectureId, note: item.note || '' }); setFormError(''); setModal({ mode: 'edit', item }); };
  const submit = async (event) => { event.preventDefault(); if (modal.mode === 'create' && (!form.topicId || !form.lectureId)) { setFormError('Vui lòng chọn đầy đủ đề tài và giảng viên hướng dẫn.'); return; } setSaving(true); setFormError(''); try { if (modal.mode === 'create') await ResourceService.create(API_ENDPOINTS.supervisors.assign(form.topicId), { lectureId: form.lectureId, role: 'PRIMARY', note: form.note.trim() || null }); else await ResourceService.update(API_ENDPOINTS.supervisors.update(modal.item.assignmentId), { role: 'PRIMARY', note: form.note.trim() || null }, 'patch'); setModal(null); toast.success(modal.mode === 'create' ? 'Phân công giảng viên hướng dẫn thành công.' : 'Cập nhật phân công thành công.'); await load(); } catch (requestError) { const message = getApiErrorMessage(requestError, 'Không thể lưu phân công hướng dẫn.'); setFormError(message); toast.error(message, { title: 'Phân công thất bại' }); } finally { setSaving(false); } };
  const deactivate = async () => { const reason = confirm?.reason?.trim(); if (!reason) return; setSaving(true); try { await ResourceService.update(API_ENDPOINTS.supervisors.deactivate(confirm.item.assignmentId), { reason }, 'patch'); setConfirm(null); toast.success('Đã ngừng phân công hướng dẫn.'); await load(); } catch (requestError) { toast.error(getApiErrorMessage(requestError, 'Không thể ngừng phân công.')); } finally { setSaving(false); } };
  const handleConfirm = () => { if (confirm.type === 'edit') { const item = confirm.item; setConfirm(null); openEdit(item); } else deactivate(); };
  const openDetail = async (item) => {
    setDetail({ assignment: item, loading: true, topic: null, team: null, lecturer: null, error: '' });
    try {
      const topic = await ResourceService.getOne(API_ENDPOINTS.topics.detail(item.topicId));
      const lecturer = lecturers.find((candidate) =>
        String(candidate.lectureId || '') === String(item.lectureId || '')
        || String(candidate.lecturerCode || '') === String(item.lectureCode || '')) || null;
      const team = topic?.teamId ? await ResourceService.getOne(API_ENDPOINTS.teams.detail(topic.teamId)) : null;
      setDetail({ assignment: item, loading: false, topic, team, lecturer, error: '' });
    } catch (requestError) {
      setDetail((current) => ({ ...current, loading: false, error: getApiErrorMessage(requestError, 'Không thể tải chi tiết phân công.') }));
    }
  };

  return <div className="page-stack supervision-page">
    <section className="page-title-row"><div><span className="page-kicker">PHÂN CÔNG ĐỒ ÁN</span><h2>Phân công giảng viên hướng dẫn</h2><p>Gán một giảng viên hướng dẫn cho mỗi đề tài đã được phê duyệt hoặc đã gán nhóm.</p></div><button className="primary-button" onClick={openCreate}><Plus size={18} /> Phân công mới</button></section>
    <section className="supervision-summary"><article><UserRoundCheck size={20} /><div><span>Đang hoạt động</span><strong>{assignments.filter((item) => item.status === 'ACTIVE').length}</strong></div></article><article><span>Đề tài có thể phân công</span><strong>{topics.length}</strong></article><article><span>Giảng viên hoạt động</span><strong>{lecturers.length}</strong></article></section>
    {dataWarning && <div className="supervision-warning"><AlertCircle size={17} /><span>{dataWarning}</span><button onClick={load}>Tải lại</button></div>}
    <section className="panel data-panel"><div className="table-toolbar"><label className="table-search"><Search size={18} /><input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Tìm theo đề tài, tên hoặc mã giảng viên..." />{query && <button className="search-clear" onClick={() => setQuery('')}><X size={15} /></button>}</label></div>
      {loading ? <State icon={LoaderCircle} text="Đang tải danh sách phân công..." spin /> : error ? <State icon={AlertCircle} text={error} action={load} /> : visible.length === 0 ? <State icon={Inbox} text={query ? 'Không tìm thấy phân công phù hợp.' : 'Chưa có phân công giảng viên hướng dẫn.'} /> : <><div className="table-scroll"><table><thead><tr><th>Đề tài</th><th>Giảng viên hướng dẫn</th><th>Ngày phân công</th><th>Trạng thái</th><th /></tr></thead><tbody>{visible.map((item) => <tr key={item.assignmentId}><td data-label="Đề tài"><strong>{item.topicTitle}</strong><small>Đề tài #{item.topicId}</small></td><td data-label="Giảng viên hướng dẫn"><strong>{item.lectureName}</strong><small>{item.lectureCode}</small></td><td data-label="Ngày phân công">{formatDate(item.assignedAt)}</td><td data-label="Trạng thái"><span className={`status-badge ${item.status === 'ACTIVE' ? 'success' : 'danger'}`}>{statusLabel(item.status)}</span></td><td className="table-actions"><div className="inline-actions"><button onClick={() => openDetail(item)} title="Xem chi tiết phân công"><Eye size={15} /></button>{item.status === 'ACTIVE' && <><button onClick={() => requestEdit(item)} title="Sửa phân công"><Edit3 size={15} /></button><button className="danger" onClick={() => setConfirm({ type: 'deactivate', item, reason: '' })} title="Ngừng phân công"><UserMinus size={15} /></button></>}</div></td></tr>)}</tbody></table></div><div className="table-footer"><span>Hiển thị {visible.length} trong tổng số {filtered.length} phân công</span><div><button disabled={page === 0} onClick={() => setPage((value) => value - 1)}>Trước</button><button className="active">{page + 1}</button><button disabled={page >= totalPages - 1} onClick={() => setPage((value) => value + 1)}>Sau</button></div></div></>}
    </section>
    {modal && <div className="modal-backdrop" onMouseDown={(event) => event.target === event.currentTarget && !saving && setModal(null)}><section className="topic-modal supervision-modal" role="dialog" aria-modal="true"><header><div><span>{modal.mode === 'create' ? 'PHÂN CÔNG MỚI' : 'CẬP NHẬT PHÂN CÔNG'}</span><h3>Giảng viên hướng dẫn</h3></div><button disabled={saving} onClick={() => setModal(null)}><X size={20} /></button></header><form onSubmit={submit}>{modal.mode === 'create' && <><SearchChoice label="Đề tài có thể phân công" value={form.topicId} items={topics} valueKey="topicId" labelKey="title" secondaryKey="technology" placeholder="Tìm và chọn đề tài..." emptyText="Chưa có đề tài đã phê duyệt hoặc đã gán nhóm." onChange={(value) => setForm({ ...form, topicId: value })} /><SearchChoice label="Giảng viên hướng dẫn" value={form.lectureId} items={lecturers} valueKey="assignmentIdentifier" labelKey="fullName" secondaryKey="lecturerCode" placeholder="Tìm theo tên hoặc mã giảng viên..." emptyText="Không có giảng viên hoạt động có thể phân công." onChange={(value) => setForm({ ...form, lectureId: value })} /></>}<label>Ghi chú<textarea value={form.note} onChange={(event) => setForm({ ...form, note: event.target.value })} placeholder="Nội dung cần lưu ý cho phân công này..." maxLength={1000} /></label>{formError && <div className="form-error"><AlertCircle size={16} />{formError}</div>}<footer><button type="button" className="secondary-button" disabled={saving} onClick={() => setModal(null)}>Hủy</button><button className="primary-button" disabled={saving || (modal.mode === 'create' && (!topics.length || !lecturers.length))}>{saving ? 'Đang lưu...' : 'Lưu phân công'}</button></footer></form></section></div>}
    {confirm?.type === 'deactivate' ? <div className="modal-backdrop" onMouseDown={(event) => event.target === event.currentTarget && !saving && setConfirm(null)}><section className="confirm-modal delete supervisor-stop-modal"><button className="confirm-close" onClick={() => setConfirm(null)}><X size={19} /></button><div className="confirm-icon"><UserMinus size={24} /></div><span>NGỪNG PHÂN CÔNG</span><h3>Xác nhận ngừng hướng dẫn?</h3><p>Giảng viên <strong>{confirm.item.lectureName}</strong> sẽ không còn phụ trách đề tài “{confirm.item.topicTitle}”.</p><label>Lý do <b>*</b><textarea autoFocus value={confirm.reason} onChange={(event) => setConfirm({ ...confirm, reason: event.target.value })} placeholder="Nhập lý do ngừng phân công..." /></label><div className="confirm-actions"><button className="secondary-button" onClick={() => setConfirm(null)}>Hủy</button><button className="primary-button" disabled={saving || !confirm.reason.trim()} onClick={deactivate}>{saving ? 'Đang xử lý...' : 'Ngừng phân công'}</button></div></section></div> : <ConfirmModal open={Boolean(confirm)} type="edit" title="Chỉnh sửa phân công?" message={`Bạn sắp thay đổi vai trò hoặc ghi chú phân công của “${confirm?.item?.lectureName || ''}”.`} confirmLabel="Tiếp tục sửa" loading={saving} onCancel={() => setConfirm(null)} onConfirm={handleConfirm} />}
    {detail && <SupervisorDetailModal detail={detail} onClose={() => setDetail(null)} />}
  </div>;
}

function SupervisorDetailModal({ detail, onClose }) {
  const { assignment, topic, team, lecturer, loading, error } = detail;
  const students = Array.isArray(team?.students) ? team.students : [];
  return <div className="modal-backdrop" onMouseDown={(event) => event.target === event.currentTarget && !loading && onClose()}><section className="supervisor-detail-modal" role="dialog" aria-modal="true"><header><div><span>CHI TIẾT PHÂN CÔNG HƯỚNG DẪN</span><h3>{assignment.topicTitle}</h3></div><button disabled={loading} onClick={onClose} aria-label="Đóng"><X size={20} /></button></header>{loading ? <State icon={LoaderCircle} text="Đang tổng hợp thông tin phân công..." spin /> : error ? <State icon={AlertCircle} text={error} /> : <div className="supervisor-detail-body"><section className="assignment-overview"><article><span>Vai trò</span><strong>{roleLabel(assignment.role)}</strong></article><article><span>Trạng thái</span><strong>{statusLabel(assignment.status)}</strong></article><article><span>Ngày phân công</span><strong>{formatDate(assignment.assignedAt)}</strong></article></section><section className="detail-card topic-information"><div className="detail-card-title"><BookOpen size={19} /><div><span>ĐỀ TÀI</span><h4>{topic?.title || assignment.topicTitle}</h4></div></div><div className="topic-detail-grid"><div><span>Mô tả</span><p>{topic?.description || 'Chưa có mô tả.'}</p></div><div><span>Mục tiêu</span><p>{topic?.objective || 'Chưa cập nhật mục tiêu.'}</p></div><div><span>Công nghệ</span><strong>{topic?.technology || 'Chưa cập nhật'}</strong></div><div><span>Đợt bảo vệ</span><strong>{topic?.defensePeriodName || 'Chưa xác định'}</strong></div></div></section><section className="detail-card lecturer-information"><div className="detail-card-title"><GraduationCap size={19} /><div><span>GIẢNG VIÊN HƯỚNG DẪN</span><h4>{lecturer?.fullName || assignment.lectureName}</h4></div></div><div className="lecturer-profile-grid"><div><span>Mã giảng viên</span><strong>{lecturer?.lecturerCode || assignment.lectureCode || '—'}</strong></div><div><span>Học vị</span><strong>{lecturer?.degree || 'Chưa cập nhật'}</strong></div><div><Mail size={15} /><span>{lecturer?.email || 'Chưa cập nhật email'}</span></div><div><Phone size={15} /><span>{lecturer?.phone || 'Chưa cập nhật số điện thoại'}</span></div></div>{assignment.note && <p className="assignment-note"><strong>Ghi chú:</strong> {assignment.note}</p>}</section><section className="detail-card student-information"><div className="detail-card-title"><Users size={19} /><div><span>SINH VIÊN ĐƯỢC HƯỚNG DẪN</span><h4>{team?.nameTeam || 'Đề tài chưa được gán nhóm'} · {students.length} sinh viên</h4></div></div>{students.length ? <div className="supervised-student-list">{students.map((student, index) => <article key={student.studentCode || index}><span className="student-number">{index + 1}</span><div><strong>{student.fullName || 'Chưa cập nhật họ tên'}</strong><small>{student.studentCode || 'Chưa có mã sinh viên'}</small></div><div><span>{student.classCode || 'Chưa xếp lớp'}</span><small>{student.email || 'Chưa có email'}</small></div></article>)}</div> : <div className="detail-empty"><Users size={22} /><span>Chưa có sinh viên thuộc đề tài này.</span></div>}</section></div>}<footer><button className="primary-button" onClick={onClose} disabled={loading}>Đóng</button></footer></section></div>;
}

function SearchChoice({ label, value, items, valueKey, labelKey, secondaryKey, placeholder, emptyText, onChange }) { const selected = items.find((item) => String(item[valueKey]) === String(value)); const [query, setQuery] = useState(selected ? selected[labelKey] : ''); const [open, setOpen] = useState(false); const filtered = items.filter((item) => normalize(`${item[labelKey]} ${item[secondaryKey] || ''}`).includes(normalize(query))); const choose = (item) => { onChange(item[valueKey]); setQuery(item[labelKey]); setOpen(false); }; return <label className="supervision-choice">{label} <b>*</b><div className={`searchable-select ${open ? 'open' : ''}`}><Search size={16} /><input value={query} placeholder={placeholder} onFocus={() => setOpen(true)} onChange={(event) => { setQuery(event.target.value); onChange(''); setOpen(true); }} /><button type="button" className="select-toggle" onClick={() => setOpen((current) => !current)} aria-label={`Mở danh sách ${label}`}><ChevronDown size={17} /></button>{open && <div className="searchable-options">{filtered.length ? filtered.map((item) => <button type="button" key={item[valueKey]} className={String(value) === String(item[valueKey]) ? 'selected' : ''} onClick={() => choose(item)}><span><strong>{item[labelKey]}</strong><small>{item[secondaryKey]}</small></span>{String(value) === String(item[valueKey]) && <Check size={16} />}</button>) : <span className="option-message">{items.length ? 'Không tìm thấy dữ liệu phù hợp.' : emptyText}</span>}</div>}</div></label>; }
function State({ icon: Icon, text, action, spin }) { return <div className="data-state"><Icon className={spin ? 'spin' : ''} size={28} /><strong>{text}</strong>{action && <button onClick={action}>Thử lại</button>}</div>; }
function roleLabel() { return 'Giảng viên hướng dẫn'; }
function statusLabel(value) { return value === 'ACTIVE' ? 'Đang hoạt động' : value === 'INACTIVE' ? 'Đã ngừng' : value || '—'; }
function normalize(value) { return String(value || '').normalize('NFD').replace(/[\u0300-\u036f]/g, '').replace(/đ/g, 'd').replace(/Đ/g, 'D').toLowerCase().trim(); }
function formatDate(value) { if (!value) return '—'; return new Intl.DateTimeFormat('vi-VN', { dateStyle: 'short', timeStyle: 'short' }).format(new Date(value)); }
