package com.delight.account.aop;

import com.delight.account.util.LoggingUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class LoggingAspect {
    private static final Logger logger = LoggerFactory.getLogger(LoggingAspect.class);

    @Around("execution(public * com.delight..*(..))")
    public Object logAround(ProceedingJoinPoint joinPoint) throws Throwable {
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
