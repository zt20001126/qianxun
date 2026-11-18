package com.qianxun.homestay.modules.channel.service;

import com.qianxun.homestay.modules.channel.entity.ChannelEntity;
import com.qianxun.homestay.modules.channel.vo.ChannelVO;
import java.util.List;

/** 固定七渠道的查询与启停业务接口。 */
public interface ChannelService {

    /** 查询全部渠道。 */
    List<ChannelVO> findChannels();

    /** 更新渠道启用状态。 */
    ChannelVO setEnabled(Long id, boolean enabled);

    /** 查询渠道，不存在时抛出业务异常。 */
    ChannelEntity requireChannel(Long id);
}
