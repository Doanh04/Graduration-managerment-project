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
                .accountType(resolveAccountType(user))
                .roles(resolveRoles(user))
                .build();
    }

    public String generateToken(UserEntity user) {
        return generateToken(user, ACCESS_TOKEN_TYPE, VALID_DURATION, true);
    }

    public String generateRefreshToken(UserEntity user) {
        return generateToken(user, REFRESH_TOKEN_TYPE, RERESHABLE_DURATION, false);
    }

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

    public IntrospectResponse introspect(IntrospectRequest request) {
        boolean valid = true;
        try {
            verifyToken(request.getToken(), false);
        } catch (AppException | JOSEException | ParseException | NullPointerException exception) {
            valid = false;
        }

        return IntrospectResponse.builder().valid(valid).build();
    }

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
                .accountType(resolveAccountType(user))
                .roles(resolveRoles(user))
                .build();
    }

    public void logout(LogoutRequest request) throws ParseException, JOSEException {
        invalidateIfValid(request.getToken(), ACCESS_TOKEN_TYPE);
        invalidateIfValid(request.getRefreshToken(), REFRESH_TOKEN_TYPE);
    }

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

    private void invalidateToken(SignedJWT signedJWT) throws ParseException {
        InvalidatedToken invalidatedToken = InvalidatedToken.builder()
                .iD(signedJWT.getJWTClaimsSet().getJWTID())
                .expiryTime(signedJWT.getJWTClaimsSet().getExpirationTime())
                .build();
        invalidatedRepository.save(invalidatedToken);
    }

    private String resolveAccountType(UserEntity user) {
        if (user.getStudent() != null) {
            return "STUDENT";
        }
        if (user.getLecture() != null) {
            return "LECTURER";
        }
        return "USER";
    }

    private void validateActiveAccount(UserEntity user) {
        if (user.getStatus() != StatusConstain.ACTIVE) {
            throw new AppException(ErrorCode.ACCOUNT_INACTIVE);
        }
    }

    private Set<RoleConstain> resolveRoles(UserEntity user) {
        if (CollectionUtils.isEmpty(user.getRoles())) {
            return Collections.emptySet();
        }

        return user.getRoles().stream().map(role -> role.getRole()).collect(Collectors.toSet());
    }

    private String resolveEmail(UserEntity user) {
        if (user.getStudent() != null) {
            return user.getStudent().getEmail();
        }
        return user.getLecture() == null ? null : user.getLecture().getEmaillecture();
    }

    private String resolvePhone(UserEntity user) {
        if (user.getStudent() != null) {
            return user.getStudent().getPhoneStudent();
        }
        return user.getLecture() == null ? null : user.getLecture().getPhoneLecture();
    }

    public SignedJWT verifyToken(String token, boolean isRefresh) throws JOSEException, ParseException {
        return verifyToken(token, isRefresh ? REFRESH_TOKEN_TYPE : ACCESS_TOKEN_TYPE);
    }

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
