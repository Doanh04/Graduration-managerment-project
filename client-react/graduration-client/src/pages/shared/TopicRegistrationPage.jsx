import React, { useEffect, useMemo, useState } from 'react';
import { AlertCircle, BookOpen, Check, Clock3, Download, Eye, FileText, LoaderCircle, Plus, Search, Send, X, XCircle } from 'lucide-react';
import API_ENDPOINTS from '../../config/endpoints.js';
import httpClient from '../../config/HttpClient.jsx';
import ResourceService from '../../services/ResourceService.jsx';
import { useToast } from '../../context/ToastContext.jsx';
import getApiErrorMessage from '../../utils/apiError.js';
import { formatDateTimeDisplay } from '../../utils/dateFormat.js';
import '../../style/TopicRegistration.scss';
import '../../style/TopicRegistrationDetail.scss';

const STATUS = { PENDING: 'Chờ duyệt', APPROVED: 'Đã duyệt', REJECTED: 'Từ chối', CANCELLED: 'Đã hủy' };
const emptyProposal = { title: '', description: '', objective: '', technology: '', defensePeriodId: '' };

export default function TopicRegistrationPage({ mode = 'student' }) {
  const student = mode === 'student';
  const toast = useToast();
  const [topics, setTopics] = useState([]);
  const [registrations, setRegistrations] = useState([]);
  const [proposals, setProposals] = useState([]);
  const [team, setTeam] = useState(null);
  const [periods, setPeriods] = useState([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [query, setQuery] = useState('');
  const [status, setStatus] = useState(student ? '' : 'PENDING');
  const [modal, setModal] = useState(null);
  const [proposal, setProposal] = useState(emptyProposal);
  const [note, setNote] = useState('');
  const [reason, setReason] = useState('');
  const [error, setError] = useState('');
  const [creatorNameMap, setCreatorNameMap] = useState(() => new Map());

  const load = async () => {
    setLoading(true); setError('');
    try {
      if (student) {
        const [myTeam, available, mine, myProposals, activePeriods] = await Promise.all([
          ResourceService.getOne(API_ENDPOINTS.teams.mine),
          ResourceService.getAll(API_ENDPOINTS.topics.list, { status: 'APPROVED' }),
          ResourceService.getOne(API_ENDPOINTS.topicRegistrations.mine),
          ResourceService.getOne(API_ENDPOINTS.topics.mine),
          ResourceService.getAll(API_ENDPOINTS.defensePeriods.list),
        ]);
        setTeam(myTeam); setTopics(available.filter((item) => !item.teamId));
        setRegistrations(Array.isArray(mine) ? mine : []);
        setProposals(Array.isArray(myProposals) ? myProposals : []);
        setPeriods(activePeriods.filter((item) => item.status !== 'FINISHED'));
      } else {
        const proposalStatus = status === 'PENDING' ? 'PENDING_APPROVAL' : status === 'APPROVED' ? 'APPROVED' : status === 'REJECTED' ? 'REJECTED' : null;
        const [topicRegistrations, proposalTopics, lecturerResult, studentResult, userRoleResult] = await Promise.all([
          ResourceService.getAll(API_ENDPOINTS.topicRegistrations.list, status ? { status } : {}),
          status === 'CANCELLED' ? [] : ResourceService.getAll(API_ENDPOINTS.topics.list, { ...(proposalStatus && { status: proposalStatus }) }),
          // Tên người tạo có thể chưa được lưu trong các topic cũ; dùng hồ sơ hiện tại để
          // ánh xạ UUID/user name/mã hồ sơ mà không làm thay đổi dữ liệu trong database.
          ResourceService.getAll(API_ENDPOINTS.lecturers.list, { size: 500 }).catch(() => []),
          ResourceService.getAll(API_ENDPOINTS.students.list, { size: 500 }).catch(() => []),
          // Endpoint này trả về userId và họ tên của cả hồ sơ sinh viên/giảng viên,
          // dùng làm nguồn dự phòng cho các đề tài cũ chỉ còn lưu UUID người tạo.
          ResourceService.getAll(API_ENDPOINTS.userRoles.list).catch(() => []),
        ]);
        const names = buildCreatorNameMap([...lecturerResult, ...studentResult, ...userRoleResult]);
        setCreatorNameMap(names);
        const proposalsForReview = proposalTopics
          .filter((item) => ['STUDENT', 'LECTURER'].includes(item.categoryTopic) && item.status !== 'DRAFT')
          .map((item) => normalizeProposalForReview(item, names));
        // Đề xuất do giảng viên tạo có một topic_registration nội bộ để liên
        // kết nhóm với đề tài. Hàng đó không phải một yêu cầu độc lập; nếu
        // hiển thị cùng topic proposal sẽ khiến Admin thấy cùng đề xuất hai lần.
        const proposalTopicIds = new Set(proposalsForReview.map((item) => String(item.topicId)));
        const standaloneRegistrations = topicRegistrations
          .filter((item) => !proposalTopicIds.has(String(item.topicId)))
          .map((item) => ({ ...item, requestKind: 'REGISTRATION' }));
        setRegistrations([...standaloneRegistrations, ...proposalsForReview]);
      }
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, student ? 'Không thể tải dữ liệu đăng ký đề tài.' : 'Không thể tải danh sách chờ duyệt.'));
    } finally { setLoading(false); }
  };
  useEffect(() => { load(); }, [mode, status]); // eslint-disable-line react-hooks/exhaustive-deps

  const pending = registrations.find((item) => item.status === 'PENDING');
  const pendingProposal = proposals.find((item) => ['DRAFT', 'PENDING_APPROVAL'].includes(item.status));
  const blocked = Boolean(team?.topicId || pending || pendingProposal);
  const filteredTopics = useMemo(() => topics.filter((item) => `${item.title} ${item.technology || ''} ${item.defensePeriodName || ''}`.toLowerCase().includes(query.toLowerCase())), [topics, query]);
  const filteredRegistrations = useMemo(() => registrations.filter((item) => `${item.topicTitle} ${item.teamName} ${item.studentName} ${item.studentCode}`.toLowerCase().includes(query.toLowerCase())), [registrations, query]);

  const register = async (topic) => {
    setSaving(true); setError('');
    try {
      await ResourceService.create(API_ENDPOINTS.topicRegistrations.create, { topicId: topic.topicId, priority: 1, note: note.trim() || null });
      toast.success('Đã gửi đăng ký đề tài đến Ban quản lý khoa.'); setModal(null); setNote(''); await load();
    } catch (requestError) { setError(getApiErrorMessage(requestError, 'Không thể gửi đăng ký đề tài.')); }
    finally { setSaving(false); }
  };

  const propose = async (event) => {
    event.preventDefault(); setSaving(true); setError('');
    try {
      const response = await ResourceService.create(API_ENDPOINTS.topics.create, { ...proposal, categoryTopic: 'STUDENT', defensePeriodId: Number(proposal.defensePeriodId) });
      const created = response?.result ?? response;
      await ResourceService.create(API_ENDPOINTS.topics.submit(created.topicId), null);
      toast.success('Đã gửi đề xuất đề tài để xét duyệt.'); setModal(null); setProposal(emptyProposal); await load();
    } catch (requestError) { setError(getApiErrorMessage(requestError, 'Không thể gửi đề xuất đề tài.')); }
    finally { setSaving(false); }
  };

  const cancel = async (item) => {
    setSaving(true);
    try { await ResourceService.update(API_ENDPOINTS.topicRegistrations.cancel(item.registrationId), null, 'patch'); toast.success('Đã hủy đăng ký đề tài.'); await load(); }
    catch (requestError) { setError(getApiErrorMessage(requestError, 'Không thể hủy đăng ký.')); }
    finally { setSaving(false); }
  };

  const approve = async (item) => {
    setSaving(true); setError('');
    try { if (item.requestKind === 'PROPOSAL') await ResourceService.create(API_ENDPOINTS.topics.approve(item.topicId), null); else await ResourceService.update(API_ENDPOINTS.topicRegistrations.approve(item.registrationId), null, 'patch'); toast.success('Đã duyệt và gán đề tài cho nhóm.'); await load(); }
    catch (requestError) { setError(getApiErrorMessage(requestError, 'Không thể duyệt đăng ký.')); }
    finally { setSaving(false); }
  };

  const reject = async () => {
    if (!reason.trim()) return;
    setSaving(true); setError('');
    try { if (modal.item.requestKind === 'PROPOSAL') await ResourceService.create(API_ENDPOINTS.topics.reject(modal.item.topicId), { reason: reason.trim() }); else await ResourceService.update(API_ENDPOINTS.topicRegistrations.reject(modal.item.registrationId), { reason: reason.trim() }, 'patch'); toast.success('Đã từ chối yêu cầu đề tài.'); setModal(null); setReason(''); await load(); }
    catch (requestError) { setError(getApiErrorMessage(requestError, 'Không thể từ chối đăng ký.')); }
    finally { setSaving(false); }
  };

  const viewDetail = async (item) => {
    setSaving(true); setError('');
    try {
      const topic = await ResourceService.getOne(API_ENDPOINTS.topics.detail(item.topicId));
      // Luôn chuẩn hóa tên trước khi mở modal: API cũ có thể chỉ trả UUID ở
      // createdBy, trong khi danh sách hồ sơ đã được tải ở bước load().
      const normalizedTopic = {
        ...topic,
        createdByName: displayCreatorName(topic, item, creatorNameMap),
      };
      setModal({ type: 'detail', item, topic: normalizedTopic });
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, 'Không thể tải chi tiết đề tài.'));
    } finally { setSaving(false); }
  };

  return <div className="topic-registration-page">
    <div className="page-heading"><div><span>{student ? 'ĐĂNG KÝ ĐỒ ÁN' : 'XÉT DUYỆT ĐỀ TÀI'}</span><h1>{student ? 'Lựa chọn và đề xuất đề tài' : 'Duyệt đăng ký đề tài'}</h1><p>{student ? 'Đăng ký đề tài có sẵn hoặc gửi đề xuất mới khi nhóm chưa có đề tài.' : 'Xét duyệt yêu cầu và gán đề tài cho nhóm sinh viên.'}</p></div>{student && !blocked && <button className="primary-button" onClick={() => { setModal({ type: 'proposal' }); setError(''); }}><Plus size={16}/> Đề xuất đề tài mới</button>}</div>
    {student && team?.topicId && <div className="registration-banner success"><Check size={19}/><div><strong>Nhóm đã có đề tài</strong><span>{team.topicTitle}. Bạn không thể đăng ký hoặc đề xuất thêm đề tài.</span></div></div>}
    {student && pending && !team?.topicId && <div className="registration-banner pending"><Clock3 size={19}/><div><strong>Đang chờ duyệt: {pending.topicTitle}</strong><span>Trong lúc chờ xét duyệt, nhóm không thể gửi thêm đăng ký khác.</span></div><button disabled={saving} onClick={() => cancel(pending)}>Hủy đăng ký</button></div>}
    {student && pendingProposal && !team?.topicId && <div className="registration-banner pending"><Clock3 size={19}/><div><strong>Đề xuất đang chờ duyệt: {pendingProposal.title}</strong><span>Khi được duyệt, đề tài sẽ tự động được gán cho nhóm {team?.nameTeam}.</span></div></div>}
    {error && <div className="workflow-error"><AlertCircle size={16}/>{error}</div>}
    <section className="registration-panel"><div className="registration-toolbar"><label><Search size={17}/><input value={query} onChange={(event) => setQuery(event.target.value)} placeholder={student ? 'Tìm theo tên đề tài, công nghệ hoặc đợt bảo vệ...' : 'Tìm theo đề tài, nhóm hoặc sinh viên...'}/></label>{!student && <select value={status} onChange={(event) => setStatus(event.target.value)}><option value="">Tất cả trạng thái</option>{Object.entries(STATUS).map(([value, label]) => <option value={value} key={value}>{label}</option>)}</select>}</div>
      {loading ? <RegistrationState/> : student ? <div className="available-topic-grid">{filteredTopics.length ? filteredTopics.map((topic) => <article key={topic.topicId}><span>{topic.defensePeriodName}</span><h3>{topic.title}</h3><p>{topic.description || 'Không có mô tả.'}</p><small>{topic.technology || 'Chưa cập nhật công nghệ'}</small><footer><button disabled={blocked || saving} onClick={() => { setModal({ type: 'register', item: topic }); setNote(''); setError(''); }}><Send size={14}/> {team?.topicId ? 'Nhóm đã có đề tài' : pending ? 'Đang chờ duyệt' : 'Đăng ký đề tài'}</button></footer></article>) : <RegistrationEmpty text="Không có đề tài phù hợp để đăng ký."/>}</div> : <RegistrationTable items={filteredRegistrations} creatorNameMap={creatorNameMap} saving={saving} onView={viewDetail} onApprove={approve} onReject={(item) => { setModal({ type: 'reject', item }); setReason(''); setError(''); }}/>}
    </section>
    {modal?.type === 'register' && <div className="modal-backdrop"><section className="registration-modal"><ModalHeader eyebrow="ĐĂNG KÝ ĐỀ TÀI" title={modal.item.title} onClose={() => setModal(null)}/><div className="registration-modal-body"><p>Đăng ký cho nhóm <strong>{team?.nameTeam}</strong>. Ban quản lý khoa sẽ kiểm tra trước khi gán đề tài.</p><label>Ghi chú<textarea value={note} onChange={(event) => setNote(event.target.value)} placeholder="Nguyện vọng hoặc thông tin bổ sung..."/></label>{error && <div className="workflow-error"><AlertCircle size={16}/>{error}</div>}</div><ModalFooter saving={saving} confirm="Gửi đăng ký" onCancel={() => setModal(null)} onConfirm={() => register(modal.item)}/></section></div>}
    {modal?.type === 'proposal' && <div className="modal-backdrop"><section className="registration-modal"><ModalHeader eyebrow="ĐỀ XUẤT MỚI" title="Đề xuất đề tài sinh viên" onClose={() => setModal(null)}/><form onSubmit={propose}><label>Tên đề tài<b>*</b><input required value={proposal.title} onChange={(e) => setProposal({...proposal,title:e.target.value})}/></label><label>Đợt bảo vệ<b>*</b><select required value={proposal.defensePeriodId} onChange={(e) => setProposal({...proposal,defensePeriodId:e.target.value})}><option value="">Chọn đợt bảo vệ</option>{periods.map((period) => <option key={period.defensePeriodId} value={period.defensePeriodId}>{period.periodName} · {period.academicYear}</option>)}</select></label><label>Mô tả<textarea value={proposal.description} onChange={(e) => setProposal({...proposal,description:e.target.value})}/></label><label>Mục tiêu<textarea value={proposal.objective} onChange={(e) => setProposal({...proposal,objective:e.target.value})}/></label><label>Công nghệ<input value={proposal.technology} onChange={(e) => setProposal({...proposal,technology:e.target.value})}/></label>{error && <div className="workflow-error"><AlertCircle size={16}/>{error}</div>}<footer><button type="button" className="secondary-button" onClick={() => setModal(null)}>Hủy</button><button className="primary-button" disabled={saving}>{saving ? 'Đang gửi...' : 'Gửi đề xuất'}</button></footer></form></section></div>}
    {modal?.type === 'reject' && <div className="modal-backdrop"><section className="registration-modal"><ModalHeader eyebrow="TỪ CHỐI ĐĂNG KÝ" title={modal.item.topicTitle} onClose={() => setModal(null)}/><div className="registration-modal-body"><p>Nhóm <strong>{modal.item.teamName}</strong> sẽ nhận được lý do từ chối này.</p><label>Lý do<b>*</b><textarea autoFocus value={reason} onChange={(event) => setReason(event.target.value)} placeholder="Nhập lý do từ chối..."/></label>{error && <div className="workflow-error"><AlertCircle size={16}/>{error}</div>}</div><ModalFooter saving={saving} disabled={!reason.trim()} confirm="Xác nhận từ chối" onCancel={() => setModal(null)} onConfirm={reject}/></section></div>}
    {modal?.type === 'detail' && <RegistrationDetail item={modal.item} topic={modal.topic} creatorNameMap={creatorNameMap} onClose={() => setModal(null)} />}
  </div>;
}

