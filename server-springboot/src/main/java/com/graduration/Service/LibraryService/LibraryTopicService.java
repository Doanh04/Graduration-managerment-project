package com.graduration.Service.LibraryService;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.graduration.Configuration.PaginationSupport;
import com.graduration.DTO.Request.LibraryTopicRequest;
import com.graduration.DTO.Response.LibraryTopicResponse;
import com.graduration.Repository.LibraryTopicRepository;
import com.graduration.entity.LibraryTopicEntity;
import com.graduration.exception.AppException;
import com.graduration.exception.ErrorCode;
import com.graduration.mapper.LibraryTopicMapper;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class LibraryTopicService {
    LibraryTopicRepository libraryTopicRepository;
    LibraryTopicMapper libraryTopicMapper;

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY')")
    @Transactional
    // Hàm createLibraryTopic: Nhận dữ liệu đầu vào của createLibraryTopic, kiểm tra các trường bắt buộc và quan hệ liên
    // quan, tạo bản ghi nghiệp vụ rồi lưu repository để trả kết quả cho API.
    public LibraryTopicResponse createLibraryTopic(LibraryTopicRequest request) {
        normalize(request);
        if (libraryTopicRepository.existsByTitleIgnoreCase(request.getTitle())) {
            throw new AppException(ErrorCode.LIBRARY_TOPIC_ALREADY_EXISTS);
        }

        LibraryTopicEntity libraryTopic = libraryTopicMapper.toLibraryTopicEntity(request);
        return libraryTopicMapper.toLibraryTopicResponse(libraryTopicRepository.save(libraryTopic));
    }

    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    // Hàm getLibraryTopic: Nhận mã hoặc điều kiện tìm kiếm của getLibraryTopic, truy vấn bản ghi/quan hệ tương ứng, báo
    // lỗi khi không tồn tại và trả về dữ liệu đã ánh xạ.
    public LibraryTopicResponse getLibraryTopic(Long idLibraryTopic) {
        return libraryTopicMapper.toLibraryTopicResponse(findLibraryTopic(idLibraryTopic));
    }

    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    // Hàm getAllLibraryTopics: Nhận các tham số lọc/phân trang của getAllLibraryTopics, truy vấn dữ liệu phù hợp từ
    // repository, ánh xạ từng entity sang DTO và trả về cho giao diện.
    public List<LibraryTopicResponse> getAllLibraryTopics() {
        return getAllLibraryTopics(0, PaginationSupport.DEFAULT_SIZE);
    }

    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    // Hàm getAllLibraryTopics: Nhận các tham số lọc/phân trang của getAllLibraryTopics, truy vấn dữ liệu phù hợp từ
    // repository, ánh xạ từng entity sang DTO và trả về cho giao diện.
    public List<LibraryTopicResponse> getAllLibraryTopics(Integer page, Integer size) {
        return libraryTopicRepository.findAll(PaginationSupport.pageRequest(page, size)).stream()
                .map(libraryTopicMapper::toLibraryTopicResponse)
                .toList();
    }

    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    // Hàm getAllLibraryTopicsPage: Nhận các tham số lọc/phân trang của getAllLibraryTopicsPage, truy vấn dữ liệu phù
    // hợp từ repository, ánh xạ từng entity sang DTO và trả về cho giao diện.
    public com.graduration.DTO.Response.PageResponse<LibraryTopicResponse> getAllLibraryTopicsPage(
            Integer page, Integer size) {
        return com.graduration.DTO.Response.PageResponse.from(
                libraryTopicRepository.findAll(PaginationSupport.pageRequest(page, size)),
                libraryTopicMapper::toLibraryTopicResponse);
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY')")
    @Transactional
    // Hàm updateLibraryTopic: Nhận mã bản ghi cùng dữ liệu cập nhật của updateLibraryTopic, tải bản ghi hiện có, kiểm
    // tra trạng thái và ràng buộc rồi ghi các giá trị mới xuống repository.
    public LibraryTopicResponse updateLibraryTopic(Long idLibraryTopic, LibraryTopicRequest request) {
        LibraryTopicEntity libraryTopic = findLibraryTopic(idLibraryTopic);
        normalize(request);
        if (libraryTopicRepository.existsByTitleIgnoreCaseAndIdLibraryTopicNot(request.getTitle(), idLibraryTopic)) {
            throw new AppException(ErrorCode.LIBRARY_TOPIC_ALREADY_EXISTS);
        }

        libraryTopicMapper.updateLibraryTopic(request, libraryTopic);
        return libraryTopicMapper.toLibraryTopicResponse(libraryTopicRepository.save(libraryTopic));
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY')")
    @Transactional
    // Hàm deleteLibraryTopic: Nhận mã bản ghi của deleteLibraryTopic, kiểm tra quyền và các quan hệ đang sử dụng, sau
    // đó xóa hoặc chuyển bản ghi sang trạng thái tương ứng.
    public void deleteLibraryTopic(Long idLibraryTopic) {
        libraryTopicRepository.delete(findLibraryTopic(idLibraryTopic));
    }

    // Hàm findLibraryTopic: Nhận mã hoặc điều kiện tìm kiếm của findLibraryTopic, truy vấn bản ghi/quan hệ tương ứng,
    // báo lỗi khi không tồn tại và trả về dữ liệu đã ánh xạ.
    private LibraryTopicEntity findLibraryTopic(Long idLibraryTopic) {
        if (idLibraryTopic == null) {
            throw new AppException(ErrorCode.LIBRARY_TOPIC_NOT_FOUND);
        }
        return libraryTopicRepository
                .findById(idLibraryTopic)
                .orElseThrow(() -> new AppException(ErrorCode.LIBRARY_TOPIC_NOT_FOUND));
    }

    // Hàm normalize: Nhận Library1TopicRequest; bắt buộc tiêu đề, trim tiêu đề và chuẩn hóa mô tả, mục tiêu, công nghệ
    // tùy chọn trước khi lưu thư viện đề tài.
    private void normalize(LibraryTopicRequest request) {
        if (request == null || request.getTitle() == null || request.getTitle().isBlank()) {
            throw new AppException(ErrorCode.LIBRARY_TOPIC_TITLE_NOT_BLANK);
        }
        request.setTitle(request.getTitle().trim());
        request.setDescription(normalizeNullable(request.getDescription()));
        request.setObjective(normalizeNullable(request.getObjective()));
        request.setTechnology(normalizeNullable(request.getTechnology()));
    }

    // Hàm normalizeNullable: Nhận giá trị văn bản không bắt buộc từ request; chuyển chuỗi null hoặc chỉ có khoảng trắng
    // thành null và trim chuỗi có nội dung trước khi lưu.
    private String normalizeNullable(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
