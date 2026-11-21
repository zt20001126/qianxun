package com.qianxun.homestay.modules.portrait.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qianxun.homestay.common.exception.BusinessException;
import com.qianxun.homestay.modules.portrait.enums.BookerPartyType;
import com.qianxun.homestay.modules.portrait.dto.BookerPortraitListQuery;
import com.qianxun.homestay.modules.portrait.dto.BookerPortraitOrderRemarkUpdateRequest;
import com.qianxun.homestay.modules.portrait.dto.BookerPortraitSaveRequest;
import com.qianxun.homestay.modules.portrait.dto.BookerPortraitUpdateRequest;
import com.qianxun.homestay.modules.portrait.dto.PortraitTagCreateRequest;
import com.qianxun.homestay.modules.portrait.entity.BookerPortraitListEntity;
import com.qianxun.homestay.modules.portrait.entity.BookerPortraitOrderEntity;
import com.qianxun.homestay.modules.portrait.entity.BookerProfileEntity;
import com.qianxun.homestay.modules.portrait.entity.PortraitTagEntity;
import com.qianxun.homestay.modules.portrait.mapper.BookerPortraitMapper;
import com.qianxun.homestay.modules.portrait.service.BookerPortraitService;
import com.qianxun.homestay.modules.portrait.vo.BookerPortraitEditableOrderVO;
import com.qianxun.homestay.modules.portrait.vo.BookerPortraitListItemVO;
import com.qianxun.homestay.modules.portrait.vo.BookerPortraitOrderVO;
import com.qianxun.homestay.modules.portrait.vo.BookerPortraitPageVO;
import com.qianxun.homestay.modules.portrait.vo.BookerPortraitVO;
import com.qianxun.homestay.modules.portrait.vo.PortraitTagVO;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 按手机号维护单条顾客画像、标签 JSON 和关联订单备注。 */
@Service
public class BookerPortraitServiceImpl implements BookerPortraitService {

    private static final BigDecimal ZERO_AMOUNT = new BigDecimal("0.00");
    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() { };

    private final BookerPortraitMapper bookerPortraitMapper;
    private final ObjectMapper objectMapper;

    public BookerPortraitServiceImpl(BookerPortraitMapper bookerPortraitMapper, ObjectMapper objectMapper) {
        this.bookerPortraitMapper = bookerPortraitMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public void ensureProfileForOrder(Long orderId, String guestName, String phone) {
        if (orderId == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "订单编号不能为空");
        }
        Long previousProfileId = bookerPortraitMapper.findProfileIdByOrderId(orderId);
        BookerProfileEntity profile = findOrCreateProfile(orderId, guestName, phone);
        if (previousProfileId != null && !previousProfileId.equals(profile.getId())) {
            BookerProfileEntity previousProfile = bookerPortraitMapper.findProfileById(previousProfileId);
            if (previousProfile != null && previousProfile.getPhone() == null) {
                mergeMissingPortraitData(previousProfile, profile);
                bookerPortraitMapper.updateProfile(profile);
                bookerPortraitMapper.deleteProfile(previousProfileId);
            }
        }
    }

    @Override
    @Transactional
    public void reassignOrder(Long orderId, String guestName, String phone) {
        ensureProfileForOrder(orderId, guestName, phone);
    }

