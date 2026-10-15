import React, { useEffect, useMemo, useState } from 'react';
import { AlertCircle, CheckCircle2, LockOpen, Save, X } from 'lucide-react';
import API_ENDPOINTS from '../config/endpoints.js';
import ResourceService from '../services/ResourceService.jsx';
import { useToast } from '../context/ToastContext.jsx';
import getApiErrorMessage from '../utils/apiError.js';
import '../style/CommitteeScoreSheet.scss';

const ROLE_LABELS = {
  CHAIRPERSON: 'Chủ tịch',
  SECRETARY: 'Thư ký',
  REVIEWER: 'Phản biện',
  MEMBER: 'Thành viên',
};

const statusLabel = (value) => ({
  DRAFT: 'Đang nhập',
  SUBMITTED: 'Đã gửi',
  LOCKED: 'Đã khóa',
}[String(value || '').toUpperCase()] || 'Chưa chấm');

export default function CommitteeScoreSheetModal({ item, student, onClose }) {
  const toast = useToast();
  const [sheet, setSheet] = useState(null);
  const [rows, setRows] = useState([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');

  const load = async () => {
    setLoading(true);
    setError('');
    try {
      const response = await ResourceService.getOne(
        API_ENDPOINTS.scores.committeeSheet(student.studentId, item.topicId),
      );
      const result = response?.result ?? response;
      setSheet(result);
      setRows((result?.entries || []).map((entry) => ({
        ...entry,
        score: entry.score ?? '',
        comment: entry.comment || '',
      })));
    } catch (exception) {
      setError(getApiErrorMessage(exception, 'Không thể tải phiếu điểm hội đồng.'));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(); }, [item.topicId, student.studentId]); // eslint-disable-line react-hooks/exhaustive-deps

  const average = useMemo(() => {
    const values = rows.map((row) => Number(row.score)).filter((value) => Number.isFinite(value) && value >= 0);
    if (!values.length) return null;
    return (values.reduce((sum, value) => sum + value, 0) / values.length).toFixed(2);
  }, [rows]);

  const updateRow = (lecturerId, field, value) => {
    setRows((current) => current.map((row) => (row.lecturerId === lecturerId ? { ...row, [field]: value } : row)));
  };

  const editableRows = () => rows.filter((row) => String(row.status || 'DRAFT').toUpperCase() === 'DRAFT');

  const validate = (requireAll) => {
    const allComplete = rows.every((row) => row.score !== ''
      && Number(row.score) >= 0
      && Number(row.score) <= 10
      && row.comment.trim());
    if (requireAll && !allComplete) {
      setError('Cần nhập đủ điểm từ 0 đến 10 và nhận xét của Chủ tịch và Phản biện.');
      return null;
    }
    const candidates = editableRows().filter((row) => row.score !== '' || row.comment.trim());
    if (!candidates.length) {
      setError('Chưa có điểm hoặc nhận xét mới để lưu.');
      return null;
    }
    if (candidates.some((row) => row.score === '' || Number(row.score) < 0 || Number(row.score) > 10)) {
      setError('Điểm của Chủ tịch và Phản biện phải nằm trong khoảng từ 0 đến 10.');
      return null;
    }
    if (candidates.some((row) => !row.comment.trim())) {
      setError('Chủ tịch và Phản biện phải có nhận xét riêng.');
      return null;
    }
    return candidates.map((row) => ({
      lecturerId: row.lecturerId,
      score: Number(row.score),
      comment: row.comment.trim(),
    }));
  };

  const save = async (submit) => {
    const entries = validate(submit);
    if (!entries) return;
    setSaving(true);
    setError('');
    try {
      const savedResponse = await ResourceService.update(
        API_ENDPOINTS.scores.committeeSheet(student.studentId, item.topicId),
        { entries },
        'put',
      );
      let result = savedResponse?.result ?? savedResponse;
      if (submit) {
        const submittedResponse = await ResourceService.update(
          API_ENDPOINTS.scores.submitCommitteeSheet(student.studentId, item.topicId),
          null,
          'patch',
        );
        result = submittedResponse?.result ?? submittedResponse;
      }
      setSheet(result);
      setRows((result?.entries || []).map((entry) => ({
        ...entry,
        score: entry.score ?? '',
        comment: entry.comment || '',
      })));
      toast.success(submit ? 'Đã khóa bảng điểm. Điểm sẽ được công bố sau khi Admin duyệt bản final.' : 'Đã lưu nháp phiếu điểm hội đồng.');
    } catch (exception) {
      const message = getApiErrorMessage(exception, 'Không thể lưu phiếu điểm hội đồng.');
      setError(message);
      toast.error(message, { title: 'Chấm điểm thất bại' });
    } finally {
      setSaving(false);
    }
  };

  const unlock = async (row) => {
    if (!row.scoreId) return;
    setSaving(true);
    setError('');
    try {
      await ResourceService.update(API_ENDPOINTS.scores.unlock(row.scoreId), null, 'patch');
      await load();
      toast.success(`Đã mở khóa phần chấm của vai trò ${ROLE_LABELS[row.role] || row.role}.`);
    } catch (exception) {
      const message = getApiErrorMessage(exception, 'Không thể mở khóa phần chấm điểm.');
      setError(message);
      toast.error(message, { title: 'Mở khóa thất bại' });
    } finally {
      setSaving(false);
    }
  };

  const hasEditableRole = rows.some((row) => !['LOCKED', 'SUBMITTED'].includes(String(row.status || '').toUpperCase()));

  return (
    <div className="modal-backdrop score-modal-backdrop">
      <section className="workflow-modal committee-score-sheet-modal">
        <header>
          <div><span>THƯ KÝ NHẬP ĐIỂM HỘI ĐỒNG</span><h3>{student.fullName || student.studentCode || 'Sinh viên'}</h3></div>
          <button type="button" onClick={onClose}><X size={19} /></button>
        </header>
        <div className="committee-score-sheet-body">
          <div className="submission-summary">
            <div><span>Đề tài</span><strong>{item.topicTitle || '—'}</strong></div>
            <div><span>Nhóm</span><strong>{item.teamName || '—'}</strong></div>
            <div><span>Mã sinh viên</span><strong>{student.studentCode || '—'}</strong></div>
            <div><span>Điểm trung bình</span><strong>{average === null ? 'Chưa có' : `${average}/10`}</strong></div>
          </div>

          {loading ? <div className="workflow-info">Đang tải phiếu điểm hội đồng...</div> : (
            <div className="committee-score-rows">
              {rows.map((row) => {
                const locked = ['LOCKED', 'SUBMITTED'].includes(String(row.status || '').toUpperCase());
                return <article className={`committee-score-row ${locked ? 'locked' : ''}`} key={row.lecturerId}>
                  <div className="committee-score-member">
                    <div><strong>{row.lecturerName || row.lecturerCode}</strong><small>{row.lecturerCode || '—'}</small></div>
                    <span>{ROLE_LABELS[row.role] || row.role}</span>
                    <em>{statusLabel(row.status)}</em>
                  </div>
                  <label>Điểm
                    <input type="number" min="0" max="10" step="0.01" value={row.score}
                      disabled={locked || saving} onChange={(event) => updateRow(row.lecturerId, 'score', event.target.value)} />
                  </label>
                  <label>Nhận xét của thành viên
                    <textarea value={row.comment} disabled={locked || saving}
                      onChange={(event) => updateRow(row.lecturerId, 'comment', event.target.value)}
                      placeholder={`Nhập nhận xét của ${ROLE_LABELS[row.role] || 'thành viên'}...`} />
                  </label>
                  {locked && <button type="button" className="secondary-button unlock-role-button" disabled={saving} onClick={() => unlock(row)}>
                    <LockOpen size={14} /> Mở khóa {ROLE_LABELS[row.role] || row.role}
                  </button>}
                </article>;
              })}
            </div>
          )}
          {sheet?.published && <div className="workflow-info score-published-notice"><CheckCircle2 size={16} /> Điểm đã được công bố. Muốn sửa vai trò nào, hãy mở khóa đúng phần chấm của vai trò đó.</div>}
          {!sheet?.published && !hasEditableRole && rows.length > 0 && <div className="workflow-info score-published-notice"><CheckCircle2 size={16} /> Bảng điểm đã khóa và chưa công bố. Sinh viên cần nộp bản báo cáo bản cuối để Trưởng khoa/Phó khoa duyệt trước khi nhận điểm.</div>}
          {error && <div className="workflow-error"><AlertCircle size={15} />{error}</div>}
        </div>
        <footer>
          <button type="button" className="secondary-button" onClick={onClose}>Đóng</button>
          {!sheet?.published && hasEditableRole && <>
            <button type="button" className="secondary-button" disabled={saving || loading} onClick={() => save(false)}><Save size={15} /> Lưu nháp</button>
            <button type="button" className="primary-button" disabled={saving || loading} onClick={() => save(true)}><CheckCircle2 size={15} /> Hoàn tất chấm điểm</button>
          </>}
        </footer>
      </section>
    </div>
  );
}
