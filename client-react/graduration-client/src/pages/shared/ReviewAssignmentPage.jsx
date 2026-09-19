import React, { useEffect, useMemo, useState } from 'react';
import { AlertCircle, Check, Download, Eye, FileText, LoaderCircle, MapPin, MessageSquare, Search, Send, Users, X } from 'lucide-react';
import API_ENDPOINTS from '../../config/endpoints.js';
import ResourceService from '../../services/ResourceService.jsx';
import httpClient from '../../config/HttpClient.jsx';
import { useToast } from '../../context/ToastContext.jsx';
import getApiErrorMessage from '../../utils/apiError.js';
import { formatDateDisplay, formatDateTimeDisplay } from '../../utils/dateFormat.js';
import '../../style/WorkflowPages.scss';

const STATUS = {
  ASSIGNED: 'Đã phân công',
  IN_PROGRESS: 'Đang phản biện',
  SUBMITTED: 'Đã gửi phản biện',
  APPROVED: 'Đã duyệt',
  REVISION_REQUIRED: 'Yêu cầu chỉnh sửa',
  CANCELLED: 'Đã hủy',
  COMMITTEE: 'Phản biện theo hội đồng',
};
const SCHEDULE_STATUS = {
  DRAFT: 'Bản nháp',
  PUBLISHED: 'Đã công bố',
  SCHEDULED: 'Đã xếp lịch',
  COMPLETED: 'Đã hoàn thành',
  POSTPONED: 'Đã hoãn',
  CANCELLED: 'Đã hủy',
};

// Chuẩn hóa mã trạng thái lịch từ enum hoặc các tên thuộc tính mà từng API
// có thể trả về, để cùng một nhãn được dùng trong danh sách và modal chi tiết.
function scheduleStatusValue(schedule = {}) {
  // Các API lịch bảo vệ có thể dùng tên trường khác nhau hoặc trả enum dưới
  // dạng object. Duyệt cả các đối tượng lồng nhau để không bỏ sót `status`
  // chuẩn của DefenseScheduleResponse khi dữ liệu đi qua một DTO trung gian.
  const statusKeys = [
    'scheduleStatus', 'scheduleState', 'statusSchedule', 'defenseScheduleStatus',
    'defenseStatus', 'state', 'status', 'schedule_status', 'defense_schedule_status',
  ];
  const containers = [
    schedule,
    schedule.schedule,
    schedule.defenseSchedule,
    schedule.scheduleInfo,
    schedule.defenseScheduleInfo,
    schedule.scheduleData,
    schedule.data,
    schedule.result,
  ].filter((value) => value && typeof value === 'object');
  const extract = (candidate, seen = new Set()) => {
    if (candidate === undefined || candidate === null) return '';
    if (typeof candidate !== 'object') return candidate;
    if (seen.has(candidate)) return '';
    seen.add(candidate);
    for (const key of statusKeys) {
      const nested = extract(candidate[key], seen);
      if (nested !== '') return nested;
    }
    const enumValue = candidate.name ?? candidate.code ?? candidate.value
      ?? candidate.key ?? candidate.label ?? '';
    return enumValue && typeof enumValue === 'object' ? extract(enumValue, seen) : enumValue;
  };
  const candidates = containers.flatMap((container) => statusKeys.map((key) => extract(container[key])));
  const value = candidates.find((candidate) => {
    const normalized = String(candidate ?? '').trim().toUpperCase();
    return normalized !== '' && normalized !== 'COMMITTEE';
  });
  return value ?? '';
}

function scheduleStatusLabel(value) {
  const code = value && typeof value === 'object'
    ? (value.name ?? value.code ?? value.value ?? value.key ?? value.status ?? value.label ?? '')
    : value;
  const normalized = String(code || '').trim().toUpperCase();
  return SCHEDULE_STATUS[normalized] || (typeof code === 'string' ? code : '') || '—';
}
const RECOMMENDATIONS = [
  { value: 'ELIGIBLE_FOR_DEFENSE', label: 'Đủ điều kiện bảo vệ' },
  { value: 'REVISION_REQUIRED', label: 'Cần chỉnh sửa' },
  { value: 'NOT_ELIGIBLE_FOR_DEFENSE', label: 'Không đủ điều kiện bảo vệ' },
];

