package org.xlyo.cocomonyab.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.xlyo.cocomonyab.source.MessageSourceHealth;
import org.xlyo.cocomonyab.source.unread.UnreadMessageSource;
import org.xlyo.cocomonyab.source.unread.dto.BufferStatusResponse;
import org.xlyo.cocomonyab.source.unread.model.UnreadMessageDetectionResult;
import org.xlyo.cocomonyab.source.unread.model.UnreadMessageStatistics;
import org.xlyo.cocomonyab.source.unread.service.UnreadMessageBufferService;
import org.xlyo.cocomonyab.source.unread.service.UnreadMessageSourceService;

/**
 * 未读消息来源控制器
 * <p>
 * 提供 REST API 用于手动触发未读消息检测、查询统计信息、健康状态和缓冲区管理。
 * <p>
 * 主要功能：
 * <ul>
 *   <li>手动触发未读消息检测</li>
 *   <li>获取统计信息和健康状态</li>
 *   <li>查询缓冲区状态</li>
 *   <li>重试失败消息</li>
 *   <li>清理已处理消息</li>
 * </ul>
 *
 * @author CocoMonya Team
 * @since 1.0
 */
@RestController
@RequestMapping("/api/message-source/unread")
@RequiredArgsConstructor
@Slf4j
public class UnreadMessageSourceController {
    
    private final UnreadMessageSource source;
    private final UnreadMessageSourceService service;
    private final UnreadMessageBufferService bufferService;
    
    /**
     * 手动触发未读消息检测
     * <p>
     * 触发一次完整的未读消息检测流程，扫描所有监控频道并获取未读消息。
     * 如果消息来源未运行或已有检测任务在进行中，将返回相应的错误状态。
     *
     * @return 检测结果统计信息
     */
    @PostMapping("/detect")
    public ResponseEntity<UnreadMessageDetectionResult> detectUnreadMessages() {
        log.info("收到手动触发未读消息检测请求");
        
        // 检查消息来源是否运行
        if (!source.isRunning()) {
            log.warn("未读消息来源未运行，无法触发检测");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .build();
        }
        
        try {
            UnreadMessageDetectionResult result = service.detectUnreadMessages();
            log.info("未读消息检测完成: 总频道={}, 未读消息={}", 
                result.getTotalChannels(), result.getTotalUnreadMessages());
            return ResponseEntity.ok(result);
        } catch (IllegalStateException e) {
            // 已有检测任务在进行中
            log.warn("未读消息检测已在进行中");
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .build();
        } catch (Exception e) {
            log.error("未读消息检测失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .build();
        }
    }
    
    /**
     * 获取统计信息
     * <p>
     * 返回未读消息来源的累计统计信息，包括扫描的频道数、检测到的消息数、
     * 处理成功和失败的消息数，以及最后一次检测时间。
     *
     * @return 统计信息
     */
    @GetMapping("/statistics")
    public ResponseEntity<UnreadMessageStatistics> getStatistics() {
        log.debug("获取未读消息来源统计信息");
        
        try {
            UnreadMessageStatistics stats = service.getStatistics();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("获取统计信息失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .build();
        }
    }
    
    /**
     * 获取健康状态
     * <p>
     * 返回未读消息来源的健康状态，包括运行状态、统计指标和缓冲区状态。
     * 健康状态可以是 HEALTHY（健康）、DEGRADED（降级）或 UNHEALTHY（不健康）。
     *
     * @return 健康状态
     */
    @GetMapping("/health")
    public ResponseEntity<MessageSourceHealth> getHealth() {
        log.debug("获取未读消息来源健康状态");
        
        try {
            MessageSourceHealth health = source.getHealth();
            return ResponseEntity.ok(health);
        } catch (Exception e) {
            log.error("获取健康状态失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .build();
        }
    }
    
    /**
     * 获取缓冲区状态
     * <p>
     * 返回缓冲区中各状态消息的统计信息，包括待处理、已处理和失败的消息数量。
     *
     * @return 缓冲区状态
     */
    @GetMapping("/buffer/status")
    public ResponseEntity<BufferStatusResponse> getBufferStatus() {
        log.debug("获取缓冲区状态");
        
        try {
            long pending = bufferService.countPendingMessages();
            long processed = bufferService.countProcessedMessages();
            long failed = bufferService.countFailedMessages();
            
            BufferStatusResponse status = new BufferStatusResponse(pending, processed, failed);
            log.debug("缓冲区状态: 待处理={}, 已处理={}, 失败={}", pending, processed, failed);
            
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            log.error("获取缓冲区状态失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .build();
        }
    }
    
    /**
     * 重试失败消息
     * <p>
     * 将所有失败状态的缓冲消息重置为待处理状态，并重新处理。
     * 这对于临时错误导致的失败很有用。
     *
     * @return 无内容响应
     */
    @PostMapping("/buffer/retry-failed")
    public ResponseEntity<Void> retryFailedMessages() {
        log.info("收到重试失败消息请求");
        
        try {
            long failedCount = bufferService.countFailedMessages();
            log.info("开始重试 {} 条失败消息", failedCount);
            
            bufferService.retryFailedMessages();
            
            log.info("失败消息重试完成");
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("重试失败消息失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .build();
        }
    }
    
    /**
     * 清理已处理消息
     * <p>
     * 手动清理缓冲区中已处理的消息。
     * 注意：已处理的消息会通过 TTL 索引自动清理，此接口用于手动触发清理。
     *
     * @return 无内容响应
     */
    @DeleteMapping("/buffer/cleanup")
    public ResponseEntity<Void> cleanupProcessedMessages() {
        log.info("收到清理已处理消息请求");
        
        try {
            long processedCount = bufferService.countProcessedMessages();
            log.info("开始清理 {} 条已处理消息", processedCount);
            
            bufferService.cleanupProcessedMessages();
            
            log.info("已处理消息清理完成");
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("清理已处理消息失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .build();
        }
    }
}