function RegistrationTable({ items, creatorNameMap, saving, onView, onApprove, onReject }) { return items.length ? <div className="registration-table"><table><thead><tr><th>Đề tài</th><th>Nhóm đăng ký</th><th>Giảng viên/Sinh viên đề xuất</th><th>Ngày gửi</th><th>Trạng thái</th><th/></tr></thead><tbody>{items.map((item) => { const creator = displayCreatorName(item, item, creatorNameMap); return <tr key={`${item.requestKind}-${item.registrationId}`}><td><strong>{item.topicTitle}</strong><small>{item.requestKind === 'PROPOSAL' ? (item.categoryTopic === 'LECTURER' ? 'Đề tài giảng viên đề xuất' : 'Đề tài sinh viên đề xuất') : item.technology || item.defensePeriodName}</small></td><td><strong>{item.teamName || 'Chưa xác định nhóm'}</strong><small>{item.note || 'Không có ghi chú'}</small></td><td>{creator}<small>{item.studentCode}</small></td><td>{formatDate(item.submittedAt)}</td><td><span className={`registration-status ${item.status.toLowerCase()}`}>{STATUS[item.status]}</span>{item.rejectionReason && <small>{item.rejectionReason}</small>}</td><td><div className="registration-actions"><button disabled={saving} className="view" onClick={() => onView(item)}><Eye size={14}/> Xem</button>{item.status === 'PENDING' && <><button disabled={saving} className="approve" onClick={() => onApprove(item)}><Check size={14}/> Duyệt</button><button disabled={saving} className="reject" onClick={() => onReject(item)}><XCircle size={14}/> Từ chối</button></>}</div></td></tr>; })}</tbody></table></div> : <RegistrationEmpty text="Không có đăng ký đề tài phù hợp."/>; }
function RegistrationDetail({ item, topic, creatorNameMap, onClose }) {
  // Chi tiết topic là nguồn tin cậy nhất; dữ liệu cũ có thể chỉ còn UUID trong item.
  const creator = displayCreatorName(topic, item, creatorNameMap);
  // Ưu tiên danh sách từ API chi tiết; nếu response cũ trả mảng rỗng thì
  // dùng dữ liệu đã có ở dòng danh sách để không làm mất sinh viên đề xuất.
  const proposedStudents = normalizeStudentList(
    Array.isArray(topic?.proposedStudents) && topic.proposedStudents.length
      ? topic.proposedStudents
      : item?.proposedStudents,
  );
  return <div className="modal-backdrop"><section className="registration-modal registration-detail-modal"><ModalHeader eyebrow="CHI TIẾT ĐĂNG KÝ ĐỀ TÀI" title={topic?.title || item.topicTitle} onClose={onClose}/><div className="registration-detail-body"><div className="registration-detail-summary"><DetailValue label="Trạng thái" value={STATUS[item.status] || item.status}/><DetailValue label="Nguồn đề xuất" value={item.requestKind === 'PROPOSAL' ? (item.categoryTopic === 'LECTURER' ? 'Giảng viên đề xuất' : 'Sinh viên đề xuất') : 'Đăng ký đề tài có sẵn'}/><DetailValue label="Đợt bảo vệ" value={topic?.defensePeriodName || item.defensePeriodName}/></div><section><h4><BookOpen size={17}/> Nội dung đề tài</h4><div className="registration-detail-grid"><DetailValue label="Mô tả" value={topic?.description}/><DetailValue label="Mục tiêu" value={topic?.objective}/><DetailValue label="Công nghệ sử dụng" value={topic?.technology || item.technology}/></div></section><TopicFileActions topic={topic}/><section><h4><span className="detail-people-icon">◎</span> Thông tin đăng ký</h4><div className="registration-detail-grid"><DetailValue label="Nhóm đăng ký" value={item.teamName}/><DetailValue label="Giảng viên/Sinh viên đề xuất" value={creator}/>{item.requestKind !== 'PROPOSAL' && <DetailValue label="Mã sinh viên" value={item.studentCode}/>}<DetailValue label="Giảng viên nguyện vọng" value={item.preferredSupervisorName}/><DetailValue label="Ngày gửi" value={formatDate(item.submittedAt)}/><DetailValue label="Ghi chú" value={item.note}/></div></section><section><h4><span className="detail-people-icon">◎</span> Sinh viên được đề xuất</h4>{proposedStudents.length ? <div className="proposed-students-list">{proposedStudents.map((student, index) => <div className="proposed-student-row" key={`${student.studentCode || student.id || 'student'}-${index}`}><div><strong>{student.fullName || 'Chưa cập nhật họ tên'}</strong><small>{student.studentCode || 'Chưa có mã sinh viên'}{student.classCode ? ` · ${student.classCode}` : ''}</small></div><small>{student.email || 'Chưa cập nhật email'}</small></div>)}</div> : <p className="detail-empty">Chưa có thông tin sinh viên được đề xuất.</p>}</section>{item.rejectionReason && <section><h4>Lý do từ chối</h4><p>{item.rejectionReason}</p></section>}</div><footer><button className="secondary-button" onClick={onClose}>Đóng</button></footer></section></div>;
}
function TopicFileActions({ topic }) {
  const [previewUrl, setPreviewUrl] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const isPdf = String(topic?.contentType || '').toLowerCase().includes('pdf') || /\.pdf$/i.test(topic?.fileName || '');

  useEffect(() => () => { if (previewUrl) URL.revokeObjectURL(previewUrl); }, [previewUrl]);
  if (!topic?.fileName || !topic?.topicId) return null;

  const fetchFile = async () => {
    const response = await httpClient.get(API_ENDPOINTS.topics.file(topic.topicId), { responseType: 'blob' });
    return response instanceof Blob ? response : new Blob([response], { type: topic.contentType || 'application/octet-stream' });
  };
  const preview = async () => {
    setLoading(true); setError('');
    try {
      const blob = await fetchFile();
      if (previewUrl) URL.revokeObjectURL(previewUrl);
      setPreviewUrl(URL.createObjectURL(new Blob([blob], { type: 'application/pdf' })));
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, 'Không thể xem trước tệp đề tài.'));
    } finally { setLoading(false); }
  };
  const download = async () => {
    setLoading(true); setError('');
    try {
      const blob = await fetchFile();
      const url = URL.createObjectURL(blob);
      const anchor = document.createElement('a'); anchor.href = url; anchor.download = topic.fileName; document.body.appendChild(anchor); anchor.click(); anchor.remove(); URL.revokeObjectURL(url);
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, 'Không thể tải tệp đề tài.'));
    } finally { setLoading(false); }
  };
  return <section className="registration-file-section"><h4><FileText size={17}/> Tệp mô tả đề tài</h4><div className="registration-file-row"><div className="registration-file-name"><strong>{topic.fileName}</strong><small>{topic.contentType || 'Tệp đính kèm'}{topic.fileSize ? ` · ${formatBytes(topic.fileSize)}` : ''}</small></div><div className="registration-file-actions"><button type="button" disabled={loading} onClick={isPdf ? preview : download}>{isPdf ? <Eye size={14}/> : <Download size={14}/>} {loading ? 'Đang xử lý...' : isPdf ? 'Xem trước' : 'Tải xuống'}</button>{previewUrl && <button type="button" className="file-close-preview" onClick={() => { URL.revokeObjectURL(previewUrl); setPreviewUrl(''); }}>Đóng xem trước</button>}</div></div>{error && <p className="registration-file-error">{error}</p>}{previewUrl && <div className="registration-file-preview"><iframe src={previewUrl} title={`Xem trước ${topic.fileName}`} /></div>}</section>;
}
function DetailValue({ label, value }) { return <div className="registration-detail-value"><span>{label}</span><strong>{value || 'Chưa cập nhật'}</strong></div>; }
function ModalHeader({ eyebrow, title, onClose }) { return <header><div><span>{eyebrow}</span><h3>{title}</h3></div><button onClick={onClose}><X size={19}/></button></header>; }
function ModalFooter({ saving, disabled, confirm, onCancel, onConfirm }) { return <footer><button className="secondary-button" disabled={saving} onClick={onCancel}>Hủy</button><button className="primary-button" disabled={saving || disabled} onClick={onConfirm}>{saving ? 'Đang xử lý...' : confirm}</button></footer>; }
function RegistrationState() { return <div className="registration-state"><LoaderCircle className="spin"/><strong>Đang tải dữ liệu...</strong></div>; }
function RegistrationEmpty({ text }) { return <div className="registration-state"><BookOpen/><strong>{text}</strong></div>; }
function formatDate(value) { return formatDateTimeDisplay(value); }
function formatBytes(bytes) { if (!bytes) return '0 B'; if (bytes < 1024 * 1024) return `${Math.ceil(bytes / 1024)} KB`; return `${(bytes / (1024 * 1024)).toFixed(1)} MB`; }
function normalizeProposalForReview(topic, creatorNameMap = new Map()) {
  const creatorName = displayCreatorName(topic, null, creatorNameMap);
  // Không lưu nhãn fallback vào trường tên: nhãn này sẽ chặn bước ánh xạ
  // UUID -> họ tên khi dòng đề xuất được render lại trong bảng/modal.
  const resolvedCreatorName = isUsableCreatorName(creatorName) ? creatorName : '';
  return { requestKind: 'PROPOSAL', registrationId: `proposal-${topic.topicId}`, topicId: topic.topicId, topicTitle: topic.title, categoryTopic: topic.categoryTopic, technology: topic.technology, teamId: topic.teamId, teamName: topic.teamName, creatorIdentifier: topic.createdBy, createdByName: resolvedCreatorName, studentName: resolvedCreatorName, studentCode: topic.categoryTopic === 'LECTURER' ? 'Giảng viên đề xuất' : 'Đề xuất đề tài mới', proposedStudents: normalizeStudentList(Array.isArray(topic.proposedStudents) && topic.proposedStudents.length ? topic.proposedStudents : topic.students), defensePeriodName: topic.defensePeriodName, submittedAt: topic.createdAt, status: topic.status === 'PENDING_APPROVAL' ? 'PENDING' : ['APPROVED', 'REGISTERED'].includes(topic.status) ? 'APPROVED' : topic.status, rejectionReason: topic.rejectionReason };
}

