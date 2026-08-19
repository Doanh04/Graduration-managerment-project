const messagesByCode = {
  9999: 'Hệ thống đang gặp lỗi. Vui lòng thử lại sau.', 1012: 'Dữ liệu gửi lên không hợp lệ.',
  1003: 'Quyền không được để trống.', 1007: 'Vai trò không được để trống.', 1014: 'Email không đúng định dạng.',
  1016: 'Tên đăng nhập đã tồn tại.', 1019: 'Phiên đăng nhập đã hết hạn.', 1020: 'Tên đăng nhập hoặc mật khẩu không chính xác.',
  1026: 'Bạn không có quyền thực hiện thao tác này.', 1027: 'Mật khẩu không được để trống.', 1028: 'Tên đăng nhập không được để trống.',
  1030: 'Họ và tên không được để trống.', 1031: 'Số điện thoại không đúng định dạng.', 1032: 'Số điện thoại đã tồn tại.',
  1033: 'Mã giảng viên đã tồn tại.', 1034: 'File Excel không hợp lệ hoặc không có dữ liệu.', 1036: 'Thông tin sinh viên không được để trống.',
  1037: 'Mã lớp không được để trống.', 1038: 'Tên lớp không được để trống.', 1042: 'Tên nhóm không được để trống.',
  1043: 'Tên nhóm đã tồn tại.', 1047: 'Năm học không được để trống.', 1048: 'Năm học phải có định dạng YYYY-YYYY.',
  1049: 'Năm học này đã được tạo trong hệ thống.', 1052: 'Tên đợt bảo vệ không được để trống.', 1054: 'Ngày kết thúc không được trước ngày bắt đầu.',
  1055: 'Đợt bảo vệ đã tồn tại trong năm học.', 1060: 'Tên đề tài tham khảo không được để trống.', 1061: 'Tên đề tài tham khảo đã tồn tại.',
  1063: 'Tên biểu mẫu không được để trống.', 1064: 'Tên biểu mẫu đã tồn tại.', 1072: 'Dữ liệu bị trùng hoặc đang được sử dụng.',
  1073: 'Tài khoản đã dừng hoạt động.', 1076: 'Tên đề tài không được để trống.', 1077: 'Nguồn đề xuất đề tài không được để trống.',
  1078: 'Tên đề tài đã tồn tại trong đợt bảo vệ.', 1086: 'Tên milestone không được để trống.', 1089: 'Deadline không được để trống.',
  1173: 'Dữ liệu import đã có trong hệ thống.',
};

export function getApiErrorMessage(error, fallback = 'Có lỗi xảy ra. Vui lòng thử lại.') {
  if (!error) return fallback;
  if (messagesByCode[error.code]) return messagesByCode[error.code];
  if (Array.isArray(error.errors) && error.errors.length) return error.errors.map((item) => item.message || item).join('\n');
  if (error.validationErrors && typeof error.validationErrors === 'object') return Object.values(error.validationErrors).join('\n');
  return error.message || fallback;
}

export default getApiErrorMessage;
