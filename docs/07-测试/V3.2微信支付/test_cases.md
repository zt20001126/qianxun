# V3.2 微信支付与退款测试用例

| 用例ID | 测试类型 | 测试场景 | 输入 | 预期输出 | 是否通过 |
|---|---|---|---|---|---|
| PAY-001 | Happy Path | 默认关闭时创建模拟支付并完成支付 | 默认配置，P01 + mock-pay | 支付成功，只创建一笔订单及积分流水 | 通过 |
| PAY-002 | Boundary | 打开开关后创建微信支付并拒绝模拟支付 | `WECHAT_PAY_ENABLED=true` | 支付方式为 WECHAT，mock-pay 返回冲突 | 通过 |
| PAY-003 | Negative | 关闭开关时拒绝微信预支付 | 有效支付号和授权 code | 返回冲突，不调用微信服务 | 通过 |
| PAY-004 | API/Authorization | 未登录调用微信预支付 | 无 JWT | HTTP 401 | 通过 |
| PAY-005 | API/Bad Request | 授权 code 为空、类型错误或超过 128 字符 | 空串、对象、129 字符 code | HTTP 400，不发起外部身份验证 | 通过 |
| PAY-006 | Authorization | 其他会员使用该支付号请求预支付 | 非订单所属会员 JWT | 返回 404，不请求微信 | 通过 |
| PAY-007 | Boundary | 锁房已过期时请求微信预支付 | 过期的 PENDING 锁房 | 返回冲突，不交换身份或调用微信 | 通过 |
| PAY-008 | Happy Path | 返回有效 `prepay_id` 后保存并返回调起参数 | 有效 `prepay_id` 和签名字段 | 保存 prepay id，返回小程序签名字段 | 通过 |
| PAY-009 | Negative | 微信返回无效 package | 缺少/错误 `prepay_id=` | 返回网关错误，不写入 prepay id | 通过 |
| PAY-010 | Negative | 支付通知 AppID、商户号、状态、币种、交易类型或必填字段不合法 | 修改回调字段 | HTTP 业务错误，不结算订单 | 通过 |
| PAY-011 | Negative | 通知验签/解密解析失败 | SDK parser 抛出异常 | 不进入订单结算 | 通过 |
| PAY-012 | Boundary | 通知为空或成功时间无法解析 | null 通知、非法时间 | HTTP 400，不创建订单 | 通过 |
| PAY-013 | Integration/Idempotency | 微信回调金额及付款人正确，重复结算 | 匹配订单金额与绑定 OpenID | 一笔正式订单、一条成功支付流水 | 通过 |
| PAY-014 | Concurrency | 两个请求并发模拟支付 | 同一个支付号 | 返回同一订单，不重复扣积分 | 通过 |
| PAY-015 | Concurrency | 不同会员并发预占相同房晚 | 同房间、同日期 | 仅一个请求成功，另一个冲突 | 通过 |
| PAY-016 | Integration/Idempotency | 积分流水写入失败时支付结算回滚 | 数据库约束阻止流水写入 | 订单、支付和锁房均回滚 | 通过 |
| PAY-017 | Integration/Boundary | 超时微信单关单成功后释放预订 | 微信 close 成功 | payment CLOSED，booking EXPIRED | 通过 |
| PAY-018 | Integration/Recovery | close 调用失败但微信查询为 CLOSED | 查询状态 CLOSED | 本地关闭支付并释放锁房 | 通过 |
| PAY-019 | Integration/Negative | 微信状态未知或 close/query 网络失败 | 查询 NOTPAY 或抛异常 | 保留 PENDING，不释放房间和积分预占 | 通过 |
| PAY-020 | Integration/Recovery | close 竞争期间查询到已成功支付 | SUCCESS，付款时间早于锁房截止 | 创建正式订单，不误释放锁房 | 通过 |
| AUTH-001 | Happy Path | 获取 OpenID 和授权手机号后绑定会员 | session、token、phone API 成功 | 绑定 OpenID 并返回 | 通过 |
| AUTH-002 | Negative | 微信登录 code 错误或返回缺少 OpenID | errcode 或空 openid | 返回网关错误，不绑定 | 通过 |
| AUTH-003 | Negative | 手机号授权接口失败或网络错误 | errcode、HTTP 5xx | 返回网关错误，不绑定 | 通过 |
| AUTH-004 | Boundary | access_token 在多次调用间复用 | 连续两次身份验证 | 只请求一次 token，分别验证一次性 code | 通过 |
| AUTH-005 | Negative | 授权手机号与会员手机号不一致 | 不同手机号 | 返回 403，不写入 OpenID | 通过 |
| AUTH-006 | Negative | OpenID 已属于其他会员或会员已有不同 OpenID | 唯一键冲突/不同绑定 | 返回 409，不覆盖关联 | 通过 |
| AUTH-007 | Idempotency | 重复绑定相同 OpenID | 同一会员、同一手机号和 OpenID | 返回原 OpenID，不重复写入 | 通过 |
| REF-001 | API/Happy Path | 微信退款请求经真实业务层进入待处理 | WECHAT 成功支付订单 | 返回 PENDING，订单与积分不变 | 通过 |
| REF-002 | API/Idempotency | 重复提交待处理微信退款 | 同一会员、订单、原因 | 返回同一退款号，只保留一笔退款 | 通过 |
| REF-003 | Happy Path/Idempotency | 微信确认退款成功并重复通知 | 匹配支付号、金额和退款号 | 订单取消、积分返还一次 | 通过 |
| REF-004 | Negative | 微信通知金额或币种不匹配 | 错误金额/非 CNY | 不完成退款事务 | 通过 |
| REF-005 | Boundary | 微信退款处理中 | `PROCESSING` | 保留 PENDING，不取消订单或返积分 | 通过 |
| REF-006 | Negative | 微信退款 CLOSED/ABNORMAL | 终态失败状态 | 退款记 FAILED，订单及积分不变 | 通过 |
| REF-007 | Recovery | 发起退款超时后查询微信结果 | 相同商户退款号查询成功 | 按查询结果幂等完成退款 | 通过 |
| REF-008 | Recovery/Negative | 创建和查询都失败 | 微信网络不可用 | 返回 502，保留 PENDING | 通过 |
| REF-009 | Idempotency | 已有微信退款号时重试 | PENDING 且 provider refund id 已有 | 查询原退款，不创建新退款 | 通过 |
| REF-010 | Boundary | 已完成退款再次提交 | SUCCESS 退款单 | 直接返回原结果，不调用微信 | 通过 |
| REF-011 | Authorization | 非订单会员查看或退款 | 他人订单 ID | HTTP 404，不创建退款 | 通过 |
| REF-012 | Negative/Boundary | 已入住订单退款及原因长度超限 | CHECKED_IN / 301 字符 | HTTP 409/400，无退款与积分变化 | 通过 |
| API-001 | API | 支付通知路由传递原始 body 和验签头 | 合法 HTTP POST | 返回 SUCCESS，服务收到原样 headers/body | 通过 |
| API-002 | API | 退款通知路由传递原始 body 和验签头 | 合法 HTTP POST | 返回 SUCCESS，服务收到原样 headers/body | 通过 |
| API-003 | API/Bad Request | 通知请求缺少签名头 | 缺少 `Wechatpay-Signature` | HTTP 400，不调用服务 | 通过 |
| CFG-001 | Boundary | 微信启用所需配置完整且 HTTPS | 完整商户设置 | 配置校验通过 | 通过 |
| CFG-002 | Negative | 缺少商户配置或回调 URL 不安全 | 空密钥、HTTP、userinfo、fragment | 配置启动校验失败 | 通过 |
