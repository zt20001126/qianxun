# 订单预订人画像实现方案

> 日期：2026-11-20（Asia/Shanghai）
> 状态：按用户后续确认修订
> 最新口径：一手机号一画像；画像表保存资料、管理员备注和已选标签 JSON；标签选项另用一张共享字典表；订单用户备注保留在订单并在画像页汇总。

## 1. 目标与背景

- **要解决的问题：** 当前实际数据库没有预订人年龄、群体分类或画像标签的存储结构，也没有画像采集、标签维护和统计入口。
- **功能目标：** 管理端按预订人手机号维护唯一画像；第三方订单创建和办理入住时可录入画像；订单备注与画像资料可在之后持续编辑。
- **预期最终效果：** 同手机号订单共用一份画像；群体、所选标签和画像级管理员备注保存在画像行内；系统预置和管理员新增标签保存在共享标签字典；每笔订单备注仍保存在订单记录中。

## 2. 已确认需求

- **使用者 / 调用方：** 管理员；画像数据只在管理端维护和查看。
- **触发方式：** 正常订单在办理入住时填写；第三方订单创建时可填写；订单详情中可继续修改，包括订单已完成后。
- **输入：** 年龄、单选群体分类、多选标签、画像级管理员备注。姓名和手机号沿用订单已有预订人字段；手机号为空时画像按订单独立保存。
- **身份证号处理：** 管理端可以提供临时身份证号输入框以计算年龄；身份证号不写入数据库，也不发送给后端。年龄可由管理员直接输入。年龄按订单入住日计算。
- **标签：** 画像所选标签名称保存在 JSON 数组；全局选项由系统预置并允许管理员新增，可被所有画像复用。
- **订单关系：** 非空手机号直接关联画像；手机号为空时画像表的 order_id 关联单笔订单。
- **修改范围：** 画像允许在入住、退房、完成等订单阶段修改；修改画像不触发订单状态、金额、积分或库存变化。
- **第三方订单：** 创建时允许选择待确认、待入住、已入住或已完成状态；无论初始状态如何，创建表单均可填写画像。

## 3. 范围

- **本次包含：** 手机号画像表与共享标签选项表、JSON 标签、后台订单创建/办理入住/画像编辑、所有关联订单备注编辑及需求/数据库/接口文档。
- **本次不包含：** 身份证号持久化或后端接收；小程序顾客端收集画像；实际入住人名单和证件登记；跨订单个人身份去重；画像导出；自动推断群体或标签。

## 4. 方案建议与审阅项

| 项目 | 建议方案 | 影响 |
|---|---|---|
| 画像主记录 | 按非空手机号建立并复用画像；手机号为空时每笔订单独立建档 | 姓名不参与身份识别；手机号是系统内归并键，不证明号码所有权 |
| 群体分类 | 单选“主要出行结构”，固定选项：独自出行、情侣/夫妻、亲子家庭、家庭/亲友、朋友结伴、商务/差旅、团队/团体、其他；未知留空 | 固定枚举便于稳定统计；如果以后需要管理员自定义分类，再增加分类字典表 |
| 标签 | 预置古镇游览、自驾、晚到、亲子设施、长住、复住；管理员可新增全局标签 | 可选项存 `portrait_tag`，所选名称存画像 JSON |
| 年龄 | 保存入住日时的整数年龄；统计报表按年龄段汇总：0–17、18–24、25–34、35–44、45–54、55+ | 未知年龄为空；身份证号仅在管理端页面内存中解析，不进入请求、日志或浏览器持久化存储 |
| 必填性 | 年龄、群体分类、标签均允许未填或部分填写；不阻断创建订单或办理入住，可之后补录 | 避免未知信息被迫猜填；报表显示未填写数量和覆盖率 |
| 统计口径 | 月份按入住日归属；取消订单不纳入画像分布；复用现有订单范围筛选 | 报表统计的是订单画像条目，不是按真实自然人去重的人数 |

用户已确认手机号作为系统内顾客记录的唯一归并键；这只是业务识别规则，不代表手机号已验证或能证明现实身份。

## 5. 当前代码结构与依据

