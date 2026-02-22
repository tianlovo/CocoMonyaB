package org.xlyo.cocomonyab.service;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.xlyo.cocomonyab.common.enums.ResponseCode;
import org.xlyo.cocomonyab.common.exception.BusinessException;
import org.xlyo.cocomonyab.domain.dto.ChannelCreateDTO;
import org.xlyo.cocomonyab.domain.dto.ChannelQueryDTO;
import org.xlyo.cocomonyab.domain.dto.ChannelUpdateDTO;
import org.xlyo.cocomonyab.domain.entity.Channel;
import org.xlyo.cocomonyab.domain.vo.ChannelVO;
import org.xlyo.cocomonyab.event.ChannelMonitoringEvent;
import org.xlyo.cocomonyab.repository.ChannelRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Channel 业务逻辑服务
 * 处理 channel 的 CRUD 操作和业务验证
 * 
 * 当频道监控配置发生变化时，会发布 ChannelMonitoringEvent 事件，
 * 通知 ChannelMonitoringFilter 更新缓存
 */
@Service
@RequiredArgsConstructor
public class ChannelService {

    private final ChannelRepository channelRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 创建新 channel
     * 验证 channelId 不存在，转换 DTO 为 Entity，保存并返回 VO
     * 
     * 如果监控状态为 true，会发布 CHANNEL_ADDED 事件
     *
     * @param dto channel 创建数据传输对象
     * @return 创建的 channel 视图对象
     * @throws BusinessException 当 channelId 已存在时抛出 DATA_ALREADY_EXISTS
     */
    @Transactional
    public ChannelVO create(ChannelCreateDTO dto) {
        // 验证 channelId 是否已存在
        if (channelRepository.existsByChannelId(dto.getChannelId())) {
            throw new BusinessException(ResponseCode.DATA_ALREADY_EXISTS, 
                    "频道ID已存在: " + dto.getChannelId());
        }

        // 转换 DTO 为 Entity
        Channel channel = new Channel();
        channel.setChannelId(dto.getChannelId());
        channel.setChannelUsername(dto.getChannelUsername());
        channel.setChannelTitle(dto.getChannelTitle());
        channel.setMonitoringStatus(dto.getMonitoringStatus());
        channel.setCreateTime(LocalDateTime.now());
        channel.setUpdateTime(LocalDateTime.now());

        // 保存 entity
        Channel saved = channelRepository.save(channel);

        // 发布频道添加事件
        eventPublisher.publishEvent(
            ChannelMonitoringEvent.channelAdded(this, saved.getChannelId(), saved.getMonitoringStatus())
        );

        // 转换为 VO 并返回
        return convertToVO(saved);
    }

