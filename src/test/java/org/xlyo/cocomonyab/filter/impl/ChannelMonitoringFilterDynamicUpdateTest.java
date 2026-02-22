package org.xlyo.cocomonyab.filter.impl;

import it.tdlight.jni.TdApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.xlyo.cocomonyab.domain.dto.ChannelCreateDTO;
import org.xlyo.cocomonyab.domain.dto.ChannelUpdateDTO;
import org.xlyo.cocomonyab.domain.entity.Channel;
import org.xlyo.cocomonyab.domain.vo.ChannelVO;
import org.xlyo.cocomonyab.filter.FilterContext;
import org.xlyo.cocomonyab.filter.FilterResult;
import org.xlyo.cocomonyab.repository.ChannelRepository;
import org.xlyo.cocomonyab.service.ChannelService;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 频道监控过滤器动态更新测试
 * 测试通过 API 增删改频道时，过滤器缓存是否正确更新
 */
@SpringBootTest
@TestPropertySource(properties = {
    "spring.data.mongodb.database=cocomonya-test"
})
@DisplayName("频道监控过滤器动态更新测试")
class ChannelMonitoringFilterDynamicUpdateTest {
    
    @Autowired
    private ChannelMonitoringFilter filter;
    
    @Autowired
    private ChannelService channelService;
    
    @Autowired
    private ChannelRepository channelRepository;
    
    @BeforeEach
    void setUp() {
        // 清空测试数据
        channelRepository.deleteAll();
        
        // 重新加载过滤器缓存
        filter.reloadMonitoringChannels();
    }
    
    @Test
    @DisplayName("创建监控频道后，过滤器应该接受该频道的消息")
    void testCreateMonitoringChannel() throws InterruptedException {
        // 准备测试数据
        Long channelId = -1001234567890L;
        ChannelCreateDTO dto = new ChannelCreateDTO();
        dto.setChannelId(channelId);
        dto.setChannelUsername("test_channel");
        dto.setChannelTitle("测试频道");
        dto.setMonitoringStatus(true);
        
        // 创建频道前，消息应该被拒绝
        TdApi.Message message = createTestMessage(channelId);
        FilterContext context = new FilterContext();
        FilterResult result = filter.filter(message, context);
        assertEquals(FilterResult.REJECT, result, "创建前应该拒绝消息");
        
        // 创建频道
        ChannelVO vo = channelService.create(dto);
        assertNotNull(vo);
        
        // 等待事件处理（异步）
        Thread.sleep(100);
        
        // 创建频道后，消息应该被接受
        context = new FilterContext();
        result = filter.filter(message, context);
        assertEquals(FilterResult.ACCEPT, result, "创建后应该接受消息");
    }
    
    @Test
    @DisplayName("创建非监控频道后，过滤器应该拒绝该频道的消息")
    void testCreateNonMonitoringChannel() throws InterruptedException {
        // 准备测试数据
        Long channelId = -1001234567891L;
        ChannelCreateDTO dto = new ChannelCreateDTO();
        dto.setChannelId(channelId);
        dto.setChannelUsername("test_channel_2");
        dto.setChannelTitle("测试频道2");
        dto.setMonitoringStatus(false);
        
        // 创建频道
        ChannelVO vo = channelService.create(dto);
        assertNotNull(vo);
        
        // 等待事件处理
        Thread.sleep(100);
        
        // 消息应该被拒绝
        TdApi.Message message = createTestMessage(channelId);
        FilterContext context = new FilterContext();
        FilterResult result = filter.filter(message, context);
        assertEquals(FilterResult.REJECT, result, "非监控频道应该拒绝消息");
    }
    
    @Test
    @DisplayName("更新频道监控状态为true后，过滤器应该接受该频道的消息")
    void testUpdateChannelToMonitoring() throws InterruptedException {
        // 创建非监控频道
        Long channelId = -1001234567892L;
        ChannelCreateDTO createDto = new ChannelCreateDTO();
        createDto.setChannelId(channelId);
        createDto.setChannelUsername("test_channel_3");
        createDto.setChannelTitle("测试频道3");
        createDto.setMonitoringStatus(false);
        
        ChannelVO vo = channelService.create(createDto);
        Thread.sleep(100);
        
        // 验证消息被拒绝
        TdApi.Message message = createTestMessage(channelId);
        FilterContext context = new FilterContext();
        FilterResult result = filter.filter(message, context);
        assertEquals(FilterResult.REJECT, result, "更新前应该拒绝消息");
        
        // 更新为监控频道
        ChannelUpdateDTO updateDto = new ChannelUpdateDTO();
        updateDto.setMonitoringStatus(true);
        channelService.update(vo.getId(), updateDto);
        Thread.sleep(100);
        
        // 验证消息被接受
        context = new FilterContext();
        result = filter.filter(message, context);
        assertEquals(FilterResult.ACCEPT, result, "更新后应该接受消息");
    }
    
