package com.qianxun.homestay.modules.portrait.mapper;

import com.qianxun.homestay.modules.portrait.dto.BookerPortraitListQuery;
import com.qianxun.homestay.modules.portrait.entity.BookerPortraitListEntity;
import com.qianxun.homestay.modules.portrait.entity.BookerPortraitOrderEntity;
import com.qianxun.homestay.modules.portrait.entity.BookerProfileEntity;
import com.qianxun.homestay.modules.portrait.entity.PortraitTagEntity;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 按手机号访问顾客画像及其订单备注的数据访问接口。 */
@Mapper
public interface BookerPortraitMapper {

    /** 按手机号散列键读取画像。 */
    BookerProfileEntity findProfileByIdentityKey(@Param("identityKey") String identityKey);

    /** 插入画像；身份键冲突时复用已有记录。 */
    int upsertProfile(BookerProfileEntity profile);

    /** 按画像主键读取画像。 */
    BookerProfileEntity findProfileById(@Param("id") Long id);

    /** 锁定画像以合并订单补充资料，避免并发写入丢失。 */
    BookerProfileEntity findProfileByIdForUpdate(@Param("id") Long id);

    /** 根据订单当前手机号读取画像主键，无手机号时按订单主键关联。 */
    Long findProfileIdByOrderId(@Param("orderId") Long orderId);

    /** 锁定并确认订单属于当前画像。 */
    Long lockOrderIdForProfile(@Param("profileId") Long profileId, @Param("orderId") Long orderId);

    /** 删除转为手机号画像后不再需要的无手机号订单画像。 */
    int deleteProfile(@Param("id") Long id);

    /** 更新画像属性、JSON 标签和画像级管理员备注。 */
    int updateProfile(BookerProfileEntity profile);

    /** 从订单表单更新画像字段，不覆盖手机号画像级管理员备注。 */
    int updateProfileFromOrder(BookerProfileEntity profile);

    /** 仅更新属于指定画像的订单备注。 */
    int updateOrderRemarks(
            @Param("orderId") Long orderId,
            @Param("remark") String remark,
            @Param("adminRemark") String adminRemark);

    /** 分页读取手机号画像及会员、已完成订单聚合数据。 */
    List<BookerPortraitListEntity> findProfilePage(
            @Param("query") BookerPortraitListQuery query, @Param("offset") long offset);

    /** 统计符合筛选条件且至少关联一笔订单的画像。 */
    long countProfiles(@Param("query") BookerPortraitListQuery query);

    /** 查询画像关联的已完成订单和两类备注。 */
    List<BookerPortraitOrderEntity> findCompletedOrders(@Param("profileIds") List<Long> profileIds);

    /** 查询画像关联的全部订单备注，供持续编辑。 */
    List<BookerPortraitOrderEntity> findPortraitOrders(@Param("profileIds") List<Long> profileIds);

    /** 查询所有启用的画像标签选项。 */
    List<PortraitTagEntity> findPortraitTags();

    /** 按名称读取标签选项，名称比较遵从数据库大小写不敏感排序规则。 */
    PortraitTagEntity findPortraitTagByName(@Param("name") String name);

    /** 新增共享标签；同名重复提交时复用原标签。 */
    int upsertPortraitTag(@Param("name") String name);
}
