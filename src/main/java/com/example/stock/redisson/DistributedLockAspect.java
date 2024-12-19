package com.example.stock.redisson;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
public class DistributedLockAspect {
    private static final String REDISSON_LOCK_PREFIX = "LOCK:";

    private final RedissonClient redissonClient;
    private final RequireNewTransactionAspect requireNewTransactionAspect;

    public DistributedLockAspect(RedissonClient redissonClient, RequireNewTransactionAspect requireNewTransactionAspect) {
        this.redissonClient = redissonClient;
        this.requireNewTransactionAspect = requireNewTransactionAspect;
    }

    @Pointcut("@annotation(distributedLock)")
    public void pointCut(DistributedLock distributedLock) {
    }

    @Around(value = "pointCut(distributedLock)", argNames = "joinPoint,distributedLock")
    public Object around(ProceedingJoinPoint joinPoint, DistributedLock distributedLock) throws Throwable {
        String key = REDISSON_LOCK_PREFIX + generateLockKey(joinPoint, distributedLock);

        RLock rLock = redissonClient.getLock(key);
        try {
            boolean available = rLock.tryLock(
                    distributedLock.waitTime(),
                    distributedLock.leaseTime(),
                    distributedLock.timeUnit()
            );

            if (!available) {
                throw new RuntimeException("Lock acquisition failed");
            }

            // 분산락을 획득하면 새로운 트랜잭션을 시작하여 비즈니스 로직 실행
            return requireNewTransactionAspect.proceed(joinPoint);
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted during lock acquisition", e);
        } finally {
            try {
                rLock.unlock();
            } catch (IllegalMonitorStateException e) {
                log.info("Redisson lock already released");
            }
        }
    }

    //어노테이션을 적용한 메서드에서 키값 데이터 추출해서 키값생성
    private String generateLockKey(ProceedingJoinPoint joinPoint, DistributedLock distributedLock) {
        MethodSignature method = (MethodSignature) joinPoint.getSignature();
        Object[] parameters = joinPoint.getArgs();
        String[] parameterNames = method.getParameterNames();

        ExpressionParser parser = new SpelExpressionParser();
        StandardEvaluationContext context = new StandardEvaluationContext();

        for (int i = 0; i < parameterNames.length; i++) {
            context.setVariable(parameterNames[i], parameters[i]);
        }

        String lockKey = parser.parseExpression(distributedLock.key()).getValue(context, String.class);

        return lockKey;
    }
}