| 现有文件 / 模块 | 当前职责 | 与本功能的关系 |
|---|---|---|
| backend/src/main/resources/db/migration/V4__create_availability_tables.sql | 创建 orders 表和支付锁房表 | 实际 orders 表只有 guest_name、phone、guest_count 等联系人字段，没有年龄或画像字段 |
| 实际运行的 qianxun MySQL 数据库 | 当前项目数据库 | 只读查询 information_schema 确认 orders 字段；未发现 order_guest 或画像表；未查询顾客记录 |
| backend/src/main/java/com/qianxun/homestay/modules/order | 订单创建、查询、修改及状态操作 | 第三方订单创建允许指定初始状态；详情和画像采集均在后台订单流程中 |
| backend/src/main/java/com/qianxun/homestay/modules/order/controller/AdminOrderController.java | 后台订单列表、详情、创建、更新、确认、入住、退房和取消接口 | 复用现有创建、详情、入住入口，画像修改使用独立订单画像资源 |
| backend/src/main/java/com/qianxun/homestay/modules/order/service/impl/OrderCommandServiceImpl.java | 事务中创建第三方订单并处理订单状态 | 创建订单时已使用事务，可在订单落库后保存可选画像 |
| backend/src/main/java/com/qianxun/homestay/modules/order/mapper/OrderMapper.java 与 backend/src/main/resources/mapper/order/OrderMapper.xml | 订单持久化和详情查询 | 订单详情需关联画像数据；列表不需要新增个人画像字段 |
| frontend/admin/src/components/operations/ExternalOrderCreateModal.vue | 创建第三方订单并选择初始状态 | 增加可选画像区，覆盖直接创建为已入住或已完成的订单 |
| frontend/admin/src/components/operations/OrderDetailModal.vue | 编辑订单资料及确认/入住/退房动作 | 办理入住时采集画像；详情中支持之后编辑，包括已完成订单 |
| backend/src/main/java/com/qianxun/homestay/modules/report 与 frontend/admin/src/views/reports/index.vue | 后台经营报表 API 和标签页 | 复用月度报表页面新增画像统计维度 |
| backend/src/test/java/com/qianxun/homestay/modules/order/AdminOrderApiIntegrationTest.java | Testcontainers MySQL 订单 API 集成测试 | 覆盖画像与订单事务、状态和完成后修改 |
| backend/src/test/java/com/qianxun/homestay/modules/report/ReportServiceImplTest.java | 报表服务统计测试 | 覆盖画像分布口径；可补数据库查询集成用例 |

当前后台第三方订单创建接口为 POST /api/admin/orders，详情为 GET /api/admin/orders/{id}，修改为 PUT /api/admin/orders/{id}，办理入住为 POST /api/admin/orders/{id}/check-in。当前报表采用月份和订单范围筛选。管理后台项目使用 Spring Boot、MyBatis XML、Flyway、Vue 3 和 TypeScript。

## 6. 架构设计

### 模块职责与边界

| 模块 | 职责 | 依赖方向 / 边界 |
|---|---|---|
| modules/portrait（新增） | 维护手机号唯一画像、JSON 标签、共享标签选项及关联订单备注 | 有手机号时通过 orders.phone 关联；手机号为空时使用画像表 order_id |
| modules/order | 在第三方订单创建和办理入住流程中调用画像服务；订单详情响应附带画像 | order 依赖 portrait service；画像服务不依赖 order service，避免循环依赖 |
| modules/report | 查询画像完整率、年龄段、群体分类和标签分布 | 只读画像表及订单表，继续遵循报表 Service/Mapper/XML 分层 |
| frontend/admin | 订单表单、画像编辑、标签配置和画像统计 | 身份证号只在画像表单组件本地短暂使用；接口类型中不定义身份证号字段 |

### 数据流

第三方创建表单 / 办理入住表单 → 管理端画像表单 → 画像接口 DTO（不含身份证号）→ 订单 Service 事务 → 画像 Service → MyBatis Mapper/XML → MySQL

订单详情 → 订单查询 Service + 画像查询 → 画像 VO 与现有订单联系人字段组合 → 订单详情弹窗

画像报表筛选 → ReportController → ReportService → ReportMapper/XML 聚合查询 → 年龄、群体及标签统计

### 群体分类建议

使用稳定英文编码保存，中文只作为管理端展示文案：SOLO、COUPLE、PARENT_CHILD、FAMILY_FRIENDS、FRIENDS、BUSINESS、GROUP、OTHER。一个订单只选一个主要类型；未知以 null 表示，不把“未知”作为有效客群。标签补充交叉特征和服务需求。

## 7. 接口影响

