# 可执行测试代码

测试放在项目原有测试目录：

- `backend/src/test/java/com/qianxun/homestay/modules/mall/MallImageCosApiIntegrationTest.java`：上传 HTTP 接口与权限。
- `backend/src/test/java/com/qianxun/homestay/modules/mall/MallProductImageUploadServiceImplTest.java`：上传类型校验及 COS 错误。
- `backend/src/test/java/com/qianxun/homestay/modules/storage/CosStorageServiceImplTest.java`：COS 地址、配置、签名与分片。
- `backend/src/test/java/com/qianxun/homestay/modules/room/RoomImageUrlServiceImplTest.java`：旧客房引用。
- `backend/src/test/java/com/qianxun/homestay/modules/mall/MallPurchaseShipmentApiIntegrationTest.java`：商品初始化迁移及商城业务。

执行：`mvn -s backend/maven-settings/aliyun.xml -f backend/pom.xml test`。HTTP 集成测试使用 Docker MySQL 8 容器和模拟 COS；不连接真实云端或生产数据库。
