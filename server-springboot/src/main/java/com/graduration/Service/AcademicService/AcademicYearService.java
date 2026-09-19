package com.graduration.Service.AcademicService;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.graduration.Configuration.PaginationSupport;
import com.graduration.DTO.Request.AcademicYearRequest;
import com.graduration.DTO.Response.AcademicYearResponse;
import com.graduration.Repository.AcademicYearRepository;
import com.graduration.entity.AcademicYearEntity;
import com.graduration.exception.AppException;
import com.graduration.exception.ErrorCode;
import com.graduration.mapper.AcademicYearMapper;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AcademicYearService {
    AcademicYearRepository academicYearRepository;
    AcademicYearMapper academicYearMapper;

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY')")
    @Transactional
    // Hàm createAcademicYear: Nhận dữ liệu đầu vào của createAcademicYear, kiểm tra các trường bắt buộc và quan hệ liên
    // quan, tạo bản ghi nghiệp vụ rồi lưu repository để trả kết quả cho API.
    public AcademicYearResponse createAcademicYear(AcademicYearRequest request) {
        validateAndNormalize(request);
        if (academicYearRepository.existsByAcademicYearIgnoreCase(request.getAcademicYear())) {
            throw new AppException(ErrorCode.ACADEMIC_YEAR_ALREADY_EXISTS);
        }

        AcademicYearEntity academicYear = academicYearMapper.toAcademicYearEntity(request);
        return academicYearMapper.toAcademicYearResponse(academicYearRepository.save(academicYear));
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY', 'ROLE_SUPERVISOR')")
    @Transactional(readOnly = true)
    // Hàm getAcademicYear: Nhận mã hoặc điều kiện tìm kiếm của getAcademicYear, truy vấn bản ghi/quan hệ tương ứng, báo
    // lỗi khi không tồn tại và trả về dữ liệu đã ánh xạ.
    public AcademicYearResponse getAcademicYear(Integer academicId) {
        return academicYearMapper.toAcademicYearResponse(findAcademicYear(academicId));
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY', 'ROLE_SUPERVISOR')")
    @Transactional(readOnly = true)
    // Hàm getAcademicYearByName: Nhận mã hoặc điều kiện tìm kiếm của getAcademicYearByName, truy vấn bản ghi/quan hệ
    // tương ứng, báo lỗi khi không tồn tại và trả về dữ liệu đã ánh xạ.
    public AcademicYearResponse getAcademicYearByName(String academicYear) {
        String normalized = normalizeAcademicYear(academicYear);
        return academicYearMapper.toAcademicYearResponse(academicYearRepository
                .findByAcademicYearIgnoreCase(normalized)
                .orElseThrow(() -> new AppException(ErrorCode.ACADEMIC_YEAR_NOT_FOUND)));
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY', 'ROLE_SUPERVISOR')")
    @Transactional(readOnly = true)
    // Hàm getAllAcademicYears: Nhận các tham số lọc/phân trang của getAllAcademicYears, truy vấn dữ liệu phù hợp từ
    // repository, ánh xạ từng entity sang DTO và trả về cho giao diện.
    public List<AcademicYearResponse> getAllAcademicYears() {
        return getAllAcademicYears(0, PaginationSupport.DEFAULT_SIZE);
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY', 'ROLE_SUPERVISOR')")
    @Transactional(readOnly = true)
    // Hàm getAllAcademicYears: Nhận các tham số lọc/phân trang của getAllAcademicYears, truy vấn dữ liệu phù hợp từ
    // repository, ánh xạ từng entity sang DTO và trả về cho giao diện.
    public List<AcademicYearResponse> getAllAcademicYears(Integer page, Integer size) {
        return academicYearRepository.findAll(PaginationSupport.pageRequest(page, size)).stream()
                .map(academicYearMapper::toAcademicYearResponse)
                .toList();
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY', 'ROLE_SUPERVISOR')")
    @Transactional(readOnly = true)
    // Hàm getAllAcademicYearsPage: Nhận các tham số lọc/phân trang của getAllAcademicYearsPage, truy vấn dữ liệu phù
    // hợp từ repository, ánh xạ từng entity sang DTO và trả về cho giao diện.
    public com.graduration.DTO.Response.PageResponse<AcademicYearResponse> getAllAcademicYearsPage(
            Integer page, Integer size) {
        return com.graduration.DTO.Response.PageResponse.from(
                academicYearRepository.findAll(PaginationSupport.pageRequest(page, size)),
                academicYearMapper::toAcademicYearResponse);
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY')")
    @Transactional
    // Hàm updateAcademicYear: Nhận mã bản ghi cùng dữ liệu cập nhật của updateAcademicYear, tải bản ghi hiện có, kiểm
    // tra trạng thái và ràng buộc rồi ghi các giá trị mới xuống repository.
    public AcademicYearResponse updateAcademicYear(Integer academicId, AcademicYearRequest request) {
        AcademicYearEntity academicYear = findAcademicYear(academicId);
        validateAndNormalize(request);
        if (academicYearRepository.existsByAcademicYearIgnoreCaseAndAcademicIdNot(
                request.getAcademicYear(), academicId)) {
            throw new AppException(ErrorCode.ACADEMIC_YEAR_ALREADY_EXISTS);
        }

        academicYearMapper.updateAcademicYear(request, academicYear);
        return academicYearMapper.toAcademicYearResponse(academicYearRepository.save(academicYear));
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY')")
    @Transactional
    // Hàm deleteAcademicYear: Nhận mã bản ghi của deleteAcademicYear, kiểm tra quyền và các quan hệ đang sử dụng, sau
    // đó xóa hoặc chuyển bản ghi sang trạng thái tương ứng.
    public void deleteAcademicYear(Integer academicId) {
        AcademicYearEntity academicYear = findAcademicYear(academicId);
        if (!academicYear.getDefensePeriod().isEmpty()) {
            throw new AppException(ErrorCode.ACADEMIC_YEAR_IN_USE);
        }
        academicYearRepository.delete(academicYear);
    }

    // Hàm findAcademicYear: Nhận mã hoặc điều kiện tìm kiếm của findAcademicYear, truy vấn bản ghi/quan hệ tương ứng,
    // báo lỗi khi không tồn tại và trả về dữ liệu đã ánh xạ.
    private AcademicYearEntity findAcademicYear(Integer academicId) {
        if (academicId == null) {
            throw new AppException(ErrorCode.ACADEMIC_YEAR_NOT_FOUND);
        }
        return academicYearRepository
                .findById(academicId)
                .orElseThrow(() -> new AppException(ErrorCode.ACADEMIC_YEAR_NOT_FOUND));
    }

    // Kiểm tra các điều kiện và quy tắc nghiệp vụ trước khi tiếp tục xử lý.
    private void validateAndNormalize(AcademicYearRequest request) {
        if (request == null) {
            throw new AppException(ErrorCode.ACADEMIC_YEAR_INVALID);
        }
        request.setAcademicYear(normalizeAcademicYear(request.getAcademicYear()));
        request.setDescription(normalize(request.getDescription()));
    }

    // Hàm normalizeAcademicYear: Nhận chuỗi năm học từ request; trim, đổi dấu gạch chéo hoặc gạch nối về định dạng
    // YYYY-YYYY, kiểm tra hai năm hợp lệ và bảo đảm năm kết thúc lớn hơn năm bắt đầu.
    private String normalizeAcademicYear(String value) {
        if (value == null || value.isBlank()) {
            throw new AppException(ErrorCode.ACADEMIC_YEAR_NOT_BLANK);
        }
        String normalized = value.trim().replaceAll("\\s*[/\\-]\\s*", "-");
        if (!normalized.matches("\\d{4}-\\d{4}")) {
            throw new AppException(ErrorCode.ACADEMIC_YEAR_INVALID);
        }
        int startYear = Integer.parseInt(normalized.substring(0, 4));
        int endYear = Integer.parseInt(normalized.substring(5));
        if (endYear <= startYear) {
            throw new AppException(ErrorCode.ACADEMIC_YEAR_INVALID);
        }
        return normalized;
    }

    // Hàm normalize: Nhận mô tả năm học từ request; chuyển null/rỗng thành null và trim nội dung trước khi ghi
    // AcademicYearEntity.
    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
