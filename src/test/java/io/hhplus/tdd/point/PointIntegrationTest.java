package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 포인트 API 통합 테스트
 * Controller → Service → Database까지 전체 플로우를 테스트합니다.
 */
@SpringBootTest
@AutoConfigureMockMvc
public class PointIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserPointTable userPointTable;

    @Autowired
    private PointHistoryTable pointHistoryTable;

    // ========== 포인트 조회 통합 테스트 ==========

    @Test
    @DisplayName("통합 테스트: 포인트 조회 - 신규 사용자 (포인트 0)")
    void 포인트_조회_신규_사용자() throws Exception {
        // given
        long userId = 1001L;

        // when & then
        mockMvc.perform(get("/point/{id}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.point").value(0));

        // 실제 데이터베이스 확인
        UserPoint userPoint = userPointTable.selectById(userId);
        assertThat(userPoint.id()).isEqualTo(userId);
        assertThat(userPoint.point()).isEqualTo(0);
    }

    @Test
    @DisplayName("통합 테스트: 포인트 조회 - 유효하지 않은 ID")
    void 포인트_조회_유효하지_않은_ID() throws Exception {
        // given
        long invalidId = -1L;

        // when & then
        mockMvc.perform(get("/point/{id}", invalidId))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("통합 테스트: 포인트 조회 - 충전 후 조회")
    void 포인트_조회_충전_후_조회() throws Exception {
        // given
        long userId = 1002L;
        long chargeAmount = 5000L;

        // 먼저 포인트 충전
        userPointTable.insertOrUpdate(userId, chargeAmount);

        // when & then
        mockMvc.perform(get("/point/{id}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.point").value(chargeAmount));
    }

    // ========== 포인트 내역 조회 통합 테스트 ==========

    @Test
    @DisplayName("통합 테스트: 포인트 내역 조회 - 신규 사용자 (내역 없음)")
    void 포인트_내역_조회_신규_사용자() throws Exception {
        // given
        long userId = 2001L;
        // 사용자를 먼저 생성 (포인트 0으로)
        userPointTable.insertOrUpdate(userId, 0L);

        // when & then
        mockMvc.perform(get("/point/{id}/histories", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        // 실제 데이터베이스 확인
        List<PointHistory> histories = pointHistoryTable.selectAllByUserId(userId);
        assertThat(histories).isEmpty();
    }

    @Test
    @DisplayName("통합 테스트: 포인트 내역 조회 - 충전 후 내역 확인")
    void 포인트_내역_조회_충전_후_확인() throws Exception {
        // given
        long userId = 2002L;
        long chargeAmount = 3000L;

        // 사용자 생성
        userPointTable.insertOrUpdate(userId, 0L);

        // 포인트 충전 (API 사용)
        mockMvc.perform(patch("/point/{id}/charge", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(chargeAmount)))
                .andExpect(status().isOk());

        // when & then - 내역 조회
        mockMvc.perform(get("/point/{id}/histories", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].userId").value(userId))
                .andExpect(jsonPath("$[0].amount").value(chargeAmount))
                .andExpect(jsonPath("$[0].type").value(TransactionType.CHARGE.name()));

        // 실제 데이터베이스 확인
        List<PointHistory> histories = pointHistoryTable.selectAllByUserId(userId);
        assertThat(histories).hasSize(1);
        assertThat(histories.get(0).userId()).isEqualTo(userId);
        assertThat(histories.get(0).amount()).isEqualTo(chargeAmount);
        assertThat(histories.get(0).type()).isEqualTo(TransactionType.CHARGE);
    }

    @Test
    @DisplayName("통합 테스트: 포인트 내역 조회 - 충전/사용 후 내역 2건 확인")
    void 포인트_내역_조회_충전_사용_후_확인() throws Exception {
        // given
        long userId = 2003L;
        long chargeAmount = 10000L;
        long useAmount = 2000L;

        // 사용자 생성
        userPointTable.insertOrUpdate(userId, 0L);

        // 포인트 충전
        mockMvc.perform(patch("/point/{id}/charge", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(chargeAmount)))
                .andExpect(status().isOk());

        // 포인트 사용
        mockMvc.perform(patch("/point/{id}/use", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(useAmount)))
                .andExpect(status().isOk());

        // when & then - 내역 조회
        mockMvc.perform(get("/point/{id}/histories", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].type").value(TransactionType.CHARGE.name()))
                .andExpect(jsonPath("$[1].type").value(TransactionType.USE.name()));

        // 실제 데이터베이스 확인
        List<PointHistory> histories = pointHistoryTable.selectAllByUserId(userId);
        assertThat(histories).hasSize(2);
        assertThat(histories.get(0).type()).isEqualTo(TransactionType.CHARGE);
        assertThat(histories.get(1).type()).isEqualTo(TransactionType.USE);
    }

    @Test
    @DisplayName("통합 테스트: 포인트 내역 조회 - 유효하지 않은 ID")
    void 포인트_내역_조회_유효하지_않은_ID() throws Exception {
        // given
        long invalidId = -1L;

        // when & then
        mockMvc.perform(get("/point/{id}/histories", invalidId))
                .andExpect(status().isInternalServerError());
    }

    // ========== 포인트 충전 통합 테스트 ==========

    @Test
    @DisplayName("통합 테스트: 포인트 충전 - 정상 충전")
    void 포인트_충전_정상() throws Exception {
        // given
        long userId = 3001L;
        long chargeAmount = 5000L;

        // 사용자 생성
        userPointTable.insertOrUpdate(userId, 0L);

        // when & then - 충전
        mockMvc.perform(patch("/point/{id}/charge", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(chargeAmount)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.point").value(chargeAmount));

        // 실제 데이터베이스 확인 - 포인트 잔액
        UserPoint userPoint = userPointTable.selectById(userId);
        assertThat(userPoint.point()).isEqualTo(chargeAmount);

        // 실제 데이터베이스 확인 - 히스토리
        List<PointHistory> histories = pointHistoryTable.selectAllByUserId(userId);
        assertThat(histories).hasSize(1);
        assertThat(histories.get(0).type()).isEqualTo(TransactionType.CHARGE);
        assertThat(histories.get(0).amount()).isEqualTo(chargeAmount);
    }

    @Test
    @DisplayName("통합 테스트: 포인트 충전 - 여러 번 충전")
    void 포인트_충전_여러번() throws Exception {
        // given
        long userId = 3002L;
        long firstCharge = 3000L;
        long secondCharge = 2000L;

        // 사용자 생성
        userPointTable.insertOrUpdate(userId, 0L);

        // when - 첫 번째 충전
        mockMvc.perform(patch("/point/{id}/charge", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(firstCharge)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.point").value(firstCharge));

        // when - 두 번째 충전
        mockMvc.perform(patch("/point/{id}/charge", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(secondCharge)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.point").value(firstCharge + secondCharge));

        // then - 실제 데이터베이스 확인
        UserPoint userPoint = userPointTable.selectById(userId);
        assertThat(userPoint.point()).isEqualTo(firstCharge + secondCharge);

        // 히스토리 2건 확인
        List<PointHistory> histories = pointHistoryTable.selectAllByUserId(userId);
        assertThat(histories).hasSize(2);
        assertThat(histories.get(0).type()).isEqualTo(TransactionType.CHARGE);
        assertThat(histories.get(1).type()).isEqualTo(TransactionType.CHARGE);
    }

    @Test
    @DisplayName("통합 테스트: 포인트 충전 - 10단위 절삭")
    void 포인트_충전_10단위_절삭() throws Exception {
        // given
        long userId = 3003L;
        long chargeAmount = 1247L;
        long expectedAmount = 1240L; // 1단위 버림

        // 사용자 생성
        userPointTable.insertOrUpdate(userId, 0L);

        // when & then
        mockMvc.perform(patch("/point/{id}/charge", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(chargeAmount)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.point").value(expectedAmount));

        // 실제 데이터베이스 확인
        UserPoint userPoint = userPointTable.selectById(userId);
        assertThat(userPoint.point()).isEqualTo(expectedAmount);
    }

    @Test
    @DisplayName("통합 테스트: 포인트 충전 - 0 이하 금액 충전 실패")
    void 포인트_충전_0이하_실패() throws Exception {
        // given
        long userId = 3004L;
        long invalidAmount = 0L;

        // 사용자 생성
        userPointTable.insertOrUpdate(userId, 0L);

        // when & then
        mockMvc.perform(patch("/point/{id}/charge", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(invalidAmount)))
                .andExpect(status().isInternalServerError());

        // 포인트가 변경되지 않았는지 확인
        UserPoint userPoint = userPointTable.selectById(userId);
        assertThat(userPoint.point()).isEqualTo(0L);
    }

    @Test
    @DisplayName("통합 테스트: 포인트 충전 - 유효하지 않은 ID")
    void 포인트_충전_유효하지_않은_ID() throws Exception {
        // given
        long invalidId = -1L;
        long chargeAmount = 1000L;

        // when & then
        mockMvc.perform(patch("/point/{id}/charge", invalidId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(chargeAmount)))
                .andExpect(status().isInternalServerError());
    }

    // ========== 포인트 사용 통합 테스트 ==========

    @Test
    @DisplayName("통합 테스트: 포인트 사용 - 정상 사용")
    void 포인트_사용_정상() throws Exception {
        // given
        long userId = 4001L;
        long initialPoint = 10000L;
        long useAmount = 2000L;
        long expectedPoint = initialPoint - useAmount;

        // 사용자 생성 및 충전
        userPointTable.insertOrUpdate(userId, initialPoint);

        // when & then - 사용
        mockMvc.perform(patch("/point/{id}/use", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(useAmount)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.point").value(expectedPoint));

        // 실제 데이터베이스 확인 - 포인트 잔액
        UserPoint userPoint = userPointTable.selectById(userId);
        assertThat(userPoint.point()).isEqualTo(expectedPoint);

        // 실제 데이터베이스 확인 - 히스토리
        List<PointHistory> histories = pointHistoryTable.selectAllByUserId(userId);
        assertThat(histories).hasSize(1);
        assertThat(histories.get(0).type()).isEqualTo(TransactionType.USE);
        assertThat(histories.get(0).amount()).isEqualTo(useAmount);
    }

    @Test
    @DisplayName("통합 테스트: 포인트 사용 - 500 단위 사용")
    void 포인트_사용_500단위() throws Exception {
        // given
        long userId = 4002L;
        long initialPoint = 10000L;
        long useAmount = 2500L;

        // 사용자 생성 및 충전
        userPointTable.insertOrUpdate(userId, initialPoint);

        // when & then
        mockMvc.perform(patch("/point/{id}/use", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(useAmount)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.point").value(initialPoint - useAmount));
    }

    @Test
    @DisplayName("통합 테스트: 포인트 사용 - 최소 금액 1000 미만 실패")
    void 포인트_사용_최소_금액_미만_실패() throws Exception {
        // given
        long userId = 4003L;
        long initialPoint = 10000L;
        long useAmount = 500L; // 최소 금액 1000 미만

        // 사용자 생성 및 충전
        userPointTable.insertOrUpdate(userId, initialPoint);

        // when & then
        mockMvc.perform(patch("/point/{id}/use", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(useAmount)))
                .andExpect(status().isInternalServerError());

        // 포인트가 변경되지 않았는지 확인
        UserPoint userPoint = userPointTable.selectById(userId);
        assertThat(userPoint.point()).isEqualTo(initialPoint);
    }

    @Test
    @DisplayName("통합 테스트: 포인트 사용 - 500 단위가 아닌 경우 실패")
    void 포인트_사용_500단위_아님_실패() throws Exception {
        // given
        long userId = 4004L;
        long initialPoint = 10000L;
        long useAmount = 2300L; // 500 단위 아님

        // 사용자 생성 및 충전
        userPointTable.insertOrUpdate(userId, initialPoint);

        // when & then
        mockMvc.perform(patch("/point/{id}/use", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(useAmount)))
                .andExpect(status().isInternalServerError());

        // 포인트가 변경되지 않았는지 확인
        UserPoint userPoint = userPointTable.selectById(userId);
        assertThat(userPoint.point()).isEqualTo(initialPoint);
    }

    @Test
    @DisplayName("통합 테스트: 포인트 사용 - 보유 포인트 5000 미만 실패")
    void 포인트_사용_보유_포인트_5000_미만_실패() throws Exception {
        // given
        long userId = 4005L;
        long initialPoint = 3000L; // 5000 미만
        long useAmount = 1000L;

        // 사용자 생성 및 충전
        userPointTable.insertOrUpdate(userId, initialPoint);

        // when & then
        mockMvc.perform(patch("/point/{id}/use", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(useAmount)))
                .andExpect(status().isInternalServerError());

        // 포인트가 변경되지 않았는지 확인
        UserPoint userPoint = userPointTable.selectById(userId);
        assertThat(userPoint.point()).isEqualTo(initialPoint);
    }

    @Test
    @DisplayName("통합 테스트: 포인트 사용 - 보유 포인트 5000 경계값 성공")
    void 포인트_사용_보유_포인트_5000_경계값_성공() throws Exception {
        // given
        long userId = 4006L;
        long initialPoint = 5000L; // 정확히 5000
        long useAmount = 2000L;

        // 사용자 생성 및 충전
        userPointTable.insertOrUpdate(userId, initialPoint);

        // when & then
        mockMvc.perform(patch("/point/{id}/use", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(useAmount)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.point").value(initialPoint - useAmount));
    }

    @Test
    @DisplayName("통합 테스트: 포인트 사용 - 사용 금액이 보유 금액 초과 실패")
    void 포인트_사용_잔액_초과_실패() throws Exception {
        // given
        long userId = 4007L;
        long initialPoint = 5000L;
        long useAmount = 10000L; // 보유 금액보다 큼

        // 사용자 생성 및 충전
        userPointTable.insertOrUpdate(userId, initialPoint);

        // when & then
        mockMvc.perform(patch("/point/{id}/use", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(useAmount)))
                .andExpect(status().isInternalServerError());

        // 포인트가 변경되지 않았는지 확인
        UserPoint userPoint = userPointTable.selectById(userId);
        assertThat(userPoint.point()).isEqualTo(initialPoint);
    }

    @Test
    @DisplayName("통합 테스트: 포인트 사용 - 유효하지 않은 ID")
    void 포인트_사용_유효하지_않은_ID() throws Exception {
        // given
        long invalidId = -1L;
        long useAmount = 1000L;

        // when & then
        mockMvc.perform(patch("/point/{id}/use", invalidId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(useAmount)))
                .andExpect(status().isInternalServerError());
    }

    // ========== 통합 시나리오 테스트 ==========

    @Test
    @DisplayName("통합 시나리오: 충전 → 사용 → 조회 전체 플로우")
    void 통합_시나리오_충전_사용_조회() throws Exception {
        // given
        long userId = 5001L;
        long chargeAmount = 10000L;
        long useAmount = 3000L;
        long expectedFinalPoint = chargeAmount - useAmount;

        // 사용자 생성
        userPointTable.insertOrUpdate(userId, 0L);

        // 1. 포인트 충전
        mockMvc.perform(patch("/point/{id}/charge", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(chargeAmount)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.point").value(chargeAmount));

        // 2. 포인트 사용
        mockMvc.perform(patch("/point/{id}/use", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(useAmount)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.point").value(expectedFinalPoint));

        // 3. 포인트 조회
        mockMvc.perform(get("/point/{id}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.point").value(expectedFinalPoint));

        // 4. 포인트 내역 조회
        mockMvc.perform(get("/point/{id}/histories", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].type").value(TransactionType.CHARGE.name()))
                .andExpect(jsonPath("$[0].amount").value(chargeAmount))
                .andExpect(jsonPath("$[1].type").value(TransactionType.USE.name()))
                .andExpect(jsonPath("$[1].amount").value(useAmount));

        // 실제 데이터베이스 확인
        UserPoint userPoint = userPointTable.selectById(userId);
        assertThat(userPoint.point()).isEqualTo(expectedFinalPoint);

        List<PointHistory> histories = pointHistoryTable.selectAllByUserId(userId);
        assertThat(histories).hasSize(2);
    }

    @Test
    @DisplayName("통합 시나리오: 여러 번 충전 → 여러 번 사용 → 조회")
    void 통합_시나리오_여러번_충전_사용() throws Exception {
        // given
        long userId = 5002L;

        // 사용자 생성
        userPointTable.insertOrUpdate(userId, 0L);

        // 1차 충전 5000
        mockMvc.perform(patch("/point/{id}/charge", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("5000"))
                .andExpect(status().isOk());

        // 2차 충전 3000 (총 8000)
        mockMvc.perform(patch("/point/{id}/charge", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("3000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.point").value(8000));

        // 1차 사용 2000 (잔액 6000)
        mockMvc.perform(patch("/point/{id}/use", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("2000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.point").value(6000));

        // 2차 사용 1500 (잔액 4500)
        mockMvc.perform(patch("/point/{id}/use", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("1500"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.point").value(4500));

        // 최종 조회
        mockMvc.perform(get("/point/{id}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.point").value(4500));

        // 내역 조회 (충전 2회 + 사용 2회 = 총 4건)
        mockMvc.perform(get("/point/{id}/histories", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4));

        // 실제 데이터베이스 확인
        UserPoint userPoint = userPointTable.selectById(userId);
        assertThat(userPoint.point()).isEqualTo(4500L);

        List<PointHistory> histories = pointHistoryTable.selectAllByUserId(userId);
        assertThat(histories).hasSize(4);
        assertThat(histories.stream().filter(h -> h.type() == TransactionType.CHARGE).count()).isEqualTo(2);
        assertThat(histories.stream().filter(h -> h.type() == TransactionType.USE).count()).isEqualTo(2);
    }
}