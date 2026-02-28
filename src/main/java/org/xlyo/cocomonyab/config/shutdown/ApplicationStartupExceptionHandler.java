package org.xlyo.cocomonyab.config.shutdown;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationFailedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.xlyo.cocomonyab.event.startup.StartupException;

/**
 * 应用启动异常处理器
 * <p>
 * 负责捕获应用启动过程中的致命错误，并触发优雅关闭流程。
 * 监听 Spring Boot 的 ApplicationFailedEvent 事件，当启动失败时自动调用 GracefulShutdownHandler。
 * </p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ApplicationStartupExceptionHandler {
    
    private final GracefulShutdownHandler shutdownHandler;
    
    /**
     * 监听应用启动失败事件
     * <p>
     * 当 Spring Boot 应用启动失败时，此方法会被自动调用。
     * 如果失败原因是 StartupException，则触发优雅关闭流程。
     * </p>
     *
     * @param event 应用启动失败事件
     */
    @EventListener
    public void onApplicationFailed(ApplicationFailedEvent event) {
        Throwable exception = event.getException();
        
        log.error("检测到应用启动失败事件");
        
        // 查找 StartupException
        StartupException startupException = findStartupException(exception);
        
        if (startupException != null) {
            log.error("启动失败原因: StartupException");
            shutdownHandler.handleStartupFailure(startupException);
        } else {
            log.error("启动失败原因: 其他异常", exception);
            // 对于非 StartupException，也执行优雅关闭
            // 但不以非零退出码退出（让 Spring Boot 自己处理）
            log.error("应用将由 Spring Boot 处理退出");
        }
    }
    
    /**
     * 从异常链中查找 StartupException
     * <p>
     * 递归遍历异常的 cause 链，查找 StartupException。
     * </p>
     *
     * @param throwable 异常
     * @return StartupException 如果找到，否则返回 null
     */
    private StartupException findStartupException(Throwable throwable) {
        if (throwable == null) {
            return null;
        }
        
        if (throwable instanceof StartupException) {
            return (StartupException) throwable;
        }
        
        return findStartupException(throwable.getCause());
    }
}