export default function ReviewAssignmentPage() {
  const toast = useToast();
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [query, setQuery] = useState('');
  const [status, setStatus] = useState('');
  const [modal, setModal] = useState(null);
  const [saving, setSaving] = useState(false);
  const [comment, setComment] = useState('');
  const [recommendation, setRecommendation] = useState('');

  const load = async () => {
    setLoading(true);
    setError('');
    try {
      const reviewParams = status && status !== 'COMMITTEE' ? { status } : {};
      const [assignmentsResult, schedulesResult] = await Promise.allSettled([
        ResourceService.getAll(API_ENDPOINTS.reviews.mine, reviewParams),
        ResourceService.getAll(API_ENDPOINTS.schedules.reviewerMine),
      ]);
      if (assignmentsResult.status === 'rejected' && schedulesResult.status === 'rejected') {
        throw assignmentsResult.reason;
      }
      const assignments = assignmentsResult.status === 'fulfilled' ? assignmentsResult.value : [];
      const committeeSchedules = schedulesResult.status === 'fulfilled' ? schedulesResult.value : [];
      // Không để lỗi API lịch hội đồng bị che khuất khiến trang chỉ hiển thị trạng thái rỗng.
      // Trường hợp thường gặp là backend chưa được khởi động lại sau khi thêm endpoint mới
      // hoặc tài khoản chưa có quyền ROLE_REVIEWER.
      if (schedulesResult.status === 'rejected' && assignments.length === 0) {
        throw schedulesResult.reason;
      }
      const scheduleItems = committeeSchedules.map((schedule) => {
        const normalized = normalizeSchedule(schedule);
        return {
        assignmentId: `committee-${normalized.scheduleId}`,
        source: 'COMMITTEE',
        scheduleId: normalized.scheduleId,
        defensePeriodName: normalized.defensePeriodName,
        academicYear: normalized.academicYear,
        projectType: normalized.projectType,
        topicId: normalized.topicId,
        topicTitle: normalized.topicTitle,
        topicDescription: normalized.topicDescription,
        topicObjective: normalized.topicObjective,
        topicTechnology: normalized.topicTechnology,
        topicFileName: normalized.topicFileName,
        topicFileContentType: normalized.topicFileContentType,
        topicFileSize: normalized.topicFileSize,
        teamName: normalized.teamName,
        teamDescription: normalized.teamDescription,
        submissionId: normalized.submissionId,
        defenseDate: normalized.defenseDate,
        startTime: normalized.startTime,
        endTime: normalized.endTime,
        deadline: normalized.defenseDate && normalized.endTime
          ? `${normalized.defenseDate}T${normalized.endTime}`
          : normalized.defenseDate,
        committeeName: normalized.committeeName,
        committeeDescription: normalized.committeeDescription,
        committeeStatus: normalized.committeeStatus,
        room: normalized.room,
        location: normalized.location,
        session: normalized.session,
        note: normalized.note,
        students: normalized.students,
        committeeMembers: normalized.committeeMembers,
        scheduleStatus: normalized.scheduleStatus,
        scheduleState: normalized.scheduleStatus,
        status: 'COMMITTEE',
      };
      });
      if (status === 'COMMITTEE') {
        setItems(scheduleItems);
      } else {
        // Nếu một đề tài vừa có bản ghi phân công cũ vừa có vai trò phản biện trong hội đồng,
        // ưu tiên bản ghi phân công cũ để không hiển thị trùng và vẫn giữ các thao tác phản biện.
        const merged = new Map();
        [...assignments, ...scheduleItems].forEach((item) => {
          const key = item.topicId || item.assignmentId;
          if (!merged.has(key)) merged.set(key, item);
        });
        setItems([...merged.values()]);
      }
    } catch (e) {
      setError(getApiErrorMessage(e, 'Không thể tải danh sách phản biện.'));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(); }, [status]); // eslint-disable-line react-hooks/exhaustive-deps

  const filtered = useMemo(
    () => items.filter((item) => `${item.topicTitle || ''} ${item.teamName || ''} ${item.assignmentId || ''}`
      .toLowerCase().includes(query.toLowerCase())),
    [items, query],
  );

  const open = (item) => {
    setModal(item);
    setComment(item.reviewComment || '');
    setRecommendation(item.recommendation || '');
    setError('');
  };

  const update = async (action, payload = null) => {
    setSaving(true);
    setError('');
    try {
      await ResourceService.update(API_ENDPOINTS.reviews[action](modal.assignmentId), payload, 'patch');
      toast.success(action === 'submit' ? 'Đã gửi kết quả phản biện.' : 'Đã cập nhật phân công phản biện.');
      setModal(null);
      await load();
    } catch (e) {
      const message = getApiErrorMessage(e, 'Không thể cập nhật phản biện.');
      setError(message);
      toast.error(message, { title: 'Thao tác thất bại' });
    } finally {
      setSaving(false);
    }
  };

  const submit = () => {
    if (!comment.trim() || !recommendation) {
      setError('Vui lòng nhập nhận xét và chọn đề xuất phản biện.');
      return;
    }
    update('submit', { reviewComment: comment.trim(), recommendation });
  };

  return (
    <div className="workflow-page review-assignment-page">
      <div className="page-heading">
        <div><span>PHẢN BIỆN ĐỀ TÀI</span><h1>Đề tài được phân công phản biện</h1><p>Xem hồ sơ, gửi nhận xét và cập nhật kết quả phản biện.</p></div>
      </div>
      {error && !modal && <div className="workflow-error page-error"><AlertCircle size={17} />{error}</div>}
      <section className="workflow-panel">
        <div className="workflow-toolbar">
          <label><Search size={17} /><input value={query} onChange={(e) => setQuery(e.target.value)} placeholder="Tìm theo đề tài hoặc nhóm..." /></label>
          <select value={status} onChange={(e) => setStatus(e.target.value)}>
            <option value="">Tất cả trạng thái</option>
            {Object.entries(STATUS).map(([key, label]) => <option key={key} value={key}>{label}</option>)}
          </select>
        </div>
        {loading ? <div className="workflow-state"><LoaderCircle className="spin" /><strong>Đang tải dữ liệu...</strong></div>
          : filtered.length === 0 ? <div className="workflow-state"><MessageSquare /><strong>Chưa có đề tài được phân công phản biện.</strong></div>
            : <div className="workflow-table"><table><thead><tr><th>Mã</th><th>Đề tài</th><th>Hạn phản biện</th><th>Đề xuất</th><th>Trạng thái</th><th /></tr></thead><tbody>
              {filtered.map((item) => <tr key={item.assignmentId}>
                <td>{item.source === 'COMMITTEE' ? `Lịch #${item.scheduleId}` : item.assignmentId}</td>
                <td><strong>{item.topicTitle || '—'}</strong><small>{item.teamName || 'Chưa có nhóm'}{item.committeeName ? ` · ${item.committeeName}` : ''}</small></td>
                <td>{item.source === 'COMMITTEE' ? formatScheduleTime(item) : formatDate(item.deadline)}</td>
                <td>{item.source === 'COMMITTEE' ? 'Theo vai trò phản biện trong hội đồng' : recommendationLabel(item.recommendation)}</td>
                <td><span className={`workflow-status ${String(item.status || '').toLowerCase()}`}>{item.source === 'COMMITTEE' ? `Hội đồng · ${scheduleStatusLabel(item.scheduleStatus)}` : STATUS[item.status] || item.status}</span></td>
                <td><div className="workflow-actions"><button title="Xem chi tiết" onClick={() => open(item)}><Eye size={15} /></button></div></td>
              </tr>)}
            </tbody></table></div>}
      </section>
      {modal && (modal.source === 'COMMITTEE' ? <CommitteeScheduleModal item={modal} onClose={() => setModal(null)} /> : <div className="modal-backdrop"><section className="workflow-modal"><header><div><span>PHẢN BIỆN ĐỀ TÀI</span><h3>{modal.topicTitle}</h3></div><button onClick={() => setModal(null)}><X size={19} /></button></header><form onSubmit={(e) => { e.preventDefault(); submit(); }}><div className="submission-summary"><div><span>Nhóm</span><strong>{modal.teamName || '—'}</strong></div><div><span>Hạn phản biện</span><strong>{formatDate(modal.deadline)}</strong></div><div><span>Trạng thái</span><strong>{STATUS[modal.status] || modal.status}</strong></div></div><label>Nhận xét<textarea value={comment} onChange={(e) => setComment(e.target.value)} disabled={modal.status === 'APPROVED' || modal.status === 'CANCELLED'} placeholder="Nhập nhận xét chi tiết..." /></label><label>Đề xuất<select value={recommendation} onChange={(e) => setRecommendation(e.target.value)} disabled={modal.status === 'APPROVED' || modal.status === 'CANCELLED'}><option value="">Chọn đề xuất</option>{RECOMMENDATIONS.map((option) => <option key={option.value} value={option.value}>{option.label}</option>)}</select></label>{error && <div className="workflow-error"><AlertCircle size={16} />{error}</div>}<footer><button type="button" className="secondary-button" onClick={() => setModal(null)}>Đóng</button>{modal.status === 'ASSIGNED' && <button type="button" className="secondary-button" disabled={saving} onClick={() => update('start')}>Bắt đầu phản biện</button>}{['ASSIGNED', 'IN_PROGRESS', 'REVISION_REQUIRED'].includes(modal.status) && <button className="primary-button" disabled={saving}>{saving ? 'Đang gửi...' : <><Send size={15} /> Gửi phản biện</>}</button>}</footer></form>{['SUBMITTED'].includes(modal.status) && <div className="review-actions" style={{ padding: '0 24px 20px' }}><button className="success" disabled={saving} onClick={() => update('approve')}><Check size={14} /> Duyệt kết quả</button><button className="warning" disabled={saving} onClick={() => update('revise')}>Yêu cầu chỉnh sửa</button></div>}</section></div>)}
    </div>
  );
}

