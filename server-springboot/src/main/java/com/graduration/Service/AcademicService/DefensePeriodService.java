package com.graduration.Service.AcademicService;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.graduration.Configuration.PaginationSupport;
import com.graduration.Constain.DefensePeriodConstain;
import com.graduration.DTO.Request.DefensePeriodRequest;
import com.graduration.DTO.Response.DefensePeriodResponse;
import com.graduration.Repository.AcademicYearRepository;
import com.graduration.Repository.DefensePeriodRepository;
import com.graduration.entity.AcademicYearEntity;
import com.graduration.entity.DefensePeriodEntity;
import com.graduration.exception.AppException;
import com.graduration.exception.ErrorCode;
import com.graduration.mapper.DefensePeriodMapper;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DefensePeriodService {
    DefensePeriodRepository defensePeriodRepository;
    AcademicYearRepository academicYearRepository;
    DefensePeriodMapper defensePeriodMapper;
    Clock clock = Clock.systemDefaultZone();

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY')")
    @Transactional
    // Hàm createDefensePeriod: Nhận dữ liệu đầu vào của createDefensePeriod, kiểm tra các trường bắt buộc và quan hệ
    // liên quan, tạo bản ghi nghiệp vụ rồi lưu repository để trả kết quả cho API.
    public DefensePeriodResponse createDefensePeriod(DefensePeriodRequest request) {
        validateAndNormalize(request);
        AcademicYearEntity academicYear = findAcademicYear(request.getAcademicId());
        validateUniqueName(request, null);

        DefensePeriodEntity defensePeriod = defensePeriodMapper.toDefensePeriodEntity(request);
        defensePeriod.setAcademicYear(academicYear);
        return defensePeriodMapper.toDefensePeriodResponse(defensePeriodRepository.save(defensePeriod));
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY', 'ROLE_SUPERVISOR', 'ROLE_STUDENT')")
    @Transactional
    // Hàm getDefensePeriod: Nhận mã hoặc điều kiện tìm kiếm của getDefensePeriod, truy vấn bản ghi/quan hệ tương ứng,
    // báo lỗi khi không tồn tại và trả về dữ liệu đã ánh xạ.
    public DefensePeriodResponse getDefensePeriod(Long defensePeriodId) {
        DefensePeriodEntity defensePeriod = findDefensePeriod(defensePeriodId);
        return defensePeriodMapper.toDefensePeriodResponse(defensePeriod);
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY', 'ROLE_SUPERVISOR', 'ROLE_STUDENT')")
    @Transactional
    // Hàm getAllDefensePeriods: Nhận các tham số lọc/phân trang của getAllDefensePeriods, truy vấn dữ liệu phù hợp từ
    // repository, ánh xạ từng entity sang DTO và trả về cho giao diện.
    public List<DefensePeriodResponse> getAllDefensePeriods() {
        return getAllDefensePeriods(0, PaginationSupport.DEFAULT_SIZE);
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY', 'ROLE_SUPERVISOR', 'ROLE_STUDENT')")
    @Transactional
    // Hàm getAllDefensePeriods: Nhận các tham số lọc/phân trang của getAllDefensePeriods, truy vấn dữ liệu phù hợp từ
    // repository, ánh xạ từng entity sang DTO và trả về cho giao diện.
    public List<DefensePeriodResponse> getAllDefensePeriods(Integer page, Integer size) {
        return defensePeriodRepository.findAllByOrderByStartDateDesc(PaginationSupport.pageRequest(page, size)).stream()
                .map(defensePeriodMapper::toDefensePeriodResponse)
                .toList();
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY', 'ROLE_SUPERVISOR', 'ROLE_STUDENT')")
    @Transactional
    // Hàm getAllDefensePeriodsPage: Nhận các tham số lọc/phân trang của getAllDefensePeriodsPage, truy vấn dữ liệu phù
    // hợp từ repository, ánh xạ từng entity sang DTO và trả về cho giao diện.
    public com.graduration.DTO.Response.PageResponse<DefensePeriodResponse> getAllDefensePeriodsPage(
            Integer page, Integer size) {
        return com.graduration.DTO.Response.PageResponse.from(
                defensePeriodRepository.findAllByOrderByStartDateDesc(PaginationSupport.pageRequest(page, size)),
                defensePeriodMapper::toDefensePeriodResponse);
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY', 'ROLE_SUPERVISOR', 'ROLE_STUDENT')")
    @Transactional
    // Hàm getDefensePeriodsByAcademicYear: Nhận mã hoặc điều kiện tìm kiếm của getDefensePeriodsByAcademicYear, truy
    // vấn bản ghi/quan hệ tương ứng, báo lỗi khi không tồn tại và trả về dữ liệu đã ánh xạ.
    public List<DefensePeriodResponse> getDefensePeriodsByAcademicYear(Integer academicId) {
        return getDefensePeriodsByAcademicYear(academicId, 0, PaginationSupport.DEFAULT_SIZE);
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY', 'ROLE_SUPERVISOR')")
    @Transactional
    // Hàm getDefensePeriodsByAcademicYear: Nhận mã hoặc điều kiện tìm kiếm của getDefensePeriodsByAcademicYear, truy
    // vấn bản ghi/quan hệ tương ứng, báo lỗi khi không tồn tại và trả về dữ liệu đã ánh xạ.
    public List<DefensePeriodResponse> getDefensePeriodsByAcademicYear(Integer academicId, Integer page, Integer size) {
        findAcademicYear(academicId);
        return defensePeriodRepository
                .findAllByAcademicYear_AcademicIdOrderByStartDateDesc(
                        academicId, PaginationSupport.pageRequest(page, size))
                .stream()
                .map(defensePeriodMapper::toDefensePeriodResponse)
                .toList();
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY')")
    @Transactional
    // Hàm updateDefensePeriod: Nhận mã bản ghi cùng dữ liệu cập nhật của updateDefensePeriod, tải bản ghi hiện có, kiểm
    // tra trạng thái và ràng buộc rồi ghi các giá trị mới xuống repository.
    public DefensePeriodResponse updateDefensePeriod(Long defensePeriodId, DefensePeriodRequest request) {
        DefensePeriodEntity defensePeriod = findDefensePeriod(defensePeriodId);
        validateAndNormalize(request);
        AcademicYearEntity academicYear = findAcademicYear(request.getAcademicId());
        validateUniqueName(request, defensePeriodId);

        defensePeriodMapper.updateDefensePeriod(request, defensePeriod);
        defensePeriod.setAcademicYear(academicYear);
        return defensePeriodMapper.toDefensePeriodResponse(defensePeriodRepository.save(defensePeriod));
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY')")
    @Transactional
    // Hàm deleteDefensePeriod: Nhận mã bản ghi của deleteDefensePeriod, kiểm tra quyền và các quan hệ đang sử dụng, sau
    // đó xóa hoặc chuyển bản ghi sang trạng thái tương ứng.
    public void deleteDefensePeriod(Long defensePeriodId) {
        DefensePeriodEntity defensePeriod = findDefensePeriod(defensePeriodId);
        if (!defensePeriod.getTopic().isEmpty()
                || !defensePeriod.getMilesStone().isEmpty()) {
            throw new AppException(ErrorCode.DEFENSE_PERIOD_IN_USE);
        }
        defensePeriodRepository.delete(defensePeriod);
    }

    @Transactional
    // Hàm finishExpiredPeriods: Quét các đợt bảo vệ đã qua ngày kết thúc nhưng còn trạng thái mở, chuyển chúng sang
    // FINISHED và lưu để các nghiệp vụ khác không tiếp tục sử dụng.
    public int finishExpiredPeriods() {
        return defensePeriodRepository.markExpiredPeriodsFinished(LocalDate.now(clock), DefensePeriodConstain.FINISHED);
    }

    // Hàm findDefensePeriod: Nhận mã hoặc điều kiện tìm kiếm của findDefensePeriod, truy vấn bản ghi/quan hệ tương ứng,
    // báo lỗi khi không tồn tại và trả về dữ liệu đã ánh xạ.
    private DefensePeriodEntity findDefensePeriod(Long defensePeriodId) {
        if (defensePeriodId == null) {
            throw new AppException(ErrorCode.DEFENSE_PERIOD_NOT_FOUND);
        }
        return defensePeriodRepository
                .findById(defensePeriodId)
                .orElseThrow(() -> new AppException(ErrorCode.DEFENSE_PERIOD_NOT_FOUND));
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
    private void validateUniqueName(DefensePeriodRequest request, Long defensePeriodId) {
        boolean exists = defensePeriodId == null
                ? defensePeriodRepository.existsByPeriodNameIgnoreCaseAndAcademicYear_AcademicId(
                        request.getPeriodName(), request.getAcademicId())
                : defensePeriodRepository.existsDuplicatePeriodName(
                        request.getPeriodName(), request.getAcademicId(), defensePeriodId);
        if (exists) {
            throw new AppException(ErrorCode.DEFENSE_PERIOD_ALREADY_EXISTS);
        }
    }

    // Kiểm tra các điều kiện và quy tắc nghiệp vụ trước khi tiếp tục xử lý.
    private void validateAndNormalize(DefensePeriodRequest request) {
        if (request == null
                || request.getPeriodName() == null
                || request.getPeriodName().isBlank()
                || request.getStartDate() == null
                || request.getEndDate() == null
                || request.getProjectType() == null
                || request.getProjectType().isBlank()
                || request.getStatus() == null) {
            throw new AppException(ErrorCode.DEFENSE_PERIOD_INVALID);
        }
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new AppException(ErrorCode.DEFENSE_PERIOD_INVALID_DATE);
        }
        request.setPeriodName(request.getPeriodName().trim());
        request.setProjectType(normalize(request.getProjectType()));
    }

    // Hàm normalize: Nhận tên, mô tả hoặc địa điểm của đợt bảo vệ; chuyển null/rỗng thành null và trim chuỗi có nội
    // dung để kiểm tra trùng và lưu.
    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
