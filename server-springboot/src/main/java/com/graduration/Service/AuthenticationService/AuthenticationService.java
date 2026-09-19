package com.graduration.Service.AuthenticationService;

import java.text.ParseException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.graduration.Constain.RoleConstain;
import com.graduration.Constain.StatusConstain;
import com.graduration.DTO.Request.AuthenticationRequest;
import com.graduration.DTO.Request.IntrospectRequest;
import com.graduration.DTO.Request.LogoutRequest;
import com.graduration.DTO.Request.RefreshRequest;
import com.graduration.DTO.Response.AuthenticationResponse;
import com.graduration.DTO.Response.IntrospectResponse;
import com.graduration.Repository.InvalidatedRepository;
import com.graduration.Repository.UserRepository;
import com.graduration.entity.InvalidatedToken;
import com.graduration.entity.UserEntity;
import com.graduration.exception.AppException;
import com.graduration.exception.ErrorCode;
import com.graduration.mapper.AuthenticationMapper;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import lombok.RequiredArgsConstructor;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationService {
    private static final String TOKEN_TYPE_CLAIM = "tokenType";
    private static final String ACCESS_TOKEN_TYPE = "ACCESS";
    private static final String REFRESH_TOKEN_TYPE = "REFRESH";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final InvalidatedRepository invalidatedRepository;
    private final AuthenticationMapper authenticationMapper;

    @NonFinal
    @Value("${jwt.valid-duration}")
    protected long VALID_DURATION;

    @NonFinal
    @Value("${jwt.signerKey}")
    protected String SIGNER_KEY;

    @NonFinal
    @Value("${jwt.refreshable-duration}")
    protected long RERESHABLE_DURATION;

    // Hàm authenticate: Nhận username và password từ yêu cầu đăng nhập; tìm tài khoản, đối chiếu mật khẩu đã mã hóa,
    // kiểm tra trạng thái rồi tạo token và thông tin người dùng.
    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        String identifier = authenticationMapper.toLoginIdentifier(request);
        if (identifier == null || identifier.isBlank()) {
            throw new AppException(ErrorCode.INVALID_USERNAME);
        }
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new AppException(ErrorCode.PASSWORD_NOT_BLANK);
        }

        List<UserEntity> candidates =
                userRepository
                        .findByUserNameOrLecture_LectureCodeOrStudent_StudentCode(identifier, identifier, identifier)
                        .stream()
                        .collect(Collectors.collectingAndThen(
                                Collectors.toMap(
                                        UserEntity::getUserId,
                                        user -> user,
                                        (first, duplicate) -> first,
                                        LinkedHashMap::new),
                                usersById -> List.copyOf(usersById.values())));

        if (candidates.isEmpty()) {
            throw new AppException(ErrorCode.USER_NOT_FOUND);
        }

        List<UserEntity> authenticatedCandidates = candidates.stream()
                .filter(candidate -> passwordEncoder.matches(request.getPassword(), candidate.getPassword()))
                .toList();

        if (authenticatedCandidates.isEmpty()) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        if (authenticatedCandidates.size() > 1) {
            throw new AppException(ErrorCode.AMBIGUOUS_LOGIN_IDENTIFIER);
        }

        UserEntity user = authenticatedCandidates.get(0);
        validateActiveAccount(user);

        return AuthenticationResponse.builder()
                .token(generateToken(user))
                .refreshToken(generateRefreshToken(user))
                .authenticated(true)
                .userName(user.getUserName())
                .fullName(resolveFullName(user))
                .accountType(resolveAccountType(user))
                .roles(resolveRoles(user))
                .build();
    }

    // Hàm generateToken: Nhận thông tin định danh và quyền của người dùng; tạo JWT access token có thời hạn, subject và
    // claims phục vụ xác thực các API.
    public String generateToken(UserEntity user) {
        return generateToken(user, ACCESS_TOKEN_TYPE, VALID_DURATION, true);
    }

    // Hàm generateRefreshToken: Nhận thông tin người dùng; tạo refresh token riêng với thời hạn dài hơn để cấp lại
    // access token khi phiên truy cập hết hạn.
    public String generateRefreshToken(UserEntity user) {
        return generateToken(user, REFRESH_TOKEN_TYPE, RERESHABLE_DURATION, false);
    }

    // Hàm generateToken: Nhận thông tin định danh và quyền của người dùng; tạo JWT access token có thời hạn, subject và
    // claims phục vụ xác thực các API.
    private String generateToken(UserEntity user, String tokenType, long duration, boolean includeUserClaims) {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(user.getUserId())
                .issuer("GradurationManagement")
                .issueTime(new Date())
                .expirationTime(new Date(
                        Instant.now().plus(duration, ChronoUnit.SECONDS).toEpochMilli()))
                .jwtID(UUID.randomUUID().toString())
                .claim(TOKEN_TYPE_CLAIM, tokenType)
                .build();

        if (includeUserClaims) {
            claims = new JWTClaimsSet.Builder(claims)
                    .claim("scope", buildScope(user))
                    .claim("userName", user.getUserName())
                    .claim("accountType", resolveAccountType(user))
                    .claim("roles", resolveRoles(user).stream().map(Enum::name).toList())
                    .claim("email", resolveEmail(user))
                    .claim("phone", resolvePhone(user))
                    .claim(
                            "status",
                            user.getStatus() == null ? null : user.getStatus().name())
                    .build();
        }

        JWSObject jwsObject = new JWSObject(new JWSHeader(JWSAlgorithm.HS256), new Payload(claims.toJSONObject()));
        try {
            jwsObject.sign(new MACSigner(SIGNER_KEY.getBytes()));
            return jwsObject.serialize();
        } catch (JOSEException exception) {
            log.error("Cannot create token", exception);
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
    }

    // Hàm introspect: Nhận token từ request; xác minh token và trả về trạng thái active cùng các thông tin subject,
    // scope cho phía gọi kiểm tra phiên.
    public IntrospectResponse introspect(IntrospectRequest request) {
        boolean valid = true;
        try {
            verifyToken(request.getToken(), false);
        } catch (AppException | JOSEException | ParseException | NullPointerException exception) {
            valid = false;
        }

        return IntrospectResponse.builder().valid(valid).build();
    }

    // Hàm buildScope: Nhận UserEntity và các role; tổng hợp username, email, phone, loại tài khoản và quyền thành chuỗi
    // scope đưa vào access token.
    public String buildScope(UserEntity user) {
        StringJoiner scope = new StringJoiner(" ");

        if (!CollectionUtils.isEmpty(user.getRoles())) {
            user.getRoles().forEach(role -> {
                scope.add("ROLE_" + role.getRole().name());
                if (!CollectionUtils.isEmpty(role.getPermission())) {
                    role.getPermission()
                            .forEach(permission -> scope.add(
                                    "PERMISSION_" + permission.getPermissionId().name()));
                }
            });
        }

        return scope.toString();
    }

    // Hàm refresh: Nhận refresh token; xác minh token còn hạn, lấy lại thông tin người dùng và phát hành cặp
    // access/refresh token mới.
    public AuthenticationResponse refresh(RefreshRequest request) throws ParseException, JOSEException {
        SignedJWT signedJWT = verifyToken(request.getToken(), true);
        invalidateToken(signedJWT);

        UserEntity user = userRepository
                .findById(signedJWT.getJWTClaimsSet().getSubject())
                .orElseThrow(() -> new AppException(ErrorCode.UNAUTHENTICATED));
        validateActiveAccount(user);

        return AuthenticationResponse.builder()
                .token(generateToken(user))
                .refreshToken(generateRefreshToken(user))
                .authenticated(true)
                .userName(user.getUserName())
                .fullName(resolveFullName(user))
                .accountType(resolveAccountType(user))
                .roles(resolveRoles(user))
                .build();
    }

    // Hàm resolveFullName: Nhận UserEntity; lấy họ tên từ hồ sơ sinh viên/giảng viên hoặc username làm giá trị dự phòng
    // để ghi vào claim và phản hồi đăng nhập.
    private String resolveFullName(UserEntity user) {
        if (user.getStudent() != null) return user.getStudent().getFullNameStudent();
        if (user.getLecture() != null) return user.getLecture().getFullNameLecture();
        return user.getUserName();
    }

    // Hàm getCurrentUser: Nhận mã hoặc điều kiện tìm kiếm của getCurrentUser, truy vấn bản ghi/quan hệ tương ứng, báo
    // lỗi khi không tồn tại và trả về dữ liệu đã ánh xạ.
    public AuthenticationResponse getCurrentUser(String userId) {
        UserEntity user = userRepository.findById(userId).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        validateActiveAccount(user);
        return AuthenticationResponse.builder()
                .authenticated(true)
                .userName(user.getUserName())
                .fullName(resolveFullName(user))
                .accountType(resolveAccountType(user))
                .roles(resolveRoles(user))
                .build();
    }

    // Hàm logout: Nhận access token hiện tại; thu hồi token còn hiệu lực và xóa/đóng cookie phiên để người dùng không
    // thể tiếp tục gọi API.
    public void logout(LogoutRequest request) throws ParseException, JOSEException {
        invalidateIfValid(request.getToken(), ACCESS_TOKEN_TYPE);
        invalidateIfValid(request.getRefreshToken(), REFRESH_TOKEN_TYPE);
    }

    // Hàm invalidateIfValid: Nhận token cần thu hồi; chỉ ghi vào danh sách invalidated khi token còn hợp lệ nhằm tránh
    // tạo bản ghi thu hồi không cần thiết.
    private void invalidateIfValid(String token, String tokenType) throws ParseException, JOSEException {
        if (token == null || token.isBlank()) {
            return;
        }
        try {
            invalidateToken(verifyToken(token, tokenType));
        } catch (AppException exception) {
            log.info("{} token already expired, invalidated or has the wrong type", tokenType);
        }
    }

    // Hàm invalidateToken: Nhận token và thời điểm hết hạn; ghi token vào danh sách invalidated để ngăn sử dụng lại sau
    // khi đăng xuất hoặc thu hồi.
    private void invalidateToken(SignedJWT signedJWT) throws ParseException {
        InvalidatedToken invalidatedToken = InvalidatedToken.builder()
                .iD(signedJWT.getJWTClaimsSet().getJWTID())
                .expiryTime(signedJWT.getJWTClaimsSet().getExpirationTime())
                .build();
        invalidatedRepository.save(invalidatedToken);
    }

    // Hàm resolveAccountType: Nhận UserEntity; xác định loại tài khoản dựa trên hồ sơ sinh viên, giảng viên hoặc quyền
    // hệ thống để gắn vào thông tin xác thực.
    private String resolveAccountType(UserEntity user) {
        if (user.getStudent() != null) {
            return "STUDENT";
        }
        if (user.getLecture() != null) {
            return "LECTURER";
        }
        return "USER";
    }

    // Kiểm tra các điều kiện và quy tắc nghiệp vụ trước khi tiếp tục xử lý.
    private void validateActiveAccount(UserEntity user) {
        if (user.getStatus() != StatusConstain.ACTIVE) {
            throw new AppException(ErrorCode.ACCOUNT_INACTIVE);
        }
    }

    // Hàm resolveRoles: Nhận UserEntity; đọc các role được gán, chuẩn hóa tên role và trả về danh sách quyền dùng cho
    // Spring Security.
    private Set<RoleConstain> resolveRoles(UserEntity user) {
        if (CollectionUtils.isEmpty(user.getRoles())) {
            return Collections.emptySet();
        }

        return user.getRoles().stream().map(role -> role.getRole()).collect(Collectors.toSet());
    }

    // Hàm resolveEmail: Nhận UserEntity; lấy email từ hồ sơ liên quan, chuẩn hóa giá trị rỗng thành null để đưa vào
    // claim người dùng.
    private String resolveEmail(UserEntity user) {
        if (user.getStudent() != null) {
            return user.getStudent().getEmail();
        }
        return user.getLecture() == null ? null : user.getLecture().getEmaillecture();
    }

    // Hàm resolvePhone: Nhận UserEntity; lấy số điện thoại từ hồ sơ sinh viên/giảng viên và trả về null nếu tài khoản
    // chưa khai báo.
    private String resolvePhone(UserEntity user) {
        if (user.getStudent() != null) {
            return user.getStudent().getPhoneStudent();
        }
        return user.getLecture() == null ? null : user.getLecture().getPhoneLecture();
    }

    // Hàm verifyToken: Nhận JWT từ request; giải mã, kiểm tra chữ ký, thời hạn và trạng thái token bị vô hiệu hóa trước
    // khi trả về thông tin claims hợp lệ.
    public SignedJWT verifyToken(String token, boolean isRefresh) throws JOSEException, ParseException {
        return verifyToken(token, isRefresh ? REFRESH_TOKEN_TYPE : ACCESS_TOKEN_TYPE);
    }

    // Hàm verifyToken: Nhận JWT từ request; giải mã, kiểm tra chữ ký, thời hạn và trạng thái token bị vô hiệu hóa trước
    // khi trả về thông tin claims hợp lệ.
    private SignedJWT verifyToken(String token, String expectedTokenType) throws JOSEException, ParseException {
        JWSVerifier verifier = new MACVerifier(SIGNER_KEY.getBytes());

        SignedJWT signedJWT = SignedJWT.parse(token);
        var verified = signedJWT.verify(verifier);
        Date expiryTime = signedJWT.getJWTClaimsSet().getExpirationTime();
        String tokenType = signedJWT.getJWTClaimsSet().getStringClaim(TOKEN_TYPE_CLAIM);

        if (!(verified && expiryTime.after(new Date()) && expectedTokenType.equals(tokenType)))
            throw new AppException(ErrorCode.UNAUTHENTICATED);

        if (invalidatedRepository.existsById(signedJWT.getJWTClaimsSet().getJWTID()))
            throw new AppException(ErrorCode.UNAUTHENTICATED);

        return signedJWT;
    }
}