export function CommitteeScheduleModal({ item, onClose, mode = 'detail' }) {
  const scoreOnly = mode === 'scores';
  const scheduleOnly = mode === 'schedule';
  const toast = useToast();
  const students = Array.isArray(item.students) ? item.students : [];
  const committeeMembers = Array.isArray(item.committeeMembers) ? item.committeeMembers : [];
  const bannedStudentCount = students.filter((student) => student.defenseEligible === false || student.enrollmentStatus === 'WITHDRAWN').length;
  const [filePreview, setFilePreview] = useState(null);
  const [fileLoading, setFileLoading] = useState(false);
  const [fileError, setFileError] = useState('');
  const [scoreStudent, setScoreStudent] = useState(null);
  const [studentScores, setStudentScores] = useState({});
  const [scoresLoading, setScoresLoading] = useState(false);
  const [comments, setComments] = useState([]);
  const [commentsLoading, setCommentsLoading] = useState(false);
  const [submissionMilestoneName, setSubmissionMilestoneName] = useState('');
  const [comment, setComment] = useState('');
  const [commentSaving, setCommentSaving] = useState(false);
  const [commentError, setCommentError] = useState('');
  const hasFile = Boolean(item.topicFileName && item.topicId);
  const isPdf = String(item.topicFileContentType || item.topicFileName || '').toLowerCase().includes('pdf');

  useEffect(() => () => {
    if (filePreview?.objectUrl) URL.revokeObjectURL(filePreview.objectUrl);
  }, [filePreview]);

  const loadComments = async () => {
    if (!item.submissionId) {
      setComments([]);
      setSubmissionMilestoneName('');
      return;
    }
    setCommentsLoading(true);
    setCommentError('');
    try {
      // Nhận xét chỉ chứa nội dung và người viết; tên mốc được lấy từ bài nộp
      // liên kết để hiển thị đúng mốc tiến độ mà nhận xét đang đánh giá.
      const [commentsResult, submissionResult] = await Promise.allSettled([
        ResourceService.getAll(API_ENDPOINTS.comments.bySubmission(item.submissionId)),
        ResourceService.getOne(API_ENDPOINTS.submissions.detail(item.submissionId)),
      ]);
      if (commentsResult.status === 'rejected') throw commentsResult.reason;
      const result = commentsResult.value;
      setComments(Array.isArray(result) ? result : result?.content || result?.data || result?.result || []);
      const submission = submissionResult.status === 'fulfilled' ? submissionResult.value : null;
      setSubmissionMilestoneName(
        milestoneNameFrom(item)
          || milestoneNameFrom(submission)
          || '',
      );
    } catch (error) {
      setCommentError(getApiErrorMessage(error, 'Không thể tải nhận xét cho hồ sơ bảo vệ.'));
    } finally {
      setCommentsLoading(false);
    }
  };

  useEffect(() => {
    setComment('');
    loadComments();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [item.submissionId]);

  // Tải điểm và nhận xét riêng của từng sinh viên để modal chấm điểm phản ánh
  // đúng trạng thái đã chấm hoặc chưa chấm ngay khi mở hồ sơ bảo vệ.
  useEffect(() => {
    let active = true;
    if (!scoreOnly || !item.topicId || !students.length) {
      setStudentScores({});
      setScoresLoading(false);
      return () => { active = false; };
    }

    const loadStudentScores = async () => {
      setScoresLoading(true);
      const entries = await Promise.all(students.map(async (student) => {
        const studentId = studentIdentifier(student);
        if (!studentId) return [studentId, null];
        try {
          const response = await ResourceService.getOne(
            API_ENDPOINTS.scores.current(studentId, item.topicId),
          );
          const result = response?.result ?? response?.data ?? response ?? null;
          return [studentId, isScoreRecord(result) ? result : null];
        } catch {
          // Không có bản ghi điểm (hoặc chưa được cấp quyền xem) không được
          // coi là lỗi tải toàn bộ modal; sinh viên sẽ hiển thị chưa đánh giá.
          return [studentId, null];
        }
      }));
      if (active) {
        setStudentScores(Object.fromEntries(entries.filter(([studentId]) => studentId)));
        setScoresLoading(false);
      }
    };

    loadStudentScores();
    return () => { active = false; };
  }, [scoreOnly, item.topicId, item.students]);

  const submitCommitteeComment = async (event) => {
    event.preventDefault();
    if (!item.submissionId) {
      setCommentError('Nhóm chưa có bài nộp để ghi nhận xét.');
      return;
    }
    if (!comment.trim()) {
      setCommentError('Vui lòng nhập nội dung phê bình.');
      return;
    }
    setCommentSaving(true);
    setCommentError('');
    try {
      await ResourceService.create(API_ENDPOINTS.comments.create(item.submissionId), { comment: comment.trim() });
      setComment('');
      await loadComments();
      toast.success('Đã gửi nhận xét cho đề tài và nhóm sinh viên.');
    } catch (error) {
      const message = getApiErrorMessage(error, 'Không thể gửi nhận xét.');
      setCommentError(message);
      toast.error(message, { title: 'Gửi nhận xét thất bại' });
    } finally {
      setCommentSaving(false);
    }
  };

  const getTopicFile = async () => {
    if (!item.topicId) throw new Error('Đề tài chưa có mã để tải tệp.');
    const response = await httpClient.get(API_ENDPOINTS.topics.file(item.topicId), { responseType: 'blob' });
    const blob = response instanceof Blob
      ? response
      : response?.data instanceof Blob
        ? response.data
        : new Blob([response], { type: item.topicFileContentType || 'application/octet-stream' });
    if (!blob.size) throw new Error('Tệp đề tài không có dữ liệu.');
    return blob;
  };

  const previewTopicFile = async () => {
    setFileLoading(true);
    setFileError('');
    try {
      const blob = await getTopicFile();
      // PDF có thể nhúng trực tiếp; Word/DOCX vẫn được tải về nhưng mở
      // khung xem trước thông tin để người dùng biết rõ loại tệp trước khi tải.
      const pdfBlob = isPdf && blob.type !== 'application/pdf'
        ? new Blob([blob], { type: 'application/pdf' })
        : blob;
      const objectUrl = URL.createObjectURL(pdfBlob);
      setFilePreview({ objectUrl, isPdf });
    } catch (error) {
      const message = getApiErrorMessage(error, 'Không thể tải tệp đề tài để xem trước.');
      setFileError(message);
    } finally {
      setFileLoading(false);
    }
  };

  const downloadTopicFile = async () => {
    setFileLoading(true);
    setFileError('');
    try {
      const blob = await getTopicFile();
      const objectUrl = URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = objectUrl;
      link.download = item.topicFileName || 'de-tai';
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.setTimeout(() => URL.revokeObjectURL(objectUrl), 1000);
    } catch (error) {
      const message = getApiErrorMessage(error, 'Không thể tải tệp đề tài.');
      setFileError(message);
    } finally {
      setFileLoading(false);
    }
  };

  const closeFilePreview = () => {
    if (filePreview?.objectUrl) URL.revokeObjectURL(filePreview.objectUrl);
    setFilePreview(null);
  };

  return <>
    <div className="modal-backdrop"><section className={`workflow-modal review-schedule-modal ${scoreOnly ? 'score-only' : ''} ${scheduleOnly ? 'schedule-only' : ''}`}><header><div><span>{scoreOnly ? 'CHẤM ĐIỂM BẢO VỆ' : 'CHI TIẾT BẢO VỆ ĐỒ ÁN'}</span><h3>{item.topicTitle || 'Đề tài'}</h3></div><button onClick={onClose}><X size={19} /></button></header><div className="review-schedule-body">
    <div className="submission-summary"><div><span>Mã đề tài</span><strong>{item.topicId ? `#${item.topicId}` : '—'}</strong></div><div><span>Nhóm thực hiện</span><strong>{item.teamName || 'Chưa có nhóm'}</strong></div><div><span>Hội đồng</span><strong>{item.committeeName || '—'}</strong></div><div><span>Trạng thái lịch</span><strong>{scheduleStatusLabel(scheduleStatusValue(item))}</strong></div><div><span>Ngày bảo vệ</span><strong>{formatDateDisplay(item.defenseDate)}</strong></div><div><span>Thời gian</span><strong>{formatTimeRange(item)}</strong></div><div><span>Đợt bảo vệ</span><strong>{item.defensePeriodName || '—'}{item.academicYear ? ` · ${item.academicYear}` : ''}</strong></div></div>
    <section className="review-detail-section detail-only"><h4><FileText size={16} /> Nội dung đề tài</h4><div className="review-detail-grid"><div><span>Mô tả</span><strong>{item.topicDescription || 'Chưa cập nhật'}</strong></div><div><span>Mục tiêu</span><strong>{item.topicObjective || 'Chưa cập nhật'}</strong></div><div><span>Công nghệ</span><strong>{item.topicTechnology || 'Chưa cập nhật'}</strong></div><div><span>Mô tả nhóm</span><strong>{item.teamDescription || 'Chưa cập nhật'}</strong></div></div></section>
    {!scoreOnly && hasFile && <section className="review-detail-section review-topic-file detail-only"><h4><FileText size={16} /> Tệp đề tài</h4><div className="review-topic-file-row"><div><strong title={item.topicFileName}>{item.topicFileName}</strong><small>{item.topicFileContentType || 'Tệp đính kèm'}{item.topicFileSize ? ` · ${formatFileSize(item.topicFileSize)}` : ''}</small></div><div className="review-file-actions"><button type="button" className="review-file-action" onClick={previewTopicFile} disabled={fileLoading}>{fileLoading ? <LoaderCircle className="spin" size={14} /> : <Eye size={14} />}Xem trước</button><button type="button" className="review-file-action" onClick={downloadTopicFile} disabled={fileLoading}><Download size={14} />Tải xuống</button></div></div>{fileError && <div className="workflow-error"><AlertCircle size={14} />{fileError}</div>}</section>}
    <section className="review-detail-section detail-only"><h4><MapPin size={16} /> Địa điểm và phiên bảo vệ</h4><div className="review-detail-grid"><div><span>Phòng</span><strong>{item.room || '—'}</strong></div><div><span>Địa điểm</span><strong>{item.location || '—'}</strong></div><div><span>Buổi</span><strong>{sessionLabel(item.session)}</strong></div></div></section>
    <section className="review-detail-section"><h4><Users size={16} /> Sinh viên thực hiện <em>{students.length}{bannedStudentCount ? ` · ${bannedStudentCount}/${students.length} sinh viên bị cấm bảo vệ` : ''}</em></h4>{students.length ? <div className="review-student-list">{students.map((student, index) => <div className={student.defenseEligible === false ? 'defense-ineligible-student' : ''} key={studentIdentifier(student) || `student-${index}`}><div><strong>{student.fullName || '—'}{student.defenseEligible === false && <span className="defense-ineligible-badge">Bị cấm bảo vệ · Điểm 0</span>}</strong><small>{[student.studentCode, student.email, student.phone].filter(Boolean).join(' · ') || 'Chưa có thông tin liên hệ'}</small>{scoreOnly && <StudentAssessment record={student.defenseEligible === false ? { score: 0, status: 'LOCKED', comment: 'Sinh viên bị hủy tư cách thi' } : studentScores[studentIdentifier(student)]} loading={student.defenseEligible === false ? false : scoresLoading} />}</div>{!scheduleOnly && item.currentRole === 'CHAIRPERSON' && student.defenseEligible !== false && student.studentId && item.topicId && <button type="button" className="review-file-action score-student-button" onClick={() => setScoreStudent(student)}><Check size={13} /> Chấm điểm</button>}</div>)}</div> : <p className="review-empty-detail">Chưa có thông tin sinh viên của nhóm.</p>}{item.currentRole && <div className="workflow-info role-info">Vai trò của bạn trong hội đồng: <strong>{committeeRoleLabel(item.currentRole)}</strong>. {item.currentRole === 'CHAIRPERSON' ? 'Bạn có thể nhập và gửi điểm cho từng sinh viên.' : 'Bạn có quyền xem hồ sơ bảo vệ; quyền chấm điểm thuộc về Chủ tịch.'}</div>}</section>
    <section className="review-detail-section committee-comments-section"><h4><MessageSquare size={16} /> {scoreOnly ? 'Nhận xét và chấm điểm' : 'Nhận xét tiến độ và bài nộp'} <em>{comments.length}</em></h4><p className="review-comments-help">{scoreOnly ? 'Tất cả thành viên hội đồng có thể gửi nhận xét. Nhận xét đánh giá khi chấm điểm được lưu cùng bản ghi điểm.' : scheduleOnly ? 'Các nhận xét hiện có của hồ sơ bảo vệ.' : 'Nhận xét tiến độ được lưu trên bài nộp mới nhất của nhóm và hiển thị cho sinh viên, giảng viên hướng dẫn và hội đồng. Nhận xét đánh giá khi chấm điểm được lưu cùng bản ghi điểm.'}</p>{commentsLoading ? <p className="review-empty-detail">Đang tải nhận xét...</p> : comments.length ? <div className="committee-comment-list">{comments.map((entry, index) => <article className="committee-comment" key={entry.commentId || `comment-${index}`}><div className="committee-comment-header"><strong>{entry.lecturerName || entry.authorUsername || entry.authorUserId || 'Thành viên hội đồng'}</strong><small>{formatDateDisplay(entry.createdAt)}</small></div><small className="committee-comment-milestone">Mốc tiến độ: {milestoneNameFrom(entry) || submissionMilestoneName || 'Chưa xác định'}</small><p>{entry.content || entry.comment || '—'}</p></article>)}</div> : <p className="review-empty-detail">Chưa có nhận xét cho bài nộp này.</p>}{scheduleOnly ? <div className="workflow-info schedule-readonly-note">Trang Bảo vệ đồ án chỉ cho phép xem thông tin và nhận xét hiện có. Bạn có thể gửi nhận xét tại trang Chấm điểm bảo vệ.</div> : item.submissionId ? <form className="committee-comment-form" onSubmit={submitCommitteeComment}><textarea value={comment} onChange={(event) => setComment(event.target.value)} placeholder="Nhập nhận xét về tiến độ hoặc bài nộp..." disabled={commentSaving} /><button type="submit" className="primary-button" disabled={commentSaving || !comment.trim()}>{commentSaving ? <LoaderCircle className="spin" size={14} /> : <Send size={14} />} {commentSaving ? 'Đang gửi...' : 'Gửi nhận xét'}</button></form> : <div className="workflow-info">Nhóm chưa có bài nộp nên chưa thể ghi nhận xét trong hồ sơ bảo vệ.</div>}{commentError && <div className="workflow-error"><AlertCircle size={14} />{commentError}</div>}</section>
    <section className="review-detail-section detail-only"><h4><Users size={16} /> Thành viên hội đồng <em>{committeeMembers.length}</em></h4>{committeeMembers.length ? <div className="review-committee-list">{committeeMembers.map((member, index) => <div key={member.lectureId || member.lectureCode || `member-${index}`}><div><strong>{member.lectureName || '—'}</strong><small>{[member.lectureCode, member.degree, member.email].filter(Boolean).join(' · ') || '—'}</small></div><span>{committeeRoleLabel(member.role)}</span></div>)}</div> : <p className="review-empty-detail">Chưa có thông tin thành viên hội đồng.</p>}</section>
    {!scoreOnly && (item.committeeDescription || item.projectType) && <section className="review-detail-section detail-only"><h4><FileText size={16} /> Thông tin hội đồng</h4><div className="review-detail-grid"><div><span>Mô tả</span><strong>{item.committeeDescription || 'Chưa cập nhật'}</strong></div><div><span>Loại đồ án</span><strong>{item.projectType || 'Chưa cập nhật'}</strong></div></div></section>}
    {!scoreOnly && item.note && <section className="review-detail-section detail-only"><h4><FileText size={16} /> Ghi chú lịch bảo vệ</h4><p className="review-note-detail">{item.note}</p></section>}
    {!scoreOnly && <div className="workflow-info review-permission-note detail-only">Mọi thành viên hội đồng đều có thể xem lịch, hồ sơ đề tài, nhóm và sinh viên. Các thao tác đánh giá được giới hạn theo vai trò được phân công.</div>}
  </div><footer><button type="button" className="secondary-button" onClick={onClose}>Đóng</button></footer></section></div>
    {!scoreOnly && filePreview && <div className="modal-backdrop review-file-preview-backdrop" onMouseDown={(event) => event.target === event.currentTarget && closeFilePreview()}><section className="file-preview-modal review-file-preview-modal" role="dialog" aria-modal="true"><header><div><span>XEM TRƯỚC TỆP ĐỀ TÀI</span><h3>{item.topicFileName}</h3></div><button type="button" onClick={closeFilePreview} aria-label="Đóng"><X size={19} /></button></header>{filePreview.isPdf ? <div className="file-preview-frame"><iframe src={filePreview.objectUrl} title={`Xem trước ${item.topicFileName}`} /></div> : <div className="file-preview-unavailable"><FileText size={34} /><strong>Không thể hiển thị trực tiếp tệp Word trong trình duyệt</strong><span>Tệp DOC/DOCX đã được tải từ hệ thống. Hãy tải tệp về máy để mở đầy đủ nội dung.</span><button type="button" className="secondary-button" onClick={downloadTopicFile} disabled={fileLoading}>{fileLoading ? <LoaderCircle className="spin" size={14} /> : <Download size={14} />}Tải xuống tệp Word</button></div>}<footer><span>{filePreview.isPdf ? 'PDF được tải có xác thực và hiển thị trực tiếp trong cửa sổ này.' : 'Tệp Word không hỗ trợ hiển thị trực tiếp; bạn có thể tải xuống để xem.'}</span><button type="button" className="secondary-button" onClick={closeFilePreview}>Đóng</button></footer></section></div>}
    {scoreStudent && <CommitteeScoreModal item={item} student={scoreStudent} onClose={() => setScoreStudent(null)} />}
  </>;
}

function CommitteeScoreModal({ item, student, onClose }) {
  const toast = useToast();
  const [score, setScore] = useState('');
  const [comment, setComment] = useState('');
  const [scoreResult, setScoreResult] = useState(null);
  const [loadingScore, setLoadingScore] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    let active = true;
    const loadScore = async () => {
      setLoadingScore(true);
      setError('');
      try {
        const result = await ResourceService.getOne(
          API_ENDPOINTS.scores.current(student.studentId, item.topicId),
        );
        if (!active) return;
        setScoreResult(result || null);
        setScore(result?.totalScore ?? '');
        setComment(result?.comment || '');
      } catch (e) {
        if (active) setError(getApiErrorMessage(e, 'Không thể tải điểm hiện tại.'));
      } finally {
        if (active) setLoadingScore(false);
      }
    };
    loadScore();
    return () => { active = false; };
  }, [item.topicId, student.studentId]);

  const scoreStatus = String(scoreResult?.status || '').toUpperCase();
  const editable = !scoreResult || scoreStatus === 'DRAFT';
  const canUnlock = scoreStatus === 'SUBMITTED' || scoreStatus === 'LOCKED';

  const saveScore = async (submit = false) => {
    if (!editable) {
      setError('Điểm đã gửi hoặc đã khóa. Vui lòng mở khóa trước khi cập nhật.');
      return;
    }
    if (score === '' || Number.isNaN(Number(score)) || Number(score) < 0 || Number(score) > 10) {
      setError('Điểm phải là số từ 0 đến 10.');
      return;
    }
    const normalizedComment = comment.trim();
    if (!normalizedComment) {
      setError('Vui lòng nhập nhận xét khi chấm điểm.');
      return;
    }
    setSaving(true);
    setError('');
    try {
      const response = await ResourceService.update(
        API_ENDPOINTS.scores.save(student.studentId, item.topicId),
        { score: Number(score), comment: normalizedComment },
        'put',
      );
      let result = response?.result ?? response;
      setScoreResult(result);
      if (submit && result?.scoreId) {
        const submitted = await ResourceService.update(API_ENDPOINTS.scores.submit(result.scoreId), null, 'patch');
        result = submitted?.result ?? submitted;
        setScoreResult(result);
      }
      toast.success(submit ? 'Đã gửi điểm bảo vệ.' : 'Đã lưu điểm nháp.');
      if (submit) onClose();
    } catch (e) {
      const message = getApiErrorMessage(e, 'Không thể lưu điểm bảo vệ.');
      setError(message);
      toast.error(message, { title: 'Chấm điểm thất bại' });
    } finally {
      setSaving(false);
    }
  };

  const unlockScore = async () => {
    if (!scoreResult?.scoreId || !canUnlock) return;
    setSaving(true);
    setError('');
    try {
      const response = await ResourceService.update(API_ENDPOINTS.scores.unlock(scoreResult.scoreId), null, 'patch');
      const result = response?.result ?? response;
      setScoreResult(result);
      setScore(result?.totalScore ?? score);
      setComment(result?.comment || comment);
      toast.success('Đã mở khóa điểm, bạn có thể sửa điểm và nhận xét.');
    } catch (e) {
      const message = getApiErrorMessage(e, 'Không thể mở khóa điểm.');
      setError(message);
      toast.error(message, { title: 'Mở khóa thất bại' });
    } finally {
      setSaving(false);
    }
  };

  const deleteScore = async () => {
    if (!scoreResult?.scoreId || scoreStatus !== 'DRAFT') return;
    if (!window.confirm('Bạn có chắc muốn xóa điểm nháp và nhận xét này không?')) return;
    setSaving(true);
    setError('');
    try {
      await ResourceService.remove(API_ENDPOINTS.scores.remove(scoreResult.scoreId));
      setScoreResult(null);
      setScore('');
      setComment('');
      toast.success('Đã xóa điểm nháp.');
    } catch (e) {
      const message = getApiErrorMessage(e, 'Không thể xóa điểm.');
      setError(message);
      toast.error(message, { title: 'Xóa điểm thất bại' });
    } finally {
      setSaving(false);
    }
  };

  return <div className="modal-backdrop score-modal-backdrop"><section className="workflow-modal compact committee-score-modal"><header><div><span>CHẤM ĐIỂM BẢO VỆ</span><h3>{student.fullName || student.studentCode || 'Sinh viên'}</h3></div><button type="button" onClick={onClose}><X size={19} /></button></header><form onSubmit={(event) => { event.preventDefault(); saveScore(true); }}><div className="submission-summary"><div><span>Đề tài</span><strong>{item.topicTitle || '—'}</strong></div><div><span>Nhóm</span><strong>{item.teamName || '—'}</strong></div><div><span>Mã sinh viên</span><strong>{student.studentCode || '—'}</strong></div><div><span>Trạng thái điểm</span><strong>{scoreStatusLabel(scoreStatus || 'DRAFT')}</strong></div></div>{loadingScore ? <div className="workflow-info">Đang tải điểm hiện tại...</div> : <><label>Điểm (0–10)<input type="number" min="0" max="10" step="0.01" value={score} onChange={(event) => setScore(event.target.value)} placeholder="Nhập điểm" disabled={!editable || saving} /></label><label>Nhận xét đánh giá <span aria-hidden="true">*</span><textarea required value={comment} onChange={(event) => setComment(event.target.value)} placeholder="Nhập nhận xét đánh giá..." disabled={!editable || saving} /></label></>}{error && <div className="workflow-error"><AlertCircle size={15} />{error}</div>}<footer><button type="button" className="secondary-button" onClick={onClose}>Hủy</button>{canUnlock && <button type="button" className="secondary-button" disabled={saving} onClick={unlockScore}>Mở khóa</button>}{editable && scoreResult?.scoreId && <button type="button" className="secondary-button score-delete-button" disabled={saving} onClick={deleteScore}>Xóa điểm</button>}{editable && <><button type="button" className="secondary-button" disabled={saving || loadingScore} onClick={() => saveScore(false)}>Lưu nháp</button><button type="submit" className="primary-button" disabled={saving || loadingScore}>{saving ? 'Đang lưu...' : 'Gửi điểm'}</button></>}</footer></form></section></div>;
}

