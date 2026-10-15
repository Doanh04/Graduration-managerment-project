import React, { useEffect, useMemo, useState } from 'react';
import { AlertCircle, BookOpen, ChevronLeft, ChevronRight, Download, Edit3, ExternalLink, Eye, FileText, Inbox, LoaderCircle, Plus, Search, Trash2, Upload, X } from 'lucide-react';
import LibraryTopicService from '../../services/LibraryTopicService.js';
import { useToast } from '../../context/ToastContext.jsx';
import getApiErrorMessage from '../../utils/apiError.js';
import ConfirmModal from '../../components/feedback/ConfirmModal.jsx';
import downloadLibraryTopicPdf from '../../utils/libraryTopicPdf.js';

const emptyForm = { title: '', description: '', objective: '', technology: '', file: null };
const maxFileSize = 10 * 1024 * 1024;

export default function LibraryTopicPage({ canManage = false }) {
  const toast = useToast();
  const [items, setItems] = useState([]);
  const [page, setPage] = useState(0);
  const [query, setQuery] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [modal, setModal] = useState(null);
  const [confirm, setConfirm] = useState(null);
  const [form, setForm] = useState(emptyForm);
  const [saving, setSaving] = useState(false);
  const [fileActionId, setFileActionId] = useState(null);
  const [exportingId, setExportingId] = useState(null);
  const [formError, setFormError] = useState('');
  const [preview, setPreview] = useState(null);

  const load = async () => {
    setLoading(true);
    setError('');
    try {
      setItems(await LibraryTopicService.listAll());
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, 'Không thể tải thư viện đề tài.'));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(); }, []); // eslint-disable-line react-hooks/exhaustive-deps
  useEffect(() => { setPage(0); }, [query]);
  useEffect(() => () => {
    if (preview?.source) URL.revokeObjectURL(preview.source);
  }, [preview?.source]);

  const filtered = useMemo(() => {
    const term = normalizeSearch(query);
    return items.filter((item) => normalizeSearch(`${item.title} ${item.description || ''} ${item.objective || ''} ${item.technology || ''} ${item.fileName || ''}`).includes(term));
  }, [items, query]);
  const totalPages = Math.ceil(filtered.length / 9);
  const visibleItems = filtered.slice(page * 9, page * 9 + 9);

  const openCreate = () => {
    setForm(emptyForm);
    setFormError('');
    setModal({ mode: 'create' });
  };

  const openDetail = async (item, mode) => {
    setFormError('');
    setModal({ mode: 'loading', item });
    try {
      const response = await LibraryTopicService.detail(item.idLibraryTopic);
      const detail = response?.result || item;
      setForm({ title: detail.title || '', description: detail.description || '', objective: detail.objective || '', technology: detail.technology || '', file: null });
      setModal({ mode, item: detail });
    } catch (requestError) {
      setModal(null);
      setError(getApiErrorMessage(requestError, 'Không thể tải chi tiết đề tài.'));
    }
  };

  const submit = async (event) => {
    event.preventDefault();
    if (!form.title.trim()) {
      const message = 'Tên đề tài không được để trống.';
      setFormError(message);
      toast.error(message, { title: 'Dữ liệu chưa hợp lệ' });
      return;
    }
    const fileError = validateSelectedFile(form.file);
    if (fileError) {
      setFormError(fileError);
      toast.error(fileError, { title: 'Tệp chưa hợp lệ' });
      return;
    }
    setSaving(true);
    setFormError('');
    try {
      const editing = modal.mode === 'edit';
      if (editing) await LibraryTopicService.update(modal.item.idLibraryTopic, form);
      else await LibraryTopicService.create(form);
      setModal(null);
      toast.success(editing ? 'Cập nhật đề tài tham khảo thành công.' : 'Thêm đề tài tham khảo thành công.');
      await load();
    } catch (requestError) {
      const message = getApiErrorMessage(requestError, 'Không thể lưu đề tài tham khảo.');
      setFormError(message);
      toast.error(message, { title: 'Dữ liệu chưa hợp lệ' });
    } finally {
      setSaving(false);
    }
  };

  const remove = async (item) => {
    setSaving(true);
    try {
      await LibraryTopicService.remove(item.idLibraryTopic);
      setConfirm(null);
      toast.success('Xóa đề tài tham khảo thành công.');
      await load();
    } catch (requestError) {
      const message = getApiErrorMessage(requestError, 'Không thể xóa đề tài.');
      setError(message);
      toast.error(message);
    } finally {
      setSaving(false);
    }
  };

  const openPreview = async (item) => {
    setPreview({ item, source: '', loading: true, error: '' });
    try {
      const data = await LibraryTopicService.file(item.idLibraryTopic);
      const blob = data instanceof Blob ? data : new Blob([data], { type: 'application/pdf' });
      if (!blob.size) throw new Error('Tệp xem trước không có dữ liệu.');
      const pdfBlob = blob.type === 'application/pdf' ? blob : new Blob([blob], { type: 'application/pdf' });
      setPreview({ item, source: URL.createObjectURL(pdfBlob), loading: false, error: '' });
    } catch (requestError) {
      setPreview({ item, source: '', loading: false, error: getApiErrorMessage(requestError, 'Không thể tải bản xem trước PDF.') });
    }
  };

  const downloadFile = async (item) => {
    setFileActionId(item.idLibraryTopic);
    try {
      const blob = await LibraryTopicService.file(item.idLibraryTopic);
      const url = URL.createObjectURL(blob);
      const anchor = document.createElement('a');
      anchor.href = url;
      anchor.download = item.fileName || 'tep-thu-vien-de-tai';
      document.body.appendChild(anchor);
      anchor.click();
      anchor.remove();
      setTimeout(() => URL.revokeObjectURL(url), 0);
    } catch (requestError) {
      toast.error(getApiErrorMessage(requestError, 'Không thể tải tệp đính kèm.'));
    } finally {
      setFileActionId(null);
    }
  };

  const openFileInNewTab = async (item) => {
    const newTab = window.open('about:blank', '_blank');
    if (!newTab) {
      toast.error('Trình duyệt đang chặn cửa sổ mới. Vui lòng cho phép popup rồi thử lại.');
      return;
    }
    newTab.opener = null;
    newTab.document.title = 'Đang tải tệp...';
    setFileActionId(item.idLibraryTopic);
    try {
      const data = await LibraryTopicService.file(item.idLibraryTopic);
      const blob = data instanceof Blob ? data : new Blob([data], { type: item.contentType || 'application/octet-stream' });
      const url = URL.createObjectURL(blob);
      newTab.location.replace(url);
      window.setTimeout(() => URL.revokeObjectURL(url), 60_000);
    } catch (requestError) {
      newTab.close();
      toast.error(getApiErrorMessage(requestError, 'Không thể mở tệp đính kèm trong tab mới.'));
    } finally {
      setFileActionId(null);
    }
  };

  const exportTopicPdf = async (item) => {
    setExportingId(item.idLibraryTopic);
    try {
      const response = await LibraryTopicService.detail(item.idLibraryTopic);
      await downloadLibraryTopicPdf(response?.result || item);
      toast.success('Đã xuất nội dung đề tài thành PDF.');
    } catch (requestError) {
      toast.error(getApiErrorMessage(requestError, 'Không thể xuất nội dung đề tài thành PDF.'));
    } finally {
      setExportingId(null);
    }
  };

  const confirmAction = () => {
    if (confirm.type === 'edit') {
      const item = confirm.item;
      setConfirm(null);
      openDetail(item, 'edit');
    } else remove(confirm.item);
  };

  return <div className="page-stack library-page">
    <section className="page-title-row"><div><span className="page-kicker">KHO TRI THỨC ĐỒ ÁN</span><h2>Thư viện đề tài tham khảo</h2><p>Tra cứu ý tưởng, mục tiêu, công nghệ và tài liệu từ các đề tài được lưu trong hệ thống.</p></div>{canManage && <button className="primary-button" onClick={openCreate}><Plus size={18} /> Thêm đề tài</button>}</section>
    <section className="library-toolbar panel"><label><Search size={18} /><input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Tìm theo tên, mô tả, công nghệ hoặc tên tệp..." /></label><span>{filtered.length} đề tài phù hợp</span></section>
    {loading ? <State icon={LoaderCircle} text="Đang tải thư viện đề tài..." spin /> : error ? <State icon={AlertCircle} text={error} action={load} /> : filtered.length === 0 ? <State icon={Inbox} text="Chưa có đề tài tham khảo phù hợp." /> : <section className="library-grid">{visibleItems.map((item) => <article className="library-card" key={item.idLibraryTopic}>
      <div className="library-card-icon"><BookOpen size={21} /></div>
      <div className="library-card-body"><span>ĐỀ TÀI #{item.idLibraryTopic}</span><h3>{item.title}</h3><p>{item.description || 'Chưa có mô tả cho đề tài này.'}</p><div className="tech-tags">{splitTech(item.technology).map((tech) => <i key={tech}>{tech}</i>)}</div>{item.fileName && <div className="library-file-meta"><FileText size={14} /><span title={item.fileName}>{item.fileName}</span><small>{formatBytes(item.fileSize)}</small></div>}</div>
      <div className="library-card-actions"><button onClick={() => openDetail(item, 'view')}><Eye size={16} /> Chi tiết</button><button className="export-topic" disabled={exportingId === item.idLibraryTopic} onClick={() => exportTopicPdf(item)}>{exportingId === item.idLibraryTopic ? <LoaderCircle className="spin" size={15} /> : <Download size={15} />} Xuất PDF</button>{item.fileName && isPdf(item) && <button className="preview-topic" onClick={() => openPreview(item)}><FileText size={15} /> Xem tệp</button>}{canManage && <><button className="icon-action" onClick={() => setConfirm({ type: 'edit', item })} aria-label="Sửa"><Edit3 size={16} /></button><button className="icon-action danger" onClick={() => setConfirm({ type: 'delete', item })} aria-label="Xóa"><Trash2 size={16} /></button></>}</div>
    </article>)}</section>}
    {!loading && !error && totalPages > 1 && <div className="library-pagination"><button disabled={page === 0} onClick={() => setPage((value) => value - 1)}><ChevronLeft size={17} /></button><span>Trang {page + 1}/{totalPages}</span><button disabled={page >= totalPages - 1} onClick={() => setPage((value) => value + 1)}><ChevronRight size={17} /></button></div>}

    {modal && <div className="modal-backdrop" role="presentation" onMouseDown={(event) => event.target === event.currentTarget && setModal(null)}><section className="topic-modal" role="dialog" aria-modal="true"><header><div><span>{modal.mode === 'create' ? 'THÊM MỚI' : modal.mode === 'edit' ? 'CHỈNH SỬA' : 'CHI TIẾT'}</span><h3>Đề tài tham khảo</h3></div><button onClick={() => setModal(null)}><X size={20} /></button></header>{modal.mode === 'loading' ? <div className="data-state"><LoaderCircle className="spin" size={28} /><strong>Đang tải chi tiết...</strong></div> : <form onSubmit={submit}>
      <label>Tên đề tài <b>*</b><input disabled={modal.mode === 'view'} value={form.title} onChange={(event) => setForm({ ...form, title: event.target.value })} placeholder="Nhập tên đề tài" /></label>
      <label>Mô tả<textarea disabled={modal.mode === 'view'} value={form.description} onChange={(event) => setForm({ ...form, description: event.target.value })} placeholder="Mô tả bài toán và phạm vi" /></label>
      <label>Mục tiêu<textarea disabled={modal.mode === 'view'} value={form.objective} onChange={(event) => setForm({ ...form, objective: event.target.value })} placeholder="Mục tiêu cần đạt được" /></label>
      <label>Công nghệ<textarea disabled={modal.mode === 'view'} value={form.technology} onChange={(event) => setForm({ ...form, technology: event.target.value })} placeholder="Ví dụ: React, Spring Boot, MySQL" /></label>
      <label className="library-file-field">Tệp tài liệu{modal.mode !== 'view' && <><div className="library-file-input"><Upload size={17} /><input type="file" accept=".pdf,.xls,.xlsx" onChange={(event) => setForm({ ...form, file: event.target.files?.[0] || null })} /></div><small>Chấp nhận PDF, XLS hoặc XLSX; dung lượng tối đa 10 MB.{modal.mode === 'edit' && modal.item.fileName ? ' Để trống nếu muốn giữ tệp hiện tại.' : ''}</small></>}{modal.item?.fileName && <div className="library-current-file"><FileText size={16} /><span><strong>{modal.item.fileName}</strong><small>{formatBytes(modal.item.fileSize)}</small></span>{isPdf(modal.item) && <button type="button" onClick={() => openPreview(modal.item)}>Xem trước</button>}<button type="button" disabled={fileActionId === modal.item.idLibraryTopic} onClick={() => openFileInNewTab(modal.item)}><ExternalLink size={14} /> Mở tab mới</button><button type="button" disabled={fileActionId === modal.item.idLibraryTopic} onClick={() => downloadFile(modal.item)}>Tải xuống</button></div>}{modal.mode === 'view' && !modal.item?.fileName && <small>Đề tài này chưa có tệp đính kèm.</small>}</label>
      {formError && <div className="form-error"><AlertCircle size={16} />{formError}</div>}
      <footer><button type="button" className="secondary-button" onClick={() => setModal(null)}>{modal.mode === 'view' ? 'Đóng' : 'Hủy'}</button>{modal.mode !== 'view' && <button className="primary-button" disabled={saving}>{saving ? 'Đang lưu...' : 'Lưu đề tài'}</button>}</footer>
    </form>}</section></div>}

    {preview && <div className="modal-backdrop" onMouseDown={(event) => event.target === event.currentTarget && setPreview(null)}><section className="file-preview-modal" role="dialog" aria-modal="true"><header><div><span>XEM TRƯỚC TỆP PDF</span><h3>{preview.item.fileName || preview.item.title}</h3></div><button onClick={() => setPreview(null)} aria-label="Đóng"><X size={20} /></button></header>{preview.loading ? <div className="file-preview-loading"><LoaderCircle className="spin" size={30} /><span>Đang tải bản xem trước...</span></div> : preview.error ? <div className="file-preview-error"><AlertCircle size={30} /><strong>{preview.error}</strong><button className="secondary-button" onClick={() => openPreview(preview.item)}>Thử lại</button></div> : <div className="file-preview-frame"><iframe src={preview.source} title={`Xem trước ${preview.item.fileName}`} /></div>}<footer><span>Tệp được đọc trực tiếp từ cơ sở dữ liệu và chỉ hiển thị cho tài khoản đã đăng nhập.</span><div className="library-preview-actions"><button className="secondary-button" onClick={() => downloadFile(preview.item)}><Download size={15} /> Tải xuống</button><button className="secondary-button" onClick={() => setPreview(null)}>Đóng</button></div></footer></section></div>}

    <ConfirmModal open={Boolean(confirm)} type={confirm?.type} title={confirm?.type === 'delete' ? 'Xóa đề tài tham khảo?' : 'Chỉnh sửa đề tài?'} message={confirm?.type === 'delete' ? `Đề tài “${confirm?.item?.title || ''}” sẽ bị xóa khỏi thư viện.` : `Bạn sắp thay đổi nội dung đề tài “${confirm?.item?.title || ''}”.`} confirmLabel={confirm?.type === 'delete' ? 'Xóa đề tài' : 'Tiếp tục sửa'} loading={saving} onCancel={() => setConfirm(null)} onConfirm={confirmAction} />
  </div>;
}

function State({ icon: Icon, text, action, spin }) {
  return <section className="panel data-state"><Icon className={spin ? 'spin' : ''} size={29} /><strong>{text}</strong>{action && <button onClick={action}>Thử lại</button>}</section>;
}

function splitTech(value) {
  return value ? value.split(/[,;/]/).map((item) => item.trim()).filter(Boolean).slice(0, 4) : ['Chưa cập nhật công nghệ'];
}

function normalizeSearch(value) {
  return String(value ?? '').normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLocaleLowerCase('vi').trim();
}

function isPdf(item) {
  return String(item?.contentType || '').toLowerCase().includes('pdf') || String(item?.fileName || '').toLowerCase().endsWith('.pdf');
}

function validateSelectedFile(file) {
  if (!file) return '';
  const extension = file.name.split('.').pop()?.toLowerCase();
  if (!['pdf', 'xls', 'xlsx'].includes(extension)) return 'Chỉ hỗ trợ tệp PDF, XLS hoặc XLSX.';
  if (file.size > maxFileSize) return 'Tệp thư viện đề tài không được vượt quá 10 MB.';
  return '';
}

function formatBytes(value) {
  const bytes = Number(value);
  if (!Number.isFinite(bytes) || bytes <= 0) return '';
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}
