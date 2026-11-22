package com.qianxun.homestay.modules.order.service;

/** 后台订单 Excel 导出服务。 */
public interface OrderExportService {

    /** 导出全部订单及后台订单界面中的完整字段。 */
    byte[] exportAllOrders();
}
