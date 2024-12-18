package com.example.stock.facade;

import com.example.stock.repository.LockRepository;
import com.example.stock.service.StockService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class NameLockStockFacade {

    private final LockRepository lockRepository;

    private final StockService stockService;

    public NameLockStockFacade(LockRepository lockRepository, StockService stockService) {
        this.lockRepository = lockRepository;
        this.stockService = stockService;
    }

    @Transactional
    public void decrease(Long id, Long quantity) throws InterruptedException {
        try {
            lockRepository.getLock(String.valueOf(id));
            stockService.decrease(id, quantity);
        } finally {
            lockRepository.releaseLock(String.valueOf(id));
        }
    }

}
