package com.qianxun.homestay.modules.creator.service;

import com.qianxun.homestay.modules.creator.dto.CreatorQuery;
import com.qianxun.homestay.modules.creator.dto.CreatorSaveRequest;
import com.qianxun.homestay.modules.creator.vo.CreatorVO;
import java.util.List;

/** 达人资料查询、创建、修改与启停业务接口。 */
public interface CreatorService {

    /** 按渠道、启用状态和名称筛选达人。 */
    List<CreatorVO> findCreators(CreatorQuery query);

    /** 查询单个达人资料。 */
    CreatorVO getCreator(Long id);

    /** 创建达人资料。 */
    CreatorVO createCreator(CreatorSaveRequest request);

    /** 更新达人资料。 */
    CreatorVO updateCreator(Long id, CreatorSaveRequest request);

    /** 更新达人启用状态。 */
    CreatorVO setEnabled(Long id, boolean enabled);
}
