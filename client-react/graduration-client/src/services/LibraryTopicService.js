import httpClient from '../config/HttpClient.jsx';
import API_ENDPOINTS from '../config/endpoints.js';

const endpoints = API_ENDPOINTS.libraryTopics;

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
  create: (payload) => httpClient.post(endpoints.create, payload),
  update: (id, payload) => httpClient.put(endpoints.update(id), payload),
  remove: (id) => httpClient.delete(endpoints.remove(id)),
};

export default LibraryTopicService;
