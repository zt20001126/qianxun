# 芊浔民宿：第一阶段登录认证

当前交付仅包含 A01–A04 四个认证接口，以及会员登录页、管理员登录页和管理员认证成功占位页。数据库只迁移 `members`、`admin` 两表。房间、订单、支付、积分业务不在本阶段。

文档分类和原型素材索引见 [docs 文档中心](docs/README.md)。

## 启动数据库与后端

需要 Docker Desktop、Java 21（仅本地 Maven 测试时需要）和 Node.js 20.19+（管理后台开发时需要）。复制 `deploy/.env.example` 为 `deploy/.env`，填好 OSS 配置并生成一个固定保管的 JWT 密钥：

```powershell
Copy-Item deploy/.env.example deploy/.env
[Convert]::ToBase64String([Security.Cryptography.RandomNumberGenerator]::GetBytes(32))
```

将生成的密钥写入本机 `deploy/.env` 的 `JWT_SECRET`，不要提交该文件。数据库仍通过一行完整的 `DATABASE_URL` 配置；OSS 的 endpoint、bucket 和访问密钥从同一 `.env` 传给后端。`.env.example` 只提供空占位，真实访问密钥只保存在本机忽略文件中。Flyway 在管理员表为空时初始化管理员账号 `admin`；数据库只保存 BCrypt 哈希，不需要管理员密码环境变量。

```powershell
docker compose --env-file deploy/.env -f deploy/docker-compose.yml up --build -d
docker compose --env-file deploy/.env -f deploy/docker-compose.yml logs -f backend
```

Compose 项目名为 `qianxun`，只启动 MySQL 8.0 和 Java 21 后端。Flyway 在后端启动时创建本阶段的两张表并初始化管理员。MySQL 初始化账号和端口固定在 Compose 配置中；如需切换国内镜像，可通过命令行环境变量覆盖 `DOCKER_REGISTRY_PREFIX` 或 `MAVEN_MIRROR_PROFILE`，默认使用 DaoCloud Docker Hub 代理和阿里云 Maven 仓库。

## 启动管理后台

```powershell
Set-Location frontend/admin
npm ci
npm run dev
```

默认访问 `http://localhost:5173`，API 默认访问 `http://localhost:8080`。需要改 API 地址时，在 `frontend/admin` 创建本地 `.env.local` 并设置 `VITE_API_BASE_URL`。后端跨域来源通过 `APP_CORS_ALLOWED_ORIGINS` 配置。

## 启动微信小程序

用微信开发者工具打开 `frontend/miniprogram`。开发者工具中可启用本地调试的 request 域名校验例外；真机预览或发布时，需在 `frontend/miniprogram/config.ts` 配置可访问的 HTTPS API 地址，并在微信公众平台配置合法 request 域名。

## 四个认证接口

| 方法与路径 | 认证 | 说明 |
|---|---|---|
| `POST /api/app/auth/login` | 公开 | 手机号登录/自动创建会员，返回 30 天 JWT |
| `GET /api/app/auth/me` | `MEMBER` | 查询当前会员 |
| `POST /api/admin/auth/login` | 公开 | 用户名与 BCrypt 密码登录 |
| `GET /api/admin/auth/me` | `ADMIN` | 查询当前管理员 |

所有响应使用 `{"code":200,"message":"success","data":{...}}`。未登录/令牌失效返回 HTTP 401，已认证但角色不符返回 HTTP 403。会员手机号登录不验证号码归属，适用于受控测试环境。

## 验证

在仓库根目录运行：

```powershell
mvn -s backend/maven-settings/aliyun.xml -f backend/pom.xml test
npm --prefix frontend/admin run build
```

Maven 可选镜像配置为 `backend/maven-settings/aliyun.xml`、`huawei.xml`、`central.xml`。后端测试使用 Testcontainers 启动 MySQL 8，覆盖四个接口、手机号重复登录、并发首次登录、BCrypt、401、403 和手机号校验。
