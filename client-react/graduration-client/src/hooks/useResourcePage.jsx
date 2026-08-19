import { useCallback, useEffect, useState } from 'react';
import ResourceService from '../services/ResourceService.jsx';

export default function useResourcePage(endpoint, { initialSize = 10, params = {}, searchFields = [], serverSearch = false } = {}) {
  const [data, setData] = useState({ content: [], page: 0, size: initialSize, totalElements: 0, totalPages: 0, first: true, last: true });
  const [page, setPage] = useState(0);
  const [keyword, setKeyword] = useState('');
  const [loading, setLoading] = useState(Boolean(endpoint));
  const [error, setError] = useState(null);
  const paramsKey = JSON.stringify(params);

  const load = useCallback(async () => {
    if (!endpoint) {
      setLoading(false);
      setData((current) => ({ ...current, content: [], totalElements: 0, totalPages: 0 }));
      return;
    }
    setLoading(true);
    setError(null);
    try {
      if (keyword && !serverSearch) {
        const source = await ResourceService.getAll(endpoint, params);
        const normalizedKeyword = normalizeSearch(keyword);
        const filtered = source.filter((item) => searchFields.some((field) => normalizeSearch(valueAt(item, field)).includes(normalizedKeyword)));
        const start = page * initialSize;
        const content = filtered.slice(start, start + initialSize);
        const totalPages = Math.ceil(filtered.length / initialSize);
        setData({ content, page, size: initialSize, totalElements: filtered.length, totalPages, first: page === 0, last: totalPages === 0 || page >= totalPages - 1 });
      } else {
        setData(await ResourceService.getPage(endpoint, { page, size: initialSize, keyword, params }));
      }
    } catch (requestError) {
      setError(requestError);
      setData((current) => ({ ...current, content: [], totalElements: 0, totalPages: 0 }));
    } finally {
      setLoading(false);
    }
  }, [endpoint, page, initialSize, keyword, paramsKey, serverSearch, searchFields.join('|')]); // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => { load(); }, [load]);

  return { ...data, page, setPage, keyword, setKeyword, loading, error, reload: load };
}

function valueAt(source, key) { return key.split('.').reduce((value, part) => value?.[part], source); }
function normalizeSearch(value) { return String(value ?? '').normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLocaleLowerCase('vi').trim(); }
