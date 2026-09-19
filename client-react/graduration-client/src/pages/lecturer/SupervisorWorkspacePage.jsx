import React, { useEffect, useMemo, useState } from 'react';
import { AlertCircle, BookOpen, CalendarDays, Check, CheckCircle2, ChevronDown, ClipboardCheck, Eye, FileText, Inbox, LoaderCircle, Plus, Search, Upload, Users, X } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import API_ENDPOINTS from '../../config/endpoints.js';
import httpClient from '../../config/HttpClient.jsx';
import ResourceService from '../../services/ResourceService.jsx';
import { useToast } from '../../context/ToastContext.jsx';
import getApiErrorMessage from '../../utils/apiError.js';
import { formatDateDisplay, formatDateTimeDisplay } from '../../utils/dateFormat.js';
import '../../style/SupervisorWorkspace.scss';

/**
 * Workspace riêng của ROLE_SUPERVISOR.
 * API /topic-supervisors/me đã giới hạn dữ liệu theo giảng viên hiện tại và
 * chỉ trả về phân công ACTIVE; giao diện không cho phép thao tác quản trị.
 */
export default function SupervisorWorkspacePage() {
  const navigate = useNavigate();
  const toast = useToast();
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [query, setQuery] = useState('');
  const [period, setPeriod] = useState('');
  const [detail, setDetail] = useState(null);
  const [proposalModal, setProposalModal] = useState(false);
  const [proposal, setProposal] = useState(emptySupervisorProposal);
  const [proposalStudents, setProposalStudents] = useState([]);
  const [proposalPeriods, setProposalPeriods] = useState([]);
  const [proposalClasses, setProposalClasses] = useState([]);
  const [proposalLoading, setProposalLoading] = useState(false);
  const [proposalSaving, setProposalSaving] = useState(false);
  const [proposalError, setProposalError] = useState('');

  const load = async () => {
    setLoading(true);
    setError('');
    try {
      const [assignmentResult, proposalResult] = await Promise.allSettled([
        ResourceService.getAll(API_ENDPOINTS.supervisors.mine),
        ResourceService.getAll(API_ENDPOINTS.topics.mine),
      ]);
      if (assignmentResult.status !== 'fulfilled') throw assignmentResult.reason;
      const assignments = Array.isArray(assignmentResult.value) ? assignmentResult.value : [];
      const proposals = proposalResult.status === 'fulfilled' && Array.isArray(proposalResult.value)
        ? proposalResult.value
        : [];
      // Backend đã lọc ACTIVE; kiểm tra lại ở client để tuyệt đối không hiển thị
      // phân công đã kết thúc hoặc bị vô hiệu hóa nếu dữ liệu cũ còn sót trong phản hồi.
      const activeAssignments = assignments.filter((item) => String(item.status || '').toUpperCase() === 'ACTIVE');
      const enriched = await Promise.all(activeAssignments.map(async (assignment) => {
        try {
          const topic = await ResourceService.getOne(API_ENDPOINTS.topics.detail(assignment.topicId));
          return { ...assignment, topic };
        } catch {
          return { ...assignment, topic: null };
        }
      }));
      const assignedTopicIds = new Set(enriched.map((item) => String(item.topicId)));
      const proposalItems = proposals
        .filter((topic) => ['DRAFT', 'PENDING_APPROVAL', 'REJECTED'].includes(String(topic.status || '').toUpperCase()))
        .filter((topic) => !assignedTopicIds.has(String(topic.topicId)))
        .map((topic) => ({
          assignmentId: `proposal-${topic.topicId}`,
          topicId: topic.topicId,
          topic,
          assignedAt: topic.createdAt,
          status: topic.status,
          isProposal: true,
        }));
      setItems([...enriched, ...proposalItems]);
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, 'Không thể tải các đề tài đang được phân công.'));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(); }, []); // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    let cancelled = false;
    const loadProposalOptions = async () => {
      setProposalLoading(true);
      try {
        const [periods, classes] = await Promise.all([
          ResourceService.getAll(API_ENDPOINTS.defensePeriods.list, { size: 500 }),
          ResourceService.getAll(API_ENDPOINTS.classes.list, { size: 500 }),
        ]);
        if (cancelled) return;
        setProposalPeriods((Array.isArray(periods) ? periods : []).filter((item) => item.status !== 'FINISHED'));
        setProposalClasses(Array.isArray(classes) ? classes : []);
      } catch (requestError) {
        if (!cancelled) setProposalError(getApiErrorMessage(requestError, 'Không thể tải danh sách lớp và đợt bảo vệ.'));
      } finally {
        if (!cancelled) setProposalLoading(false);
      }
    };
    loadProposalOptions();
    return () => { cancelled = true; };
  }, []);

  const openProposal = () => {
    setProposal(emptySupervisorProposal);
    setProposalStudents([]);
    setProposalError('');
    setProposalModal(true);
  };

  // Sinh viên được chọn trước khi tạo nhóm/ghi danh đợt. Vì vậy lọc theo lớp/khóa
  // đã có ngay từ lúc tạo tài khoản, không lọc theo đợt bảo vệ.
  useEffect(() => {
    if (!proposalModal || !proposal.classCode) {
      setProposalStudents([]);
      return undefined;
    }
    let cancelled = false;
    const loadStudentsForClass = async () => {
      setProposalLoading(true);
      setProposalError('');
      try {
        const students = await ResourceService.getAll(API_ENDPOINTS.students.list, {
          classCode: proposal.classCode,
          size: 500,
        });
        if (!cancelled) setProposalStudents(Array.isArray(students) ? students : []);
      } catch (requestError) {
        if (!cancelled) setProposalError(getApiErrorMessage(requestError, 'Không thể tải danh sách sinh viên của lớp đã chọn.'));
      } finally {
        if (!cancelled) setProposalLoading(false);
      }
    };
    loadStudentsForClass();
    return () => { cancelled = true; };
  }, [proposalModal, proposal.classCode]);

  const submitProposal = async (event) => {
    event.preventDefault();
    if (!proposal.title.trim() || !proposal.teamName.trim() || !proposal.defensePeriodId || !proposal.studentCodes.length) {
      setProposalError('Vui lòng nhập tên đề tài, tên nhóm, đợt bảo vệ và chọn ít nhất một sinh viên.');
      return;
    }
    if (proposal.file) {
      const extension = proposal.file.name.split('.').pop()?.toLowerCase();
      if (!['pdf', 'doc', 'docx'].includes(extension)) {
        setProposalError('Chỉ hỗ trợ tệp PDF, DOC hoặc DOCX.');
        return;
      }
      if (proposal.file.size > 10 * 1024 * 1024) {
        setProposalError('Tệp không được vượt quá 10 MB.');
        return;
      }
    }
    setProposalSaving(true);
    setProposalError('');
    try {
      const data = new FormData();
      data.append('title', proposal.title.trim());
      data.append('teamName', proposal.teamName.trim());
      data.append('defensePeriodId', String(Number(proposal.defensePeriodId)));
      ['description', 'objective', 'technology'].forEach((field) => {
        if (proposal[field]?.trim()) data.append(field, proposal[field].trim());
      });
      proposal.studentCodes.forEach((studentCode) => data.append('studentCodes', studentCode));
      if (proposal.file) data.append('file', proposal.file);
      await ResourceService.create(API_ENDPOINTS.topics.supervisorProposal, data);
      toast.success('Đã gửi đề xuất đề tài và nhóm chờ admin duyệt.');
      setProposalModal(false);
      setProposal(emptySupervisorProposal);
      await load();
    } catch (requestError) {
      setProposalError(getApiErrorMessage(requestError, 'Không thể gửi đề xuất đề tài.'));
    } finally {
      setProposalSaving(false);
    }
  };

  const periods = useMemo(() => {
    const values = items.map((item) => item.topic?.defensePeriodName).filter(Boolean);
    return [...new Set(values)];
  }, [items]);

  const filtered = useMemo(() => {
    const keyword = normalize(query);
    return items.filter((item) => {
      const topic = item.topic || {};
      const haystack = normalize(`${item.topicTitle} ${topic.title} ${topic.teamName} ${item.lectureCode} ${topic.defensePeriodName}`);
      return (!keyword || haystack.includes(keyword)) && (!period || topic.defensePeriodName === period);
    });
  }, [items, query, period]);

  const openDetail = async (item) => {
    setDetail({ item, topic: item.topic, team: null, milestones: [], submissions: [], loading: true, error: '' });
    try {
      const requests = [];
      if (item.topic?.teamId) requests.push(ResourceService.getOne(API_ENDPOINTS.teams.detail(item.topic.teamId)));
      else requests.push(Promise.resolve(null));
      if (item.topic?.defensePeriodId) requests.push(ResourceService.getAll(API_ENDPOINTS.milestones.byPeriod(item.topic.defensePeriodId)));
      else requests.push(Promise.resolve([]));
      if (item.topic?.teamId) requests.push(ResourceService.getAll(API_ENDPOINTS.submissions.byTeam(item.topic.teamId), { size: 500 }));
      else requests.push(Promise.resolve([]));
      const [teamResult, milestonesResult, submissionsResult] = await Promise.allSettled(requests);
      setDetail((current) => current ? {
        ...current,
        loading: false,
        team: teamResult.status === 'fulfilled' ? teamResult.value : null,
        milestones: milestonesResult.status === 'fulfilled' ? milestonesResult.value : [],
        submissions: submissionsResult.status === 'fulfilled' ? submissionsResult.value : [],
        error: teamResult.status === 'rejected' && milestonesResult.status === 'rejected' && submissionsResult.status === 'rejected'
          ? 'Không thể tải thông tin nhóm, mốc tiến độ và bài nộp.' : '',
      } : current);
    } catch (requestError) {
      setDetail((current) => current ? { ...current, loading: false, error: getApiErrorMessage(requestError, 'Không thể tải chi tiết đề tài.') } : current);
    }
  };

  const showSubmissions = (item) => {
    const teamId = item.topic?.teamId;
    navigate(teamId ? `/lecturer/submissions?teamId=${encodeURIComponent(teamId)}` : '/lecturer/submissions');
  };

  const retry = async () => { await load(); toast.success('Đã tải lại danh sách phân công.'); };

  return <div className="page-stack supervisor-workspace">
    <section className="page-title-row supervisor-workspace-heading">
      <div>
        <span className="page-kicker">PHẠM VI HƯỚNG DẪN</span>
        <h2>Sinh viên hướng dẫn</h2>
        <p>Chỉ xem và xử lý các đề tài, nhóm sinh viên được phân công hướng dẫn đang hoạt động.</p>
      </div>
      <div className="supervisor-heading-actions"><button className="primary-button" onClick={openProposal}><Plus size={15} /> Đề xuất đề tài</button><div className="supervisor-scope-badge"><CheckCircle2 size={16} /><span>Phân công ACTIVE</span></div></div>
    </section>

    <section className="supervisor-summary">
      <article><BookOpen size={20} /><div><span>Đề tài & đề xuất</span><strong>{items.length}</strong></div></article>
      <article><Users size={20} /><div><span>Nhóm sinh viên</span><strong>{new Set(items.map((item) => item.topic?.teamId).filter(Boolean)).size}</strong></div></article>
      <article><CalendarDays size={20} /><div><span>Đợt bảo vệ</span><strong>{periods.length}</strong></div></article>
    </section>

    <section className="panel data-panel supervisor-assignment-panel">
      <div className="table-toolbar">
        <label className="table-search"><Search size={18} /><input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Tìm theo đề tài, nhóm hoặc đợt bảo vệ..." />{query && <button className="search-clear" onClick={() => setQuery('')} aria-label="Xóa tìm kiếm"><X size={15} /></button>}</label>
        <select className="supervisor-period-filter" value={period} onChange={(event) => setPeriod(event.target.value)} aria-label="Lọc theo đợt bảo vệ">
          <option value="">Tất cả đợt bảo vệ</option>{periods.map((value) => <option key={value} value={value}>{value}</option>)}
        </select>
      </div>
      {loading ? <State icon={LoaderCircle} text="Đang tải đề tài được phân công..." spin />
      : error ? <State icon={AlertCircle} text={error} action={retry} />
          : filtered.length === 0 ? <State icon={Inbox} text={items.length ? 'Không tìm thấy đề tài phù hợp.' : 'Chưa có đề tài được phân công hoặc đề xuất.'} />
            : <>
              <div className="table-scroll"><table><thead><tr><th>Đề tài</th><th>Nhóm sinh viên</th><th>Đợt bảo vệ</th><th>Phân công</th><th>Trạng thái</th><th /></tr></thead><tbody>
                {filtered.map((item) => {
                  const topic = item.topic || {};
                  return <tr key={item.assignmentId}>
                    <td data-label="Đề tài"><strong>{topic.title || item.topicTitle || `Đề tài #${item.topicId}`}</strong><small>{topic.technology || `Mã đề tài: ${item.topicId}`}</small></td>
                    <td data-label="Nhóm sinh viên"><strong>{topic.teamName || 'Chưa có nhóm'}</strong><small>{topic.teamId ? `Nhóm #${topic.teamId}` : 'Chưa được gán nhóm'}</small></td>
                    <td data-label="Đợt bảo vệ"><strong>{topic.defensePeriodName || '—'}</strong><small>{topic.academicYear || ''}</small></td>
                    <td data-label={item.isProposal ? 'Ngày gửi' : 'Phân công'}>{formatDateDisplay(item.assignedAt)}</td>
                    <td data-label="Trạng thái"><span className={`status-badge ${item.isProposal ? topicStatusClass(topic.status) : 'success'}`}>{item.isProposal ? topicStatusLabel(topic.status) : 'Đang hoạt động'}</span></td>
                    <td className="table-actions"><div className="inline-actions"><button className="view" onClick={() => openDetail(item)} title="Xem đề tài, nhóm và sinh viên"><Eye size={15} /></button><button className="view" onClick={() => showSubmissions(item)} title="Xem bài nộp và nhận xét"><FileText size={15} /></button></div></td>
                  </tr>;
                })}
              </tbody></table></div>
              <div className="table-footer"><span>Hiển thị {filtered.length} trong tổng số {items.length} đề tài và đề xuất</span></div>
            </>}
    </section>
    {detail && <SupervisorWorkspaceDetail detail={detail} onClose={() => setDetail(null)} onSubmissions={() => showSubmissions(detail.item)} />}
    {proposalModal && <SupervisorProposalModal proposal={proposal} setProposal={setProposal} students={proposalStudents} periods={proposalPeriods} classes={proposalClasses} loading={proposalLoading} saving={proposalSaving} error={proposalError} onSubmit={submitProposal} onClose={() => setProposalModal(false)} />}
  </div>;
}

