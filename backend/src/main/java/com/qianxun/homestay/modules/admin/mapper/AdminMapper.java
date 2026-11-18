package com.qianxun.homestay.modules.admin.mapper;

import com.qianxun.homestay.modules.admin.entity.AdminEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 管理员数据库访问入口，所有 SQL 由 admin XML 映射文件维护。
 */
@Mapper
public interface AdminMapper {

    /**
     * 按后台用户名读取账号及 BCrypt 哈希。
     *
     * @param username 后台登录名
     * @return 管理员实体；不存在时为空
     */
    AdminEntity findByUsername(@Param("username") String username);

    /**
     * 按 JWT 中的管理员 ID 读取当前账号。
     *
     * @param id 管理员 ID
     * @return 管理员实体；不存在时为空
     */
    AdminEntity findById(@Param("id") Long id);

    /**
     * 统计管理员账号数，用于首次安全初始化。
     *
     * @return 当前账号数
     */
    long countAdmins();

    /**
     * 插入首次初始化的管理员 BCrypt 哈希。
     *
     * @param admin 管理员资料
     * @return 插入行数
     */
    int insert(AdminEntity admin);
}
