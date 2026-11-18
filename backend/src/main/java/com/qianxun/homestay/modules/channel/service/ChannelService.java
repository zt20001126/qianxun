package com.qianxun.homestay.modules.channel.service;

import com.qianxun.homestay.common.exception.BusinessException;
import com.qianxun.homestay.common.util.ApiTime;
import com.qianxun.homestay.modules.channel.entity.ChannelEntity;
import com.qianxun.homestay.modules.channel.mapper.ChannelMapper;
import com.qianxun.homestay.modules.channel.vo.ChannelVO;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 固定七渠道的查询与启停。 */
@Service
public class ChannelService {

    private final ChannelMapper channelMapper;

    public ChannelService(ChannelMapper channelMapper) {
        this.channelMapper = channelMapper;
    }

    public List<ChannelVO> findChannels() {
        return channelMapper.findAll().stream().map(this::toVO).toList();
    }

    public ChannelVO setEnabled(Long id, boolean enabled) {
        requireChannel(id);
        channelMapper.updateEnabled(id, enabled);
        return toVO(channelMapper.findById(id));
    }

    public ChannelEntity requireChannel(Long id) {
        ChannelEntity channel = channelMapper.findById(id);
        if (channel == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "渠道不存在");
        }
        return channel;
    }

    private ChannelVO toVO(ChannelEntity channel) {
        return new ChannelVO(
                channel.getId(), channel.getName(), channel.getEnabled(),
                ApiTime.fromShanghai(channel.getCreatedAt()), ApiTime.fromShanghai(channel.getUpdatedAt()));
    }
}