| 接口 | 操作 | 契约建议 | 兼容性 / 调用方影响 |
|---|---|---|---|
| GET /api/admin/orders/{id} | 修改 | 响应增加 booker_portrait：age、party_type、tags[]；空画像返回空值与空标签列表 | 新字段为附加响应，旧字段不变 |
| POST /api/admin/orders | 修改 | 请求增加可选 booker_portrait；第三方订单所有允许的初始状态都可录入 | 旧请求不传该对象时行为不变 |
| POST /api/admin/orders/{id}/check-in | 修改 | 接受可选 booker_portrait，随办理入住事务保存；画像为空时仍允许办理入住 | 旧的无请求体调用继续可用 |
| PUT /api/admin/portraits/{id} | 修改 | 更新画像资料、画像级管理员备注、JSON 标签和该手机号全部关联订单的两类备注 | 单事务保存；订单状态和金额不可修改 |
| GET/POST /api/admin/portrait-tags | 新增 | 查询预置标签并新增全局可复用标签 | 画像只提交字典中现有标签名称 |
| GET /api/admin/reports/portraits | 新增 | 按 month、scope 返回画像总量/填写率、年龄段、群体和标签分布 | 放入现有经营报表的画像维度 |

不改变小程序下单表单、会员积分规则、现有订单金额接口和第三方订单状态编码。接口扩展文档应记录未填、重复标签、订单归属校验和越权访问的错误处理。

## 8. 数据库影响

项目当前最高 Flyway 迁移为 V10；V11–V13 逐步迁移，不修改已应用迁移。画像功能最终只新增 `booker_profile` 和 `portrait_tag` 两张表；订单备注仍保留在现有 `orders` 表。

| 表 / 字段 | 操作 | 设计 | 理由与迁移处理 |
|---|---|---|---|
| booker_profile | 新增 | 唯一手机号、姓名、群体、年龄、地区、tags JSON；手机号为空时使用唯一 order_id | 一手机号一画像；订单通过 phone 直接关联，无需画像订单关联表 |
| booker_profile_order、booker_profile_tag | V13 移除 | 订单通过手机号直接匹配；标签关联迁入 booker_profile.tags | 画像不再依赖订单关联表或标签关联表 |
| portrait_tag | 保留 | 保存预置及管理员新增的共享标签选项 | 管理员新增的标签可供其他画像选择 |
| booker_profile.admin_remark | V13 新增 | 保存手机号级管理员长期备注 | 与每笔订单的 orders.admin_remark 区分 |
| orders.guest_name、orders.phone、orders.remark、orders.admin_remark | 保持 | 保留每笔订单的联系人姓名、手机号、用户备注、管理员备注 | 订单备注逐笔保存并可持续编辑；手机号用于关联画像 |
| order_guest | 不新增 | 不使用 | 实际数据库不存在此表；该功能记录的是预订人，不是实际入住人列表 |

V11/V12 回填历史画像，V13 将标签转入 JSON、保留历史标签选项、手机号画像按手机号收敛、无手机号画像以 order_id 保留，并移除订单/标签关联表。有效年龄范围为 0–120；未知年龄允许 null。群体编码使用集中枚举。

## 9. 文件改动清单

| 文件路径 | 操作 | 职责 / 改动内容 |
|---|---|---|
| backend/src/main/resources/db/migration/V11__create_booker_profiles.sql | 新增 | 初始画像扩展结构 |
| backend/src/main/resources/db/migration/V12__identify_booker_profiles_by_phone.sql | 新增 | 同手机号画像归并 |
| backend/src/main/resources/db/migration/V13__store_portrait_tags_and_order_fallback_in_profile.sql | 新增 | 标签 JSON、订单备注编辑所需字段及单画像表收敛 |
| backend/src/main/java/com/qianxun/homestay/modules/portrait/controller/AdminBookerPortraitController.java | 新增 | 管理端画像查询、持续编辑和标签选项路由 |
| backend/src/main/java/com/qianxun/homestay/modules/portrait/service/BookerPortraitService.java | 新增 | 画像及订单备注业务接口 |
| backend/src/main/java/com/qianxun/homestay/modules/portrait/service/impl/BookerPortraitServiceImpl.java | 新增 | 按手机号归并画像、保存 JSON 标签、管理共享标签及订单备注 |
| backend/src/main/java/com/qianxun/homestay/modules/portrait/mapper/BookerPortraitMapper.java | 新增 | 画像、共享标签、订单备注查询与更新声明 |
| backend/src/main/resources/mapper/portrait/BookerPortraitMapper.xml | 新增 | 按手机号关联订单和编辑备注 SQL |
| backend/src/main/java/com/qianxun/homestay/modules/portrait/dto | 新增 | 画像与订单备注请求 DTO；不定义身份证号字段 |
| backend/src/main/java/com/qianxun/homestay/modules/portrait/vo | 新增 | 画像、订单摘要和备注编辑视图 |
| backend/src/main/java/com/qianxun/homestay/modules/order | 修改 | 订单创建、入住和详情接入画像；订单原备注仍逐笔保存 |
| frontend/admin/src/api/portraits.ts、frontend/admin/src/types/portraits.ts | 新增 | 手机号画像和持续编辑契约 |
| frontend/admin/src/components/operations/BookerPortraitFields.vue | 新增 | 群体、年龄、地区和标签选择/新增 |
| frontend/admin/src/components/operations/BookerPortraitEditModal.vue | 新增 | 编辑画像及所有关联订单的两类备注 |
| frontend/admin/src/views/portraits/index.vue | 新增 | 按手机号查看画像与已完成订单摘要 |
| frontend/admin/src/components/operations/ExternalOrderCreateModal.vue、OrderDetailModal.vue | 修改 | 创建、入住和订单详情使用画像资料 |
| docs/01-需求、docs/02-数据库设计、docs/03-接口契约 | 修改 | 同步手机号唯一、一表标签 JSON 和可持续编辑口径 |

