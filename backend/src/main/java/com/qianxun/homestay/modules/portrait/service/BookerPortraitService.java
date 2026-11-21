package com.qianxun.homestay.modules.portrait.service;

import com.qianxun.homestay.modules.portrait.dto.BookerPortraitListQuery;
import com.qianxun.homestay.modules.portrait.dto.BookerPortraitSaveRequest;
import com.qianxun.homestay.modules.portrait.dto.BookerPortraitUpdateRequest;
import com.qianxun.homestay.modules.portrait.dto.PortraitTagCreateRequest;
import com.qianxun.homestay.modules.portrait.vo.BookerPortraitPageVO;
import com.qianxun.homestay.modules.portrait.vo.PortraitTagVO;
import com.qianxun.homestay.modules.portrait.vo.BookerPortraitVO;
import java.util.List;

/** 按手机号维护顾客画像与订单备注的业务服务。 */
public interface BookerPortraitService {

    /**
     * 为订单建立或复用按手机号识别的画像关联；手机号缺失时按订单单独建立。
     *
     * @param orderId 订单主键
     * @param guestName 预订人姓名
     * @param phone 预订人手机号
     */
    void ensureProfileForOrder(Long orderId, String guestName, String phone);

    /**
     * 订单联系人发生变化时按新手机号切换画像关联。
     *
     * @param orderId 订单主键
     * @param guestName 新预订人姓名
     * @param phone 新预订人手机号
     */
    void reassignOrder(Long orderId, String guestName, String phone);

    /**
     * 保存订单对应画像资料。
     *
     * @param orderId 订单主键
     * @param request 画像字段和所选标签
     */
    void saveForOrder(Long orderId, BookerPortraitSaveRequest request);

    /**
     * 读取订单关联画像，供后台订单详情和入住表单回显。
     *
     * @param orderId 订单主键
     * @return 当前画像；未建立关联时返回空
     */
    BookerPortraitVO findForOrder(Long orderId);

    /**
     * 分页查询所有顾客画像及已完成订单统计。
     *
     * @param query 姓名、手机号和分页条件
     * @return 画像列表和分页信息
     */
    BookerPortraitPageVO findProfiles(BookerPortraitListQuery query);

    /**
     * 修改顾客画像和关联订单备注；允许画像关联订单处于任意状态。
     *
     * @param profileId 画像主键
     * @param request 画像字段、画像管理员备注和订单备注
     */
    void updateProfile(Long profileId, BookerPortraitUpdateRequest request);

    /** 查询后台可供画像选择的标签选项。 */
    List<PortraitTagVO> findPortraitTags();

    /** 新增全局可复用的画像标签；同名提交会复用已有标签。
     *
     * @param request 标签名称
     * @return 保存后的标签选项
     */
    PortraitTagVO createPortraitTag(PortraitTagCreateRequest request);
}
