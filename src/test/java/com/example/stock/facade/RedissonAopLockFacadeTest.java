package com.example.stock.facade;

import com.example.stock.domain.Stock;
import com.example.stock.repository.StockRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
public class RedissonAopLockFacadeTest {
    @Autowired
    private RedissonAopLockStockFacade redissonAopLockStockFacade;

    @Autowired
    private StockRepository stockRepository;

    @BeforeEach
    void setUp() {
        stockRepository.saveAndFlush(new Stock(1L, 100L));
    }

    @AfterEach
    void after() {
        stockRepository.deleteAll();
    }

    @Test
    void 동시에_100개_요청() throws InterruptedException {
        int threadCount = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(32);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    redissonAopLockStockFacade.decrease(1L, 1L);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();

        Stock stock = stockRepository.findById(1L).orElseThrow();
        assertEquals(0L, stock.getQuantity());
    }

}
