-- 默认商品先下架，管理员检查商品信息后再上架销售。
INSERT INTO mall_product (name, description, images, price, status)
VALUES (
    '蚕丝被',
    '蚕丝被',
    JSON_ARRAY(
        'https://qianxunminsu.oss-cn-shanghai.aliyuncs.com/mall/products/silk-quilt/61b9f757-520b-4efd-88dc-b76f77ccd324.png',
        'https://qianxunminsu.oss-cn-shanghai.aliyuncs.com/mall/products/silk-quilt/926eb7be-b27c-4503-8aa8-a0032da9402b.png'
    ),
    1.00,
    'OFF_SHELF'
);