function scoreStatusLabel(value) {
  return { DRAFT: 'Bản nháp', SUBMITTED: 'Đã gửi', LOCKED: 'Đã khóa' }[String(value || '').toUpperCase()] || 'Chưa chấm';
}

// Lấy mã định danh ổn định của sinh viên để ghép bản ghi điểm với đúng dòng
// sinh viên trong hồ sơ bảo vệ, kể cả khi API trả về tên thuộc tính khác nhau.
function studentIdentifier(student = {}) {
  return student.studentId || student.idStudent || student.studentCode || student.code || '';
}

// API điểm có thể trả về null/rỗng khi sinh viên chưa được đánh giá; chỉ coi
// đối tượng có dữ liệu thực tế là một bản ghi điểm hợp lệ để hiển thị.
function isScoreRecord(value) {
  return Boolean(value && typeof value === 'object' && Object.keys(value).length > 0);
}

// Hiển thị điểm và nhận xét đã lưu cho từng sinh viên; nếu chưa có bản ghi,
// thông báo rõ sinh viên chưa được đánh giá thay vì để ô trống khó hiểu.
function StudentAssessment({ record, loading }) {
  if (loading) {
    return <div className="review-student-assessment"><small className="review-student-assessment-loading">Đang tải kết quả đánh giá...</small></div>;
  }
  if (!record) {
    return <div className="review-student-assessment"><small className="review-student-unrated">Sinh viên chưa được đánh giá</small></div>;
  }

  const score = record.totalScore ?? record.score ?? record.point;
  const evaluation = record.comment ?? record.evaluation ?? record.feedback ?? record.reviewComment;
  return <div className="review-student-assessment">
    <small><strong>Điểm:</strong> {score === null || score === undefined || score === '' ? 'Chưa có điểm' : `${score}/10`}</small>
    <small><strong>Đánh giá:</strong> {evaluation || 'Chưa có nhận xét'}</small>
  </div>;
}