function normalizeStudentList(value) {
  const list = Array.isArray(value) ? value : [];
  return list.map((student) => ({
    id: student?.id || student?.studentId || student?.idStudent,
    studentCode: student?.studentCode || student?.code,
    fullName: student?.fullName || student?.fullNameStudent || student?.name,
    email: student?.email || student?.emailStudent,
    classCode: student?.classCode || student?.class?.classCode,
  }));
}

function normalizeIdentifier(value) {
  return value == null ? '' : String(value).trim().toLowerCase();
}

function looksLikeIdentifier(value) {
  // User/lecture/student identifiers are never suitable labels in the UI.
  // Keep this deliberately version-agnostic because old rows may contain a
  // UUID generated with a non-standard version nibble.
  return /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(String(value || '').trim());
}

const UNKNOWN_CREATOR_LABEL = 'Chưa xác định người đề xuất';

function isUsableCreatorName(value) {
  const text = String(value || '').trim();
  return Boolean(text)
    && !looksLikeIdentifier(text)
    && text.toLowerCase() !== UNKNOWN_CREATOR_LABEL.toLowerCase();
}

function firstDisplayName(values) {
  return values
    .flatMap((value) => Array.isArray(value) ? value : [value])
    .find((value) => typeof value === 'string' && isUsableCreatorName(value))?.trim() || '';
}

