<div align="center">

![logo](https://bot.starlwr.com/images/static/logo.jpg)

**<h2>StarBot 示例插件</h2>**
</div>

## 目录

- [快速开始](#快速开始)
- [项目结构](#项目结构)
- [开发说明](#开发说明)
- [依赖管理](#依赖管理)
- [构建与部署](#构建与部署)

## 快速开始

1. 克隆本示例项目作为模板创建新项目
2. 修改 `pom.xml` 中的项目信息, 该信息将于打包时被使用 (groupId, artifactId, version 等)
3. 修改 `src/main/resources/plugin.json` 中的插件元数据，该信息将在被 StarBot 加载时输出到控制台和日志中
4. 开发你的插件功能, 开发时可正常使用绝大多数 Spring 注解 (可参考 `StarBotExampleStartEventListener.java` 和 `StarBotExampleDanmuEventListener.java` 示例)
5. 使用 Maven 构建项目: `mvn clean package`
6. 将 `target` 中生成的 JAR 文件放入 StarBot 的 `plugins` 目录

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
│   │   │       └── StarBotExampleDanmuEventListener.java  # 示例功能: 弹幕监听器
│   │   └── resources/
│   │       └── plugin.json                                # 插件元数据配置文件
│   └── test/                                              # 测试代码目录
├── convert.ps1                                            # 依赖转换脚本, 请不要修改
└── target/                                                # 构建输出目录
    └── starbot-example-plugin-1.0.0.jar                   # 构建后的插件 JAR 文件
```

## 开发说明

### 插件信息

每个 StarBot 插件都需要在 `src/main/resources/plugin.json` 文件中定义其元数据, 其内容将在被 StarBot 加载时输出到控制台和日志中：

```json
{
  "name": "示例插件",
  "author": "作者",
  "version": "1.0.0",
  "description": "示例插件描述"
}
```

### 打包配置

> **注意**: 请不要随意修改 `pom.xml` 的 `<build>` 构建配置部分和 `convert.ps1` 文件，否则可能导致插件无法被 StarBot 加载

### 注意事项

>- 开发插件时, 可以使用绝大多数的 Spring 注解, 例如使用 `@Controller` 创建 API 接口, 使用 `@EventListener` 监听事件等
>- 需要注册至 Spring 容器或使用 Spring 机制 (例如事件机制) 的类, 需要在类上使用 `@StarBotComponent` 注解, 该注解会将类注册为 StarBot 组件, 并被 StarBot 扫描并注册至 Spring 容器中  
>- 使用了 `@StarBotComponent` 注解的类, 类名不可以与 StarBot 本体或其他插件中的类名重复, 请命名时尽量避免过于简单或过于通用的命名
>- StarBot 内部大量使用了 Spring 的事件机制, 插件可以通过创建事件监听器来处理这些事件, 常用事件类型请参考 [StarBotCore](https://github.com/Starlwr/StarBotCore) 项目相关文档

## 依赖管理

StarBot 插件使用 Maven 进行依赖管理, 插件可以依赖其他第三方库, 这些库将在被 StarBot 加载时自动下载

### 核心依赖

每个 StarBot 插件必须依赖 StarBotCore：

```xml
<dependency>
    <groupId>com.starlwr</groupId>
    <artifactId>starbot-core</artifactId>
    <version>3.0.0</version>
</dependency>
```

### 添加第三方依赖

插件可以按需添加其他第三方依赖，例如：

```xml
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <scope>provided</scope>
    <optional>true</optional>
</dependency>
```

## 构建与部署

### 构建插件

使用 Maven 构建 StarBot 插件：

```bash
mvn clean package
```

构建成功后，插件 JAR 文件将生成在 `target` 目录中，文件名格式为 `{artifactId}-{version}.jar`，例如 `starbot-example-plugin-1.0.0.jar`。

### 部署插件

将生成的 JAR 文件复制到 StarBot 的插件目录中：

1. 找到 StarBot 的插件目录（`plugins` 文件夹）
2. 将插件 JAR 文件复制到该目录
3. 启动 StarBot
