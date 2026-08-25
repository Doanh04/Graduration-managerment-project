import React, { useEffect, useMemo, useState } from 'react';
import { AlertCircle, BookOpen, Check, Clock3, Eye, LoaderCircle, Plus, Search, Send, X, XCircle } from 'lucide-react';
import API_ENDPOINTS from '../../config/endpoints.js';
import ResourceService from '../../services/ResourceService.jsx';
import { useToast } from '../../context/ToastContext.jsx';
import getApiErrorMessage from '../../utils/apiError.js';
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
        const proposalStatus = status === 'PENDING' ? 'PENDING_APPROVAL' : status === 'APPROVED' ? 'REGISTERED' : status === 'REJECTED' ? 'REJECTED' : null;
        const [topicRegistrations, studentProposals] = await Promise.all([
          ResourceService.getAll(API_ENDPOINTS.topicRegistrations.list, status ? { status } : {}),
          status === 'CANCELLED' ? [] : ResourceService.getAll(API_ENDPOINTS.topics.list, { categoryTopic: 'STUDENT', ...(proposalStatus && { status: proposalStatus }) }),
        ]);
        const proposalsForReview = studentProposals.filter((item) => item.status !== 'DRAFT').map(normalizeProposalForReview);
        setRegistrations([...topicRegistrations.map((item) => ({ ...item, requestKind: 'REGISTRATION' })), ...proposalsForReview]);
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
      setModal({ type: 'detail', item, topic });
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
      {loading ? <RegistrationState/> : student ? <div className="available-topic-grid">{filteredTopics.length ? filteredTopics.map((topic) => <article key={topic.topicId}><span>{topic.defensePeriodName}</span><h3>{topic.title}</h3><p>{topic.description || 'Không có mô tả.'}</p><small>{topic.technology || 'Chưa cập nhật công nghệ'}</small><footer><button disabled={blocked || saving} onClick={() => { setModal({ type: 'register', item: topic }); setNote(''); setError(''); }}><Send size={14}/> {team?.topicId ? 'Nhóm đã có đề tài' : pending ? 'Đang chờ duyệt' : 'Đăng ký đề tài'}</button></footer></article>) : <RegistrationEmpty text="Không có đề tài phù hợp để đăng ký."/>}</div> : <RegistrationTable items={filteredRegistrations} saving={saving} onView={viewDetail} onApprove={approve} onReject={(item) => { setModal({ type: 'reject', item }); setReason(''); setError(''); }}/>} 
    </section>
    {modal?.type === 'register' && <div className="modal-backdrop"><section className="registration-modal"><ModalHeader eyebrow="ĐĂNG KÝ ĐỀ TÀI" title={modal.item.title} onClose={() => setModal(null)}/><div className="registration-modal-body"><p>Đăng ký cho nhóm <strong>{team?.nameTeam}</strong>. Ban quản lý khoa sẽ kiểm tra trước khi gán đề tài.</p><label>Ghi chú<textarea value={note} onChange={(event) => setNote(event.target.value)} placeholder="Nguyện vọng hoặc thông tin bổ sung..."/></label>{error && <div className="workflow-error"><AlertCircle size={16}/>{error}</div>}</div><ModalFooter saving={saving} confirm="Gửi đăng ký" onCancel={() => setModal(null)} onConfirm={() => register(modal.item)}/></section></div>}
    {modal?.type === 'proposal' && <div className="modal-backdrop"><section className="registration-modal"><ModalHeader eyebrow="ĐỀ XUẤT MỚI" title="Đề xuất đề tài sinh viên" onClose={() => setModal(null)}/><form onSubmit={propose}><label>Tên đề tài<b>*</b><input required value={proposal.title} onChange={(e) => setProposal({...proposal,title:e.target.value})}/></label><label>Đợt bảo vệ<b>*</b><select required value={proposal.defensePeriodId} onChange={(e) => setProposal({...proposal,defensePeriodId:e.target.value})}><option value="">Chọn đợt bảo vệ</option>{periods.map((period) => <option key={period.defensePeriodId} value={period.defensePeriodId}>{period.periodName} · {period.academicYear}</option>)}</select></label><label>Mô tả<textarea value={proposal.description} onChange={(e) => setProposal({...proposal,description:e.target.value})}/></label><label>Mục tiêu<textarea value={proposal.objective} onChange={(e) => setProposal({...proposal,objective:e.target.value})}/></label><label>Công nghệ<input value={proposal.technology} onChange={(e) => setProposal({...proposal,technology:e.target.value})}/></label>{error && <div className="workflow-error"><AlertCircle size={16}/>{error}</div>}<footer><button type="button" className="secondary-button" onClick={() => setModal(null)}>Hủy</button><button className="primary-button" disabled={saving}>{saving ? 'Đang gửi...' : 'Gửi đề xuất'}</button></footer></form></section></div>}
    {modal?.type === 'reject' && <div className="modal-backdrop"><section className="registration-modal"><ModalHeader eyebrow="TỪ CHỐI ĐĂNG KÝ" title={modal.item.topicTitle} onClose={() => setModal(null)}/><div className="registration-modal-body"><p>Nhóm <strong>{modal.item.teamName}</strong> sẽ nhận được lý do từ chối này.</p><label>Lý do<b>*</b><textarea autoFocus value={reason} onChange={(event) => setReason(event.target.value)} placeholder="Nhập lý do từ chối..."/></label>{error && <div className="workflow-error"><AlertCircle size={16}/>{error}</div>}</div><ModalFooter saving={saving} disabled={!reason.trim()} confirm="Xác nhận từ chối" onCancel={() => setModal(null)} onConfirm={reject}/></section></div>}
    {modal?.type === 'detail' && <RegistrationDetail item={modal.item} topic={modal.topic} onClose={() => setModal(null)}/>} 
  </div>;
}

