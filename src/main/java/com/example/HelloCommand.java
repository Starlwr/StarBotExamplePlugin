package com.example;

import com.starlwr.bot.core.command.CommandContext;
import com.starlwr.bot.core.command.StarBotCommand;
import com.starlwr.bot.core.plugin.StarBotComponent;

import java.util.List;

/**
 * 示例 4: 注册 StarBot 命令
 * 示例 StarBot 命令
 * 该命令会在各推送平台接收到 “你好” 或 “hello” 时触发，并回复 “你好，[昵称]”
 * 该示例仅重写了 StarBotCommand 接口的部分方法，权限、触发规则等使用接口默认实现，可通过重写对应方法自定义命令行为
 */
@StarBotComponent // 使用该注解将此类注册为 StarBot 组件，会被 StarBot 扫描并注册至 Spring 容器中
public class HelloCommand implements StarBotCommand { // 实现 StarBotCommand 接口，即可被 StarBot 自动注册为命令
    /**
     * 命令唯一标识，如不重写此方法，默认为实现类的全路径类名
     */
    @Override
    public String getId() {
        return "starbot.example.hello"; // 通过重写方法，可以自定义命令 ID，要求唯一
    }

    /**
     * 命令名称，默认情况下会作为命令唤醒词
     */
    @Override
    public String getName() {
        return "你好";
    }

    /**
     * 命令别名，默认情况下会作为命令唤醒词，如不重写此方法默认无别名
     */
    @Override
    public List<String> getAliases() {
        return List.of("hello"); // 通过重写方法，可以自定义多个命令别名
    }

    /**
     * 命令简介
     */
    @Override
    public String getDescription() {
        return "向你问好，并带上你的昵称";
    }

    /**
     * 命令所需参数个数，参数自动按空白分隔，非负数表示由 StarBot 统一检查参数个数，数量不符时不会触发命令，如不重写此方法，默认 -1 表示不限制，由命令自行处理
     */
    @Override
    public int getArgumentCount() {
        return 0; // 此命令不需要参数，“你好 1” 或 “hello 1” 不会触发
    }

    /**
     * 触发命令后命令执行的方法
     * @param context 命令上下文
     */
    @Override
    public void execute(CommandContext context) {
        context.reply("你好，" + context.getNickname());
    }
}
