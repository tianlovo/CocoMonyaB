package org.xlyo.cocomonyab.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import org.xlyo.cocomonyab.domain.entity.TagFilterConfig;

import java.util.Optional;

/**
 * TagFilterConfig实体的Repository接口
 * 提供标签过滤配置的CRUD操作和自定义查询方法
 */
@Repository
public interface TagFilterConfigRepository extends MongoRepository<TagFilterConfig, String> {
    
    /**
     * 通过channelId查询配置（用于频道配置）
     *
     * @param channelId Telegram频道ID
     * @return 包含找到的配置的Optional，若未找到则为空
     */
    Optional<TagFilterConfig> findByChannelId(Long channelId);
    
    /**
     * 查询全局配置（channelId为null）
     *
     * @return 包含全局配置的Optional，若未找到则为空
     */
    Optional<TagFilterConfig> findByChannelIdIsNull();
    
    /**
     * 检查channelId是否存在
     *
     * @param channelId Telegram频道ID
     * @return 如果存在给定channelId的配置则返回true，否则返回false
     */
    boolean existsByChannelId(Long channelId);
    
    /**
     * 分页查询所有频道配置（排除全局配置）
     *
     * @param pageable 分页信息
     * @return 频道配置的分页结果
     */
    Page<TagFilterConfig> findByChannelIdIsNotNull(Pageable pageable);
    
    /**
     * 根据匹配模式分页查询
     *
     * @param matchMode 匹配模式（whitelist或blacklist）
     * @param pageable 分页信息
     * @return 符合条件的分页配置
     */
    Page<TagFilterConfig> findByMatchMode(String matchMode, Pageable pageable);
    
    /**
     * 根据启用状态分页查询
     *
     * @param enabled 启用状态
     * @param pageable 分页信息
     * @return 符合条件的分页配置
     */
    Page<TagFilterConfig> findByEnabled(Boolean enabled, Pageable pageable);
    
    /**
     * 根据匹配模式和启用状态分页查询
     *
     * @param matchMode 匹配模式（whitelist或blacklist）
     * @param enabled 启用状态
     * @param pageable 分页信息
     * @return 符合条件的分页配置
     */
    Page<TagFilterConfig> findByMatchModeAndEnabled(String matchMode, Boolean enabled, Pageable pageable);
}
