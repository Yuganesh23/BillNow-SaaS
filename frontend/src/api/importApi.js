import axiosClient from './axiosClient';

export async function downloadImportTemplate(type, format = 'xlsx') {
  const response = await axiosClient.get('/imports/template', {
    params: { type, format },
    responseType: 'blob',
  });
  const url = URL.createObjectURL(response.data);
  const anchor = document.createElement('a');
  anchor.href = url;
  anchor.download = `${type.toLowerCase()}-template.${format}`;
  anchor.click();
  URL.revokeObjectURL(url);
}

export async function previewImport(type, file) {
  const form = new FormData();
  form.append('type', type);
  form.append('file', file);
  const response = await axiosClient.post('/imports/preview', form, {
    headers: { 'Content-Type': 'multipart/form-data' },
  });
  return response.data;
}

export async function commitImport(type, rows) {
  const response = await axiosClient.post('/imports/commit', { type, rows });
  return response.data;
}
