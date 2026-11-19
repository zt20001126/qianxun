# 小程序核心预订页面还原方案

> 日期：2026-11-19（Asia/Shanghai）
> 状态：已实施；微信开发者工具视觉与接口联调待执行
> 需求确认：已澄清；详情轮播按附件的 4:3 比例实现

## 1. 目标与背景

- **要解决的问题：** 当前首页把客房列表和日期筛选放在同一页；项目没有独立可订房间页。详情页将整块月历直接放在正文中，预订页使用系统日期选择器和积分输入框，与附件原型的抽屉日历、四字段表单、积分开关和页面层级不一致。
- **功能目标：** 在原生微信小程序中还原首页、可订房间、房间详情、填写预订信息、预订成功五个原型页面，并复用已存在的日期、房态、积分、预支付和模拟支付契约。
- **预期最终效果：** 顾客从首页选择日期并查询房间，浏览整段行程可订的房间，查看详情后填写联系人信息；积分金额由服务端预览，预支付由服务端创建 10 分钟锁房；支付页由服务端模拟支付；只有确认支付成功及正式订单后才进入可复制订单号的成功页。

## 2. 已确认需求

- **使用者 / 调用方：** 微信小程序顾客。浏览客房和房态沿用公开查询；积分预览、预支付、支付状态和模拟支付沿用登录会员接口。
- **触发方式：** 首页日期卡、可订房间日期摘要、详情页已选行程条和预订页行程摘要均可打开日历抽屉；主流程按钮按下文路由跳转。
- **输入：** 入住日、退房日、房间、联系人姓名、手机号、入住人数和备注；积分采用开关选择使用/不使用。
- **输出：** 区间可订房间、服务端积分预览和实付金额、服务端支付状态及正式订单编号。
- **主流程：**
  1. 首页展示水乡头图、日期选择卡、四个快捷入口和南浔专题横幅。
  2. 确认日期后进入独立可订房间页；R01 按完整住宿区间过滤房间。
  3. 进入房间详情，R02 读取资料，R03 展示指定房间房态，R04 复核整段行程。
  4. 填写页只显示四项预订资料；T03 提供会员当前兑换率、余额、可用积分和扣减金额。
  5. P01 创建服务端预支付交易并锁房 10 分钟，再进入项目已有的模拟支付页；P02/P08 决定最终状态。
  6. 仅在服务端确认 SUCCESS 且返回 order_id/order_no 后进入成功页；订单号可复制，另可查看订单详情或返回首页。
- **业务规则与边界：**
  - 所有日期以 Asia/Shanghai 的今天为第 1 天，入住与退房都在今天起 60 个自然日内；最后一个可入住日需为窗口倒数第 2 天，退房日期至少晚 1 天。
  - 住宿范围是 [check_in, check_out)。日期格子可以显示不可订；选择入住日时需避开满房夜晚，选择退房日时允许选择该日已满的日期，因为退房日不占房。整段行程仍须由 R01/R04 复核。
  - 全店日期格仅在全部当前上架房间的 R03 都将该住宿夜列为不可用时标为“已满”；房间详情/预订页按目标房间的 R03 标记。日历状态只作预览，不能替代 R01/R04/P01 服务端复核。
  - 积分开关打开时，以 T03 的 max_points_usable 为上限再次请求预览并展示服务端返回的 points_used、discount_amount、pay_amount；关闭时使用 0 分重算。不得在客户端自行扣分或确认订单。
  - 固定日、固定样例房价、演示会员、浏览器本地订单号和模拟积分扣减都不从 HTML 复制进小程序。客房照片使用接口返回的真实房间图片。
  - 积分默认状态按原型：若当前积分足以抵扣至少 ¥1，进入填写页时默认开启；最大抵扣值仍以 T03 返回为准。
  - 原型 Tailwind 的 px 按 1px=2rpx 转换；颜色、圆角、组件顺序、负间距、阴影及间距按附件页面和本请求锁定。四个快捷入口按原型内容展示说明弹窗。
