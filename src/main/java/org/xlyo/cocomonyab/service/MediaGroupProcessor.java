package org.xlyo.cocomonyab.service;

import it.tdlight.jni.TdApi;

/**
 * 媒体组处理器接口
 * <p>
 * 定义媒体组处理的核心操作，包括消息处理、超时处理和状态查询。
 * 实现此接口的类应该保证并发安全性，使用适当的锁机制防止竞态条件。
 * </p>
 * 
 * <h3>并发安全要求：</h3>
 * <ul>
 *   <li>所有方法必须是线程安全的</li>
 *   <li>状态检查和更新必须是原子操作</li>
 *   <li>使用分段锁减少锁竞争</li>
 * </ul>
 * 
 * <h3>典型使用场景：</h3>
 * <pre>{@code
 * // 处理新到达的媒体组消息
 * if (processor.handleMediaGroupMessage(message)) {
 *     log.info("消息已接受");
 * } else {
 *     log.warn("消息被拒绝，媒体组可能正在处理或已完成");
 * }
 * 
 * // 定时任务处理超时的媒体组
 * processor.processTimedOutMediaGroups();
 * 
 * // 查询媒体组状态
 * MediaGroupState state = processor.getMediaGroupState(groupKey);
 * }</pre>
 * 
 * @see MediaGroupState
 * @since 1.0
 */
public interface MediaGroupProcessor {
    
    /**
     * 处理媒体组消息
     * <p>
     * 当新的媒体组消息到达时调用此方法。方法会检查媒体组的当前状态，
     * 只有在 COLLECTING 状态下才会接受新消息。
     * </p>
     * 
     * <h4>行为说明：</h4>
     * <ul>
     *   <li>如果媒体组不存在，创建新的媒体组并设置状态为 COLLECTING</li>
     *   <li>如果媒体组状态为 COLLECTING，添加消息到缓冲区</li>
     *   <li>如果媒体组状态为 PROCESSING 或 COMPLETED，拒绝消息</li>
     * </ul>
     * 
     * <h4>并发安全：</h4>
     * <p>
     * 此方法必须在锁保护下原子地执行状态检查和消息添加操作，
     * 防止与定时任务产生竞态条件。
     * </p>
     * 
     * @param message 新到达的 Telegram 消息，必须包含有效的 mediaAlbumId
     * @return {@code true} 如果消息被接受并添加到缓冲区，
     *         {@code false} 如果消息被拒绝（媒体组正在处理或已完成）
     * @throws NullPointerException 如果 message 为 null
     */
    boolean handleMediaGroupMessage(TdApi.Message message);
    
    /**
     * 处理超时的媒体组
     * <p>
     * 由定时任务定期调用（通常每秒一次），检查所有媒体组的超时状态。
     * 对于超时且状态为 COLLECTING 的媒体组，将其状态转换为 PROCESSING
     * 并触发实际的处理逻辑。
     * </p>
     * 
     * <h4>处理流程：</h4>
     * <ol>
     *   <li>遍历所有媒体组，检查时间戳</li>
     *   <li>对于超时的媒体组，在锁保护下检查状态</li>
     *   <li>如果状态为 COLLECTING，转换为 PROCESSING</li>
     *   <li>移除缓冲区数据并执行处理逻辑</li>
     *   <li>处理成功后转换为 COMPLETED，失败则重置状态</li>
     * </ol>
     * 
     * <h4>并发安全：</h4>
     * <p>
     * 此方法必须在锁保护下原子地执行状态检查、状态转换和数据移除操作，
     * 防止与新消息处理产生竞态条件。
     * </p>
     */
    void processTimedOutMediaGroups();
    
    /**
     * 获取媒体组当前状态
     * <p>
     * 查询指定媒体组的当前处理状态。此方法主要用于监控、调试和测试。
     * </p>
     * 
     * @param groupKey 媒体组键，格式为 "chatId:mediaAlbumId"，例如 "-1001234567890:5629499534213120"
     * @return 媒体组的当前状态，如果媒体组不存在则返回 {@code null}
     * @throws NullPointerException 如果 groupKey 为 null
     * @see MediaGroupState
     */
    MediaGroupState getMediaGroupState(String groupKey);
}
