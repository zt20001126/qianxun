package com.qianxun.homestay.modules.invitation;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.DriverManager;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** 验证已有邀请关系从 V18 升级时不会丢失或补发奖励。 */
@Testcontainers(disabledWithoutDocker = true)
class InvitationRewardMigrationTest {
    @Container static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("invitation_migration_test").withUsername("test").withPassword("test");

    @Test void preservesExistingInvitationDuringUpgrade() throws Exception {
        Flyway.configure().dataSource(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword())
                .locations("classpath:db/migration").target("18").load().migrate();
        try (var connection = DriverManager.getConnection(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword());
             var statement = connection.createStatement()) {
            statement.executeUpdate("INSERT INTO members(id,phone) VALUES (101,'13800000001'),(102,'13800000002')");
            statement.executeUpdate("INSERT INTO member_invitation(inviter_member_id,invitee_member_id,created_at,updated_at) VALUES (101,102,NOW(),NOW())");
        }
        Flyway.configure().dataSource(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword())
                .locations("classpath:db/migration").load().migrate();
        try (var connection = DriverManager.getConnection(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword());
             var statement = connection.createStatement();
             var result = statement.executeQuery("SELECT inviter_member_id,invitee_member_id,reward_order_id,reward_amount,reward_issued FROM member_invitation")) {
            assertThat(result.next()).isTrue();
            assertThat(result.getLong("inviter_member_id")).isEqualTo(101);
            assertThat(result.getLong("invitee_member_id")).isEqualTo(102);
            assertThat(result.getObject("reward_order_id")).isNull();
            assertThat(result.getObject("reward_amount")).isNull();
            assertThat(result.getBoolean("reward_issued")).isFalse();
            assertThat(result.next()).isFalse();
        }
    }
}
