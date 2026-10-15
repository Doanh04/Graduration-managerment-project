import httpClient from '../config/HttpClient.jsx';
import API_ENDPOINTS from '../config/endpoints.js';

const endpoints = API_ENDPOINTS.libraryTopics;

function toFormData(payload) {
  const data = new FormData();
  data.append('title', payload.title || '');
  ['description', 'objective', 'technology'].forEach((field) => {
    if (payload[field] != null) data.append(field, payload[field]);
  });
  if (payload.file instanceof File) data.append('file', payload.file);
  return data;
}

export const LibraryTopicService = {
  list: (params) => httpClient.get(endpoints.list, { params }),
  async listAll() {
    const firstResponse = await this.list({ page: 0, size: 100 });
    const first = firstResponse?.result || {};
    if ((first.totalPages || 0) <= 1) return first.content || [];
    const remaining = await Promise.all(Array.from({ length: first.totalPages - 1 }, (_, index) => this.list({ page: index + 1, size: 100 })));
    return [firstResponse, ...remaining].flatMap((response) => response?.result?.content || []);
  },
  detail: (id) => httpClient.get(endpoints.detail(id)),
  file: (id) => httpClient.get(endpoints.file(id), { responseType: 'blob' }),
  create: (payload) => httpClient.post(endpoints.create, toFormData(payload)),
  update: (id, payload) => httpClient.put(endpoints.update(id), toFormData(payload)),
  remove: (id) => httpClient.delete(endpoints.remove(id)),
};

export default LibraryTopicService;
