package com.qianxun.homestay.modules.notification.config;

import java.util.Arrays;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** 管理订单邮件收件人配置，影响支付及退款后的管理员通知；收件人由部署环境注入。 */
@ConfigurationProperties(prefix = "app.mail-notification")
public class MailNotificationProperties {
    /** 英文逗号分隔的管理员收件人邮箱。 */
    private String recipients = "";
    /** 邮件中展示的发件人名称。 */
    private String senderName = "芊浔民宿";
    /** SMTP 发件邮箱地址；为空时使用 SMTP 登录账号。 */
    private String fromEmail = "";
    public String getRecipients() { return recipients; }
    public void setRecipients(String recipients) { this.recipients = recipients; }
    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }
    public String getFromEmail() { return fromEmail; }
    public void setFromEmail(String fromEmail) { this.fromEmail = fromEmail; }
    /** 返回已去除空白和空项的收件人列表。 */
    public List<String> recipientList() {
        return Arrays.stream(recipients.split(",")).map(String::trim).filter(value -> !value.isEmpty()).distinct().toList();
    }
}