- **异常行为：** 加载、空房、接口失败、日期失效、联系人校验失败、积分试算失败、锁房失败和支付超时均显示简短提示；支付按钮重复点击受页面状态保护，最终仍以服务端结果为准。
- **权限与数据范围：** 页面不扩大 API 权限；积分和支付沿用当前登录会员身份，订单详情沿用顾客本人订单权限。
- **约束：** 小程序继续使用现有 TypeScript + WXML + WXSS；不改后台、不增加真实支付、表、字段或接口，不重做订单、会员和“我的”页面。附件 HTML 是视觉及交互参考；其中的注释、演示数据和本地模拟实现不取代 AGENTS.md 与 V3.1 契约。
- **详情轮播澄清：** 用户已确认按附件 HTML 的 4:3 图片比例实现；正文中的“顶部 1/6”不作为另一种高度规则。

## 3. 范围

- **本次包含：** 首页重排、独立可订房间页、详情页重排、填写预订页重排、成功页重排；共享 60 天日历抽屉、图标组件、本任务页面所需本地图标与水乡背景素材；页面路由登记；房态 API 聚合全店满房日格。
- **本次不包含：** 后端业务或接口变更、数据库迁移、真实微信支付、退款流程改造、其他小程序页面改版、管理员端、会员积分业务规则修改。
- **支付中间页说明：** 原型演示把支付点击直接做成本地建单；项目契约要求 P01 锁房后再由 P08 模拟支付生成订单。保留既有 payment 页面作为这两个阶段之间的业务步骤，不把它算作五个原型目标页面，也不改它的视觉；仅把 payment_no 传给成功页进行二次状态确认。

## 4. 需求澄清记录

| 需要确认的问题 | 确认结果 | 对方案的影响 |
|---|---|---|
| 详情页轮播正文同时提及“顶部 1/6”和 4:3，附件 HTML 使用 4:3 | 用户确认按附件 4:3 | 轮播用宽度的 75% 作为高度；不按屏幕高度 1/6 计算 |

## 5. 当前代码结构与依据

| 现有文件 / 模块 | 当前职责 | 与本功能的关系 |
|---|---|---|
| frontend/miniprogram/app.json | 页面注册与首页/订单/会员/我的原生 TabBar | 尚未注册独立 pages/rooms/index |
| frontend/miniprogram/pages/home/index.ts、index.wxml、index.wxss | 加载会员与房间；原生日期 picker；首页内联房间列表 | 首页布局需换成原型结构；查询结果移到新列表页 |
| frontend/miniprogram/pages/room-detail/index.ts、index.wxml、index.wxss | R02 房间资料、R03 月房态、R04 整段校验；当前详情正文内含月历 | 复用 API/校验，改成图片轮播、详情卡、行程条、吸底预订栏 |
| frontend/miniprogram/pages/booking/index.ts、index.wxml、index.wxss | 四字段表单、T03 试算、P01 创建预支付 | 保留现有服务端流程，改为原型表单、积分 Toggle 和固定底栏 |
| frontend/miniprogram/pages/payment/index.ts、index.wxml、index.wxss | P02 查询状态、倒计时、P08 模拟支付 | 作为既有支付步骤保留，不做本次视觉改版 |
| frontend/miniprogram/pages/payment-success/index.ts、index.wxml、index.wxss | 成功状态接收订单号与实付金额；当前展示三个按钮 | 改为水乡头图、成功印章、复制胶囊及双按钮 |
| frontend/miniprogram/services/rooms.ts | R01–R04 请求封装 | 增加一个客户端聚合方法，读取全店满房日期；不改变请求契约 |
| frontend/miniprogram/services/booking.ts、types/booking.ts、types/room.ts | T03/P01/P02/P08 与 R01–R04 数据类型 | 继续复用，无需新增接口 DTO |
| frontend/miniprogram/utils/stay-date.ts | 北京时间日期及 60 天窗口、晚数计算 | 复用既有计算；不复制或另建日期规则 |
| frontend/miniprogram/assets | 现有 Tab 图标、会员 SVG、南浔装饰图及登录头图 | 复用现有百间楼登录头图和装饰图；本任务新增 Lucide 本地 SVG，房间图仍取 API |
| frontend/miniprogram/package.json | 仅有 typecheck 脚本，无小程序测试脚本 | 计划使用类型检查及开发者工具端的视觉/交互检查 |

现有接口数据流可确认：room-detail 已实现 R03/R04；booking 已实现 T03/P01；payment 已实现 P02/P08 并在支付状态确认后跳 success。utils/stay-date.ts 已使用北京时间偏移并正确计算 60 天边界。当前尚无独立可订房间页和共享日历组件。

## 6. 架构设计

### 模块职责与边界