function formatDate(value) { return formatDateTimeDisplay(value); }
function formatTimeRange(item) { return item.startTime ? `${item.startTime}${item.endTime ? `–${item.endTime}` : ''}` : '—'; }
function formatScheduleTime(item) { return `${formatDateDisplay(item.defenseDate)}${item.startTime ? ` · ${formatTimeRange(item)}` : ''}`; }
function recommendationLabel(value) { return RECOMMENDATIONS.find((item) => item.value === value)?.label || '—'; }
function milestoneNameFrom(value = {}) {
  return value?.milestoneName
    || value?.milestoneTitle
    || value?.milesStoneName
    || value?.milestone?.milestoneName
    || value?.milestone?.name
    || value?.milestone?.title
    || value?.submission?.milestoneName
    || value?.submission?.milestone?.milestoneName
    || '';
}
function committeeRoleLabel(value) {
  const labels = { CHAIRPERSON: 'Chủ tịch', SECRETARY: 'Thư ký', REVIEWER: 'Phản biện', MEMBER: 'Thành viên' };
  const normalized = String(value || '').toUpperCase();
  return labels[normalized] || value || '—';
}
function sessionLabel(value) { return { MORNING: 'Buổi sáng', AFTERNOON: 'Buổi chiều', EVENING: 'Buổi tối' }[value] || value || '—'; }

