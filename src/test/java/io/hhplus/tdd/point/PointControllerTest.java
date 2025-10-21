package io.hhplus.tdd.point;

import io.hhplus.tdd.point.service.PointService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PointController.class)
public class PointControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PointService pointService;

    @Test
    @DisplayName("포인트 조회 API 성")
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
    @DisplayName("유효하지 않은 ID로 포인트 조회 시 400 에러")
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
    @DisplayName("존재하지 않는 사용자 포인트 조회 시 400 에러")
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
}