function RegistrationTable({ items, saving, onView, onApprove, onReject }) { return items.length ? <div className="registration-table"><table><thead><tr><th>Đề tài</th><th>Nhóm đăng ký</th><th>Sinh viên gửi</th><th>Ngày gửi</th><th>Trạng thái</th><th/></tr></thead><tbody>{items.map((item) => <tr key={`${item.requestKind}-${item.registrationId}`}><td><strong>{item.topicTitle}</strong><small>{item.requestKind === 'PROPOSAL' ? 'Đề tài sinh viên đề xuất' : item.technology || item.defensePeriodName}</small></td><td><strong>{item.teamName || 'Chưa xác định nhóm'}</strong><small>{item.note || 'Không có ghi chú'}</small></td><td>{item.studentName}<small>{item.studentCode}</small></td><td>{formatDate(item.submittedAt)}</td><td><span className={`registration-status ${item.status.toLowerCase()}`}>{STATUS[item.status]}</span>{item.rejectionReason && <small>{item.rejectionReason}</small>}</td><td><div className="registration-actions"><button disabled={saving} className="view" onClick={() => onView(item)}><Eye size={14}/> Xem</button>{item.status === 'PENDING' && <><button disabled={saving} className="approve" onClick={() => onApprove(item)}><Check size={14}/> Duyệt</button><button disabled={saving} className="reject" onClick={() => onReject(item)}><XCircle size={14}/> Từ chối</button></>}</div></td></tr>)}</tbody></table></div> : <RegistrationEmpty text="Không có đăng ký đề tài phù hợp."/>; }
function RegistrationDetail({ item, topic, onClose }) { return <div className="modal-backdrop"><section className="registration-modal registration-detail-modal"><ModalHeader eyebrow="CHI TIẾT ĐĂNG KÝ ĐỀ TÀI" title={topic?.title || item.topicTitle} onClose={onClose}/><div className="registration-detail-body"><div className="registration-detail-summary"><DetailValue label="Trạng thái" value={STATUS[item.status] || item.status}/><DetailValue label="Nguồn đề xuất" value={item.requestKind === 'PROPOSAL' ? 'Sinh viên đề xuất' : 'Đăng ký đề tài có sẵn'}/><DetailValue label="Đợt bảo vệ" value={topic?.defensePeriodName || item.defensePeriodName}/></div><section><h4><BookOpen size={17}/> Nội dung đề tài</h4><div className="registration-detail-grid"><DetailValue label="Mô tả" value={topic?.description}/><DetailValue label="Mục tiêu" value={topic?.objective}/><DetailValue label="Công nghệ sử dụng" value={topic?.technology || item.technology}/></div></section><section><h4><span className="detail-people-icon">◎</span> Thông tin đăng ký</h4><div className="registration-detail-grid"><DetailValue label="Nhóm đăng ký" value={item.teamName}/><DetailValue label="Sinh viên gửi" value={item.studentName}/><DetailValue label="Mã sinh viên" value={item.studentCode}/><DetailValue label="Giảng viên nguyện vọng" value={item.preferredSupervisorName}/><DetailValue label="Ngày gửi" value={formatDate(item.submittedAt)}/><DetailValue label="Ghi chú" value={item.note}/></div></section>{item.rejectionReason && <section><h4>Lý do từ chối</h4><p>{item.rejectionReason}</p></section>}</div><footer><button className="secondary-button" onClick={onClose}>Đóng</button></footer></section></div>; }
function DetailValue({ label, value }) { return <div className="registration-detail-value"><span>{label}</span><strong>{value || 'Chưa cập nhật'}</strong></div>; }
function ModalHeader({ eyebrow, title, onClose }) { return <header><div><span>{eyebrow}</span><h3>{title}</h3></div><button onClick={onClose}><X size={19}/></button></header>; }
function ModalFooter({ saving, disabled, confirm, onCancel, onConfirm }) { return <footer><button className="secondary-button" disabled={saving} onClick={onCancel}>Hủy</button><button className="primary-button" disabled={saving || disabled} onClick={onConfirm}>{saving ? 'Đang xử lý...' : confirm}</button></footer>; }
function RegistrationState() { return <div className="registration-state"><LoaderCircle className="spin"/><strong>Đang tải dữ liệu...</strong></div>; }
function RegistrationEmpty({ text }) { return <div className="registration-state"><BookOpen/><strong>{text}</strong></div>; }
function formatDate(value) { return value ? new Intl.DateTimeFormat('vi-VN', { dateStyle: 'short', timeStyle: 'short' }).format(new Date(value)) : '—'; }
function normalizeProposalForReview(topic) { return { requestKind: 'PROPOSAL', registrationId: `proposal-${topic.topicId}`, topicId: topic.topicId, topicTitle: topic.title, technology: topic.technology, teamId: topic.teamId, teamName: topic.teamName, studentName: topic.createdByName || topic.createdBy, studentCode: 'Đề xuất đề tài mới', defensePeriodName: topic.defensePeriodName, submittedAt: topic.createdAt, status: topic.status === 'PENDING_APPROVAL' ? 'PENDING' : topic.status === 'REGISTERED' ? 'APPROVED' : topic.status, rejectionReason: topic.rejectionReason }; }