const emptySupervisorProposal = { title: '', description: '', objective: '', technology: '', defensePeriodId: '', classCode: '', teamName: '', studentCodes: [], file: null };

function SupervisorProposalModal({ proposal, setProposal, students, periods, classes, loading, saving, error, onSubmit, onClose }) {
  const [studentQuery, setStudentQuery] = useState('');
  const filteredStudents = useMemo(() => {
    const query = normalize(studentQuery);
    if (!query) return students;
    return students.filter((student) => normalize(`${student.fullName} ${student.studentCode} ${student.classCode}`).includes(query));
  }, [studentQuery, students]);
  const toggleStudent = (studentCode) => setProposal((current) => ({
    ...current,
    studentCodes: current.studentCodes.includes(studentCode)
      ? current.studentCodes.filter((code) => code !== studentCode)
      : [...current.studentCodes, studentCode],
  }));
  return <div className="modal-backdrop" onMouseDown={(event) => event.target === event.currentTarget && !saving && onClose()}>
    <section className="supervisor-proposal-modal" role="dialog" aria-modal="true">
      <header><div><span>ĐỀ XUẤT ĐỀ TÀI</span><h3>Đề xuất đề tài kèm nhóm</h3></div><button type="button" onClick={onClose} aria-label="Đóng"><X size={20} /></button></header>
      <form onSubmit={onSubmit}>
        <div className="supervisor-proposal-grid">
          <label>Tên đề tài<b>*</b><input required value={proposal.title} onChange={(event) => setProposal({ ...proposal, title: event.target.value })} placeholder="Nhập tên đề tài" /></label>
          <label>Tên nhóm<b>*</b><input required value={proposal.teamName} onChange={(event) => setProposal({ ...proposal, teamName: event.target.value })} placeholder="Nhập tên nhóm" /></label>
          <label>Đợt bảo vệ<b>*</b><select required value={proposal.defensePeriodId} onChange={(event) => setProposal({ ...proposal, defensePeriodId: event.target.value })}><option value="">Chọn đợt bảo vệ</option>{periods.map((period) => <option key={period.defensePeriodId} value={period.defensePeriodId}>{period.periodName} · {period.academicYear}</option>)}</select></label>
          <ClassSearchChoice value={proposal.classCode} classes={classes} onChange={(classCode) => { setStudentQuery(''); setProposal({ ...proposal, classCode }); }} />
          <label>Công nghệ<input value={proposal.technology} onChange={(event) => setProposal({ ...proposal, technology: event.target.value })} placeholder="Ví dụ: Spring Boot, React" /></label>
          <label className="full-width">Mô tả<textarea value={proposal.description} onChange={(event) => setProposal({ ...proposal, description: event.target.value })} /></label>
          <label className="full-width">Mục tiêu<textarea value={proposal.objective} onChange={(event) => setProposal({ ...proposal, objective: event.target.value })} /></label>
          <label className="full-width proposal-file-field">Tệp mô tả đề tài
            <span className="proposal-file-input"><Upload size={15} /><span>{proposal.file ? proposal.file.name : 'Chọn tệp PDF, DOC hoặc DOCX (tối đa 10 MB)'}</span><input type="file" accept=".pdf,.doc,.docx,application/pdf,application/msword,application/vnd.openxmlformats-officedocument.wordprocessingml.document" onChange={(event) => setProposal({ ...proposal, file: event.target.files?.[0] || null })} /></span>
            {proposal.file && <small className="proposal-file-selected">{formatFileSize(proposal.file.size)} · Nhấn để thay tệp</small>}
          </label>
        </div>
        <div className="supervisor-student-picker"><div className="picker-heading"><span>Chọn sinh viên vào nhóm<b>*</b></span><strong>Đã chọn {proposal.studentCodes.length}</strong></div>{!proposal.classCode ? <div className="proposal-option-state">Chọn lớp/khóa để tải danh sách sinh viên. Bạn có thể đổi lớp để chọn thêm sinh viên khác.</div> : loading ? <div className="proposal-option-state">Đang tải danh sách sinh viên...</div> : students.length ? <><label className="proposal-student-search"><Search size={15} /><input value={studentQuery} onChange={(event) => setStudentQuery(event.target.value)} placeholder="Tìm theo họ tên hoặc MSSV..." /></label><div className="proposal-student-list">{filteredStudents.map((student) => <label key={student.studentCode} className={proposal.studentCodes.includes(student.studentCode) ? 'selected' : ''}><input type="checkbox" checked={proposal.studentCodes.includes(student.studentCode)} onChange={() => toggleStudent(student.studentCode)} /><span><strong>{student.fullName || 'Chưa cập nhật họ tên'}</strong><small>{student.studentCode} · {student.classCode || 'Chưa cập nhật lớp'}</small></span></label>)}{filteredStudents.length === 0 && <div className="proposal-option-state">Không tìm thấy sinh viên phù hợp.</div>}</div></> : <div className="proposal-option-state">Lớp/khóa này chưa có sinh viên.</div>}</div>
        {error && <div className="workflow-error"><AlertCircle size={16} />{error}</div>}
        <footer><button type="button" className="secondary-button" onClick={onClose} disabled={saving}>Hủy</button><button type="submit" className="primary-button" disabled={saving || loading}>{saving ? 'Đang gửi...' : 'Gửi đề xuất'}</button></footer>
      </form>
    </section>
  </div>;
}

