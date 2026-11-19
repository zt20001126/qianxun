# 阶段 10 验收记录

执行日期：2026-11-19（Asia/Shanghai）

## 自动检查

| 检查 | 结果 |
| --- | --- |
| `mvn -s backend/maven-settings/aliyun.xml -f backend/pom.xml test` | 通过：43 tests，0 failures，0 errors，0 skipped；集成测试使用 Testcontainers + MySQL 8.0 |
| `npm --prefix frontend/admin run build` | 通过：TypeScript 检查和 Vite 生产构建 |
| `npm --prefix frontend/miniprogram run typecheck` | 通过 |
| Compose 配置解析（临时进程环境中提供测试 JWT 密钥） | 通过；测试密钥未写入工作区 |
| `docker compose ... build backend admin` | 通过：后端与管理后台镜像均构建完成 |
| 临时隔离 Compose 栈 | 通过：后端 `/actuator/health` 为 `UP`，Nginx `/health` 为 `ok`，同域 `/api` 房间查询与手机号登录成功，未认证后台请求为 401，Flyway 初始化 7 个渠道；验收后删除独立容器、网络和全新数据卷 |
| `docker run --rm --add-host backend:127.0.0.1 qianxun-admin nginx -t` | 通过：Nginx 配置语法有效 |
| `sh -n scripts/backup_database.sh`（通过临时容器中的 Alpine `sh` 执行） | 通过 |
| `git diff --check` | 通过 |

支付新增的五个 MySQL 集成用例验证了 P01/P08 金额与积分快照、重复支付幂等、同支付号并发、同房间并发预占、积分流水失败回滚和过期支付关单。

## 尚未执行的人工验收

- 临时验收栈使用独立项目名、专用端口和新建数据卷，结束后已移除；未连接原有本机数据卷或生产数据库。
- 未执行数据库备份/恢复演练；备份命令只完成脚本语法验证。
- 未通过浏览器/微信开发者工具做 UI 全链路验收，未验证真实 HTTPS 域名、证书或微信合法域名。
- 未部署到服务器。公开营业前仍受无手机号验证、模拟支付和 OTA 手工录单等 V3.1 边界限制。
