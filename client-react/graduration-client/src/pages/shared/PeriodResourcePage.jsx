import React, { useEffect, useState } from 'react';
import { CalendarRange, LoaderCircle } from 'lucide-react';
import ResourceService from '../../services/ResourceService.jsx';
import API_ENDPOINTS from '../../config/endpoints.js';
import ModulePage from './ModulePage.jsx';

export default function PeriodResourcePage({ endpointForPeriod, ...pageProps }) {
  const [periods, setPeriods] = useState([]); const [selected, setSelected] = useState(''); const [loading, setLoading] = useState(true);
  useEffect(() => { ResourceService.getPage(API_ENDPOINTS.defensePeriods.list, { page: 0, size: 100 }).then((result) => { setPeriods(result.content); if (result.content.length) setSelected(String(result.content[0].defensePeriodId ?? result.content[0].idDefense ?? result.content[0].id_Defense)); }).finally(() => setLoading(false)); }, []);
  return <div className="period-resource"><section className="period-selector panel"><div><CalendarRange size={20} /><span><strong>Đợt bảo vệ</strong><small>Chọn đợt để tải dữ liệu tương ứng</small></span></div>{loading ? <LoaderCircle className="spin" size={20} /> : <select value={selected} onChange={(event) => setSelected(event.target.value)}><option value="">Chọn đợt bảo vệ</option>{periods.map((period) => { const id = period.defensePeriodId ?? period.idDefense ?? period.id_Defense; return <option value={id} key={id}>{period.periodName || period.defensePeriodName || `Đợt ${id}`}</option>; })}</select>}</section><ModulePage {...pageProps} endpoint={selected ? endpointForPeriod(selected) : null} emptyMessage={periods.length ? 'Chọn đợt bảo vệ để xem dữ liệu.' : 'Chưa có đợt bảo vệ trong hệ thống.'} /></div>;
}