function ClassSearchChoice({ value, classes, onChange }) {
  const selected = classes.find((item) => String(item.classCode) === String(value));
  const [query, setQuery] = useState(selected ? `${selected.classCode} · ${selected.className || selected.classCode}` : '');
  const [open, setOpen] = useState(false);
  const filtered = useMemo(() => {
    const normalizedQuery = normalize(query);
    return classes.filter((item) => normalize(`${item.classCode} ${item.className || ''}`).includes(normalizedQuery));
  }, [classes, query]);
  const choose = (item) => {
    onChange(item.classCode);
    setQuery(`${item.classCode} · ${item.className || item.classCode}`);
    setOpen(false);
  };

  return <label className="supervisor-class-choice">Lọc theo lớp/khóa
    <div className={`searchable-select ${open ? 'open' : ''}`}>
      <Search size={16} />
      <input value={query} placeholder="Tìm mã hoặc tên lớp..." autoComplete="off" onFocus={() => setOpen(true)} onChange={(event) => { setQuery(event.target.value); onChange(''); setOpen(true); }} />
      <button type="button" className="select-toggle" aria-label="Mở danh sách lớp" aria-expanded={open} onClick={() => setOpen((current) => !current)}><ChevronDown size={17} /></button>
      {open && <div className="searchable-options">{filtered.length ? <><span className="options-caption">Danh sách lớp · {filtered.length} kết quả</span>{filtered.map((item) => <button type="button" key={item.idClass || item.classCode} className={String(value) === String(item.classCode) ? 'selected' : ''} onClick={() => choose(item)}><span><strong>{item.classCode}</strong><small>{item.className || item.classCode}</small></span>{String(value) === String(item.classCode) && <Check size={16} />}</button>)}</> : <span className="option-message">{classes.length ? 'Không tìm thấy lớp phù hợp.' : 'Chưa có lớp để chọn.'}</span>}</div>}
    </div>
    <small className="proposal-class-help">Gõ để lọc danh sách lớp, sau đó chọn lớp cần tải sinh viên.</small>
  </label>;
}

