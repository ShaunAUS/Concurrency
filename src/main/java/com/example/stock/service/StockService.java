package com.example.stock.service;

import com.example.stock.domain.Stock;
import com.example.stock.repository.StockRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StockService {


    private final StockRepository stockRepository;

    public StockService(StockRepository stockRepository) {
        this.stockRepository = stockRepository;
    }


    //synchronized + Transactional 트랜잭션 끝날떄쯤에 (db업데이트전)에 다른 스레드가 자원접근
    //synchronized 각 프로세스 안에서만 보장 , 즉 여러 쓰레드에서 동시에 접근하면 레이스컨디션 발생
/*
    @Transactional
    public synchronized void decrease(Long id, Long quantity){
        Stock stock = stockRepository.findById(id).orElseThrow();
        stock.decrease(quantity);
        stockRepository.save(stock);
    }
*/

    // 비관적락, 낙관적락
/*
    @Transactional
    public  void decrease(Long id, Long quantity){
        Stock stock = stockRepository.findById(id).orElseThrow();
        stock.decrease(quantity);
        stockRepository.save(stock);
    }
*/

    //namedLock
    // 부모의 트랜잭션과 별도로 실행되어야 하기때문
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public  void decrease(Long id, Long quantity){
        Stock stock = stockRepository.findById(id).orElseThrow();
        stock.decrease(quantity);
        stockRepository.save(stock);
    }


}
