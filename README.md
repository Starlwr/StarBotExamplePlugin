<div align="center">

![logo](https://bot.starlwr.com/images/static/logo.jpg)

[![Java](https://img.shields.io/badge/Java-17%2B-blue)](https://www.java.com/)
[![StarBotCore](https://img.shields.io/badge/StarBotCore-3.0.0-blue)](https://github.com/Starlwr/StarBotCore)
[![License](https://img.shields.io/badge/License-AGPL--3.0-green)](LICENSE)

**StarBot 3.0 示例插件**

</div>

## 目录

- [快速开始](#快速开始)
- [项目结构](#项目结构)
- [示例功能](#示例功能)
- [插件配置](#插件配置)
- [开发说明](#开发说明)
- [依赖管理](#依赖管理)
- [构建与部署](#构建与部署)

## 快速开始

1. 克隆本示例项目作为模板创建新项目
2. 修改 `pom.xml` 中的项目信息，包括 `groupId`、`artifactId`、`version`、`name`、`description`、`url` 和 `developers`。这些信息会写入插件元数据，在 StarBot 加载插件时输出到控制台和日志
3. 开发插件功能。开发时可以使用大多数 Spring 注解，参考[示例功能](#示例功能)中的启动监听、弹幕监听、消息修改、命令注册和配置变更监听，按需保留或移除示例类
4. 使用 Maven 构建项目：`mvn clean package`
5. 把 `target` 目录中生成的 JAR 放入 StarBot 启动工作目录下的 `plugins` 文件夹，启动或重启 StarBot，并检查插件加载日志，详见[部署插件](#部署插件)

## 项目结构

StarBot 插件使用标准的 Maven 项目结构：

```
StarBotExamplePlugin/
├── pom.xml                                                 # Maven 项目配置与插件元数据
├── README.md                                               # 项目说明
├── CONFIG-METADATA-GUIDE.md                                # 配置元数据编写指南
├── LICENSE                                                 # 开源许可证
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/example/                                # 插件代码包
│   │   │       ├── StarBotExampleStartEventListener.java   # 示例 1: 启动事件监听器
│   │   │       ├── StarBotExampleDanmuEventListener.java   # 示例 2: 弹幕事件监听器
│   │   │       ├── StarBotExampleMeowAdder.java            # 示例 3: 推送消息拦截与修改
│   │   │       ├── HelloCommand.java                       # 示例 4: 命令注册
│   │   │       ├── StarBotExampleConfigChangeListener.java # 示例 5: 配置变更事件监听器
│   │   │       └── StarBotExampleProperties.java           # 配置类与默认值
│   │   └── resources/
│   │       └── config/
│   │           └── config.json                             # 配置元数据
│   └── test/
│       └── java/
│           └── com/example/                                # 测试代码（可选）
└── target/                                                 # 构建输出目录
    └── starbot-example-plugin-1.0.0.jar                    # 构建出的插件 JAR
```

## 示例功能

本项目包含五个示例，用模板开发自己的插件时，可以按需保留或移除。

| 示例类 | 功能 | 验证方式与运行条件 |
| --- | --- | --- |
| [StarBotExampleStartEventListener.java](src/main/java/com/example/StarBotExampleStartEventListener.java) | 监听 `ApplicationReadyEvent`，在主程序启动完毕后执行一次 | 开启「输出启动日志」并重启，启动完成时会输出「主程序已启动完毕, 示例插件已就绪」 |
| [StarBotExampleDanmuEventListener.java](src/main/java/com/example/StarBotExampleDanmuEventListener.java) | 监听 `DanmuEvent`，输出弹幕的发送者和内容 | 安装对应的直播平台插件并配置直播间监听，开启「输出弹幕日志」后查看「示例插件接收到弹幕事件」日志 |
| [StarBotExampleMeowAdder.java](src/main/java/com/example/StarBotExampleMeowAdder.java) | 使用 Spring AOP 拦截 `StarBotMessageSender.send(..)`，为每行以中文结尾的内容追加配置中的后缀 | 接入推送平台并开启「全局拦截推送消息」，使用默认后缀时，「测试消息」会变成「测试消息喵」 |
| [HelloCommand.java](src/main/java/com/example/HelloCommand.java) | 实现 `StarBotCommand`，注册「你好」命令和 `hello` 别名，默认回复「你好，[昵称]」 | 接入支持 StarBot 命令框架的推送平台并启用示例命令，按主程序的命令触发规则发送「你好」或 `hello` |
| [StarBotExampleConfigChangeListener.java](src/main/java/com/example/StarBotExampleConfigChangeListener.java) | 监听 `StarBotConfigChangedEvent`，输出「消息后缀」变更前后的值及当前生效值 | 在配置页面的「示例插件」选项卡中开启「全局拦截推送消息」，修改「消息后缀」并保存，查看控制台日志 |

> **注意**：`StarBotExampleMeowAdder` 会影响所有经过 `StarBotMessageSender.send(..)` 发送的消息，包括符合条件的命令回复。可以在配置页面关闭「全局拦截推送消息」。若只需要参考示例, 请在构建前移除这些类。

## 插件配置

配置页面中的「示例插件」选项卡对应 `starbot.example` 前缀。默认值在 [StarBotExampleProperties.java](src/main/java/com/example/StarBotExampleProperties.java) 中维护，页面描述由 `src/main/resources/config/config.json` 提供。

| 配置键 | 默认值 | 作用 | 生效方式 |
| --- | --- | --- | --- |
| startup-log | true | 输出主程序启动完成的日志 | 重启 |
| danmu-log | true | 输出接收到的弹幕内容 | 实时 |
| intercept-messages | true | 统一修改推送消息，为符合条件的行追加后缀 | 实时 |
| message-suffix | 喵 | 追加到以中文结尾的行后，留空则不追加 | 实时 |
| hello-enabled | true | 注册「你好」命令及 hello 别名 | 重启 |
| greeting | 你好， | 问候回复中昵称前的文字 | 实时 |

事件监听器、消息切面和命令都通过注入配置类来读取当前值。`hello-enabled` 通过元数据中的 `springKey` 注入 Spring Environment，再配合 `@ConditionalOnProperty` 控制命令 Bean 是否注册，演示了需要重启的配置用法。

实时生效的配置修改保存后，getter 会立即读到新值。如果插件需要在这时刷新缓存或重新连接，可以监听 `StarBotConfigChangedEvent`，写法见[监听配置变更事件](#监听配置变更事件)。

## 开发说明

### 插件信息

每个 StarBot 插件都需要在 `pom.xml` 中定义元数据，这些内容会在插件加载时输出到控制台和日志：

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

> **注意**：请不要随意修改 `pom.xml` 的 `<build>` 构建配置部分，否则可能导致插件无法被 StarBot 加载。

模板中的构建配置会在打包时完成以下工作：

1. `starbot-plugin-processor` 根据 Maven 项目信息生成 `plugin.json`，并根据依赖关系生成 `dependency.json`，分别记录插件元数据、前置插件和普通运行时依赖
2. `maven-resources-plugin` 把这些文件复制到构建输出目录，最终打入插件 JAR 的根目录，供 StarBot 加载时读取
3. 插件的配置元数据放在 `src/main/resources/config/config.json`，随插件 JAR 打包

插件由 StarBot 主程序加载，不需要添加启动类，也不需要打包成 Spring Boot 可执行 JAR。

### 配置元数据

`src/main/resources/config/config.json` 描述配置中心的页面，包括分组、字段类型、默认值、校验规则、条件显示、是否需要重启生效，以及页面上的按钮。

字段、条件、校验规则和配置动作的完整写法见 [CONFIG-METADATA-GUIDE.md](CONFIG-METADATA-GUIDE.md)。

### 监听配置变更事件

配置保存后，或者 `data/config.json` 被外部修改后，StarBot 会重新读取配置。如果有实时生效的配置发生变化，StarBot 会发布 `StarBotConfigChangedEvent`。插件可以用 Spring 的 `@EventListener` 监听这个事件，在配置变化时刷新缓存或重新连接。完整示例见 [StarBotExampleConfigChangeListener.java](src/main/java/com/example/StarBotExampleConfigChangeListener.java)。

- 事件里的配置键是完整的点号路径，例如 `starbot.example.message-suffix`。`isChanged(...)` 也可以传父路径，例如 `starbot.example`，父项或任意子项变化都会返回 `true`。
- `getOldValue(...)` 和 `getNewValue(...)` 读取变化前后的值；还有带 `Class` 参数的重载，可以直接得到 `String`、`Integer` 等类型。需要查看全部变化时，可以遍历 `event.getKeys()`。
- 只有实时生效的配置会出现在事件里。声明了 `restartRequired: true`、`springKey` 或属于高级字段的配置，需要重启后才生效，重启前不会触发事件；没有实际变化时也不会发布事件。
- 事件在配置已经生效后发布，监听器里通过注入的配置类读到的就是新值。监听器在发布事件的线程中执行，应保持简短，耗时操作交给线程池。

### 开发命令

命令类需要实现 `StarBotCommand` 接口，并添加 `@StarBotComponent` 注解注册至 Spring 容器，StarBot 会自动发现并注册命令。可参考 [HelloCommand.java](src/main/java/com/example/HelloCommand.java)：

接口中的所有方法如下，其中 `getName()`、`getDescription()` 和 `execute(...)` 必须实现，其余方法已有默认实现，可以按需重写：

| 方法 | 作用 | 默认行为 | HelloCommand 中的行为 |
| --- | --- | --- | --- |
| `getId()` | 命令唯一标识，用于注册、查找和禁用命令，自定义时应避免与其他命令重复 | 返回实现类的全路径类名 | 重写为 `starbot.example.hello` |
| `getName()` | 命令名称，默认作为命令唤醒词 | 无默认实现，必须实现 | 返回 `你好` |
| `getAliases()` | 命令别名列表，默认作为额外的命令唤醒词 | 返回空列表，无别名 | 返回 `List.of("hello")` |
| `getDescription()` | 命令简介，在帮助信息中展示 | 无默认实现，必须实现 | 返回「向你问好，并带上你的昵称」 |
| `getUsage()` | 命令用法说明，可以描述参数格式，在帮助信息中展示 | 返回 `getName()` | 沿用默认实现，返回 `你好` |
| `getArgumentCount()` | 设置命令需要的文本参数个数，非负数表示必须恰好具有该数量的参数 | 返回 `-1`，不限制参数个数，由命令自行处理 | 返回 `0`，不接受文本参数 |
| `getPermission()` | 设置执行命令所需的默认权限，返回 `CommandPermission`，可由主程序配置覆盖 | 返回 `CommandPermission.EVERYONE`，所有人均可使用 | 沿用默认实现 |
| `getScope()` | 设置命令适用的会话类型，返回 `CommandScope` | 返回 `CommandScope.ALL`，群聊和私聊均可使用 | 沿用默认实现 |
| `isDisableable()` | 设置是否允许通过禁用命令禁用当前命令 | 返回 `true`，允许禁用；配置文件中的全局禁用设置不受此限制 | 沿用默认实现 |
| `isTrigger(CommandContext context)` | 根据命令上下文判断当前消息是否触发命令，返回 `true` 表示匹配 | 不区分大小写匹配命令名称和别名 | 沿用默认实现，匹配 `你好` 或 `hello` |
| `execute(CommandContext context)` | 命令触发并通过策略检查后的执行逻辑，可以读取昵称、参数等信息，并使用 `context.reply(...)` 回复 | 无默认实现，必须实现 | 回复「你好，[昵称]」 |

`CommandPermission` 的可选值为 `EVERYONE`（所有人）、`OWNER`（仅主人）和 `ADMIN`（主人、当前群群主或管理员，私聊中仅主人可用），主人账号由主程序的 `starbot.core.command.owners` 配置。`CommandScope` 的可选值为 `ALL`（群聊和私聊）、`GROUP`（仅群聊）和 `PRIVATE`（仅私聊）。

主程序可以通过 `starbot.core.command.permissions` 按命令 ID 覆盖 `getPermission()` 返回的默认权限，对所有平台和会话统一生效，未配置的命令沿用默认权限。例如把 `starbot.example.hello` 配置为 `OWNER`，即可把示例命令限制为仅主人使用。帮助菜单会同步展示覆盖后的可用状态，命令的会话范围和禁用规则仍然生效。

`isTrigger(...)` 应当快速且无副作用，只负责判断触发条件，实际业务逻辑放在 `execute(...)` 中。权限、会话范围、禁用检查和参数个数由 StarBot 统一处理，不需要在触发规则中重复判断。

命令触发还受 StarBot 主程序的 `starbot.core.command` 配置控制：`prefix` 默认为空字符串，表示无需前缀；`mention` 默认为 `false`，表示群聊中无需提及机器人。如果配置了前缀或要求提及机器人，测试「你好」命令时也需要满足这些条件。

### 注意事项

- 开发插件时可以使用绝大多数的 Spring 注解，例如用 `@Controller` 创建 API 接口，用 `@EventListener` 监听事件
- 需要注册至 Spring 容器或使用 Spring 机制（例如事件机制）的类，要在类上使用 `@StarBotComponent` 注解，StarBot 会扫描并把这类组件注册到 Spring 容器中
- 使用了 `@StarBotComponent` 注解的类，类名不能与 StarBot 本体或其他插件中的类名重复，命名时应尽量避免过于简单或通用的名称
- StarBot 内部大量使用 Spring 的事件机制，插件可以通过创建事件监听器来处理这些事件，常用事件类型请参考 [StarBotCore](https://github.com/Starlwr/StarBotCore) 项目的相关文档

## 依赖管理

StarBot 插件使用 Maven 管理依赖。构建时由 Maven 解析依赖，打包时由 `starbot-plugin-processor` 生成依赖清单，StarBot 启动时再根据清单处理缺失的普通运行时依赖。

| 依赖作用域 | 是否记录到运行时依赖清单 | 用途 |
| --- | --- | --- |
| `compile`（默认） | 是 | 编译和运行插件都需要的依赖 |
| `runtime` | 是 | 运行插件时需要的依赖 |
| `provided` | 否 | 编译时需要，运行时由环境提供或无需保留的依赖，例如 Lombok |
| `test` | 否 | 仅用于测试的依赖 |

普通运行时依赖如果已经存在于 StarBot 的 `lib` 或 `plugins-lib` 目录，会直接复用；缺失时可以自动下载至 `plugins-lib`。自动下载由主程序的 `starbot.core.plugin.auto-download-dependency` 控制，默认开启，也可以使用 `--skip-download-dependency` 启动参数临时跳过。

### 核心依赖

每个 StarBot 插件都必须直接依赖 StarBotCore，或通过依赖其他插件间接依赖 StarBotCore，建议开发时使用最新版本：

```xml
<dependency>
    <groupId>com.starlwr</groupId>
    <artifactId>starbot-core</artifactId>
    <version>3.0.0</version>
</dependency>
```

### 依赖其他插件

需要使用其他插件提供的功能时，可以在 `pom.xml` 的 `<dependencies>` 中声明对应的插件依赖。

StarBot 官方插件都发布在 StarBot 官方 Maven 仓库中。保留模板中的仓库配置并添加依赖后，Maven 会在构建时自动获取对应插件，无需手动安装到本地 Maven 仓库。例如依赖 Bilibili 插件：

```xml
<dependency>
    <groupId>com.starlwr</groupId>
    <artifactId>starbot-bilibili</artifactId>
    <version>1.0.0</version>
</dependency>
```

第三方前置插件请按照作者提供的方式引入：如果已经发布到可访问的 Maven 仓库，添加对应的仓库和依赖配置即可；如果只提供 JAR 文件，需要先按作者提供的 Maven 坐标安装到本地 Maven 仓库，再添加依赖配置。

构建时由 Maven 获取前置插件，部署时则需要确保这些插件已经安装到 StarBot 的 `plugins` 目录中。

### 添加第三方依赖

插件可以按需添加其他第三方依赖。例如添加一个编译和运行时都需要的缓存库：

```xml
<dependency>
    <groupId>com.github.ben-manes.caffeine</groupId>
    <artifactId>caffeine</artifactId>
    <version>3.2.0</version>
</dependency>
```

上面的依赖没有指定 `scope`，默认为 `compile`，会被记录到普通运行时依赖清单，供 StarBot 在缺失时自动下载。

Lombok 只在编译时使用，可以保留模板中的以下配置，它不会加入运行时依赖清单：

```xml
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <scope>provided</scope>
    <optional>true</optional>
</dependency>
```

构建时使用插件项目 `pom.xml` 中的 Maven 仓库配置，运行时下载普通依赖则使用 StarBot 主程序的 `starbot.core.plugin.maven-base-urls` 配置。如果某个依赖只发布在其他 Maven 仓库中，请同时确认构建环境和 StarBot 运行环境都能访问对应的仓库。

## 构建与部署

### 构建插件

在插件项目根目录（`pom.xml` 所在目录）使用 Maven 构建 StarBot 插件：

```bash
mvn clean package
```

构建成功后，插件 JAR 会生成在 `target` 目录中，文件名格式为 `{artifactId}-{version}.jar`，例如 `starbot-example-plugin-1.0.0.jar`。

### 部署插件

StarBot 启动时会扫描启动工作目录下的 `plugins` 文件夹。启动工作目录是运行 StarBot 时使用的当前目录，使用 IDE 或启动脚本运行时请检查工作目录设置。

1. 如果 StarBot 正在运行，先停止主程序
2. 在 StarBot 的启动工作目录下找到或创建 `plugins` 文件夹
3. 把构建好的插件 JAR 复制到该目录；更新已有插件时请移走旧版 JAR，避免同时保留同一插件的多个版本
4. 启动 StarBot，等待依赖处理和插件加载完成
5. 检查「已注册插件 StarBotExamplePlugin v1.0.0」和「已加载插件 StarBotExamplePlugin v1.0.0」日志，修改过插件名称或版本时以实际信息为准；命令示例还会输出「已注册 你好 命令: starbot.example.hello」日志，之后可以按[示例功能](#示例功能)中的方式验证

如果当前插件依赖其他插件，请把所需前置插件的 JAR 一并放入 `plugins` 目录。前置插件缺失时，StarBot 会记录「未安装插件 ... 的前置依赖插件 ..., 无法加载该插件」日志，并跳过加载依赖它的插件。

首次启动时，如果缺少普通运行时依赖并且已经开启自动下载，StarBot 会先把依赖下载到启动工作目录下的 `plugins-lib` 文件夹，然后尝试自动重启。请确保运行环境可以访问配置的 Maven 仓库，并且可以执行 `java` 命令；自动重启失败时，可以在依赖下载完成后手动启动 StarBot。

如果插件没有成功加载，可以优先检查以下内容：

- JAR 是否放在正确工作目录下的 `plugins` 文件夹中
- JAR 根目录是否包含 `plugin.json` 和 `dependency.json`
- 插件组件类是否添加了 `@StarBotComponent` 注解
- 日志中是否出现依赖下载失败、前置插件缺失或组件加载异常
