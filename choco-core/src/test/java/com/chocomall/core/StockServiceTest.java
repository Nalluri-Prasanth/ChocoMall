package com.chocomall.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.chocomall.common.exception.ApiException;
import com.chocomall.core.stock.StockService;
import com.chocomall.model.entity.Product;
import com.chocomall.model.enums.ProductKind;
import com.chocomall.model.enums.StockReason;
import com.chocomall.model.repository.StockMovementRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest(classes = CoreTestApplication.class)
class StockServiceTest {

    @Autowired StockService stock;
    @Autowired Fixtures fixtures;
    @Autowired TransactionTemplate tx;
    @Autowired StockMovementRepository movements;

    @Test
    void lockThenSaleMovesUnitsFromReservedToSold() {
        Product p = fixtures.product(10, "2.00");

        tx.executeWithoutResult(s -> stock.lock(p, 4, "T1"));
        Product afterLock = fixtures.fresh(p);
        assertThat(afterLock.getStock()).isEqualTo(10);
        assertThat(afterLock.getLockStock()).isEqualTo(4);

        tx.executeWithoutResult(s -> stock.confirmSale(p, 4, 4, "T1"));
        Product afterSale = fixtures.fresh(p);
        assertThat(afterSale.getStock()).isEqualTo(6);
        assertThat(afterSale.getLockStock()).isZero();
        assertThat(afterSale.getSale()).isEqualTo(4);
    }

    @Test
    void releaseReturnsReservedUnits() {
        Product p = fixtures.product(5, "2.00");
        tx.executeWithoutResult(s -> stock.lock(p, 3, "T2"));
        tx.executeWithoutResult(s -> stock.release(p, 3, "T2"));

        assertThat(fixtures.fresh(p).getLockStock()).isZero();
        assertThat(fixtures.fresh(p).getStock()).isEqualTo(5);
    }

    @Test
    void cannotReserveMoreThanIsAvailable() {
        Product p = fixtures.product(2, "2.00");

        assertThatThrownBy(() -> tx.executeWithoutResult(s -> stock.lock(p, 3, "T3")))
                .isInstanceOf(ApiException.class).hasMessageContaining("Not enough stock");
        assertThat(fixtures.fresh(p).getLockStock()).isZero();
    }

    @Test
    void concurrentCheckoutsNeverOversell() throws Exception {
        Product p = fixtures.product(10, "2.00");
        int buyers = 30;
        ExecutorService pool = Executors.newFixedThreadPool(10);
        CountDownLatch go = new CountDownLatch(1);
        AtomicInteger succeeded = new AtomicInteger();
        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < buyers; i++) {
            futures.add(pool.submit(() -> {
                go.await();
                try {
                    tx.executeWithoutResult(s -> stock.lock(p, 1, "RACE"));
                    succeeded.incrementAndGet();
                } catch (ApiException expected) {
                    // out of stock
                }
                return null;
            }));
        }
        go.countDown();
        for (Future<?> f : futures) {
            f.get();
        }
        pool.shutdown();

        assertThat(succeeded.get()).isEqualTo(10);
        Product after = fixtures.fresh(p);
        assertThat(after.getLockStock()).isEqualTo(10);
        assertThat(after.getStock() - after.getLockStock()).isZero();
    }

    @Test
    void untrackedItemsNeverTouchStock() {
        Product drink = fixtures.product(0, "4.50", ProductKind.MADE_TO_ORDER);
        tx.executeWithoutResult(s -> stock.lock(drink, 100, "T4"));
        assertThat(fixtures.fresh(drink).getLockStock()).isZero();
        assertThat(StockService.tracksStock(drink)).isFalse();
    }

    @Test
    void everyChangeIsInTheLedgerAndAdjustmentsCannotEatReservedUnits() {
        Product p = fixtures.product(10, "2.00");
        tx.executeWithoutResult(s -> stock.lock(p, 8, "T5"));

        assertThatThrownBy(() -> stock.adjust(p.getId(), -5, StockReason.WASTE, "w", "tester"))
                .isInstanceOf(ApiException.class).hasMessageContaining("not reserved");

        int balance = stock.adjust(p.getId(), 5, StockReason.PRODUCTION, "BATCH-1", "tester");
        assertThat(balance).isEqualTo(15);
        assertThat(movements.findByProductIdOrderByIdDesc(p.getId(), PageRequest.of(0, 10)).getContent())
                .extracting("reason").contains(StockReason.ORDER_LOCK, StockReason.PRODUCTION);
    }

    @Test
    void stockTakeCannotGoBelowReservedUnits() {
        Product p = fixtures.product(10, "2.00");
        tx.executeWithoutResult(s -> stock.lock(p, 6, "T6"));

        assertThatThrownBy(() -> stock.setCount(p.getId(), 4, "tester")).isInstanceOf(ApiException.class);
        assertThat(stock.setCount(p.getId(), 9, "tester")).isEqualTo(9);
    }
}