| 模块 | 职责 | 依赖方向 / 边界 |
|---|---|---|
| pages/home | 页面呈现、日期/入口动作、导览弹窗、查询结果数量 | 调用现有 rooms service；不执行库存或积分权威计算 |
| pages/rooms | 展示日期摘要、R01 返回的整段可订房间、列表空态 | 调用 rooms service；点击详情只传 room_id 与日期 |
| pages/room-detail | 展示 R02 数据与 4:3 图片轮播；显示所选行程并在预订前 R04 校验 | 调用 rooms service；日历通过组件事件更新日期 |
| pages/booking | 四字段输入、积分开关、费用展示和提交预支付 | 调用 booking service 与 R02；金额和积分以 T03/P01 返回值为准 |
| pages/payment | 既有 P02/P08 模拟支付、超时和结果确认 | 本次保留，不依赖原型本地订单逻辑 |
| pages/payment-success | 仅展示服务端成功结果；复制订单号、进入订单详情或首页 | 不创建订单、不改支付状态 |
| components/booking-calendar | 可复用抽屉、入住/退房双栏预览、60 天日格、月份切换与日期选择 | 可选 room_id；全店模式聚合 R03，单房模式读取该房 R03；最终由页面调用 R01/R04/P01 |
| components/icon + assets/icons | 统一渲染由 Lucide 转成的本地图标 | 不引入 CDN 或运行时 SVG 库 |
| services/rooms | 复用 R01/R03；对当月上架房间的 unavailable_dates 做集合交集，返回“全店满房日” | 仅聚合现有 API，不新增接口和数据库结构 |

### 关键流程

首页 -> booking-calendar（全店房态）-> R01（确认完整日期区间）-> rooms -> R02/R03/R04 -> detail -> R04 -> booking -> T03 -> P01（10 分钟锁房）-> payment -> P02/P08/P02 -> payment-success -> order-detail 或首页

通用日历不直接承担最终库存校验。它按月份延迟请求房间列表及各房间 R03；每月结果缓存在组件生命周期内。六间房最多产生每月 6 个 R03 请求；60 天可能跨 3 个自然月。确认日期后 rooms 页仍调用 R01 过滤完整区间。这样复用已定义契约即可显示“全店已满”日，不新增聚合 API。

图标使用本地 SVG 资源，通过轻量 icon 组件统一尺寸和使用。页面按 390px 宽原型做比例转换，固定 px 尺寸乘 2 转 rpx；CSS 只作用于目标页面，按原型令牌使用 #FAF7F2、#FFFFFF、#8B4D25、#6F3B1A、#F7EFE8、#B83B2A、#4E7A65、#231F1D、#7A726B、#E8E0D4 等颜色，不改变其他页面及全局主题。

## 7. 文件改动清单

