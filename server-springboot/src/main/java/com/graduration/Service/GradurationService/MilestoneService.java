package com.graduration.Service.GradurationService;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Locale;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.graduration.Configuration.PaginationSupport;
import com.graduration.Constain.DefensePeriodConstain;
import com.graduration.Constain.MilesStoneStatusConstain;
import com.graduration.Constain.MilesStoneTypeConstain;
import com.graduration.DTO.Request.CreateMilestoneRequest;
import com.graduration.DTO.Request.UpdateMilestoneRequest;
import com.graduration.DTO.Response.MilestoneResponse;
import com.graduration.DTO.Response.PageResponse;
import com.graduration.Repository.DefensePeriodRepository;
import com.graduration.Repository.MilestoneRepository;
import com.graduration.Repository.TeamRepository;
import com.graduration.entity.DefensePeriodEntity;
import com.graduration.entity.MilesStoneEntity;
import com.graduration.exception.AppException;
import com.graduration.exception.ErrorCode;
import com.graduration.mapper.MilestoneMapper;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MilestoneService {
    MilestoneRepository milestoneRepository;
    DefensePeriodRepository defensePeriodRepository;
    TeamRepository teamRepository;
    MilestoneMapper milestoneMapper;

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY')")
    @Transactional
    // Hàm createMilestone: Nhận dữ liệu đầu vào của createMilestone, kiểm tra các trường bắt buộc và quan hệ liên quan,
    // tạo bản ghi nghiệp vụ rồi lưu repository để trả kết quả cho API.
    public MilestoneResponse createMilestone(Long defensePeriodId, CreateMilestoneRequest request) {
        DefensePeriodEntity period = findActiveDefensePeriod(defensePeriodId);
        validate(request.getMilestoneName(), request.getMilestoneType(), request.getStartAt(), request.getDeadline());
        validateWithinPeriod(period, request.getStartAt(), request.getDeadline());
        String name = request.getMilestoneName().trim();
        if (milestoneRepository.existsNameInDefensePeriod(name, defensePeriodId)) {
            throw new AppException(ErrorCode.MILESTONE_ALREADY_EXISTS);
        }
        MilesStoneEntity milestone = MilesStoneEntity.builder()
                .milesStoneName(name)
                .Description(normalize(request.getDescription()))
                .startAt(request.getStartAt())
                .deadLine(request.getDeadline())
                .milestoneType(request.getMilestoneType())
                .status(MilesStoneStatusConstain.DRAFT)
                .allowLateSubmission(defaultTrue(request.getAllowLateSubmission()))
                .required(defaultTrue(request.getRequired()))
                .maxFileSize(request.getMaxFileSize())
                .allowedFileTypes(normalizeFileTypes(request.getAllowedFileTypes()))
                .defensePeriod(period)
                .build();
        return milestoneMapper.toResponse(milestoneRepository.save(milestone));
    }

    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    // Hàm getMilestone: Nhận mã hoặc điều kiện tìm kiếm của getMilestone, truy vấn bản ghi/quan hệ tương ứng, báo lỗi
    // khi không tồn tại và trả về dữ liệu đã ánh xạ.
    public MilestoneResponse getMilestone(Long milestoneId) {
        MilesStoneEntity milestone = findMilestone(milestoneId);
        requireVisible(milestone);
        return milestoneMapper.toResponse(milestone);
    }

    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    // Hàm getMilestones: Nhận mã hoặc điều kiện tìm kiếm của getMilestones, truy vấn bản ghi/quan hệ tương ứng, báo lỗi
    // khi không tồn tại và trả về dữ liệu đã ánh xạ.
    public PageResponse<MilestoneResponse> getMilestones(
            Long defensePeriodId,
            Integer page,
            Integer size,
            MilesStoneStatusConstain status,
            MilesStoneTypeConstain type,
            String keyword) {
        if (defensePeriodId != null && !defensePeriodRepository.existsById(defensePeriodId)) {
            throw new AppException(ErrorCode.DEFENSE_PERIOD_NOT_FOUND);
        }
        Specification<MilesStoneEntity> specification = Specification.where(null);
        if (defensePeriodId != null) {
            specification = specification.and(
                    (root, query, cb) -> cb.equal(root.get("defensePeriod").get("ID_Defense"), defensePeriodId));
        }
        if (status != null) {
            specification = specification.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        if (type != null) {
            specification = specification.and((root, query, cb) -> cb.equal(root.get("milestoneType"), type));
        }
        if (keyword != null && !keyword.isBlank()) {
            String pattern = "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";
            specification = specification.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("milesStoneName")), pattern),
                    cb.like(cb.lower(root.get("Description")), pattern)));
        }
        if (isStudent()) {
            if (status != null
                    && status != MilesStoneStatusConstain.OPEN
                    && status != MilesStoneStatusConstain.CLOSED) {
                throw new AppException(ErrorCode.ACCESS_DENIED);
            }
            specification = specification.and((root, query, cb) ->
                    root.get("status").in(MilesStoneStatusConstain.OPEN, MilesStoneStatusConstain.CLOSED));
            List<Long> permittedPeriodIds = currentStudentDefensePeriodIds();
            if (defensePeriodId != null && !permittedPeriodIds.contains(defensePeriodId)) {
                throw new AppException(ErrorCode.ACCESS_DENIED);
            }
            specification = specification.and((root, query, cb) -> permittedPeriodIds.isEmpty()
                    ? cb.disjunction()
                    : root.get("defensePeriod").get("ID_Defense").in(permittedPeriodIds));
        }
        return PageResponse.from(
                milestoneRepository.findAll(
                        specification,
                        PaginationSupport.pageRequest(
                                page, size, Sort.by(Sort.Order.asc("startAt"), Sort.Order.asc("deadLine")))),
                milestoneMapper::toResponse);
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY')")
    @Transactional
    // Hàm updateMilestone: Nhận mã bản ghi cùng dữ liệu cập nhật của updateMilestone, tải bản ghi hiện có, kiểm tra
    // trạng thái và ràng buộc rồi ghi các giá trị mới xuống repository.
    public MilestoneResponse updateMilestone(Long milestoneId, UpdateMilestoneRequest request) {
        MilesStoneEntity milestone = findMilestone(milestoneId);
        requireStatus(milestone, MilesStoneStatusConstain.DRAFT);
        DefensePeriodEntity period =
                findActiveDefensePeriod(milestone.getDefensePeriod().getID_Defense());
        validate(request.getMilestoneName(), request.getMilestoneType(), request.getStartAt(), request.getDeadline());
        validateWithinPeriod(period, request.getStartAt(), request.getDeadline());
        String name = request.getMilestoneName().trim();
        if (milestoneRepository.existsDuplicateName(name, period.getID_Defense(), milestoneId)) {
            throw new AppException(ErrorCode.MILESTONE_ALREADY_EXISTS);
        }
        milestone.setMilesStoneName(name);
        milestone.setDescription(normalize(request.getDescription()));
        milestone.setStartAt(request.getStartAt());
        milestone.setDeadLine(request.getDeadline());
        milestone.setMilestoneType(request.getMilestoneType());
        milestone.setAllowLateSubmission(defaultTrue(request.getAllowLateSubmission()));
        milestone.setRequired(defaultTrue(request.getRequired()));
        milestone.setMaxFileSize(request.getMaxFileSize());
        milestone.setAllowedFileTypes(normalizeFileTypes(request.getAllowedFileTypes()));
        return milestoneMapper.toResponse(milestoneRepository.save(milestone));
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY')")
    @Transactional
    // Hàm openMilestone: Nhận mã bản ghi và thông tin thao tác của openMilestone, kiểm tra trạng thái hiện tại cùng
    // quyền thực hiện, cập nhật trạng thái/lý do và lưu thay đổi.
    public MilestoneResponse openMilestone(Long milestoneId) {
        MilesStoneEntity milestone = findMilestone(milestoneId);
        requireStatus(milestone, MilesStoneStatusConstain.DRAFT);
        requireActiveDefensePeriod(milestone.getDefensePeriod());
        validateWithinPeriod(milestone.getDefensePeriod(), milestone.getStartAt(), milestone.getDeadLine());
        milestone.setStatus(MilesStoneStatusConstain.OPEN);
        return milestoneMapper.toResponse(milestoneRepository.save(milestone));
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY')")
    @Transactional
    // Hàm closeMilestone: Nhận mã bản ghi và thông tin thao tác của closeMilestone, kiểm tra trạng thái hiện tại cùng
    // quyền thực hiện, cập nhật trạng thái/lý do và lưu thay đổi.
    public MilestoneResponse closeMilestone(Long milestoneId) {
        MilesStoneEntity milestone = findMilestone(milestoneId);
        requireStatus(milestone, MilesStoneStatusConstain.OPEN);
        milestone.setStatus(MilesStoneStatusConstain.CLOSED);
        return milestoneMapper.toResponse(milestoneRepository.save(milestone));
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY')")
    @Transactional
    // Hàm cancelMilestone: Nhận milestoneId; chỉ cho phép mốc đang DRAFT hoặc OPEN chuyển sang CANCELLED rồi lưu và trả
    // mốc đã cập nhật.
    public MilestoneResponse cancelMilestone(Long milestoneId) {
        MilesStoneEntity milestone = findMilestone(milestoneId);
        if (milestone.getStatus() != MilesStoneStatusConstain.DRAFT
                && milestone.getStatus() != MilesStoneStatusConstain.OPEN) {
            throw new AppException(ErrorCode.MILESTONE_OPERATION_NOT_ALLOWED);
        }
        milestone.setStatus(MilesStoneStatusConstain.CANCELLED);
        return milestoneMapper.toResponse(milestoneRepository.save(milestone));
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY')")
    @Transactional
    // Hàm deleteMilestone: Nhận mã bản ghi của deleteMilestone, kiểm tra quyền và các quan hệ đang sử dụng, sau đó xóa
    // hoặc chuyển bản ghi sang trạng thái tương ứng.
    public void deleteMilestone(Long milestoneId) {
        MilesStoneEntity milestone = findMilestone(milestoneId);
        requireStatus(milestone, MilesStoneStatusConstain.DRAFT);
        if (!milestone.getSubmistion().isEmpty()) {
            throw new AppException(ErrorCode.MILESTONE_IN_USE);
        }
        milestoneRepository.delete(milestone);
    }

    // Hàm findMilestone: Nhận mã hoặc điều kiện tìm kiếm của findMilestone, truy vấn bản ghi/quan hệ tương ứng, báo lỗi
    // khi không tồn tại và trả về dữ liệu đã ánh xạ.
    private MilesStoneEntity findMilestone(Long milestoneId) {
        if (milestoneId == null) {
            throw new AppException(ErrorCode.MILESTONE_NOT_FOUND);
        }
        return milestoneRepository
                .findById(milestoneId)
                .orElseThrow(() -> new AppException(ErrorCode.MILESTONE_NOT_FOUND));
    }

    // Hàm findActiveDefensePeriod: Nhận mã hoặc điều kiện tìm kiếm của findActiveDefensePeriod, truy vấn bản ghi/quan
    // hệ tương ứng, báo lỗi khi không tồn tại và trả về dữ liệu đã ánh xạ.
    private DefensePeriodEntity findActiveDefensePeriod(Long defensePeriodId) {
        if (defensePeriodId == null) {
            throw new AppException(ErrorCode.DEFENSE_PERIOD_NOT_FOUND);
        }
        DefensePeriodEntity period = defensePeriodRepository
                .findById(defensePeriodId)
                .orElseThrow(() -> new AppException(ErrorCode.DEFENSE_PERIOD_NOT_FOUND));
        requireActiveDefensePeriod(period);
        return period;
    }

    // Kiểm tra các điều kiện và quy tắc nghiệp vụ trước khi tiếp tục xử lý.
    private void requireActiveDefensePeriod(DefensePeriodEntity period) {
        if (period.getStatus() == DefensePeriodConstain.FINISHED) {
            throw new AppException(ErrorCode.DEFENSE_PERIOD_FINISHED);
        }
    }

    // Kiểm tra các điều kiện và quy tắc nghiệp vụ trước khi tiếp tục xử lý.
    private void validate(String name, MilesStoneTypeConstain type, LocalDateTime startAt, LocalDateTime deadline) {
        if (name == null || name.isBlank()) {
            throw new AppException(ErrorCode.MILESTONE_NAME_NOT_BLANK);
        }
        if (type == null) {
            throw new AppException(ErrorCode.MILESTONE_TYPE_NOT_BLANK);
        }
        if (startAt == null) {
            throw new AppException(ErrorCode.MILESTONE_START_AT_NOT_BLANK);
        }
        if (deadline == null) {
            throw new AppException(ErrorCode.MILESTONE_DEADLINE_NOT_BLANK);
        }
        if (!startAt.isBefore(deadline)) {
            throw new AppException(ErrorCode.MILESTONE_INVALID_DATE);
        }
    }

    // Kiểm tra các điều kiện và quy tắc nghiệp vụ trước khi tiếp tục xử lý.
    private void validateWithinPeriod(DefensePeriodEntity period, LocalDateTime startAt, LocalDateTime deadline) {
        validate("milestone", MilesStoneTypeConstain.OTHER, startAt, deadline);
        LocalDateTime periodStart = period.getStartDate().atStartOfDay();
        LocalDateTime periodEnd = period.getEndDate().atTime(LocalTime.MAX);
        if (startAt.isBefore(periodStart) || deadline.isAfter(periodEnd)) {
            throw new AppException(ErrorCode.MILESTONE_OUTSIDE_DEFENSE_PERIOD);
        }
    }

    // Kiểm tra các điều kiện và quy tắc nghiệp vụ trước khi tiếp tục xử lý.
    private void requireStatus(MilesStoneEntity milestone, MilesStoneStatusConstain expected) {
        if (milestone.getStatus() != expected) {
            throw new AppException(ErrorCode.MILESTONE_OPERATION_NOT_ALLOWED);
        }
    }

    // Kiểm tra các điều kiện và quy tắc nghiệp vụ trước khi tiếp tục xử lý.
    private void requireVisible(MilesStoneEntity milestone) {
        if (isStudent()
                && milestone.getStatus() != MilesStoneStatusConstain.OPEN
                && milestone.getStatus() != MilesStoneStatusConstain.CLOSED) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }
        if (isStudent()
                && !currentStudentDefensePeriodIds()
                        .contains(milestone.getDefensePeriod().getID_Defense())) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }
    }

    private List<Long> currentStudentDefensePeriodIds() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
        return teamRepository
                .findAllByStudentEntities_UserEntity_UserIdOrderByDefensePeriod_EndDateDesc(authentication.getName())
                .stream()
                .map(team -> team.getDefensePeriod() == null
                        ? null
                        : team.getDefensePeriod().getID_Defense())
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
    }

    // Hàm isStudent: Kiểm tra danh sách quyền của tài khoản hiện tại có ROLE_STUDENT để giới hạn thao tác dành riêng
    // cho sinh viên.
    private boolean isStudent() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        boolean manager = authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN")
                        || authority.getAuthority().equals("ROLE_FACULTY"));
        return !manager
                && authentication.getAuthorities().stream()
                        .anyMatch(authority -> authority.getAuthority().equals("ROLE_STUDENT"));
    }

    // Hàm defaultTrue: Nhận giá trị Boolean từ request mốc tiến độ; dùng true khi client không truyền giá trị để các cờ
    // bắt buộc/muộn có mặc định an toàn.
    private Boolean defaultTrue(Boolean value) {
        return value == null || value;
    }

    // Hàm normalize: Nhận mô tả mốc tiến độ từ request; chuyển null hoặc chuỗi trắng thành null và trim phần còn lại
    // trước khi lưu MilesStoneEntity.
    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    // Hàm normalizeFileTypes: Nhận danh sách phần mở rộng tệp phân cách bằng dấu phẩy; trim từng phần tử, bỏ dấu chấm
    // đầu, chuyển về chữ thường, loại giá trị rỗng/trùng và ghép lại thành chuỗi chuẩn để kiểm tra tệp nộp.
    private String normalizeFileTypes(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return String.join(
                ",",
                java.util.Arrays.stream(value.split(","))
                        .map(String::trim)
                        .map(item -> item.startsWith(".") ? item.substring(1) : item)
                        .map(item -> item.toLowerCase(Locale.ROOT))
                        .filter(item -> !item.isBlank())
                        .distinct()
                        .toList());
    }
}
