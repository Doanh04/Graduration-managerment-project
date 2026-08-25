package com.graduration.Configuration;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.graduration.Constain.PermissionConstain;
import com.graduration.Constain.RoleConstain;
import com.graduration.Constain.RoleNameConstain;
import com.graduration.Constain.StatusConstain;
import com.graduration.Repository.PermissionRepository;
import com.graduration.Repository.RoleRepository;
import com.graduration.Repository.UserRepository;
import com.graduration.entity.PermissionEntity;
import com.graduration.entity.Roles;
import com.graduration.entity.UserEntity;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class InitialDataInitializer implements ApplicationRunner {
    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_INITIAL_PASSWORD = "123456";

    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Map<PermissionConstain, PermissionEntity> permissions = initializePermissions();
        Map<RoleConstain, Roles> roles = initializeRoles(permissions);
        initializeAdmin(roles.get(RoleConstain.ADMIN));
    }

    private Map<PermissionConstain, PermissionEntity> initializePermissions() {
        Map<PermissionConstain, PermissionEntity> result = new EnumMap<>(PermissionConstain.class);
        for (PermissionConstain permissionId : PermissionConstain.values()) {
            PermissionEntity permission = permissionRepository
                    .findById(permissionId)
                    .orElseGet(() -> permissionRepository.save(PermissionEntity.builder()
                            .permissionId(permissionId)
                            .permissionName(permissionName(permissionId))
                            .description(permissionDescription(permissionId))
                            .build()));
            result.put(permissionId, permission);
        }
        return result;
    }

    private Map<RoleConstain, Roles> initializeRoles(Map<PermissionConstain, PermissionEntity> permissions) {
        Map<RoleConstain, Roles> result = new EnumMap<>(RoleConstain.class);
        for (RoleConstain roleId : RoleConstain.values()) {
            Roles role = roleRepository.findById(roleId).orElse(null);
            if (role == null) {
                role = Roles.builder()
                        .role(roleId)
                        .roleName(roleName(roleId))
                        .description(roleDescription(roleId))
                        .permission(resolvePermissions(roleId, permissions))
                        .build();
                role = roleRepository.save(role);
            } else if (role.getPermission() == null || role.getPermission().isEmpty()) {
                role.setPermission(resolvePermissions(roleId, permissions));
                role = roleRepository.save(role);
            }
            result.put(roleId, role);
        }
        return result;
    }

    private void initializeAdmin(Roles adminRole) {
        if (userRepository.existsByUserName(ADMIN_USERNAME)) return;

        userRepository.save(UserEntity.builder()
                .userName(ADMIN_USERNAME)
                .password(passwordEncoder.encode(ADMIN_INITIAL_PASSWORD))
                .status(StatusConstain.ACTIVE)
                .createAt(LocalDateTime.now())
                .roles(new HashSet<>(Set.of(adminRole)))
                .build());
        log.warn(
                "Đã tạo tài khoản quản trị mặc định '{}'; cần đổi mật khẩu ngay sau lần đăng nhập đầu tiên",
                ADMIN_USERNAME);
    }

    private Set<PermissionEntity> resolvePermissions(
            RoleConstain role, Map<PermissionConstain, PermissionEntity> permissions) {
        EnumSet<PermissionConstain> ids =
                switch (role) {
                    case ADMIN -> EnumSet.allOf(PermissionConstain.class);
                    case FACULTY -> EnumSet.of(
                            PermissionConstain.user_read,
                            PermissionConstain.user_create,
                            PermissionConstain.user_update,
                            PermissionConstain.user_assign_role,
                            PermissionConstain.semester_read,
                            PermissionConstain.semester_create,
                            PermissionConstain.semeste_update,
                            PermissionConstain.semester_delete,
                            PermissionConstain.topic_read,
                            PermissionConstain.topi_create,
                            PermissionConstain.topic_update,
                            PermissionConstain.topic_delete,
                            PermissionConstain.topic_approve,
                            PermissionConstain.assignment_read,
                            PermissionConstain.assignment_create,
                            PermissionConstain.assignment_update,
                            PermissionConstain.assignment_delete,
                            PermissionConstain.progress_read,
                            PermissionConstain.progress_comment,
                            PermissionConstain.council_read,
                            PermissionConstain.council_create,
                            PermissionConstain.council_update,
                            PermissionConstain.council_delete,
                            PermissionConstain.score_read,
                            PermissionConstain.score_evaluate_council,
                            PermissionConstain.score_publish);
                    case SUPERVISOR -> EnumSet.of(
                            PermissionConstain.semester_read,
                            PermissionConstain.topic_read,
                            PermissionConstain.topi_create,
                            PermissionConstain.topic_update,
                            PermissionConstain.assignment_read,
                            PermissionConstain.progress_read,
                            PermissionConstain.progress_comment,
                            PermissionConstain.council_read,
                            PermissionConstain.score_read,
                            PermissionConstain.score_evaluate_advisor);
                    case REVIEWER -> EnumSet.of(
                            PermissionConstain.semester_read,
                            PermissionConstain.topic_read,
                            PermissionConstain.assignment_read,
                            PermissionConstain.progress_read,
                            PermissionConstain.council_read,
                            PermissionConstain.score_read,
                            PermissionConstain.score_evaluate_reviewer,
                            PermissionConstain.score_evaluate_council);
                    case STUDENT -> EnumSet.of(
                            PermissionConstain.semester_read,
                            PermissionConstain.topic_read,
                            PermissionConstain.topi_create,
                            PermissionConstain.topic_update,
                            PermissionConstain.topic_register,
                            PermissionConstain.assignment_read,
                            PermissionConstain.progress_read,
                            PermissionConstain.progress_create,
                            PermissionConstain.council_read,
                            PermissionConstain.score_read);
                };
        Set<PermissionEntity> result = new HashSet<>();
        ids.forEach(id -> result.add(permissions.get(id)));
        return result;
    }

    private RoleNameConstain roleName(RoleConstain role) {
        return RoleNameConstain.valueOf("NAME_" + role.name());
    }

    private String roleDescription(RoleConstain role) {
        return switch (role) {
            case ADMIN -> "Quản trị toàn bộ hệ thống, tài khoản, phân quyền và dữ liệu nghiệp vụ.";
            case FACULTY -> "Ban quản lý khoa, quản lý các đợt đồ án, đề tài, phân công, hội đồng và công bố điểm.";
            case SUPERVISOR -> "Giảng viên hướng dẫn, theo dõi tiến độ, nhận xét và chấm điểm sinh viên hướng dẫn.";
            case REVIEWER -> "Giảng viên phản biện, đánh giá đề tài và tham gia chấm điểm bảo vệ.";
            case STUDENT -> "Sinh viên tham gia đồ án, đăng ký đề tài, nộp tiến độ và xem kết quả.";
        };
    }

    private String permissionName(PermissionConstain permission) {
        return switch (permission) {
            case user_read -> "Xem người dùng";
            case user_create -> "Tạo người dùng";
            case user_update -> "Cập nhật người dùng";
            case user_delete -> "Xóa người dùng";
            case user_assign_role -> "Phân quyền người dùng";
            case semester_read -> "Xem năm học và đợt đồ án";
            case semester_create -> "Tạo năm học và đợt đồ án";
            case semeste_update -> "Cập nhật năm học và đợt đồ án";
            case semester_delete -> "Xóa năm học và đợt đồ án";
            case topic_read -> "Xem đề tài";
            case topi_create -> "Tạo đề tài";
            case topic_update -> "Cập nhật đề tài";
            case topic_delete -> "Xóa đề tài";
            case topic_approve -> "Phê duyệt đề tài";
            case topic_register -> "Đăng ký đề tài";
            case assignment_read -> "Xem phân công hướng dẫn";
            case assignment_create -> "Tạo phân công hướng dẫn";
            case assignment_update -> "Cập nhật phân công hướng dẫn";
            case assignment_delete -> "Hủy phân công hướng dẫn";
            case progress_read -> "Xem tiến độ đồ án";
            case progress_create -> "Tạo báo cáo tiến độ";
            case progress_comment -> "Nhận xét tiến độ";
            case council_read -> "Xem hội đồng và lịch bảo vệ";
            case council_create -> "Tạo hội đồng và lịch bảo vệ";
            case council_update -> "Cập nhật hội đồng và lịch bảo vệ";
            case council_delete -> "Xóa hội đồng";
            case score_read -> "Xem điểm";
            case score_evaluate_advisor -> "Chấm điểm hướng dẫn";
            case score_evaluate_reviewer -> "Chấm điểm phản biện";
            case score_evaluate_council -> "Chấm điểm hội đồng";
            case score_publish -> "Công bố điểm";
        };
    }

    private String permissionDescription(PermissionConstain permission) {
        return switch (permission) {
            case user_read -> "Xem danh sách và thông tin chi tiết của giảng viên, sinh viên.";
            case user_create -> "Thêm tài khoản mới bằng biểu mẫu hoặc nhập dữ liệu Excel.";
            case user_update -> "Chỉnh sửa thông tin và trạng thái hoạt động của tài khoản.";
            case user_delete -> "Xóa tài khoản người dùng khỏi hệ thống.";
            case user_assign_role -> "Gán vai trò và quyền truy cập cho người dùng.";
            case semester_read -> "Xem danh sách năm học và các đợt thực hiện đồ án.";
            case semester_create -> "Tạo năm học hoặc đợt thực hiện đồ án mới.";
            case semeste_update -> "Cập nhật thông tin và thời gian của đợt đồ án.";
            case semester_delete -> "Xóa năm học hoặc đợt đồ án khi đủ điều kiện.";
            case topic_read -> "Xem danh sách và nội dung chi tiết đề tài.";
            case topi_create -> "Tạo hoặc đề xuất một đề tài đồ án mới.";
            case topic_update -> "Chỉnh sửa thông tin đề tài khi trạng thái cho phép.";
            case topic_delete -> "Xóa đề tài khi chưa phát sinh dữ liệu liên quan.";
            case topic_approve -> "Phê duyệt hoặc từ chối đề tài được đề xuất.";
            case topic_register -> "Đăng ký nguyện vọng lựa chọn đề tài.";
            case assignment_read -> "Xem danh sách giảng viên được phân công hướng dẫn.";
            case assignment_create -> "Phân công giảng viên hướng dẫn cho đề tài.";
            case assignment_update -> "Điều chỉnh vai trò hoặc nội dung phân công hướng dẫn.";
            case assignment_delete -> "Hủy một phân công hướng dẫn đang có.";
            case progress_read -> "Xem nhật ký, bài nộp và tình trạng tiến độ đồ án.";
            case progress_create -> "Tạo nhật ký hoặc nộp báo cáo tiến độ đồ án.";
            case progress_comment -> "Viết nhận xét và phản hồi đối với tiến độ sinh viên.";
            case council_read -> "Xem hội đồng, thành viên và lịch bảo vệ.";
            case council_create -> "Thành lập hội đồng và xây dựng lịch bảo vệ.";
            case council_update -> "Thay đổi thành viên hội đồng hoặc thông tin lịch bảo vệ.";
            case council_delete -> "Xóa hội đồng khi chưa có dữ liệu ràng buộc.";
            case score_read -> "Xem điểm đã được phép công bố hoặc thuộc phạm vi quản lý.";
            case score_evaluate_advisor -> "Giảng viên hướng dẫn nhập điểm cho từng sinh viên.";
            case score_evaluate_reviewer -> "Giảng viên phản biện nhập điểm và nhận xét.";
            case score_evaluate_council -> "Thành viên hội đồng nhập điểm bảo vệ.";
            case score_publish -> "Khóa và công bố kết quả điểm chính thức.";
        };
    }
}
