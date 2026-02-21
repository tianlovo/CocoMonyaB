package org.xlyo.cocomonyab.service;

import lombok.RequiredArgsConstructor;
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
import org.xlyo.cocomonyab.repository.ChannelRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Channel业务逻辑服务
 * 处理channel的CRUD操作和业务验证
 */
@Service
@RequiredArgsConstructor
public class ChannelService {

    private final ChannelRepository channelRepository;

    /**
     * 创建新channel
     * 验证channelId不存在，转换DTO为Entity，保存并返回VO
     *
     * @param dto channel创建数据传输对象
     * @return 创建的channel视图对象
     * @throws BusinessException 当channelId已存在时抛出DATA_ALREADY_EXISTS
     */
    @Transactional
    public ChannelVO create(ChannelCreateDTO dto) {
        // 验证channelId是否已存在
        if (channelRepository.existsByChannelId(dto.getChannelId())) {
            throw new BusinessException(ResponseCode.DATA_ALREADY_EXISTS, 
                    "频道ID已存在: " + dto.getChannelId());
        }

        // 转换DTO为Entity
        Channel channel = new Channel();
        channel.setChannelId(dto.getChannelId());
        channel.setChannelUsername(dto.getChannelUsername());
        channel.setChannelTitle(dto.getChannelTitle());
        channel.setMonitoringStatus(dto.getMonitoringStatus());
        channel.setCreateTime(LocalDateTime.now());
        channel.setUpdateTime(LocalDateTime.now());

        // 保存entity
        Channel saved = channelRepository.save(channel);

        // 转换为VO并返回
        return convertToVO(saved);
    }

    /**
     * 更新现有channel
     * 查找entity，更新非null字段，保存并返回VO
     *
     * @param id channel的MongoDB文档ID
     * @param dto channel更新数据传输对象
     * @return 更新后的channel视图对象
     * @throws BusinessException 当channel不存在时抛出DATA_NOT_FOUND
     */
    @Transactional
    public ChannelVO update(String id, ChannelUpdateDTO dto) {
        // 查找entity
        Channel channel = channelRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ResponseCode.DATA_NOT_FOUND, 
                        "频道不存在: " + id));

        // 更新非null字段
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

        // 保存entity
        Channel updated = channelRepository.save(channel);

        // 转换为VO并返回
        return convertToVO(updated);
    }

    /**
     * 根据ID删除channel
     * 验证entity存在后删除
     *
     * @param id channel的MongoDB文档ID
     * @throws BusinessException 当channel不存在时抛出DATA_NOT_FOUND
     */
    @Transactional
    public void deleteById(String id) {
        // 验证entity存在
        if (!channelRepository.existsById(id)) {
            throw new BusinessException(ResponseCode.DATA_NOT_FOUND, 
                    "频道不存在: " + id);
        }

        // 删除entity
        channelRepository.deleteById(id);
    }

    /**
     * 根据ID获取channel
     * 查找entity并转换为VO
     *
     * @param id channel的MongoDB文档ID
     * @return channel视图对象
     * @throws BusinessException 当channel不存在时抛出DATA_NOT_FOUND
     */
    public ChannelVO getById(String id) {
        // 查找entity
        Channel channel = channelRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ResponseCode.DATA_NOT_FOUND, 
                        "频道不存在: " + id));

        // 转换为VO并返回
        return convertToVO(channel);
    }

    /**
     * 获取所有channels列表
     * 查找所有entities并转换为VO列表
     *
     * @return channel视图对象列表，无数据时返回空列表
     */
    public List<ChannelVO> list() {
        // 查找所有entities
        List<Channel> channels = channelRepository.findAll();

        // 转换为VO列表并返回
        return channels.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    /**
     * 分页查询channels
     * 根据查询条件构建Pageable，应用过滤器，查询并转换为VO列表
     *
     * @param current 当前页码
     * @param size 每页大小
     * @param query 查询过滤条件
     * @return channel视图对象列表
     */
    public List<ChannelVO> page(Long current, Long size, ChannelQueryDTO query) {
        // 构建Pageable（页码从0开始，需要减1）
        Pageable pageable = PageRequest.of(current.intValue() - 1, size.intValue());

        // 应用过滤器并查询
        Page<Channel> page = applyFilters(query, pageable);

        // 转换为VO列表并返回
        return page.getContent().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    /**
     * 统计符合查询条件的channels总数
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
     * 根据ChannelQueryDTO中的条件选择合适的repository方法
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
     * 将Channel entity转换为ChannelVO
     * 映射所有字段从Entity到VO
     *
     * @param entity channel实体
     * @return channel视图对象
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
