package org.xlyo.cocomonyab.repository;

import lombok.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import org.xlyo.cocomonyab.domain.entity.Channel;

import java.util.List;
import java.util.Optional;

/**
 * Channel实体的Repository接口
 * 提供CRUD操作和用于频道管理的自定义查询方法
 */
@Repository
public interface ChannelRepository extends MongoRepository<@NonNull Channel, @NonNull String> {

    /**
     * 根据channelId检查频道是否存在
     *
     * @param channelId Telegram频道ID
     * @return 如果存在给定channelId的频道则返回true，否则返回false
     */
    boolean existsByChannelId(Long channelId);

    /**
     * 根据channelId查找频道
     *
     * @param channelId Telegram频道ID
     * @return 包含找到的频道的Optional，若未找到则为空
     */
    Optional<Channel> findByChannelId(Long channelId);

    /**
     * 根据监控状态查找所有频道
     *
     * @param status 监控状态
     * @return 具有指定监控状态的频道列表
     */
    List<Channel> findByMonitoringStatus(Boolean status);

    /**
     * 根据用户名（部分匹配）和监控状态分页查找频道
     *
     * @param username 要搜索的用户名（部分匹配）
     * @param status 监控状态
     * @param pageable 分页信息
     * @return 符合条件的分页频道
     */
    Page<Channel> findByChannelUsernameContainingAndMonitoringStatus(
            String username, Boolean status, Pageable pageable);

    /**
     * 根据用户名（部分匹配）分页查找频道
     *
     * @param username 要搜索的用户名（部分匹配）
     * @param pageable 分页信息
     * @return 符合条件的分页频道
     */
    Page<Channel> findByChannelUsernameContaining(String username, Pageable pageable);

    /**
     * 根据监控状态分页查找频道
     *
     * @param status 监控状态
     * @param pageable 分页信息
     * @return 具有指定监控状态的分页频道
     */
    Page<Channel> findByMonitoringStatus(Boolean status, Pageable pageable);
}