function creatorDisplayName(topic, item = null, creatorNameMap = new Map()) {
  // Chi tiết và dòng danh sách có thể chứa các trường khác nhau; gộp cả hai
  // để không bỏ sót định danh khi API chi tiết không trả lại createdBy.
  const sources = [topic, item].filter(Boolean);
  for (const source of sources) {
    // API phiên bản mới trả createdByName; các response cũ có thể dùng tên
    // tổng quát fullName/displayName. Chỉ nhận giá trị là tên, không nhận UUID.
    const direct = firstDisplayName([
      source.createdByName, source.creatorName, source.createdByDisplayName, source.creatorFullName,
      source.fullName, source.fullNameLecture, source.fullNameStudent, source.fullNameLecturer,
      source.displayName, source.name, extractCreatorName(source.createdByUser),
      extractCreatorName(source.creator), extractCreatorName(source.createdByProfile),
      extractCreatorName(source.createdBy),
    ]);
    if (direct) return direct;
  }
  // Một số phiên bản API đặt hồ sơ người tạo trong object lồng nhau; lấy cả
  // định danh của các object này để ánh xạ đúng tên từ danh sách người dùng.
  const relatedProfiles = sources.flatMap((source) => [
    source.createdByUser, source.creator, source.createdByProfile, source.createdBy,
  ]).filter((profile) => profile && typeof profile === 'object');
  const identifiers = sources.flatMap((source) => [
    source.createdBy, source.createdById, source.creatorUserId, source.creatorIdentifier,
    source.userId, source.idUser, source.lectureId, source.idLecture,
    source.studentId, source.idStudent, source.userName, source.studentName,
    source.lecturerCode, source.lectureCode, source.studentCode, source.userCode, source.code,
  ]).concat(relatedProfiles.flatMap((profile) => [
    profile.userId, profile.idUser, profile.id, profile.lectureId, profile.idLecture,
    profile.studentId, profile.idStudent, profile.userName, profile.userCode,
    profile.lecturerCode, profile.lectureCode, profile.studentCode, profile.code,
  ]));
  for (const identifier of identifiers) {
    const mapped = creatorNameMap.get(normalizeIdentifier(identifier));
    if (mapped && !looksLikeIdentifier(mapped)) return mapped;
  }
  for (const source of sources) {
    if (isUsableCreatorName(source.studentName)) return source.studentName.trim();
  }
  return UNKNOWN_CREATOR_LABEL;
}