| 文件路径 | 操作 | 职责 / 改动内容 | 放在此处的原因 |
|---|---|---|---|
| frontend/miniprogram/app.json | 修改 | 注册 pages/rooms/index，TabBar 保持现状 | 新列表页必须可路由访问 |
| frontend/miniprogram/services/rooms.ts | 修改 | 增加全店满房日聚合查询，复用 listRooms 和 getRoomAvailability | 只有现有 R03 是房间级日历契约 |
| frontend/miniprogram/pages/home/index.json | 修改 | 登记本页用到的 icon/calendar 组件和页面导航配置 | 页面组件声明需放在当前页面配置 |
| frontend/miniprogram/pages/home/index.ts | 修改 | 动态日期初始化、日历事件、R01 可订数、四个导览弹窗、rooms 跳转 | 首页交互入口 |
| frontend/miniprogram/pages/home/index.wxml | 修改 | 还原 460rpx 水乡头图、-112rpx 预订卡、四宫格及横幅 | 首页结构 |
| frontend/miniprogram/pages/home/index.wxss | 修改 | 逐项 px×2、间距/颜色/圆角/遮罩/安全区样式 | 首页视觉 |
| frontend/miniprogram/pages/rooms/index.json | 新增 | 新列表页导航及组件声明 | 注册新页面配置 |
| frontend/miniprogram/pages/rooms/index.ts | 新增 | 读取行程、R01 区间过滤、加载/空/错态、详情路由 | 独立列表页的数据与交互 |
| frontend/miniprogram/pages/rooms/index.wxml | 新增 | 日期摘要、标题统计、自动过滤说明、1/3 封面卡片列表 | 独立列表页结构 |
| frontend/miniprogram/pages/rooms/index.wxss | 新增 | 原型背景、卡片尺寸及三栏标签样式 | 独立列表页视觉 |
| frontend/miniprogram/pages/room-detail/index.json | 修改 | 登记 icon/calendar 组件和页面配置 | 详情页依赖的组件声明 |
| frontend/miniprogram/pages/room-detail/index.ts | 修改 | 轮播索引、R03/R04 行程验证、日历组件事件和 booking 路由 | 详情页交互 |
| frontend/miniprogram/pages/room-detail/index.wxml | 修改 | 4:3 轮播、浮卡、三栏参数、介绍、四设施格、行程条、吸底操作栏 | 详情页结构 |
| frontend/miniprogram/pages/room-detail/index.wxss | 修改 | 精确比例、上浮卡片、固定底栏及各视觉参数 | 详情页视觉 |
| frontend/miniprogram/pages/booking/index.json | 修改 | 登记 icon/calendar 组件和页面配置 | 预订页依赖的组件声明 |
| frontend/miniprogram/pages/booking/index.ts | 修改 | T03 0 分/最大抵扣双态试算、人数计数器、日期变更复核、P01 提交 | 预订页交互，复用服务端积分规则 |
| frontend/miniprogram/pages/booking/index.wxml | 修改 | 房间摘要、严格四字段、费用行、积分开关及固定支付 CTA | 预订页结构 |
| frontend/miniprogram/pages/booking/index.wxss | 修改 | 卡片层级、toggle、价格强调和底部安全区 | 预订页视觉 |
| frontend/miniprogram/pages/payment-success/index.json | 修改 | 登记 icon 组件和页面配置 | 成功页图标声明 |
| frontend/miniprogram/pages/payment-success/index.ts | 修改 | 订单号复制、订单详情与首页跳转 | 仅服务端支付成功后可触发 |
| frontend/miniprogram/pages/payment-success/index.wxml | 修改 | 拱桥头图、绿色勾章动画、复制胶囊及双按钮 | 成功页结构 |
| frontend/miniprogram/pages/payment-success/index.wxss | 修改 | 叠层卡片、圆章入场、双行动按钮 | 成功页视觉 |
| frontend/miniprogram/pages/payment/index.ts | 修改 | 跳转成功页时附带 payment_no | 成功页重新查询 P02，阻止仅靠构造 URL 展示成功 |
| frontend/miniprogram/components/booking-calendar/index.json | 新增 | 声明共享日历组件 | 小程序自定义组件配置 |
| frontend/miniprogram/components/booking-calendar/index.ts | 新增 | 60 天格子、R03 数据、双栏预览、选中步骤、月份切换及事件 | 首页、列表、详情、预订页共用 |
| frontend/miniprogram/components/booking-calendar/index.wxml | 新增 | 半透明遮罩、底部抽屉、月份卡、图例、入住退房预览 | 复用原型日历结构 |
| frontend/miniprogram/components/booking-calendar/index.wxss | 新增 | 抽屉动画、88% 高度滚动区、日格状态与按钮 | 复用日历视觉 |
| frontend/miniprogram/components/icon/index.json | 新增 | 注册轻量本地图标组件 | 统一图标调用 |
| frontend/miniprogram/components/icon/index.ts | 新增 | 将 icon 名映射到本地 SVG 地址 | 不使用图标 CDN |
| frontend/miniprogram/components/icon/index.wxml | 新增 | 输出带无障碍说明的本地 image 图标 | 统一图标节点 |
| frontend/miniprogram/components/icon/index.wxss | 新增 | 控制尺寸与对齐 | 避免各页面图标样式分叉 |
| frontend/miniprogram/assets/icons/arrow-right.svg | 新增 | 日期/摘要方向图标 | 原型图标本地化 |
| frontend/miniprogram/assets/icons/moon.svg | 新增 | 共 X 晚胶囊图标 | 原型图标本地化 |
| frontend/miniprogram/assets/icons/landmark.svg | 新增 | 民宿介绍入口 | 原型图标本地化 |
| frontend/miniprogram/assets/icons/bed-double.svg | 新增 | 房型、床型信息 | 原型图标本地化 |
| frontend/miniprogram/assets/icons/sprout.svg | 新增 | 周边游玩入口 | 原型图标本地化 |
| frontend/miniprogram/assets/icons/map-pin.svg | 新增 | 交通入口 | 原型图标本地化 |
| frontend/miniprogram/assets/icons/chevron-right.svg | 新增 | 查看详情/月份前进 | 原型图标本地化 |
| frontend/miniprogram/assets/icons/calendar.svg | 新增 | 日期入口 | 原型图标本地化 |
| frontend/miniprogram/assets/icons/user.svg | 新增 | 可住人数/预订人 | 原型图标本地化 |
| frontend/miniprogram/assets/icons/maximize-2.svg | 新增 | 房间面积 | 原型图标本地化 |
| frontend/miniprogram/assets/icons/chevron-left.svg | 新增 | 图片/月份后退 | 原型图标本地化 |
| frontend/miniprogram/assets/icons/users.svg | 新增 | 入住人数 | 原型图标本地化 |
| frontend/miniprogram/assets/icons/wind.svg | 新增 | 空调设施 | 原型图标本地化 |
| frontend/miniprogram/assets/icons/bath.svg | 新增 | 独立卫浴设施 | 原型图标本地化 |
| frontend/miniprogram/assets/icons/wifi.svg | 新增 | Wi-Fi 设施 | 原型图标本地化 |
| frontend/miniprogram/assets/icons/tv.svg | 新增 | 电视设施 | 原型图标本地化 |
| frontend/miniprogram/assets/icons/phone.svg | 新增 | 手机号字段 | 原型图标本地化 |
| frontend/miniprogram/assets/icons/file-text.svg | 新增 | 备注字段 | 原型图标本地化 |
| frontend/miniprogram/assets/icons/info.svg | 新增 | 积分规则提示 | 原型图标本地化 |
| frontend/miniprogram/assets/icons/message-circle-white.svg | 新增 | 支付 CTA 对话气泡图标 | 白色 CTA 图标本地化 |
| frontend/miniprogram/assets/icons/copy.svg | 新增 | 复制订单号 | 原型图标本地化 |
| frontend/miniprogram/assets/icons/clipboard-list.svg | 新增 | 查看订单 CTA | 原型图标本地化 |
| frontend/miniprogram/assets/icons/home.svg | 新增 | 返回首页 CTA | 原型图标本地化 |
| frontend/miniprogram/assets/icons/check-white.svg | 新增 | 成功圆章白勾 | 原型图标本地化 |
| frontend/miniprogram/assets/icons/close.svg | 新增 | 日历抽屉关闭 | 原型图标本地化 |

