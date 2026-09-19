package com.graduration.Service.UserService;

import java.util.HashSet;
import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.graduration.Constain.RoleConstain;
import com.graduration.DTO.Request.UpdateUserRolesRequest;
import com.graduration.DTO.Response.UserRoleResponse;
import com.graduration.Repository.RoleRepository;
import com.graduration.Repository.UserRepository;
import com.graduration.entity.Roles;
import com.graduration.entity.UserEntity;
import com.graduration.exception.AppException;
import com.graduration.exception.ErrorCode;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserRoleService {
    UserRepository userRepository;
    RoleRepository roleRepository;

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Transactional(readOnly = true)
    // Hàm getUsers: Nhận mã hoặc điều kiện tìm kiếm của getUsers, truy vấn bản ghi/quan hệ tương ứng, báo lỗi khi không
    // tồn tại và trả về dữ liệu đã ánh xạ.
    public List<UserRoleResponse> getUsers() {
        return userRepository.findAll().stream().map(this::toResponse).toList();
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Transactional
    // Hàm updateRoles: Nhận mã bản ghi cùng dữ liệu cập nhật của updateRoles, tải bản ghi hiện có, kiểm tra trạng thái
    // và ràng buộc rồi ghi các giá trị mới xuống repository.
    public UserRoleResponse updateRoles(String userId, UpdateUserRolesRequest request) {
        UserEntity user = userRepository.findById(userId).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        var roleIds = request == null || request.getRoles() == null ? new HashSet<RoleConstain>() : request.getRoles();
        List<Roles> roles = roleRepository.findAllById(roleIds);
        if (roles.size() != roleIds.size()) {
            throw new AppException(ErrorCode.ROLE_NOT_FOUND);
        }
        user.setRoles(new HashSet<>(roles));
        return toResponse(userRepository.save(user));
    }

    // Hàm toResponse: Nhận UserEntity cùng role hiện có; ánh xạ username, hồ sơ hiển thị và danh sách role sang DTO
    // phục vụ màn hình phân quyền.
    private UserRoleResponse toResponse(UserEntity user) {
        String fullName = user.getStudent() != null
                ? user.getStudent().getFullNameStudent()
                : user.getLecture() != null ? user.getLecture().getFullNameLecture() : user.getUserName();
        String userCode = user.getStudent() != null
                ? user.getStudent().getStudentCode()
                : user.getLecture() != null ? user.getLecture().getLectureCode() : null;
        String userType = user.getStudent() != null ? "STUDENT" : user.getLecture() != null ? "LECTURER" : "SYSTEM";
        return UserRoleResponse.builder()
                .userId(user.getUserId())
                .userName(user.getUserName())
                .fullName(fullName)
                .userCode(userCode)
                .userType(userType)
                .status(user.getStatus())
                .roles(user.getRoles().stream().map(Roles::getRole).collect(java.util.stream.Collectors.toSet()))
                .build();
    }
}
