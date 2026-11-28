# 阶段 10：部署与运维

本目录提供开发/验收用 Compose 配置。Compose 会启动 MySQL 8.0、Spring Boot API 和 Vue 管理后台；Nginx 提供 SPA 路由，并把同域 `/api` 请求转发给后端。后端健康检查通过后才启动管理后台。

## 本机验收启动

1. 本地和服务器使用同一份配置模板 `.env.example`，各自在自己的 `deploy/.env` 中保存运行配置。服务器上的 `deploy/.env` 只需把 `MYSQL_DATABASE`、`MYSQL_USER`、`MYSQL_PASSWORD`、`MYSQL_ROOT_PASSWORD` 改为生产专用值；Compose 会据此初始化服务器 MySQL 并生成后端连接地址。不要把服务器上的真实配置提交到 Git。`DATABASE_URL` 通常留空；只有连接 Compose 之外的数据库或自定义连接参数时才填写。
2. 启动并检查服务：

   ```sh
   docker compose --env-file deploy/.env -f deploy/docker-compose.yml up --build -d
   docker compose --env-file deploy/.env -f deploy/docker-compose.yml ps
   curl http://localhost:8080/actuator/health
   curl http://localhost:8081/health
   ```

3. 管理后台默认在 `http://localhost:8081`，API 通过同源 `/api` 访问。开发者工具打开 `frontend/miniprogram`；真机访问须把 `frontend/miniprogram/config.ts` 指向 HTTPS API 域名并配置微信合法 request 域名。
4. 第一次数据库迁移只初始化结构、固定渠道和管理员记录。部署前应按环境准备六间客房资料，确认管理员凭据已由受控流程设置；不要把口令或 JWT 密钥写进仓库。旧系统迁移时使用仓库提供的只读源库导入脚本。

默认端口：管理后台 `8081`、局域网可访问的后端 `8080`、仅本机可访问的 MySQL `3307`。小程序本机联调时，需将 `frontend/miniprogram/config.ts` 中的 API 地址设为开发电脑当前 Wi‑Fi IPv4 地址；正式环境应使用 HTTPS 合法域名。发生端口冲突时，可在 `deploy/.env` 追加 `ADMIN_HTTP_PORT`、`BACKEND_HOST_PORT` 或 `MYSQL_HOST_PORT` 覆盖映射。

## HTTPS 与上线边界

### 宿主机 Nginx 接入 HTTPS（测试环境）

以下示例针对 Ubuntu 宿主机已运行 Nginx、管理后台使用默认端口 8081 的情况。宿主机处理 HTTPS，转发到本机管理后台容器；容器继续提供页面并转发 `/api/`。若端口有调整，须同步修改 `nginx.qianxun.https.conf.example` 的 `proxy_pass`。

1. 将本仓库部署到服务器，在服务器上按 `.env.example` 创建 `deploy/.env`，并将数据库相关值替换为服务器生产专用配置；设置 `APP_CORS_ALLOWED_ORIGINS=https://111.229.107.194`（使用域名时改为实际 HTTPS 来源，不带路径或末尾斜杠），并设置 `ADMIN_BIND_ADDRESS=127.0.0.1`、`BACKEND_BIND_ADDRESS=127.0.0.1`，避免直接向公网开放容器端口。开发用数据库口令不得用于生产环境。
2. 从仓库根目录运行 `docker compose --env-file deploy/.env -f deploy/docker-compose.yml up --build -d`，检查 `docker compose --env-file deploy/.env -f deploy/docker-compose.yml ps` 和 `curl http://127.0.0.1:8081/health`。
3. 获取与访问地址匹配的可信证书，将完整证书链和私钥放在服务器安全位置。示例文件中的 `/etc/letsencrypt/live/111.229.107.194/` 仅为路径示意，必须改为证书实际路径；不要将证书私钥提交到仓库。IP 地址证书需要覆盖该 IP，域名证书需要使用对应域名访问。
4. 将 `deploy/nginx.qianxun.https.conf.example` 复制到 `/etc/nginx/sites-available/qianxun`，按实际证书路径、IP 或域名及端口修改。检查现有站点配置，避免同地址同端口的默认站点与其冲突；确认无需默认站点后，可移除 `/etc/nginx/sites-enabled/default` 这个链接，然后建立 `sudo ln -s /etc/nginx/sites-available/qianxun /etc/nginx/sites-enabled/qianxun`。修改前备份现有配置。
5. 执行 `sudo nginx -t`；仅在检查成功后执行 `sudo systemctl reload nginx`。云防火墙需允许 TCP 80、443。随后从外部访问 `https://111.229.107.194/`，并检查后台登录及 `/api/` 请求。
6. 配置并验证证书自动续期；续期后重载宿主机 Nginx 才能加载新证书。HTTP-01 验证使用示例中的 `/var/www/html` 验证目录；申请证书前需确保验证文件能通过 HTTP 访问。实际路径和验证方式以证书颁发机构说明为准。

