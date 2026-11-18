package com.qianxun.homestay.modules.admin.entity;

import java.time.LocalDateTime;

/**
 * 管理员持久化实体；密码哈希只在后端认证和初始化流程中使用。
 */
public class AdminEntity {

    /** 管理员数据库主键，也是管理员 JWT 身份标识。 */
    private Long id;
    /** 后台登录用户名。 */
    private String username;
    /** BCrypt 密码哈希，绝不映射到接口响应。 */
    private String passwordHash;
    /** 管理员账号是否允许登录。 */
    private Boolean enabled;
    /** 管理员记录创建时间。 */
    private LocalDateTime createdAt;
    /** 管理员记录更新时间。 */
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
