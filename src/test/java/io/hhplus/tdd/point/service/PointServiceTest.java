package io.hhplus.tdd.point.service;

import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.point.UserPoint;
import org.apache.catalina.User;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PointServiceTest {

    @Mock
    private UserPointTable userPointTable = new UserPointTable();

    @InjectMocks
    private PointService pointService;

    @Test
    @DisplayName("포인트 조회 실패")
    void 포인트_조회_실패() {
        // given 사용자 1번이 1000 포인트를 가지고 있다.
        long id = 1L;
        long amount = 2000L;

        // when userPointTable 객체를 stub
        when(userPointTable.selectById(id))
                .thenReturn(new UserPoint(id, amount, System.currentTimeMillis()));

        long point = pointService.getPoint(id);
        assertThat(point).isEqualTo(amount);

        // table 조회
//        long tablePoint = userPointTable.selectById(id).point();
//        assertThat(tablePoint).isEqualTo(amount);

        // TablePoint 객체의 selectedBy(id) 메서드 호출 검증
        verify(userPointTable, times(1)).selectById(id);
    }

    @Test
    @DisplayName("포인트 조회-유저 존재 여부 확인 실패")
    void 유저_조회() {
        long id = 999L;
//        long point = pointService.getPoint(id);

        // 예외 검증
        assertThatThrownBy(() -> pointService.getPoint(id))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("존재하지 않는 사용자입니다.");
    }
}