// Lớp bảo vệ cuối cùng cho dữ liệu legacy: dù API nào trả về UUID ở trường
// tên người đề xuất thì giao diện vẫn hiển thị nhãn tiếng Việt thay vì UUID.
function displayCreatorName(topic, item = null, creatorNameMap = new Map()) {
  const name = creatorDisplayName(topic, item, creatorNameMap);
  return isUsableCreatorName(name) ? name : UNKNOWN_CREATOR_LABEL;
}

// Đọc tên từ các dạng DTO lồng nhau (createdByUser/creator/profile) mà không
// bao giờ trả UUID làm tên hiển thị.
function extractCreatorName(value) {
  if (!value || typeof value !== 'object') return '';
  return firstDisplayName([
    value.fullName, value.fullNameLecture, value.fullNameStudent, value.fullNameLecturer,
    value.lecturerName, value.studentFullName, value.displayName, value.name,
  ]);
}

function buildCreatorNameMap(users) {
  const names = new Map();
  const visit = (user, seen = new Set()) => {
    if (!user) return;
    if (typeof user !== 'object') return;
    if (seen.has(user)) return;
    seen.add(user);
    const profile = user.lecture || user.lecturer || user.student || user.studentEntity || user.userEntity
      || user.lectureEntity || user.createdByUser || user.creator || user.createdByProfile;
    // Prefer a real profile name even when an old API response incorrectly
    // places the UUID in `fullName`/`createdByName`.
    const name = firstDisplayName([
      user.fullName, user.fullNameLecture, user.fullNameStudent, user.fullNameLecturer,
      user.lecturerName, user.studentFullName, user.displayName, user.name,
      extractCreatorName(profile), profile?.fullNameLecture, profile?.fullNameStudent,
      profile?.fullName, profile?.name,
    ]);
    if (name) {
      [
        user.userId, user.idUser, user.id, user.lectureId, user.idLecture, user.studentId, user.idStudent,
        user.userName, user.userCode, user.lecturerCode, user.lectureCode, user.studentCode, user.code,
        profile?.userId, profile?.idUser, profile?.id, profile?.lectureId, profile?.idLecture,
        profile?.studentId, profile?.idStudent, profile?.lectureCode, profile?.lecturerCode,
        profile?.studentCode,
      ].forEach((identifier) => {
        const key = normalizeIdentifier(identifier);
        if (key) names.set(key, name);
      });
    }
    Object.values(user).forEach((nested) => {
      if (nested && typeof nested === 'object') {
        (Array.isArray(nested) ? nested : [nested]).forEach((entry) => visit(entry, seen));
      }
    });
  };
  (Array.isArray(users) ? users : [users]).forEach((user) => visit(user));
  return names;
}
