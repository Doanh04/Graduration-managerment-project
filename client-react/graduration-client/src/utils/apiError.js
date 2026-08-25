const messagesByCode = {
  9999: 'Hệ thống đang gặp lỗi. Vui lòng thử lại sau.', 1012: 'Dữ liệu gửi lên không hợp lệ.',
  1003: 'Quyền không được để trống.', 1007: 'Vai trò không được để trống.', 1014: 'Email không đúng định dạng.',
  1016: 'Tên đăng nhập đã tồn tại.', 1019: 'Phiên đăng nhập đã hết hạn.', 1020: 'Tên đăng nhập hoặc mật khẩu không chính xác.',
  1021: 'Không tìm thấy tài khoản hoặc giảng viên tương ứng trong hệ thống.',
  1026: 'Bạn không có quyền thực hiện thao tác này.', 1027: 'Mật khẩu không được để trống.', 1028: 'Tên đăng nhập không được để trống.',
  1030: 'Họ và tên không được để trống.', 1031: 'Số điện thoại không đúng định dạng.', 1032: 'Số điện thoại đã tồn tại.',
  1033: 'Mã giảng viên đã tồn tại.', 1034: 'File Excel không hợp lệ hoặc không có dữ liệu.', 1036: 'Thông tin sinh viên không được để trống.',
  1037: 'Mã lớp không được để trống.', 1038: 'Tên lớp không được để trống.', 1042: 'Tên nhóm không được để trống.',
  1043: 'Tên nhóm đã tồn tại.', 1044: 'Sinh viên này đã thuộc một nhóm khác.', 1047: 'Năm học không được để trống.', 1048: 'Năm học phải có định dạng YYYY-YYYY.',
  1049: 'Năm học này đã được tạo trong hệ thống.', 1050: 'Năm học đang có đợt bảo vệ hoặc dữ liệu hội đồng nên không thể xóa.', 1052: 'Tên đợt bảo vệ không được để trống.', 1054: 'Ngày kết thúc không được trước ngày bắt đầu.',
  1055: 'Đợt bảo vệ đã tồn tại trong năm học.', 1056: 'Đợt bảo vệ đang chứa đề tài hoặc mốc tiến độ nên chưa thể xóa.', 1060: 'Tên đề tài tham khảo không được để trống.', 1061: 'Tên đề tài tham khảo đã tồn tại.',
  1063: 'Tên biểu mẫu không được để trống.', 1064: 'Tên biểu mẫu đã tồn tại.', 1072: 'Dữ liệu không phù hợp với cấu trúc lưu trữ. Vui lòng kiểm tra lại các trường đã nhập.',
  1073: 'Tài khoản đã dừng hoạt động.', 1076: 'Tên đề tài không được để trống.', 1077: 'Nguồn đề xuất đề tài không được để trống.',
  1074: 'Không tìm thấy đề tài.', 1075: 'Thông tin đề tài không hợp lệ.',
  1078: 'Tên đề tài đã tồn tại trong đợt bảo vệ.', 1079: 'Đề tài đang được nhóm, giảng viên hoặc lịch bảo vệ sử dụng nên không thể xóa.',
  1080: 'Chỉ có thể sửa hoặc xóa đề tài ở trạng thái bản nháp hoặc đã từ chối.', 1081: 'Vui lòng nhập lý do từ chối đề tài.',
  1082: 'Đợt bảo vệ đã kết thúc. Vui lòng chọn một đợt đang hoặc chưa diễn ra.', 1083: 'Đề tài hiện không khả dụng để lựa chọn.',
  1084: 'Nhóm đã có đề tài nên không thể đăng ký hoặc đề xuất thêm.',
  1085: 'Không tìm thấy mốc tiến độ.', 1086: 'Tên mốc tiến độ không được để trống.',
  1087: 'Vui lòng chọn loại mốc tiến độ.', 1088: 'Thời gian bắt đầu không được để trống.',
  1089: 'Hạn nộp không được để trống.', 1090: 'Thời gian bắt đầu phải trước hạn nộp.',
  1091: 'Thời gian của mốc tiến độ phải nằm trong thời gian diễn ra đợt bảo vệ.',
  1092: 'Tên mốc tiến độ đã tồn tại trong đợt bảo vệ này.',
  1093: 'Không thể thực hiện thao tác với trạng thái hiện tại của mốc tiến độ.',
  1094: 'Mốc tiến độ đã có bài nộp nên không thể xóa.', 1095: 'Dung lượng tệp tối đa phải lớn hơn 0.',
  1096: 'Không tìm thấy bài nộp.', 1097: 'Vui lòng chọn tệp cần nộp.',
  1098: 'Dung lượng tệp vượt quá giới hạn của mốc tiến độ.', 1099: 'Định dạng tệp không được phép.',
  1100: 'Mốc tiến độ hiện không nhận bài.', 1101: 'Chưa đến thời gian nộp bài.',
  1102: 'Đã hết hạn nộp bài và mốc không cho phép nộp muộn.',
  1103: 'Không thể thực hiện thao tác với trạng thái hiện tại của bài nộp.',
  1104: 'Bài nộp mới nhất đã được phê duyệt nên không thể nộp phiên bản khác.',
  1105: 'Nhóm không thuộc đợt bảo vệ của mốc tiến độ này.',
  1108: 'Nội dung nhận xét không được để trống.', 1110: 'Không tìm thấy nhận xét.',
  1111: 'Không thể sửa hoặc xóa nhận xét hệ thống này.',
  1126: 'Không tìm thấy bản phân công hướng dẫn.', 1127: 'Vui lòng chọn vai trò hướng dẫn.', 1128: 'Giảng viên này đã được phân công cho đề tài.',
  1129: 'Đề tài đã có giảng viên hướng dẫn chính.', 1130: 'Bản phân công này không còn hoạt động.', 1131: 'Vui lòng nhập lý do ngừng phân công.',
  1132: 'Tài khoản giảng viên không hoạt động.', 1133: 'Giảng viên đã đạt giới hạn số đề tài được hướng dẫn trong đợt.',
  1173: 'Dữ liệu import đã có trong hệ thống.',
  1174: 'Không tìm thấy đăng ký đề tài.', 1175: 'Nhóm đã có một đăng ký hoặc đề xuất đang chờ duyệt.',
  1176: 'Không thể thực hiện thao tác với đăng ký đề tài này.', 1177: 'Vui lòng nhập lý do từ chối đăng ký.',
  1178: 'Sinh viên phải thuộc một nhóm trước khi đăng ký hoặc đề xuất đề tài.',
};

export function getApiErrorMessage(error, fallback = 'Có lỗi xảy ra. Vui lòng thử lại.') {
  if (!error) return fallback;
  if (messagesByCode[error.code]) return messagesByCode[error.code];
  if (Array.isArray(error.errors) && error.errors.length) return error.errors.map((item) => item.message || item).join('\n');
  if (error.validationErrors && typeof error.validationErrors === 'object') return Object.values(error.validationErrors).join('\n');
  return error.message || fallback;
}

export default getApiErrorMessage;
