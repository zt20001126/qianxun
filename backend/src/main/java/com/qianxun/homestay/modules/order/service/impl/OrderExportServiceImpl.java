package com.qianxun.homestay.modules.order.service.impl;

import com.qianxun.homestay.common.util.ApiTime;
import com.qianxun.homestay.modules.order.entity.OrderViewEntity;
import com.qianxun.homestay.modules.order.mapper.OrderMapper;
import com.qianxun.homestay.modules.order.service.OrderExportService;
import com.qianxun.homestay.modules.order.enums.OrderStatus;
import com.qianxun.homestay.modules.portrait.service.BookerPortraitService;
import com.qianxun.homestay.modules.portrait.vo.BookerPortraitVO;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 生成包含全部订单管理字段的单工作表 Excel 文件。 */
@Service
public class OrderExportServiceImpl implements OrderExportService {

    private static final List<String> HEADERS = List.of(
            "订单ID", "订单编号", "订单来源ID", "订单来源", "达人ID", "达人/主播", "房间ID", "房号", "房间名称",
            "入住日期", "退房日期", "住宿晚数", "预订人", "预订手机号", "入住人数", "实付金额（元）",
            "平台手续费（元）", "平台达人提成（元）", "达人额外提成（元）", "预期收入（元）", "商家到手（元）",
            "订单状态", "第三方平台订单号", "第三方平台原始下单日期", "下单会员ID", "预订人下单备注",
            "管理员备注", "画像ID", "画像年龄", "出行类型", "推断地区", "画像标签", "系统创建时间", "最后更新时间");
    private static final Map<String, String> PARTY_TYPE_LABELS = Map.of(
            "SOLO", "独自出行", "COUPLE", "情侣/夫妻", "PARENT_CHILD", "亲子家庭", "FAMILY_FRIENDS", "家庭/亲友",
            "FRIENDS", "朋友结伴", "BUSINESS", "商务/差旅", "GROUP", "团队/团体", "OTHER", "其他");
    private static final Map<OrderStatus, String> STATUS_LABELS = Map.of(
            OrderStatus.PENDING_CONFIRM, "已预订（待确认）", OrderStatus.PENDING_CHECKIN, "待入住",
            OrderStatus.CHECKED_IN, "已入住", OrderStatus.COMPLETED, "已退房（已完成）",
            OrderStatus.CANCELLED, "已取消");

    private final OrderMapper orderMapper;
    private final BookerPortraitService bookerPortraitService;

    public OrderExportServiceImpl(OrderMapper orderMapper, BookerPortraitService bookerPortraitService) {
        this.orderMapper = orderMapper;
        this.bookerPortraitService = bookerPortraitService;
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportAllOrders() {
        List<OrderViewEntity> orders = orderMapper.findAllForExport();
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("订单管理");
            Row header = sheet.createRow(0);
            for (int i = 0; i < HEADERS.size(); i++) {
                header.createCell(i).setCellValue(HEADERS.get(i));
            }
            int rowIndex = 1;
            for (OrderViewEntity order : orders) {
                writeOrder(sheet.createRow(rowIndex++), order);
            }
            sheet.createFreezePane(0, 1);
            sheet.setAutoFilter(new CellRangeAddress(0, orders.size(), 0, HEADERS.size() - 1));
            for (int i = 0; i < HEADERS.size(); i++) {
                sheet.setColumnWidth(i, Math.min(60, Math.max(14, HEADERS.get(i).length() * 2 + 4)) * 256);
            }
            workbook.write(output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("生成订单 Excel 文件失败", exception);
        }
    }

    private void writeOrder(Row row, OrderViewEntity order) {
        BookerPortraitVO portrait = bookerPortraitService.findForOrder(order.getId());
        int col = 0;
        text(row, col++, order.getId());
        text(row, col++, order.getOrderNo());
        text(row, col++, order.getChannelId());
        text(row, col++, order.getChannelName());
        text(row, col++, order.getCreatorId());
        text(row, col++, order.getCreatorName());
        text(row, col++, order.getRoomId());
        text(row, col++, order.getRoomNumber());
        text(row, col++, order.getRoomName());
        text(row, col++, order.getCheckIn());
        text(row, col++, order.getCheckOut());
        text(row, col++, order.getNights());
        text(row, col++, order.getGuestName());
        text(row, col++, order.getPhone());
        text(row, col++, order.getGuestCount());
        text(row, col++, order.getPaidAmount());
        text(row, col++, order.getPlatformFee());
        text(row, col++, order.getPlatformCommission());
        text(row, col++, order.getExtraCommission());
        text(row, col++, order.getExpectedAmount());
        text(row, col++, order.getNetAmount());
        text(row, col++, STATUS_LABELS.get(order.getStatus()));
        text(row, col++, order.getPlatformOrderNo());
        text(row, col++, order.getPlatformOrderDate());
        text(row, col++, order.getMemberId());
        text(row, col++, order.getRemark());
        text(row, col++, order.getAdminRemark());
        text(row, col++, portrait == null ? null : portrait.id());
        text(row, col++, portrait == null ? null : portrait.age());
        text(row, col++, portrait == null || portrait.partyType() == null ? null
                : PARTY_TYPE_LABELS.getOrDefault(portrait.partyType(), portrait.partyType()));
        text(row, col++, portrait == null ? null : portrait.inferredRegion());
        text(row, col++, portrait == null || portrait.tags() == null ? null : String.join("、", portrait.tags()));
        text(row, col++, order.getCreatedAt() == null ? null : ApiTime.fromShanghai(order.getCreatedAt()));
        text(row, col, order.getUpdatedAt() == null ? null : ApiTime.fromShanghai(order.getUpdatedAt()));
    }

    private void text(Row row, int column, Object value) {
        if (value != null) {
            row.createCell(column).setCellValue(value.toString());
        }
    }
}
