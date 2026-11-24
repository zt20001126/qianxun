# 芊浔民宿

芊浔民宿包含 Spring Boot API、Vue 管理后台和微信小程序，基础业务遵循 V3.1 契约，并增加 V3.2 微信支付扩展。默认 `WECHAT_PAY_ENABLED=false`，继续使用不产生真实资金的模拟支付；仅在商户配置与公网 HTTPS 回调准备完成后，才可切换为真实微信支付。开启后新交易只使用微信支付。OTA 订单由管理员手动录入。

本仓库已实现实施指南阶段 0–9 的业务模块，并补充阶段 10 的支付闭环验收、容器化管理后台、Nginx API 代理和数据库备份脚本。当前工作区不代表已部署到任何服务器。配置步骤见 [阶段 10 部署与运维说明](deploy/README.md)，支付扩展契约见 [V3.2 微信支付需求](docs/01-需求/芊浔民宿_需求扩展_V3.2_微信支付_20261120.md)；完整文档见 [docs 文档中心](docs/README.md)。

## 环境要求

- Docker Compose（用于 MySQL 8.0 和后端容器）
- Java 21 与 Maven（本机后端开发和测试）
- Node.js 22（管理后台构建）；微信开发者工具（小程序）

## 本地容器启动

从模板创建本机配置，并为本机 JWT 随机生成至少 32 字节密钥：

```powershell
Copy-Item deploy/.env.example deploy/.env
[Convert]::ToBase64String([Security.Cryptography.RandomNumberGenerator]::GetBytes(32))
```

把生成值填入 `deploy/.env` 的 `JWT_SECRET`，然后启动本地开发容器：

```powershell
docker compose --env-file deploy/.env -f deploy/docker-compose.yml up --build -d
docker compose --env-file deploy/.env -f deploy/docker-compose.yml ps
docker compose --env-file deploy/.env -f deploy/docker-compose.yml logs -f backend
```

服务器使用同一份 `deploy/.env.example` 作为配置模板，在服务器单独创建 `deploy/.env`；只需将数据库相关参数改为服务器的生产数据库配置。不要把服务器上的真实配置提交到 Git。

管理后台经 Nginx 访问 `http://localhost:8081`，同域 `/api` 由 Nginx 转发到后端。后端健康检查地址为 `http://localhost:8080/actuator/health`，仅绑定本机回环地址。MySQL 映射在本机 `3307`，应用连接容器网络中的 `mysql:3306`。首次启动由 Flyway 创建表和七个固定渠道，并按迁移初始化管理员行；房间资料按实际环境通过后台创建或使用旧库导入脚本。

开发期间也可直接启动管理后台：

```powershell
Set-Location frontend/admin
npm ci
npm run dev
```

默认 Vite 地址为 `http://localhost:5173`，API 默认地址为 `http://localhost:8080`。需要改地址时，在 `frontend/admin` 下建立忽略提交的 `.env.local` 并设置 `VITE_API_BASE_URL`。

## 旧库房间导入

新后端完成 Flyway 迁移后，可预览旧库房间映射，再导入目标开发库中缺少的固定房号。脚本只读旧库，不覆盖目标库同号房间。

```powershell
powershell -ExecutionPolicy Bypass -File scripts/migrate_legacy_rooms.ps1
powershell -ExecutionPolicy Bypass -File scripts/migrate_legacy_rooms.ps1 -Apply
```

源库和目标库密码会在运行时提示输入，不要写进文档或提交到仓库。更多步骤见 [阶段 10 部署与运维说明](deploy/README.md)。

## 微信小程序

用微信开发者工具打开 `frontend/miniprogram`。开发调试地址配置在 `frontend/miniprogram/config.ts`；真机预览或发布需要配置可访问的 HTTPS API 域名，并在微信公众平台登记合法 request 域名。默认仍关闭真实微信支付；开启方式、私钥挂载和回调域名要求见部署说明。微信付款前会通过手机号授权核对已有会员并绑定 OpenID。

## 质量检查

```powershell
mvn -s backend/maven-settings/aliyun.xml -f backend/pom.xml test
npm --prefix frontend/admin run build
npm --prefix frontend/miniprogram run typecheck
```

真实 MySQL 行为由 Testcontainers 集成测试覆盖；运行时需要 Docker。支付回归覆盖模拟支付、支付模式开关、微信结算幂等、付款人/金额校验及微信退款成功/失败后的订单与积分状态。当前自动检查与未完成的线上验收见 [阶段 10 验收记录](deploy/VERIFICATION.md)。
