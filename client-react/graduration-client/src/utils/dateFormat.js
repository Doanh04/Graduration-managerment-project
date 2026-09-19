/**
 * Định dạng ngày chỉ phục vụ hiển thị trên giao diện.
 * Chuỗi ngày thuần YYYY-MM-DD được tách theo thành phần để tránh lệch ngày
 * do trình duyệt diễn giải nó thành UTC; các giá trị có giờ vẫn giữ múi giờ cục bộ.
 */
export function formatDateDisplay(value, { includeTime = false, includeSeconds = false } = {}) {
  if (!value) return '—';

  const raw = String(value);
  const dateOnly = raw.match(/^(\d{4})-(\d{2})-(\d{2})$/);
  const date = dateOnly
    ? new Date(Number(dateOnly[1]), Number(dateOnly[2]) - 1, Number(dateOnly[3]))
    : new Date(value);

  if (Number.isNaN(date.getTime())) return raw;

  const datePart = `${String(date.getDate()).padStart(2, '0')}/${String(date.getMonth() + 1).padStart(2, '0')}/${date.getFullYear()}`;
  if (!includeTime) return datePart;

  const timePart = `${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`;
  return `${datePart} ${includeSeconds ? `${timePart}:${String(date.getSeconds()).padStart(2, '0')}` : timePart}`;
}

/** Định dạng ngày/giờ theo kiểu hiển thị của từng màn hình. */
export function formatDateTimeDisplay(value, options = {}) {
  return formatDateDisplay(value, { includeTime: true, ...options });
}
