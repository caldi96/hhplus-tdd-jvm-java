package io.hhplus.tdd.point.service;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.point.PointHistory;
import io.hhplus.tdd.point.TransactionType;
import io.hhplus.tdd.point.UserPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PointService {

    private final UserPointTable userPointTable;
    private final PointHistoryTable pointHistoryTable;

    // 포인트 조회
    public UserPoint getPoint(long id) {
        if (id <= 0L) {
            throw new IllegalArgumentException("유효하지 않는 사용자 ID입니다.");
        }

        UserPoint userPoint = userPointTable.selectById(id);

        if (userPoint == null) {
            throw new IllegalArgumentException("존재하지 않는 사용자입니다.");
        }

        return userPoint;
    }

    // 포인트 충전/사용 내역 조회
    public List<PointHistory> getPointHistories(long userId) {
        if (userId <= 0L) {
            throw new IllegalArgumentException("유효하지 않는 사용자 ID입니다.");
        }

        // 사용자 존재 여부 확인
        UserPoint userPoint = userPointTable.selectById(userId);
        if (userPoint == null) {
            throw new IllegalArgumentException("존재하지 않는 사용자입니다.");
        }

        return pointHistoryTable.selectAllByUserId(userId);
    }

    // 포인트 충전
    public UserPoint chargePoint(long id, long amount) {
        UserPoint userPoint = getPoint(id);
        long newAmount = userPoint.point() + amount;
        UserPoint newUserPoint = userPointTable.insertOrUpdate(id, newAmount);
        return newUserPoint;
    }
}