该示例只展示 HTTPS 链路，不包含管理后台公网访问限制和生产数据库加固。微信小程序仍须按微信要求配置合法域名，不能仅凭 IP 证书假定符合要求。

Compose 暴露 HTTP 管理站点，供开发和内网验收使用。正式环境应在 TLS 反向代理或云负载均衡器后提供 HTTPS，并限制管理后台来源；不要把 MySQL 或后端直连端口开放到公网。设置 `APP_CORS_ALLOWED_ORIGINS` 为实际管理后台来源。配置域名、证书、微信合法域名和服务器防火墙后，才进行真机验收。

第一期默认仍使用模拟支付。微信支付仅在 `WECHAT_PAY_ENABLED=true` 时启用，此时新交易只创建微信支付；关闭时新交易只走模拟支付。启用前须配置 `.env` 中的 AppID/AppSecret、商户号、商户证书序列号、APIv3 密钥和两个 HTTPS 回调地址，并将商户私钥放在 `WECHAT_PAY_KEY_DIR` 指定目录下（默认 `deploy/secrets/apiclient_key.pem`）。私钥目录已加入 Git 忽略规则，不要提交凭证。

商户使用微信支付公钥模式时，需将微信支付公钥放在该目录，并在 `.env` 中同时设置 `WECHAT_PAY_PUBLIC_KEY_ID` 和 `WECHAT_PAY_PUBLIC_KEY_PATH=/run/secrets/wechat/pub_key.pem`。公钥 ID 和文件必须属于同一微信支付商户；二者只填一项会阻止后端启动。两项都未填写时沿用原有平台证书模式。商户私钥仍用于请求签名，不可用微信支付公钥替代。

微信回调路径为 `/api/payment/wechat/notify/payment` 和 `/api/payment/wechat/notify/refund`。当前 Compose 仅供开发/验收使用，默认通过 HTTP 暴露后端；真实支付测试必须先在 TLS 反向代理或云负载均衡后提供公网 HTTPS 地址，并将小程序 request 合法域名、回调 URL 和商户平台配置为该地址。微信支付成功以服务端验签回调结算；退款在微信确认成功后才取消订单及返还积分。手机号登录本身仍不验证归属，只有支付前微信授权手机号与当前会员手机号匹配时才绑定 OpenID。

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
- [ ] 检查 `/actuator/health` 返回 `UP`，并确认 V1–V10 迁移与渠道种子数据。
- [ ] 后台管理员登录、顾客手机号登录、角色越权和过期 Token 行为正确。
- [ ] 完成一次小程序预支付、模拟支付、后台确认、入住、退房及积分奖励；确认所有金额标注为模拟业务数据。
- [ ] 验证自助退款资格的入住日 17:59:59 / 18:00:00 边界和退款积分返还。
- [ ] 验证管理员封房、后台订单、日历、支出、报表和跨月金额。
- [ ] 完成备份、在隔离库恢复，并核对关键表和记录数。
- [ ] 为管理后台和小程序 API 配好 HTTPS、域名白名单和网络访问控制。

自动化测试执行记录应与发布版本一同留存；未能运行的 MySQL 并发测试必须标注为未验收，不可按通过处理。

本次仓库修改的自动检查与未执行的人工检查见 [阶段 10 验收记录](VERIFICATION.md)。


## 邀请首单奖励启用

本版本启动时自动执行 V20 迁移，奖励从迁移完成后的下一整秒起生效，无需配置生效时间。迁移时间保存在 Flyway 历史表中，多实例及重启沿用同一边界；只有此时间起且邀请关系绑定后支付的房间订单，完成退房才产生奖励。上线时须避免新旧服务混跑，防止旧服务处理退房时漏记奖励。系统只记录发放状态，管理员需在系统外发放。

## Redis 展示缓存

Compose 启动内网 Redis，后端通过 REDIS_HOST 和 REDIS_PORT 连接；不对宿主机公开端口。缓存保存房间基础资料、商城商品展示资料及部分房间图片签名，不保存图片文件和交易数据。Redis 中断时展示查询回退 MySQL，缓存过期后按访问重新加载。直接通过 SQL 修改房间或商品资料时，不会触发应用失效，需等待展示缓存过期；不要使用 FLUSHALL 清理共享实例。Redis 不设密码时仅适用于可信的隔离网络，不得暴露公网。
