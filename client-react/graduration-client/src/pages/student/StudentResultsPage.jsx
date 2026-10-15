import React, { useEffect, useMemo, useState } from 'react';
import { AlertCircle, BookOpen, CalendarDays, CheckCircle2, Eye, Inbox, LoaderCircle, Search, Users, X } from 'lucide-react';
import API_ENDPOINTS from '../../config/endpoints.js';
import ResourceService from '../../services/ResourceService.jsx';
import getApiErrorMessage from '../../utils/apiError.js';
import { formatDateTimeDisplay } from '../../utils/dateFormat.js';
import '../../style/StudentResults.scss';

const PAGE_SIZE = 10;

export default function StudentResultsPage() {
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [query, setQuery] = useState('');
  const [page, setPage] = useState(0);
  const [detail, setDetail] = useState(null);

  const load = async () => {
    setLoading(true);
    setError('');
    try {
      const results = await ResourceService.getAll(API_ENDPOINTS.scores.mine);
      setItems(Array.isArray(results) ? results : []);
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, 'Không thể tải kết quả đánh giá.'));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(); }, []); // eslint-disable-line react-hooks/exhaustive-deps

  const filtered = useMemo(() => {
    const keyword = normalize(query);
    if (!keyword) return items;
    return items.filter((item) => normalize([
      item.studentCode,
      item.studentName,
      item.topicTitle,
      item.teamName,
      item.defensePeriodName,
      item.academicYear,
    ].filter(Boolean).join(' ')).includes(keyword));
  }, [items, query]);

  const totalPages = Math.max(1, Math.ceil(filtered.length / PAGE_SIZE));
  const visibleItems = filtered.slice(page * PAGE_SIZE, (page + 1) * PAGE_SIZE);

  useEffect(() => { setPage(0); }, [query]);
  useEffect(() => {
    if (page >= totalPages) setPage(Math.max(0, totalPages - 1));
  }, [page, totalPages]);

  return <div className="page-stack student-results-page">
    <section className="page-title-row">
      <div><span className="page-kicker">QUẢN LÝ DỮ LIỆU</span><h2>Kết quả đánh giá</h2><p>Điểm và nhận xét đã được công bố theo từng đợt bảo vệ.</p></div>
    </section>

    <section className="panel data-panel student-results-panel">
      <div className="table-toolbar">
        <label className="table-search"><Search size={18} /><input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Tìm theo đề tài, nhóm hoặc đợt bảo vệ..." />{query && <button type="button" className="search-clear" onClick={() => setQuery('')} aria-label="Xóa tìm kiếm"><X size={15} /></button>}</label>
      </div>
      {loading ? <ResultState icon={LoaderCircle} text="Đang tải kết quả đánh giá..." spin />
        : error ? <ResultState icon={AlertCircle} text={error} action={load} />
          : visibleItems.length === 0 ? <ResultState icon={Inbox} text={items.length ? 'Không tìm thấy kết quả phù hợp.' : 'Kết quả sẽ hiển thị khi điểm được công bố.'} />
            : <>
              <div className="table-scroll"><table><thead><tr><th>Mã SV</th><th>Sinh viên</th><th>Đợt bảo vệ</th><th>Điểm</th><th>Cập nhật</th><th>Trạng thái</th><th /></tr></thead><tbody>
                {visibleItems.map((item, index) => <tr key={item.scoreId || `${item.topicId}-${item.defensePeriodId}-${index}`}>
                  <td data-label="Mã SV">{item.studentCode || '—'}</td>
                  <td data-label="Sinh viên"><strong>{item.studentName || '—'}</strong><small>{item.topicTitle || 'Chưa có đề tài'}</small></td>
                  <td data-label="Đợt bảo vệ"><strong>{item.defensePeriodName || `Đợt #${item.defensePeriodId || '—'}`}</strong><small>{item.academicYear || 'Chưa cập nhật năm học'}</small></td>
                  <td data-label="Điểm"><strong className="student-result-score">{formatScore(item.totalScore)}</strong></td>
                  <td data-label="Cập nhật">{formatDateTimeDisplay(item.publishedAt || item.updatedAt)}</td>
                  <td data-label="Trạng thái"><span className="status-badge success">Đã công bố</span></td>
                  <td className="table-actions"><button type="button" className="student-result-view" onClick={() => setDetail(item)} title="Xem kết quả chi tiết"><Eye size={15} /><span>Xem</span></button></td>
                </tr>)}
              </tbody></table></div>
              <div className="table-footer"><span>Hiển thị {visibleItems.length} trong tổng số {filtered.length} kết quả</span><div><button type="button" disabled={page === 0} onClick={() => setPage((current) => current - 1)}>Trước</button><button type="button" className="active">{page + 1}</button><button type="button" disabled={page + 1 >= totalPages} onClick={() => setPage((current) => current + 1)}>Sau</button></div></div>
            </>}
    </section>
    {detail && <StudentResultDetail item={detail} onClose={() => setDetail(null)} />}
  </div>;
}

