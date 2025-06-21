package com.delight.account.aop;

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
            logger.debug("Enter: {} with args {}", joinPoint.getSignature(), joinPoint.getArgs());
        } else {
            logger.info("Enter: {}", joinPoint.getSignature());
        }
        try {
            Object result = joinPoint.proceed();
            if (logger.isDebugEnabled()) {
                logger.debug("Exit: {} with result {}", joinPoint.getSignature(), result);
            } else {
                logger.info("Exit: {}", joinPoint.getSignature());
            }
            return result;
        } catch (Throwable e) {
            logger.error("Error in {}", joinPoint.getSignature(), e);
            throw e;
        }
    }
}
