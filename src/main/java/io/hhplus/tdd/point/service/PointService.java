package io.hhplus.tdd.point.service;

import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.point.UserPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PointService {

    private UserPointTable userPointTable;

    public long getPoint(long id) {
        UserPoint userPoint = userPointTable.selectById(id);

        if (userPoint == null) {
            throw new IllegalArgumentException("존재하지 않는 사용자입니다.");
        }

        return userPoint.point();
    }
}
