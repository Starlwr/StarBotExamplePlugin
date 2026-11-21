package com.example;

import com.starlwr.bot.core.message.Message;
import com.starlwr.bot.core.plugin.StarBotComponent;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;

import java.util.ArrayList;
import java.util.List;

/**
 * 示例 3: 拦截并修改 StarBot 全局默认行为
 * 示例 StarBot 消息推送拦截器
 * 给推送消息中以中文结尾的每一行加上配置中的后缀
 */
@Aspect
@RequiredArgsConstructor // 生成构造函数，供 Spring 注入配置类
@StarBotComponent // 使用该注解将此类注册为 StarBot 组件，会被 StarBot 扫描并注册至 Spring 容器中
public class StarBotExampleMeowAdder {
    /**
     * 示例插件配置类
     */
    private final StarBotExampleProperties properties;

    /**
     * 匹配以中文字符结尾的正则表达式
     */
    private static final String CHINESE_END_PATTERN = ".*[\\u4e00-\\u9fa5]$";

    /**
     * 拦截 StarBot 全局消息发送方法
     */
    @Pointcut("execution(* com.starlwr.bot.core.message.StarBotMessageSender.send(..))")
    public void sendMethod() {}

    @Around("sendMethod()")
    public Object aroundSendMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        // 读取消息后缀，未启用拦截时直接发送原消息
        Object[] args = joinPoint.getArgs();
        String suffix = properties.getMessageSuffix();
        if (!properties.isInterceptMessages() || StringUtils.isEmpty(suffix)) { // 根据配置决定是否添加后缀
            return joinPoint.proceed(args);
        }

        if (args.length > 0 && args[0] instanceof Message message) {
            // 获取原消息内容
            String originalContent = message.getContent();

            // 拆分每一行
            String[] lines = originalContent.split("\n");

            // 创建一个新列表，存储修改后的每行消息内容
            List<String> newLines = new ArrayList<>();

            // 循环处理每一行
            for (String line: lines) {
                if (line.matches(CHINESE_END_PATTERN)) {
                    // 如果当前行以中文结尾，添加一个后缀
                    newLines.add(line + suffix);
                } else {
                    // 否则不添加后缀
                    newLines.add(line);
                }
            }

            // 重新拼接处理后的消息内容
            String newContent = String.join("\n", newLines);

            // 修改原参数
            message.setContent(newContent);
        }

        // 继续调用发送方法
        return joinPoint.proceed(args);
    }
}