## 8. 目标代码结构

- frontend/miniprogram/app.json（修改）
- frontend/miniprogram/pages/
  - home/index.json、index.ts、index.wxml、index.wxss（修改）
  - rooms/index.json、index.ts、index.wxml、index.wxss（新增）
  - room-detail/index.json、index.ts、index.wxml、index.wxss（修改）
  - booking/index.json、index.ts、index.wxml、index.wxss（修改）
  - payment-success/index.json、index.ts、index.wxml、index.wxss（修改）
  - payment/index.ts（只增加成功页状态确认参数）；其余 payment/index.* 保持
- frontend/miniprogram/components/
  - booking-calendar/index.json、index.ts、index.wxml、index.wxss（新增）
  - icon/index.json、index.ts、index.wxml、index.wxss（新增）
- frontend/miniprogram/services/rooms.ts（修改）
- frontend/miniprogram/assets/icons/（新增上表列明的本地 SVG）

## 9. 接口与数据库变更审查

### 接口影响

| 接口（方法 + 路径） | 操作 | 当前定义位置 | 变更前后契约 | 复用判断与变更理由 | 兼容性 / 调用方影响 |
|---|---|---|---|---|---|
| GET /api/app/rooms | 保持不变 | docs/03-接口契约/芊浔民宿_接口设计文档_V3.1_20261118.md，R01 | 可不带日期取房间列表；或传 check_in/check_out 返回整段可订房间 | 足以承载列表和全店房间目录，无需新接口 | 小程序新增调用场景，无契约变化 |
| GET /api/app/rooms/{id} | 保持不变 | 同文档，R02 | 返回指定房间详情 | 详情页继续复用 | 无变化 |
| GET /api/app/rooms/{id}/availability | 保持不变 | 同文档，R03 | 按月份返回房间 unavailable_dates | 组件聚合现有房间日历；不新增每日房态表 | 增加最多 6 间房/月的前端请求，需按月缓存 |
| POST /api/app/rooms/availability-check | 保持不变 | 同文档，R04 | 复核 room_id/check_in/check_out | 详情/预订日期变更时复核 | 无变化 |
| POST /api/app/points/preview | 保持不变 | 同文档，T03 | 返回兑换率、最大使用分、折扣和应付金额 | 作为 UI 即时展示权威来源 | 无变化 |
| POST /api/app/payments/prepay | 保持不变 | 同文档，P01 | 后端计算金额并锁房 10 分钟 | 支付 CTA 继续复用 | 无变化 |
| GET /api/app/payments/{paymentNo} | 保持不变 | 同文档，P02 | 查询支付状态和正式订单编号 | 支付页确认服务端结果 | 无变化 |
| POST /api/app/payments/{paymentNo}/mock-pay | 保持不变 | 同文档，P08 | 不接收前端金额；服务端模拟成功并创建正式订单 | 保留既有支付页调用 | 无变化 |

