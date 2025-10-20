package io.hhplus.tdd.point.service;

import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.point.UserPoint;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class PointServiceTest {

    private UserPointTable userPointTable = new UserPointTable();

    @InjectMocks
    private PointService pointService;

    @Test
    @DisplayName("포인트 조회 실패")
    void 포인트_조회_실패() {
        // given 사용자 1번이 1000 포인트를 가지고 있다.
        long id = 1L;
        long amount = 2000L;
        long point = pointService.getPoint(id, amount);
        assertThat(point).isEqualTo(amount);

        // table 조회
        long tablePoint = userPointTable.selectById(id).point();
        assertThat(tablePoint).isEqualTo(amount);
    }
}
