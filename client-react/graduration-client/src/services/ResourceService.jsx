import httpClient from '../config/HttpClient.jsx';

const unwrapPage = (response) => {
  const result = response?.result ?? response ?? {};
  if (Array.isArray(result)) {
    return { content: result, page: 0, size: result.length, totalElements: result.length, totalPages: result.length ? 1 : 0, first: true, last: true };
  }
  return {
    content: Array.isArray(result.content) ? result.content : [],
    page: result.page ?? 0,
    size: result.size ?? 10,
    totalElements: result.totalElements ?? 0,
    totalPages: result.totalPages ?? 0,
    first: result.first ?? true,
    last: result.last ?? true,
  };
};

export const ResourceService = {
  async getPage(endpoint, { page = 0, size = 10, keyword = '', params = {} } = {}) {
    if (!endpoint) return unwrapPage(null);
    const response = await httpClient.get(endpoint, { params: { page, size, ...(keyword ? { keyword } : {}), ...params } });
    return unwrapPage(response);
  },

  async count(endpoint, params = {}) {
    const page = await this.getPage(endpoint, { page: 0, size: 1, params });
    return page.totalElements;
  },

  async getAll(endpoint, params = {}) {
    const firstPage = await this.getPage(endpoint, { page: 0, size: 100, params });
    if (firstPage.totalPages <= 1) return firstPage.content;
    const remaining = await Promise.all(Array.from({ length: firstPage.totalPages - 1 }, (_, index) => this.getPage(endpoint, { page: index + 1, size: 100, params })));
    return [firstPage, ...remaining].flatMap((result) => result.content);
  },

  async getOne(endpoint, params = {}) {
    const response = await httpClient.get(endpoint, { params });
    return response?.result ?? response;
  },

  create(endpoint, payload) { return httpClient.post(endpoint, payload); },
  update(endpoint, payload, method = 'put') { return httpClient.request({ url: endpoint, method, data: payload }); },
  remove(endpoint) { return httpClient.delete(endpoint); },
  importFile(endpoint, file) { const data = new FormData(); data.append('file', file); return httpClient.post(endpoint, data, { headers: { 'Content-Type': 'multipart/form-data' } }); },
  downloadFile(endpoint, params = {}) { return httpClient.get(endpoint, { params, responseType: 'blob' }); },
};

export default ResourceService;
