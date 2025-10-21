package io.hhplus.tdd.point.service;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.point.PointHistory;
import io.hhplus.tdd.point.TransactionType;
import io.hhplus.tdd.point.UserPoint;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PointServiceTest {

    @Mock
    private UserPointTable userPointTable;

    @Mock
    private PointHistoryTable pointHistoryTable;

    @InjectMocks
    private PointService pointService;

    // 포인트 조회
    @Test
    @DisplayName("포인트 조회 성공")
    void 포인트_조회_성공() {
        // given 사용자 1번이 2000 포인트를 가지고 있다.
        long id = 1L;
        long amount = 2000L;
        UserPoint mockUserPoint = new UserPoint(id, amount, System.currentTimeMillis());


        // when userPointTable 객체를 stub
        when(userPointTable.selectById(id)).thenReturn(mockUserPoint);

        // then
        UserPoint userPoint = pointService.getPoint(id);
        assertThat(userPoint).isNotNull();
        assertThat(userPoint.id()).isEqualTo(id);
        assertThat(userPoint.point()).isEqualTo(amount);

        // TablePoint 객체의 selectedBy(id) 메서드 호출 검증
        verify(userPointTable, times(1)).selectById(id);
    }

    @Test
    @DisplayName("포인트 조회-유저 존재 여부 확인 실패")
    void 존재하지_않는_사용자_예외() {
        long id = 999L;

        when(userPointTable.selectById(id)).thenReturn(null);

        // 예외 검증
        assertThatThrownBy(() -> pointService.getPoint(id))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("존재하지 않는 사용자입니다.");
    }

    @Test
    @DisplayName("유효하지 않은 ID로 조회 시 예외 발생")
    void 유효하지_않는_사용자_ID_예외() {
        long invalidId = -1L;

        // 예외 검증
        assertThatThrownBy(() -> pointService.getPoint(invalidId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("유효하지 않는 사용자 ID입니다.");

        // TablePoint 테이블이 조회되지 않았는지 확인
        verify(userPointTable, never()).selectById(invalidId);
    }

    @Test
    @DisplayName("ID가 0인 경우 예외 발생_경계값 테스트")
    void ID가_0인_경우_예외() {
        long invalidId = 0L;

        // 예외 검증
        assertThatThrownBy(() -> pointService.getPoint(invalidId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("유효하지 않는 사용자 ID입니다.");

        // TablePoint 테이블이 조회되지 않았는지 확인
        verify(userPointTable, never()).selectById(invalidId);
    }

    // 포인트 충전/이용 내역 조회
    @Test
    @DisplayName("포인트 충전/이용 내역 조회")
    void 포인트_충전_내역_조회_성공() {
        // given
        long userId = 1L;
        List<PointHistory> mockHistories = List.of(
                new PointHistory(1L, userId, 1000L, TransactionType.CHARGE, System.currentTimeMillis()),
                new PointHistory(2L, userId, 500L, TransactionType.USE, System.currentTimeMillis()),
                new PointHistory(3L, userId, 2000L, TransactionType.CHARGE, System.currentTimeMillis())
        );

        // when
        when(pointHistoryTable.selectAllByUserId(userId)).thenReturn(mockHistories);

        List<PointHistory> histories = pointService.getPointHistories(userId);

        assertThat(histories).hasSize(3);
        assertThat(1L).isEqualTo(mockHistories.get(0).id());
        assertThat(userId).isEqualTo(mockHistories.get(0).userId());
        assertThat(1000L).isEqualTo(mockHistories.get(0).amount());
        assertThat(TransactionType.CHARGE).isEqualTo(mockHistories.get(0).type());

        verify(pointHistoryTable, times(1)).selectAllByUserId(userId);
    }
}
