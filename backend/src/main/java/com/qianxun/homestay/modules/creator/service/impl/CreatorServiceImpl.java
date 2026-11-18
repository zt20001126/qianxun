package com.qianxun.homestay.modules.creator.service.impl;

import com.qianxun.homestay.common.exception.BusinessException;
import com.qianxun.homestay.common.util.ApiTime;
import com.qianxun.homestay.modules.channel.service.ChannelService;
import com.qianxun.homestay.modules.creator.dto.CreatorQuery;
import com.qianxun.homestay.modules.creator.dto.CreatorSaveRequest;
import com.qianxun.homestay.modules.creator.entity.CreatorEntity;
import com.qianxun.homestay.modules.creator.mapper.CreatorMapper;
import com.qianxun.homestay.modules.creator.service.CreatorService;
import com.qianxun.homestay.modules.creator.vo.CreatorVO;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 达人资料查询、创建、修改与启停。 */
@Service
public class CreatorServiceImpl implements CreatorService {

    private final CreatorMapper creatorMapper;
    private final ChannelService channelService;

    public CreatorServiceImpl(CreatorMapper creatorMapper, ChannelService channelService) {
        this.creatorMapper = creatorMapper;
        this.channelService = channelService;
    }

    @Override
    public List<CreatorVO> findCreators(CreatorQuery query) {
        if (query.channelId() != null && query.channelId() < 1) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "渠道编号不正确");
        }
        if (query.name() != null && query.name().length() > 80) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "达人名称筛选不能超过 80 个字符");
        }
        String name = query.name() == null || query.name().isBlank() ? null : query.name().trim();
        return creatorMapper.findAll(query.channelId(), query.enabled(), name).stream().map(this::toVO).toList();
    }

    @Override
    public CreatorVO getCreator(Long id) {
        return toVO(requireCreator(id));
    }

    @Override
    @Transactional
    public CreatorVO createCreator(CreatorSaveRequest request) {
        channelService.requireChannel(request.channelId());
        CreatorEntity creator = new CreatorEntity();
        creator.setName(request.name().trim());
        creator.setChannelId(request.channelId());
        creatorMapper.insert(creator);
        return getCreator(creator.getId());
    }

    @Override
    @Transactional
    public CreatorVO updateCreator(Long id, CreatorSaveRequest request) {
        requireCreator(id);
        channelService.requireChannel(request.channelId());
        CreatorEntity creator = new CreatorEntity();
        creator.setId(id);
        creator.setName(request.name().trim());
        creator.setChannelId(request.channelId());
        creatorMapper.update(creator);
        return getCreator(id);
    }

    @Override
    @Transactional
    public CreatorVO setEnabled(Long id, boolean enabled) {
        requireCreator(id);
        creatorMapper.updateEnabled(id, enabled);
        return getCreator(id);
    }

    private CreatorEntity requireCreator(Long id) {
        CreatorEntity creator = creatorMapper.findById(id);
        if (creator == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "达人不存在");
        }
        return creator;
    }

    private CreatorVO toVO(CreatorEntity creator) {
        return new CreatorVO(
                creator.getId(), creator.getName(), creator.getChannelId(), creator.getEnabled(),
                ApiTime.fromShanghai(creator.getCreatedAt()), ApiTime.fromShanghai(creator.getUpdatedAt()));
    }
}
