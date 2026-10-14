<div align="center">

![logo](https://bot.starlwr.com/images/static/logo.jpg)

**<h2>StarBot 示例插件</h2>**
</div>

## 目录

- [快速开始](#快速开始)
- [项目结构](#项目结构)
- [示例功能](#示例功能)
- [开发说明](#开发说明)
- [依赖管理](#依赖管理)
- [构建与部署](#构建与部署)

## 快速开始

1. 克隆本示例项目作为模板创建新项目
2. 修改 `pom.xml` 中的项目信息, 该部分信息会作为插件元数据, 在被 StarBot 加载时输出到控制台和日志中 (groupId, artifactId, version, name, description, url, developers 等)
3. 开发你的插件功能, 开发时可正常使用绝大多数 Spring 注解, 可参考启动监听、弹幕监听、消息修改和命令注册四个[示例功能](#示例功能), 按需保留或移除示例类
4. 使用 Maven 构建项目: `mvn clean package`
5. 将 `target` 中生成的 JAR 文件放入 StarBot 启动工作目录下的 `plugins` 目录, 启动或重启 StarBot, 并检查插件加载日志, 详见[部署插件](#部署插件)

## 项目结构

StarBot 插件使用标准的 Maven 项目结构：

```
starbot-example-plugin/
├── pom.xml                                                # Maven 项目配置文件
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/example/                               # 插件代码包
│   │   │       ├── StarBotExampleStartEventListener.java  # 示例功能: 启动事件监听器
│   │   │       ├── StarBotExampleDanmuEventListener.java  # 示例功能: 弹幕监听器
│   │   │       ├── StarBotExampleMeowAdder.java           # 示例功能: 推送消息修改器
│   │   │       └── HelloCommand.java                     # 示例功能: 命令注册与回复
│   │   └── resources/
│   └── test/                                              # 测试代码目录
└── target/                                                # 构建输出目录
    └── starbot-example-plugin-1.0.0.jar                   # 构建后的插件 JAR 文件
```

## 示例功能

本项目包含四个示例, 使用模板开发自己的插件时可按需保留或移除：

| 示例类 | 功能 | 验证方式与运行条件 |
| --- | --- | --- |
| [StarBotExampleStartEventListener.java](src/main/java/com/example/StarBotExampleStartEventListener.java) | 监听 `ApplicationReadyEvent`, 在主程序启动完毕后执行一次 | 插件加载成功且主程序启动完毕后, 日志中会输出“主程序已启动完毕，示例插件已开始监听弹幕事件” |
| [StarBotExampleDanmuEventListener.java](src/main/java/com/example/StarBotExampleDanmuEventListener.java) | 监听 `DanmuEvent`, 打印弹幕发送者和内容 | 需要安装对应直播平台插件, 并配置直播间监听; 收到弹幕后检查“示例插件接收到弹幕事件”日志 |
| [StarBotExampleMeowAdder.java](src/main/java/com/example/StarBotExampleMeowAdder.java) | 使用 Spring AOP 拦截 `StarBotMessageSender.send(..)`, 为消息中每行以中文字符结尾的内容追加“喵” | 需要接入可发送消息的推送平台; 例如“测试消息”会变为“测试消息喵”, 以标点或英文结尾的行不会追加 |
| [HelloCommand.java](src/main/java/com/example/HelloCommand.java) | 实现 `StarBotCommand`, 注册“你好”命令和 `hello` 别名, 回复“你好，[昵称]” | 需要接入支持 StarBot 命令框架的消息平台, 例如 OneBot 适配器插件; 默认发送“你好”或 `hello` 即可触发 |

> **注意**: 部分示例具有副作用, 例如 `StarBotExampleMeowAdder` 会影响所有经过 `StarBotMessageSender.send(..)` 发送的消息, 包括符合条件的命令回复。若只需要参考示例, 请在构建前移除这些类。

## 开发说明

### 插件信息

每个 StarBot 插件都需要在 `pom.xml` 文件中定义其元数据, 其内容将在被 StarBot 加载时输出到控制台和日志中：

> 2025-11-22 01:58:39.062  INFO 64528 --- [                main] c.s.bot.core.plugin.StarBotPluginLoader  : 已注册插件 StarBotExamplePlugin v1.0.0 --Author: Example Plugin For StarBot

```xml
<!-- 组名，通常使用反向域名，必填，与 artifactId 共同构成插件唯一标识 -->
<groupId>com.example</groupId>
<!-- 插件 ID，必填，与 groupId 共同构成插件唯一标识 -->
<artifactId>starbot-example-plugin</artifactId>
<!-- 插件版本，必填，会在插件加载时输出至 StarBot 日志中 -->
<version>1.0.0</version>
<!-- 插件名称，必填，会在插件加载时输出至 StarBot 日志中 -->
<name>StarBotExamplePlugin</name>
<!-- 插件描述，必填，会在插件加载时输出至 StarBot 日志中 -->
<description>Example Plugin For StarBot</description>
<!-- 插件主页 URL -->
<url>https://www.example.com</url>

<!-- 插件作者信息 -->
<developers>
    <developer>
        <!-- 插件作者 ID，必填，可与名称保持一致 -->
        <id>Author</id>
        <!-- 插件作者名称，必填，会在插件加载时输出至 StarBot 日志中 -->
        <name>Author</name>
        <!-- 插件作者邮箱 -->
        <email>example@example.com</email>
    </developer>
</developers>
```

### 打包配置

> **注意**: 请不要随意修改 `pom.xml` 的 `<build>` 构建配置部分，否则可能导致插件无法被 StarBot 加载

模板中的构建配置会在打包时完成以下工作：

1. `starbot-plugin-processor` 根据 Maven 项目信息生成 `plugin.json`, 并根据依赖关系生成 `dependency.json`, 分别记录插件元数据、前置插件和普通运行时依赖
2. `maven-resources-plugin` 将上述文件复制到构建输出目录, 最终打入插件 JAR 的根目录, 供 StarBot 加载时读取
3. 资源复制配置排除 `application.yml`、`application.properties`、`application-dev.yml` 和 `application-dev.properties`, 这些文件不会随模板配置打入插件 JAR

插件由 StarBot 主程序加载, 无需添加启动类或将其打包为 Spring Boot 可执行 JAR。插件运行所需的配置建议放在 StarBot 主程序的外部配置文件中, 通过 Spring 的配置机制读取。

### 开发命令

命令类需要实现 `StarBotCommand` 接口, 并添加 `@StarBotComponent` 注解注册至 Spring 容器, StarBot 会自动发现并注册命令。可参考 [HelloCommand.java](src/main/java/com/example/HelloCommand.java)：

接口中的所有方法如下, 其中 `getName()`、`getDescription()` 和 `execute(...)` 必须实现, 其余方法已有默认实现, 可按需重写：

| 方法 | 作用 | 默认行为 | HelloCommand 中的行为 |
| --- | --- | --- | --- |
| `getId()` | 命令唯一标识, 用于注册、查找和禁用命令, 自定义时应避免与其他命令重复 | 返回实现类的全路径类名 | 重写为 `starbot.example.hello` |
| `getName()` | 命令名称, 默认作为命令唤醒词 | 无默认实现, 必须实现 | 返回 `你好` |
| `getAliases()` | 命令别名列表, 默认作为额外的命令唤醒词 | 返回空列表, 无别名 | 返回 `List.of("hello")` |
| `getDescription()` | 命令简介, 在帮助信息中展示 | 无默认实现, 必须实现 | 返回“向你问好，并带上你的昵称” |
| `getUsage()` | 命令用法说明, 可描述参数格式, 在帮助信息中展示 | 返回 `getName()` | 沿用默认实现, 返回 `你好` |
| `getArgumentCount()` | 设置命令所需的文本参数个数, 非负数表示必须恰好具有该数量的参数 | 返回 `-1`, 不限制参数个数, 由命令自行处理 | 返回 `0`, 不接受文本参数 |
| `getPermission()` | 设置执行命令所需的默认权限, 返回 `CommandPermission`, 可由主程序配置覆盖 | 返回 `CommandPermission.EVERYONE`, 所有人均可使用 | 沿用默认实现 |
| `getScope()` | 设置命令适用的会话类型, 返回 `CommandScope` | 返回 `CommandScope.ALL`, 群聊和私聊均可使用 | 沿用默认实现 |
| `isDisableable()` | 设置是否允许通过禁用命令禁用当前命令 | 返回 `true`, 允许禁用; 配置文件中的全局禁用设置不受此限制 | 沿用默认实现 |
| `isTrigger(CommandContext context)` | 根据命令上下文判断当前消息是否触发命令, 返回 `true` 表示匹配 | 不区分大小写匹配命令名称和别名 | 沿用默认实现, 匹配 `你好` 或 `hello` |
| `execute(CommandContext context)` | 命令触发并通过策略检查后的执行逻辑, 可读取昵称、参数等信息, 并使用 `context.reply(...)` 回复 | 无默认实现, 必须实现 | 回复“你好，[昵称]” |

`CommandPermission` 可选值为 `EVERYONE` (所有人)、`OWNER` (仅主人) 和 `ADMIN` (主人、当前群群主或管理员, 私聊中仅主人可用), 主人账号由主程序的 `starbot.core.command.owners` 配置。`CommandScope` 可选值为 `ALL` (群聊和私聊)、`GROUP` (仅群聊) 和 `PRIVATE` (仅私聊)。

主程序可通过 `starbot.core.command.permissions` 按命令 ID 覆盖 `getPermission()` 返回的默认权限, 对所有平台和会话统一生效, 未配置的命令沿用默认权限。例如将 `starbot.example.hello` 配置为 `OWNER`, 即可将示例命令限制为仅主人使用。帮助菜单同步展示覆盖后的可用状态, 命令的会话范围和禁用规则仍然生效。

`isTrigger(...)` 应保持快速且无副作用, 只负责判断触发条件, 实际业务逻辑放在 `execute(...)` 中。权限、会话范围、禁用检查和参数个数由 StarBot 统一处理, 无需在触发规则中重复判断。

命令触发还受 StarBot 主程序的 `starbot.core.command` 配置控制：`prefix` 默认为空字符串, 表示无需前缀; `mention` 默认为 `false`, 表示群聊中无需提及机器人。若配置了前缀或要求提及机器人, 测试“你好”命令时也需要满足这些条件。

### 注意事项

>- 开发插件时, 可以使用绝大多数的 Spring 注解, 例如使用 `@Controller` 创建 API 接口, 使用 `@EventListener` 监听事件等
>- 需要注册至 Spring 容器或使用 Spring 机制 (例如事件机制) 的类, 需要在类上使用 `@StarBotComponent` 注解, 该注解会将类注册为 StarBot 组件, 并被 StarBot 扫描并注册至 Spring 容器中  
>- 使用了 `@StarBotComponent` 注解的类, 类名不可以与 StarBot 本体或其他插件中的类名重复, 请命名时尽量避免过于简单或过于通用的命名
>- StarBot 内部大量使用了 Spring 的事件机制, 插件可以通过创建事件监听器来处理这些事件, 常用事件类型请参考 [StarBotCore](https://github.com/Starlwr/StarBotCore) 项目相关文档

## 依赖管理

StarBot 插件使用 Maven 进行依赖管理。构建时由 Maven 解析依赖, 打包时由 `starbot-plugin-processor` 生成依赖清单, StarBot 启动时再根据清单处理缺失的普通运行时依赖。

| 依赖作用域 | 是否记录到运行时依赖清单 | 用途 |
| --- | --- | --- |
| `compile` (默认) | 是 | 编译和运行插件都需要的依赖 |
| `runtime` | 是 | 运行插件时需要的依赖 |
| `provided` | 否 | 编译时需要, 运行时由环境提供或无需保留的依赖, 例如 Lombok |
| `test` | 否 | 仅用于测试的依赖 |

普通运行时依赖已存在于 StarBot 的 `lib` 或 `plugins-lib` 目录时会复用, 缺失时可自动下载至 `plugins-lib`。自动下载由主程序的 `starbot.core.plugin.auto-download-dependency` 配置控制, 默认开启, 也可使用 `--skip-download-dependency` 启动参数临时跳过。

### 核心依赖

每个 StarBot 插件必须直接依赖 StarBotCore 或通过依赖其他插件的方式间接依赖 StarBotCore, 建议开发时使用最新版本：

```xml
<dependency>
    <groupId>com.starlwr</groupId>
    <artifactId>starbot-core</artifactId>
    <version>3.0.0</version>
</dependency>
```

### 依赖其他插件

需要使用其他插件提供的功能时, 可在 `pom.xml` 的 `<dependencies>` 中声明对应插件依赖。

StarBot 官方插件均发布在 StarBot 官方 Maven 仓库中, 保留模板中的仓库配置并添加依赖后, Maven 会在构建时自动获取对应插件, 无需手动安装至本地 Maven 仓库。例如, 依赖 Bilibili 插件：

```xml
<dependency>
    <groupId>com.starlwr</groupId>
    <artifactId>starbot-bilibili</artifactId>
    <version>1.0.0</version>
</dependency>
```

第三方前置插件请按照作者提供的方式引入：若已发布至可访问的 Maven 仓库, 添加对应仓库和依赖配置即可; 若仅提供 JAR 文件, 则需要先按作者提供的 Maven 坐标安装至本地 Maven 仓库, 再添加依赖配置。

构建时由 Maven 获取前置插件, 部署时则需要确保这些插件已安装到 StarBot 的 `plugins` 目录中。

### 添加第三方依赖

插件可以按需添加其他第三方依赖。例如, 添加一个编译和运行时都需要的缓存库：

```xml
<dependency>
    <groupId>com.github.ben-manes.caffeine</groupId>
    <artifactId>caffeine</artifactId>
    <version>3.2.0</version>
</dependency>
```

上述依赖未指定 `scope`, 默认为 `compile`, 会被记录到普通运行时依赖清单, 供 StarBot 在缺失时自动下载。

Lombok 仅在编译时使用, 可保留模板中的以下配置, 不会加入运行时依赖清单：

```xml
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <scope>provided</scope>
    <optional>true</optional>
</dependency>
```

构建时使用插件项目 `pom.xml` 中的 Maven 仓库配置, 运行时下载普通依赖则使用 StarBot 主程序的 `starbot.core.plugin.maven-base-urls` 配置。若某个依赖仅发布在其他 Maven 仓库中, 请同时确认构建环境和 StarBot 运行环境均可访问对应仓库。

## 构建与部署

### 构建插件

在插件项目根目录 (`pom.xml` 所在目录) 使用 Maven 构建 StarBot 插件：

```bash
mvn clean package
```

构建成功后，插件 JAR 文件将生成在 `target` 目录中，文件名格式为 `{artifactId}-{version}.jar`，例如 `starbot-example-plugin-1.0.0.jar`。

### 部署插件

StarBot 在启动时扫描启动工作目录下的 `plugins` 文件夹。启动工作目录是运行 StarBot 时使用的当前目录, 使用 IDE 或启动脚本运行时请检查工作目录设置。

1. 如果 StarBot 正在运行, 先停止主程序
2. 在 StarBot 的启动工作目录下找到或创建 `plugins` 文件夹
3. 将构建后的插件 JAR 文件复制到该目录, 更新已有插件时请移走旧版 JAR, 避免同时保留同一插件的多个版本
4. 启动 StarBot, 等待依赖处理和插件加载完成
5. 检查“已加载插件 StarBotExamplePlugin v1.0.0”日志, 若修改了插件名称或版本, 请以实际信息为准; 命令示例还会输出“已注册 你好 命令: starbot.example.hello”日志, 随后可按[示例功能](#示例功能)中的方式验证

若当前插件依赖其他插件, 请将所需前置插件的 JAR 文件一并放入 `plugins` 目录。前置插件缺失时, StarBot 会记录“未安装插件 ... 的前置依赖插件 ..., 无法加载该插件”日志, 并跳过加载依赖它的插件。

首次启动时, 如果缺少普通运行时依赖且已开启自动下载, StarBot 会将依赖下载到启动工作目录下的 `plugins-lib` 文件夹, 然后尝试自动重启。请确保运行环境可以访问配置的 Maven 仓库, 并且可以执行 `java` 命令; 自动重启失败时, 可在依赖下载完成后手动启动 StarBot。

若插件未成功加载, 可优先检查以下项目：

- JAR 是否放在正确工作目录下的 `plugins` 文件夹中
- JAR 根目录是否包含 `plugin.json` 和 `dependency.json`
- 插件组件类是否添加了 `@StarBotComponent` 注解
- 日志中是否出现依赖下载失败、前置插件缺失或组件加载异常
