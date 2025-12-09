package com.delight.account.aop;

import com.delight.account.service.job.JobWorker;
import com.delight.account.util.LoggingUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

@Aspect
@Component
public class LoggingAspect {
    private static final Logger logger = LoggerFactory.getLogger(LoggingAspect.class);

    @Around("execution(public * com.delight.account.controller..*(..)) || execution(public * com.delight.account.service..*(..))")
    public Object logAround(ProceedingJoinPoint joinPoint) throws Throwable {
        // Check if this is a background job (JobWorker.processJob)
        boolean isBackgroundJob = isBackgroundJob(joinPoint);
        
        if (isBackgroundJob) {
            // Extract jobId, accountId, userId from method parameters
            Object[] args = joinPoint.getArgs();
            Long jobId = null;
            Long accountId = null;
            Long userId = null;
            
            if (args.length >= 1 && args[0] instanceof Long) {
                jobId = (Long) args[0];
            }
            if (args.length >= 2 && args[1] instanceof Long) {
                accountId = (Long) args[1];
            }
            if (args.length >= 3 && args[2] instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> metadata = (Map<String, Object>) args[2];
                // Extract accountId from metadata if not in parameter
                if (accountId == null && metadata != null && metadata.containsKey("accountId")) {
                    Object accountIdObj = metadata.get("accountId");
                    if (accountIdObj instanceof Long) {
                        accountId = (Long) accountIdObj;
                    } else if (accountIdObj instanceof Number) {
                        accountId = ((Number) accountIdObj).longValue();
                    }
                }
                // Extract userId from metadata
                if (metadata != null && metadata.containsKey("userId")) {
                    Object userIdObj = metadata.get("userId");
                    if (userIdObj instanceof Long) {
                        userId = (Long) userIdObj;
                    } else if (userIdObj instanceof Number) {
                        userId = ((Number) userIdObj).longValue();
                    }
                }
            }
            
            if (logger.isDebugEnabled()) {
                LoggingUtils.debugWithJobContext(logger, jobId, accountId, userId, 
                    "Enter: {} with args {}", joinPoint.getSignature(), joinPoint.getArgs());
            }
            try {
                Object result = joinPoint.proceed();
                if (logger.isDebugEnabled()) {
                    LoggingUtils.debugWithJobContext(logger, jobId, accountId, userId,
                        "Exit: {} with result {}", joinPoint.getSignature(), result);
                }
                return result;
            } catch (Throwable e) {
                LoggingUtils.errorWithJobContext(logger, jobId, accountId, userId,
                    "Error in {}", joinPoint.getSignature(), e);
                throw e;
            }
        } else {
            // Regular request logging (uses MDC from filters)
            if (logger.isDebugEnabled()) {
                LoggingUtils.debug(logger, "Enter: {} with args {}", joinPoint.getSignature(), joinPoint.getArgs());
            }
            try {
                Object result = joinPoint.proceed();
                if (logger.isDebugEnabled()) {
                    LoggingUtils.debug(logger, "Exit: {} with result {}", joinPoint.getSignature(), result);
                }
                return result;
            } catch (Throwable e) {
                LoggingUtils.error(logger, "Error in {}", joinPoint.getSignature(), e);
                throw e;
            }
        }
    }
    
    /**
     * Check if the join point is a background job (JobWorker.processJob method).
     */
    private boolean isBackgroundJob(ProceedingJoinPoint joinPoint) {
        String methodName = joinPoint.getSignature().getName();
        Class<?> targetClass = joinPoint.getTarget().getClass();
        
        // Check if method is processJob and target implements JobWorker
        return "processJob".equals(methodName) && 
               (JobWorker.class.isAssignableFrom(targetClass) || 
                targetClass.getPackage().getName().contains("job.worker"));
    }
}