function SupervisorWorkspaceDetail({ detail, onClose, onSubmissions }) {
  const { item, topic, team, milestones, submissions = [], loading, error } = detail;
  const isProposal = Boolean(item.isProposal);
  const students = Array.isArray(team?.students) ? team.students : [];
  const latestSubmissionByMilestone = new Map();
  submissions.filter((submission) => String(submission.status || '').toUpperCase() !== 'WITHDRAWN').forEach((submission) => {
    const key = String(submission.milestoneId);
    const current = latestSubmissionByMilestone.get(key);
    if (!current || Number(submission.version || 0) > Number(current.version || 0) || (Number(submission.version || 0) === Number(current.version || 0) && String(submission.submittedAt || '') > String(current.submittedAt || ''))) latestSubmissionByMilestone.set(key, submission);
  });
  return <div className="modal-backdrop" onMouseDown={(event) => event.target === event.currentTarget && !loading && onClose()}>
    <section className="supervisor-workspace-detail" role="dialog" aria-modal="true">
      <header><div><span>{isProposal ? 'ĐỀ XUẤT ĐỀ TÀI' : 'ĐỀ TÀI ĐƯỢC PHÂN CÔNG'}</span><h3>{topic?.title || item.topicTitle || `Đề tài #${item.topicId}`}</h3></div><button onClick={onClose} aria-label="Đóng"><X size={20} /></button></header>
      {loading ? <State icon={LoaderCircle} text="Đang tải thông tin đề tài và nhóm..." spin /> : error ? <State icon={AlertCircle} text={error} /> : <div className="supervisor-workspace-detail-body">
        <section className="assignment-overview"><article><span>{isProposal ? 'Trạng thái đề xuất' : 'Trạng thái phân công'}</span><strong className={isProposal ? `text-${topicStatusClass(topic?.status)}` : 'text-success'}>{isProposal ? topicStatusLabel(topic?.status) : 'Đang hoạt động'}</strong></article><article><span>Đợt bảo vệ</span><strong>{topic?.defensePeriodName || '—'}</strong></article><article><span>{isProposal ? 'Ngày gửi đề xuất' : 'Ngày phân công'}</span><strong>{formatDateDisplay(item.assignedAt)}</strong></article></section>
        <section className="detail-card"><div className="detail-card-title"><BookOpen size={19} /><div><span>THÔNG TIN ĐỀ TÀI</span><h4>{topic?.title || item.topicTitle}</h4></div></div><div className="topic-detail-grid"><div><span>Mô tả</span><p>{topic?.description || 'Chưa có mô tả.'}</p></div><div><span>Mục tiêu</span><p>{topic?.objective || 'Chưa cập nhật mục tiêu.'}</p></div><div><span>Công nghệ</span><strong>{topic?.technology || 'Chưa cập nhật'}</strong></div><div><span>Ghi chú phân công</span><p>{item.note || 'Không có ghi chú.'}</p></div></div></section>
        {isProposal && String(topic?.status || '').toUpperCase() === 'PENDING_APPROVAL' && <PendingProposalPdf topic={topic} />}
        {isProposal && String(topic?.status || '').toUpperCase() === 'REJECTED' && topic?.rejectionReason && <section className="detail-card proposal-rejection-reason"><div className="detail-card-title"><AlertCircle size={19} /><div><span>PHẢN HỒI TỪ BAN QUẢN LÝ</span><h4>Lý do từ chối đề xuất</h4></div></div><p>{topic.rejectionReason}</p></section>}
        <section className="detail-card"><div className="detail-card-title"><Users size={19} /><div><span>NHÓM SINH VIÊN</span><h4>{team?.nameTeam || topic?.teamName || 'Chưa có nhóm'} · {students.length} sinh viên</h4></div></div>{students.length ? <div className="supervised-student-list">{students.map((student, index) => <article key={student.studentCode || index}><span className="student-number">{index + 1}</span><div><strong>{student.fullName || 'Chưa cập nhật họ tên'}</strong><small>{student.studentCode || 'Chưa có mã sinh viên'}</small></div><div><span>{student.email || 'Chưa có email'}</span><small>{student.phone || 'Chưa có số điện thoại'}</small></div></article>)}</div> : <div className="detail-empty"><Users size={22} /><span>Đề tài chưa có nhóm sinh viên.</span></div>}</section>
        <section className="detail-card"><div className="detail-card-title"><ClipboardCheck size={19} /><div><span>MỐC TIẾN ĐỘ</span><h4>{milestones.length} mốc trong đợt bảo vệ · {latestSubmissionByMilestone.size} mốc đã nộp</h4></div></div>{milestones.length ? <div className="supervisor-milestone-list">{milestones.map((milestone) => { const submission = latestSubmissionByMilestone.get(String(milestone.milestoneId)); const submissionStatus = String(submission?.status || '').toUpperCase(); return <article className={submission ? `has-submission ${submissionStatus === 'APPROVED' ? 'approved' : ''}` : ''} key={milestone.milestoneId}><div><strong>{milestone.milestoneName}</strong><small>{milestone.description || 'Không có mô tả.'}</small>{submission&&<small className="milestone-submitter">{submission.submittedByName || 'Sinh viên'}{submission.studentCode ? ` · ${submission.studentCode}` : ''} · Nộp {formatDateTimeDisplay(submission.submittedAt)}</small>}</div><div className="milestone-progress-state"><span>{milestoneStatusLabel(milestone.status)} · Hạn {formatDateTimeDisplay(milestone.deadline)}</span>{submission?<strong className={`submission-state ${submissionStatus.toLowerCase()}`}>{submissionStatusLabel(submissionStatus)} · Phiên bản {submission.version}</strong>:<em>Chưa nộp</em>}</div></article>; })}</div> : <div className="detail-empty"><ClipboardCheck size={22} /><span>Chưa có mốc tiến độ cho đợt này.</span></div>}</section>
      </div>}
      <footer><button className="secondary-button" onClick={onClose}>Đóng</button><button className="primary-button" onClick={onSubmissions} disabled={loading || isProposal || !topic?.teamId}><FileText size={15} /> Xem bài nộp & nhận xét</button></footer>
    </section>
  </div>;
}

