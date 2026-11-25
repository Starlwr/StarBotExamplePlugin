# 插件配置元数据编写指南

本指南说明如何为 StarBot 插件编写配置元数据，也就是插件项目里的 `src/main/resources/config/config.json`。写好后，配置中心就能显示插件的设置页面，校验用户填写的内容，并把值保存到运行目录的 `data/config.json`。

元数据随插件一起发布，打包后位于插件 JAR 的 `config/config.json`。同一个 JAR 的根目录还需要有 `plugin.json`，配置中心用其中的 `groupId:artifactId` 区分插件。没有提供元数据的插件不会显示配置页。

读这份文档时，可以先看第 1 节的最小示例，了解一份元数据长什么样，再按需查阅后面的表格。表格会先说明每项内容的作用，再列出取值和要求。「必填」列写「否，建议填写」时，表示这一项可以留空，但建议填写，具体作用见对应说明。

## 目录

- [1. 最小示例](#1-最小示例)
- [2. 模块和配置分组](#2-模块和配置分组)
- [3. 字段](#3-字段)
- [4. 默认值、配置读取和生效方式](#4-默认值配置读取和生效方式)
- [5. 选项和界面](#5-选项和界面)
- [6. 条件](#6-条件)
- [7. 校验规则](#7-校验规则)
- [8. 配置动作](#8-配置动作)
- [9. 编写检查](#9-编写检查)

## 1. 最小示例

下面以「定时提醒」插件为例，给出一份可以直接运行的最小元数据。这个插件会按固定间隔检查是否有到期的提醒，并把提醒发送到配置好的通知渠道。

```json
{
  "title": "定时提醒",
  "description": "设置提醒任务的检查频率\n启用后会定期检查需要发送的提醒",
  "configurationClass": "com.example.reminder.ReminderProperties",
  "basePath": ["starbot", "reminder"],
  "groups": [
    {
      "id": "schedule",
      "title": "提醒任务",
      "description": "选择是否运行提醒任务，以及多久检查一次",
      "fields": [
        {
          "key": "enabled",
          "label": "启用提醒",
          "type": "boolean"
        },
        {
          "key": "check-interval",
          "label": "检查间隔",
          "description": "两次检查之间的等待时间",
          "type": "integer",
          "restartRequired": true,
          "minimum": 1,
          "maximum": 3600,
          "ui": {"unit": "秒"}
        }
      ]
    }
  ]
}
```

这段元数据会在配置中心生成一个「定时提醒」模块，里面有「提醒任务」分组，分组里有一个开关和一个数字输入框。

对应的 Java 配置类：

```java
package com.example.reminder;

import com.starlwr.bot.core.config.StarBotConfiguration;
import com.starlwr.bot.core.plugin.StarBotComponent;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@StarBotComponent
@StarBotConfiguration(prefix = "starbot.reminder")
public class ReminderProperties {
    private boolean enabled = false;

    private int checkInterval = 60;
}
```

`basePath` 拼成 `starbot.reminder`，与配置类的前缀一致。分组没有声明 `path`，所以 `check-interval` 的完整路径是 `["starbot", "reminder", "check-interval"]`。用户把检查间隔改成 120 秒后，配置文件中的对应片段为：

```json
{
  "starbot": {
    "reminder": {
      "check-interval": 120
    }
  }
}
```

页面上的默认值 60 秒来自 Java 字段初值。配置类需要能通过无参构造方法创建，构造时只初始化数据。为了让 StarBot 的 Getter 代理读到当前生效值，配置类和 Getter 都不能声明为 `final`，Getter 应直接返回同名字段：`getCheckInterval()` 对应 `checkInterval` 和配置键 `check-interval`。手动 `new` 出来的对象只读取自身初值，或之后赋给它的值。

`check-interval` 声明了 `restartRequired: true`，所以修改后要重启 StarBot 才会生效。

## 2. 模块和配置分组

一个插件的配置元数据对应配置中心里的一个模块。模块下面分成若干分组，分组里放字段和按钮。元数据只描述页面；用户保存后的值会写入启动目录的 `data/config.json`，按 `basePath`、分组的 `path` 和字段的 `key` 拼出的路径存放。

模块、分组、配置项和按钮的 `description` 都按纯文本显示。需要换行时写 `\n`，需要空一行时写 `\n\n`；页面仍会按宽度自动折行。

### 模块根对象

| 字段 | 必填 | 说明 |
| --- | --- | --- |
| `title` | 是 | 用来填写模块名称，显示为配置中心的模块选项卡标题。 |
| `description` | 否 | 用来填写模块说明，显示在模块选项卡下方，可以留空。 |
| `configurationClass` | 否，建议填写 | 用来指定业务字段默认值的来源类，填写完整类名（含包名），例如 `com.example.reminder.ReminderProperties`。不填时，页面上的业务字段没有来自配置类的默认值。写法和失败情况见[关于 configurationClass](#关于-configurationclass)。 |
| `basePath` | 是 | 用来指定这个模块的业务配置保存在 `starbot` 下的位置，填字符串数组，例如 `["starbot", "reminder"]`。模块里的业务字段会按这个前缀写入 `data/config.json`。每段是一个键名，不要在一段里写点号；不要和其他模块使用相同前缀。只声明高级字段，或只给其他模块追加选项时，可以填 `[]`。 |
| `conditions` | 否 | 用来定义可以重复使用的命名条件。这里的每个键是条件名称，值是一个完整的条件对象；字段的 `visibleWhen`、`requiredWhen`、`validateWhen`，按钮的 `visibleWhen` 和校验规则的 `when` 都可以用名称引用，避免重复写同样的条件。见[第 6 节](#6-条件)。 |
| `groups` | 是 | 用来列出模块里的配置分组，页面按数组顺序显示。只给其他模块追加选项时可以填 `[]`。 |
| `optionContributions` | 否 | 用来给其他模块的下拉选项补充选项，例如把「企业微信」加到另一个插件的通知方式里。写法见[扩展其他模块的选项](#扩展其他模块的选项)。 |

#### 关于 `configurationClass`

这个字段回答一个问题：页面上显示的默认值是从哪来的。StarBot 会这样使用它：

1. 按 `basePath` 拼出这个模块的配置前缀，例如 `starbot.reminder`；
2. 找到 `configurationClass` 指向的类，创建它的一个实例；
3. 读取实例里每个字段的初值，作为页面上对应字段的默认值。

正常启动时，插件自己的配置 Bean 也会提供默认值；但以独立配置中心模式启动时不会加载插件，只有这里的类能提供默认值，所以通常都要填写。

因此，这个类需要满足以下条件：

- 用 `@StarBotConfiguration(prefix = "...")` 标注，并且 `prefix` 必须和 `basePath` 拼出的前缀完全相同，例如 `starbot.reminder`；
- 同时用 `@StarBotComponent` 标注，让 StarBot 在加载插件时把这个类注册到 Spring 容器；
- 有公开的无参构造方法，创建实例时只初始化数据，不做耗时或有副作用的操作；
- 字段名和配置键一一对应：Java 字段用驼峰命名，配置键用短横线命名，例如 `checkInterval` 对应 `check-interval`；
- 类本身和 getter 不能声明为 `final`，因为运行时 StarBot 会为它创建代理。

如果类找不到、没有加 `@StarBotConfiguration`、`prefix` 和 `basePath` 对不上，或者没有可用的无参构造方法，配置中心会提示「配置 DTO 暂时无法加载」，页面仍可编辑，但这些字段没有来自这个类的默认值。

同一个 `basePath` 下的多个模块如果都声明了 `configurationClass`，它们必须声明同一个类。声明了不同的类时，配置中心会把这种情况当成声明冲突，这个前缀的默认值同样不会加载。

### 分组对象

| 字段 | 必填 | 说明 |
| --- | --- | --- |
| `id` | 是 | 用来在模块内标识这个分组，页面和按钮会用它定位分组。只用于元数据，不会写入配置文件，所以修改分组名称或位置不会改变已有的配置键。 |
| `title` | 是 | 用来填写分组名称，显示在侧栏和内容区顶部。 |
| `description` | 否 | 用来填写分组说明，显示在内容区标题下方，可以留空。 |
| `path` | 否 | 用来指定分组里的业务字段保存在 `basePath` 下的哪个子路径，默认 `[]`。例如 `basePath` 是 `["starbot", "reminder"]`、`path` 是 `["delivery"]`，字段 `channel` 就保存在 `starbot.reminder.delivery.channel`。`target` 为 `advanced` 时这个路径不生效。 |
| `target` | 否 | 用来决定分组里的字段默认保存在业务配置（`starbot`）还是 Spring 高级配置（`advanced`）。分组里的直属字段可以单独覆盖。 |
| `restartRequired` | 否 | 用来统一要求分组里的业务字段修改后必须重启才生效。默认 `false`，表示保存后立即生效；单个字段可以覆盖。 |
| `ui` | 否 | 用来选择分组的排版方式。填 `{"layout": "cards"}` 时，分组里直属的 `object` 字段会分别显示成卡片，普通字段按顺序分段显示；分组里没有 `object` 字段时仍使用普通表单。 |
| `actions` | 否 | 用来给分组添加按钮，按钮显示在字段下方，见[第 8 节](#8-配置动作)。 |
| `fields` | 否 | 用来列出分组里的字段，见[第 3 节](#3-字段)。 |

分组的 `id` 不会写入配置路径。因此给「提醒任务」改名，或把它移到侧栏的其他位置，都不会改变已有的配置键。只有动作的分组仍会显示；既没有字段也没有动作的分组会隐藏。

一个分组里有多套并列的固定设置时，例如「工作日提醒」和「周末提醒」，可以用卡片分开。卡片内部沿用原来的字段和校验；普通字段会按原有顺序分段显示在卡片之间。分组中没有 `object` 字段时，卡片布局不生效。复杂的 Map 条目和列表元素不会因为卡片布局而改变。

字段按 `target` 分为两类：`starbot` 下的业务字段由插件配置类读取，`advanced` 下的高级字段由 Spring 直接读取。后文的「业务字段」和「高级字段」都指这两类。

### 路径如何计算

| `target` 取值 | 保存路径 | 示例 |
| --- | --- | --- |
| `starbot` | `basePath + group.path + field.key` | `basePath: ["starbot", "reminder"]`，`path: ["delivery"]`，`key: "channel"`，得到 `["starbot", "reminder", "delivery", "channel"]`。 |
| `advanced` | 一般为 `["advanced", field.key]` | `key: "reminder.push.timeout"`，得到 `["advanced", "reminder.push.timeout"]`。 |

路径数组中的每一段都是一个完整的键名。上例 `reminder.push.timeout` 的点号保留在同一段中。高级 Map 的单个条目写法不同，会拆成 `["advanced", Map 名, 条目名]`，见[高级字段的写法](#高级字段交给-spring-的配置)。

## 3. 字段

字段是页面上用户能填写的一项配置。每个字段对象描述：键名是什么、显示什么名称、用什么控件、怎么校验。字段可以嵌套：对象的子字段写在 `fields` 里，列表的元素写在 `items` 里，Map 的键和值分别写在 `keyMeta` 和 `values` 里。

「直属字段」指直接写在分组 `fields` 里的字段；写在 `fields`、`items`、`values` 或 `keyMeta` 内部的字段属于嵌套字段。

### 字段对象

| 字段 | 必填 | 说明 |
| --- | --- | --- |
| `key` | 是 | 用来指定配置键名，会拼进配置路径。同一个对象内必须非空且唯一。`items`、`values`、`keyMeta` 描述的是模板，不需要填写 `key`；高级字段要填完整的 Spring 属性名。 |
| `label` | 否 | 用来填写页面上显示的名称。列表元素和 Map 条目也可以设置，作为输入框标签或校验提示。 |
| `description` | 否 | 用来填写字段说明，显示在输入框旁，帮助用户理解怎么填。可以留空。 |
| `type` | 是 | 用来指定字段保存的数据类型，决定页面控件和校验方式。填一个类型名或类型数组，见下面的[类型](#类型)。 |
| `target` | 否 | 用来指定字段保存到业务配置（`starbot`）还是高级配置（`advanced`）。只能写在直属字段上，不填时跟随分组；嵌套字段不能单独改。 |
| `springKey` | 否 | 用来把业务字段额外映射成一个 Spring 属性，填写完整的属性名，例如 `starbot.example.hello-enabled`，之后可以用 `@ConditionalOnProperty`、`@Value` 等 Spring 机制读取。适合固定位置的业务字段；`items`、`values`、`keyMeta` 里的模板不能单独映射。映射后的字段修改后需要重启才能生效；高级字段本身就是 Spring 属性，不需要填写。 |
| `restartRequired` | 否 | 用来说明修改这个字段后是否要重启才生效。不填时跟随分组设置；父字段要求重启时，子字段也会重启。高级字段和带 `springKey` 的字段始终需要重启，填 `false` 不会改变这一点。 |
| `keyMeta` | 否 | 用来描述 Map 的键。仅在 `type` 含 `map` 时使用，可以写键的标签、校验规则等。Map 的键在 JSON 里总是字符串，所以不写 `type` 时按 `"string"` 处理。 |
| `fields` | 是（含 `object` 时） | 用来列出固定对象的子字段，每个子字段都要有 `key` 和 `type`。可以为空数组。 |
| `items` | 是（含 `array` 时） | 用来描述列表里的每一个元素：写一个字段对象，不需要 `key`。 |
| `values` | 是（含 `map` 时） | 用来描述 Map 里的每一个值：写一个字段对象。值用 `object` 时，每个条目会显示一张固定表单。 |
| `required` | 否 | 用来要求用户必须填写。为 `true` 时，缺失、`null` 和空字符串都不通过。 |
| `nonBlank` | 否 | 用来要求文本不能全是空格，至少包含一个非空白字符。 |
| `format` | 否 | 用来按预设格式检查值，例如邮箱、URL、颜色，见[格式](#格式)表。 |
| `minimum` | 否 | 用来限制数值下限，包含这个数。 |
| `maximum` | 否 | 用来限制数值上限，包含这个数。 |
| `options` | 否 | 用来给用户提供固定的下拉选项，用户只能从这些值里选。 |
| `optionsFrom` | 否 | 用来从其他配置里读取候选值，生成会变化的下拉选项，例如从已填写的方案列表里选默认方案。 |
| `requiredWhen` | 否 | 用来只在满足条件时要求必填。条件明确为 `true` 时生效；字段本身已声明 `required: true` 时始终必填。 |
| `visibleWhen` | 否 | 用来控制字段什么时候显示。条件明确为 `false` 时隐藏，已填写的值会保留；隐藏不会停止校验。 |
| `validateWhen` | 否 | 用来控制什么时候跳过校验。条件明确不成立时，当前字段和它的子字段都不校验。 |
| `nullable` | 否 | 用来允许用户把值显式设为 `null`。字段有值时，标题右侧会出现「清除设置」；必填字段仍然不能为 `null`。 |
| `emptyAsNull` | 否 | 用来把空字符串保存成 `null`，适合可以留空的文本。只对同时声明了 `nullable: true` 的文本字段有效，不能和 `ui.emptyAsDefault` 一起使用。 |
| `validators` | 否 | 用来添加更复杂的校验，例如和另一个字段比较、要求列表至少几项，见[第 7 节](#7-校验规则)。 |
| `ui` | 否 | 用来调整字段的界面，例如单位、密码框、列表项标题，见[界面设置](#界面设置)。 |
| `suggestedValue` | 是（`target` 为 `advanced` 的直属字段） | 用来给高级字段一个建议值。填与类型匹配的 JSON 值；填 `null` 表示不设置这个 Spring 属性，沿用 Spring 或使用方的默认行为。 |
| `suggestedReason` | 否，建议填写 | 用来解释为什么建议这个值。多个插件给同一个属性不同建议时，配置中心会把理由显示给用户；留空时显示「插件未说明原因」。通常和 `suggestedValue` 一起填写。 |

### 嵌套字段

| 父字段类型 | 需要怎么填 | 子项说明 |
| --- | --- | --- |
| `object` | `fields` | 每个子字段都要有 `key` 和 `type`，同一对象内键名不能重复。业务子字段的默认值来自配置类中对应字段的初值。 |
| `array` | `items` | `items` 是一个字段对象，描述一个元素，不需要 `key`。元素是文本或数字时，`label` 用作输入框占位或下拉提示；元素是对象时，可以用父字段的 `ui.itemTitlePath` 指定用哪个字段作为标题。 |
| `map` | `values`（`keyMeta` 可选） | `values` 是一个字段对象，描述每个值；`keyMeta` 描述键。键由用户输入，`keyMeta.label` 是键输入框的标签，省略时显示「条目名称」。 |

如果 Java 泛型声明了具体的 Map 值类型或列表元素配置类，配置中心就用该类的初值作为新条目的模板。没有业务默认值时，按声明的类型生成起始内容：文本是 `""`，整数和小数是 `0`，开关是 `false`，固定对象和 Map 是 `{}`，列表是 `[]`，`null` 类型是 `null`。类型数组取第一项。静态选项不会自动选中第一项，用户仍需自己选择合适的值。起始内容只是表单草稿，仍然需要通过校验。

嵌套字段不能声明自己的 `target`。高级对象、Map 或列表的建议值写在最外层的 `suggestedValue` 中，子项只描述类型和规则。父字段要求重启时，嵌套字段不能再用 `restartRequired: false` 取消。

### 类型

| `type` | 用来保存什么 | 页面效果 | 常用设置 |
| --- | --- | --- | --- |
| `"string"` | 文本，如 `"daily"`。 | 单行或多行文本框。 | `nonBlank`、`format`、`options` |
| `"integer"` | 整数，如 `60`。 | 数字输入框。 | `minimum`、`maximum` |
| `"number"` | 整数或小数，如 `0.5`。 | 数字输入框。 | `minimum`、`maximum` |
| `"duration"` | 时长，如 `"30s"`，或整数毫秒，如 `30000`。 | 数字输入框和单位下拉框。 | `positiveDuration`、`durationRange` |
| `"boolean"` | `true` 或 `false`。 | 开关。 | — |
| `"object"` | 键名预先确定的对象。 | 一组固定表单。 | `fields` |
| `"map"` | 键名由用户指定的对象。 | 可增删条目的表单。 | `values`、`keyMeta` |
| `"array"` | 按顺序保存的列表。 | 可增删的列表。 | `items` |
| `"any"` | 任意 JSON 值，结构由使用方决定。 | 通用树形编辑器。 | — |
| `"null"` | 只能填写 `null`。 | 不需要输入。 | — |

`type` 也可以写成 `["string", "integer"]` 这样的数组。只要包含对象、Map 或列表，仍然要声明对应的子结构。需要配置中心渲染完整的嵌套表单时，优先给对象、Map 或列表声明单一类型。需要明确改变 JSON 类型时可以直接编辑原文；允许清空字段时使用 `nullable: true`。

时长控件支持天、小时、分钟、秒和毫秒。配置类可使用 `java.time.Duration` 读取。按固定单位存储的业务字段可直接使用 `type: "integer"` 和 `ui.unit`。

在 Map 中，点击「添加条目」后先填写名称，确认后才创建条目。空白名称、重复名称，以及已经有独立控件的键都不能添加。所有 Map 条目的 `keyMeta.label` 都显示在名称输入框左侧，省略或留空时隐藏该标签。列表表单只显示条目数量，保存时仍按原顺序保留。

## 4. 默认值、配置读取和生效方式

这一节说明配置值从哪里来、插件怎么读到、修改后什么时候生效。了解这些，可以避免出现「页面上改了，插件却没反应」的情况。

### 默认值从哪里来

页面上显示的默认值有两个来源：

- 业务字段：来自 [`configurationClass`](#关于-configurationclass) 指定类的字段初值；
- 高级字段：来自元数据里的 `suggestedValue`。

打开页面时，配置中心会把这些默认值填进表单，但不会写入 `data/config.json`。只有用户修改并保存后，值才会写入文件；保存时会省略与默认值相同的项，让默认值继续生效。

### 插件怎么读取配置

业务字段通过 `@StarBotConfiguration` 配置类的 getter 读取。StarBot 会给这个类创建代理，每次调用 getter 都会按 `prefix + 属性名` 去当前配置里取值，取不到时返回字段初值。

```java
@StarBotComponent
@StarBotConfiguration(prefix = "starbot.example")
@Getter
@Setter
public class StarBotExampleProperties {
    private String messageSuffix = "喵";
}
```

使用时注入这个配置类，每次需要时再调用 getter：

```java
// 每次发送时重新读取，配置修改后立即使用新值
String suffix = properties.getMessageSuffix();
```

因此需要注意：

- 不要自己 `new` 配置类，手动创建的对象没有代理，只能读到自身初值；
- 不要在构造方法或字段初始化时把 getter 的结果存到长期字段里，否则配置修改后仍会用旧值；
- 类和 getter 不能声明为 `final`，否则 StarBot 无法创建代理；
- 字段名按驼峰命名，配置键按短横线命名，两者要一一对应。

### 修改后什么时候生效

| 配置类型 | 保存后什么时候生效 | 说明 |
| --- | --- | --- |
| 实时生效的业务字段（`restartRequired` 为 `false`） | 立即生效 | 保存后配置文件重载，getter 下次调用就返回新值，同时发布配置变更事件。 |
| 需要重启的业务字段（`restartRequired` 为 `true`） | 重启后生效 | 运行中的 getter 仍返回启动时的值；修改不会触发配置变更事件。 |
| 带 `springKey` 的业务字段 | 重启后生效 | 启动时映射为 Spring 属性，供 `@ConditionalOnProperty` 等条件注解使用。 |
| 高级字段 | 重启后生效 | 启动时展开为 Spring 属性，交给 Spring 或依赖库使用。 |

配置中心保存时，会先写入 `data/config.json`，再重新读取文件，只更新允许实时生效的字段。声明重启的字段仍保留启动时的值，页面会显示等待重启的标记。直接编辑配置文件也会触发同样的重载流程。

如果插件需要在配置变化时执行操作，例如刷新缓存、重新连接，可以监听 `StarBotConfigChangedEvent`。写法见 [README.md](README.md#监听配置变更事件)。

### 高级字段：交给 Spring 的配置

高级字段保存到 `advanced` 下，不是给插件自己的 getter 读的，而是给 Spring 和依赖库用的。StarBot 启动时会把这些字段展开成 Spring 属性，例如 `reminder.push.timeout`。插件如果想读取，可以注入 `Environment` 或使用 `@Value`，这类配置修改后需要重启。

下面的例子使用提醒插件自己的 `reminder.push.*` 属性。仅仅声明属性不会自动产生功能，插件仍需要在业务代码中读取或绑定这些属性。

可以用 `array → map → object` 声明一组按顺序读取的命名推送目标：

```json
{
  "key": "reminder.push.targets",
  "label": "推送目标",
  "type": "array",
  "target": "advanced",
  "items": {
    "type": "map",
    "keyMeta": {"label": "目标名称", "nonBlank": true},
    "values": {
      "type": "object",
      "fields": [
        {"key": "url", "label": "推送地址", "type": "string", "format": "http-url", "required": true}
      ]
    }
  },
  "suggestedValue": [],
  "suggestedReason": "由使用者填写实际的推送地址"
}
```

| 用户填写的值 | 注入 Spring 的属性示例 |
| --- | --- |
| `"reminder.push.targets": [{"wecom": {"url": "https://notify.example.com/hook"}}]` | `reminder.push.targets[0][wecom].url=https://notify.example.com/hook` |
| 另一字段声明为文本列表，保存 `"reminder.push.time-windows": ["morning"]` | `reminder.push.time-windows[0]=morning` |

固定对象用点号连接子字段，列表用方括号下标，Map 用方括号保留键名中的点号。嵌套写法和完整键写法指向同一个属性时，保存和启动检查都会指出这两处位置。

如果某个高级 Map 键需要单独的控件，可以在同一个分组里同时声明这个 Map 和一个带方括号的字段：

```json
[
  {
    "key": "reminder.push.flags",
    "label": "额外开关",
    "type": "map",
    "target": "advanced",
    "keyMeta": {"label": "开关名"},
    "values": {"type": "boolean"},
    "suggestedValue": null
  },
  {
    "key": "reminder.push.flags[retry.on-timeout]",
    "label": "超时重试",
    "type": "boolean",
    "target": "advanced",
    "suggestedValue": true,
    "suggestedReason": "推送超时后自动重试一次"
  }
]
```

这个开关仍然保存在 Map 中，路径是 `["advanced", "reminder.push.flags", "retry.on-timeout"]`。Map 编辑区只显示其余键，恢复 Map 当前生效值时也只恢复其余键。这种写法要求根属性已经声明为 Map，并且字段名中只有一段方括号。

### 多个插件共用同一个属性

多个模块可以分别给同一个高级属性提供入口，共用同一个配置值。当建议值不同，或者类型、子结构无法合并时，配置中心会给出建议方案和人工填写入口；类型不一致的选择可能无法兼容所有插件。显式填写的 `false`、`0`、空文本、空集合或 `null` 都可以用来解决冲突。冲突未解决时会阻止正常启动；配置中心已启用时，程序会切换到独立模式，便于先处理冲突。

元数据错误和建议冲突不是一回事。比如两个业务字段映射到同一个 `springKey`，或多个来源的限制没有共同取值，都属于声明问题。配置中心会指出涉及的来源，由插件作者修正；重复的业务映射不会注入 Spring。

高级字段和映射到 Spring 的业务字段支持完整占位符，例如 `${REMINDER_HOST:localhost}`。配置中心保留原文，由 Spring 在启动时解析；普通业务字段以及不完整的占位符按字段原有规则校验。

## 5. 选项和界面

### 静态选项

`options` 用来给用户提供固定的候选值，页面上显示为下拉框。每一项包含 `label`（显示文本，可选）和 `value`（实际保存的 JSON 值，必填）。已设置且非 `null` 的值必须与其中某一项匹配，数字 `1` 和字符串 `"1"` 不相等。数组顺序就是下拉框的展示顺序，「请选择」只是占位提示。

例如给提醒任务增加一个提醒模式，让用户从两个固定选项中选择：

```json
{
  "key": "mode",
  "label": "提醒模式",
  "type": "string",
  "options": [
    {"label": "固定间隔", "value": "interval"},
    {"label": "每日定时", "value": "daily"}
  ]
}
```

### 动态选项

`optionsFrom` 用来从当前配置里读取候选值，适合选项会随着其他配置变化的场景。

| `optionsFrom` 字段 | 必填 | 说明 |
| --- | --- | --- |
| `scope` | 否 | 用来决定从哪份配置里读取候选值：`"parent"`（当前字段所在的对象，默认）、`"current"`（当前字段自己）或 `"document"`（整份配置）。含义同[条件作用域](#单个条件)。 |
| `path` | 是 | 用来指定在作用域里读取哪个对象或列表，填相对路径。`[]` 表示直接使用作用域中的值。 |
| `source` | 是 | 用来决定取对象的键还是值：`"keys"` 取键或列表下标，`"values"` 取值或列表元素。 |
| `enforce` | 否 | 用来要求用户选择的值必须仍然在候选列表里。默认 `false`；为 `true` 时，已设置且非 `null` 的值不在候选中会报错。 |

同时设置静态选项和动态选项时，优先使用静态选项。来源缺失或不是对象、列表时，没有动态候选。来源是启用了 Spring 解析的占位符时，暂缓候选值校验。候选项变化不会自动改写已选值；启用 `enforce` 后，旧选项需要重新选择。是否允许清空仍由字段自己的规则决定。

例如，利用同级的 `profiles` Map 选择默认提醒方案：

```json
{
  "key": "active-profile",
  "label": "默认提醒方案",
  "type": "string",
  "optionsFrom": {
    "scope": "parent",
    "path": ["profiles"],
    "source": "keys",
    "enforce": true
  },
  "nullable": true
}
```

### 扩展其他模块的选项

`optionContributions` 用在模块根对象上，用来给另一个模块的下拉选项补充选项。例如你的插件提供了企业微信发送能力，可以把它加到通知模块的「通知方式」里。每一项的 `path` 和 `options` 都必填：前者是配置文档中的完整路径，后者与静态选项格式相同。使用之前，应先与目标模块约定好取值和对应的功能。

```json
{
  "path": ["starbot", "notify", "channels", "provider"],
  "options": [
    {"label": "企业微信", "value": "wecom"}
  ]
}
```

配置中心按带类型的 `value` 去重，保留目标已有的选项，并用合并后的列表进行校验。目标字段不存在时，会提示配置元数据问题。扩展选项不会安装对应的业务实现，上例中的插件还需要提供企业微信的发送实现。

### 界面设置

`ui` 用来调整字段的显示方式，每一项都可以省略。

| `ui` 字段 | 说明 |
| --- | --- |
| `widget` | 用来指定输入控件的类型，具体取值见下表。不填时根据字段类型和候选选项自动选择。 |
| `unit` | 用来在输入框旁显示单位，例如 `"秒"` 或 `"MiB"`。只改变展示，不换算配置值。 |
| `itemTitlePath` | 用来给复杂列表的每一项取标题，填相对于元素的路径，例如 `["name"]`。标题会随输入实时更新，不填时显示「第 N 项」。 |
| `emptyAsDefault` | 用来在用户清空输入时恢复默认值。默认 `false`；不能与 `emptyAsNull` 同时启用。 |
| `confirmOnEnable` | `type: "boolean"` 的开关从关闭切到开启时显示的确认文案。填非空文本，最多 256 个字符。 |
| `confirmOnDisable` | `type: "boolean"` 的开关从开启切到关闭时显示的确认文案。填非空文本，最多 256 个字符。 |

下表列出常用的 `widget` 取值；没有专门处理的取值按普通输入框显示。

| `widget` 取值 | 页面效果 |
| --- | --- |
| `"password"` | 把输入内容遮住，并提供显示和隐藏按钮。需要校验可用字符时，可另加 `format: "password"`。 |
| `"textarea"` | 多行文本框。 |
| `"slider"` | 数值滑块，配合 `minimum` 和 `maximum` 使用。 |
| `"color"` | 颜色选择器和文本框，可配合 `format: "color"` 校验。 |
| `"select"` | 有 `options` 或 `optionsFrom` 时自动显示为下拉框；单独填写 `widget` 不会生成选项。 |
| `"tree"` | `type: "any"` 时自动使用树形编辑器；单独填写 `widget` 不会改变字段类型。 |

### 格式

`format` 和校验规则 `{"rule": "format", "value": "格式名"}` 使用同一组格式名。

| 格式名 | 用来校验什么 |
| --- | --- |
| `listen-address` | 完整域名（如 `smtp.example.com`）、IPv4、IPv6 或 `localhost`。IPv6 可以带方括号和网卡标识，例如 `fe80::1%eth0`。只检查写法，地址中不能带协议、端口或路径。 |
| `email` | 邮箱地址文本，例如 `"alerts@example.com"`。只检查写法，不判断邮箱能否收信。 |
| `password` | 只允许大小写英文字母、数字和半角符号，即 ASCII 的 `!` 到 `~`。允许空字符串；必填、长度等要求可以单独声明。 |
| `http-base-url` | 带主机的绝对 HTTP 或 HTTPS 地址，可以包含端口和路径，不能包含凭据、查询参数或片段。 |
| `http-url` | 完整的 HTTP 或 HTTPS 地址，可以带主机名、IPv4 或方括号 IPv6、端口、路径和查询参数。不允许凭据或 `#` 片段。非 ASCII 域名使用 Punycode，地址中的特殊字符使用百分号编码。 |
| `color` | 颜色文本，如 `"#336699"`、`"336699"`、`"51,102,153"`，或 `white`、`black`、`red`、`green`、`blue`、`yellow`、`orange`、`pink`、`gray`、`light_gray`、`dark_gray`、`cyan`、`magenta`。 |
| `url` | 带协议的绝对地址。只接受 HTTP 或 HTTPS 的场景应使用 `http-url` 或 `http-base-url`。 |
| `uuid` | 8-4-4-4-12 位十六进制 UUID，带连字符。 |
| `ipv4` | 四段用点号分隔的十进制数字，每段为 0 到 255。 |
| `hostname` | 主机名，整串最多 253 个字符。每段 1 到 63 个字符，首尾为字母或数字，中间可以用连字符。 |
| `json` | 可解析的 JSON **文本**。想直接编辑 JSON 对象或列表时，使用对应的字段类型。 |

## 6. 条件

条件用来控制「什么时候显示」「什么时候必填」「什么时候校验」。可以写在字段、动作或校验规则里，也可以在模块根对象的 `conditions` 中预先定义并命名，供本模块的字段、动作和校验规则按名称引用。

### 单个条件

| 字段 | 必填 | 说明 |
| --- | --- | --- |
| `scope` | 否 | 用来决定从哪份配置里取值比较：`"parent"`（当前字段所属的对象，默认）、`"current"`（当前字段的值）或 `"document"`（整份配置）。动作的 `parent` 和 `current` 都指动作所在分组的配置。 |
| `path` | 是 | 用来指定在作用域里取哪个值，填相对路径。`[]` 表示直接比较作用域中的值。 |
| `op` | 是 | 用来指定比较方式，取值见下表。 |
| `value` | 视比较符而定 | 用来指定比较目标。判断存在或为空时不需要。 |
| `measure` | 否 | 用来先把值换成长度再比较。填 `"size"` 时，先取字符数、列表长度或对象的键数。 |

| `op` | `value` | 成立条件 |
| --- | --- | --- |
| `=`、`!=` | JSON 值 | 相等或不相等，比较时保留 JSON 类型。 |
| `>`、`>=`、`<`、`<=` | 数字 | 左侧数值满足相应的不等式。 |
| `in`、`!in` | JSON 数组 | 左侧值属于或不属于候选数组。 |
| `contains`、`!contains` | 子字符串或 JSON 值 | 左侧文本或列表包含或不包含指定内容。 |
| `hasKey`、`!hasKey` | 键名文本 | 左侧对象有或没有指定键。 |
| `exists`、`!exists` | 省略 | 字段存在或不存在。显式 `null` 算存在。 |
| `empty`、`!empty` | 省略 | 空或非空。缺失、`null`、空字符串、空列表和空对象算空；`false`、`0` 和纯空格文本不算空。 |
| `startsWith`、`!startsWith`、`endsWith`、`!endsWith` | 文本 | 检查前缀或后缀。 |
| `matches`、`!matches` | 正则表达式 | 文本能或不能找到匹配；使用 `^` 和 `$` 匹配整段。 |
| `between`、`!between` | `[min, max]` | 数值位于含边界的区间内或区间外。 |

### 组合条件

| 写法 | 取值 | 成立条件 |
| --- | --- | --- |
| `all` | 条件数组 | 所有子条件成立。空数组成立。 |
| `any` | 条件数组 | 至少一个子条件成立。空数组不成立。 |
| `not` | 单个条件对象 | 子条件不成立。 |

一个条件对象只使用一种组合写法，允许嵌套。引用接入 Spring 的占位符时，条件有时无法提前判断。字段的 `visibleWhen` 和 `validateWhen` 只有明确为 `false` 才隐藏或停用；必填条件的判断，以及配置动作的显示和执行，都要求条件明确为 `true`。

下面是模块根对象中 `conditions` 的片段。自动提醒相关字段可以引用 `auto-remind`：

```json
{
  "auto-remind": {
    "all": [
      {"scope": "document", "path": ["starbot", "reminder", "enabled"], "op": "=", "value": true},
      {"scope": "parent", "path": ["mode"], "op": "=", "value": "daily"}
    ]
  }
}
```

## 7. 校验规则

简单限制直接写在字段上，例如 `required`、`nonBlank`、`minimum`、`maximum` 和 `format`。需要组合条件、比较其他字段或自定义错误提示时，使用 `validators`。

每条规则先写 `rule`，再填写它需要的 `scope`、`path`、`value` 等参数。`when` 用来限定一条普通规则什么时候执行，`message` 用来提供具体的失败提示。规则引用其他字段时，`scope` 和 `path` 默认分别是 `"current"` 和 `[]`；引用同级字段时，应显式写 `"scope": "parent"`。只声明格式、长度等限制，并不会让字段变成必填。

### 基础、数字与文本

| `rule` | 参数 | 用途 |
| --- | --- | --- |
| `required` | 无 | 用来要求字段必须有值；缺失、`null` 和空字符串都不通过。 |
| `nonBlank` | 无 | 用来要求文本中至少有一个非空白字符。 |
| `notEmpty` | 无 | 用来拒绝空字符串、空列表和空对象。如果还要求字段存在且不为 `null`，需要同时设置必填。 |
| `minimum`、`maximum` | `value` 必填，数字 | 用来限制含边界的下限或上限。 |
| `exclusiveMinimum`、`exclusiveMaximum` | `value` 必填，数字 | 用来要求严格大于下限或严格小于上限。 |
| `range` | `min`、`max` 必填，数字 | 用来要求同时满足含边界的上下限。 |
| `multipleOf` | `value` 必填，正数 | 用来要求数值是 `value` 的整数倍。 |
| `integer` | 无 | 用来要求数值是整数。 |
| `finite` | 无 | 用来要求数值有限。 |
| `port` | 可选布尔值 `allowZero`，默认 `false` | 用来要求端口是 1 到 65535 的整数，`allowZero: true` 时允许 0。 |
| `trimmed` | 无 | 用来要求文本首尾没有空白字符。 |
| `lowercase`、`uppercase` | 无 | 用来要求文本保持小写或大写。 |
| `minLength`、`maxLength`、`length` | `value` 必填，填写非负整数 | 用来要求文本字符数至少、至多或恰好为指定数量。 |
| `pattern` | `value` 必填，正则表达式文本 | 用来要求文本中存在匹配。 |
| `enum` | `values` 必填，JSON 数组 | 用来要求当前值等于其中一个候选值。 |
| `format` | `value` 必填，[格式名](#格式) | 用来校验配置值的格式。 |
| `positiveDuration` | 无 | 用来要求时长大于零。 |
| `durationRange` | `min`、`max` 必填，均为合法时长，且 `min` 不大于 `max` | 用来要求时长处于含边界的范围内。 |

### 列表与对象

| `rule` | 参数 | 用途 |
| --- | --- | --- |
| `contains` | `value` 必填，JSON 值 | 用来要求列表至少包含一个指定值。 |
| `minContains`、`maxContains` | `value` 和 `count` 必填，`count` 填写非负整数 | 用来要求列表中指定值的出现次数至少或至多为 `count`。 |
| `minItems`、`maxItems` | `value` 必填，填写非负整数 | 用来要求列表元素数量的下限或上限。 |
| `minKeys`、`maxKeys` | `value` 必填，填写非负整数 | 用来要求对象键数量的下限或上限。 |
| `uniqueItems` | 无 | 用来要求列表元素互不重复。 |
| `uniqueItemField` | 可选 `path`，相对于每个元素 | 用来要求列表各元素指定字段的值互不重复。未设置该字段的元素跳过比较。 |
| `requiredKeys`、`allowedKeys` | `keys` 必填，字符串数组 | 用来要求对象必须包含列出的全部键，或只允许使用列出的键。 |
| `keyPattern` | `value` 必填，正则表达式文本 | 用来要求对象中的每个键名都能找到匹配。 |

### 字段引用和联动

| `rule` | 参数 | 用途 |
| --- | --- | --- |
| `keyInMap` | `scope`、`path` 指向对象，可选布尔值 `allowUnset`，默认 `false` | 用来要求当前值是目标对象的键。要接受空值时可使用 `allowUnset: true`。 |
| `valueInField` | `scope`、`path` 指向列表 | 用来要求当前值存在于目标列表中。 |
| `equalTo`、`notEqualTo` | `scope`、`path` 指向对照值 | 用来要求当前值等于或不等于对照值。 |
| `compare` | `op` 必填，使用条件比较符，`scope`、`path` 指向右侧值 | 用来按指定比较符比较当前值和对照值。 |
| `uniqueAcross` | `scope`、`path` 指向目标列表或对象，可选 `itemPath` | 用来要求当前值或当前列表元素不与目标列表元素、目标对象的值重复。填写 `itemPath` 时，先读取目标每一项的指定字段再比较。 |
| `dependentRequired` | `dependencies` 必填，数组 | 用来在一项有值时要求另一组字段也有值。每项填写 `when` 路径和 `require` 路径数组；当前对象中 `when` 指向的字段非空时，`require` 指向的字段都须非空。 |
| `atLeastOne`、`exactlyOne` | `paths` 必填，路径数组 | 用来要求当前对象中指定字段至少一项或恰好一项非空。 |
| `allOrNone`、`mutuallyExclusive` | `paths` 必填，路径数组 | 用来要求指定字段全部填写或全部留空，或最多填写一项。 |
| `requiredWhen`、`forbiddenWhen` | `when` 必填，条件对象或名称 | 用来在条件成立时要求当前值非空或为空。 |
| `nonEmptyOnlyWhen` | `when` 必填，条件对象或名称，可选 `path` | 用来要求当前值或其内部路径指向的值非空时，条件不能为 `false`。条件的 `parent` 和 `current` 均为当前值。 |

对 `requiredWhen`、`forbiddenWhen` 和 `nonEmptyOnlyWhen` 来说，`when` 是规则本身的必填参数。其他规则只在 `when` 明确成立时执行；省略 `when` 时照常校验。要同时停用一个字段及其子项的所有校验，请使用字段的 `validateWhen`。

例如，启用提醒时至少选择一个通知渠道。关闭提醒时允许列表为空，但已有条目仍需符合类型和其他规则：

```json
{
  "key": "channels",
  "label": "通知渠道",
  "type": "array",
  "items": {"type": "string", "nonBlank": true},
  "validators": [
    {
      "rule": "minItems",
      "value": 1,
      "when": {"scope": "parent", "path": ["enabled"], "op": "=", "value": true},
      "message": "启用提醒后至少填写一个通知渠道"
    }
  ]
}
```

`dependentRequired` 的路径从当前对象开始，例如填写用户名时同时要求密码：

```json
{
  "rule": "dependentRequired",
  "dependencies": [
    {"when": ["username"], "require": [["password"]]}
  ],
  "message": "填写用户名后也需要填写密码"
}
```

在 `keyMeta` 的普通规则中，`when` 的 `current` 是当前 Map 键名，`parent` 和 `document` 都是只含 `key` 的对象。正则最多 256 个字符，匹配文本最多 4096 个字符；应使用简单模式，避免以 `(?` 开头的扩展写法、反向引用，以及对整个分组重复量化的写法。

这些规则只服务于配置中心的校验；`ConfigService.set` 和正常启动都不等同于提交表单。插件仍应在使用配置前检查业务上必须满足的条件。

## 8. 配置动作

在分组的 `actions` 中声明按钮。动作可以接收当前页面草稿中的指定参数，用户可以直接执行，无需先保存或重启。检查连接参数、预览输出等动作可以使用 `handler`，需要访问运行中服务的动作可以使用 `handlerBean`。

### 动作对象

| 字段 | 必填 | 说明 |
| --- | --- | --- |
| `id` | 是 | 用来在分组内标识这个按钮，例如 `"check-notify"`。同一个分组内不能重复。 |
| `label` | 是 | 用来填写按钮文字。 |
| `description` | 否 | 用来填写按钮下方的说明，支持换行。 |
| `handler` | 与 `handlerBean` 二选一 | 填写处理器的完整类名。处理器需实现 `ConfigActionHandler`，并提供公开的无参构造方法。 |
| `handlerBean` | 与 `handler` 二选一 | 填写 Spring Bean 名称。该 Bean 需实现 `ConfigActionHandler`。 |
| `synchronous` | 否 | 用来决定点击后是否等待结果。默认 `false`，后台执行，页面再查询结果；为 `true` 时请求会等执行完再返回。 |
| `parameters` | 否 | 用来声明按钮需要读取哪些配置。每个参数名对应一个参数选择器，写法见[动作参数](#动作参数)。省略或填 `{}` 时，处理器收到空对象。 |
| `confirmation` | 否 | 用来在点击后弹出确认提示。填非空文本；省略时无需二次确认。 |
| `visibleWhen` | 否 | 用来控制按钮什么时候显示。条件明确为 `true` 时显示，并在后端执行前复核。 |

使用 `handler` 时，StarBot 根据类名创建处理器，正常运行和独立配置模式下都可以执行。

使用 `handlerBean` 时，StarBot 按名称从 Spring 容器获取处理器。插件处理器可用 `@StarBotComponent` 注册，并通过构造器注入所需的 Bean。独立配置模式下，满足显示条件的按钮仍会显示，但不可点击，页面会以红色提示「此操作仅在 StarBot 正常运行时可用」。

耗时操作通常使用默认的后台执行方式。配置中心最多同时执行三个动作，同一个按钮在执行期间不会重复启动任务。

处理器的 `execute(JSONObject parameters)` 返回成功提示，返回空白时自动显示「执行成功」。失败时，页面显示异常类名和消息，控制台记录堆栈。不要在异常消息中包含凭据或完整的请求内容。

### 动作参数

每个参数都必须从 `path` 或 `springProperties: true` 中选择一种来源，两者不能同时出现。

| 参数字段 | 适用参数 | 必填 | 说明 |
| --- | --- | --- | --- |
| `path` | 普通参数 | 是 | 用来指定要从配置文档的哪个位置读取值。填从根开始、非空的字符串数组，可以选中一个字段、整个对象或整个列表；不能通过路径访问单个列表下标。用 `["advanced"]` 可以按 JSON 结构读取高级配置，属性名不会展开。 |
| `springProperties` | Spring 参数 | 是 | 用来告诉 StarBot 按 Spring 属性读取配置。只能填布尔值 `true`，表示从 `advanced` 选取配置，并按启动时的规则展开成 Spring 属性。 |
| `keyPrefix` | 两种参数 | 否 | 用来按前缀筛选所选对象的直接键。填非空白字符串，最多 256 个字符，区分大小写。 |
| `keys` | Spring 参数 | 否 | 用来精确指定要读取的 Spring 属性名。填非空数组，每项都是非空白且不重复的完整键名，区分大小写。与 `keyPrefix` 互斥。 |

筛选发生在 Spring 属性展开之前。`keys` 精确匹配 `advanced` 下的直接键；选中的 Map 或列表会按整个条目选入，再展开其内部属性。不能用展开后的子键筛选某个 Map 条目。两种筛选都省略时，普通参数传递路径选中的完整值，Spring 参数读取全部高级属性。建议只传入按钮真正需要的字段。

### 按钮示例

例如给提醒插件的通知分组增加「检查提醒参数」按钮。这里假设分组路径为 `["delivery"]`，业务字段为 `enabled` 和 `endpoint`，另外还有两个已声明的高级 Spring 属性：

```json
{
  "id": "check-notify",
  "label": "检查提醒参数",
  "description": "检查当前填写的提醒地址和连接参数",
  "handler": "com.example.reminder.CheckNotifyAction",
  "synchronous": true,
  "parameters": {
    "enabled": {"path": ["starbot", "reminder", "delivery", "enabled"]},
    "endpoint": {"path": ["starbot", "reminder", "delivery", "endpoint"]},
    "push": {
      "springProperties": true,
      "keys": ["reminder.push.timeout", "reminder.push.flags"]
    }
  },
  "confirmation": "使用当前页面的配置检查提醒参数？",
  "visibleWhen": {"path": ["enabled"], "op": "=", "value": true}
}
```

需要整个属性前缀时，把 `push` 的声明改成 `{"keyPrefix": "reminder.push.", "springProperties": true}`。本例中，处理器通过 `parameters.getString("endpoint")` 取得地址，通过 `parameters.getJSONObject("push")` 取得展开的属性，例如 `reminder.push.flags[retry.on-timeout]`。

```java
package com.example.reminder;

import com.alibaba.fastjson2.JSONObject;
import com.starlwr.bot.core.config.action.ConfigActionHandler;

public class CheckNotifyAction implements ConfigActionHandler {
    @Override
    public String execute(JSONObject parameters) {
        String endpoint = parameters.getString("endpoint");
        if (endpoint == null || endpoint.isBlank()) {
            throw new IllegalArgumentException("请先填写提醒地址");
        }
        JSONObject push = parameters.getJSONObject("push");
        String timeoutKey = "reminder.push.timeout";
        if (push.containsKey(timeoutKey) && push.getIntValue(timeoutKey) < 1) {
            throw new IllegalArgumentException("连接超时必须大于零");
        }
        return "提醒参数检查通过";
    }
}
```

普通参数保留所选值的 JSON 类型，包括 `false`、`0`、`null` 和空集合。路径缺失时，先找元数据中对应的默认值，仍无值时传入 `null`。按前缀筛选已经找到的对象时，没有匹配项就返回 `{}`；如果找到的是文本、列表或 `null`，动作会报错。Spring 参数始终返回对象，优先使用草稿中的值，再补充所选项的默认值；值为 `null` 的 Spring 属性会从结果中省略。传给处理器的对象和列表都是副本。

动作条件引用其他字段时，应把这些字段也纳入参数范围。页面有配置错误或未解决的配置冲突时不会执行按钮；后端会复核动作所需的配置、冲突和启用条件。动作额外要求的值由处理器自行检查。

### 注入 Spring Bean 的按钮

以「查看当前消息后缀」按钮为例。在分组的 `actions` 数组中声明动作，处理器通过构造器注入配置 Bean，读取当前生效的消息后缀。

```json
{
  "id": "show-message-suffix",
  "label": "查看当前消息后缀",
  "handlerBean": "starBotExampleMessageSuffixAction"
}
```

处理器注册后的默认 Bean 名称为 `starBotExampleMessageSuffixAction`。

```java
package com.example;

import com.alibaba.fastjson2.JSONObject;
import com.starlwr.bot.core.config.action.ConfigActionHandler;
import com.starlwr.bot.core.plugin.StarBotComponent;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 消息后缀查看动作
 */
@StarBotComponent
public class StarBotExampleMessageSuffixAction implements ConfigActionHandler {
    /**
     * 示例插件配置类
     */
    private final StarBotExampleProperties properties;

    @Autowired
    public StarBotExampleMessageSuffixAction(StarBotExampleProperties properties) {
        this.properties = properties;
    }

    /**
     * 读取当前生效的消息后缀
     * @param parameters 动作参数
     * @return 当前消息后缀提示
     */
    @Override
    public String execute(JSONObject parameters) {
        String suffix = properties.getMessageSuffix();
        return StringUtils.isEmpty(suffix) ? "当前消息后缀为空" : "当前消息后缀为「" + suffix + "」";
    }
}
```

## 9. 编写检查

- 文件使用 UTF-8 编码的 JSON，元数据上限为 8 MiB。字段和条件的嵌套校验上限均为 32 层；路径最多 32 段，每段必须是非空字符串。空路径 `[]` 可以表示没有前缀或引用当前作用域；动作参数的 `path` 必须非空。
- 模块标题、分组 ID 和标题、字段键、`springKey`、动作 ID、按钮文字、处理器类名或 Bean 名称、确认文字，以及动作参数名和前缀等标识文本，都不能为空白，最长 256 个字符。
- 打包后确认 JAR 中包含 `plugin.json` 和 `config/config.json`。在配置中心验证初始值、隐藏和必填条件、动作参数、修改与恢复后的 JSON，以及需要重启的标记。
