package com.example;

import com.starlwr.bot.core.plugin.StarBotComponent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;

/**
 * 示例 1: 主程序启动后触发自定义逻辑
 * 示例 StarBot 启动事件监听器
 * 该监听器会在主程序启动完毕后触发一次
 */
@Slf4j
@RequiredArgsConstructor // 生成构造函数，供 Spring 注入配置类
@StarBotComponent // 使用该注解将此类注册为 StarBot 组件，会被 StarBot 扫描并注册至 Spring 容器中
public class StarBotExampleStartEventListener {
    /**
     * 示例插件配置类
     */
    private final StarBotExampleProperties properties;

    @Order(0) // 如需指定执行顺序，使用 @Order 注解，数值越小，执行时机越早
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationEvent() {
        if (properties.isStartupLog()) { // 根据配置决定是否打印日志
            log.info("主程序已启动完毕, 示例插件已就绪");
        }
    }
}
