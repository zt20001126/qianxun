# 芊浔民宿：第三阶段房态校验与封房

当前交付包含 A01–A04 认证接口、客房/封房 R01–R13、渠道 C01/C02 和达人 D01–D05。后台提供客房、封房、渠道、达人管理页；小程序支持浏览房间、按日期筛选可订房间、查看单房 60 天日历并复核日期区间。

Flyway 迁移创建 `members`、`admin`、`room`、`channels`、`creators`、`room_block`、`orders` 和 `booking_lock`，并初始化七个固定渠道。`orders` 与 `booking_lock` 目前建立 V3.1 定义的完整表结构，供统一房态查询读取，录单、支付和积分流程仍属于后续阶段。六间客房资料通过独立的一次性脚本从旧库 `homestay_db.room` 与 `room_type` 导入。

文档分类和原型素材索引见 [docs 文档中心](docs/README.md)。

## 启动数据库与后端

需要 Docker Desktop、Java 21 和 Node.js 20.19+。复制 `deploy/.env.example` 为 `deploy/.env`，配置 OSS 和 JWT 密钥：

```powershell
Copy-Item deploy/.env.example deploy/.env
[Convert]::ToBase64String([Security.Cryptography.RandomNumberGenerator]::GetBytes(32))
```

将生成的密钥写入本机 `deploy/.env` 的 `JWT_SECRET`，不要提交该文件。`DATABASE_URL` 配置目标开发库；OSS endpoint、bucket 和访问密钥从同一 `.env` 传给后端。真实访问密钥只保存在本机忽略文件中。

```powershell
docker compose --env-file deploy/.env -f deploy/docker-compose.yml up --build -d
docker compose --env-file deploy/.env -f deploy/docker-compose.yml logs -f backend
```

Compose 项目名为 `qianxun`，启动 MySQL 8.0 和 Java 21 后端。Flyway 在后端启动时创建当前阶段的数据库表并初始化管理员账号 `admin`；数据库只保存 BCrypt 哈希。MySQL 本地开发账号和端口见 `deploy/docker-compose.yml`。如需切换镜像，可通过命令行环境变量覆盖 `DOCKER_REGISTRY_PREFIX` 或 `MAVEN_MIRROR_PROFILE`。

### 导入旧库客房

新后端启动并完成 Flyway 迁移后，预览旧库房间映射，再导入目标开发库中缺少的固定房号。脚本只读取旧库，不修改或删除源数据；目标库同号房间会保留，不会覆盖管理员后续调整。

```powershell
powershell -ExecutionPolicy Bypass -File scripts/migrate_legacy_rooms.ps1
powershell -ExecutionPolicy Bypass -File scripts/migrate_legacy_rooms.ps1 -Apply
```

脚本分别提示输入源库和目标库密码，凭据不会写入仓库。旧库图片以私有 OSS URI 保存；小程序房间接口会在后端 OSS 配置完整时生成一小时有效的签名读取地址。图片上传不在本阶段接口范围内。

## 启动管理后台

```powershell
Set-Location frontend/admin
npm ci
npm run dev
```

默认访问 `http://localhost:5173`，API 默认访问 `http://localhost:8080`。需要改 API 地址时，在 `frontend/admin` 创建本地 `.env.local` 并设置 `VITE_API_BASE_URL`。后端跨域来源通过 `APP_CORS_ALLOWED_ORIGINS` 配置。

## 启动微信小程序

用微信开发者工具打开 `frontend/miniprogram`。开发者工具中可启用本地调试的 request 域名校验例外；真机预览或发布时，需在 `frontend/miniprogram/config.ts` 配置可访问的 HTTPS API 地址，并在微信公众平台配置合法 request 域名。

## 当前接口范围

| 方法与路径 | 认证 | 说明 |
|---|---|---|
| `POST /api/app/auth/login` | 公开 | 手机号登录/自动创建会员，返回 30 天 JWT |
| `GET /api/app/auth/me` | `MEMBER` | 查询当前会员 |
| `POST /api/admin/auth/login` | 公开 | 用户名与 BCrypt 密码登录 |
| `GET /api/admin/auth/me` | `ADMIN` | 查询当前管理员 |
| `GET /api/app/rooms` | 公开 | 查询已上架房间；可同时提供 `check_in`、`check_out` 筛选可订房间 |
| `GET /api/app/rooms/{id}` | 公开 | 查询房间详情 |
| `GET /api/app/rooms/{id}/availability?month=YYYY-MM` | 公开 | 查询未来 60 天范围内该房间不可订日期 |
| `POST /api/app/rooms/availability-check` | 公开 | 复核指定住宿区间、晚数和当前房费 |
| `/api/admin/rooms` | `ADMIN` | 房间列表、新增、编辑、上下架 |
| `/api/admin/room-blocks` | `ADMIN` | 封房列表、新增、调整、整段解封；写入时检查订单和有效临时锁房冲突 |
| `/api/admin/channels` | `ADMIN` | 渠道列表、启用/停用 |
| `/api/admin/creators` | `ADMIN` | 达人列表、详情、新增、编辑、启用/停用 |

所有响应使用 `{"code":200,"message":"success","data":{...}}`，列表放在 `data.list`。未登录/令牌失效返回 HTTP 401，已认证但角色不符返回 HTTP 403。会员手机号登录不验证号码归属，适用于受控测试环境。

## 质量检查

```powershell
mvn -s backend/maven-settings/aliyun.xml -f backend/pom.xml test
npm --prefix frontend/admin run build
npm --prefix frontend/miniprogram run typecheck
```

打包可单独运行：

```powershell
mvn -s backend/maven-settings/aliyun.xml -f backend/pom.xml -DskipTests package
```

Maven 可选镜像配置为 `backend/maven-settings/aliyun.xml`、`huawei.xml`、`central.xml`。房态阶段 Testcontainers 集成测试覆盖日期筛选、R03/R04、60 天边界、封房冲突与管理员权限；运行这些接口测试需要 Docker Desktop。