## 10. 目标代码结构

modules/portrait/
- controller/AdminBookerPortraitController.java
- service/BookerPortraitService.java 与 service/impl/BookerPortraitServiceImpl.java
- mapper/BookerPortraitMapper.java 与 mapper XML
- entity/BookerProfileEntity.java（手机号画像单表映射）
- dto/BookerPortraitSaveRequest.java、BookerPortraitUpdateRequest.java、BookerPortraitOrderRemarkUpdateRequest.java
- vo/BookerPortraitVO.java、BookerPortraitListItemVO.java、BookerPortraitEditableOrderVO.java
- enums/BookerPartyType.java

现有 order 模块（修改订单创建、入住、详情）；现有 report 模块（新增画像统计）；管理后台复用订单弹窗及经营报表页面，不新增路由。

## 11. 分步实施计划

### 步骤 1：确认业务扩展口径并更新契约

- **涉及文件：** 三份 V3.3 扩展文档、docs/README.md。
- **实施内容：** 固定手机号唯一画像、JSON 所选标签、预置及管理员新增标签、画像级管理员备注、订单备注汇总与逐单编辑、身份证号仅本地解析及已完成订单统计口径。
- **验收标准：** [ ] 需求、数据库、接口三份扩展对画像归属、可空字段、标签停用和统计口径描述一致。
- **验证方式：** 文档交叉检查。

### 步骤 2：新增 Flyway 画像表

- **涉及文件：** V11–V13 迁移。
- **实施内容：** 在画像表内保存手机号、order_id 兜底、画像级管理员备注和 JSON 标签；共享字典表维护标签选项；回填历史标签后移除订单/标签关联表。
- **前置依赖：** 步骤 1。
- **验收标准：** [ ] 每个非空手机号只有一条画像；无手机号时按订单关联；标签 JSON 可往返保存；旧订单和标签迁移后仍可读取。
- **验证方式：** Testcontainers MySQL 8 Flyway 迁移和约束集成检查。

### 步骤 3：实现画像与标签 API

- **涉及文件：** modules/portrait 全部新增文件。
- **实施内容：** 实现管理端画像保存/读取、标签查询/新增和订单备注持续编辑；API DTO 不含身份证号；非法年龄、群体和重复标签拒绝保存。
- **前置依赖：** 步骤 2。
- **验收标准：** [ ] 群体、标签及两类订单备注可多次修改；备注订单必须属于当前手机号画像；未授权请求被拒绝。
- **验证方式：** 新增 MySQL API 集成测试，覆盖正常、参数错误、权限和重复提交。

### 步骤 4：接入订单创建、入住和编辑

- **涉及文件：** AdminOrderController、CreateExternalOrderRequest、OrderCommandService/Impl、OrderQueryServiceImpl、OrderDetailVO、订单 API 集成测试。
- **实施内容：** 第三方订单创建和办理入住可附带画像；订单详情返回手机号画像；画像更新可在一个事务中更新群体、标签和任意状态订单备注。
- **前置依赖：** 步骤 3。
- **验收标准：** [ ] 同手机号订单读取同一画像；已完成订单和任意状态订单备注可持续编辑；失败时画像和备注回滚；金额、积分和房态不变化。
- **验证方式：** AdminOrderApiIntegrationTest 使用真实 MySQL Testcontainers。

