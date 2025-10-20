package io.hhplus.tdd.point.service;

import io.hhplus.tdd.database.UserPointTable;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PointService {

    private UserPointTable userPointTable;

    public long getPoint(long id) {
        long point = userPointTable.selectById(id).point();
        return point;
    }
}