    @Override
    @Transactional
    public void saveForOrder(Long orderId, BookerPortraitSaveRequest request) {
        if (request == null) {
            return;
        }
        Long profileId = bookerPortraitMapper.findProfileIdByOrderId(orderId);
        if (profileId == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "订单画像不存在");
        }
        saveOrderProfileFields(profileId, request.age(), request.partyType(), request.inferredRegion(), request.tags());
    }

    @Override
    @Transactional(readOnly = true)
    public BookerPortraitVO findForOrder(Long orderId) {
        Long profileId = bookerPortraitMapper.findProfileIdByOrderId(orderId);
        if (profileId == null) {
            return null;
        }
        BookerProfileEntity profile = bookerPortraitMapper.findProfileById(profileId);
        if (profile == null) {
            return null;
        }
        return new BookerPortraitVO(profile.getId(), profile.getAge(), profile.getPartyType(),
                profile.getInferredRegion(), readTags(profile.getTagsJson()));
    }

    @Override
    @Transactional(readOnly = true)
    public BookerPortraitPageVO findProfiles(BookerPortraitListQuery query) {
        String guestName = trimToNull(query.guestName());
        String phone = trimToNull(query.phone());
        BookerPortraitListQuery normalizedQuery = new BookerPortraitListQuery(
                guestName, phone, query.page(), query.size());
        long offset = (long) (query.page() - 1) * query.size();
        List<BookerPortraitListEntity> profileRows = bookerPortraitMapper.findProfilePage(normalizedQuery, offset);
        if (profileRows.isEmpty()) {
            return new BookerPortraitPageVO(List.of(), bookerPortraitMapper.countProfiles(normalizedQuery),
                    query.page(), query.size());
        }

        List<Long> profileIds = profileRows.stream().map(BookerPortraitListEntity::getId).toList();
        Map<Long, List<BookerPortraitOrderVO>> completedOrdersByProfile = new LinkedHashMap<>();
        for (BookerPortraitOrderEntity order : bookerPortraitMapper.findCompletedOrders(profileIds)) {
            completedOrdersByProfile.computeIfAbsent(order.getProfileId(), ignored -> new ArrayList<>())
                    .add(new BookerPortraitOrderVO(order.getOrderId(), order.getOrderNo(), order.getCheckIn(),
                            money(order.getPaidAmount()), order.getRemark(), order.getAdminRemark()));
        }

        Map<Long, List<BookerPortraitEditableOrderVO>> allOrdersByProfile = new LinkedHashMap<>();
        for (BookerPortraitOrderEntity order : bookerPortraitMapper.findPortraitOrders(profileIds)) {
            allOrdersByProfile.computeIfAbsent(order.getProfileId(), ignored -> new ArrayList<>())
                    .add(new BookerPortraitEditableOrderVO(order.getOrderId(), order.getOrderNo(), order.getCheckIn(),
                            order.getStatus(), order.getRemark(), order.getAdminRemark()));
        }

        List<BookerPortraitListItemVO> list = profileRows.stream()
                .map(row -> toListItem(row, completedOrdersByProfile, allOrdersByProfile))
                .toList();
        return new BookerPortraitPageVO(list, bookerPortraitMapper.countProfiles(normalizedQuery),
                query.page(), query.size());
    }

    @Override
    @Transactional
    public void updateProfile(Long profileId, BookerPortraitUpdateRequest request) {
        BookerProfileEntity profile = bookerPortraitMapper.findProfileById(profileId);
        if (profile == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "顾客画像不存在");
        }
        List<BookerPortraitOrderRemarkUpdateRequest> orderRemarks = request.orderRemarks();
        if (orderRemarks != null && !orderRemarks.isEmpty()) {
            Set<Long> updatedOrderIds = new LinkedHashSet<>();
            for (BookerPortraitOrderRemarkUpdateRequest orderRemark : orderRemarks) {
                Long orderId = orderRemark.orderId();
                if (!updatedOrderIds.add(orderId)) {
                    throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, "同一订单备注不能重复提交");
                }
                if (bookerPortraitMapper.lockOrderIdForProfile(profileId, orderId) == null) {
                    throw new BusinessException(HttpStatus.CONFLICT, "订单不属于当前手机号画像，请刷新后重试");
                }
                bookerPortraitMapper.updateOrderRemarks(
                        orderId, trimToNull(orderRemark.remark()), trimToNull(orderRemark.adminRemark()));
            }
        }
        BookerProfileEntity updatedProfile = new BookerProfileEntity();
        updatedProfile.setId(profileId);
        updatedProfile.setAge(request.age());
        updatedProfile.setPartyType(request.partyType() == null ? null : request.partyType().name());
        updatedProfile.setInferredRegion(trimToNull(request.inferredRegion()));
        updatedProfile.setTagsJson(writeTags(resolveTags(request.tags())));
        updatedProfile.setAdminRemark(trimToNull(request.adminRemark()));
        bookerPortraitMapper.updateProfile(updatedProfile);
    }

    private BookerProfileEntity findOrCreateProfile(Long orderId, String guestName, String phone) {
        String normalizedName = trimToNull(guestName);
        String normalizedPhone = trimToNull(phone);
        BookerProfileEntity profile = new BookerProfileEntity();
        profile.setIdentityKey(identityKey(orderId, normalizedPhone));
        profile.setOrderId(normalizedPhone == null ? orderId : null);
        profile.setGuestName(normalizedName);
        profile.setPhone(normalizedPhone);
        profile.setAge(null);
        profile.setTagsJson("[]");
        bookerPortraitMapper.upsertProfile(profile);
        BookerProfileEntity saved = bookerPortraitMapper.findProfileByIdentityKey(profile.getIdentityKey());
        if (saved == null) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "创建顾客画像失败");
        }
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PortraitTagVO> findPortraitTags() {
        return bookerPortraitMapper.findPortraitTags().stream()
                .map(tag -> new PortraitTagVO(tag.getId(), tag.getName()))
                .toList();
    }

    @Override
    @Transactional
    public PortraitTagVO createPortraitTag(PortraitTagCreateRequest request) {
        String name = trimToNull(request.name());
        if (name == null) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, "标签名称不能为空");
        }
        if (name.length() > 40) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, "标签名称不能超过 40 个字符");
        }
        bookerPortraitMapper.upsertPortraitTag(name);
        PortraitTagEntity saved = bookerPortraitMapper.findPortraitTagByName(name);
        if (saved == null) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "画像标签保存失败");
        }
        return new PortraitTagVO(saved.getId(), saved.getName());
    }

    private void saveOrderProfileFields(
            Long profileId,
            Integer age,
            BookerPartyType partyType,
            String inferredRegion,
            List<String> tags) {
        BookerProfileEntity profile = bookerPortraitMapper.findProfileByIdForUpdate(profileId);
        if (profile == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "顾客画像不存在");
        }
        // 订单表单只补充已填写字段；空值不能清空共享画像。
        if (age != null) profile.setAge(age);
        if (partyType != null) profile.setPartyType(partyType.name());
        String region = trimToNull(inferredRegion);
        if (region != null) profile.setInferredRegion(region);
        LinkedHashSet<String> mergedTags = new LinkedHashSet<>(readTags(profile.getTagsJson()));
        mergedTags.addAll(resolveTags(tags));
        profile.setTagsJson(writeTags(normalizeTags(new ArrayList<>(mergedTags))));
        bookerPortraitMapper.updateProfileFromOrder(profile);
    }

    private List<String> resolveTags(List<String> tags) {
        List<String> normalizedTags = normalizeTags(tags);
        if (normalizedTags.isEmpty()) {
            return normalizedTags;
        }
        Map<String, String> availableTags = new LinkedHashMap<>();
        for (PortraitTagEntity tag : bookerPortraitMapper.findPortraitTags()) {
            availableTags.put(tag.getName().toLowerCase(Locale.ROOT), tag.getName());
        }
        List<String> resolvedTags = new ArrayList<>();
        for (String tag : normalizedTags) {
            String availableName = availableTags.get(tag.toLowerCase(Locale.ROOT));
            if (availableName == null) {
                throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, "画像标签不存在或已停用，请刷新标签列表");
            }
            resolvedTags.add(availableName);
        }
        return List.copyOf(resolvedTags);
    }

    private List<String> normalizeTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return List.of();
        }
        List<String> normalizedTags = new ArrayList<>();
        Set<String> uniqueTagNames = new LinkedHashSet<>();
        for (String tag : tags) {
            String normalized = trimToNull(tag);
            if (normalized == null) {
                continue;
            }
            if (normalized.length() > 40) {
                throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, "单个画像标签不能超过 40 个字符");
            }
            if (!uniqueTagNames.add(normalized.toLowerCase(Locale.ROOT))) {
                throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, "画像标签不能重复");
            }
            normalizedTags.add(normalized);
        }
        if (normalizedTags.size() > 30) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, "画像标签不能超过 30 个");
        }
        return List.copyOf(normalizedTags);
    }

    private void mergeMissingPortraitData(BookerProfileEntity source, BookerProfileEntity target) {
        if (target.getAge() == null) {
            target.setAge(source.getAge());
        }
        if (target.getPartyType() == null) {
            target.setPartyType(source.getPartyType());
        }
        if (target.getInferredRegion() == null) {
            target.setInferredRegion(source.getInferredRegion());
        }
        if (target.getAdminRemark() == null || target.getAdminRemark().isBlank()) {
            target.setAdminRemark(source.getAdminRemark());
        }
        LinkedHashSet<String> mergedTags = new LinkedHashSet<>(readTags(target.getTagsJson()));
        mergedTags.addAll(readTags(source.getTagsJson()));
        target.setTagsJson(writeTags(normalizeTags(new ArrayList<>(mergedTags))));
    }

    private BookerPortraitListItemVO toListItem(
            BookerPortraitListEntity row,
            Map<Long, List<BookerPortraitOrderVO>> completedOrdersByProfile,
            Map<Long, List<BookerPortraitEditableOrderVO>> allOrdersByProfile) {
        BigDecimal cumulativeAmount = row.getCumulativePaidAmount() == null
                ? ZERO_AMOUNT : row.getCumulativePaidAmount();
        return new BookerPortraitListItemVO(
                row.getId(), row.getGuestName(), row.getPhone(), row.getAge(), row.getPartyType(),
                row.getInferredRegion(), Boolean.TRUE.equals(row.getMember()), readTags(row.getTagsJson()),
                row.getAdminRemark(),
                row.getCompletedOrderCount() == null ? 0 : row.getCompletedOrderCount(),
                money(cumulativeAmount), completedOrdersByProfile.getOrDefault(row.getId(), List.of()),
                allOrdersByProfile.getOrDefault(row.getId(), List.of()));
    }

    private List<String> readTags(String tagsJson) {
        if (tagsJson == null || tagsJson.isBlank()) {
            return List.of();
        }
        try {
            List<String> tags = objectMapper.readValue(tagsJson, STRING_LIST_TYPE);
            return tags == null ? List.of() : List.copyOf(tags);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "画像标签数据格式不正确");
        }
    }

    private String writeTags(List<String> tags) {
        try {
            return objectMapper.writeValueAsString(tags);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("无法序列化画像标签", exception);
        }
    }

    private String identityKey(Long orderId, String phone) {
        String identityText = phone == null ? "ORDER:" + orderId : "PHONE:" + phone;
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(identityText.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String money(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}
