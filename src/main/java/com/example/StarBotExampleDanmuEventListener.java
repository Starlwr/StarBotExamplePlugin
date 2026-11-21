package com.example;

import com.starlwr.bot.core.event.live.common.DanmuEvent;
import com.starlwr.bot.core.plugin.StarBotComponent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;

/**
 * 示例 2: 接收 StarBot 事件，收到指定事件后触发自定义逻辑
 * 示例 StarBot 弹幕事件监听器
 * 该监听器会在接收到弹幕事件时打印发送者和内容
 */
@Slf4j
@RequiredArgsConstructor // 生成构造函数，供 Spring 注入配置类
@StarBotComponent // 使用该注解将此类注册为 StarBot 组件，会被 StarBot 扫描并注册至 Spring 容器中
public class StarBotExampleDanmuEventListener {
    /**
     * 示例插件配置类
     */
    private final StarBotExampleProperties properties;

    @EventListener
    public void handle(DanmuEvent event) {
        if (properties.isDanmuLog()) { // 根据配置决定是否打印日志
            log.info("示例插件接收到弹幕事件: {} -> {}", event.getSender().getUname(), event.getContent());
        }
    }
}
