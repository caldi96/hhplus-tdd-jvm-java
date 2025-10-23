package io.hhplus.tdd.point.service;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.point.PointHistory;
import io.hhplus.tdd.point.TransactionType;
import io.hhplus.tdd.point.UserPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Service
@RequiredArgsConstructor
public class PointService {

    private final UserPointTable userPointTable;
    private final PointHistoryTable pointHistoryTable;

    // 사용자별 Lock을 관리하는 ConcurrentHashMap
    private final ConcurrentHashMap<Long, ReentrantLock> userLocks = new ConcurrentHashMap<>();

    // 사용자별 Lock 획득
    private Lock getUserLock(long userId) {
        return userLocks.computeIfAbsent(userId, id -> new ReentrantLock());
    }

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
        Lock lock = getUserLock(id);
        lock.lock();
        try {
            // 1단위는 버림
            long actualAmount = amount / 10 * 10;

            if (actualAmount <= 0) {
                throw new IllegalArgumentException("충전할 포인트는 0보다 커야합니다.");
            }

            UserPoint userPoint = getPoint(id);
            long newAmount = userPoint.point() + actualAmount;
            UserPoint chargedUserPoint = userPointTable.insertOrUpdate(id, newAmount);

            pointHistoryTable.insert(id, amount, TransactionType.CHARGE, System.currentTimeMillis());

            return chargedUserPoint;
        } finally {
            lock.unlock();
        }
    }

    // 포인트 사용
    public UserPoint usePoint(long id, long amount) {
        // 최소 사용금액 1000 이상
        if (amount < 1000L) throw new IllegalArgumentException(String.format("최소 사용 금액은 1000포인트 이상이여야 합니다. 사용 금액 : %d", amount));

        // 500 단위로 사용 가능
        if (amount % 500L != 0) throw new IllegalArgumentException(String.format("500포인트 단위로 사용 가능합니다. 사용 금액 : %d", amount));

        UserPoint userPoint = getPoint(id);
        long currentPoint = userPoint.point();

        // 보유 포인트 5000 이상일 경우 사용 가능
        if (currentPoint < 5000L) throw new IllegalArgumentException(String.format("보유 포인트가 5000 이상부터 사용 가능합니다. 보유 금액 : %d", currentPoint));

        // 사용 금액이 현재 보유 금액보다 작아야 함
        if (amount > currentPoint) throw new IllegalArgumentException(String.format("사용 금액이 현재 금액보다 큽니다.\n현재 금액 : %d, 사용 금액 : %d", currentPoint, amount));
        long newAmount = userPoint.point() - amount;
        UserPoint usedUserPoint = userPointTable.insertOrUpdate(id, newAmount);

        pointHistoryTable.insert(id, amount, TransactionType.USE, System.currentTimeMillis());

        return usedUserPoint;
    }
}