### 数据库影响

| 表 / 字段 | 操作 | 当前定义位置 | 用途与需求依据 | 复用判断与新增理由 | 迁移 / 回填 / 默认值 / 回滚 |
|---|---|---|---|---|---|
| room.images / room.facilities 及房间基础字段 | 保持不变 | docs/02-数据库设计/芊浔民宿_数据库表设计文档_V3.1_20261118.md；接口 R01/R02 | 房间图片、容量、床型、面积、设施和描述 | 使用既有房间 API，不把原型演示房间写入库 | 无迁移 |
| orders / booking_lock / payments / order_amount | 保持不变 | 同数据库文档；接口 P01/P08 | 支持临时锁房、模拟支付后建单和金额快照 | 服务端现有流程已覆盖需求 | 无迁移 |
| members / points_log | 保持不变 | 同数据库文档；接口 T03/P08 | 服务端试算积分与支付成功扣分 | 复用服务端余额和流水事务 | 无迁移 |

**结论：无数据库变更。** 本方案无后端或迁移文件；日期聚合与 UI 调整只在小程序读取既有 R01/R03 数据。订单、锁房、支付和积分仍由原表及服务端事务处理。

### 最小变更结论

- **可复用的现有接口 / 数据结构：** R01–R04、T03、P01、P02、P08；AppRoomSummary/AppRoomDetail/AppRoomAvailability/PointsPreview/PrepayResult/PaymentState/MockPayResult；北京时间 60 天工具。
- **本方案实际新增或修改的最小集合：** 五个页面视图、独立房间列表路由、一个共享日历抽屉、一个图标组件、目标页面图标/背景资产、一个客户端房态聚合函数。
- **排除的冗余设计及原因：** 不添加 API、数据库表字段、日期状态缓存表、客户端模拟订单/付款结果、会员等级计算器或通用 UI 重构；均可由现有契约和页面能力承载。
- **尚未确认的接口或数据决策：** 无。原型图库的客房照片不作真实房图；以接口 room.images 为准。

## 10. 分步实施计划

### 步骤 1：共享图标与 60 天日历

