# COS 切换测试报告

> 复测记录（2026-11-27）：缺少 `file` 返回 500 的问题已修复；上传接口、图片校验和 COS 相关测试共 19 项通过。下文保留修复前的原始测试结果。新版部署、微信真机和私有对象边界仍需验收。

测试日期：2026-11-27（北京时间）。测试覆盖当前工作区源码；未重建或部署已有运行容器。

## 执行结果

| 检查 | 结果 |
|---|---|
| 后端完整回归：`mvn -s backend/maven-settings/aliyun.xml -f backend/pom.xml test` | 199 项通过，0 失败，0 跳过；包含 MySQL 8 容器测试。此次新增的 HTTP 测试是在该轮编译后加入，另行执行。 |
| 新增 HTTP 测试：`mvn -s backend/maven-settings/aliyun.xml -f backend/pom.xml -Dtest=MallImageCosApiIntegrationTest test` | 6 项中 5 项通过、1 项失败；真实 Spring Security 链和 MySQL 8 测试容器，COS 使用模拟对象。 |
| 管理后台：`npm --prefix frontend/admin run build` | 通过。 |
| 小程序：`npm --prefix frontend/miniprogram run typecheck` | 通过。 |
| 小程序测试：`node --test frontend/miniprogram/tests/*.test.cjs` | 5 项通过，0 失败。 |
| 实际 COS 上传 | 使用当前商城上传服务和本地配置上传测试 PNG，公开 GET 返回 200、内容一致；删除测试对象请求成功。独立进程运行，不是已运行容器的 HTTP 接口。 |
| 本地数据库图片地址 | 房间 61 条、商品 10 条，共 71 条；逐条 HEAD 均返回 200 和图片 Content-Type。 |
| 微信真机、已部署新版本后端与小程序 | 未执行；当前容器尚未重建，不能据此认定部署或真机页面通过。 |

## 已发现问题

**上传请求缺少 file 字段时返回 500。** 管理员对 `POST /api/admin/mall/products/images` 发送不含 `file` 的 multipart 请求，`MissingServletRequestPartException` 未被现有参数异常分支处理，落入全局兜底并返回 500。项目契约要求参数错误返回 400。回归测试 `rejectsMissingFilePartAsBadRequest` 预期 400，实际 500；本次只测试并报告，未更改异常处理器。

## 其他风险和限制

- 上传接口重复提交会创建不同对象，现有接口没有去重设计；本次已验证行为，可能产生重复文件，不将其标记为幂等。
- 商品和房间数据之外的其他环境、旧小程序版本、外部引用未覆盖。旧迁移 V17/V18 不可修改，V22 仅处理指定初始化商品图片；其他数据库必须先清点。
- 已验证普通文件的真实上传和公开读取；本轮未验证真实私有桶鉴权、链接过期、大于 5 GiB 上传、网络中断后的真实 COS 分片清理。
- 本地链接 HEAD 成功不能替代微信真机的图片内嵌显示、合法域名和防盗链验证；真实线上后端/小程序尚未部署本次代码。
- 完整测试期间有测试容器关闭后连接池检查警告；该轮所有测试通过，但不把警告等同于线上环境健康验证。

## 上线判断

**不可上线（本次测试范围）。** 缺少文件字段的接口参数错误被错误地映射为 500，且新版后端部署、小程序真机图片展示及真实私有对象边界尚未验收。修复接口错误并复测后，仍需在目标环境完成部署与真机验证，才能重新判断。
