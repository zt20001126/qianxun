# 阶段 10：部署与运维

本目录提供开发/验收用 Compose 配置。Compose 会启动 MySQL 8.0、Spring Boot API 和 Vue 管理后台；Nginx 提供 SPA 路由，并把同域 `/api` 请求转发给后端。后端健康检查通过后才启动管理后台。

## 本机验收启动

1. 将 `.env.example` 复制为 `.env`，至少设置 `JWT_SECRET`。开发用数据库、账号、口令和连接 URL 固定为 `qianxun`，由 Compose 统一配置。生产环境不要复用这些值；应提供独立的生产 Compose 配置，同时修改数据库和后端连接 URL，并使用独立 JWT 密钥。
2. 启动并检查服务：

   ```sh
   docker compose --env-file deploy/.env -f deploy/docker-compose.yml up --build -d
   docker compose --env-file deploy/.env -f deploy/docker-compose.yml ps
   curl http://localhost:8080/actuator/health
   curl http://localhost:8081/health
   ```

3. 管理后台默认在 `http://localhost:8081`，API 通过同源 `/api` 访问。开发者工具打开 `frontend/miniprogram`；真机访问须把 `frontend/miniprogram/config.ts` 指向 HTTPS API 域名并配置微信合法 request 域名。
4. 第一次数据库迁移只初始化结构、固定渠道和管理员记录。部署前应按环境准备六间客房资料，确认管理员凭据已由受控流程设置；不要把口令或 JWT 密钥写进仓库。旧系统迁移时使用仓库提供的只读源库导入脚本。

默认端口：管理后台 `8081`、仅本机可访问的后端 `8080`、仅本机可访问的 MySQL `3307`。发生端口冲突时，可在 `deploy/.env` 追加 `ADMIN_HTTP_PORT`、`BACKEND_HOST_PORT` 或 `MYSQL_HOST_PORT` 覆盖对应映射；默认开发环境不需要这些变量。

## HTTPS 与上线边界

Compose 暴露 HTTP 管理站点，供开发和内网验收使用。正式环境应在 TLS 反向代理或云负载均衡器后提供 HTTPS，并限制管理后台来源；不要把 MySQL 或后端直连端口开放到公网。设置 `APP_CORS_ALLOWED_ORIGINS` 为实际管理后台来源。配置域名、证书、微信合法域名和服务器防火墙后，才进行真机验收。

阶段 10 不会把第一期测试版本变成真实营业系统：手机号登录不验证号码归属，支付和退款都是模拟流程，六家 OTA 不会自动同步。公开营业前必须另行确认身份验证、真实支付和平台订单管理方案；不得将模拟支付流水当作真实收款。

## 数据库备份

在 Linux 部署主机上执行：

```sh
sh scripts/backup_database.sh
```

脚本使用 MySQL 一致性快照生成 gzip 压缩 SQL 文件，写入 `backups/`，文件权限限定为当前用户，并通过 `gzip -t` 检查压缩完整性。可传入独立备份目录：

```sh
sh scripts/backup_database.sh /var/backups/qianxun
```

备份文件应复制到与应用主机隔离且有访问控制的存储，制定保留策略，并定期在隔离的恢复库演练。恢复会覆盖目标数据库内容；执行前必须核对目标实例、库名和备份文件，并停止写入。不要将恢复命令用于仍在运行的生产库。

迁移通过 Flyway 管理。已应用迁移不允许原地修改；升级前先备份数据库，再部署新镜像并检查 `/actuator/health` 和 Flyway 迁移结果。

## 发布前人工验收

- [ ] `docker compose ps` 中 MySQL 健康、后端健康、管理后台运行。
- [ ] 检查 `/actuator/health` 返回 `UP`，并确认 V1–V9 迁移与渠道种子数据。
- [ ] 后台管理员登录、顾客手机号登录、角色越权和过期 Token 行为正确。
- [ ] 完成一次小程序预支付、模拟支付、后台确认、入住、退房及积分奖励；确认所有金额标注为模拟业务数据。
- [ ] 验证自助退款资格的入住日 17:59:59 / 18:00:00 边界和退款积分返还。
- [ ] 验证管理员封房、后台订单、日历、支出、报表和跨月金额。
- [ ] 完成备份、在隔离库恢复，并核对关键表和记录数。
- [ ] 为管理后台和小程序 API 配好 HTTPS、域名白名单和网络访问控制。

自动化测试执行记录应与发布版本一同留存；未能运行的 MySQL 并发测试必须标注为未验收，不可按通过处理。

本次仓库修改的自动检查与未执行的人工检查见 [阶段 10 验收记录](VERIFICATION.md)。
