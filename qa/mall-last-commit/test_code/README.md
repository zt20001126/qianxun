# 可执行测试代码

新增的商城 HTTP 全链路测试位于项目原生 Maven 测试目录：

- `backend/src/test/java/com/qianxun/homestay/modules/mall/MallPurchaseShipmentApiIntegrationTest.java`

它以 Testcontainers 启动独立 MySQL 8，并通过 MockMvc 执行管理员商品管理、会员购买、管理员发货、会员物流可见、完成奖励、权限、参数、退款和并发状态变更。

执行命令：

```powershell
cd backend
mvn -s maven-settings/aliyun.xml -Dtest=MallPurchaseShipmentApiIntegrationTest test
```

全量回归：

```powershell
cd backend
mvn -s maven-settings/aliyun.xml test
```

