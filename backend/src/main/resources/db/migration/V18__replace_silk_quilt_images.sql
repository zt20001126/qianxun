-- 将 V17 初始化的蚕丝被商品图片替换为新的五张公开 OSS 图片；保留商品其他资料和上架状态。
UPDATE mall_product
SET images = JSON_ARRAY(
    'https://qianxunminsu.oss-cn-shanghai.aliyuncs.com/mall/products/silk-quilt/0613e860-2066-4315-90f3-623a2729915b.png',
    'https://qianxunminsu.oss-cn-shanghai.aliyuncs.com/mall/products/silk-quilt/745a0006-eb78-4e8f-92bb-2a1c5a7897da.png',
    'https://qianxunminsu.oss-cn-shanghai.aliyuncs.com/mall/products/silk-quilt/7cae336f-036b-44e0-82b5-1a1e91b837ab.png',
    'https://qianxunminsu.oss-cn-shanghai.aliyuncs.com/mall/products/silk-quilt/8e718161-c03c-4cb2-a599-335e45bcd87f.png',
    'https://qianxunminsu.oss-cn-shanghai.aliyuncs.com/mall/products/silk-quilt/91a9696e-f40f-4bf1-907f-4670f104eafe.png'
)
WHERE JSON_CONTAINS(
    images,
    JSON_QUOTE('https://qianxunminsu.oss-cn-shanghai.aliyuncs.com/mall/products/silk-quilt/61b9f757-520b-4efd-88dc-b76f77ccd324.png')
)
  AND JSON_CONTAINS(
    images,
    JSON_QUOTE('https://qianxunminsu.oss-cn-shanghai.aliyuncs.com/mall/products/silk-quilt/926eb7be-b27c-4503-8aa8-a0032da9402b.png')
);
