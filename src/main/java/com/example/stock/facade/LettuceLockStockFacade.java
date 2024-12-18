package com.example.stock.facade;

import com.example.stock.repository.RedisLockRepository;
import com.example.stock.service.StockService;
import org.springframework.stereotype.Component;

@Component
public class LettuceLockStockFacade {

    private RedisLockRepository redisLockRepository;
    private StockService stockService;

    public LettuceLockStockFacade(RedisLockRepository redisLockRepository, StockService stockService) {
        this.redisLockRepository = redisLockRepository;
        this.stockService = stockService;
    }

    public void decrease(Long id, Long quantity) throws InterruptedException {
        getLock(id);
        try {
            stockService.decrease(id, quantity);
        } finally {
            redisLockRepository.unlock(id);
        }
    }

    private void getLock(Long id) throws InterruptedException {
        while (failGetLockFromRedis(id)) {
            Thread.sleep(100); //Lettuce 는 스핀락단점이 있기때문에 레디스에 부담을 줄 수있어서 이렇게 텀을 줘야함
        }
    }

    private Boolean failGetLockFromRedis(Long id) {
        return !redisLockRepository.lock(id);
    }

}
