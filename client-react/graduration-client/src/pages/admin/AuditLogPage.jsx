import React, { useEffect, useMemo, useState } from 'react';
import { AlertCircle, CalendarRange, ChevronLeft, ChevronRight, Clock3, Download, Eye, Inbox, LoaderCircle, Search, ShieldCheck, X } from 'lucide-react';
import API_ENDPOINTS from '../../config/endpoints.js';
import ResourceService from '../../services/ResourceService.jsx';
import getApiErrorMessage from '../../utils/apiError.js';
import { formatDateTimeDisplay } from '../../utils/dateFormat.js';
import { useToast } from '../../context/ToastContext.jsx';
import '../../style/AuditLogPage.scss';
import '../../style/AuditLogExport.scss';

const PAGE_SIZE = 10;

export default function AuditLogPage() {
  const toast = useToast();
  const [logs, setLogs] = useState([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [query, setQuery] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [detail, setDetail] = useState(null);
  const [detailLoading, setDetailLoading] = useState(false);
  const [exportOpen, setExportOpen] = useState(false);
  const [exporting, setExporting] = useState(false);
  const [exportError, setExportError] = useState('');
  const [exportRange, setExportRange] = useState(initialExportRange);

  const load = async () => {
    setLoading(true);
    setError('');
    try {
      const result = await ResourceService.getPage(API_ENDPOINTS.auditLogs.list, { page, size: PAGE_SIZE });
      setLogs(result.content);
      setTotalPages(result.totalPages);
      setTotalElements(result.totalElements);
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, 'Không thể tải nhật ký hoạt động.'));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(); }, [page]); // eslint-disable-line react-hooks/exhaustive-deps

  const visibleLogs = useMemo(() => {
    const keyword = normalize(query);
    if (!keyword) return logs;
    return logs.filter((log) => normalize(`${log.userName} ${log.userId} ${log.action} ${log.resourceType} ${log.resourceId} ${log.description} ${log.ipAddress}`).includes(keyword));
  }, [logs, query]);

  const openDetail = async (log) => {
    setDetail(log);
    setDetailLoading(true);
    try {
      setDetail(await ResourceService.getOne(API_ENDPOINTS.auditLogs.detail(log.auditLogId)));
    } catch (requestError) {
      setDetail({ ...log, detailError: getApiErrorMessage(requestError, 'Không thể tải chi tiết nhật ký.') });
    } finally {
      setDetailLoading(false);
    }
  };

  const openExport = () => {
    setExportRange(initialExportRange());
    setExportError('');
    setExportOpen(true);
  };

  const exportExcel = async (event) => {
    event.preventDefault();
    if (!exportRange.fromDate || !exportRange.toDate) {
      setExportError('Vui lòng chọn đầy đủ thời gian bắt đầu và kết thúc.');
      return;
    }
    if (exportRange.fromDate > exportRange.toDate) {
      setExportError('Thời gian bắt đầu không được sau thời gian kết thúc.');
      return;
    }
    setExporting(true); setExportError('');
    try {
      const blob = await ResourceService.downloadFile(API_ENDPOINTS.auditLogs.export, exportRange);
      const source = blob instanceof Blob ? blob : new Blob([blob], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' });
      const url = URL.createObjectURL(source);
      const anchor = document.createElement('a');
      anchor.href = url;
      anchor.download = `nhat-ky-hoat-dong_${exportRange.fromDate}_den_${exportRange.toDate}.xlsx`;
      document.body.appendChild(anchor);
      anchor.click();
      anchor.remove();
      URL.revokeObjectURL(url);
      setExportOpen(false);
      toast.success('Đã xuất nhật ký hoạt động.');
    } catch (requestError) {
      setExportError(await exportErrorMessage(requestError));
    } finally { setExporting(false); }
  };

  return <div className="page-stack audit-page">
    <section className="page-title-row"><div><span className="page-kicker">GIÁM SÁT HỆ THỐNG</span><h2>Nhật ký hoạt động</h2><p>Theo dõi các thao tác tạo, cập nhật, xóa và thay đổi dữ liệu trong hệ thống.</p></div><button className="secondary-button audit-export-button" onClick={openExport}><Download size={17}/> Xuất Excel</button></section>
    <section className="audit-summary"><article><ShieldCheck size={21} /><div><span>Tổng số nhật ký</span><strong>{totalElements}</strong></div></article><article><Clock3 size={21} /><div><span>Cập nhật gần nhất</span><strong>{logs[0]?.createdAt ? formatDate(logs[0].createdAt) : '—'}</strong></div></article></section>
    <section className="panel data-panel">
      <div className="table-toolbar"><label className="table-search"><Search size={18} /><input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Tìm theo tài khoản, hành động, đối tượng hoặc IP..." />{query && <button className="search-clear" onClick={() => setQuery('')}><X size={15} /></button>}</label></div>
      {loading ? <AuditState icon={LoaderCircle} text="Đang tải nhật ký hoạt động..." spin /> : error ? <AuditState icon={AlertCircle} text={error} action={load} /> : visibleLogs.length === 0 ? <AuditState icon={Inbox} text={query ? 'Không tìm thấy nhật ký phù hợp trên trang này.' : 'Chưa có nhật ký hoạt động.'} /> : <><div className="table-scroll"><table><thead><tr><th>Thời gian</th><th>Người thực hiện</th><th>Hành động</th><th>Đối tượng</th><th>Địa chỉ IP</th><th /></tr></thead><tbody>{visibleLogs.map((log) => <tr key={log.auditLogId}><td data-label="Thời gian"><strong>{formatDate(log.createdAt)}</strong></td><td data-label="Người thực hiện"><strong>{log.userName || 'Hệ thống'}</strong><small>{log.userId || '—'}</small></td><td data-label="Hành động"><span className={`audit-action ${actionClass(log.action)}`}>{actionLabel(log.action)}</span><small>{log.description || '—'}</small></td><td data-label="Đối tượng"><strong>{resourceLabel(log.resourceType)}</strong><small>{log.resourceId || 'Không có mã'}</small></td><td data-label="Địa chỉ IP">{log.ipAddress || '—'}</td><td className="table-actions"><button className="view" onClick={() => openDetail(log)} title="Xem chi tiết nhật ký"><Eye size={15} /></button></td></tr>)}</tbody></table></div><div className="table-footer"><span>Hiển thị {visibleLogs.length} trong tổng số {totalElements} nhật ký</span><div><button disabled={page === 0} onClick={() => setPage((value) => value - 1)}><ChevronLeft size={14} /> Trước</button><button className="active">{page + 1}</button><button disabled={page >= totalPages - 1} onClick={() => setPage((value) => value + 1)}>Sau <ChevronRight size={14} /></button></div></div></>}
    </section>
    {detail && <AuditDetail log={detail} loading={detailLoading} onClose={() => !detailLoading && setDetail(null)} />}
    {exportOpen && <div className="modal-backdrop" onMouseDown={(event) => event.target === event.currentTarget && !exporting && setExportOpen(false)}><section className="audit-export-modal" role="dialog" aria-modal="true" aria-labelledby="audit-export-title"><header><div className="audit-export-heading"><span><CalendarRange size={20}/></span><div><small>XUẤT DỮ LIỆU</small><h3 id="audit-export-title">Chọn thời gian xuất nhật ký</h3></div></div><button type="button" disabled={exporting} onClick={() => setExportOpen(false)} aria-label="Đóng"><X size={19}/></button></header><form onSubmit={exportExcel}><div className="audit-export-fields"><label>Từ ngày<b>*</b><input required type="date" value={exportRange.fromDate} max={exportRange.toDate || undefined} onChange={(event) => setExportRange((current) => ({ ...current, fromDate: event.target.value }))}/></label><label>Đến ngày<b>*</b><input required type="date" value={exportRange.toDate} min={exportRange.fromDate || undefined} onChange={(event) => setExportRange((current) => ({ ...current, toDate: event.target.value }))}/></label></div><p className="audit-export-note">File Excel bao gồm toàn bộ nhật ký phát sinh trong khoảng thời gian đã chọn, tính trọn ngày bắt đầu và ngày kết thúc.</p>{exportError && <div className="audit-export-error"><AlertCircle size={16}/>{exportError}</div>}<footer><button type="button" className="secondary-button" disabled={exporting} onClick={() => setExportOpen(false)}>Hủy</button><button type="submit" className="primary-button" disabled={exporting || !exportRange.fromDate || !exportRange.toDate}>{exporting ? <><LoaderCircle className="spin" size={16}/> Đang xuất...</> : <><Download size={16}/> Xuất file Excel</>}</button></footer></form></section></div>}
  </div>;
}

function AuditDetail({ log, loading, onClose }) {
  const metadata = log.metadata && typeof log.metadata === 'object' ? Object.entries(log.metadata) : [];
  return <div className="modal-backdrop" onMouseDown={(event) => event.target === event.currentTarget && onClose()}><section className="audit-detail-modal" role="dialog" aria-modal="true"><header><div><span>CHI TIẾT NHẬT KÝ</span><h3>{actionLabel(log.action)} · {resourceLabel(log.resourceType)}</h3></div><button disabled={loading} onClick={onClose}><X size={20} /></button></header>{loading ? <AuditState icon={LoaderCircle} text="Đang tải chi tiết..." spin /> : <div className="audit-detail-body">{log.detailError && <div className="audit-detail-error"><AlertCircle size={16} />{log.detailError}</div>}<div className="audit-detail-grid"><div><span>Thời gian</span><strong>{formatDate(log.createdAt)}</strong></div><div><span>Hành động</span><strong>{actionLabel(log.action)}</strong></div><div><span>Người thực hiện</span><strong>{log.userName || 'Hệ thống'}</strong><small>{log.userId || '—'}</small></div><div><span>Địa chỉ IP</span><strong>{log.ipAddress || '—'}</strong></div><div><span>Loại đối tượng</span><strong>{resourceLabel(log.resourceType)}</strong></div><div><span>Mã đối tượng</span><strong>{log.resourceId || '—'}</strong></div></div><section><span>MÔ TẢ</span><p>{log.description || 'Không có mô tả.'}</p></section><section><span>THÔNG TIN KỸ THUẬT</span>{metadata.length ? <div className="audit-metadata">{metadata.map(([key, value]) => <div key={key}><code>{key}</code><span>{formatMetadata(value)}</span></div>)}</div> : <p>Không có metadata.</p>}</section><div className="audit-log-id">Mã nhật ký: <code>{log.auditLogId}</code></div></div>}<footer><button className="primary-button" onClick={onClose} disabled={loading}>Đóng</button></footer></section></div>;
}

function AuditState({ icon: Icon, text, action, spin }) { return <div className="data-state"><Icon className={spin ? 'spin' : ''} size={28} /><strong>{text}</strong>{action && <button onClick={action}>Thử lại</button>}</div>; }
function normalize(value) { return String(value || '').normalize('NFD').replace(/[\u0300-\u036f]/g, '').replace(/đ/g, 'd').replace(/Đ/g, 'D').toLowerCase().trim(); }
function formatDate(value) { return formatDateTimeDisplay(value, { includeSeconds: true }); }
function actionLabel(value) { const action = String(value || '').toUpperCase(); const labels = { CREATE: 'Tạo mới', UPDATE: 'Cập nhật', DELETE: 'Xóa', IMPORT: 'Import', ASSIGN: 'Phân công', DEACTIVATE: 'Ngừng hoạt động', SELECT: 'Lựa chọn', SUBMIT: 'Gửi duyệt', APPROVE: 'Phê duyệt', REJECT: 'Từ chối', UPLOAD: 'Tải lên', REVIEW: 'Đánh giá', REVISE: 'Yêu cầu chỉnh sửa', CANCEL: 'Hủy', OPEN: 'Mở', CLOSE: 'Đóng', PUBLISH: 'Công bố', START: 'Bắt đầu', ADD: 'Thêm', REMOVE: 'Gỡ bỏ', RESET: 'Đặt lại', REGISTER: 'Đăng ký', FINISH: 'Kết thúc' }; const prefix = Object.keys(labels).find((key) => action === key || action.startsWith(`${key}_`)); return prefix ? labels[prefix] : value || 'Không xác định'; }
function actionClass(value) { const action = String(value || '').toLowerCase(); return action.includes('delete') || action.includes('deactivate') ? 'danger' : action.includes('create') || action.includes('import') ? 'success' : 'info'; }
function resourceLabel(value) { if (!value) return 'Không xác định'; return String(value).replace(/([a-z])([A-Z])/g, '$1 $2').replace(/_/g, ' '); }
function formatMetadata(value) { if (value === null || value === undefined) return '—'; return typeof value === 'object' ? JSON.stringify(value, null, 2) : String(value); }

function toInputDate(date) {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

function initialExportRange() {
  const today = new Date();
  return { fromDate: toInputDate(new Date(today.getFullYear(), today.getMonth(), 1)), toDate: toInputDate(today) };
}

async function exportErrorMessage(error) {
  if (error instanceof Blob) {
    try {
      const payload = JSON.parse(await error.text());
      return payload.message || payload.mesage || 'Không thể xuất nhật ký hoạt động.';
    } catch {
      return 'Không thể xuất nhật ký hoạt động.';
    }
  }
  return getApiErrorMessage(error, 'Không thể xuất nhật ký hoạt động.');
}