function PendingProposalPdf({ topic }) {
  const [previewUrl, setPreviewUrl] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const isPdf = String(topic?.contentType || '').toLowerCase().includes('pdf') || /\.pdf$/i.test(topic?.fileName || '');

  useEffect(() => () => {
    if (previewUrl) URL.revokeObjectURL(previewUrl);
  }, [previewUrl]);

  if (!topic?.topicId || !topic?.fileName || !isPdf) return null;

  const previewFile = async () => {
    setLoading(true);
    setError('');
    try {
      // HttpClient đã trả trực tiếp response.data từ interceptor, nên kết quả
      // ở đây chính là Blob PDF; không đọc response.data lần nữa.
      const response = await httpClient.get(API_ENDPOINTS.topics.file(topic.topicId), { responseType: 'blob' });
      const pdfBlob = response instanceof Blob
        ? response
        : new Blob([response], { type: 'application/pdf' });
      if (pdfBlob.size === 0) throw new Error('Tệp PDF không có nội dung.');
      const nextPreviewUrl = URL.createObjectURL(pdfBlob);
      setPreviewUrl((current) => {
        if (current) URL.revokeObjectURL(current);
        return nextPreviewUrl;
      });
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, 'Không thể tải tệp PDF để xem trước.'));
    } finally {
      setLoading(false);
    }
  };

  return <section className="detail-card pending-proposal-pdf">
    <div className="detail-card-title"><FileText size={19} /><div><span>TỆP ĐÍNH KÈM</span><h4>File đề xuất đề tài</h4></div></div>
    <div className="pending-proposal-pdf__file"><div><strong>{topic.fileName}</strong><small>{formatFileSize(topic.fileSize)} · PDF</small></div><button type="button" className="secondary-button" onClick={previewFile} disabled={loading}>{loading ? 'Đang tải...' : 'Xem trước'}</button></div>
    {error && <p className="pending-proposal-pdf__error"><AlertCircle size={14} />{error}</p>}
    {previewUrl && <div className="pending-proposal-pdf__viewer"><iframe title={`Xem trước ${topic.fileName}`} src={previewUrl} /><button type="button" onClick={() => setPreviewUrl('')}>Đóng xem trước</button></div>}
  </section>;
}

