import React, { useEffect, useState } from 'react';
import { CalendarRange, LoaderCircle } from 'lucide-react';
import ResourceService from '../../services/ResourceService.jsx';
import API_ENDPOINTS from '../../config/endpoints.js';
import ModulePage from './ModulePage.jsx';

export default function PeriodResourcePage({ endpointForPeriod, crudForPeriod, studentOnly = false, ...pageProps }) {
  const [periods, setPeriods] = useState([]); const [selected, setSelected] = useState(''); const [loading, setLoading] = useState(true);
  useEffect(() => { Promise.all([ResourceService.getPage(API_ENDPOINTS.defensePeriods.list, { page: 0, size: 100 }), studentOnly ? ResourceService.getAll(API_ENDPOINTS.teams.mineAll) : Promise.resolve(null)]).then(([result, teams]) => { const assigned = studentOnly ? new Set((Array.isArray(teams) ? teams : []).map((team) => String(team.defensePeriodId))) : null; const available = result.content.filter((period) => !assigned || assigned.has(String(period.defensePeriodId ?? period.idDefense ?? period.id_Defense))); setPeriods(available); if (available.length) setSelected(String(available[0].defensePeriodId ?? available[0].idDefense ?? available[0].id_Defense)); }).finally(() => setLoading(false)); }, [studentOnly]);
  const endpoint = selected ? endpointForPeriod(selected) : null;
  const crud = selected && crudForPeriod ? crudForPeriod(selected) : undefined;
  const emptyMessage = !periods.length
    ? 'Chưa có đợt bảo vệ trong hệ thống.'
    : selected
      ? `Chưa có dữ liệu ${pageProps.title?.toLowerCase() || ''} trong đợt bảo vệ này.`
      : 'Chọn đợt bảo vệ để xem dữ liệu.';
  return <div className={`period-resource ${studentOnly ? 'student-schedule-view' : ''}`}><section className="period-selector panel"><div><CalendarRange size={20} /><span><strong>Đợt bảo vệ</strong><small>Chọn đợt để tải dữ liệu tương ứng</small></span></div>{loading ? <LoaderCircle className="spin" size={20} /> : <select value={selected} onChange={(event) => setSelected(event.target.value)}><option value="">Chọn đợt bảo vệ</option>{periods.map((period) => { const id = period.defensePeriodId ?? period.idDefense ?? period.id_Defense; return <option value={id} key={id}>{period.periodName || period.defensePeriodName || `Đợt ${id}`}</option>; })}</select>}</section><ModulePage {...pageProps} endpoint={endpoint} crud={crud} emptyMessage={emptyMessage} /></div>;
}