function StudentResultDetail({ item, onClose }) {
  const committeeScores = Array.isArray(item.committeeScores) ? item.committeeScores : [];
  return <div className="modal-backdrop" onMouseDown={(event) => event.target === event.currentTarget && onClose()}>
    <section className="student-result-modal" role="dialog" aria-modal="true" aria-label="Chi tiết kết quả đánh giá">
      <header><div><span>CHI TIẾT KẾT QUẢ ĐÁNH GIÁ</span><h3>{item.studentName || item.studentCode || 'Sinh viên'}</h3></div><button type="button" onClick={onClose} aria-label="Đóng"><X size={20} /></button></header>
      <div className="student-result-detail-body">
        <section className="student-result-summary">
          <article><span>Điểm công bố</span><strong>{formatScore(item.totalScore)}/10</strong></article>
          <article><span>Trạng thái</span><strong className="published"><CheckCircle2 size={15} /> Đã công bố</strong></article>
          <article><span>Thời gian công bố</span><strong>{formatDateTimeDisplay(item.publishedAt || item.updatedAt)}</strong></article>
        </section>

        <section className="student-result-section">
          <h4><BookOpen size={18} /> Thông tin đề tài</h4>
          <dl className="student-result-information">
            <div><dt>Tên đề tài</dt><dd>{item.topicTitle || 'Chưa cập nhật'}</dd></div>
            <div><dt>Mã đề tài</dt><dd>{item.topicId || '—'}</dd></div>
            <div><dt>Nhóm sinh viên</dt><dd>{item.teamName || 'Chưa cập nhật'}</dd></div>
            <div><dt>Mã sinh viên</dt><dd>{item.studentCode || '—'}</dd></div>
          </dl>
        </section>

        <section className="student-result-section">
          <h4><CalendarDays size={18} /> Đợt bảo vệ</h4>
          <dl className="student-result-information">
            <div><dt>Tên đợt</dt><dd>{item.defensePeriodName || `Đợt #${item.defensePeriodId || '—'}`}</dd></div>
            <div><dt>Năm học</dt><dd>{item.academicYear || 'Chưa cập nhật'}</dd></div>
          </dl>
        </section>

        <section className="student-result-section">
          <h4><Users size={18} /> Điểm và nhận xét của giảng viên</h4>
          {committeeScores.length ? <div className="student-result-comments">{committeeScores.map((entry, index) => <article key={entry.scoreId || `${entry.lecturerId}-${index}`}>
            <div><strong>{entry.lecturerName || entry.lecturerCode || 'Giảng viên'}</strong><span>{committeeRoleLabel(entry.role)}</span></div>
            <b>{formatScore(entry.score)}/10</b>
            <p>{entry.comment || 'Giảng viên chưa nhập nhận xét.'}</p>
          </article>)}</div>
            : <div className="student-result-single-comment"><strong>{item.lecturerName || 'Nhận xét đánh giá'}</strong><p>{item.comment || 'Chưa có nhận xét.'}</p></div>}
        </section>
      </div>
      <footer><button type="button" className="primary-button" onClick={onClose}>Đóng</button></footer>
    </section>
  </div>;
}

function ResultState({ icon: Icon, text, action, spin }) {
  return <div className="data-state"><Icon className={spin ? 'spin' : ''} size={28} /><strong>{text}</strong>{action && <button type="button" onClick={action}>Tải lại</button>}</div>;
}

function committeeRoleLabel(role) {
  return { CHAIRPERSON: 'Chủ tịch', REVIEWER: 'Phản biện', SECRETARY: 'Thư ký', MEMBER: 'Ủy viên' }[String(role || '').toUpperCase()] || 'Thành viên hội đồng';
}

function formatScore(value) {
  const number = Number(value);
  if (!Number.isFinite(number)) return '—';
  return number.toLocaleString('vi-VN', { minimumFractionDigits: Number.isInteger(number) ? 0 : 1, maximumFractionDigits: 2 });
}

function normalize(value) {
  return String(value || '').normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase().trim();
}
