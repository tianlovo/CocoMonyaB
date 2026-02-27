package org.xlyo.cocomonyab.source;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 消息来源注册表
 * <p>
 * 管理所有已注册的消息来源，提供注册、查询、启动和停止功能。
 * 使用线程安全的 ConcurrentHashMap 支持并发访问。
 * 
 * @author tianluoqaq
 * @since 1.0
 */
@Slf4j
@Component
public class MessageSourceRegistry {
    
    /**
     * 已注册的消息来源
     * key: sourceId, value: MessageSource
     */
    private final Map<String, MessageSource> sources = new ConcurrentHashMap<>();
    
    /**
     * 注册一个消息来源
     * <p>
     * 如果已存在相同 sourceId 的消息来源，将抛出异常
     * 
     * @param source 消息来源
     * @throws IllegalArgumentException 如果 source 为 null 或 sourceId 已存在
     */
    public void register(MessageSource source) {
        if (source == null) {
            throw new IllegalArgumentException("消息来源不能为 null");
        }
        
        String sourceId = source.getSourceId();
        if (sourceId == null || sourceId.trim().isEmpty()) {
            throw new IllegalArgumentException("消息来源 ID 不能为空");
        }
        
        if (sources.containsKey(sourceId)) {
            throw new IllegalArgumentException("消息来源已存在: " + sourceId);
        }
        
        sources.put(sourceId, source);
        log.info("消息来源已注册: id={}, name={}", sourceId, source.getSourceName());
    }
    
    /**
     * 注销一个消息来源
     * <p>
     * 如果消息来源正在运行，将先停止它
     * 
     * @param sourceId 消息来源 ID
     * @return true 如果成功注销，false 如果消息来源不存在
     */
    public boolean unregister(String sourceId) {
        MessageSource source = sources.remove(sourceId);
        if (source == null) {
            log.warn("尝试注销不存在的消息来源: {}", sourceId);
            return false;
        }
        
        // 如果正在运行，先停止
        if (source.isRunning()) {
            try {
                source.stop();
                log.info("消息来源已停止: id={}", sourceId);
            } catch (MessageSourceException e) {
                log.error("停止消息来源失败: id={}", sourceId, e);
            }
        }
        
        log.info("消息来源已注销: id={}, name={}", sourceId, source.getSourceName());
        return true;
    }
    
    /**
     * 获取指定的消息来源
     * 
     * @param sourceId 消息来源 ID
     * @return 消息来源，如果不存在返回 null
     */
    public MessageSource getSource(String sourceId) {
        return sources.get(sourceId);
    }
    
    /**
     * 获取所有已注册的消息来源
     * 
     * @return 消息来源列表（不可修改）
     */
    public List<MessageSource> getAllSources() {
        return List.copyOf(sources.values());
    }
    
    /**
     * 获取所有正在运行的消息来源
     * 
     * @return 正在运行的消息来源列表
     */
    public List<MessageSource> getRunningSources() {
        return sources.values().stream()
            .filter(MessageSource::isRunning)
            .toList();
    }
    
    /**
     * 检查消息来源是否已注册
     * 
     * @param sourceId 消息来源 ID
     * @return true 如果已注册，false 否则
     */
    public boolean isRegistered(String sourceId) {
        return sources.containsKey(sourceId);
    }
    
    /**
     * 获取已注册的消息来源数量
     * 
     * @return 消息来源数量
     */
    public int getSourceCount() {
        return sources.size();
    }
    
    /**
     * 启动指定的消息来源
     * 
     * @param sourceId 消息来源 ID
     * @throws MessageSourceException 如果启动失败
     * @throws IllegalArgumentException 如果消息来源不存在
     */
    public void startSource(String sourceId) throws MessageSourceException {
        MessageSource source = sources.get(sourceId);
        if (source == null) {
            throw new IllegalArgumentException("消息来源不存在: " + sourceId);
        }
        
        if (source.isRunning()) {
            log.warn("消息来源已在运行: id={}", sourceId);
            return;
        }
        
        log.info("启动消息来源: id={}, name={}", sourceId, source.getSourceName());
        source.start();
        log.info("消息来源已启动: id={}", sourceId);
    }
    
    /**
     * 停止指定的消息来源
     * 
     * @param sourceId 消息来源 ID
     * @throws MessageSourceException 如果停止失败
     * @throws IllegalArgumentException 如果消息来源不存在
     */
    public void stopSource(String sourceId) throws MessageSourceException {
        MessageSource source = sources.get(sourceId);
        if (source == null) {
            throw new IllegalArgumentException("消息来源不存在: " + sourceId);
        }
        
        if (!source.isRunning()) {
            log.warn("消息来源未运行: id={}", sourceId);
            return;
        }
        
        log.info("停止消息来源: id={}, name={}", sourceId, source.getSourceName());
        source.stop();
        log.info("消息来源已停止: id={}", sourceId);
    }
    
    /**
     * 启动所有已注册的消息来源
     * <p>
     * 如果某个消息来源启动失败，将记录错误但继续启动其他消息来源
     */
    public void startAll() {
        log.info("启动所有消息来源，总数: {}", sources.size());
        
        for (MessageSource source : sources.values()) {
            if (source.isRunning()) {
                log.debug("消息来源已在运行，跳过: id={}", source.getSourceId());
                continue;
            }
            
            try {
                source.start();
                log.info("消息来源已启动: id={}, name={}", 
                    source.getSourceId(), source.getSourceName());
            } catch (MessageSourceException e) {
                log.error("启动消息来源失败: id={}, name={}", 
                    source.getSourceId(), source.getSourceName(), e);
            }
        }
    }
    
    /**
     * 停止所有正在运行的消息来源
     * <p>
     * 如果某个消息来源停止失败，将记录错误但继续停止其他消息来源
     */
    public void stopAll() {
        List<MessageSource> runningSources = getRunningSources();
        log.info("停止所有消息来源，运行中数量: {}", runningSources.size());
        
        for (MessageSource source : runningSources) {
            try {
                source.stop();
                log.info("消息来源已停止: id={}, name={}", 
                    source.getSourceId(), source.getSourceName());
            } catch (MessageSourceException e) {
                log.error("停止消息来源失败: id={}, name={}", 
                    source.getSourceId(), source.getSourceName(), e);
            }
        }
    }
    
    /**
     * 获取所有消息来源的健康状态
     * 
     * @return 消息来源 ID 到健康状态的映射
     */
    public Map<String, MessageSourceHealth> getAllHealthStatus() {
        Map<String, MessageSourceHealth> healthMap = new HashMap<>();
        
        for (Map.Entry<String, MessageSource> entry : sources.entrySet()) {
            try {
                MessageSourceHealth health = entry.getValue().getHealth();
                healthMap.put(entry.getKey(), health);
            } catch (Exception e) {
                log.error("获取消息来源健康状态失败: id={}", entry.getKey(), e);
                healthMap.put(entry.getKey(), MessageSourceHealth.unhealthy(
                    "获取健康状态失败: " + e.getMessage()
                ));
            }
        }
        
        return healthMap;
    }
}
