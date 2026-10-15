import React, { useEffect, useMemo, useState } from 'react';
import { Archive, CheckCircle2, Download, Eye, FileCheck2, LoaderCircle, Upload, X } from 'lucide-react';
import API_ENDPOINTS from '../../config/endpoints.js';
import ResourceService from '../../services/ResourceService.jsx';
import getApiErrorMessage from '../../utils/apiError.js';
import { formatDateTimeDisplay } from '../../utils/dateFormat.js';
import '../../style/DefenseReport.scss';

const labels = {
  WAITING_SCORE: 'Chờ hội đồng hoàn tất điểm',
  NOT_SUBMITTED: 'Chưa nộp bản final',
  PENDING: 'Chờ Admin duyệt',
  REVISION_REQUIRED: 'Admin yêu cầu nộp lại',
  APPROVED: 'Đã duyệt',
  PUBLISHED: 'Đã duyệt và công bố điểm',
  EXPIRED: 'Quá hạn · 0 điểm',
};

export default function FinalSubmissionPage({ mode = 'student' }) {
  const admin = mode === 'admin';
  const [items, setItems] = useState([]);
  const [selectedId, setSelectedId] = useState('');
  const [periodId, setPeriodId] = useState('');
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [file, setFile] = useState(null);
  const [sourceUrl, setSourceUrl] = useState('');
  const [productUrl, setProductUrl] = useState('');
  const [note, setNote] = useState('');
  const [comments, setComments] = useState({});
  const [preview, setPreview] = useState(null);
  const [backupOpen, setBackupOpen] = useState(false);
  const [backupPeriodId, setBackupPeriodId] = useState('');
  const [backupBusy, setBackupBusy] = useState(false);
  const selected = useMemo(() => items.find((item) => String(item.topicId) === selectedId), [items, selectedId]);
  const periods = useMemo(() => [...new Map(items.map((item) => [
    String(item.defensePeriodId), { id: item.defensePeriodId, name: item.periodName, year: item.academicYear }],
  )).values()], [items]);
  const periodItems = useMemo(() => items.filter((item) => !periodId || String(item.defensePeriodId) === periodId), [items, periodId]);

  const load = async (keepSelection = true) => {
    setLoading(true); setError('');
    try {
      const result = await ResourceService.getOne(admin ? API_ENDPOINTS.finalSubmissions.admin : API_ENDPOINTS.finalSubmissions.mine);
      const list = Array.isArray(result) ? result : [];
      setItems(list);
      setSelectedId((current) => keepSelection && list.some((item) => String(item.topicId) === current)
        ? current : String(list[0]?.topicId || ''));
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, 'Không thể tải danh sách bản final.'));
    } finally { setLoading(false); }
  };

  useEffect(() => { load(false); }, [admin]);
  useEffect(() => () => { if (preview) URL.revokeObjectURL(preview.url); }, [preview]);

  const submit = async (event) => {
    event.preventDefault();
    if (!file || (!sourceUrl.trim() && !productUrl.trim())) {
      setError('Vui lòng chọn báo cáo và nhập ít nhất một link source code hoặc sản phẩm.'); return;
    }
    if (file.size > 10 * 1024 * 1024 || !/\.(pdf|docx)$/i.test(file.name)) {
      setError('Chỉ nhận PDF hoặc DOCX tối đa 10 MB.'); return;
    }
    setBusy(true); setError('');
    try {
      const payload = new FormData();
      payload.append('file', file); payload.append('sourceUrl', sourceUrl);
      payload.append('productUrl', productUrl); payload.append('note', note);
      await ResourceService.create(API_ENDPOINTS.finalSubmissions.topic(selected.topicId), payload);
      setFile(null); setSourceUrl(''); setProductUrl(''); setNote('');
      await load();
    } catch (requestError) { setError(getApiErrorMessage(requestError, 'Không thể nộp bản final.')); }
    finally { setBusy(false); }
  };

  const review = async (approved, target = selected) => {
    const topicComment = comments[String(target?.topicId)] || '';
    if (!approved && !topicComment.trim()) { setError('Vui lòng nhập lý do yêu cầu nộp lại.'); return; }
    setBusy(true); setError('');
    try {
      await ResourceService.create(API_ENDPOINTS.finalSubmissions.review(target.topicId), {
        submissionId: target.versions[0].id, approved, comment: topicComment,
      });
      setComments((current) => ({ ...current, [String(target.topicId)]: '' })); await load();
    } catch (requestError) { setError(getApiErrorMessage(requestError, 'Không thể duyệt bản final.')); }
    finally { setBusy(false); }
  };

  const openFile = async (version) => {
    setBusy(true); setError('');
    try {
      const blob = await ResourceService.downloadFile(API_ENDPOINTS.finalSubmissions.file(version.id));
      const url = URL.createObjectURL(new Blob([blob], { type: version.contentType }));
      if (version.contentType === 'application/pdf') setPreview({ url, name: version.fileName });
      else {
        const link = document.createElement('a'); link.href = url; link.download = version.fileName;
        document.body.appendChild(link); link.click(); link.remove();
        window.setTimeout(() => URL.revokeObjectURL(url), 1000);
      }
    } catch (requestError) { setError(getApiErrorMessage(requestError, 'Không thể mở bản final.')); }
    finally { setBusy(false); }
  };

  const openBackup = () => {
    setBackupPeriodId(periodId || String(periods[0]?.id || ''));
    setBackupOpen(true);
    setError('');
  };

  const downloadBackup = async () => {
    if (!backupPeriodId) { setError('Vui lòng chọn đợt bảo vệ cần sao lưu.'); return; }
    setBackupBusy(true); setError('');
    try {
      const blob = await ResourceService.downloadFile(API_ENDPOINTS.finalSubmissions.backup(backupPeriodId));
      const period = periods.find((item) => String(item.id) === backupPeriodId);
      const safeName = (period?.name || 'dot-bao-ve').replace(/[\\/:*?"<>|]/g, '_').trim() || 'dot-bao-ve';
      const url = URL.createObjectURL(new Blob([blob], { type: 'application/zip' }));
      const link = document.createElement('a');
      link.href = url; link.download = `${safeName}.zip`;
      document.body.appendChild(link); link.click(); link.remove();
      window.setTimeout(() => URL.revokeObjectURL(url), 1000);
      setBackupOpen(false);
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, 'Không thể tạo file backup cho đợt bảo vệ.'));
    } finally { setBackupBusy(false); }
  };

  return <div className="page-stack">
    <div className="page-heading"><div><span>{admin ? 'QUẢN LÝ BÁO CÁO SAU BẢO VỆ' : 'HOÀN TẤT HỒ SƠ SAU BẢO VỆ'}</span>
      <h1>{admin ? 'Duyệt báo cáo' : 'Nộp báo cáo bản cuối'}</h1>
      <p>{admin ? 'Kiểm tra báo cáo và liên kết source code hoặc sản phẩm trước khi công bố điểm cho sinh viên.' : 'Sau khi hội đồng chấm xong, nhóm nộp bản báo cáo và link source/sản phẩm cuối cùng để Admin duyệt.'}</p>
    </div>{admin && items.length > 0 && <button type="button" className="primary-button report-backup-button" onClick={openBackup}><Archive size={17}/> Backup</button>}</div>
    {error && <p className="report-error" role="alert">{error}</p>}
    {loading ? <section className="defense-report-panel"><p>Đang tải dữ liệu...</p></section>
      : !items.length ? <section className="defense-report-panel defense-report-empty"><header><h3>Chưa có hồ sơ cần xử lý</h3></header><p>{admin ? 'Chưa có nhóm nào hoàn tất chấm điểm.' : 'Bản final sẽ được mở sau khi thư ký hoàn tất bảng điểm hội đồng.'}</p></section>
      : <>
        {admin ? <>
          <label className="report-schedule-select">Lọc theo đợt bảo vệ
            <select value={periodId} onChange={(event) => setPeriodId(event.target.value)}>
              <option value="">Tất cả đợt bảo vệ</option>
              {periods.map((period) => <option value={period.id} key={period.id}>{period.name} · {period.year}</option>)}
            </select>
          </label>
          <div className="final-submission-list">
            {periodItems.map((item) => <section className="defense-report-panel" key={item.topicId}>
              <header><div><h3>{item.topicTitle}</h3><small>Nhóm: {item.teamName} · {item.periodName} · {item.academicYear}</small></div><span className={`report-badge ${item.status.toLowerCase()}`}>{labels[item.status]}</span></header>
              <p><strong>Hạn nộp:</strong> {formatDateTimeDisplay(item.deadline)} (giờ Việt Nam)<br/><strong>Trạng thái điểm:</strong> {item.scorePublished ? 'Đã công bố' : item.scoreCompleted ? 'Đã chấm, đang giữ chưa công bố' : 'Hội đồng chưa chấm xong'}</p>
              {!item.scorePublished && <p className="report-notice">Điểm chỉ được hiển thị cho sinh viên sau khi Admin duyệt bản báo cáo bản cuối. Quá hạn mà nhóm chưa nộp (hoặc chưa nộp lại theo yêu cầu), hệ thống tự gán và công bố 0 điểm. Bản đã nộp đúng hạn có thể được Admin duyệt sau hạn.</p>}
              {!item.versions.length && <p>Nhóm chưa nộp bản báo cáo bản cuối.</p>}
              {item.versions.map((version, index) => <article className="report-version" key={version.id}>
                <div><strong>Phiên bản {version.revision}{index === 0 ? ' · Mới nhất' : ''}</strong><small>{formatDateTimeDisplay(version.submittedAt)} · {labels[version.status]}</small></div>
                <button type="button" className="secondary-button" disabled={busy} onClick={() => openFile(version)}>{version.contentType === 'application/pdf' ? <Eye size={15}/> : <Download size={15}/>} {version.fileName}</button>
                <div className="report-links">{version.sourceUrl && <a href={version.sourceUrl} target="_blank" rel="noreferrer">Mở source code ↗</a>}{version.productUrl && <a href={version.productUrl} target="_blank" rel="noreferrer">Mở sản phẩm ↗</a>}</div>
                {version.note && <p>{version.note}</p>}{version.reviewComment && <p><strong>Admin:</strong> {version.reviewComment}</p>}
              </article>)}
              {item.canReview && <div className="report-review"><label>Nhận xét Admin<textarea value={comments[String(item.topicId)] || ''} onChange={(event) => setComments((current) => ({ ...current, [String(item.topicId)]: event.target.value }))} placeholder="Bắt buộc khi yêu cầu nộp lại" /></label><div><button type="button" className="secondary-button" disabled={busy || !(comments[String(item.topicId)] || '').trim()} onClick={() => review(false, item)}>Yêu cầu nộp lại</button><button type="button" className="primary-button" disabled={busy} onClick={() => review(true, item)}><CheckCircle2 size={16}/> Duyệt và công bố điểm</button></div></div>}
              {item.scorePublished && <p><FileCheck2 size={18}/> Hồ sơ đã hoàn tất; điểm đã được gán và công bố cho sinh viên.</p>}
            </section>)}
            {!periodItems.length && <section className="defense-report-panel defense-report-empty"><p>Đợt bảo vệ này chưa có đề tài cần xử lý.</p></section>}
          </div>
        </> : <>
        <label className="report-schedule-select">Đề tài / đợt bảo vệ
          <select value={selectedId} onChange={(event) => { setSelectedId(event.target.value); setError(''); }}>
            {items.map((item) => <option value={item.topicId} key={item.topicId}>{item.periodName} · {item.academicYear} · {item.topicTitle}</option>)}
          </select>
        </label>
        {selected && <section className="defense-report-panel">
          <header><div><h3>{selected.topicTitle}</h3><small>Nhóm: {selected.teamName}</small></div><span className={`report-badge ${selected.status.toLowerCase()}`}>{labels[selected.status]}</span></header>
          <p><strong>Hạn Admin cấu hình:</strong> {formatDateTimeDisplay(selected.deadline)} (giờ Việt Nam)<br/><strong>Trạng thái điểm:</strong> {selected.scorePublished ? 'Đã công bố' : selected.scoreCompleted ? 'Đã chấm, đang giữ chưa công bố' : 'Hội đồng chưa chấm xong'}</p>
          {!selected.scorePublished && <p className="report-notice">Điểm chỉ được hiển thị cho sinh viên sau khi Admin duyệt bản báo cáo bản cuối. Quá hạn mà nhóm chưa nộp (hoặc chưa nộp lại theo yêu cầu), hệ thống tự gán và công bố 0 điểm. Bản đã nộp đúng hạn có thể được Admin duyệt sau hạn.</p>}
          {selected.canSubmit && <form className="report-upload" onSubmit={submit}>
            <label>Báo cáo final PDF / DOCX<input required disabled={busy} type="file" accept=".pdf,.docx" onChange={(event) => setFile(event.target.files?.[0] || null)} /></label>
            <label>Link source code<input type="url" value={sourceUrl} onChange={(event) => setSourceUrl(event.target.value)} placeholder="https://github.com/..." /></label>
            <label>Link sản phẩm / demo<input type="url" value={productUrl} onChange={(event) => setProductUrl(event.target.value)} placeholder="https://..." /></label>
            <label>Ghi chú<textarea value={note} onChange={(event) => setNote(event.target.value)} /></label>
            <button className="primary-button" disabled={busy}>{busy ? <LoaderCircle className="spin" size={16}/> : <Upload size={16}/>} Nộp bản final</button>
          </form>}
          {!selected.versions.length && <p>Nhóm chưa nộp bản báo cáo final.</p>}
          {selected.versions.map((version, index) => <article className="report-version" key={version.id}>
            <div><strong>Phiên bản {version.revision}{index === 0 ? ' · Mới nhất' : ''}</strong><small>{formatDateTimeDisplay(version.submittedAt)} · {labels[version.status]}</small></div>
            <button type="button" className="secondary-button" disabled={busy} onClick={() => openFile(version)}>{version.contentType === 'application/pdf' ? <Eye size={15}/> : <Download size={15}/>} {version.fileName}</button>
            <div className="report-links">{version.sourceUrl && <a href={version.sourceUrl} target="_blank" rel="noreferrer">Mở source code ↗</a>}{version.productUrl && <a href={version.productUrl} target="_blank" rel="noreferrer">Mở sản phẩm ↗</a>}</div>
            {version.note && <p>{version.note}</p>}{version.reviewComment && <p><strong>Admin:</strong> {version.reviewComment}</p>}
          </article>)}
          {selected.scorePublished && <p><FileCheck2 size={18}/> Hồ sơ đã hoàn tất; điểm đã được gán và công bố cho sinh viên.</p>}
        </section>}
        </>}
      </>}
    {backupOpen && <div className="modal-backdrop report-backup-backdrop" onMouseDown={(event) => event.target === event.currentTarget && !backupBusy && setBackupOpen(false)}>
      <section className="report-backup-modal" role="dialog" aria-modal="true" aria-labelledby="backup-title">
        <header><div><span>SAO LƯU BÁO CÁO</span><h3 id="backup-title">Chọn đợt bảo vệ</h3></div><button type="button" onClick={() => setBackupOpen(false)} disabled={backupBusy} aria-label="Đóng"><X size={19}/></button></header>
        <div className="report-backup-body"><p>Tất cả các phiên bản báo cáo bản cuối và thông tin link source/demo trong đợt sẽ được đóng gói vào một file ZIP.</p><label>Đợt bảo vệ<select value={backupPeriodId} onChange={(event) => setBackupPeriodId(event.target.value)} disabled={backupBusy}><option value="">Chọn đợt bảo vệ</option>{periods.map((period) => <option value={period.id} key={period.id}>{period.name} · {period.year}</option>)}</select></label></div>
        <footer><button type="button" className="secondary-button" onClick={() => setBackupOpen(false)} disabled={backupBusy}>Hủy</button><button type="button" className="primary-button" onClick={downloadBackup} disabled={backupBusy || !backupPeriodId}>{backupBusy ? <LoaderCircle className="spin" size={16}/> : <Download size={16}/>} {backupBusy ? 'Đang tạo ZIP...' : 'Tải file ZIP'}</button></footer>
      </section>
    </div>}
    {preview && <div className="report-preview" role="dialog" aria-modal="true"><header><strong>{preview.name}</strong><button className="secondary-button" onClick={() => setPreview(null)}>Đóng</button></header><iframe title={preview.name} src={preview.url}/></div>}
  </div>;
}