    /**
     * 更新现有 channel
     * 查找 entity，更新非 null 字段，保存并返回 VO
     * 
     * 如果 monitoringStatus 发生变化，会发布 CHANNEL_UPDATED 事件
     *
     * @param id channel 的 MongoDB 文档 ID
     * @param dto channel 更新数据传输对象
     * @return 更新后的 channel 视图对象
     * @throws BusinessException 当 channel 不存在时抛出 DATA_NOT_FOUND
     */
    @Transactional
    public ChannelVO update(String id, ChannelUpdateDTO dto) {
        // 查找 entity
        Channel channel = channelRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ResponseCode.DATA_NOT_FOUND, 
                        "频道不存在: " + id));

        // 记录原始监控状态
        Boolean oldMonitoringStatus = channel.getMonitoringStatus();

        // 更新非 null 字段
        if (dto.getChannelUsername() != null) {
            channel.setChannelUsername(dto.getChannelUsername());
        }
        if (dto.getChannelTitle() != null) {
            channel.setChannelTitle(dto.getChannelTitle());
        }
        if (dto.getMonitoringStatus() != null) {
            channel.setMonitoringStatus(dto.getMonitoringStatus());
        }
        channel.setUpdateTime(LocalDateTime.now());

        // 保存 entity
        Channel updated = channelRepository.save(channel);

        // 如果监控状态发生变化，发布事件
        Boolean newMonitoringStatus = updated.getMonitoringStatus();
        if (!oldMonitoringStatus.equals(newMonitoringStatus)) {
            eventPublisher.publishEvent(
                ChannelMonitoringEvent.channelUpdated(this, updated.getChannelId(), newMonitoringStatus)
            );
        }

        // 转换为 VO 并返回
        return convertToVO(updated);
    }

    /**
     * 根据 ID 删除 channel
     * 验证 entity 存在后删除
     * 
     * 会发布 CHANNEL_REMOVED 事件
     *
     * @param id channel 的 MongoDB 文档 ID
     * @throws BusinessException 当 channel 不存在时抛出 DATA_NOT_FOUND
     */
    @Transactional
    public void deleteById(String id) {
        // 查找 entity（需要获取 channelId 用于发布事件）
        Channel channel = channelRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ResponseCode.DATA_NOT_FOUND, 
                        "频道不存在: " + id));

        Long channelId = channel.getChannelId();

        // 删除 entity
        channelRepository.deleteById(id);

        // 发布频道移除事件
        eventPublisher.publishEvent(
            ChannelMonitoringEvent.channelRemoved(this, channelId)
        );
    }

    /**
     * 根据 ID 获取 channel
     * 查找 entity 并转换为 VO
     *
     * @param id channel 的 MongoDB 文档 ID
     * @return channel 视图对象
     * @throws BusinessException 当 channel 不存在时抛出 DATA_NOT_FOUND
     */
    public ChannelVO getById(String id) {
        // 查找 entity
        Channel channel = channelRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ResponseCode.DATA_NOT_FOUND, 
                        "频道不存在: " + id));

        // 转换为 VO 并返回
        return convertToVO(channel);
    }

    /**
     * 获取所有 channels 列表
     * 查找所有 entities 并转换为 VO 列表
     *
     * @return channel 视图对象列表，无数据时返回空列表
     */
    public List<ChannelVO> list() {
        // 查找所有 entities
        List<Channel> channels = channelRepository.findAll();

        // 转换为 VO 列表并返回
        return channels.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    /**
     * 分页查询 channels
     * 根据查询条件构建 Pageable，应用过滤器，查询并转换为 VO 列表
     *
     * @param current 当前页码
     * @param size 每页大小
     * @param query 查询过滤条件
     * @return channel 视图对象列表
     */
    public List<ChannelVO> page(Long current, Long size, ChannelQueryDTO query) {
        // 验证分页参数
        if (current == null || current < 1) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "页码必须大于等于1");
        }
        if (size == null || size < 1) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "每页大小必须大于等于1");
        }
        if (size > 100) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "每页大小不能超过100");
        }
        
        // 构建 Pageable（页码从 0 开始，需要减 1）
        Pageable pageable = PageRequest.of(current.intValue() - 1, size.intValue());

        // 应用过滤器并查询
        Page<Channel> page = applyFilters(query, pageable);

        // 转换为 VO 列表并返回
        return page.getContent().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    /**
     * 统计符合查询条件的 channels 总数
     * 应用过滤器并返回总数
     *
     * @param query 查询过滤条件
     * @return 符合条件的总记录数
     */
    public Long count(ChannelQueryDTO query) {
        // 应用过滤器并返回总数
        Pageable pageable = PageRequest.of(0, 1);
        Page<Channel> page = applyFilters(query, pageable);
        return page.getTotalElements();
    }

    /**
     * 应用查询过滤器
     * 根据 ChannelQueryDTO 中的条件选择合适的 repository 方法
     *
     * @param query 查询过滤条件
     * @param pageable 分页信息
     * @return 分页查询结果
     */
    private Page<Channel> applyFilters(ChannelQueryDTO query, Pageable pageable) {
        if (query == null) {
            return channelRepository.findAll(pageable);
        }

        String username = query.getChannelUsername();
        Boolean status = query.getMonitoringStatus();

        // 根据过滤条件选择查询方法
        if (username != null && status != null) {
            return channelRepository.findByChannelUsernameContainingAndMonitoringStatus(
                    username, status, pageable);
        } else if (username != null) {
            return channelRepository.findByChannelUsernameContaining(username, pageable);
        } else if (status != null) {
            return channelRepository.findByMonitoringStatus(status, pageable);
        } else {
            return channelRepository.findAll(pageable);
        }
    }

    /**
     * 将 Channel entity 转换为 ChannelVO
     * 映射所有字段从 Entity 到 VO
     *
     * @param entity channel 实体
     * @return channel 视图对象
     */
    private ChannelVO convertToVO(Channel entity) {
        ChannelVO vo = new ChannelVO();
        vo.setId(entity.getId());
        vo.setChannelId(entity.getChannelId());
        vo.setChannelUsername(entity.getChannelUsername());
        vo.setChannelTitle(entity.getChannelTitle());
        vo.setMonitoringStatus(entity.getMonitoringStatus());
        vo.setCreateTime(entity.getCreateTime());
        vo.setUpdateTime(entity.getUpdateTime());
        return vo;
    }
}
