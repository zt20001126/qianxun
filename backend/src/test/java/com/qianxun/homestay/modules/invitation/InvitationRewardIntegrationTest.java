package com.qianxun.homestay.modules.invitation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qianxun.homestay.modules.invitation.service.InvitationService;
import com.qianxun.homestay.modules.invitation.mapper.InvitationMapper;
import java.time.LocalDateTime;
import org.springframework.test.util.AopTestUtils;
import org.springframework.test.util.ReflectionTestUtils;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.RepeatedTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** 使用真实 MySQL 验证邀请首单、事务、接口权限与并发退房。 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = {"app.jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
        "app.auth.default-avatar=/assets/avatar-default.png"})
@AutoConfigureMockMvc
class InvitationRewardIntegrationTest {
    @Container static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("invitation_reward_test").withUsername("test").withPassword("test");
    @DynamicPropertySource static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
    }
    @Autowired JdbcTemplate db;
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired PasswordEncoder encoder;
    @Autowired InvitationService invitationService;
    @Autowired InvitationMapper invitationMapper;
    private long inviter;
    private long invitee;
    private long relation;
    private long room1;
    private long room2;
    private String admin;
    private String member;

    @BeforeEach void setup() throws Exception {
        // 使用固定边界覆盖历史时间样本，自动生效边界另行验证。
        ReflectionTestUtils.setField((Object) AopTestUtils.getTargetObject(invitationService), "effectiveAt",
                LocalDateTime.of(2025, 1, 1, 0, 0));
        db.update("DELETE FROM points_log");
        db.update("DELETE FROM member_invitation");
        db.update("DELETE FROM payments");
        db.update("DELETE FROM order_amount");
        db.update("DELETE FROM orders");
        db.update("DELETE FROM booking_lock");
        db.update("DELETE FROM room");
        db.update("DELETE FROM members");
        db.update("DELETE FROM admin");
        db.update("INSERT INTO admin(username,password_hash,enabled) VALUES ('reward-admin',?,1)", encoder.encode("reward-pass"));
        db.update("INSERT INTO members(phone,nickname) VALUES ('13800000001','0001'),('13800000002','0002')");
        inviter = db.queryForObject("SELECT id FROM members WHERE phone='13800000001'", Long.class);
        invitee = db.queryForObject("SELECT id FROM members WHERE phone='13800000002'", Long.class);
        db.update("INSERT INTO member_invitation(inviter_member_id,invitee_member_id,created_at,updated_at) VALUES (?,?, '2024-12-01 00:00:00','2024-12-01 00:00:00')", inviter, invitee);
        relation = db.queryForObject("SELECT id FROM member_invitation WHERE invitee_member_id=?", Long.class, invitee);
        db.update("INSERT INTO room(number,name,floor,price,capacity,enabled) VALUES ('101','测试房一',1,100,2,1),('102','测试房二',1,100,2,1)");
        room1 = db.queryForObject("SELECT id FROM room WHERE number='101'", Long.class);
        room2 = db.queryForObject("SELECT id FROM room WHERE number='102'", Long.class);
        admin = token("/api/admin/auth/login", "{\"username\":\"reward-admin\",\"password\":\"reward-pass\"}");
        member = token("/api/app/auth/phone-login", "{\"phone\":\"13800000001\"}");
    }

    @Test void automaticEffectiveTimeIsPersistedAndShared() throws Exception {
        LocalDateTime effective = invitationMapper.findRewardEffectiveAt();
        assertThat(effective).isNotNull();
        assertThat(invitationMapper.findRewardEffectiveAt()).isEqualTo(effective);
        var restarted = new com.qianxun.homestay.modules.invitation.service.impl.InvitationServiceImpl(invitationMapper);
        assertThat(ReflectionTestUtils.getField(restarted, "effectiveAt")).isEqualTo(effective);
        assertThat(effective).isAfter(LocalDateTime.now(java.time.ZoneId.of("Asia/Shanghai")).minusMinutes(5));
        assertThat(effective).isBefore(LocalDateTime.now(java.time.ZoneId.of("Asia/Shanghai")).plusMinutes(1));
        ReflectionTestUtils.setField((Object) AopTestUtils.getTargetObject(invitationService), "effectiveAt", effective);
        long before = order("CHECKED_IN", "100.00", effective.minusSeconds(1).toString().replace('T', ' '), room1);
        checkout(before).andExpect(status().isOk());
        assertThat(db.queryForObject("SELECT reward_order_id FROM member_invitation WHERE id=?", Long.class, relation)).isNull();
        long after = order("CHECKED_IN", "100.00", effective.plusSeconds(1).toString().replace('T', ' '), room2);
        checkout(after).andExpect(status().isOk());
        assertThat(db.queryForObject("SELECT reward_order_id FROM member_invitation WHERE id=?", Long.class, relation)).isEqualTo(after);
    }

    @Test void completedFirstEligibleOrderAndManualIssuedStatus() throws Exception {
        long cancelled = order("CANCELLED", "30.00", "2025-02-01 00:00:00", room1);
        long first = order("CHECKED_IN", "123.45", "2025-02-02 00:00:00", room1);
        long second = order("CHECKED_IN", "900.00", "2025-02-03 00:00:00", room2);
        mvc.perform(get("/api/app/invitations").header("Authorization", bearer(member)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.list[0].has_completed_order").value(false));
        checkout(first).andExpect(status().isOk());
        checkout(first).andExpect(status().isConflict());
        checkout(second).andExpect(status().isOk());
        assertThat(db.queryForObject("SELECT reward_order_id FROM member_invitation WHERE id=?", Long.class, relation)).isEqualTo(first);
        assertThat(db.queryForObject("SELECT reward_amount FROM member_invitation WHERE id=?", BigDecimal.class, relation)).isEqualByComparingTo("12.35");
        mvc.perform(get("/api/app/invitations").header("Authorization", bearer(member)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.list[0].has_completed_order").value(true))
                .andExpect(jsonPath("$.data.list[0].reward_amount").doesNotExist());
        mvc.perform(get("/api/admin/invitations").header("Authorization", bearer(member))).andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/invitations").header("Authorization", bearer(admin))).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.list[0].paid_amount").value("123.45"))
                .andExpect(jsonPath("$.data.list[0].reward_amount").value("12.35"))
                .andExpect(jsonPath("$.data.list[0].reward_issued").value(false));
        mark(true).andExpect(status().isOk());
        mark(true).andExpect(status().isOk());
        mark(false).andExpect(status().isUnprocessableEntity());
        assertThat(db.queryForObject("SELECT reward_issued FROM member_invitation WHERE id=?", Boolean.class, relation)).isTrue();
        assertThat(cancelled).isNotEqualTo(first);
    }

    @Test void adminFiltersBothPhoneRolesAndRewardStatusAcrossPages() throws Exception {
        db.update("INSERT INTO members(phone,nickname) VALUES ('13911110003','0003'),('13922220004','0004')");
        long otherInviter = db.queryForObject("SELECT id FROM members WHERE phone='13911110003'", Long.class);
        long otherInvitee = db.queryForObject("SELECT id FROM members WHERE phone='13922220004'", Long.class);
        db.update("INSERT INTO member_invitation(inviter_member_id,invitee_member_id,created_at,updated_at) VALUES (?,?,NOW(),NOW())", otherInviter, otherInvitee);
        long target = order("CHECKED_IN", "100.00", "2025-02-01 00:00:00", room1);
        checkout(target).andExpect(status().isOk());
        mvc.perform(get("/api/admin/invitations").header("Authorization", bearer(admin)).param("phone", "13800000001"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.total").value(1));
        mvc.perform(get("/api/admin/invitations").header("Authorization", bearer(admin)).param("phone", "00000002").param("reward_status", "UNISSUED"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.total").value(1));
        mvc.perform(get("/api/admin/invitations").header("Authorization", bearer(admin)).param("phone", "1392222").param("reward_status", "NO_REWARD"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.total").value(1));
        mvc.perform(get("/api/admin/invitations").header("Authorization", bearer(admin)).param("phone", "1391111").param("reward_status", "UNISSUED"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.total").value(0));
        mvc.perform(get("/api/admin/invitations").header("Authorization", bearer(admin)).param("size", "1").param("page", "2"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.total").value(2)).andExpect(jsonPath("$.data.list.length()").value(1));
        mvc.perform(get("/api/admin/invitations").header("Authorization", bearer(admin)).param("phone", "%"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/admin/invitations").header("Authorization", bearer(admin)).param("reward_status", "INVALID"))
                .andExpect(status().isBadRequest());
        mark(true).andExpect(status().isOk());
        mvc.perform(get("/api/admin/invitations").header("Authorization", bearer(admin)).param("reward_status", "ISSUED"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.total").value(1));
        mvc.perform(get("/api/admin/invitations").header("Authorization", bearer(admin)).param("reward_status", "UNISSUED"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.total").value(0));
        mark(false).andExpect(status().isUnprocessableEntity());
        assertThat(db.queryForObject("SELECT reward_issued FROM member_invitation WHERE id=?", Boolean.class, relation)).isTrue();
    }

    @Test void paymentBoundariesZeroAndInvalidMark() throws Exception {
        mark(true).andExpect(status().isUnprocessableEntity());
        long old = order("CHECKED_IN", "100.00", "2024-12-31 23:59:59", room1);
        checkout(old).andExpect(status().isOk());
        assertThat(db.queryForObject("SELECT reward_order_id FROM member_invitation WHERE id=?", Long.class, relation)).isNull();
        long zero = order("CHECKED_IN", "0.00", "2025-01-01 00:00:00", room2);
        checkout(zero).andExpect(status().isOk());
        assertThat(db.queryForObject("SELECT reward_order_id FROM member_invitation WHERE id=?", Long.class, relation)).isEqualTo(zero);
        assertThat(db.queryForObject("SELECT reward_amount FROM member_invitation WHERE id=?", BigDecimal.class, relation)).isEqualByComparingTo("0.00");
    }

    @Test void paymentBeforeBindingOrFailedPaymentDoesNotConsumeOpportunity() throws Exception {
        db.update("UPDATE member_invitation SET created_at='2025-02-01 00:00:00' WHERE id=?", relation);
        long before = order("CHECKED_IN", "100.00", "2025-01-31 23:59:59", room1);
        checkout(before).andExpect(status().isOk());
        long equalTime = order("CHECKED_IN", "90.00", "2025-02-01 00:00:00", room1);
        checkout(equalTime).andExpect(status().isOk());
        long failed = order("CHECKED_IN", "80.00", "2025-02-02 00:00:00", room2);
        db.update("UPDATE payments SET status='FAILED' WHERE order_id=?", failed);
        checkout(failed).andExpect(status().isOk());
        assertThat(db.queryForObject("SELECT reward_order_id FROM member_invitation WHERE id=?", Long.class, relation)).isNull();
        long eligible = order("CHECKED_IN", "50.00", "2025-02-03 00:00:00", room1);
        checkout(eligible).andExpect(status().isOk());
        assertThat(db.queryForObject("SELECT reward_order_id FROM member_invitation WHERE id=?", Long.class, relation)).isEqualTo(eligible);
        mvc.perform(put("/api/admin/invitations/{id}/reward-issued", relation).header("Authorization", bearer(member))
                .contentType("application/json").content("{\"reward_issued\":true}")).andExpect(status().isForbidden());
        mvc.perform(put("/api/admin/invitations/{id}/reward-issued", relation).header("Authorization", bearer(admin))
                .contentType("application/json").content("{}")).andExpect(status().isBadRequest());
    }

    @Test void databaseRejectsOrphanIssuedFlagAndDuplicateRewardOrder() throws Exception {
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> db.update(
                "UPDATE member_invitation SET reward_issued=1 WHERE id=?", relation))
                .isInstanceOf(org.springframework.dao.DataAccessException.class);
        long target = order("CHECKED_IN", "100.00", "2025-02-01 00:00:00", room1);
        checkout(target).andExpect(status().isOk());
        db.update("INSERT INTO members(phone,nickname) VALUES ('13800000003','0003')");
        long other = db.queryForObject("SELECT id FROM members WHERE phone='13800000003'", Long.class);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> db.update(
                "INSERT INTO member_invitation(inviter_member_id,invitee_member_id,created_at,updated_at,reward_order_id,reward_amount) VALUES (?,?,NOW(),NOW(),?,10)",
                inviter, other, target)).isInstanceOf(org.springframework.dao.DataAccessException.class);
    }

    @Test void rewardWriteFailureRollsBackCheckoutAndPoints() throws Exception {
        long target = order("CHECKED_IN", "100.00", "2025-02-01 00:00:00", room1);
        db.execute("ALTER TABLE member_invitation ADD CONSTRAINT chk_test_zero CHECK (reward_amount IS NULL OR reward_amount = 0)");
        try {
            checkout(target).andExpect(status().isInternalServerError());
            assertThat(db.queryForObject("SELECT status FROM orders WHERE id=?", String.class, target)).isEqualTo("CHECKED_IN");
            assertThat(db.queryForObject("SELECT COUNT(*) FROM points_log WHERE order_id=?", Integer.class, target)).isZero();
            assertThat(db.queryForObject("SELECT reward_order_id FROM member_invitation WHERE id=?", Long.class, relation)).isNull();
        } finally { db.execute("ALTER TABLE member_invitation DROP CHECK chk_test_zero"); }
    }

    @RepeatedTest(5) void twoSimultaneousCompletionsChooseExactlyOne() throws Exception {
        long one = order("CHECKED_IN", "101.00", "2025-02-01 00:00:00", room1);
        long two = order("CHECKED_IN", "202.00", "2025-02-01 00:00:00", room2);
        CountDownLatch gate = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            List<Callable<Integer>> calls = List.of(
                    () -> { gate.await(); return checkout(one).andReturn().getResponse().getStatus(); },
                    () -> { gate.await(); return checkout(two).andReturn().getResponse().getStatus(); });
            var first = executor.submit(calls.get(0)); var second = executor.submit(calls.get(1)); gate.countDown();
            assertThat(first.get(30, TimeUnit.SECONDS)).isEqualTo(200);
            assertThat(second.get(30, TimeUnit.SECONDS)).isEqualTo(200);
        }
        Long winner = db.queryForObject("SELECT reward_order_id FROM member_invitation WHERE id=?", Long.class, relation);
        assertThat(winner).isIn(one, two);
        BigDecimal reward = db.queryForObject("SELECT reward_amount FROM member_invitation WHERE id=?", BigDecimal.class, relation);
        assertThat(reward).isEqualByComparingTo(winner == one ? "10.10" : "20.20");
    }

    private long order(String status, String amount, String paidAt, long room) {
        String key = java.util.UUID.randomUUID().toString().substring(0, 12);
        db.update("INSERT INTO orders(order_no,room_id,channel_id,member_id,check_in,check_out,status) VALUES (?,?,1,?,'2024-01-01','2024-01-02',?)", key, room, invitee, status);
        long id = db.queryForObject("SELECT id FROM orders WHERE order_no=?", Long.class, key);
        db.update("INSERT INTO order_amount(order_id,paid_amount) VALUES (?,?)", id, amount);
        db.update("INSERT INTO booking_lock(member_id,room_id,check_in,check_out,guest_name,phone,guest_count,room_amount,amount,status,expires_at) VALUES (?,?,'2024-01-01','2024-01-02','测试','13800000002',1,?,?,'PAID','2025-01-01')", invitee, room, amount, amount);
        long booking = db.queryForObject("SELECT MAX(id) FROM booking_lock", Long.class);
        db.update("INSERT INTO payments(booking_id,order_id,payment_no,amount,status,paid_at) VALUES (?,?,?,?,'SUCCESS',?)", booking, id, key, amount, paidAt);
        return id;
    }
    private org.springframework.test.web.servlet.ResultActions checkout(long id) throws Exception {
        return mvc.perform(post("/api/admin/orders/{id}/check-out", id).header("Authorization", bearer(admin)));
    }
    private org.springframework.test.web.servlet.ResultActions mark(boolean issued) throws Exception {
        return mvc.perform(put("/api/admin/invitations/{id}/reward-issued", relation).header("Authorization", bearer(admin))
                .contentType("application/json").content("{\"reward_issued\":" + issued + "}"));
    }
    private String token(String url, String body) throws Exception {
        String response = mvc.perform(post(url).contentType("application/json").content(body)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(response).path("data").path("token").asText();
    }
    private String bearer(String token) { return "Bearer " + token; }
}
