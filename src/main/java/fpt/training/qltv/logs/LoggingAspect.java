package fpt.training.qltv.logs;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Aspect
@Component
public class LoggingAspect {

    @Around("execution(* fpt.training.qltv.controller..*(..))")
    public Object logMethodExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        HttpServletRequest request = attributes.getRequest();

        long startTime = System.currentTimeMillis();

        log.info(
                "REQUEST START | {} {} | IP={}",
                request.getMethod(),
                request.getRequestURI(),
                request.getRemoteAddr());

        try {
            return joinPoint.proceed();
        } finally {

            long executionTime = System.currentTimeMillis() - startTime;

            log.info(
                    "REQUEST END | {} {} | Duration={} ms",
                    request.getMethod(),
                    request.getRequestURI(),
                    executionTime);
        }
    }

}