// Chuẩn hóa các biến thể dữ liệu lịch từ API để modal luôn hiển thị đủ đề tài,
// phòng, sinh viên và thành viên hội đồng kể cả khi backend trả về đối tượng lồng nhau.
function normalizeSchedule(schedule = {}) {
  const topic = schedule.topic || schedule.topicInfo || {};
  const team = schedule.team || topic.team || topic.proposedTeam || {};
  const committee = schedule.committee || schedule.defenseCommittee || {};
  const normalizedStatus = scheduleStatusValue(schedule);
  const students = firstNonEmptyArray(
    schedule.students,
    schedule.studentSummaries,
    team.students,
    team.studentEntities,
    topic.students,
  ).map((student) => ({
    studentId: student.studentId || student.idStudent || student.id,
    studentCode: student.studentCode || student.code,
    fullName: student.fullName || student.fullNameStudent || student.name,
    email: student.email || student.emailStudent,
    phone: student.phone || student.phoneStudent,
    enrollmentStatus: student.enrollmentStatus,
    defenseEligible: student.defenseEligible !== false && student.enrollmentStatus !== 'WITHDRAWN',
  }));
  const committeeMembers = firstNonEmptyArray(
    schedule.committeeMembers,
    schedule.members,
    schedule.memberSummaries,
    committee.committeeMembers,
    committee.members,
    committee.comitteesMember,
  ).map((member) => ({
    lectureId: member.lectureId || member.idLecture || member.lecturerId,
    lectureCode: member.lectureCode || member.lecturerCode || member.code,
    lectureName: member.lectureName || member.fullNameLecture || member.fullName || member.name,
    email: member.email || member.emailLecture,
    degree: member.degree,
    role: member.role,
  }));
  return {
    ...schedule,
    // DefenseScheduleResponse dùng trường `status`; giữ các alias để màn
    // hình bảo vệ, phản biện và chấm điểm dùng chung đúng trạng thái lịch.
    status: normalizedStatus,
    scheduleStatus: normalizedStatus,
    scheduleState: normalizedStatus,
    scheduleId: schedule.scheduleId || schedule.idDefenseScheduce || schedule.id,
    defensePeriodName: schedule.defensePeriodName || schedule.periodName,
    academicYear: schedule.academicYear || schedule.academicYearName,
    projectType: schedule.projectType,
    topicId: schedule.topicId || schedule.idTopic || topic.topicId || topic.idTopic || topic.id,
    topicTitle: schedule.topicTitle || topic.title || topic.topicTitle,
    topicDescription: schedule.topicDescription || topic.description,
    topicObjective: schedule.topicObjective || topic.objective,
    topicTechnology: schedule.topicTechnology || topic.technology,
    topicFileName: schedule.topicFileName || schedule.fileName || topic.fileName || topic.originalFileName,
    topicFileContentType: schedule.topicFileContentType || schedule.contentType || topic.contentType,
    topicFileSize: schedule.topicFileSize || schedule.fileSize || topic.fileSize,
    submissionId: schedule.submissionId || schedule.latestSubmissionId || topic.submissionId,
    teamName: schedule.teamName || team.nameTeam || team.teamName || team.name,
    teamDescription: schedule.teamDescription || team.description,
    committeeName: schedule.committeeName || committee.comitteesName || committee.committeeName || committee.name,
    committeeId: schedule.committeeId || schedule.idCommittee || committee.committeeId || committee.idCommittee || committee.id,
    committeeDescription: schedule.committeeDescription || committee.description,
    committeeStatus: schedule.committeeStatus || committee.status,
    room: schedule.room || schedule.roomName,
    location: schedule.location || schedule.venue,
    session: schedule.session || schedule.sesstion,
    students,
    committeeMembers,
  };
}

function firstNonEmptyArray(...values) {
  const arrays = values.filter(Array.isArray);
  return arrays.find((value) => value.length > 0) || arrays[0] || [];
}

function formatFileSize(value) {
  const size = Number(value);
  if (!Number.isFinite(size) || size <= 0) return '';
  if (size < 1024) return `${size} B`;
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`;
  return `${(size / (1024 * 1024)).toFixed(1)} MB`;
}

export {
  normalizeSchedule,
  committeeRoleLabel,
  formatTimeRange,
  formatScheduleTime,
  scheduleStatusValue,
  scheduleStatusLabel,
  SCHEDULE_STATUS,
};
