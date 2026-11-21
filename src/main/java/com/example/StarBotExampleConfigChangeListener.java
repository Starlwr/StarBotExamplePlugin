package com.example;

import com.starlwr.bot.core.event.config.StarBotConfigChangedEvent;
import com.starlwr.bot.core.plugin.StarBotComponent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;

/**
 * 示例 5: 监听实时生效的配置变更
 * 示例 StarBot 配置变更事件监听器
 * 该监听器会在消息后缀变更时打印变更前后的值和当前生效值
 */
@Slf4j
@RequiredArgsConstructor // 生成构造函数，供 Spring 注入配置类
@StarBotComponent // 使用该注解将此类注册为 StarBot 组件，会被 StarBot 扫描并注册至 Spring 容器中
public class StarBotExampleConfigChangeListener {
    /**
     * 消息后缀的完整配置键
     */
    private static final String MESSAGE_SUFFIX_KEY = "starbot.example.message-suffix";

    /**
     * 示例插件配置类
     */
    private final StarBotExampleProperties properties;

    @EventListener
    public void onConfigChanged(StarBotConfigChangedEvent event) {
        if (!event.isChanged(MESSAGE_SUFFIX_KEY)) { // 忽略与消息后缀无关的配置变更
            return;
        }

        log.info("消息后缀已变更: {} -> {}", event.getOldValue(MESSAGE_SUFFIX_KEY), event.getNewValue(MESSAGE_SUFFIX_KEY));
        log.info("当前生效的消息后缀: {}", properties.getMessageSuffix());
    }
}