### 步骤 5：实现画像统计

- **涉及文件：** report Controller、Service、Mapper/XML、PortraitReportVO、ReportServiceImplTest。
- **实施内容：** 增加月度画像报表，按入住月份和现有订单范围统计年龄段、分类、标签分布及画像覆盖率；排除取消订单。
- **前置依赖：** 步骤 2。
- **验收标准：** [ ] 多标签订单在各标签下分别计数；年龄为空进入未填写数；统计不重复读取订单金额；取消订单不进入分布。
- **验证方式：** 服务层边界测试及 MySQL SQL 集成验证。

### 步骤 6：接入管理端表单与报表

- **涉及文件：** 管理端 types/API、BookerPortraitFields、BookerPortraitEditModal、ExternalOrderCreateModal、OrderDetailModal 和 portraits/index.vue。
- **实施内容：** 提供可直接添加/删除的画像标签；画像编辑弹窗持续编辑群体、标签及所有关联订单备注。
- **前置依赖：** 步骤 3–5。
- **验收标准：** [ ] 输入身份证号仅在浏览器内计算年龄；请求不含身份证号；手机号画像可以重复编辑；订单备注和画像资料保存后重新读取一致。
- **验证方式：** 管理端构建和人工界面检查；检查请求载荷与浏览器存储中不含身份证号。

## 12. 验证与测试策略

| 验收点 | 验证类型 | 建议位置 / 方法 | 通过条件 |
|---|---|---|---|
| Flyway 表结构与外键 | MySQL 集成 | Testcontainers 启动完整迁移 | V11 可重复用于全新测试库；关联约束符合设计 |
| 订单创建/办理入住画像事务 | API 集成 | AdminOrderApiIntegrationTest | 创建或状态变更失败时画像和订单状态无部分提交 |
| 完成后画像修改 | API 集成 | AdminPortraitApiIntegrationTest | COMPLETED 订单可改年龄、分类及标签，金额保持原值 |
| 身份证号不出浏览器 | 前端人工与请求检查 | 管理端开发环境网络请求、localStorage/sessionStorage 检查 | 请求、日志和持久化存储中无身份证号 |
| 标签维护与历史标签 | API 集成 | AdminPortraitApiIntegrationTest | 停用标签不出现在新选择列表，但历史统计仍能识别 |
| 画像报表 | 单元 + MySQL 集成 | ReportServiceImplTest 和 mapper SQL 集成测试 | 月份、状态、空值、多标签、取消单口径符合文档 |
| 管理端页面 | 构建 + 人工检查 | npm run build、订单弹窗和报表页面 | 第三方状态可选创建；入住与完成后可编辑；加载/错误/空态可见 |

本方案尚未执行上述测试；测试安排属于后续实施阶段。

## 13. 风险、假设与运行考虑

- **手机号画像归并规则：** 非空手机号按去首尾空格后的值归并；手机号变更后订单转关联到新手机号画像，不把原画像属性和标签复制给新手机号。手机号为空时按订单单独归档。
- **画像字段允许不完整是假设：** 办理入住不因年龄或标签缺失而失败；报表提供覆盖率，避免管理员猜填。
- **群体分类和报表口径为待审阅建议：** 分类选项固定；报表按入住月份统计；取消订单排除。
- **身份证号安全：** 不定义后端字段，不在 API 请求、访问日志、错误日志、数据库或浏览器持久化中出现；仅在输入组件内存中短暂解析。
- **数据范围：** 画像仅描述预订人，不代表所有实际入住人；不替代住宿实名登记流程。
- **迁移与回滚：** V11 只新增表，无破坏性 DDL 和数据回填。应用回滚时先停止新写入；已产生画像数据的回退需保留表或制定单独数据处置方案，不直接删表。

## 14. 完成定义

- [ ] 用户审阅并确认群体选项、必填性和统计口径。
- [ ] 新增表及代码按本方案实现，订单及报表 API 与 V3.3 扩展契约一致。
- [ ] 身份证号只用于前端计算年龄，不进入后端或任何持久化位置。
- [ ] 第三方订单初始已入住/已完成仍可录入画像，完成订单画像可修改。
- [ ] 本文列出的集成、权限、隐私和前端验证均执行并如实记录结果。
