package org.xlyo.cocomonyab.service.tag.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.xlyo.cocomonyab.common.enums.ResponseCode;
import org.xlyo.cocomonyab.common.exception.BusinessException;
import org.xlyo.cocomonyab.domain.dto.tag.TagFilterConfigCreateDTO;
import org.xlyo.cocomonyab.domain.dto.tag.TagFilterConfigUpdateDTO;
import org.xlyo.cocomonyab.domain.entity.tag.TagFilterConfig;
import org.xlyo.cocomonyab.domain.vo.tag.TagFilterConfigVO;
import org.xlyo.cocomonyab.event.TagConfigurationEvent;
import org.xlyo.cocomonyab.repository.tag.TagFilterConfigRepository;
import org.xlyo.cocomonyab.service.tag.TagFilterConfigService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * 标签过滤配置服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TagFilterConfigServiceImpl implements TagFilterConfigService {
    
    /**
     * 全局配置的固定ID
     */
    private static final String GLOBAL_CONFIG_ID = "global";
    
    private final TagFilterConfigRepository configRepository;
    private final ApplicationEventPublisher eventPublisher;
    
    @Override
    @Transactional
    public TagFilterConfigVO createOrUpdateGlobal(TagFilterConfigCreateDTO dto) {
        // 查询是否存在全局配置
        TagFilterConfig config = configRepository.findById(GLOBAL_CONFIG_ID)
            .orElse(null);
        
        boolean isNew = (config == null);
        
        if (config == null) {
            // 创建新配置
            config = new TagFilterConfig();
            config.setId(GLOBAL_CONFIG_ID);
            config.setCreateTime(LocalDateTime.now());
            log.info("创建全局标签过滤配置");
        } else {
            log.info("更新全局标签过滤配置");
        }
        
        // 设置字段
        config.setAuthorIds(dto.getAuthorIds() != null ? dto.getAuthorIds() : new ArrayList<>());
        config.setCharacterIds(dto.getCharacterIds() != null ? dto.getCharacterIds() : new ArrayList<>());
        config.setWorkIds(dto.getWorkIds() != null ? dto.getWorkIds() : new ArrayList<>());
        config.setCustomTags(dto.getCustomTags() != null ? dto.getCustomTags() : new HashMap<>());
        config.setMatchMode(dto.getMatchMode());
        config.setEnabled(dto.getEnabled());
        config.setUpdateTime(LocalDateTime.now());
        
        // 保存
        config = configRepository.save(config);
        
        // 发布事件
        if (isNew) {
            eventPublisher.publishEvent(
                TagConfigurationEvent.tagFilterAdded(this, config.getId())
            );
            log.info("全局标签过滤配置创建成功，已发布 TAG_FILTER_ADDED 事件: id={}, enabled={}, matchMode={}", 
                config.getId(), config.getEnabled(), config.getMatchMode());
        } else {
            eventPublisher.publishEvent(
                TagConfigurationEvent.tagFilterUpdated(this, config.getId())
            );
            log.info("全局标签过滤配置更新成功，已发布 TAG_FILTER_UPDATED 事件: id={}, enabled={}, matchMode={}", 
                config.getId(), config.getEnabled(), config.getMatchMode());
        }
        
        return toVO(config);
    }
    
    @Override
    public TagFilterConfigVO getGlobal() {
        return configRepository.findById(GLOBAL_CONFIG_ID)
            .map(this::toVO)
            .orElse(null);
    }
    
    @Override
    @Transactional
    public TagFilterConfigVO update(String id, TagFilterConfigUpdateDTO dto) {
        // 查询配置
        TagFilterConfig config = configRepository.findById(id)
            .orElseThrow(() -> new BusinessException(
                ResponseCode.DATA_NOT_FOUND,
                "标签过滤配置不存在: " + id
            ));
        
        // 更新字段
        if (dto.getAuthorIds() != null) {
            config.setAuthorIds(dto.getAuthorIds());
        }
        if (dto.getCharacterIds() != null) {
            config.setCharacterIds(dto.getCharacterIds());
        }
        if (dto.getWorkIds() != null) {
            config.setWorkIds(dto.getWorkIds());
        }
        if (dto.getCustomTags() != null) {
            config.setCustomTags(dto.getCustomTags());
        }
        if (dto.getMatchMode() != null) {
            config.setMatchMode(dto.getMatchMode());
        }
        if (dto.getEnabled() != null) {
            config.setEnabled(dto.getEnabled());
        }
        
        config.setUpdateTime(LocalDateTime.now());
        
        // 保存
        config = configRepository.save(config);
        
        // 发布标签过滤配置更新事件
        eventPublisher.publishEvent(
            TagConfigurationEvent.tagFilterUpdated(this, config.getId())
        );
        
        log.info("更新标签过滤配置成功，已发布 TAG_FILTER_UPDATED 事件: id={}", config.getId());
        
        return toVO(config);
    }
    
    @Override
    public TagFilterConfigVO getById(String id) {
        TagFilterConfig config = configRepository.findById(id)
            .orElseThrow(() -> new BusinessException(
                ResponseCode.DATA_NOT_FOUND,
                "标签过滤配置不存在: " + id
            ));
        return toVO(config);
    }
    
    /**
     * 系统启动时初始化默认全局配置
     */
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void initializeDefaultConfig() {
        // 检查全局配置是否存在
        if (!configRepository.existsById(GLOBAL_CONFIG_ID)) {
            TagFilterConfig defaultConfig = new TagFilterConfig();
            defaultConfig.setId(GLOBAL_CONFIG_ID);
            defaultConfig.setAuthorIds(new ArrayList<>());
            defaultConfig.setCharacterIds(new ArrayList<>());
            defaultConfig.setWorkIds(new ArrayList<>());
            defaultConfig.setCustomTags(new HashMap<>());
            defaultConfig.setMatchMode("whitelist");
            defaultConfig.setEnabled(false);
            defaultConfig.setCreateTime(LocalDateTime.now());
            defaultConfig.setUpdateTime(LocalDateTime.now());
            
            configRepository.save(defaultConfig);
            
            log.info("系统启动：创建默认全局标签过滤配置");
        } else {
            log.info("系统启动：全局标签过滤配置已存在");
        }
    }
    
    /**
     * 实体转VO
     */
    private TagFilterConfigVO toVO(TagFilterConfig config) {
        TagFilterConfigVO vo = new TagFilterConfigVO();
        vo.setId(config.getId());
        vo.setAuthorIds(config.getAuthorIds());
        vo.setCharacterIds(config.getCharacterIds());
        vo.setWorkIds(config.getWorkIds());
        vo.setCustomTags(config.getCustomTags());
        vo.setMatchMode(config.getMatchMode());
        vo.setEnabled(config.getEnabled());
        vo.setCreateTime(config.getCreateTime());
        vo.setUpdateTime(config.getUpdateTime());
        return vo;
    }
}
