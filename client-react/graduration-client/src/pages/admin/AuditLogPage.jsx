import React, { useEffect, useMemo, useState } from 'react';
import { AlertCircle, ChevronLeft, ChevronRight, Clock3, Eye, Inbox, LoaderCircle, Search, ShieldCheck, X } from 'lucide-react';
import API_ENDPOINTS from '../../config/endpoints.js';
import ResourceService from '../../services/ResourceService.jsx';
import getApiErrorMessage from '../../utils/apiError.js';
import '../../style/AuditLogPage.scss';

const PAGE_SIZE = 10;

export default function AuditLogPage() {
  const [logs, setLogs] = useState([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [query, setQuery] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [detail, setDetail] = useState(null);
  const [detailLoading, setDetailLoading] = useState(false);

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

  return <div className="page-stack audit-page">
    <section className="page-title-row"><div><span className="page-kicker">GIÁM SÁT HỆ THỐNG</span><h2>Nhật ký hoạt động</h2><p>Theo dõi các thao tác tạo, cập nhật, xóa và thay đổi dữ liệu trong hệ thống.</p></div></section>
    <section className="audit-summary"><article><ShieldCheck size={21} /><div><span>Tổng số nhật ký</span><strong>{totalElements}</strong></div></article><article><Clock3 size={21} /><div><span>Cập nhật gần nhất</span><strong>{logs[0]?.createdAt ? formatDate(logs[0].createdAt) : '—'}</strong></div></article></section>
    <section className="panel data-panel">
      <div className="table-toolbar"><label className="table-search"><Search size={18} /><input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Tìm theo tài khoản, hành động, đối tượng hoặc IP..." />{query && <button className="search-clear" onClick={() => setQuery('')}><X size={15} /></button>}</label></div>
      {loading ? <AuditState icon={LoaderCircle} text="Đang tải nhật ký hoạt động..." spin /> : error ? <AuditState icon={AlertCircle} text={error} action={load} /> : visibleLogs.length === 0 ? <AuditState icon={Inbox} text={query ? 'Không tìm thấy nhật ký phù hợp trên trang này.' : 'Chưa có nhật ký hoạt động.'} /> : <><div className="table-scroll"><table><thead><tr><th>Thời gian</th><th>Người thực hiện</th><th>Hành động</th><th>Đối tượng</th><th>Địa chỉ IP</th><th /></tr></thead><tbody>{visibleLogs.map((log) => <tr key={log.auditLogId}><td data-label="Thời gian"><strong>{formatDate(log.createdAt)}</strong></td><td data-label="Người thực hiện"><strong>{log.userName || 'Hệ thống'}</strong><small>{log.userId || '—'}</small></td><td data-label="Hành động"><span className={`audit-action ${actionClass(log.action)}`}>{actionLabel(log.action)}</span><small>{log.description || '—'}</small></td><td data-label="Đối tượng"><strong>{resourceLabel(log.resourceType)}</strong><small>{log.resourceId || 'Không có mã'}</small></td><td data-label="Địa chỉ IP">{log.ipAddress || '—'}</td><td className="table-actions"><button className="view" onClick={() => openDetail(log)} title="Xem chi tiết nhật ký"><Eye size={15} /></button></td></tr>)}</tbody></table></div><div className="table-footer"><span>Hiển thị {visibleLogs.length} trong tổng số {totalElements} nhật ký</span><div><button disabled={page === 0} onClick={() => setPage((value) => value - 1)}><ChevronLeft size={14} /> Trước</button><button className="active">{page + 1}</button><button disabled={page >= totalPages - 1} onClick={() => setPage((value) => value + 1)}>Sau <ChevronRight size={14} /></button></div></div></>}
    </section>
    {detail && <AuditDetail log={detail} loading={detailLoading} onClose={() => !detailLoading && setDetail(null)} />}
  </div>;
}

function AuditDetail({ log, loading, onClose }) {
  const metadata = log.metadata && typeof log.metadata === 'object' ? Object.entries(log.metadata) : [];
  return <div className="modal-backdrop" onMouseDown={(event) => event.target === event.currentTarget && onClose()}><section className="audit-detail-modal" role="dialog" aria-modal="true"><header><div><span>CHI TIẾT NHẬT KÝ</span><h3>{actionLabel(log.action)} · {resourceLabel(log.resourceType)}</h3></div><button disabled={loading} onClick={onClose}><X size={20} /></button></header>{loading ? <AuditState icon={LoaderCircle} text="Đang tải chi tiết..." spin /> : <div className="audit-detail-body">{log.detailError && <div className="audit-detail-error"><AlertCircle size={16} />{log.detailError}</div>}<div className="audit-detail-grid"><div><span>Thời gian</span><strong>{formatDate(log.createdAt)}</strong></div><div><span>Hành động</span><strong>{actionLabel(log.action)}</strong></div><div><span>Người thực hiện</span><strong>{log.userName || 'Hệ thống'}</strong><small>{log.userId || '—'}</small></div><div><span>Địa chỉ IP</span><strong>{log.ipAddress || '—'}</strong></div><div><span>Loại đối tượng</span><strong>{resourceLabel(log.resourceType)}</strong></div><div><span>Mã đối tượng</span><strong>{log.resourceId || '—'}</strong></div></div><section><span>MÔ TẢ</span><p>{log.description || 'Không có mô tả.'}</p></section><section><span>THÔNG TIN KỸ THUẬT</span>{metadata.length ? <div className="audit-metadata">{metadata.map(([key, value]) => <div key={key}><code>{key}</code><span>{formatMetadata(value)}</span></div>)}</div> : <p>Không có metadata.</p>}</section><div className="audit-log-id">Mã nhật ký: <code>{log.auditLogId}</code></div></div>}<footer><button className="primary-button" onClick={onClose} disabled={loading}>Đóng</button></footer></section></div>;
}

function AuditState({ icon: Icon, text, action, spin }) { return <div className="data-state"><Icon className={spin ? 'spin' : ''} size={28} /><strong>{text}</strong>{action && <button onClick={action}>Thử lại</button>}</div>; }
function normalize(value) { return String(value || '').normalize('NFD').replace(/[\u0300-\u036f]/g, '').replace(/đ/g, 'd').replace(/Đ/g, 'D').toLowerCase().trim(); }
function formatDate(value) { if (!value) return '—'; const date = new Date(value); return Number.isNaN(date.getTime()) ? value : new Intl.DateTimeFormat('vi-VN', { dateStyle: 'short', timeStyle: 'medium' }).format(date); }
function actionLabel(value) { const action = String(value || '').toUpperCase(); const labels = { CREATE: 'Tạo mới', UPDATE: 'Cập nhật', DELETE: 'Xóa', IMPORT: 'Import', ASSIGN: 'Phân công', DEACTIVATE: 'Ngừng hoạt động', SELECT: 'Lựa chọn', SUBMIT: 'Gửi duyệt', APPROVE: 'Phê duyệt', REJECT: 'Từ chối', UPLOAD: 'Tải lên', REVIEW: 'Đánh giá', REVISE: 'Yêu cầu chỉnh sửa', CANCEL: 'Hủy', OPEN: 'Mở', CLOSE: 'Đóng', PUBLISH: 'Công bố', START: 'Bắt đầu', ADD: 'Thêm', REMOVE: 'Gỡ bỏ', RESET: 'Đặt lại', REGISTER: 'Đăng ký', FINISH: 'Kết thúc' }; const prefix = Object.keys(labels).find((key) => action === key || action.startsWith(`${key}_`)); return prefix ? labels[prefix] : value || 'Không xác định'; }
function actionClass(value) { const action = String(value || '').toLowerCase(); return action.includes('delete') || action.includes('deactivate') ? 'danger' : action.includes('create') || action.includes('import') ? 'success' : 'info'; }
function resourceLabel(value) { if (!value) return 'Không xác định'; return String(value).replace(/([a-z])([A-Z])/g, '$1 $2').replace(/_/g, ' '); }
function formatMetadata(value) { if (value === null || value === undefined) return '—'; return typeof value === 'object' ? JSON.stringify(value, null, 2) : String(value); }
