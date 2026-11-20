# V3.2 微信支付与退款测试计划

## Feature scope

验证微信支付开关、微信身份绑定、JSAPI 预支付、支付/退款通知处理、真实退款状态流转及过期支付对账。模拟支付继续可用，关闭开关时仅模拟支付，开启时仅微信支付。测试使用 MySQL 8 Testcontainers 和本地 HTTP stub；不连接真实微信商户、真实密钥或公网服务。

## Module breakdown

| 模块 | 覆盖边界 |
| --- | --- |
| 微信身份 | code2session、access_token、手机号授权、OpenID 唯一绑定、手机号匹配和冲突 |
| 支付 | 开关、订单归属、锁房状态、预支付响应、回调字段核验、金额/付款人核对、幂等结算 |
| 通知 API | 支付与退款回调 HTTP 路由、签名头和原始 body 传递、必需请求头校验 |
| 退款 | 原渠道路由、退款 pending/成功/失败、相同商户退款号重试、积分和订单状态一致性 |
| 超时对账 | 微信关单成功、关单失败后查询、已支付结算、未知状态保留锁房 |
| 配置 | 启用配置完整性、HTTPS 回调地址、安全 URL 结构校验 |

## Test objectives

- 确保支付模式开关不会让模拟和真实支付同时可用。
- 确保微信提供的订单、金额、币种、交易类型、付款人和时间字段经过校验后才进入账务事务。
- 确保 OpenID 只能绑定到同手机号会员，且唯一性冲突不会误绑。
- 确保退款平台状态未确认前不取消订单、不返还积分；重复调用和回调不会重复记账。
- 确保微信状态不明时不释放锁房和积分预占。
- 验证 API 的 HTTP 状态码、请求校验、授权和回包形状。

## Test strategy

### Unit Test

使用 JUnit 5、Mockito 和 Spring `MockRestServiceServer` 测试通知字段映射、身份服务外部 HTTP 响应、OpenID 绑定边界、退款 SDK 响应状态及配置校验。

### API Test

测试面包括 `POST /api/app/payments/{paymentNo}/wechat-prepay`、`POST /api/app/orders/{id}/refunds`、`GET /api/app/orders/{id}/refunds/latest`、`POST /api/payment/wechat/notify/payment` 和 `POST /api/payment/wechat/notify/refund`。使用 MockMvc 验证状态码、JSON、身份权限和原始通知内容传递。

### Integration Test

使用 MySQL 8 Testcontainers 验证 Flyway V1–V10、支付/退款数据库事务、积分账本、并发支付与锁房、退款等待和超时对账。本地 HTTP stub 模拟微信身份 API；微信支付 SDK 网络接口用 Mockito 隔离。

### Boundary Test

覆盖空通知、缺少必需字段、无效成功时间、错误币种/交易类型、授权码长度边界、退款处理中、支付已过期及微信状态未知。

### Negative Test

覆盖错误 AppID/商户号、错误 OpenID/金额、无效签名解析异常、手机号不匹配、账号重复绑定、未登录、其他会员订单、关闭开关调用微信预支付、退款请求失败及网络结果未知。

### Idempotency Test

覆盖重复模拟支付、重复微信支付通知、重复微信退款完成通知、退款请求重复提交和超时关单重复扫描，核对只生成一笔订单/退款/积分流水。

## Run command

```powershell
mvn -f backend/pom.xml test
```

运行 MySQL 集成测试需要本机 Docker 可用。该测试命令不向微信发起真实请求。
