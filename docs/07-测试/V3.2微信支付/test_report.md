# V3.2 微信支付与退款测试报告

执行日期：2026-11-20（Asia/Shanghai）

## 测试执行结果

| 范围 | 命令 | 结果 |
| --- | --- | --- |
| 后端全量 | `mvn -f backend/pom.xml test` | 99 tests，99 passed，0 failures，0 errors，0 skipped；Flyway V1–V10 在 MySQL 8 Testcontainers 执行 |
| 微信专项 | `mvn -f backend/pom.xml "-Dtest=WechatPaymentServiceImplTest,WechatPayNotificationControllerTest,WechatIdentityBindingServiceImplTest,WechatIdentityServiceImplTest,WechatRefundServiceImplTest,PaymentFlowApiIntegrationTest,RefundApiIntegrationTest,WechatPayPropertiesTest" test` | 65 tests，65 passed，0 failures，0 errors，0 skipped |
| 支付超时专项复核 | `mvn -f backend/pom.xml "-Dtest=PaymentFlowApiIntegrationTest" test` | 14 tests，14 passed，0 failures，0 errors，0 skipped；在全量套件后复跑，包含重复超时扫描断言 |
| 小程序类型检查 | `npm run typecheck`（`frontend/miniprogram`） | 通过：`tsc --noEmit` |
| 差异格式 | `git diff --check` | 通过 |

未执行项：微信商户真实支付/退款、实际通知投递、微信开发者工具真机界面联调。这些均依赖当前尚未配置的商户证书、微信合法域名和公网 HTTPS 回调地址，不计入上述自动测试数量。

## 覆盖范围

- 开关关闭/开启时的支付渠道约束、微信预支付身份认证和输入验证。
- OpenID 绑定的手机号一致性、唯一性、重复绑定和冲突处理。
- 微信支付通知字段校验、金额/币种/交易类型/付款人核对、时间转北京时间、异常解析不得结算。
- 微信退款处理中、成功、失败、请求超时后查询、重复请求及订单/积分事务一致性。
- 超时关单成功、close 失败后查询 CLOSED/SUCCESS、微信状态未知时保留锁房和积分预占。
- MySQL 8 并发模拟支付、并发预占、积分流水事务回滚，以及支付/退款重复提交幂等。
- 支付和退款回调 HTTP 路由、原始 body/签名头传递、必需头缺失时的 400 响应。

详细用例见 [test_cases.md](test_cases.md)，可执行测试代码位置见 [test_code/README.md](test_code/README.md)。

## 风险分析

- 自动测试模拟微信网络调用；尚未验证商户 API 私钥、平台证书获取/轮换以及 SDK 对真实签名和 AES-GCM 通知报文的端到端处理。通知业务映射和解析异常分支已由单元测试覆盖。
- 当前没有公网 HTTPS 回调，尚未确认微信平台能访问回调地址、回调重试时序和平台实际状态查询结果。
- 小程序已通过 TypeScript 检查，但尚未在微信开发者工具或真机验证手机号授权、`wx.requestPayment`、支付取消和前后台切换后的状态轮询体验。
- 未运行真实商户退款，因此平台受理、异步退款通知、退款到账时间及异常退款对账仍需联调。

## 上线判断

**真实微信支付/退款：不可开启。** 后端自动测试全部通过，核心状态流转和数据库一致性有 MySQL 8 集成覆盖；但平台证书/签名验证、真实扣款与退款通知及小程序真机流程尚未完成端到端验收。完成商户配置和公网 HTTPS 部署后，应按用例清单执行小额支付、退款及通知重试验收，再开启 `WECHAT_PAY_ENABLED`。

**模拟支付：** 本次保留的模拟支付路径继续由自动测试覆盖，微信支付开关默认关闭。
