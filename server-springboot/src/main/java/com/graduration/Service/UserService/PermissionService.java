package com.graduration.Service.UserService;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.graduration.Configuration.PaginationSupport;
import com.graduration.Constain.PermissionConstain;
import com.graduration.DTO.Request.CreatePermissionRequest;
import com.graduration.DTO.Request.UpdatePermissionRequest;
import com.graduration.DTO.Response.PermissionResponse;
import com.graduration.Repository.PermissionRepository;
import com.graduration.entity.PermissionEntity;
import com.graduration.exception.AppException;
import com.graduration.exception.ErrorCode;
import com.graduration.mapper.PermissioMapper;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PermissionService {
    PermissionRepository permissionRepository;
    PermissioMapper permissioMapper;

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN')")
    @Transactional
    // Hàm createPermission: Nhận dữ liệu đầu vào của createPermission, kiểm tra các trường bắt buộc và quan hệ liên
    // quan, tạo bản ghi nghiệp vụ rồi lưu repository để trả kết quả cho API.
    public PermissionResponse createPermission(CreatePermissionRequest request) {
        if (permissionRepository.existsById(request.getPermissionId())
                || permissionRepository.existsByPermissionName(request.getPermissionName())) {
            throw new AppException(ErrorCode.PERMISSION_IS_EXITED);
        }

        PermissionEntity permission = permissioMapper.toPermissionEntity(request);
        return permissioMapper.toPermissionResponse(permissionRepository.save(permission));
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Transactional
    // Hàm updatePermission: Nhận mã bản ghi cùng dữ liệu cập nhật của updatePermission, tải bản ghi hiện có, kiểm tra
    // trạng thái và ràng buộc rồi ghi các giá trị mới xuống repository.
    public PermissionResponse updatePermission(PermissionConstain permissionId, UpdatePermissionRequest request) {
        PermissionEntity permission = findPermission(permissionId);

        if (permissionRepository.existsByPermissionNameAndPermissionIdNot(request.getPermissionName(), permissionId)) {
            throw new AppException(ErrorCode.PERMISSION_IS_EXITED);
        }

        permissioMapper.updatePermissionEntity(request, permission);
        return permissioMapper.toPermissionResponse(permissionRepository.save(permission));
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Transactional
    // Hàm deletePermission: Nhận mã bản ghi của deletePermission, kiểm tra quyền và các quan hệ đang sử dụng, sau đó
    // xóa hoặc chuyển bản ghi sang trạng thái tương ứng.
    public void deletePermission(PermissionConstain permissionId) {
        PermissionEntity permission = findPermission(permissionId);

        permission.getRoles().forEach(role -> role.getPermission().remove(permission));
        permission.getRoles().clear();
        permissionRepository.delete(permission);
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Transactional(readOnly = true)
    // Hàm getPermission: Nhận mã hoặc điều kiện tìm kiếm của getPermission, truy vấn bản ghi/quan hệ tương ứng, báo lỗi
    // khi không tồn tại và trả về dữ liệu đã ánh xạ.
    public PermissionResponse getPermission(PermissionConstain permissionId) {
        return permissioMapper.toPermissionResponse(findPermission(permissionId));
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Transactional(readOnly = true)
    // Hàm getAllPermissions: Nhận các tham số lọc/phân trang của getAllPermissions, truy vấn dữ liệu phù hợp từ
    // repository, ánh xạ từng entity sang DTO và trả về cho giao diện.
    public List<PermissionResponse> getAllPermissions() {
        return getAllPermissions(0, PaginationSupport.DEFAULT_SIZE);
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Transactional(readOnly = true)
    // Hàm getAllPermissions: Nhận các tham số lọc/phân trang của getAllPermissions, truy vấn dữ liệu phù hợp từ
    // repository, ánh xạ từng entity sang DTO và trả về cho giao diện.
    public List<PermissionResponse> getAllPermissions(Integer page, Integer size) {
        return permissionRepository.findAll(PaginationSupport.pageRequest(page, size)).stream()
                .map(permissioMapper::toPermissionResponse)
                .toList();
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Transactional(readOnly = true)
    // Hàm getAllPermissionsPage: Nhận các tham số lọc/phân trang của getAllPermissionsPage, truy vấn dữ liệu phù hợp từ
    // repository, ánh xạ từng entity sang DTO và trả về cho giao diện.
    public com.graduration.DTO.Response.PageResponse<PermissionResponse> getAllPermissionsPage(
            Integer page, Integer size) {
        return com.graduration.DTO.Response.PageResponse.from(
                permissionRepository.findAll(PaginationSupport.pageRequest(page, size)),
                permissioMapper::toPermissionResponse);
    }

    // Hàm findPermission: Nhận mã hoặc điều kiện tìm kiếm của findPermission, truy vấn bản ghi/quan hệ tương ứng, báo
    // lỗi khi không tồn tại và trả về dữ liệu đã ánh xạ.
    private PermissionEntity findPermission(PermissionConstain permissionId) {
        return permissionRepository
                .findById(permissionId)
                .orElseThrow(() -> new AppException(ErrorCode.PERMISSION_NOT_FOUND));
    }
}
