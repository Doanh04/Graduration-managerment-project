package com.graduration.Service.ManagerService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.graduration.Configuration.PaginationSupport;
import com.graduration.Constain.CommitteeMemberRoleConstain;
import com.graduration.Constain.CommitteeMemberStatusConstain;
import com.graduration.Constain.DefenseCommitteeStatusConstain;
import com.graduration.Constain.DefensePeriodConstain;
import com.graduration.Constain.DefenseScheduleStatusConstain;
import com.graduration.Constain.StatusConstain;
import com.graduration.DTO.Request.DeactivateDefenseCommitteeRequest;
import com.graduration.DTO.Request.DefenseCommitteeRequest;
import com.graduration.DTO.Response.DefenseCommitteeResponse;
import com.graduration.DTO.Response.DefenseCommitteeValidationResponse;
import com.graduration.DTO.Response.PageResponse;
import com.graduration.Repository.DefenseCommitteeRepository;
import com.graduration.Repository.DefensePeriodRepository;
import com.graduration.Repository.UserRepository;
import com.graduration.entity.ComitteesMemberEntity;
import com.graduration.entity.DefenseCommitteesEntity;
import com.graduration.entity.DefensePeriodEntity;
import com.graduration.entity.UserEntity;
import com.graduration.exception.AppException;
import com.graduration.exception.ErrorCode;
import com.graduration.mapper.DefenseCommitteeMapper;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DefenseCommitteeService {
    private static final int MINIMUM_ACTIVE_MEMBERS = 3;

    DefenseCommitteeRepository committeeRepository;
    DefensePeriodRepository defensePeriodRepository;
    UserRepository userRepository;
    DefenseCommitteeMapper committeeMapper;

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY')")
    @Transactional
    // Hàm create: Nhận dữ liệu đầu vào của create, kiểm tra các trường bắt buộc và quan hệ liên quan, tạo bản ghi
    // nghiệp vụ rồi lưu repository để trả kết quả cho API.
    public DefenseCommitteeResponse create(Long defensePeriodId, DefenseCommitteeRequest request) {
        DefensePeriodEntity period = findOpenPeriod(defensePeriodId);
        validateRequest(request);
        String name = request.getCommitteeName().trim();
        if (committeeRepository.existsNameInDefensePeriod(name, defensePeriodId)) {
            throw new AppException(ErrorCode.DEFENSE_COMMITTEE_ALREADY_EXISTS);
        }
        DefenseCommitteesEntity committee = DefenseCommitteesEntity.builder()
                .comitteesName(name)
                .description(normalize(request.getDescription()))
                .status(DefenseCommitteeStatusConstain.DRAFT)
                .defensePeriod(period)
                .createdBy(currentUser())
                .build();
        return committeeMapper.toResponse(committeeRepository.save(committee));
    }

    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    // Hàm getByDefensePeriod: Nhận các tham số lọc/phân trang của getByDefensePeriod, truy vấn dữ liệu phù hợp từ
    // repository, ánh xạ từng entity sang DTO và trả về cho giao diện.
    public PageResponse<DefenseCommitteeResponse> getByDefensePeriod(
            Long defensePeriodId, DefenseCommitteeStatusConstain status, String keyword, Integer page, Integer size) {
        if (!defensePeriodRepository.existsById(defensePeriodId)) {
            throw new AppException(ErrorCode.DEFENSE_PERIOD_NOT_FOUND);
        }
        return PageResponse.from(
                committeeRepository.findByDefensePeriod(
                        defensePeriodId, status, normalize(keyword), PaginationSupport.pageRequest(page, size)),
                committeeMapper::toResponse);
    }

    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    // Hàm getById: Nhận các tham số lọc/phân trang của getById, truy vấn dữ liệu phù hợp từ repository, ánh xạ từng
    // entity sang DTO và trả về cho giao diện.
    public DefenseCommitteeResponse getById(Long committeeId) {
        return committeeMapper.toResponse(findCommittee(committeeId));
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY')")
    @Transactional
    // Hàm update: Nhận mã bản ghi cùng dữ liệu cập nhật của update, tải bản ghi hiện có, kiểm tra trạng thái và ràng
    // buộc rồi ghi các giá trị mới xuống repository.
    public DefenseCommitteeResponse update(Long committeeId, DefenseCommitteeRequest request) {
        DefenseCommitteesEntity committee = findCommittee(committeeId);
        requireStatus(committee, DefenseCommitteeStatusConstain.DRAFT);
        findOpenPeriod(committee.getDefensePeriod().getID_Defense());
        validateRequest(request);
        String name = request.getCommitteeName().trim();
        if (committeeRepository.existsDuplicateName(
                name, committee.getDefensePeriod().getID_Defense(), committeeId)) {
            throw new AppException(ErrorCode.DEFENSE_COMMITTEE_ALREADY_EXISTS);
        }
        committee.setComitteesName(name);
        committee.setDescription(normalize(request.getDescription()));
        return committeeMapper.toResponse(committeeRepository.save(committee));
    }

    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    // Kiểm tra các điều kiện và quy tắc nghiệp vụ trước khi tiếp tục xử lý.
    public DefenseCommitteeValidationResponse validate(Long committeeId) {
        return buildValidation(findCommittee(committeeId));
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY')")
    @Transactional
    // Hàm activate: Nhận mã hội đồng; kiểm tra hội đồng đang ở bản nháp, đợt bảo vệ còn mở và thành viên đạt điều kiện,
    // sau đó chuyển trạng thái sang ACTIVE và lưu thời điểm kích hoạt.
    public DefenseCommitteeResponse activate(Long committeeId) {
        DefenseCommitteesEntity committee = findCommittee(committeeId);
        requireStatus(committee, DefenseCommitteeStatusConstain.DRAFT);
        findOpenPeriod(committee.getDefensePeriod().getID_Defense());
        DefenseCommitteeValidationResponse validation = buildValidation(committee);
        if (!validation.isValid()) {
            throw new AppException(ErrorCode.DEFENSE_COMMITTEE_NOT_READY);
        }
        committee.setStatus(DefenseCommitteeStatusConstain.ACTIVE);
        committee.setActivatedAt(LocalDateTime.now());
        committee.setDeactivationReason(null);
        return committeeMapper.toResponse(committeeRepository.save(committee));
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY')")
    @Transactional
    // Hàm moveToDraft: Nhận mã hội đồng ACTIVE; kiểm tra chưa có lịch sử dụng, sau đó chuyển hội đồng về DRAFT để chỉnh
    // sửa và xóa thời điểm kích hoạt.
    public DefenseCommitteeResponse moveToDraft(Long committeeId) {
        DefenseCommitteesEntity committee = findCommittee(committeeId);
        requireStatus(committee, DefenseCommitteeStatusConstain.ACTIVE);
        requireNoUsableSchedule(committee);
        committee.setStatus(DefenseCommitteeStatusConstain.DRAFT);
        committee.setActivatedAt(null);
        return committeeMapper.toResponse(committeeRepository.save(committee));
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY')")
    @Transactional
    // Hàm deactivate: Nhận mã đối tượng cùng lý do vô hiệu hóa; kiểm tra trạng thái hiện tại và ràng buộc đang sử dụng,
    // ghi lý do, đổi sang INACTIVE rồi lưu.
    public DefenseCommitteeResponse deactivate(Long committeeId, DeactivateDefenseCommitteeRequest request) {
        DefenseCommitteesEntity committee = findCommittee(committeeId);
        if (committee.getStatus() == DefenseCommitteeStatusConstain.INACTIVE) {
            throw new AppException(ErrorCode.DEFENSE_COMMITTEE_OPERATION_NOT_ALLOWED);
        }
        if (request == null
                || request.getReason() == null
                || request.getReason().isBlank()) {
            throw new AppException(ErrorCode.DEFENSE_COMMITTEE_DEACTIVATION_REASON_NOT_BLANK);
        }
        requireNoUsableSchedule(committee);
        committee.setStatus(DefenseCommitteeStatusConstain.INACTIVE);
        committee.setDeactivationReason(request.getReason().trim());
        return committeeMapper.toResponse(committeeRepository.save(committee));
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY')")
    @Transactional
    // Hàm delete: Nhận mã bản ghi của delete, kiểm tra quyền và các quan hệ đang sử dụng, sau đó xóa hoặc chuyển bản
    // ghi sang trạng thái tương ứng.
    public void delete(Long committeeId) {
        DefenseCommitteesEntity committee = findCommittee(committeeId);
        requireStatus(committee, DefenseCommitteeStatusConstain.DRAFT);
        if (!committee.getComitteesMember().isEmpty()
                || !committee.getDefenseSchedules().isEmpty()) {
            throw new AppException(ErrorCode.DEFENSE_COMMITTEE_IN_USE);
        }
        committeeRepository.delete(committee);
    }

    // Hàm buildValidation: Nhận hội đồng và danh sách thành viên; đếm chủ tịch, thư ký, phản biện, kiểm tra số lượng
    // tối thiểu và trạng thái giảng viên để tạo kết quả hợp lệ cùng danh sách lỗi.
    private DefenseCommitteeValidationResponse buildValidation(DefenseCommitteesEntity committee) {
        List<ComitteesMemberEntity> activeMembers = committee.getComitteesMember().stream()
                .filter(member -> member.getStatus() == CommitteeMemberStatusConstain.ACTIVE)
                .toList();
        long chairpersons = countRole(activeMembers, CommitteeMemberRoleConstain.CHAIRPERSON);
        long secretaries = countRole(activeMembers, CommitteeMemberRoleConstain.SECRETARY);
        long reviewers = countRole(activeMembers, CommitteeMemberRoleConstain.REVIEWER);
        List<String> errors = new ArrayList<>();
        if (chairpersons != 1) {
            errors.add("Committee must have exactly one active chairperson");
        }
        if (secretaries != 1) {
            errors.add("Committee must have exactly one active secretary");
        }
        if (reviewers < 1) {
            errors.add("Committee must have at least one active reviewer");
        }
        if (activeMembers.size() < MINIMUM_ACTIVE_MEMBERS) {
            errors.add("Committee must have at least three active members");
        }
        if (activeMembers.stream()
                .anyMatch(member -> member.getLecture() == null
                        || member.getLecture().getUser() == null
                        || member.getLecture().getUser().getStatus() != StatusConstain.ACTIVE)) {
            errors.add("Every active committee member must have an active lecturer account");
        }
        return DefenseCommitteeValidationResponse.builder()
                .valid(errors.isEmpty())
                .activeMemberCount(activeMembers.size())
                .chairpersonCount(chairpersons)
                .secretaryCount(secretaries)
                .reviewerCount(reviewers)
                .errors(errors)
                .build();
    }

    // Hàm countRole: Nhận danh sách thành viên ACTIVE và vai trò cần đếm; lọc các thành viên có đúng vai trò đó và trả
    // về số lượng để kiểm tra cấu hình hội đồng.
    private long countRole(List<ComitteesMemberEntity> members, CommitteeMemberRoleConstain role) {
        return members.stream().filter(member -> member.getRole() == role).count();
    }

    // Kiểm tra các điều kiện và quy tắc nghiệp vụ trước khi tiếp tục xử lý.
    private void requireNoUsableSchedule(DefenseCommitteesEntity committee) {
        boolean inUse = committee.getDefenseSchedules().stream()
                .anyMatch(schedule -> schedule.getStatus() != DefenseScheduleStatusConstain.CANCELLED);
        if (inUse) {
            throw new AppException(ErrorCode.DEFENSE_COMMITTEE_IN_USE);
        }
    }

    // Hàm findOpenPeriod: Nhận mã hoặc điều kiện tìm kiếm của findOpenPeriod, truy vấn bản ghi/quan hệ tương ứng, báo
    // lỗi khi không tồn tại và trả về dữ liệu đã ánh xạ.
    private DefensePeriodEntity findOpenPeriod(Long defensePeriodId) {
        DefensePeriodEntity period = defensePeriodRepository
                .findById(defensePeriodId)
                .orElseThrow(() -> new AppException(ErrorCode.DEFENSE_PERIOD_NOT_FOUND));
        if (period.getStatus() == DefensePeriodConstain.FINISHED) {
            throw new AppException(ErrorCode.DEFENSE_PERIOD_FINISHED);
        }
        return period;
    }

    // Hàm findCommittee: Nhận mã hoặc điều kiện tìm kiếm của findCommittee, truy vấn bản ghi/quan hệ tương ứng, báo lỗi
    // khi không tồn tại và trả về dữ liệu đã ánh xạ.
    private DefenseCommitteesEntity findCommittee(Long committeeId) {
        return committeeRepository
                .findById(committeeId)
                .orElseThrow(() -> new AppException(ErrorCode.DEFENSE_COMMITTEE_NOT_FOUND));
    }

    // Kiểm tra các điều kiện và quy tắc nghiệp vụ trước khi tiếp tục xử lý.
    private void requireStatus(DefenseCommitteesEntity committee, DefenseCommitteeStatusConstain expected) {
        if (committee.getStatus() != expected) {
            throw new AppException(ErrorCode.DEFENSE_COMMITTEE_OPERATION_NOT_ALLOWED);
        }
    }

    // Kiểm tra các điều kiện và quy tắc nghiệp vụ trước khi tiếp tục xử lý.
    private void validateRequest(DefenseCommitteeRequest request) {
        if (request == null
                || request.getCommitteeName() == null
                || request.getCommitteeName().isBlank()) {
            throw new AppException(ErrorCode.DEFENSE_COMMITTEE_NAME_NOT_BLANK);
        }
    }

    // Hàm currentUser: Lấy userId của tài khoản đang đăng nhập từ Authentication, truy vấn UserEntity tương ứng và trả
    // về người thực hiện để gắn vào bản ghi.
    private UserEntity currentUser() {
        return userRepository
                .findById(currentAuthentication().getName())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }

    // Hàm currentAuthentication: Đọc Authentication từ SecurityContext của request hiện tại; từ chối khi chưa đăng nhập
    // và trả về đối tượng xác thực để lấy userId cùng quyền.
    private Authentication currentAuthentication() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
        return authentication;
    }

    // Hàm normalize: Nhận từ khóa tìm kiếm hoặc mô tả hội đồng; chuyển chuỗi trống thành null và trim để truy vấn/lưu
    // DefenseCommitteesEntity.
    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
