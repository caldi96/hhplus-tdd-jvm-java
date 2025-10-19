package io.hhplus.tdd.point.service;

import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.point.UserPoint;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

public class PointServiceTest {

    @Mock
    private UserPointTable userPointTable;

    @InjectMocks
    private PointService pointService;

    @Test
    @DisplayName("포인트 조회 실패")
    void selectUserPoint_fail() {
        // given 사용자 1번이 1000 포인트를 가지고 있다.
        long id = 1;
        userPointTable.insertOrUpdate(id, 1000L);
        int point = PointService.getPoint(id);
    }
}
