package io.drozda.sandbox.visualization.aop;

import org.springframework.stereotype.Component;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Aspect
@Component
public class VisualActionAspect {
    private static final Logger log = LoggerFactory.getLogger(VisualActionAspect.class);

    @Around("@annotation(visualAction)")
    public Object aroundVisualAction(ProceedingJoinPoint pjp, VisualAction visualAction)
            throws Throwable {
        log.info("VISUAL before: action={}, method={}", visualAction.value(), pjp.getSignature().toShortString());

        try {
            Object proceed = pjp.proceed();
            log.info("VISUAL after: action={}, method={}", visualAction.value(), pjp.getSignature().toShortString());
            return proceed;
        } catch (Throwable throwable) {
            log.error("VISUAL error: action={}, method={}", visualAction.value(), pjp.getSignature().toShortString(),
                    throwable);
            throw throwable;
        }
    }
}
