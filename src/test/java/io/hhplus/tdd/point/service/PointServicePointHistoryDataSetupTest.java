package io.hhplus.tdd.point.service;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.point.PointHistory;
import io.hhplus.tdd.point.TransactionType;
import io.hhplus.tdd.point.UserPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PointServicePointHistoryDataSetupTest {
    @Mock
    private UserPointTable userPointTable;

    @Mock
    private PointHistoryTable pointHistoryTable;

    @InjectMocks
    private PointService pointService;

    private Long testUserId;
    private List<PointHistory> setupHistories;
    private UserPoint testUserPoint;

    @BeforeEach
    void setUp() {
        // 테스트용 사용자 ID
        testUserId = 1L;

        // 포인트 내역 조회 테스트를 위한 데이터 셋업
        // 충전 1000 -> 사용 300 -> 충전 2000 -> 사용 500
        setupHistories = List.of(
                new PointHistory(1L, testUserId, 1000L, TransactionType.CHARGE, System.currentTimeMillis()),
                new PointHistory(2L, testUserId, 300L, TransactionType.USE, System.currentTimeMillis()),
                new PointHistory(3L, testUserId, 2000L, TransactionType.CHARGE, System.currentTimeMillis()),
                new PointHistory(4L, testUserId, 500L, TransactionType.USE, System.currentTimeMillis())
        );

        // 현재 포인트: 1000 - 300 + 2000 - 500 = 2200
        testUserPoint = new UserPoint(testUserId, 2200L, System.currentTimeMillis());

        // Mock 설정
        when(userPointTable.selectById(testUserId)).thenReturn(testUserPoint);
        when(pointHistoryTable.selectAllByUserId(testUserId)).thenReturn(setupHistories);
    }

    @Test
    @DisplayName("setUp으로 준비된 포인트 내역을 조회한다")
    void 포인트_내역_조회_setUp_데이터() {
        // when
        List<PointHistory> histories = pointService.getPointHistories(testUserId);

        // then
        assertThat(histories).hasSize(4);
        assertThat(histories).isEqualTo(setupHistories);

        verify(userPointTable, times(1)).selectById(testUserId);
        verify(pointHistoryTable, times(1)).selectAllByUserId(testUserId);
    }

    @Test
    @DisplayName("포인트 내역의 금액을 검증한다")
    void 포인트_내역_금액_검증() {
        // when
        List<PointHistory> histories = pointService.getPointHistories(testUserId);

        // then
        assertThat(histories.get(0).amount()).isEqualTo(1000L);
        assertThat(histories.get(1).amount()).isEqualTo(300L);
        assertThat(histories.get(2).amount()).isEqualTo(2000L);
        assertThat(histories.get(3).amount()).isEqualTo(500L);
    }

    @Test
    @DisplayName("포인트 내역의 타입을 검증한다 - 충전/사용 순서")
    void 포인트_내역_타입_검증() {
        // when
        List<PointHistory> histories = pointService.getPointHistories(testUserId);

        // then
        assertThat(histories.get(0).type()).isEqualTo(TransactionType.CHARGE);
        assertThat(histories.get(1).type()).isEqualTo(TransactionType.USE);
        assertThat(histories.get(2).type()).isEqualTo(TransactionType.CHARGE);
        assertThat(histories.get(3).type()).isEqualTo(TransactionType.USE);
    }

    @Test
    @DisplayName("포인트 내역의 사용자 ID를 검증한다")
    void 포인트_내역_사용자ID_검증() {
        // when
        List<PointHistory> histories = pointService.getPointHistories(testUserId);

        // then
        assertThat(histories).allMatch(history -> history.userId() == testUserId);
    }

    @Test
    @DisplayName("충전 내역만 필터링하여 검증한다")
    void 충전_내역만_필터링_검증() {
        // when
        List<PointHistory> histories = pointService.getPointHistories(testUserId);
        List<PointHistory> chargeHistories = histories.stream()
                .filter(h -> h.type() == TransactionType.CHARGE)
                .toList();

        // then
        assertThat(chargeHistories).hasSize(2);
        assertThat(chargeHistories.get(0).amount()).isEqualTo(1000L);
        assertThat(chargeHistories.get(1).amount()).isEqualTo(2000L);
    }

    @Test
    @DisplayName("사용 내역만 필터링하여 검증한다")
    void 사용_내역만_필터링_검증() {
        // when
        List<PointHistory> histories = pointService.getPointHistories(testUserId);
        List<PointHistory> useHistories = histories.stream()
                .filter(h -> h.type() == TransactionType.USE)
                .toList();

        // then
        assertThat(useHistories).hasSize(2);
        assertThat(useHistories.get(0).amount()).isEqualTo(300L);
        assertThat(useHistories.get(1).amount()).isEqualTo(500L);
    }

    @Test
    @DisplayName("총 충전 금액을 계산한다")
    void 총_충전_금액_계산() {
        // when
        List<PointHistory> histories = pointService.getPointHistories(testUserId);
        long totalCharged = histories.stream()
                .filter(h -> h.type() == TransactionType.CHARGE)
                .mapToLong(PointHistory::amount)
                .sum();

        // then
        assertThat(totalCharged).isEqualTo(3000L); // 1000 + 2000
    }

    @Test
    @DisplayName("총 사용 금액을 계산한다")
    void 총_사용_금액_계산() {
        // when
        List<PointHistory> histories = pointService.getPointHistories(testUserId);
        long totalUsed = histories.stream()
                .filter(h -> h.type() == TransactionType.USE)
                .mapToLong(PointHistory::amount)
                .sum();

        // then
        assertThat(totalUsed).isEqualTo(800L); // 300 + 500
    }
}

