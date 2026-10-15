package com.graduration.Service.AuditService;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import com.graduration.Configuration.PaginationSupport;
import com.graduration.DTO.Response.AuditLogResponse;
import com.graduration.Repository.AuditLogRepository;
import com.graduration.entity.AuditLogDocument;
import com.graduration.exception.AppException;
import com.graduration.exception.ErrorCode;
import com.graduration.mapper.AuditLogMapper;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuditLogService {
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter EXPORT_DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    AuditLogRepository auditLogRepository;
    AuditLogMapper auditLogMapper;

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    // Hàm getAuditLog: Nhận mã hoặc điều kiện tìm kiếm của getAuditLog, truy vấn bản ghi/quan hệ tương ứng, báo lỗi khi
    // không tồn tại và trả về dữ liệu đã ánh xạ.
    public AuditLogResponse getAuditLog(String auditLogId) {
        return auditLogMapper.toAuditLogResponse(findAuditLog(auditLogId));
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    // Hàm getAllAuditLogs: Nhận các tham số lọc/phân trang của getAllAuditLogs, truy vấn dữ liệu phù hợp từ repository,
    // ánh xạ từng entity sang DTO và trả về cho giao diện.
    public List<AuditLogResponse> getAllAuditLogs() {
        return getAllAuditLogs(0, PaginationSupport.DEFAULT_SIZE);
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    // Hàm getAllAuditLogs: Nhận các tham số lọc/phân trang của getAllAuditLogs, truy vấn dữ liệu phù hợp từ repository,
    // ánh xạ từng entity sang DTO và trả về cho giao diện.
    public List<AuditLogResponse> getAllAuditLogs(Integer page, Integer size) {
        return mapToResponses(auditLogRepository
                .findAll(PaginationSupport.pageRequest(page, size, Sort.by(Sort.Direction.DESC, "createdAt")))
                .getContent());
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    // Hàm getAllAuditLogsPage: Nhận các tham số lọc/phân trang của getAllAuditLogsPage, truy vấn dữ liệu phù hợp từ
    // repository, ánh xạ từng entity sang DTO và trả về cho giao diện.
    public com.graduration.DTO.Response.PageResponse<AuditLogResponse> getAllAuditLogsPage(Integer page, Integer size) {
        return com.graduration.DTO.Response.PageResponse.from(
                auditLogRepository.findAll(
                        PaginationSupport.pageRequest(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))),
                auditLogMapper::toAuditLogResponse);
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    // Hàm getAuditLogsByUserId: Nhận mã hoặc điều kiện tìm kiếm của getAuditLogsByUserId, truy vấn bản ghi/quan hệ
    // tương ứng, báo lỗi khi không tồn tại và trả về dữ liệu đã ánh xạ.
    public List<AuditLogResponse> getAuditLogsByUserId(String userId) {
        return getAuditLogsByUserId(userId, 0, PaginationSupport.DEFAULT_SIZE);
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    // Hàm getAuditLogsByUserId: Nhận mã hoặc điều kiện tìm kiếm của getAuditLogsByUserId, truy vấn bản ghi/quan hệ
    // tương ứng, báo lỗi khi không tồn tại và trả về dữ liệu đã ánh xạ.
    public List<AuditLogResponse> getAuditLogsByUserId(String userId, Integer page, Integer size) {
        if (userId == null || userId.isBlank()) {
            throw new AppException(ErrorCode.AUDIT_LOG_USER_ID_NOT_BLANK);
        }
        return mapToResponses(auditLogRepository
                .findByUserIdOrderByCreatedAtDesc(userId.trim(), PaginationSupport.pageRequest(page, size))
                .getContent());
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    // Hàm getAuditLogsByAction: Nhận mã hoặc điều kiện tìm kiếm của getAuditLogsByAction, truy vấn bản ghi/quan hệ
    // tương ứng, báo lỗi khi không tồn tại và trả về dữ liệu đã ánh xạ.
    public List<AuditLogResponse> getAuditLogsByAction(String action) {
        return getAuditLogsByAction(action, 0, PaginationSupport.DEFAULT_SIZE);
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    // Hàm getAuditLogsByAction: Nhận mã hoặc điều kiện tìm kiếm của getAuditLogsByAction, truy vấn bản ghi/quan hệ
    // tương ứng, báo lỗi khi không tồn tại và trả về dữ liệu đã ánh xạ.
    public List<AuditLogResponse> getAuditLogsByAction(String action, Integer page, Integer size) {
        if (action == null || action.isBlank()) {
            throw new AppException(ErrorCode.AUDIT_LOG_ACTION_NOT_BLANK);
        }
        return mapToResponses(auditLogRepository
                .findByActionIgnoreCaseOrderByCreatedAtDesc(action.trim(), PaginationSupport.pageRequest(page, size))
                .getContent());
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public byte[] exportAuditLogs(LocalDate fromDate, LocalDate toDate) {
        if (fromDate == null || toDate == null) {
            throw new AppException(ErrorCode.AUDIT_EXPORT_DATE_REQUIRED);
        }
        if (fromDate.isAfter(toDate)) {
            throw new AppException(ErrorCode.AUDIT_EXPORT_DATE_INVALID);
        }
        List<AuditLogDocument> logs = auditLogRepository.findForExport(
                fromDate.atStartOfDay(BUSINESS_ZONE).toInstant(),
                toDate.plusDays(1).atStartOfDay(BUSINESS_ZONE).toInstant());
        return createAuditWorkbook(logs, fromDate, toDate);
    }

    private byte[] createAuditWorkbook(List<AuditLogDocument> logs, LocalDate fromDate, LocalDate toDate) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Nhật ký hoạt động");
            sheet.createFreezePane(0, 4);

            CellStyle titleStyle = workbook.createCellStyle();
            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 16);
            titleStyle.setFont(titleFont);
            titleStyle.setAlignment(HorizontalAlignment.CENTER);
            titleStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            Row titleRow = sheet.createRow(0);
            titleRow.setHeightInPoints(28);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("NHẬT KÝ HOẠT ĐỘNG HỆ THỐNG");
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 7));

            CellStyle rangeStyle = workbook.createCellStyle();
            rangeStyle.setAlignment(HorizontalAlignment.CENTER);
            Row rangeRow = sheet.createRow(1);
            Cell rangeCell = rangeRow.createCell(0);
            rangeCell.setCellValue("Từ ngày " + fromDate + " đến ngày " + toDate);
            rangeCell.setCellStyle(rangeStyle);
            sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 7));

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_TEAL.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            addBorders(headerStyle);

            CellStyle bodyStyle = workbook.createCellStyle();
            bodyStyle.setVerticalAlignment(VerticalAlignment.TOP);
            bodyStyle.setWrapText(true);
            addBorders(bodyStyle);

            String[] headers = {
                "STT", "Thời gian", "Người thực hiện", "Mã tài khoản", "Hành động", "Đối tượng", "Mô tả", "Địa chỉ IP"
            };
            Row headerRow = sheet.createRow(3);
            headerRow.setHeightInPoints(24);
            for (int column = 0; column < headers.length; column++) {
                Cell cell = headerRow.createCell(column);
                cell.setCellValue(headers[column]);
                cell.setCellStyle(headerStyle);
            }

            for (int index = 0; index < logs.size(); index++) {
                AuditLogDocument log = logs.get(index);
                Row row = sheet.createRow(index + 4);
                setCell(row, 0, index + 1, bodyStyle);
                setCell(
                        row,
                        1,
                        log.getCreatedAt() == null
                                ? ""
                                : EXPORT_DATE_TIME.format(log.getCreatedAt().atZone(BUSINESS_ZONE)),
                        bodyStyle);
                setCell(row, 2, valueOr(log.getUserName(), "Hệ thống"), bodyStyle);
                setCell(row, 3, valueOr(log.getUserId(), ""), bodyStyle);
                setCell(row, 4, valueOr(log.getAction(), ""), bodyStyle);
                setCell(row, 5, valueOr(log.getResourceType(), "")
                        + (log.getResourceId() == null || log.getResourceId().isBlank() ? "" : " #" + log.getResourceId()), bodyStyle);
                setCell(row, 6, valueOr(log.getDescription(), ""), bodyStyle);
                setCell(row, 7, valueOr(log.getIpAddress(), ""), bodyStyle);
            }

            int[] widths = {8, 22, 24, 28, 20, 28, 52, 18};
            for (int column = 0; column < widths.length; column++) {
                sheet.setColumnWidth(column, widths[column] * 256);
            }
            sheet.setAutoFilter(new CellRangeAddress(3, Math.max(3, logs.size() + 3), 0, 7));
            workbook.write(output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new AppException(ErrorCode.FILE_STORAGE_ERROR);
        }
    }

    private void setCell(Row row, int column, Object value, CellStyle style) {
        Cell cell = row.createCell(column);
        if (value instanceof Number number) {
            cell.setCellValue(number.doubleValue());
        } else {
            cell.setCellValue(value == null ? "" : String.valueOf(value));
        }
        cell.setCellStyle(style);
    }

    private void addBorders(CellStyle style) {
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
    }

    private String valueOr(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    // Hàm findAuditLog: Nhận mã hoặc điều kiện tìm kiếm của findAuditLog, truy vấn bản ghi/quan hệ tương ứng, báo lỗi
    // khi không tồn tại và trả về dữ liệu đã ánh xạ.
    private AuditLogDocument findAuditLog(String auditLogId) {
        if (auditLogId == null || auditLogId.isBlank()) {
            throw new AppException(ErrorCode.AUDIT_LOG_NOT_FOUND);
        }
        return auditLogRepository
                .findById(auditLogId.trim())
                .orElseThrow(() -> new AppException(ErrorCode.AUDIT_LOG_NOT_FOUND));
    }

    // Hàm mapToResponses: Nhận danh sách AuditLogEntity; ánh xạ từng bản ghi sang DTO, định dạng actor/action/thời gian
    // để trả về danh sách cho giao diện.
    private List<AuditLogResponse> mapToResponses(List<AuditLogDocument> auditLogs) {
        return auditLogs.stream().map(auditLogMapper::toAuditLogResponse).toList();
    }
}
