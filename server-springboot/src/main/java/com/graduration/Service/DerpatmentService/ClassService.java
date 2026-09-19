package com.graduration.Service.DerpatmentService;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.graduration.Configuration.PaginationSupport;
import com.graduration.DTO.Request.ClassRequest;
import com.graduration.DTO.Response.ClassResponse;
import com.graduration.Repository.ClassRepository;
import com.graduration.Repository.MajorRepository;
import com.graduration.entity.ClassEntity;
import com.graduration.entity.MajorEntity;
import com.graduration.exception.AppException;
import com.graduration.exception.ErrorCode;
import com.graduration.mapper.ClassMapper;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ClassService {
    ClassRepository classRepository;
    MajorRepository majorRepository;
    ClassMapper classMapper;

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY')")
    @Transactional
    // Hàm createClass: Nhận dữ liệu đầu vào của createClass, kiểm tra các trường bắt buộc và quan hệ liên quan, tạo bản
    // ghi nghiệp vụ rồi lưu repository để trả kết quả cho API.
    public ClassResponse createClass(ClassRequest request) {
        normalizeRequest(request);

        if (classRepository.existsByClassCodeIgnoreCase(request.getClassCode())
                || classRepository.existsByClassNameIgnoreCase(request.getNameClass())) {
            throw new AppException(ErrorCode.INVALID_KEY);
        }

        MajorEntity major = majorRepository
                .findById(parseMajorId(request.getMajorId()))
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_KEY));
        ClassEntity classEntity = classMapper.toClassEntity(request);
        classEntity.setMajor(major);

        return classMapper.toClassResponse(classRepository.save(classEntity));
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY')")
    @Transactional(readOnly = true)
    // Hàm getClass: Nhận mã hoặc điều kiện tìm kiếm của getClass, truy vấn bản ghi/quan hệ tương ứng, báo lỗi khi không
    // tồn tại và trả về dữ liệu đã ánh xạ.
    public ClassResponse getClass(String classCode) {
        if (classCode == null || classCode.isBlank()) {
            throw new AppException(ErrorCode.INVALID_KEY);
        }

        return classMapper.toClassResponse(classRepository
                .findByClassCodeIgnoreCase(classCode.trim())
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_KEY)));
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY')")
    @Transactional(readOnly = true)
    // Hàm getAllClasses: Nhận các tham số lọc/phân trang của getAllClasses, truy vấn dữ liệu phù hợp từ repository, ánh
    // xạ từng entity sang DTO và trả về cho giao diện.
    public List<ClassResponse> getAllClasses() {
        return getAllClasses(0, PaginationSupport.DEFAULT_SIZE);
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY')")
    @Transactional(readOnly = true)
    // Hàm getAllClasses: Nhận các tham số lọc/phân trang của getAllClasses, truy vấn dữ liệu phù hợp từ repository, ánh
    // xạ từng entity sang DTO và trả về cho giao diện.
    public List<ClassResponse> getAllClasses(Integer page, Integer size) {
        return classRepository.findAll(PaginationSupport.pageRequest(page, size)).stream()
                .map(classMapper::toClassResponse)
                .toList();
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY', 'ROLE_SUPERVISOR')")
    @Transactional(readOnly = true)
    // Hàm getAllClassesPage: Nhận các tham số lọc/phân trang của getAllClassesPage, truy vấn dữ liệu phù hợp từ
    // repository, ánh xạ từng entity sang DTO và trả về cho giao diện.
    public com.graduration.DTO.Response.PageResponse<ClassResponse> getAllClassesPage(Integer page, Integer size) {
        return com.graduration.DTO.Response.PageResponse.from(
                classRepository.findAll(PaginationSupport.pageRequest(page, size)), classMapper::toClassResponse);
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY')")
    @Transactional
    // Hàm updateClass: Nhận mã bản ghi cùng dữ liệu cập nhật của updateClass, tải bản ghi hiện có, kiểm tra trạng thái
    // và ràng buộc rồi ghi các giá trị mới xuống repository.
    public ClassResponse updateClass(Long classId, ClassRequest request) {
        ClassEntity existingClass =
                classRepository.findById(classId).orElseThrow(() -> new AppException(ErrorCode.INVALID_KEY));
        normalizeRequest(request);

        if (classRepository.existsByClassCodeIgnoreCaseAndClassIdNot(request.getClassCode(), classId)
                || classRepository.existsByClassNameIgnoreCaseAndClassIdNot(request.getNameClass(), classId)) {
            throw new AppException(ErrorCode.INVALID_KEY);
        }

        MajorEntity major = majorRepository
                .findById(parseMajorId(request.getMajorId()))
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_KEY));
        ClassEntity mappedClass = classMapper.toClassEntity(request);
        existingClass.setClassCode(mappedClass.getClassCode());
        existingClass.setClassName(mappedClass.getClassName());
        existingClass.setDescription(mappedClass.getDescription());
        existingClass.setMajor(major);

        return classMapper.toClassResponse(classRepository.save(existingClass));
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY')")
    @Transactional
    // Hàm deleteClass: Nhận mã bản ghi của deleteClass, kiểm tra quyền và các quan hệ đang sử dụng, sau đó xóa hoặc
    // chuyển bản ghi sang trạng thái tương ứng.
    public void deleteClass(Long classId) {
        ClassEntity classEntity =
                classRepository.findById(classId).orElseThrow(() -> new AppException(ErrorCode.INVALID_KEY));
        if (!classEntity.getStudent().isEmpty()) {
            throw new AppException(ErrorCode.CLASS_IN_USE);
        }
        classRepository.delete(classEntity);
    }

    // Hàm normalizeRequest: Nhận ClassRequest; kiểm tra mã lớp, tên lớp và mã ngành bắt buộc, trim mã/tên rồi giữ DTO
    // sạch để tạo hoặc cập nhật lớp.
    private void normalizeRequest(ClassRequest request) {
        if (request == null
                || request.getClassCode() == null
                || request.getClassCode().isBlank()
                || request.getNameClass() == null
                || request.getNameClass().isBlank()
                || request.getMajorId() == null
                || request.getMajorId().isBlank()) {
            throw new AppException(ErrorCode.INVALID_KEY);
        }

        request.setClassCode(request.getClassCode().trim());
        request.setNameClass(request.getNameClass().trim());
        request.setMajorId(request.getMajorId().trim());
        if (request.getDescription() != null) {
            String description = request.getDescription().trim();
            request.setDescription(description.isEmpty() ? null : description);
        }
    }

    // Hàm parseMajorId: Nhận mã ngành từ request lớp học; trim giá trị, chuyển sang định dạng định danh dùng truy vấn
    // MajorEntity và báo lỗi nếu không hợp lệ.
    private Long parseMajorId(String majorId) {
        try {
            return Long.valueOf(majorId);
        } catch (NumberFormatException exception) {
            throw new AppException(ErrorCode.INVALID_KEY);
        }
    }
}