    @Test
    @DisplayName("更新频道监控状态为false后，过滤器应该拒绝该频道的消息")
    void testUpdateChannelToNonMonitoring() throws InterruptedException {
        // 创建监控频道
        Long channelId = -1001234567893L;
        ChannelCreateDTO createDto = new ChannelCreateDTO();
        createDto.setChannelId(channelId);
        createDto.setChannelUsername("test_channel_4");
        createDto.setChannelTitle("测试频道4");
        createDto.setMonitoringStatus(true);
        
        ChannelVO vo = channelService.create(createDto);
        Thread.sleep(100);
        
        // 验证消息被接受
        TdApi.Message message = createTestMessage(channelId);
        FilterContext context = new FilterContext();
        FilterResult result = filter.filter(message, context);
        assertEquals(FilterResult.ACCEPT, result, "更新前应该接受消息");
        
        // 更新为非监控频道
        ChannelUpdateDTO updateDto = new ChannelUpdateDTO();
        updateDto.setMonitoringStatus(false);
        channelService.update(vo.getId(), updateDto);
        Thread.sleep(100);
        
        // 验证消息被拒绝
        context = new FilterContext();
        result = filter.filter(message, context);
        assertEquals(FilterResult.REJECT, result, "更新后应该拒绝消息");
    }
    
    @Test
    @DisplayName("删除频道后，过滤器应该拒绝该频道的消息")
    void testDeleteChannel() throws InterruptedException {
        // 创建监控频道
        Long channelId = -1001234567894L;
        ChannelCreateDTO createDto = new ChannelCreateDTO();
        createDto.setChannelId(channelId);
        createDto.setChannelUsername("test_channel_5");
        createDto.setChannelTitle("测试频道5");
        createDto.setMonitoringStatus(true);
        
        ChannelVO vo = channelService.create(createDto);
        Thread.sleep(100);
        
        // 验证消息被接受
        TdApi.Message message = createTestMessage(channelId);
        FilterContext context = new FilterContext();
        FilterResult result = filter.filter(message, context);
        assertEquals(FilterResult.ACCEPT, result, "删除前应该接受消息");
        
        // 删除频道
        channelService.deleteById(vo.getId());
        Thread.sleep(100);
        
        // 验证消息被拒绝
        context = new FilterContext();
        result = filter.filter(message, context);
        assertEquals(FilterResult.REJECT, result, "删除后应该拒绝消息");
    }
    
    @Test
    @DisplayName("批量操作后，过滤器缓存应该正确")
    void testBatchOperations() throws InterruptedException {
        // 创建多个频道
        Long channelId1 = -1001234567895L;
        Long channelId2 = -1001234567896L;
        Long channelId3 = -1001234567897L;
        
        // 频道1：监控
        ChannelCreateDTO dto1 = new ChannelCreateDTO();
        dto1.setChannelId(channelId1);
        dto1.setChannelUsername("channel_1");
        dto1.setChannelTitle("频道1");
        dto1.setMonitoringStatus(true);
        ChannelVO vo1 = channelService.create(dto1);
        
        // 频道2：非监控
        ChannelCreateDTO dto2 = new ChannelCreateDTO();
        dto2.setChannelId(channelId2);
        dto2.setChannelUsername("channel_2");
        dto2.setChannelTitle("频道2");
        dto2.setMonitoringStatus(false);
        ChannelVO vo2 = channelService.create(dto2);
        
        // 频道3：监控
        ChannelCreateDTO dto3 = new ChannelCreateDTO();
        dto3.setChannelId(channelId3);
        dto3.setChannelUsername("channel_3");
        dto3.setChannelTitle("频道3");
        dto3.setMonitoringStatus(true);
        ChannelVO vo3 = channelService.create(dto3);
        
        Thread.sleep(200);
        
        // 验证过滤结果
        assertEquals(FilterResult.ACCEPT, filter.filter(createTestMessage(channelId1), new FilterContext()));
        assertEquals(FilterResult.REJECT, filter.filter(createTestMessage(channelId2), new FilterContext()));
        assertEquals(FilterResult.ACCEPT, filter.filter(createTestMessage(channelId3), new FilterContext()));
        
        // 更新频道2为监控
        ChannelUpdateDTO updateDto = new ChannelUpdateDTO();
        updateDto.setMonitoringStatus(true);
        channelService.update(vo2.getId(), updateDto);
        Thread.sleep(100);
        
        // 验证频道2现在被接受
        assertEquals(FilterResult.ACCEPT, filter.filter(createTestMessage(channelId2), new FilterContext()));
        
        // 删除频道1
        channelService.deleteById(vo1.getId());
        Thread.sleep(100);
        
        // 验证频道1现在被拒绝
        assertEquals(FilterResult.REJECT, filter.filter(createTestMessage(channelId1), new FilterContext()));
        
        // 验证其他频道不受影响
        assertEquals(FilterResult.ACCEPT, filter.filter(createTestMessage(channelId2), new FilterContext()));
        assertEquals(FilterResult.ACCEPT, filter.filter(createTestMessage(channelId3), new FilterContext()));
    }
    
    /**
     * 创建测试消息
     */
    private TdApi.Message createTestMessage(Long chatId) {
        TdApi.Message message = new TdApi.Message();
        message.chatId = chatId;
        message.id = System.currentTimeMillis();
        message.date = (int) (System.currentTimeMillis() / 1000);
        message.content = new TdApi.MessageText(
            new TdApi.FormattedText("测试消息", new TdApi.TextEntity[0]),
            null
        );
        return message;
    }
}
