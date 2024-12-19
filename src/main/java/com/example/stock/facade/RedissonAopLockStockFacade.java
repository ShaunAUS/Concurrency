package com.example.stock.facade;

import com.example.stock.redisson.DistributedLock;
import com.example.stock.service.StockService;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class RedissonAopLockStockFacade {

    private StockService stockService;

    public RedissonAopLockStockFacade(StockService stockService) {
        this.stockService = stockService;
    }

    @DistributedLock(key = "'stock:' + #stockId")
    public void decrease(Long stockId, Long quantity) {
        stockService.decrease(stockId, quantity);
    }
}