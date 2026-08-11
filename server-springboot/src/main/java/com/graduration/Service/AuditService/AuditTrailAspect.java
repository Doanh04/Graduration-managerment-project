package com.graduration.Service.AuditService;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.graduration.Repository.AuditLogRepository;
import com.graduration.entity.AuditLogDocument;

import lombok.RequiredArgsConstructor;

@Aspect
@Component
@RequiredArgsConstructor
public class AuditTrailAspect {
    private static final Logger log = LoggerFactory.getLogger(AuditTrailAspect.class);

    private final AuditLogRepository auditLogRepository;

    @AfterReturning(
            pointcut = "execution(public * com.graduration.Service..*.create*(..)) || "
                    + "execution(public * com.graduration.Service..*.update*(..)) || "
                    + "execution(public * com.graduration.Service..*.delete*(..)) || "
                    + "execution(public * com.graduration.Service..*.register*(..)) || "
                    + "execution(public * com.graduration.Service..*.reset*(..)) || "
                    + "execution(public * com.graduration.Service..*.import*(..)) || "
                    + "execution(public * com.graduration.Service..*.add*(..)) || "
                    + "execution(public * com.graduration.Service..*.remove*(..)) || "
                    + "execution(public * com.graduration.Service..*.finish*(..))",
            returning = "result")
    public void recordSuccessfulChange(JoinPoint joinPoint, Object result) {
        String className = joinPoint.getTarget().getClass().getName();
        if (className.contains(".AuthenticationService.")
                || className.endsWith(".AuthenticationService")
                || className.contains(".AuditService.")) {
            return;
        }
        if (joinPoint.getSignature().getName().startsWith("finish")
                && result instanceof Number count
                && count.longValue() == 0) {
            return;
        }

        AuditLogDocument auditLog = buildAuditLog(joinPoint, result);
        saveAfterCommit(auditLog);
    }

    private AuditLogDocument buildAuditLog(JoinPoint joinPoint, Object result) {
        String serviceName = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        String resourceType = serviceName.endsWith("Service")
                ? serviceName.substring(0, serviceName.length() - "Service".length())
                : serviceName;

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId =
                authentication != null && authentication.isAuthenticated() ? authentication.getName() : "SYSTEM";
        String userName = resolveUserName(authentication, userId);

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("service", serviceName);
        metadata.put("method", methodName);

        return AuditLogDocument.builder()
                .userId(userId)
                .userName(userName)
                .action(toAction(methodName))
                .resourceType(resourceType)
                .resourceId(resolveResourceId(joinPoint.getArgs(), result))
                .description("Successful operation: " + serviceName + "." + methodName)
                .ipAddress(resolveIpAddress())
                .metadata(metadata)
                .createdAt(Instant.now())
                .build();
    }

    private void saveAfterCommit(AuditLogDocument auditLog) {
        if (TransactionSynchronizationManager.isActualTransactionActive()
                && TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    saveSafely(auditLog);
                }
            });
            return;
        }
        saveSafely(auditLog);
    }

    private void saveSafely(AuditLogDocument auditLog) {
        RuntimeException lastException = null;
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                auditLogRepository.save(auditLog);
                return;
            } catch (RuntimeException exception) {
                lastException = exception;
                log.warn("Could not save audit log on attempt {}", attempt, exception);
            }
        }
        log.error("Audit log was lost for {}.{}", auditLog.getResourceType(), auditLog.getAction(), lastException);
    }

    private String resolveResourceId(Object[] arguments, Object result) {
        String argumentId = extractResourceId(arguments);
        return argumentId != null ? argumentId : extractResultId(result);
    }

    private String extractResultId(Object result) {
        if (result == null) {
            return null;
        }
        BeanWrapperImpl wrapper = new BeanWrapperImpl(result);
        String[] idProperties = {
            "userId",
            "idUser",
            "studentCode",
            "lectureId",
            "academicId",
            "defensePeriodId",
            "idTeam",
            "majorId",
            "idClass",
            "idLibraryTopic",
            "templateId",
            "role",
            "permissionId"
        };
        for (String property : idProperties) {
            if (wrapper.isReadableProperty(property)) {
                Object value = wrapper.getPropertyValue(property);
                if (value != null) {
                    return String.valueOf(value);
                }
            }
        }
        return null;
    }

    private String resolveUserName(Authentication authentication, String fallback) {
        if (authentication instanceof JwtAuthenticationToken jwtAuthentication) {
            String userName = jwtAuthentication.getToken().getClaimAsString("userName");
            return userName == null || userName.isBlank() ? fallback : userName;
        }
        return fallback;
    }

    private String extractResourceId(Object[] arguments) {
        if (arguments == null) {
            return null;
        }
        for (Object argument : arguments) {
            if (argument instanceof String value && !value.isBlank()) {
                return value;
            }
            if (argument instanceof Number || argument instanceof Enum<?>) {
                return String.valueOf(argument);
            }
        }
        return null;
    }

    private String resolveIpAddress() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
            return null;
        }
        HttpServletRequest request = attributes.getRequest();
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String toAction(String methodName) {
        return methodName.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toUpperCase();
    }
}
