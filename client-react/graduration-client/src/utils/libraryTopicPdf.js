let fontPromise;

async function loadFonts() {
  if (!fontPromise) {
    fontPromise = Promise.all([
      fetch(`${import.meta.env.BASE_URL}fonts/DejaVuSans.ttf`).then((response) => response.arrayBuffer()),
      fetch(`${import.meta.env.BASE_URL}fonts/DejaVuSans-Bold.ttf`).then((response) => response.arrayBuffer()),
    ]);
  }
  return fontPromise;
}

function toBase64(buffer) {
  const bytes = new Uint8Array(buffer);
  let binary = '';
  const chunkSize = 8192;
  for (let index = 0; index < bytes.length; index += chunkSize) {
    binary += String.fromCharCode(...bytes.subarray(index, index + chunkSize));
  }
  return window.btoa(binary);
}

function safeFileName(value) {
  return String(value || 'de-tai-tham-khao')
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/đ/g, 'd')
    .replace(/Đ/g, 'D')
    .replace(/[^a-zA-Z0-9]+/g, '-')
    .replace(/^-|-$/g, '')
    .toLowerCase()
    .slice(0, 70);
}

export default async function downloadLibraryTopicPdf(topic) {
  const { jsPDF } = await import('jspdf');
  const [regularFont, boldFont] = await loadFonts();
  const pdf = new jsPDF({ unit: 'mm', format: 'a4', orientation: 'portrait' });
  pdf.addFileToVFS('DejaVuSans.ttf', toBase64(regularFont));
  pdf.addFileToVFS('DejaVuSans-Bold.ttf', toBase64(boldFont));
  pdf.addFont('DejaVuSans.ttf', 'DejaVu', 'normal');
  pdf.addFont('DejaVuSans-Bold.ttf', 'DejaVu', 'bold');
  pdf.setFont('DejaVu', 'normal');

  const pageWidth = pdf.internal.pageSize.getWidth();
  const pageHeight = pdf.internal.pageSize.getHeight();
  const margin = 20;
  const contentWidth = pageWidth - margin * 2;

  const centeredText = (text, x, width, y, style = 'normal', size = 11) => {
    pdf.setFont('DejaVu', style); pdf.setFontSize(size);
    pdf.text(text, x + width / 2, y, { align: 'center' });
  };

  centeredText('TRƯỜNG ĐHCN VIỆT-HUNG', margin, 76, 22, 'bold', 10);
  centeredText('KHOA CÔNG NGHỆ THÔNG TIN', margin, 76, 29, 'bold', 10);
  centeredText('CỘNG HÒA XÃ HỘI CHỦ NGHĨA VIỆT NAM', 104, 86, 22, 'bold', 10);
  centeredText('Độc lập - Tự do - Hạnh phúc', 104, 86, 29, 'bold', 10);
  pdf.setLineWidth(0.35);
  pdf.line(38, 32, 78, 32);
  pdf.line(125, 32, 171, 32);

  centeredText('BIỂU MẪU ĐỀ TÀI THAM KHẢO', margin, contentWidth, 49, 'bold', 15);
  centeredText(`Mã đề tài: ${topic.idLibraryTopic ?? '—'}`, margin, contentWidth, 57, 'normal', 9.5);

  let y = 72;
  const ensureSpace = (height) => {
    if (y + height <= pageHeight - 20) return;
    pdf.addPage();
    pdf.setFont('DejaVu', 'normal');
    y = 22;
  };
  const addField = (index, label, value) => {
    const text = value?.trim?.() || 'Chưa cập nhật';
    pdf.setFont('DejaVu', 'bold'); pdf.setFontSize(11);
    const heading = `${index}. ${label}`;
    const headingLines = pdf.splitTextToSize(heading, contentWidth);
    const bodyLines = pdf.splitTextToSize(text, contentWidth - 5);
    const height = headingLines.length * 6 + bodyLines.length * 5.4 + 7;
    ensureSpace(height);
    pdf.text(headingLines, margin, y);
    y += headingLines.length * 6 + 2;
    pdf.setFont('DejaVu', 'normal'); pdf.setFontSize(10.5);
    pdf.text(bodyLines, margin + 5, y, { lineHeightFactor: 1.45 });
    y += bodyLines.length * 5.4 + 7;
  };

  addField(1, 'Tên đề tài', topic.title);
  addField(2, 'Mô tả đề tài', topic.description);
  addField(3, 'Mục tiêu', topic.objective);
  addField(4, 'Công nghệ sử dụng', topic.technology);

  const totalPages = pdf.getNumberOfPages();
  for (let page = 1; page <= totalPages; page += 1) {
    pdf.setPage(page);
    pdf.setFont('DejaVu', 'normal'); pdf.setFontSize(8);
    pdf.setTextColor(100);
    pdf.text(`Trang ${page}/${totalPages}`, pageWidth / 2, pageHeight - 10, { align: 'center' });
  }

  pdf.save(`bieu-mau-de-tai-${safeFileName(topic.title)}.pdf`);
}
