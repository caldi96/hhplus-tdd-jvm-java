package io.hhplus.tdd.point;

import io.hhplus.tdd.point.service.PointService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PointController.class)
public class PointControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PointService pointService;

    // ========== 포인트 조회 테스트 ==========
    @Test
    @DisplayName("포인트 조회 API 성공")
    void 포인트_조회_API_성공() throws Exception {
        // given
        long userId = 1L;
        long amount = 2000L;
        UserPoint mockUserPoint = new UserPoint(userId, amount, System.currentTimeMillis());

        // when
        when(pointService.getPoint(userId)).thenReturn(mockUserPoint);

        // then
        mockMvc.perform(get("/point/{id}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.point").value(amount));

        verify(pointService, times(1)).getPoint(userId);
    }

    @Test
    @DisplayName("유효하지 않은 ID로 포인트 조회 시 500 에러")
    void 유효하지_않는_ID_포인트_조회_실패() throws Exception {
        // given
        long invalidId = -1L;

        // when
        when(pointService.getPoint(invalidId))
                .thenThrow(new IllegalArgumentException("유효하지 않는 사용자 ID입니다."));

        // then
        mockMvc.perform(get("/point/{id}", invalidId))
                .andExpect(status().isInternalServerError());


        verify(pointService, times(1)).getPoint(invalidId);
    }

    @Test
    @DisplayName("존재하지 않는 사용자 포인트 조회 시 500 에러")
    void 존재하지_않는_사용자_포인트_조회_실패() throws Exception {
        // given
        long userId = 999L;

        // when
        when(pointService.getPoint(userId))
                .thenThrow(new IllegalArgumentException("존재하지 않는 사용자입니다."));

        // then
        mockMvc.perform(get("/point/{id}", userId))
                .andExpect(status().isInternalServerError());

        verify(pointService, times(1)).getPoint(userId);
    }

    // ========== 포인트 내역 조회 테스트 ==========
    @Test
    @DisplayName("포인트 내역 조회 API 성공")
    void 포인트_내역_조회_API_성공() throws Exception {
        // given
        long userId = 1L;
        List<PointHistory> mockHistories = List.of(
                new PointHistory(1L, userId, 1000L, TransactionType.CHARGE, System.currentTimeMillis()),
                new PointHistory(2L, userId, 500L, TransactionType.USE, System.currentTimeMillis())
        );

        // when
        when(pointService.getPointHistories(userId)).thenReturn(mockHistories);

        // then
        mockMvc.perform(get("/point/{id}/histories", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].userId").value(userId))
                .andExpect(jsonPath("$[0].amount").value(1000L))
                .andExpect(jsonPath("$[0].type").value(TransactionType.CHARGE.name()))
                .andExpect(jsonPath("$[1].amount").value(500L))
                .andExpect(jsonPath("$[1].type").value(TransactionType.USE.name()));

        verify(pointService, times(1)).getPointHistories(userId);
    }

    @Test
    @DisplayName("포인트 내역이 없는 경우 빈 배열 반환")
    void 포인트_내역_없음() throws Exception {
        // given
        long userId = 1L;

        // when
        when(pointService.getPointHistories(userId)).thenReturn(List.of());

        // then
        mockMvc.perform(get("/point/{id}/histories", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        verify(pointService, times(1)).getPointHistories(userId);
    }

    @Test
    @DisplayName("유효하지 않은 ID로 내역 조회 시 예외 발생")
    void 유효하지_않은_ID_내역_조회_실패() throws Exception {
        // given
        long invalidId = -1L;

        // when
        when(pointService.getPointHistories(invalidId))
                .thenThrow(new IllegalArgumentException("유효하지 않는 사용자 ID입니다."));

        // then
        mockMvc.perform(get("/point/{id}/histories", invalidId))
                .andExpect(status().isInternalServerError());

        verify(pointService, times(1)).getPointHistories(invalidId);
    }

    // ========== 포인트 충전 테스트 ==========
    @Test
    @DisplayName("포인트 충전 API 성공")
    void 포인트_충전_API_성공() throws Exception {
        // given
        long userId = 1L;
        long amount = 1000L;
        long currentPoint = 2000L;
        long expectedPoint = currentPoint + amount;
        UserPoint mockUserPoint = new UserPoint(userId, expectedPoint, System.currentTimeMillis());

        // when
        when(pointService.chargePoint(userId, amount)).thenReturn(mockUserPoint);

        // then
        mockMvc.perform(patch("/point/{id}/charge", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(amount)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.point").value(expectedPoint));

        verify(pointService, times(1)).chargePoint(userId, amount);
    }

    @Test
    @DisplayName("포인트 충전 - 유효하지 않은 ID로 충전 시 500 에러")
    void 포인트_충전_유효하지_않은_ID() throws Exception {
        // given
        long invalidId = -1L;
        long amount = 1000L;

        // when
        when(pointService.chargePoint(invalidId, amount))
                .thenThrow(new IllegalArgumentException("유효하지 않는 사용자 ID입니다."));

        // then
        mockMvc.perform(patch("/point/{id}/charge", invalidId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(amount)))
                .andExpect(status().isInternalServerError());

        verify(pointService, times(1)).chargePoint(invalidId, amount);
    }

    @Test
    @DisplayName("포인트 충전 - 존재하지 않는 사용자")
    void 포인트_충전_존재하지_않는_사용자() throws Exception {
        // given
        long userId = 999L;
        long amount = 1000L;

        // when
        when(pointService.chargePoint(userId, amount))
                .thenThrow(new IllegalArgumentException("존재하지 않는 사용자입니다."));

        // then
        mockMvc.perform(patch("/point/{id}/charge", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(amount)))
                .andExpect(status().isInternalServerError());

        verify(pointService, times(1)).chargePoint(userId, amount);
    }

    @Test
    @DisplayName("포인트 충전 - 0 이하 금액으로 충전 시 500 에러")
    void 포인트_충전_0이하_금액() throws Exception {
        // given
        long userId = 1L;
        long invalidAmount = 0L;

        // when
        when(pointService.chargePoint(userId, invalidAmount))
                .thenThrow(new IllegalArgumentException("충전할 포인트는 0보다 커야합니다."));

        // then
        mockMvc.perform(patch("/point/{id}/charge", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(invalidAmount)))
                .andExpect(status().isInternalServerError());

        verify(pointService, times(1)).chargePoint(userId, invalidAmount);
    }

    @Test
    @DisplayName("포인트 충전 - 10보다 작은 금액 충전 시 500 에러")
    void 포인트_충전_10보다_작은_금액() throws Exception {
        // given
        long userId = 1L;
        long invalidAmount = 9L;

        // when
        when(pointService.chargePoint(userId, invalidAmount))
                .thenThrow(new IllegalArgumentException("충전할 포인트는 0보다 커야합니다."));

        // then
        mockMvc.perform(patch("/point/{id}/charge", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(invalidAmount)))
                .andExpect(status().isInternalServerError());

        verify(pointService, times(1)).chargePoint(userId, invalidAmount);
    }

    @Test
    @DisplayName("포인트 충전 - 10단위로 충전 (1단위 버림)")
    void 포인트_충전_10단위_절삭() throws Exception {
        // given
        long userId = 1L;
        long amount = 1247L;
        long currentPoint = 2000L;
        long actualAmount = 1240L; // 1단위 버림
        long expectedPoint = currentPoint + actualAmount;
        UserPoint mockUserPoint = new UserPoint(userId, expectedPoint, System.currentTimeMillis());

        // when
        when(pointService.chargePoint(userId, amount)).thenReturn(mockUserPoint);

        // then
        mockMvc.perform(patch("/point/{id}/charge", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(amount)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.point").value(expectedPoint));

        verify(pointService, times(1)).chargePoint(userId, amount);
    }

    // ========== 포인트 사용 테스트 ==========
    @Test
    @DisplayName("포인트 사용 API 성공")
    void 포인트_사용_API_성공() throws Exception {
        // given
        long userId = 1L;
        long amount = 1000L;
        long currentPoint = 10000L;
        long expectedPoint = currentPoint - amount;
        UserPoint mockUserPoint = new UserPoint(userId, expectedPoint, System.currentTimeMillis());

        // when
        when(pointService.usePoint(userId, amount)).thenReturn(mockUserPoint);

        // then
        mockMvc.perform(patch("/point/{id}/use", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(amount)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.point").value(expectedPoint));

        verify(pointService, times(1)).usePoint(userId, amount);
    }

    @Test
    @DisplayName("포인트 사용 - 유효하지 않은 ID로 사용 시 500 에러")
    void 포인트_사용_유효하지_않은_ID() throws Exception {
        // given
        long invalidId = -1L;
        long amount = 1000L;

        // when
        when(pointService.usePoint(invalidId, amount))
                .thenThrow(new IllegalArgumentException("유효하지 않는 사용자 ID입니다."));

        // then
        mockMvc.perform(patch("/point/{id}/use", invalidId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(amount)))
                .andExpect(status().isInternalServerError());

        verify(pointService, times(1)).usePoint(invalidId, amount);
    }

    @Test
    @DisplayName("포인트 사용 - 존재하지 않는 사용자")
    void 포인트_사용_존재하지_않는_사용자() throws Exception {
        // given
        long userId = 999L;
        long amount = 1000L;

        // when
        when(pointService.usePoint(userId, amount))
                .thenThrow(new IllegalArgumentException("존재하지 않는 사용자입니다."));

        // then
        mockMvc.perform(patch("/point/{id}/use", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(amount)))
                .andExpect(status().isInternalServerError());

        verify(pointService, times(1)).usePoint(userId, amount);
    }

    @Test
    @DisplayName("포인트 사용 - 최소 사용 금액 1000 미만 시 500 에러")
    void 포인트_사용_최소_금액_미만() throws Exception {
        // given
        long userId = 1L;
        long invalidAmount = 500L;

        // when
        when(pointService.usePoint(userId, invalidAmount))
                .thenThrow(new IllegalArgumentException(
                        String.format("최소 사용 금액은 1000포인트 이상이여야 합니다. 사용 금액 : %d", invalidAmount)
                ));

        // then
        mockMvc.perform(patch("/point/{id}/use", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(invalidAmount)))
                .andExpect(status().isInternalServerError());

        verify(pointService, times(1)).usePoint(userId, invalidAmount);
    }

    @Test
    @DisplayName("포인트 사용 - 500 단위가 아닐 경우 500 에러")
    void 포인트_사용_500단위_아님() throws Exception {
        // given
        long userId = 1L;
        long invalidAmount = 2300L;

        // when
        when(pointService.usePoint(userId, invalidAmount))
                .thenThrow(new IllegalArgumentException(
                        String.format("500포인트 단위로 사용 가능합니다. 사용 금액 : %d", invalidAmount)
                ));

        // then
        mockMvc.perform(patch("/point/{id}/use", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(invalidAmount)))
                .andExpect(status().isInternalServerError());

        verify(pointService, times(1)).usePoint(userId, invalidAmount);
    }

    @Test
    @DisplayName("포인트 사용 - 보유 포인트 5000 미만 시 500 에러")
    void 포인트_사용_보유_포인트_부족() throws Exception {
        // given
        long userId = 1L;
        long amount = 2000L;
        long currentPoint = 3000L;

        // when
        when(pointService.usePoint(userId, amount))
                .thenThrow(new IllegalArgumentException(
                        String.format("보유 포인트가 5000 이상부터 사용 가능합니다. 보유 금액 : %d", currentPoint)
                ));

        // then
        mockMvc.perform(patch("/point/{id}/use", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(amount)))
                .andExpect(status().isInternalServerError());

        verify(pointService, times(1)).usePoint(userId, amount);
    }

    @Test
    @DisplayName("포인트 사용 - 사용 금액이 보유 금액 초과 시 500 에러")
    void 포인트_사용_잔액_초과() throws Exception {
        // given
        long userId = 1L;
        long currentPoint = 5000L;
        long amount = 10000L;

        // when
        when(pointService.usePoint(userId, amount))
                .thenThrow(new IllegalArgumentException(
                        String.format("사용 금액이 현재 금액보다 큽니다.\n현재 금액 : %d, 사용 금액 : %d", currentPoint, amount)
                ));

        // then
        mockMvc.perform(patch("/point/{id}/use", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(amount)))
                .andExpect(status().isInternalServerError());

        verify(pointService, times(1)).usePoint(userId, amount);
    }

    @Test
    @DisplayName("포인트 사용 - 500 단위로 사용 성공")
    void 포인트_사용_500단위_성공() throws Exception {
        // given
        long userId = 1L;
        long amount = 2500L;
        long currentPoint = 10000L;
        long expectedPoint = currentPoint - amount;
        UserPoint mockUserPoint = new UserPoint(userId, expectedPoint, System.currentTimeMillis());

        // when
        when(pointService.usePoint(userId, amount)).thenReturn(mockUserPoint);

        // then
        mockMvc.perform(patch("/point/{id}/use", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(amount)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.point").value(expectedPoint));

        verify(pointService, times(1)).usePoint(userId, amount);
    }

    @Test
    @DisplayName("포인트 사용 - 보유 포인트 5000일 때 사용 성공 (경계값)")
    void 포인트_사용_보유_5000_경계값() throws Exception {
        // given
        long userId = 1L;
        long amount = 2500L;
        long currentPoint = 5000L;
        long expectedPoint = currentPoint - amount;
        UserPoint mockUserPoint = new UserPoint(userId, expectedPoint, System.currentTimeMillis());

        // when
        when(pointService.usePoint(userId, amount)).thenReturn(mockUserPoint);

        // then
        mockMvc.perform(patch("/point/{id}/use", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(amount)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.point").value(expectedPoint));

        verify(pointService, times(1)).usePoint(userId, amount);
    }
}
