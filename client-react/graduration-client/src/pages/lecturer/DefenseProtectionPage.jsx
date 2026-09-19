import React, { useEffect, useMemo, useState } from 'react';
import { AlertCircle, CalendarDays, Check, Eye, LoaderCircle, MessageSquare, Search, Users } from 'lucide-react';
import API_ENDPOINTS from '../../config/endpoints.js';
import ResourceService from '../../services/ResourceService.jsx';
import getApiErrorMessage from '../../utils/apiError.js';
import { formatDateDisplay } from '../../utils/dateFormat.js';
import { CommitteeScheduleModal, SCHEDULE_STATUS, normalizeSchedule, committeeRoleLabel, formatTimeRange, scheduleStatusValue, scheduleStatusLabel } from '../shared/ReviewAssignmentPage.jsx';
import '../../style/WorkflowPages.scss';

/**
 * Danh sách các buổi bảo vệ mà giảng viên được phân vào hội đồng.
 * Mọi vai trò trong hội đồng đều được xem hồ sơ; modal chi tiết chỉ mở nút
 * chấm điểm khi backend xác định giảng viên đang giữ vai trò Chủ tịch.
 */
export default function DefenseProtectionPage({ focus = 'schedule' }) {
  const isScores = focus === 'scores';
  const [schedules, setSchedules] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [query, setQuery] = useState('');
  const [status, setStatus] = useState('');
  const [selected, setSelected] = useState(null);

  const load = async () => {
    setLoading(true);
    setError('');
    try {
      const [scheduleResult, memberResult] = await Promise.all([
        ResourceService.getAll(API_ENDPOINTS.schedules.reviewerMine),
        ResourceService.getAll(API_ENDPOINTS.committeeMembers.mine),
      ]);
      const activeMemberships = (Array.isArray(memberResult) ? memberResult : []).filter((member) => {
        const memberStatus = String(member.status || '').toUpperCase();
        return !memberStatus || memberStatus === 'ACTIVE';
      });
      setSchedules((Array.isArray(scheduleResult) ? scheduleResult : []).map((raw) => {
        const item = normalizeSchedule(raw);
        const committeeId = raw.committeeId || raw.idCommittee || raw.committee?.committeeId || item.committeeId;
        const membership = activeMemberships.find((member) => String(member.committeeId || member.idCommittee) === String(committeeId));
        const role = membership?.role || raw.currentRole || null;
        return { ...item, committeeId, currentRole: role ? String(role).toUpperCase() : null };
      }));
    } catch (e) {
      setError(getApiErrorMessage(e, 'Không thể tải danh sách bảo vệ đồ án.'));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(); }, []); // eslint-disable-line react-hooks/exhaustive-deps

  const filtered = useMemo(() => schedules.filter((item) => {
    const haystack = `${item.topicTitle || ''} ${item.teamName || ''} ${item.committeeName || ''} ${item.room || ''}`.toLowerCase();
    return haystack.includes(query.trim().toLowerCase()) && (!status || scheduleStatusValue(item) === status);
  }), [schedules, query, status]);

  const title = isScores ? 'Chấm điểm bảo vệ' : 'Bảo vệ đồ án';
  const description = isScores
    ? 'Nhận xét hồ sơ bảo vệ và nhập điểm cho từng sinh viên theo vai trò được phân công.'
    : 'Theo dõi lịch, hồ sơ đề tài, nhóm sinh viên và thành viên hội đồng được phân công.';

  return <div className="workflow-page defense-protection-page">
    <div className="page-heading"><div><span>TỔ CHỨC BẢO VỆ</span><h1>{title}</h1><p>{description}</p></div></div>
    {error && <div className="workflow-error page-error"><AlertCircle size={17} />{error}<button type="button" className="secondary-button" onClick={load}>Thử lại</button></div>}
    <section className="workflow-panel">
      <div className="workflow-toolbar"><label><Search size={17} /><input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Tìm theo đề tài, nhóm, hội đồng hoặc phòng..." /></label><select value={status} onChange={(event) => setStatus(event.target.value)}><option value="">Tất cả trạng thái lịch</option>{Object.entries(SCHEDULE_STATUS).map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select></div>
      {loading ? <div className="workflow-state"><LoaderCircle className="spin" /><strong>Đang tải dữ liệu...</strong></div> : filtered.length === 0 ? <div className="workflow-state"><CalendarDays /><strong>{isScores ? 'Chưa có lịch bảo vệ cần đánh giá.' : 'Chưa có lịch bảo vệ được phân công.'}</strong><small>{isScores ? 'Danh sách dùng để ghi nhận nhận xét và chấm điểm theo vai trò trong hội đồng.' : 'Danh sách chỉ bao gồm các hội đồng mà bạn đang là thành viên ACTIVE.'}</small></div> : <div className="workflow-table"><table><thead>{isScores ? <tr><th>Đề tài</th><th>Sinh viên thực hiện</th><th>Hội đồng / vai trò</th><th>Lịch bảo vệ</th><th /></tr> : <tr><th>Đề tài</th><th>Đợt bảo vệ</th><th>Ngày và giờ</th><th>Hội đồng / vai trò</th><th>Địa điểm</th><th>Trạng thái</th><th /></tr>}</thead><tbody>{filtered.map((item) => isScores ? <tr key={item.scheduleId}><td><strong>{item.topicTitle || '—'}</strong><small>{item.teamName || 'Chưa có nhóm'}</small></td><td><strong>{item.students?.length || 0} sinh viên</strong><small>{(item.students || []).map((student) => student.fullName || student.studentCode).filter(Boolean).join(', ') || 'Chưa có thông tin'}</small></td><td><strong>{item.committeeName || '—'}</strong><small>{item.currentRole ? committeeRoleLabel(item.currentRole) : 'Thành viên hội đồng'}</small></td><td>{formatDateDisplay(item.defenseDate)}<small>{formatTimeRange(item)}</small></td><td><div className="workflow-actions"><button type="button" title={item.currentRole === 'CHAIRPERSON' ? 'Chấm điểm và nhận xét' : 'Nhận xét hồ sơ bảo vệ'} onClick={() => setSelected(item)}>{item.currentRole === 'CHAIRPERSON' ? <Check size={15} /> : <MessageSquare size={15} />}</button></div></td></tr> : <tr key={item.scheduleId}><td><strong>{item.topicTitle || '—'}</strong><small>{item.teamName || 'Chưa có nhóm'}</small></td><td>{item.defensePeriodName || '—'}<small>{item.academicYear || ''}</small></td><td>{formatDateDisplay(item.defenseDate)}<small>{formatTimeRange(item)}</small></td><td><strong>{item.committeeName || '—'}</strong><small>{item.currentRole ? committeeRoleLabel(item.currentRole) : 'Thành viên hội đồng'}</small></td><td>{item.room || '—'}<small>{item.location || ''}</small></td><td>{(() => { const statusValue = scheduleStatusValue(item); return <span className={`workflow-status ${String(statusValue || '').toLowerCase()}`}><span>{scheduleStatusLabel(statusValue)}</span></span>; })()}</td><td><div className="workflow-actions"><button type="button" title="Xem hồ sơ bảo vệ" onClick={() => setSelected(item)}><Eye size={15} /></button></div></td></tr>)}</tbody></table></div>}
    </section>
    <div className="workflow-info defense-role-guide"><Users size={15} /> <span>{isScores ? <>Tất cả thành viên hội đồng có thể xem hồ sơ và gửi nhận xét. Chỉ <strong>Chủ tịch</strong> được nhập, sửa và gửi điểm cho từng sinh viên.</> : <>Tất cả thành viên hội đồng được xem đầy đủ lịch, hồ sơ đề tài, nhóm, sinh viên và tệp đề tài. Các thao tác đánh giá thực hiện tại trang Chấm điểm bảo vệ.</>}</span></div>
    {selected && <CommitteeScheduleModal item={selected} mode={isScores ? 'scores' : 'schedule'} onClose={() => setSelected(null)} />}
  </div>;
}
