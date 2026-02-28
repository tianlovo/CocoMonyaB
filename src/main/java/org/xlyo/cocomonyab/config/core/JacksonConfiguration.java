package org.xlyo.cocomonyab.config.core;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.text.SimpleDateFormat;

/**
 * Jackson配置
 * 提供ObjectMapper Bean用于JSON序列化和反序列化
 */
@Configuration
public class JacksonConfiguration {
    
    /**
     * 配置ObjectMapper
     */
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        
        // 注册 Java 8 日期时间模块
        mapper.registerModule(new JavaTimeModule());
        
        // 禁用将日期写为时间戳
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        
        // 忽略未知属性（提高兼容性）
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        
        // 配置多态类型处理，支持 TdApi 的抽象类型和数组
        BasicPolymorphicTypeValidator validator = BasicPolymorphicTypeValidator.builder()
            .allowIfBaseType(Object.class)  // 允许所有类型（包括数组）
            .build();
        
        mapper.activateDefaultTyping(validator, ObjectMapper.DefaultTyping.NON_FINAL);
        
        return mapper;
    }
    
    /**
     * 配置导出专用的ObjectMapper
     * 不启用多态类型处理，生成标准JSON格式
     */
    @Bean
    @Qualifier("exportObjectMapper")
    public ObjectMapper exportObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        
        // 注册 Java 8 日期时间模块
        mapper.registerModule(new JavaTimeModule());
        
        // 设置日期格式
        mapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));
        
        // 禁用将日期写为时间戳
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        
        // 忽略未知属性（提高兼容性）
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        
        // 不启用多态类型处理，保持标准JSON格式
        
        return mapper;
    }
}
