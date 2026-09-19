package com.graduration.Service.RoleService;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.graduration.Configuration.PaginationSupport;
import com.graduration.Constain.PermissionConstain;
import com.graduration.Constain.RoleConstain;
import com.graduration.DTO.Request.CreateRoleRequest;
import com.graduration.DTO.Request.UpdateRoleRequest;
import com.graduration.DTO.Response.RoleResponse;
import com.graduration.Repository.PermissionRepository;
import com.graduration.Repository.RoleRepository;
import com.graduration.entity.PermissionEntity;
import com.graduration.entity.Roles;
import com.graduration.exception.AppException;
import com.graduration.exception.ErrorCode;
import com.graduration.mapper.RoleMaper;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RoleService {
    RoleRepository roleRepository;
    PermissionRepository permissionRepository;
    RoleMaper roleMaper;

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Transactional
    // Hàm createRole: Nhận dữ liệu đầu vào của createRole, kiểm tra các trường bắt buộc và quan hệ liên quan, tạo bản
    // ghi nghiệp vụ rồi lưu repository để trả kết quả cho API.
    public RoleResponse createRole(CreateRoleRequest request) {
        if (roleRepository.existsById(request.getRole()) || roleRepository.existsByRoleName(request.getRoleName())) {
            throw new AppException(ErrorCode.ROLE_IS_EXITED);
        }

        Roles role = roleMaper.toRoleEntity(request);
        role.setPermission(findPermissions(request.getPermissions()));

        return roleMaper.toRoleResponse(roleRepository.save(role));
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Transactional
    // Hàm updateRole: Nhận mã bản ghi cùng dữ liệu cập nhật của updateRole, tải bản ghi hiện có, kiểm tra trạng thái và
    // ràng buộc rồi ghi các giá trị mới xuống repository.
    public RoleResponse updateRole(RoleConstain roleId, UpdateRoleRequest request) {
        Roles role = findRole(roleId);

        if (roleRepository.existsByRoleNameAndRoleNot(request.getRoleName(), roleId)) {
            throw new AppException(ErrorCode.ROLE_IS_EXITED);
        }

        roleMaper.updateRoleEntity(request, role);
        role.setPermission(findPermissions(request.getPermissions()));

        return roleMaper.toRoleResponse(roleRepository.save(role));
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Transactional
    // Hàm deleteRole: Nhận mã bản ghi của deleteRole, kiểm tra quyền và các quan hệ đang sử dụng, sau đó xóa hoặc
    // chuyển bản ghi sang trạng thái tương ứng.
    public void deleteRole(RoleConstain roleId) {
        Roles role = findRole(roleId);

        role.getUser().forEach(user -> user.getRoles().remove(role));
        role.getUser().clear();
        role.getPermission().clear();
        roleRepository.delete(role);
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Transactional(readOnly = true)
    // Hàm getRole: Nhận mã hoặc điều kiện tìm kiếm của getRole, truy vấn bản ghi/quan hệ tương ứng, báo lỗi khi không
    // tồn tại và trả về dữ liệu đã ánh xạ.
    public RoleResponse getRole(RoleConstain roleId) {
        return roleMaper.toRoleResponse(findRole(roleId));
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Transactional(readOnly = true)
    // Hàm getAllRoles: Nhận các tham số lọc/phân trang của getAllRoles, truy vấn dữ liệu phù hợp từ repository, ánh xạ
    // từng entity sang DTO và trả về cho giao diện.
    public List<RoleResponse> getAllRoles() {
        return getAllRoles(0, PaginationSupport.DEFAULT_SIZE);
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Transactional(readOnly = true)
    // Hàm getAllRoles: Nhận các tham số lọc/phân trang của getAllRoles, truy vấn dữ liệu phù hợp từ repository, ánh xạ
    // từng entity sang DTO và trả về cho giao diện.
    public List<RoleResponse> getAllRoles(Integer page, Integer size) {
        return roleRepository.findAll(PaginationSupport.pageRequest(page, size)).stream()
                .map(roleMaper::toRoleResponse)
                .toList();
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Transactional(readOnly = true)
    // Hàm getAllRolesPage: Nhận các tham số lọc/phân trang của getAllRolesPage, truy vấn dữ liệu phù hợp từ repository,
    // ánh xạ từng entity sang DTO và trả về cho giao diện.
    public com.graduration.DTO.Response.PageResponse<RoleResponse> getAllRolesPage(Integer page, Integer size) {
        return com.graduration.DTO.Response.PageResponse.from(
                roleRepository.findAll(PaginationSupport.pageRequest(page, size)), roleMaper::toRoleResponse);
    }

    // Hàm findRole: Nhận mã hoặc điều kiện tìm kiếm của findRole, truy vấn bản ghi/quan hệ tương ứng, báo lỗi khi không
    // tồn tại và trả về dữ liệu đã ánh xạ.
    private Roles findRole(RoleConstain roleId) {
        return roleRepository.findById(roleId).orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));
    }

    // Hàm findPermissions: Nhận mã hoặc điều kiện tìm kiếm của findPermissions, truy vấn bản ghi/quan hệ tương ứng, báo
    // lỗi khi không tồn tại và trả về dữ liệu đã ánh xạ.
    private Set<PermissionEntity> findPermissions(Set<PermissionConstain> permissionIds) {
        if (permissionIds == null || permissionIds.isEmpty()) {
            return new HashSet<>();
        }

        List<PermissionEntity> permissions = permissionRepository.findAllById(permissionIds);
        if (permissions.size() != permissionIds.size()) {
            throw new AppException(ErrorCode.PERMISSION_NOT_FOUND);
        }

        return new HashSet<>(permissions);
    }
}
