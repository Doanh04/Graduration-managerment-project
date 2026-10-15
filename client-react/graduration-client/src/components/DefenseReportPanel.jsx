import React, { useCallback, useEffect, useState } from 'react';
import { CheckCircle2, Download, Eye, LoaderCircle, Upload } from 'lucide-react';
import ResourceService from '../services/ResourceService.jsx';
import API_ENDPOINTS from '../config/endpoints.js';
import getApiErrorMessage from '../utils/apiError.js';
import { formatDateTimeDisplay } from '../utils/dateFormat.js';
import '../style/DefenseReport.scss';

export const REPORT_LABELS = {
  EXEMPT: 'Không áp dụng',
  NOT_SUBMITTED: 'Chưa nộp hồ sơ',
  PENDING: 'Chờ phản biện',
  REVISION_REQUIRED: 'Yêu cầu nộp lại',
  REJECTED: 'Không đủ điều kiện',
  APPROVED: 'Đủ điều kiện xếp lịch',
  FAILED: 'Không đủ điều kiện · 0 điểm',
};

export default function DefenseReportPanel({ assignmentId, onEligibilityChange }) {
  const [data, setData] = useState(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [file, setFile] = useState(null);
  const [sourceUrl, setSource] = useState('');
  const [productUrl, setProduct] = useState('');
  const [note, setNote] = useState('');
  const [comment, setComment] = useState('');
  const [preview, setPreview] = useState(null);

  const accept = useCallback((result) => {
    setData(result);
    onEligibilityChange?.(result?.canScore === true);
  }, [onEligibilityChange]);

  useEffect(() => {
    let active = true;
    setData(null); setError(''); onEligibilityChange?.(false);
    const refresh = () => ResourceService.getOne(API_ENDPOINTS.defenseReports.assignment(assignmentId))
      .then((result) => { if (active) accept(result); })
      .catch((err) => {
        if (active) {
          setError(getApiErrorMessage(err, 'Không thể tải hồ sơ phản biện.'));
          onEligibilityChange?.(false);
        }
      });
    refresh();
    const timer = window.setInterval(refresh, 30000);
    return () => { active = false; window.clearInterval(timer); };
  }, [assignmentId, accept, onEligibilityChange]);

  useEffect(() => () => { if (preview) URL.revokeObjectURL(preview.url); }, [preview]);

  const submit = async (event) => {
    event.preventDefault();
    if (!file || (!sourceUrl.trim() && !productUrl.trim())) {
      setError('Chọn báo cáo và nhập ít nhất một link source code hoặc sản phẩm.');
      return;
    }
    if (file.size > 10 * 1024 * 1024 || !/\.(pdf|docx)$/i.test(file.name)) {
      setError('Chỉ nhận PDF hoặc DOCX tối đa 10 MB.');
      return;
    }
    setBusy(true); setError('');
    try {
      const payload = new FormData();
      payload.append('file', file);
      payload.append('sourceUrl', sourceUrl);
      payload.append('productUrl', productUrl);
      payload.append('note', note);
      const response = await ResourceService.create(API_ENDPOINTS.defenseReports.assignment(assignmentId), payload);
      accept(response.result ?? response);
      setFile(null);
    } catch (err) {
      setError(getApiErrorMessage(err, 'Không thể nộp hồ sơ.'));
    } finally { setBusy(false); }
  };

  const review = async (approved, terminal = false) => {
    if (!approved && !comment.trim()) {
      setError('Vui lòng nhập lý do yêu cầu nộp lại hoặc kết luận không đủ điều kiện.');
      return;
    }
    setBusy(true); setError('');
    try {
      const response = await ResourceService.create(API_ENDPOINTS.defenseReports.review(assignmentId), {
        reportId: data.versions[0].id, approved, terminal, comment,
      });
      accept(response.result ?? response);
      setComment('');
    } catch (err) {
      setError(getApiErrorMessage(err, 'Không thể duyệt hồ sơ.'));
    } finally { setBusy(false); }
  };

  const openFile = async (version, view) => {
    setBusy(true); setError('');
    try {
      const blob = await ResourceService.downloadFile(API_ENDPOINTS.defenseReports.file(version.id));
      const url = URL.createObjectURL(new Blob([blob], { type: version.contentType }));
      if (view && version.contentType === 'application/pdf') {
        setPreview({ url, name: version.fileName });
      } else {
        const link = document.createElement('a');
        link.href = url; link.download = version.fileName;
        document.body.appendChild(link); link.click(); link.remove();
        window.setTimeout(() => URL.revokeObjectURL(url), 1000);
      }
    } catch (err) {
      let message = 'Không thể tải báo cáo.';
      if (err instanceof Blob) {
        try { message = JSON.parse(await err.text()).message || message; } catch { /* giữ thông báo mặc định */ }
      } else message = getApiErrorMessage(err, message);
      setError(message);
    } finally { setBusy(false); }
  };

  return <section className="defense-report-panel">
    <header><h3>Hồ sơ phản biện trước lịch bảo vệ</h3>{data && <span className={`report-badge ${data.status.toLowerCase()}`}>{REPORT_LABELS[data.status]}</span>}</header>
    {error && <p className="report-error" role="alert">{error}</p>}
    {!data ? <p>{error ? 'Chưa tải được hồ sơ. Vui lòng mở lại màn hình.' : 'Đang tải hồ sơ...'}</p> : <>
      <p><strong>Hội đồng dự kiến:</strong> {data.committeeName || 'Chưa cập nhật'}<br/><strong>Hạn chốt phản biện:</strong> {formatDateTimeDisplay(data.deadline)} (giờ Việt Nam)</p>
      {!['APPROVED', 'FAILED'].includes(data.status) && <p className="report-notice">Chỉ sau khi phản biện đồng ý, đề tài mới được đưa vào danh sách xếp lịch bảo vệ. Hết hạn mà chưa được đồng ý, mỗi sinh viên còn tư cách trong nhóm sẽ nhận 0 điểm.</p>}
      {data.failureReason && <p className="report-error">{data.failureReason}</p>}
      {data.status === 'APPROVED' && <p>Hồ sơ đã được phản biện đồng ý. Đề tài đủ điều kiện xếp lịch với hội đồng này sau khi hội đồng được công bố.</p>}
      {data.canSubmit && <form className="report-upload" onSubmit={submit}>
        <label>Báo cáo PDF / DOCX (tối đa 10 MB)<input required disabled={busy} type="file" accept=".pdf,.docx" onChange={(event) => setFile(event.target.files?.[0] || null)} /></label>
        <label>Link source code<input type="url" maxLength={2000} disabled={busy} value={sourceUrl} onChange={(event) => setSource(event.target.value)} placeholder="https://github.com/..." /></label>
        <label>Link sản phẩm / demo<input type="url" maxLength={2000} disabled={busy} value={productUrl} onChange={(event) => setProduct(event.target.value)} placeholder="https://..." /></label>
        <label>Ghi chú<textarea maxLength={4000} disabled={busy} value={note} onChange={(event) => setNote(event.target.value)} /></label>
        <button className="primary-button" disabled={busy}>{busy ? <LoaderCircle className="spin" size={16}/> : <Upload size={16}/>} {data.versions.length ? 'Nộp lại hồ sơ' : 'Nộp hồ sơ'}</button>
      </form>}
      {!data.versions.length && <p>Nhóm chưa nộp báo cáo và liên kết source/sản phẩm.</p>}
      {data.versions.map((version, index) => <article className="report-version" key={version.id}>
        <div><strong>Phiên bản {version.revision}{index === 0 ? ' · Mới nhất' : ''}</strong><small>{formatDateTimeDisplay(version.submittedAt)} · {REPORT_LABELS[version.status]}</small></div>
        <button type="button" className="secondary-button" disabled={busy} onClick={() => openFile(version, true)}>{version.contentType === 'application/pdf' ? <Eye size={15}/> : <Download size={15}/>} {version.fileName} {version.contentType !== 'application/pdf' && '(Tải để mở bằng Word)'}</button>
        <div className="report-links">{version.sourceUrl && <a href={version.sourceUrl} target="_blank" rel="noopener noreferrer">Mở source code ↗</a>}{version.productUrl && <a href={version.productUrl} target="_blank" rel="noopener noreferrer">Mở sản phẩm ↗</a>}</div>
        {version.note && <p>{version.note}</p>}
        {version.reviewedAt && <p><strong>{version.reviewerName}</strong> · {formatDateTimeDisplay(version.reviewedAt)}<br/>{version.reviewComment || 'Đồng ý để xếp lịch bảo vệ.'}</p>}
      </article>)}
      {data.canReview && <div className="report-review"><label>Nhận xét phản biện<textarea maxLength={4000} disabled={busy} value={comment} onChange={(event) => setComment(event.target.value)} placeholder="Bắt buộc khi yêu cầu sửa hoặc kết luận không đủ điều kiện." /></label><div><button type="button" className="secondary-button" disabled={busy || !comment.trim()} onClick={() => review(false)}>Yêu cầu nộp lại</button><button type="button" className="secondary-button danger" disabled={busy || !comment.trim()} onClick={() => review(false, true)}>Không đủ điều kiện · 0 điểm</button><button type="button" className="primary-button" disabled={busy} onClick={() => review(true)}><CheckCircle2 size={16}/> Đồng ý để xếp lịch</button></div></div>}
    </>}
    {preview && <div className="report-preview" role="dialog" aria-modal="true" aria-label="Xem báo cáo PDF"><header><strong>{preview.name}</strong><button type="button" className="secondary-button" onClick={() => setPreview(null)}>Đóng bản xem trước</button></header><iframe title={preview.name} src={preview.url}/></div>}
  </section>;
}
