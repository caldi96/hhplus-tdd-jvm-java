package io.hhplus.tdd.point.service;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.point.UserPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.*;
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

        boolean completed = latch.await(10, TimeUnit.SECONDS);
        executorService.shutdown();

        // then
        assertThat(completed).isTrue().as("모든 스레드가 제시간에 완료되어야 함");

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

        boolean completed = latch.await(10, TimeUnit.SECONDS);
        executorService.shutdown();

        // then
        assertThat(completed).isTrue().as("모든 스레드가 제시간에 완료되어야 함");

        UserPoint finalUserPoint = pointService.getPoint(userId);

        log.info("=== 동시에 여러 사용 요청 - 동시성 테스트 결과 ===");
        log.info("성공 횟수: {}", successCount.get());
        log.info("실패 횟수: {}", failCount.get());
        log.info("최종 포인트: {}", finalUserPoint.point());
        log.info("예상 포인트: {}", expectedFinalPoint);

        assertThat(successCount.get()).isEqualTo(threadCount);
        assertThat(finalUserPoint.point()).isEqualTo(expectedFinalPoint);
    }

    @Test
    @DisplayName("동시에 충전과 사용 요청 - 최종 포인트가 정확해야 함")
    void 동시_충전과_사용_요청_처리() throws InterruptedException {
        // given
        long userId = 1L;
        long chargeAmount = 1000L; // 1000포인트씩 충전
        long useAmount = 1000L; // 1000포인트씩 사용
        int chargeThreadCount = 5;
        int useThreadCount = 3;
        long expectedFinalPoint = 10000L + (chargeAmount * chargeThreadCount) - (useAmount * useThreadCount);
        // 10000 + 5000 - 3000 = 12000

        ExecutorService executorService = Executors.newFixedThreadPool(chargeThreadCount + useThreadCount);
        CountDownLatch latch = new CountDownLatch(chargeThreadCount + useThreadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when - 충전 5번, 사용 3번 동시 실행
        for (int i = 0; i < chargeThreadCount; i++) {
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

        for (int i = 0; i < useThreadCount; i++) {
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

        boolean completed = latch.await(15, TimeUnit.SECONDS);
        executorService.shutdown();

        // then
        assertThat(completed).isTrue().as("모든 스레드가 제시간에 완료되어야 함");

        UserPoint finalUserPoint = pointService.getPoint(userId);

        log.info("=== 동시에 여러 충전과 사용 요청 - 동시성 테스트 결과 ===");
        log.info("성공 횟수: {}", successCount.get());
        log.info("실패 횟수: {}", failCount.get());
        log.info("최종 포인트: {}", finalUserPoint.point());
        log.info("예상 포인트: {}", expectedFinalPoint);

        assertThat(successCount.get()).isEqualTo(chargeThreadCount + useThreadCount);
        assertThat(finalUserPoint.point()).isEqualTo(expectedFinalPoint);
    }

    @Test
    @DisplayName("서로 다른 사용자의 동시 요청 - 각각 독립적으로 처리되어야 함")
    void 서로_다른_사용자_동시_요청_처리() throws InterruptedException {
        // given
        long userId1 = 1L;
        long userId2 = 2L;
        userPointTable.insertOrUpdate(userId2, 10000L);

        long chargeAmount = 1000L;
        int threadCountPerUser = 5;
        long expectedFinalPoint = 10000L + (chargeAmount * threadCountPerUser); // 15000

        ExecutorService executorService = Executors.newFixedThreadPool(threadCountPerUser * 2);
        CountDownLatch latch = new CountDownLatch(threadCountPerUser * 2);
        AtomicInteger successCount = new AtomicInteger(0);

        // when - 사용자1과 사용자2가 각각 5번씩 충전
        for (int i = 0; i < threadCountPerUser; i++) {
            executorService.submit(() -> {
                try {
                    pointService.chargePoint(userId1, chargeAmount);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    log.error("사용자1 충전 실패: " + e);
                } finally {
                    latch.countDown();
                }
            });

            executorService.submit(() -> {
                try {
                    pointService.chargePoint(userId2, chargeAmount);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    log.error("사용자2 충전 실패: " + e);
                } finally {
                    latch.countDown();
                }
            });
        }

        boolean completed = latch.await(15, TimeUnit.SECONDS);
        executorService.shutdown();

        // then
        assertThat(completed).isTrue().as("모든 스레드가 제시간에 완료되어야 함");

        UserPoint user1FinalPoint = pointService.getPoint(userId1);
        UserPoint user2FinalPoint = pointService.getPoint(userId2);

        log.info("=== 동시에 서로 다른 사용자의 요청 - 동시성 테스트 결과 ===");
        log.info("사용자1 최종 포인트: {}", user1FinalPoint.point());
        log.info("사용자2 최종 포인트: {}", user2FinalPoint.point());
        log.info("전체 성공 횟수: {}", successCount.get());

        assertThat(successCount.get()).isEqualTo(threadCountPerUser * 2);
        assertThat(user1FinalPoint.point()).isEqualTo(expectedFinalPoint);
        assertThat(user2FinalPoint.point()).isEqualTo(expectedFinalPoint);
    }

    @Test
    @DisplayName("동시성 테스트 - 포인트 사용 시 5000원 미만 검증")
    void 동시_사용_최소_보유_금액_검증() throws InterruptedException {
        // given
        long userId = 1L;
        long initialPoint = 6000L; // 6,000원
        int threadCount = 3;
        long useAmount = 1000L;

        userPointTable.insertOrUpdate(userId, initialPoint);

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when - 3번 동시 사용
        // 6000 (>= 5000) -> 5000 (성공)
        // 5000 (>= 5000) -> 4000 (성공)
        // 4000 (< 5000) -> 실패 (사용 전 잔액이 5000 미만)
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    pointService.usePoint(userId, useAmount);
                    successCount.incrementAndGet();
                } catch (IllegalArgumentException e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(10, TimeUnit.SECONDS);
        executorService.shutdown();

        // then
        UserPoint result = userPointTable.selectById(userId);

        log.info("=== 최소 보유 금액 검증 - 동시성 테스트 결과 ===");
        log.info("성공 횟수: {}", successCount.get());
        log.info("실패 횟수: {}", failCount.get());
        log.info("최종 포인트: {}", result.point());

        // 동시성 제어가 되어 있으면, 2번 성공하고 최종 포인트는 4000
        assertThat(successCount.get()).isEqualTo(2);
        assertThat(failCount.get()).isEqualTo(1);
        assertThat(result.point()).isEqualTo(4000L);
    }

    @Test
    @DisplayName("동시 사용 요청 중 잔액 부족 발생 - 잔액 초과 요청은 실패해야 함")
    void 동시_사용_중_잔액_부족_발생() throws InterruptedException {
        // given
        long userId = 1L;
        userPointTable.insertOrUpdate(userId, 10000L); // 10000 포인트

        long useAmount = 1500L; // 1500포인트씩 사용
        int threadCount = 10; // 10번 시도

        // 시나리오:
        // 10000 (≥5000) - 1500 = 8500
        // 8500 (≥5000) - 1500 = 7000
        // 7000 (≥5000) - 1500 = 5500
        // 5500 (≥5000) - 1500 = 4000
        // 4000 (≥5000) - 사용 불가
        // 총 4번 사용 가능
        int expectedSuccessCount = 4;
        long expectedFinalPoint = 4000L;

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failByInsufficientBalance = new AtomicInteger(0);
        AtomicInteger failByMinimumBalance = new AtomicInteger(0);
        ConcurrentLinkedQueue<String> errorMessages = new ConcurrentLinkedQueue<>();

        // when
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    pointService.usePoint(userId, useAmount);
                    successCount.incrementAndGet();
                    log.debug("사용 성공");
                } catch (IllegalArgumentException e) {
                    String message = e.getMessage();
                    errorMessages.offer(message);

                    // 에러 타입 분류
                    if (message.contains("사용 금액이 현재 금액보다 큽니다") ||
                            message.contains("현재 금액")) {
                        failByInsufficientBalance.incrementAndGet();
                        log.debug("잔액 부족으로 실패: {}", message);
                    } else if (message.contains("5000 이상")) {
                        failByMinimumBalance.incrementAndGet();
                        log.debug("최소 보유 포인트(5000) 미달로 실패: {}", message);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        boolean completed = latch.await(15, TimeUnit.SECONDS);
        executorService.shutdown();
        executorService.awaitTermination(5, TimeUnit.SECONDS);

        // then
        assertThat(completed).as("모든 스레드가 제한 시간 내에 완료되어야 함").isTrue();

        UserPoint finalUserPoint = pointService.getPoint(userId);
        int totalFailCount = failByInsufficientBalance.get() + failByMinimumBalance.get();

        log.info("=== 동시 사용 중 잔액 부족 발생 테스트 결과 ===");
        log.info("성공 횟수: {} (예상: {})", successCount.get(), expectedSuccessCount);
        log.info("잔액 부족 실패: {}", failByInsufficientBalance.get());
        log.info("최소 보유 포인트(5000) 미달 실패: {}", failByMinimumBalance.get());
        log.info("전체 실패: {} (예상: {})", totalFailCount, threadCount - expectedSuccessCount);
        log.info("최종 포인트: {} (예상: {})", finalUserPoint.point(), expectedFinalPoint);
        log.info("발생한 에러 유형: {}", errorMessages.stream().distinct().toList());

        // 검증
        assertThat(successCount.get() + totalFailCount).isEqualTo(threadCount);
        assertThat(successCount.get()).isEqualTo(expectedSuccessCount);
        assertThat(totalFailCount).isEqualTo(threadCount - expectedSuccessCount);
        assertThat(finalUserPoint.point()).isEqualTo(expectedFinalPoint);
    }

    @Test
    @DisplayName("보유 포인트 5000 이상에서는 사용 가능 - 경계값 테스트")
    void 최소_보유_포인트_5000_이상_사용_가능() throws InterruptedException {
        // given
        long userId = 1L;
        long initialPoint = 5000L; // 5000원 (경계값)
        long useAmount = 1000L;
        int threadCount = 3;

        userPointTable.insertOrUpdate(userId, initialPoint);

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when - 5000원일 때 1000원씩 3번 사용 시도
        // 5000 (>= 5000) -> 4000 (성공)
        // 4000 (< 5000) -> 실패
        // 4000 (< 5000) -> 실패
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    pointService.usePoint(userId, useAmount);
                    successCount.incrementAndGet();
                } catch (IllegalArgumentException e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        boolean completed = latch.await(10, TimeUnit.SECONDS);
        executorService.shutdown();
        executorService.awaitTermination(5, TimeUnit.SECONDS);

        // then
        UserPoint result = pointService.getPoint(userId);

        log.info("=== 보유 포인트 5000원 - 사용 가능 경계값 테스트 결과 ===");
        log.info("성공 횟수: {}", successCount.get());
        log.info("실패 횟수: {}", failCount.get());
        log.info("최종 포인트: {}", result.point());

        // 5000원일 때는 1번 사용 가능, 이후 4000원이 되어 2번 실패
        assertThat(completed).as("모든 스레드가 완료되어야 함").isTrue();
        assertThat(successCount.get()).isEqualTo(1).as("5000원일 때 1000원 사용 가능");
        assertThat(failCount.get()).isEqualTo(2).as("사용 후 4000원이 되어 나머지는 실패");
        assertThat(result.point()).isEqualTo(4000L);
    }

    @Test
    @DisplayName("보유 포인트 5000 미만에서는 사용 불가")
    void 최소_보유_포인트_5000_미만_사용_불가() throws InterruptedException {
        // given
        long userId = 1L;

        // 5000 미만 포인트들 테스트
        long[] invalidBalances = {4999L, 4500L, 3000L, 1000L};

        for (long balance : invalidBalances) {
            userPointTable.insertOrUpdate(userId, balance);

            // 매번 새로운 ExecutorService 생성
            ExecutorService executorService = Executors.newFixedThreadPool(3);
            CountDownLatch latch = new CountDownLatch(3);
            AtomicInteger failCount = new AtomicInteger(0);

            for (int i = 0; i < 3; i++) {
                executorService.submit(() -> {
                    try {
                        pointService.usePoint(userId, 1000L);
                    } catch (IllegalArgumentException e) {
                        failCount.incrementAndGet();
                        assertThat(e.getMessage()).contains("5000 이상");
                    } finally {
                        latch.countDown();
                    }
                });
            }

            boolean completed = latch.await(10, TimeUnit.SECONDS);
            executorService.shutdown();
            executorService.awaitTermination(5, TimeUnit.SECONDS); // 추가

            log.info("보유 포인트 {}원 - 실패 횟수: {}, 타임아웃: {}", balance, failCount.get(), !completed);

            assertThat(completed).as("%d 포인트 - 모든 스레드가 완료되어야 함", balance).isTrue();
            assertThat(failCount.get()).isEqualTo(3)
                    .as("%d 포인트에서는 사용 불가", balance);
            assertThat(pointService.getPoint(userId).point()).isEqualTo(balance)
                    .as("포인트는 변경되지 않아야 함");
        }
    }

    @Test
    @DisplayName("5000원 보유 시 5000원 전액 사용 가능 - 사용 후 0원")
    void 최소_보유_포인트_5000원_전액_사용_가능() throws InterruptedException {
        // given
        long userId = 1L;
        long initialPoint = 5000L; // 5000원 (경계값)
        long useAmount = 5000L; // 전액 사용

        userPointTable.insertOrUpdate(userId, initialPoint);

        ExecutorService executorService = Executors.newFixedThreadPool(1);
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when - 5000원 전액 사용
        executorService.submit(() -> {
            try {
                UserPoint result = pointService.usePoint(userId, useAmount);
                successCount.incrementAndGet();
                log.info("전액 사용 성공: {} -> {}", initialPoint, result.point());
            } catch (IllegalArgumentException e) {
                failCount.incrementAndGet();
                log.error("전액 사용 실패: {}", e.getMessage());
            } finally {
                latch.countDown();
            }
        });

        boolean completed = latch.await(10, TimeUnit.SECONDS);
        executorService.shutdown();
        executorService.awaitTermination(5, TimeUnit.SECONDS);

        // then
        UserPoint result = pointService.getPoint(userId);

        log.info("=== 5000원 전액 사용 테스트 결과 ===");
        log.info("성공 횟수: {}", successCount.get());
        log.info("실패 횟수: {}", failCount.get());
        log.info("최종 포인트: {}", result.point());

        assertThat(completed).as("스레드가 완료되어야 함").isTrue();
        assertThat(successCount.get()).isEqualTo(1).as("5000원 보유 시 5000원 전액 사용 가능");
        assertThat(failCount.get()).isEqualTo(0);
        assertThat(result.point()).isEqualTo(0L).as("사용 후 잔액 0원");
    }

    @Test
    @DisplayName("여러 스레드가 동시에 5000원 전액 사용 시도 - 1개만 성공")
    void 동시_전액_사용_시도() throws InterruptedException {
        // given
        long userId = 1L;
        long initialPoint = 5000L; // 5000원
        long useAmount = 5000L; // 전액 사용
        int threadCount = 10; // 10개 스레드

        userPointTable.insertOrUpdate(userId, initialPoint);

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failByMinimumBalance = new AtomicInteger(0);
        AtomicInteger failByInsufficientBalance = new AtomicInteger(0);
        ConcurrentLinkedQueue<String> errorMessages = new ConcurrentLinkedQueue<>();

        // when - 10개 스레드가 동시에 5000원 전액 사용 시도
        // 예상: 1개만 성공 (5000 -> 0), 나머지 9개는 실패 (0 < 5000 또는 잔액 부족)
        for (int i = 0; i < threadCount; i++) {
            final int threadNum = i;
            executorService.submit(() -> {
                try {
                    UserPoint result = pointService.usePoint(userId, useAmount);
                    successCount.incrementAndGet();
                    log.debug("스레드 {} - 전액 사용 성공: {}", threadNum, result.point());
                } catch (IllegalArgumentException e) {
                    String message = e.getMessage();
                    errorMessages.offer(message);

                    if (message.contains("5000 이상")) {
                        failByMinimumBalance.incrementAndGet();
                        log.debug("스레드 {} - 최소 보유 금액 미달로 실패", threadNum);
                    } else if (message.contains("사용 금액이 현재 금액보다 큽니다") ||
                               message.contains("현재 금액")) {
                        failByInsufficientBalance.incrementAndGet();
                        log.debug("스레드 {} - 잔액 부족으로 실패", threadNum);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        boolean completed = latch.await(15, TimeUnit.SECONDS);
        executorService.shutdown();
        executorService.awaitTermination(5, TimeUnit.SECONDS);

        // then
        UserPoint finalUserPoint = pointService.getPoint(userId);
        int totalFailCount = failByMinimumBalance.get() + failByInsufficientBalance.get();

        log.info("=== 동시 전액 사용 시도 테스트 결과 ===");
        log.info("성공 횟수: {}", successCount.get());
        log.info("최소 보유 금액 미달 실패: {}", failByMinimumBalance.get());
        log.info("잔액 부족 실패: {}", failByInsufficientBalance.get());
        log.info("전체 실패 횟수: {}", totalFailCount);
        log.info("최종 포인트: {}", finalUserPoint.point());
        log.info("발생한 에러 유형: {}", errorMessages.stream().distinct().toList());

        assertThat(completed).as("모든 스레드가 완료되어야 함").isTrue();
        assertThat(successCount.get()).isEqualTo(1).as("10개 중 1개만 성공");
        assertThat(totalFailCount).isEqualTo(9).as("나머지 9개는 실패");
        assertThat(successCount.get() + totalFailCount).isEqualTo(threadCount);
        assertThat(finalUserPoint.point()).isEqualTo(0L).as("최종 잔액 0원");
    }
}