function State({ icon: Icon, text, action, spin }) { return <div className="data-state"><Icon className={spin ? 'spin' : ''} size={28} /><strong>{text}</strong>{action && <button onClick={action}>Thử lại</button>}</div>; }
function normalize(value) { return String(value || '').normalize('NFD').replace(/[\u0300-\u036f]/g, '').replace(/đ/g, 'd').replace(/Đ/g, 'D').toLowerCase().trim(); }
function milestoneStatusLabel(status) { return { DRAFT: 'Bản nháp', OPEN: 'Đang mở', CLOSED: 'Đã đóng', CANCELLED: 'Đã hủy' }[status] || 'Chưa xác định'; }
function submissionStatusLabel(status) { return { SUBMITTED: 'Đã nộp', UNDER_REVIEW: 'Đang duyệt', REVISION_REQUIRED: 'Cần chỉnh sửa', APPROVED: 'Đã phê duyệt', REJECTED: 'Đã từ chối' }[status] || 'Đã nộp'; }
function topicStatusLabel(status) { return { DRAFT: 'Bản nháp', PENDING_APPROVAL: 'Chờ duyệt', APPROVED: 'Đã duyệt', REGISTERED: 'Đã đăng ký', REJECTED: 'Từ chối' }[String(status || '').toUpperCase()] || 'Chưa xác định'; }
function topicStatusClass(status) { return { DRAFT: 'warning', PENDING_APPROVAL: 'warning', APPROVED: 'success', REGISTERED: 'success', REJECTED: 'danger' }[String(status || '').toUpperCase()] || 'muted'; }
function formatFileSize(bytes) { if (!bytes) return '0 B'; if (bytes < 1024 * 1024) return `${Math.ceil(bytes / 1024)} KB`; return `${(bytes / (1024 * 1024)).toFixed(1)} MB`; }
