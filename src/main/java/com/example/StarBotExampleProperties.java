package com.example;

import com.starlwr.bot.core.config.StarBotConfiguration;
import com.starlwr.bot.core.plugin.StarBotComponent;
import lombok.Getter;
import lombok.Setter;

/**
 * 示例插件配置类
 */
@Getter
@Setter
@StarBotComponent
@StarBotConfiguration(prefix = "starbot.example")
public class StarBotExampleProperties {
    /**
     * 是否输出启动日志
     */
    private boolean startupLog = true;

    /**
     * 是否输出弹幕日志
     */
    private boolean danmuLog = true;

    /**
     * 是否全局拦截推送消息
     */
    private boolean interceptMessages = true;

    /**
     * 全局拦截推送消息后添加的后缀
     */
    private String messageSuffix = "喵";

    /**
     * 是否注册你好命令
     */
    private boolean helloEnabled = true;

    /**
     * 你好命令回复前缀
     */
    private String greeting = "你好，";

}
