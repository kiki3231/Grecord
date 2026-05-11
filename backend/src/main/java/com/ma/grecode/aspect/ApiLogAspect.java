package com.ma.grecode.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Slf4j
@Aspect
@Component
public class ApiLogAspect {

    @Pointcut("execution(* com.ma.grecode.controller..*(..))")
    public void controllerPointcut() {}

    @Around("controllerPointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().toShortString();
        Object[] args = joinPoint.getArgs();
        long start = System.currentTimeMillis();

        log.info(">> {} args={}", methodName, Arrays.toString(args));

        Object result;
        try {
            result = joinPoint.proceed();
        } catch (Throwable t) {
            long elapsed = System.currentTimeMillis() - start;
            log.error("<< {} ERROR [{}ms]: {}", methodName, elapsed, t.getMessage());
            throw t;
        }

        long elapsed = System.currentTimeMillis() - start;
        if (elapsed > 500) {
            log.warn("<< {} SLOW [{}ms]", methodName, elapsed);
        } else {
            log.info("<< {} [{}ms]", methodName, elapsed);
        }

        return result;
    }
}
