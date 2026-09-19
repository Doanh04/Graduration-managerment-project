package com.graduration.Service.DerpatmentService;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.graduration.Configuration.PaginationSupport;
import com.graduration.DTO.Request.MajorRequest;
import com.graduration.DTO.Response.MajorResponse;
import com.graduration.Repository.MajorRepository;
import com.graduration.entity.MajorEntity;
import com.graduration.exception.AppException;
import com.graduration.exception.ErrorCode;
import com.graduration.mapper.MajorMapper;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MajorService {
    MajorRepository majorRepository;
    MajorMapper majorMapper;

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY')")
    @Transactional
    // Hàm createMajor: Nhận dữ liệu đầu vào của createMajor, kiểm tra các trường bắt buộc và quan hệ liên quan, tạo bản
    // ghi nghiệp vụ rồi lưu repository để trả kết quả cho API.
    public MajorResponse createMajor(MajorRequest request) {
        validateAndNormalizeRequest(request);
        if (majorRepository.existsByMajorNameIgnoreCase(request.getMajorName())
                || majorRepository.existsByMajorCodeIgnoreCase(request.getMajorCode())) {
            throw new AppException(ErrorCode.INVALID_KEY);
        }

        MajorEntity major = majorMapper.toMajorEntity(request);
        return majorMapper.toMajorResponse(majorRepository.save(major));
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY')")
    @Transactional(readOnly = true)
    // Hàm getMajor: Nhận mã hoặc điều kiện tìm kiếm của getMajor, truy vấn bản ghi/quan hệ tương ứng, báo lỗi khi không
    // tồn tại và trả về dữ liệu đã ánh xạ.
    public MajorResponse getMajor(Long majorId) {
        return majorMapper.toMajorResponse(findMajor(majorId));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY')")
    // Hàm getAllMajors: Nhận các tham số lọc/phân trang của getAllMajors, truy vấn dữ liệu phù hợp từ repository, ánh
    // xạ từng entity sang DTO và trả về cho giao diện.
    public List<MajorResponse> getAllMajors() {
        return getAllMajors(0, PaginationSupport.DEFAULT_SIZE);
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY')")
    @Transactional(readOnly = true)
    // Hàm getAllMajors: Nhận các tham số lọc/phân trang của getAllMajors, truy vấn dữ liệu phù hợp từ repository, ánh
    // xạ từng entity sang DTO và trả về cho giao diện.
    public List<MajorResponse> getAllMajors(Integer page, Integer size) {
        return majorRepository.findAll(PaginationSupport.pageRequest(page, size)).stream()
                .map(majorMapper::toMajorResponse)
                .toList();
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY')")
    @Transactional(readOnly = true)
    // Hàm getAllMajorsPage: Nhận các tham số lọc/phân trang của getAllMajorsPage, truy vấn dữ liệu phù hợp từ
    // repository, ánh xạ từng entity sang DTO và trả về cho giao diện.
    public com.graduration.DTO.Response.PageResponse<MajorResponse> getAllMajorsPage(Integer page, Integer size) {
        return com.graduration.DTO.Response.PageResponse.from(
                majorRepository.findAll(PaginationSupport.pageRequest(page, size)), majorMapper::toMajorResponse);
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY')")
    @Transactional
    // Hàm updateMajor: Nhận mã bản ghi cùng dữ liệu cập nhật của updateMajor, tải bản ghi hiện có, kiểm tra trạng thái
    // và ràng buộc rồi ghi các giá trị mới xuống repository.
    public MajorResponse updateMajor(Long majorId, MajorRequest request) {
        MajorEntity major = findMajor(majorId);
        validateAndNormalizeRequest(request);
        if (majorRepository.existsByMajorNameIgnoreCaseAndMajorIdNot(request.getMajorName(), majorId)
                || majorRepository.existsByMajorCodeIgnoreCaseAndMajorIdNot(request.getMajorCode(), majorId)) {
            throw new AppException(ErrorCode.INVALID_KEY);
        }

        MajorEntity mappedMajor = majorMapper.toMajorEntity(request);
        major.setMajorCode(mappedMajor.getMajorCode());
        major.setMajorName(mappedMajor.getMajorName());
        major.setDescription(mappedMajor.getDescription());

        return majorMapper.toMajorResponse(majorRepository.save(major));
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY')")
    @Transactional
    // Hàm deleteMajor: Nhận mã bản ghi của deleteMajor, kiểm tra quyền và các quan hệ đang sử dụng, sau đó xóa hoặc
    // chuyển bản ghi sang trạng thái tương ứng.
    public void deleteMajor(Long majorId) {
        MajorEntity major = findMajor(majorId);
        if (!major.getClassEntity().isEmpty()) {
            throw new AppException(ErrorCode.MAJOR_IN_USE);
        }
        majorRepository.delete(major);
    }

    // Hàm findMajor: Nhận mã hoặc điều kiện tìm kiếm của findMajor, truy vấn bản ghi/quan hệ tương ứng, báo lỗi khi
    // không tồn tại và trả về dữ liệu đã ánh xạ.
    private MajorEntity findMajor(Long majorId) {
        if (majorId == null) {
            throw new AppException(ErrorCode.INVALID_KEY);
        }

        return majorRepository.findById(majorId).orElseThrow(() -> new AppException(ErrorCode.INVALID_KEY));
    }

    // Kiểm tra các điều kiện và quy tắc nghiệp vụ trước khi tiếp tục xử lý.
    private void validateAndNormalizeRequest(MajorRequest request) {
        if (request == null
                || request.getMajorCode() == null
                || request.getMajorCode().isBlank()
                || request.getMajorName() == null
                || request.getMajorName().isBlank()) {
            throw new AppException(ErrorCode.INVALID_KEY);
        }

        request.setMajorCode(request.getMajorCode().trim().toUpperCase());
        request.setMajorName(request.getMajorName().trim());
        if (request.getDescription() != null) {
            String description = request.getDescription().trim();
            request.setDescription(description.isEmpty() ? null : description);
        }
    }
}
