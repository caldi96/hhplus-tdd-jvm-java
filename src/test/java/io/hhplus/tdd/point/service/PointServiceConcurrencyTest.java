package io.hhplus.tdd.point.service;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.point.UserPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SpringBootTest
public class PointServiceConcurrencyTest {

    private static final Logger log = LoggerFactory.getLogger(PointServiceConcurrencyTest.class);

    @Autowired
    private PointService pointService;

    @Autowired
    private UserPointTable userPointTable;

    @Autowired
    private PointHistoryTable pointHistoryTable;

    @BeforeEach
    void setUp() {
        // 테스트용 사용자 초기 포인트 설정 (10000 포인트)
        userPointTable.insertOrUpdate(1L, 10000L);
    }

    @Test
    @DisplayName("동시에 여러 충전 요청 - 모든 충전이 정상 반영되어야 함")
    void 동시_충전_요청_처리() throws InterruptedException {
        // given
        long userId = 1L;
        long chargeAmount = 1000L; // 1000포인트씩 충전
        int threadCount = 10; // 10개의 스레드
        long expectedFinalPoint = 10000L + (chargeAmount * threadCount); // 10000 + 10000 = 20000

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when - 동시에 10번 충전
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    pointService.chargePoint(userId, chargeAmount);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                    log.error("충전 실패: " + e);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(10, TimeUnit.SECONDS);
        executorService.shutdown();

        // then
        UserPoint finalUserPoint = pointService.getPoint(userId);

        log.info("=== 동시에 여러 충전 요청 - 동시성 테스트 결과 ===");
        log.info("성공 횟수: {}", successCount.get());
        log.info("실패 횟수: {}", failCount.get());
        log.info("최종 포인트: {}", finalUserPoint.point());
        log.info("예상 포인트: {}", expectedFinalPoint);

        assertThat(successCount.get()).isEqualTo(threadCount);
        assertThat(finalUserPoint.point()).isEqualTo(expectedFinalPoint);
    }

    @Test
    @DisplayName("동시에 여러 사용 요청 - 모든 사용이 정상 반영되어야 함")
    void 동시_사용_요청_처리() throws InterruptedException {
        // given
        long userId = 1L;
        long useAmount = 1000L; // 1000포인트씩 사용
        int threadCount = 5; // 5개의 스레드
        long expectedFinalPoint = 10000L - (useAmount * threadCount); // 10000 - 5000 = 5000

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when - 동시에 5번 사용
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    pointService.usePoint(userId, useAmount);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                    log.error("사용 실패: " + e);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(10, TimeUnit.SECONDS);
        executorService.shutdown();

        // then
        UserPoint finalUserPoint = pointService.getPoint(userId);

        log.info("=== 동시에 여러 사용 요청 - 동시성 테스트 결과 ===");
        log.info("성공 횟수: {}", successCount.get());
        log.info("실패 횟수: {}", failCount.get());
        log.info("최종 포인트: {}", finalUserPoint.point());
        log.info("예상 포인트: {}", expectedFinalPoint);

        assertThat(successCount.get()).isEqualTo(threadCount);
        assertThat(finalUserPoint.point()).isEqualTo(expectedFinalPoint);
    }
}