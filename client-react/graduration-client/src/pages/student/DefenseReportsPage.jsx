import React, { useEffect, useState } from 'react';
import { Clock3, FileText, Link2, ShieldCheck, Upload } from 'lucide-react';
import ResourceService from '../../services/ResourceService.jsx';
import API_ENDPOINTS from '../../config/endpoints.js';
import DefenseReportPanel, { REPORT_LABELS } from '../../components/DefenseReportPanel.jsx';
import getApiErrorMessage from '../../utils/apiError.js';
import '../../style/DefenseReport.scss';
import '../../style/DefenseReportEmpty.scss';

function EmptyDefenseReport() {
  return (
    <section className="defense-report-panel defense-report-empty">
      <header>
        <div className="report-empty-title">
          <span className="report-empty-icon"><FileText size={20} /></span>
          <div>
            <h3>Hồ sơ phản biện trước lịch bảo vệ</h3>
            <p>Khung nộp hồ sơ sẽ được mở ngay khi hội đồng có giảng viên phản biện.</p>
          </div>
        </div>
        <span className="report-badge waiting">Chờ phân công</span>
      </header>

      <div className="report-empty-summary">
        <div><ShieldCheck size={18} /><span>Hội đồng dự kiến<strong>Chưa phân công</strong></span></div>
        <div><Clock3 size={18} /><span>Hạn chót phản biện<strong>Chưa cập nhật</strong></span></div>
      </div>

      <p className="report-notice">
        Nhóm chưa được phân công giảng viên phản biện. Bạn có thể chuẩn bị trước báo cáo và
        liên kết source code hoặc sản phẩm; các trường bên dưới sẽ được mở khi có phân công.
      </p>

      <div className="report-upload report-upload-placeholder" aria-disabled="true">
        <label>
          Báo cáo PDF / DOCX (tối đa 10 MB)
          <span className="report-disabled-field"><Upload size={16} /> Chọn tệp báo cáo</span>
        </label>
        <label>
          Link source code
          <span className="report-disabled-field"><Link2 size={16} /> https://github.com/...</span>
        </label>
        <label>
          Link sản phẩm / demo
          <span className="report-disabled-field"><Link2 size={16} /> https://...</span>
        </label>
        <label>
          Ghi chú
          <span className="report-disabled-field report-disabled-note">Nội dung cần lưu ý cho giảng viên phản biện...</span>
        </label>
        <button type="button" className="primary-button" disabled><Upload size={16} /> Nộp hồ sơ</button>
      </div>

      <div className="report-empty-footer">
        <span>1</span><p><strong>Chờ phân công phản biện</strong>Ban quản lý thêm giảng viên phản biện vào hội đồng.</p>
        <span>2</span><p><strong>Nộp hồ sơ</strong>Một thành viên đại diện nộp báo cáo và liên kết cho cả nhóm.</p>
        <span>3</span><p><strong>Chờ xác nhận</strong>Đề tài chỉ được xếp lịch sau khi phản biện đồng ý.</p>
      </div>
    </section>
  );
}

export default function DefenseReportsPage() {
  const [items, setItems] = useState([]);
  const [selected, setSelected] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    let active = true;
    ResourceService.getOne(API_ENDPOINTS.defenseReports.mine)
      .then((result) => {
        if (!active) return;
        const list = Array.isArray(result) ? result : [];
        setItems(list);
        setSelected(String(list[0]?.assignmentId || ''));
      })
      .catch((err) => active && setError(getApiErrorMessage(err, 'Không thể tải phân công phản biện.')))
      .finally(() => active && setLoading(false));
    return () => { active = false; };
  }, []);

  return (
    <div className="page-stack">
      <div className="page-heading">
        <div>
          <span>ĐIỀU KIỆN XẾP LỊCH BẢO VỆ</span>
          <h1>Hồ sơ phản biện</h1>
          <p>Một thành viên nộp báo cáo và link source code hoặc sản phẩm cho cả nhóm trước hạn phản biện.</p>
        </div>
      </div>
      {error && <p role="alert" className="report-error">{error}</p>}
      {loading ? <section className="defense-report-panel report-loading"><p>Đang tải hồ sơ phản biện...</p></section>
        : !items.length ? <EmptyDefenseReport />
          : <>
            <label className="report-schedule-select">
              Đợt / phân công phản biện
              <select value={selected} onChange={(event) => setSelected(event.target.value)}>
                {items.map((item) => (
                  <option key={item.assignmentId} value={item.assignmentId}>
                    {item.periodName} · {item.academicYear} · {item.topicTitle} · {REPORT_LABELS[item.status]}
                  </option>
                ))}
              </select>
            </label>
            <DefenseReportPanel key={selected} assignmentId={selected} />
          </>}
    </div>
  );
}
