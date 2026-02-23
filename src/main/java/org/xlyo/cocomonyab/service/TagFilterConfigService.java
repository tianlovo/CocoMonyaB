package org.xlyo.cocomonyab.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.xlyo.cocomonyab.common.enums.ResponseCode;
import org.xlyo.cocomonyab.common.exception.BusinessException;
import org.xlyo.cocomonyab.domain.dto.TagFilterConfigCreateDTO;
import org.xlyo.cocomonyab.domain.dto.TagFilterConfigQueryDTO;
import org.xlyo.cocomonyab.domain.dto.TagFilterConfigUpdateDTO;
import org.xlyo.cocomonyab.domain.entity.TagFilterConfig;
import org.xlyo.cocomonyab.domain.vo.TagFilterConfigVO;
import org.xlyo.cocomonyab.event.TagFilterConfigEvent;
import org.xlyo.cocomonyab.repository.TagFilterConfigRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 标签过滤配置服务类
 * 提供标签过滤配置的业务逻辑处理
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TagFilterConfigService {
    
    private final TagFilterConfigRepository repository;
    private final ApplicationEventPublisher eventPublisher;
    
    /**
     * 创建或更新全局配置
     * 如果全局配置已存在则更新，否则创建新的全局配置
     *
     * @param dto 创建配置的DTO
     * @return 配置的VO对象
     */
    @Transactional
    public TagFilterConfigVO createOrUpdateGlobalConfig(TagFilterConfigCreateDTO dto) {
        log.info("创建或更新全局配置: {}", dto);
        
        // 验证参数
        validateCreateDTO(dto, false);
        
        // 查找现有的全局配置
        TagFilterConfig config = repository.findByChannelIdIsNull()
                .orElse(new TagFilterConfig());
        
        boolean isNew = config.getId() == null;
        
        // 设置字段
        config.setChannelId(null);
        config.setTags(dto.getTags());
        config.setMatchMode(dto.getMatchMode());
        config.setEnabled(dto.getEnabled());
        
        // 设置时间戳
        LocalDateTime now = LocalDateTime.now();
        if (isNew) {
            config.setCreateTime(now);
        }
        config.setUpdateTime(now);
        
        // 保存配置
        config = repository.save(config);
        
        // 发布事件
        try {
            TagFilterConfigEvent event = TagFilterConfigEvent.configUpdated(
                    this, null, config.getId(), config.getEnabled());
            eventPublisher.publishEvent(event);
            log.info("发布全局配置更新事件: {}", event);
        } catch (Exception e) {
            log.error("发布全局配置更新事件失败", e);
            // 事件发布失败不影响数据保存
        }
        
        return convertToVO(config);
    }
    
    /**
     * 获取全局配置
     *
     * @return 全局配置的VO对象
     * @throws BusinessException 如果全局配置不存在
     */
    public TagFilterConfigVO getGlobalConfig() {
        log.info("获取全局配置");
        
        TagFilterConfig config = repository.findByChannelIdIsNull()
                .orElseThrow(() -> new BusinessException(
                        ResponseCode.DATA_NOT_FOUND, "全局配置不存在"));
        
        return convertToVO(config);
    }
    
    /**
     * 创建频道配置
     *
     * @param dto 创建配置的DTO
     * @return 配置的VO对象
     * @throws BusinessException 如果channelId为null、无效或已存在
     */
    @Transactional
    public TagFilterConfigVO createChannelConfig(TagFilterConfigCreateDTO dto) {
        log.info("创建频道配置: {}", dto);
        
        // 验证参数
        validateCreateDTO(dto, true);
        
        // 检查channelId是否已存在
        if (repository.existsByChannelId(dto.getChannelId())) {
            throw new BusinessException(ResponseCode.DATA_ALREADY_EXISTS, 
                    "频道配置已存在: " + dto.getChannelId());
        }
        
        // 创建新配置
        TagFilterConfig config = convertToEntity(dto);
        
        // 设置时间戳
        LocalDateTime now = LocalDateTime.now();
        config.setCreateTime(now);
        config.setUpdateTime(now);
        
        // 保存配置
        config = repository.save(config);
        
        // 发布事件
        try {
            TagFilterConfigEvent event = TagFilterConfigEvent.configCreated(
                    this, config.getChannelId(), config.getId(), config.getEnabled());
            eventPublisher.publishEvent(event);
            log.info("发布频道配置创建事件: {}", event);
        } catch (Exception e) {
            log.error("发布频道配置创建事件失败", e);
            // 事件发布失败不影响数据保存
        }
        
        return convertToVO(config);
    }
    
    /**
     * 更新配置（通过MongoDB ID）
     *
     * @param id MongoDB文档ID
     * @param dto 更新配置的DTO
     * @return 更新后的配置VO对象
     * @throws BusinessException 如果配置不存在
     */
    @Transactional
    public TagFilterConfigVO updateConfig(String id, TagFilterConfigUpdateDTO dto) {
        log.info("更新配置 ID={}: {}", id, dto);
        
        // 验证参数
        validateUpdateDTO(dto);
        
        // 查找配置
        TagFilterConfig config = repository.findById(id)
                .orElseThrow(() -> new BusinessException(
                        ResponseCode.DATA_NOT_FOUND, "配置不存在: " + id));
        
        // 更新字段（只更新非null的字段）
        if (dto.getTags() != null) {
            config.setTags(dto.getTags());
        }
        if (dto.getMatchMode() != null) {
            config.setMatchMode(dto.getMatchMode());
        }
        if (dto.getEnabled() != null) {
            config.setEnabled(dto.getEnabled());
        }
        
        // 更新时间戳
        config.setUpdateTime(LocalDateTime.now());
        
        // 保存配置
        config = repository.save(config);
        
        // 发布事件
        try {
            TagFilterConfigEvent event = TagFilterConfigEvent.configUpdated(
                    this, config.getChannelId(), config.getId(), config.getEnabled());
            eventPublisher.publishEvent(event);
            log.info("发布配置更新事件: {}", event);
        } catch (Exception e) {
            log.error("发布配置更新事件失败", e);
            // 事件发布失败不影响数据保存
        }
        
        return convertToVO(config);
    }
    
    /**
     * 删除配置（通过MongoDB ID）
     *
     * @param id MongoDB文档ID
     * @throws BusinessException 如果配置不存在
     */
    @Transactional
    public void deleteConfig(String id) {
        log.info("删除配置 ID={}", id);
        
        // 查找配置
        TagFilterConfig config = repository.findById(id)
                .orElseThrow(() -> new BusinessException(
                        ResponseCode.DATA_NOT_FOUND, "配置不存在: " + id));
        
        Long channelId = config.getChannelId();
        
        // 删除配置
        repository.deleteById(id);
        
        // 发布事件
        try {
            TagFilterConfigEvent event = TagFilterConfigEvent.configDeleted(
                    this, channelId, id);
            eventPublisher.publishEvent(event);
            log.info("发布配置删除事件: {}", event);
        } catch (Exception e) {
            log.error("发布配置删除事件失败", e);
            // 事件发布失败不影响数据删除
        }
    }
    
    /**
     * 通过MongoDB ID获取配置
     *
     * @param id MongoDB文档ID
     * @return 配置的VO对象
     * @throws BusinessException 如果配置不存在
     */
    public TagFilterConfigVO getConfigById(String id) {
        log.info("通过ID获取配置: {}", id);
        
        TagFilterConfig config = repository.findById(id)
                .orElseThrow(() -> new BusinessException(
                        ResponseCode.DATA_NOT_FOUND, "配置不存在: " + id));
        
        return convertToVO(config);
    }
    
    /**
     * 通过channelId获取配置
     *
     * @param channelId Telegram频道ID
     * @return 配置的VO对象
     * @throws BusinessException 如果配置不存在
     */
    public TagFilterConfigVO getConfigByChannelId(Long channelId) {
        log.info("通过channelId获取配置: {}", channelId);
        
        TagFilterConfig config = repository.findByChannelId(channelId)
                .orElseThrow(() -> new BusinessException(
                        ResponseCode.DATA_NOT_FOUND, "频道配置不存在: " + channelId));
        
        return convertToVO(config);
    }
    
    /**
     * 获取有效配置（频道配置优先，否则返回全局配置）
     *
     * @param channelId Telegram频道ID
     * @return 有效配置的VO对象
     * @throws BusinessException 如果频道配置和全局配置都不存在
     */
    public TagFilterConfigVO getEffectiveConfig(Long channelId) {
        log.info("获取频道 {} 的有效配置", channelId);
        
        // 先查找频道配置
        return repository.findByChannelId(channelId)
                .map(this::convertToVO)
                .orElseGet(() -> {
                    // 如果频道配置不存在，返回全局配置
                    log.info("频道配置不存在，使用全局配置");
                    return repository.findByChannelIdIsNull()
                            .map(this::convertToVO)
                            .orElseThrow(() -> new BusinessException(
                                    ResponseCode.DATA_NOT_FOUND, 
                                    "未找到频道配置和全局配置"));
                });
    }
    
    /**
     * 分页查询所有频道配置
     *
     * @param current 当前页码（从1开始）
     * @param size 每页大小
     * @param query 查询条件
     * @return 配置VO列表
     */
    public List<TagFilterConfigVO> pageChannelConfigs(Long current, Long size, 
                                                       TagFilterConfigQueryDTO query) {
        log.info("分页查询频道配置: current={}, size={}, query={}", current, size, query);
        
        // 创建分页对象（PageRequest的页码从0开始）
        Pageable pageable = PageRequest.of(current.intValue() - 1, size.intValue());
        
        Page<TagFilterConfig> page;
        
        // 根据查询条件选择合适的查询方法
        if (query != null && query.getMatchMode() != null && query.getEnabled() != null) {
            page = repository.findByMatchModeAndEnabled(
                    query.getMatchMode(), query.getEnabled(), pageable);
        } else if (query != null && query.getMatchMode() != null) {
            page = repository.findByMatchMode(query.getMatchMode(), pageable);
        } else if (query != null && query.getEnabled() != null) {
            page = repository.findByEnabled(query.getEnabled(), pageable);
        } else {
            page = repository.findByChannelIdIsNotNull(pageable);
        }
        
        return page.getContent().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }
    
    /**
     * 统计符合条件的配置数量
     *
     * @param query 查询条件
     * @return 配置数量
     */
    public Long countChannelConfigs(TagFilterConfigQueryDTO query) {
        log.info("统计频道配置数量: query={}", query);
        
        // 根据查询条件选择合适的查询方法
        Pageable pageable = PageRequest.of(0, 1);
        
        Page<TagFilterConfig> page;
        if (query != null && query.getMatchMode() != null && query.getEnabled() != null) {
            page = repository.findByMatchModeAndEnabled(
                    query.getMatchMode(), query.getEnabled(), pageable);
        } else if (query != null && query.getMatchMode() != null) {
            page = repository.findByMatchMode(query.getMatchMode(), pageable);
        } else if (query != null && query.getEnabled() != null) {
            page = repository.findByEnabled(query.getEnabled(), pageable);
        } else {
            page = repository.findByChannelIdIsNotNull(pageable);
        }
        
        return page.getTotalElements();
    }
    
    /**
     * 发布重新加载事件
     */
    public void publishReloadAllEvent() {
        log.info("发布重新加载所有配置事件");
        
        try {
            TagFilterConfigEvent event = TagFilterConfigEvent.reloadAll(this);
            eventPublisher.publishEvent(event);
            log.info("发布重新加载事件成功: {}", event);
        } catch (Exception e) {
            log.error("发布重新加载事件失败", e);
            throw new BusinessException(ResponseCode.BUSINESS_ERROR, 
                    "发布重新加载事件失败", e);
        }
    }
    
    /**
     * 验证创建配置的DTO参数
     * 
     * @param dto 创建配置的DTO
     * @param isChannelConfig 是否为频道配置
     * @throws BusinessException 如果验证失败
     */
    private void validateCreateDTO(TagFilterConfigCreateDTO dto, boolean isChannelConfig) {
        // 验证tags不为null
        if (dto.getTags() == null) {
            throw new BusinessException(ResponseCode.VALIDATION_ERROR, 
                    "标签列表不能为null");
        }
        
        // 验证matchMode为whitelist或blacklist
        if (dto.getMatchMode() == null || 
                (!dto.getMatchMode().equals("whitelist") && !dto.getMatchMode().equals("blacklist"))) {
            throw new BusinessException(ResponseCode.VALIDATION_ERROR, 
                    "匹配模式必须是whitelist或blacklist");
        }
        
        // 验证enabled不为null
        if (dto.getEnabled() == null) {
            throw new BusinessException(ResponseCode.VALIDATION_ERROR, 
                    "启用状态不能为null");
        }
        
        // 如果是频道配置，验证channelId
        if (isChannelConfig) {
            // 验证channelId不为null
            if (dto.getChannelId() == null) {
                throw new BusinessException(ResponseCode.VALIDATION_ERROR, 
                        "创建频道配置时channelId不能为null");
            }
            
            // 验证channelId为负数（有效的Telegram频道ID）
            if (dto.getChannelId() >= 0) {
                throw new BusinessException(ResponseCode.VALIDATION_ERROR, 
                        "channelId必须是负数（有效的Telegram频道ID）");
            }
        }
    }
    
    /**
     * 验证更新配置的DTO参数
     * 
     * @param dto 更新配置的DTO
     * @throws BusinessException 如果验证失败
     */
    private void validateUpdateDTO(TagFilterConfigUpdateDTO dto) {
        // 验证matchMode（如果提供）
        if (dto.getMatchMode() != null && 
                (!dto.getMatchMode().equals("whitelist") && !dto.getMatchMode().equals("blacklist"))) {
            throw new BusinessException(ResponseCode.VALIDATION_ERROR, 
                    "匹配模式必须是whitelist或blacklist");
        }
    }
    
    /**
     * 将DTO转换为Entity
     *
     * @param dto 创建配置的DTO
     * @return TagFilterConfig实体
     */
    private TagFilterConfig convertToEntity(TagFilterConfigCreateDTO dto) {
        TagFilterConfig entity = new TagFilterConfig();
        entity.setChannelId(dto.getChannelId());
        entity.setTags(dto.getTags());
        entity.setMatchMode(dto.getMatchMode());
        entity.setEnabled(dto.getEnabled());
        return entity;
    }
    
    /**
     * 将Entity转换为VO
     *
     * @param entity TagFilterConfig实体
     * @return 配置的VO对象
     */
    private TagFilterConfigVO convertToVO(TagFilterConfig entity) {
        TagFilterConfigVO vo = new TagFilterConfigVO();
        vo.setId(entity.getId());
        vo.setChannelId(entity.getChannelId());
        vo.setTags(entity.getTags());
        vo.setMatchMode(entity.getMatchMode());
        vo.setEnabled(entity.getEnabled());
        vo.setCreateTime(entity.getCreateTime());
        vo.setUpdateTime(entity.getUpdateTime());
        return vo;
    }
}
