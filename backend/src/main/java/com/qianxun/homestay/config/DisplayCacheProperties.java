package com.qianxun.homestay.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.AssertTrue;

/** 展示缓存参数，集中管理房间、商品和签名链接的有效期及分页范围。 */
@Component
@Validated
@ConfigurationProperties(prefix = "app.display-cache")
public class DisplayCacheProperties {
    private Duration roomTtl = Duration.ofMinutes(2);
    private Duration productTtl = Duration.ofMinutes(2);
    private Duration pageTtl = Duration.ofSeconds(30);
    private Duration imageTtl = Duration.ofMinutes(50);
    private int pageSize = 20;
    private int maxPages = 5;
    /** 校验有效期和分页边界，签名缓存必须比一小时签名有效期短至少五分钟。 */
    @AssertTrue(message = "展示缓存配置不合法")
    public boolean isValid() {
        return positive(roomTtl) && positive(productTtl) && positive(pageTtl) && positive(imageTtl)
                && imageTtl.compareTo(Duration.ofMinutes(55)) <= 0 && pageSize > 0 && pageSize <= 100 && maxPages > 0;
    }
    private boolean positive(Duration value) { return value != null && !value.isNegative() && !value.isZero(); }
    public Duration getRoomTtl() { return roomTtl; }
    public void setRoomTtl(Duration value) { roomTtl = value; }
    public Duration getProductTtl() { return productTtl; }
    public void setProductTtl(Duration value) { productTtl = value; }
    public Duration getPageTtl() { return pageTtl; }
    public void setPageTtl(Duration value) { pageTtl = value; }
    public Duration getImageTtl() { return imageTtl; }
    public void setImageTtl(Duration value) { imageTtl = value; }
    public int getPageSize() { return pageSize; }
    public void setPageSize(int value) { pageSize = value; }
    public int getMaxPages() { return maxPages; }
    public void setMaxPages(int value) { maxPages = value; }
}