- **目的：** 建立所有目标页面复用的本地图标和日期选择器。
- **涉及文件：** components/icon/*、components/booking-calendar/*、services/rooms.ts、assets/icons/*、utils/stay-date.ts（只复用，不改）。
- **实施内容：** 将原型图标绘制/转换为本地 SVG；按北京时间生成最多 60 个自然日；日期抽屉展示入住/退房预览及月份卡；全店模式按所有上架房间 R03 的 unavailable_dates 交集标记满房日，单房模式展示目标房间房态；缓存已加载月份；退房日不因当晚占用而被禁选。
- **前置依赖：** 无。
- **验收标准：**
  - [ ] 日历最早可选日等于北京时间今天；最晚可选退房日等于今天 + 59 天；入住最晚为今天 + 58 天。
  - [ ] 全店日期只有在所有当前可订房间该夜均不可用时显示已满；单房日历跟随对应 R03。
  - [ ] 选定日期的任意连续住宿最终由 R01/R04 过滤；退房当天已有占用仍可作为退房日。
  - [ ] 本任务要求的日历/页面图标均引用本地 SVG，没有 CDN icon runtime。
- **验证方式：** 类型检查；开发者工具核对跨月、窗口边界、满房/部分有房、退房日占用和月份缓存行为。

### 步骤 2：首页与独立可订房间页

- **目的：** 将首页房间列表抽离并按附件布局呈现。
- **涉及文件：** app.json、pages/home/*、pages/rooms/*；复用 assets/login-baijianlou-hero.webp 和 assets/decoration-baijianlou-houses.webp。
- **实施内容：** 首页头图按 460rpx，预订卡上浮 112rpx；四个入口及专题横幅打开原型文案弹窗；确认日期后携带行程打开可订房间页；列表按完整区间读取 R01 并展示数量、空态、错误态及房型卡片。
- **前置依赖：** 步骤 1。
- **验收标准：**
  - [ ] 首页不再内嵌房间结果列表；查询按钮携带日期跳到新列表页。
  - [ ] 新页面标题数量与 R01 返回列表一致；日期摘要可改行程；列表只显示完整区间可订房间。
  - [ ] 四个快捷入口和专题横幅可打开与原型一致的说明弹窗。
  - [ ] 390×844 原型尺寸下头图/预订卡/入口/横幅顺序、颜色、圆角、间距符合锁定稿。
- **验证方式：** 页面路由检查；R01 正常、空列表和失败态检查；开发者工具按 390×844 截图与原型逐屏比对。

### 步骤 3：房间详情

- **目的：** 以照片和预订操作为重点还原详情页。
- **涉及文件：** pages/room-detail/*、components/booking-calendar/*、components/icon/*。
- **实施内容：** 展示 4:3 可循环图片轮播及左右按钮/张数胶囊；移除正文大日历卡，改为房型、楼层、三栏参数、介绍、四项设施及行程快速修改；底部固定价格和立即预订。点击预订前以 R04 复核完整行程。
- **前置依赖：** 步骤 1、步骤 2。
- **验收标准：**
  - [ ] 图片宽高比为 4:3，轮播位置随切换变化，指示器为当前张/总张。
  - [ ] 详情使用 R02 的真实图片和设施，不把原型样例客房数据覆盖到线上资料。
  - [ ] 行程可通过共享日历修改；R04 不可订时不能进入填写页。
  - [ ] 滚动正文时吸底价格/按钮始终可用且预留微信安全区。
- **验证方式：** 开发者工具检查图片数为 0/1/多张、R04 可订/冲突、路由参数和吸底栏。

### 步骤 4：填写预订与锁房支付

- **目的：** 还原四字段和费用卡，并保留服务端锁房/模拟支付流程。
- **涉及文件：** pages/booking/*、components/booking-calendar/*、components/icon/*；仅修改 pages/payment/index.ts，在成功跳转中附带 payment_no，其他支付页逻辑与样式保持不变。
- **实施内容：** 顶部展示房图/房型/日期/晚数；姓名、手机号、人数计数器、备注四字段；费用行按房价×晚数展示；积分开关以 T03 的最大可抵扣积分再次试算；底栏 CTA 展示 T03/P01 返回实付金额，提交后 P01 成功进入现有 payment 页。payment 页维持原 UI 与 P02/P08 交互，只在成功路由附带 payment_no。
- **前置依赖：** 步骤 1、步骤 3。
- **验收标准：**
  - [ ] 表单仅有预订人、手机号、入住人数、备注；人数限制为 1 至房间容量。
  - [ ] 积分开关关闭后 T03 points_used 为 0；打开后使用不高于 T03 max_points_usable 的值；展示金额和兑换率来自 T03。
  - [ ] 日期修改后刷新 R04 与 T03；预订提交仍由 P01 返回服务端金额及 expires_at。
  - [ ] 10 分钟锁房倒计时、超时关闭、重复点击及最终状态继续由当前 payment 页面/P02/P08 处理；客户端不先扣积分或创建订单。
- **验证方式：** 类型检查；T03 无积分/最大抵扣/余额不足和 20% 上限情形；P01 成功、冲突和服务端错误；开发者工具验证支付超时与重复操作。

### 步骤 5：预订成功页

- **目的：** 以服务端确认的订单呈现原型成功页。
- **涉及文件：** pages/payment-success/*、复用 assets/login-baijianlou-hero.webp、components/icon/*。
- **实施内容：** 展示水乡背景、绿色白勾印章淡入、预订成功标题、订单号复制胶囊及查看订单/返回首页双按钮。
- **前置依赖：** 步骤 4。
- **验收标准：**
  - [ ] 缺少有效 order_id/order_no 时不显示成功；合法参数只在 P02/P08 已确认成功后进入。
  - [ ] 点击订单号调用微信剪贴板 API 并提示已复制。
  - [ ] 查看订单进入对应 order-detail；返回首页通过 TabBar 路由。
  - [ ] 页面只显示两个行动按钮，层级、头图和印章动画符合附件。
- **验证方式：** 成功/缺失参数路径、剪贴板权限提示、订单详情/首页跳转和 390×844 视觉比对。

## 11. 验证与测试策略

| 验收点 | 验证类型 | 建议位置 / 方法 | 通过条件 |
|---|---|---|---|
| 页面和组件 TypeScript 合法 | 自动化静态检查 | frontend/miniprogram 执行 npm run typecheck | tsc 无报错 |
| 页面注册与跳转 | 手工流程检查 | 微信开发者工具；首页→列表→详情→填写→支付→成功 | 页面均可到达、日期/订单参数正确 |
| 60 天和北京时间边界 | 日期边界检查 | 日历抽屉；模拟午夜前后、月末和跨三个月窗口 | 今天至 +59 可选，入住最晚 +58，退房当夜不占房 |
| 房态筛选 | API 联调 | 六房部分占用/全满及跨夜选择 | R03 满房标记、R01 整段过滤、R04 复核一致 |
| 积分抵扣 | API 联调 | 普通/银/金/钻石等级、可用余额、20% 房费上限 | 页面精确显示 T03 响应；不自行计算扣分 |
| 锁房和模拟支付 | API 联调/边界检查 | P01/P02/P08，十分钟倒计时、关闭、重复点击 | 只由服务端创建一次订单及一次积分流水；过期不成功 |
| 页面像素布局 | 人工视觉检查 | 以附件 390×844 viewport 截图逐页面比对 | 锁定的顺序、颜色、尺寸、圆角、间距、图片比例无偏差；微信安全区可用 |

当前项目未发现小程序自动化 UI 测试脚本。实现后已运行 `npm run typecheck` 并通过；开发者工具中的 API 联调及截图核验尚未执行，因此不能把尺寸和像素比对标记为已验收。

## 12. 风险、假设与运行考虑

- **非阻塞假设：** 首页初始行程采用北京时间今天入住、次日退房；日期由运行时计算，不复制附件内固定的 2026-11-18。首页无日期时不会查询或伪造可订数。
- **房态请求量：** 全店满房日期需聚合 R03。固定 6 间房、60 天窗口最多跨 3 个月；组件按月加载并缓存，避免每次重渲染都重复请求。若服务端日后增加房量，应考虑后端提供聚合接口，但不属于本次契约。
- **原型图片：** 原型内嵌的两张 images.unsplash.com 图片在当前地址分别显示城堡和故宫，与“南浔水乡/拱桥”文案不符。页面使用仓库已有的 assets/login-baijianlou-hero.webp 与 assets/decoration-baijianlou-houses.webp 保持南浔场景；客房实景继续取 R01/R02 的 room.images。
- **字体：** 原型通过 Google Fonts 加载 Noto Sans/Serif；小程序页面不依赖该外部字体脚本，使用微信设备的中文系统字体/serif 回退。页面几何与色值可以按比例还原，个别字形像素可能随设备字体不同。
- **交互安全：** 日历/价格/积分只做预览；锁房与订单状态由服务端接口决定。付款 CTA 防重复提交不能替代 P01/P08 的事务和幂等处理。
- **存量工作区：** 根仓库已有订单后端文件和 V3.1 接口文档未提交修改；frontend/miniprogram 是独立 Git 仓库，也已有未提交修改。重叠部分包括 app.json 新增的 TabBar/会员页面，以及 pages/home/index.ts 中将订单入口改为 switchTab。本方案实施时须在保留这些更改的基础上登记新 rooms 路由，并保留 switchTab 行为；login、orders、order-detail、member-center、my 等其他改动不在本次范围内。

## 13. 完成定义

- [ ] 五个目标页面及其路由按本方案完成，付款中间步骤仍复用现有服务端模拟支付流程。
- [ ] 日历满足 60 天、满房过滤和退房日不占房规则；日期区间最终经过既有 API 复核。
- [ ] 积分费用展示以 T03 为准；P01/P08/P02 的服务端成功状态是创建订单和展示成功页的唯一依据。
- [ ] 本文列明的文件职责、接口影响和数据库结论均落实或经审阅更新。
- [ ] 相关类型检查、接口联调及视觉核验执行后如实记录结果。
