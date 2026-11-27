# COS 切换测试用例

> 复测记录（2026-11-27）：COS-02 中缺失 `file` 的请求已按预期返回 400；相关测试共 19 项通过。下表保留原测试时的结果。

| 用例ID | 测试类型 | 测试场景 | 输入 | 预期输出 | 是否通过 |
|---|---|---|---|---|---|
| COS-01 | API / Happy Path | 管理员上传图片 | PNG 和管理员 JWT | 200，统一响应包含 COS URL | 通过 |
| COS-02 | API / Null/Empty | 空图片及缺失文件字段 | 空文件、缺失 file | 400，统一错误响应 | 部分失败：缺失 file 返回 500，空文件返回 400 |
| COS-03 | API / Invalid Type | 非图片内容 | SVG 内容 | 400，不调用 COS | 通过 |
| COS-04 | API / 权限 | 匿名及会员上传 | 无 JWT、会员 JWT | 401 / 403，不调用 COS | 通过 |
| COS-05 | API / Negative | COS 返回错误 | 模拟 502 | 返回 502，不泄露敏感信息 | 通过 |
| COS-06 | API / Idempotency | 重复上传 | 同一图片两次 | 两次成功，生成不同对象键；当前接口不去重 | 通过 |
| COS-07 | Unit / Boundary | 大图分片 | 大于 8 MiB | 分片上传并完成 | 通过 |
| COS-08 | Unit / Negative | 输入流长度不足 | 不完整流 | 上传失败并中止分片 | 通过 |
| COS-09 | Unit / Overflow | 签名有效期 | 零、超过七天 | 业务异常 | 通过 |
| COS-10 | Integration | 初始化图片迁移 | MySQL 8 新库 | V22 后初始商品图使用 COS | 通过 |
| COS-11 | 真实环境 | 本地数据库图片可访问 | 房间和商品 71 条地址 | HTTP 200，图片内容类型 | 通过 |
| COS-12 | Unit | 旧房间 URI | `oss://qianxunminsu/rooms/a.jpg` | 使用 COS 生成限时地址 | 通过 |
| COS-13 | 人工 / Integration | 微信真机及后台图片预览 | 新版后端和小程序 | 房间、商品、订单、攻略图显示正常 | 未执行 |
