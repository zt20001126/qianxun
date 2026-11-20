# 可执行测试代码

本项目已有 Spring Boot/JUnit 5 测试位于 `backend/src/test/java`，使用真实 MySQL 8 Testcontainers 和 `MockMvc`。本轮新增的后台日历 HTTP 集成断言位于 `AvailabilityApiIntegrationTest`；按渠道报表 HTTP 断言补入 `ExpenseReportApiIntegrationTest`。没有复制测试源文件，以免同一测试类在 Maven 中重复编译。

关键测试类：

- `modules/auth/AuthApiIntegrationTest`
- `modules/availability/AvailabilityApiIntegrationTest`
- `modules/payment/PaymentFlowApiIntegrationTest`
- `modules/order/AdminOrderApiIntegrationTest`
- `modules/member/MemberPointsApiIntegrationTest`
- `modules/refund/RefundApiIntegrationTest`
- `modules/expense/ExpenseReportApiIntegrationTest`
- `modules/report/ReportServiceImplTest`

从仓库根目录运行本目录的 `run-all.ps1`，依次执行后端全量测试、管理后台生产构建和小程序类型检查。脚本调用项目自身测试，不需要复制源代码。

