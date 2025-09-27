# Cleardrops

一个仅服务端的 Forge 1.20.1 掉落物清理与回收站模组。支持自动/手动回收、原版 GUI 领取、配置热重载、渐变色消息、物品黑名单与虚空物品保护。原版客户端无需安装也可使用。

- 目标环境：Minecraft 1.20.1 + Forge 47.4.x（Java 17）
- Mod ID：`cleardrops`
- 客户端依赖：无（原版客户端即可）

## 功能特性

- 自动定时回收世界掉落物（可关闭/自定义间隔）
- 回收站（54 格）：使用原版 6 行箱子界面打开并领取
- 配置热重载：编辑并保存 `world/serverconfig/cleardrops-server.toml` 即生效，或执行 `/cleardrops reload`
- 可配置消息与渐变色前缀：
  - 支持 `<gradient:#RRGGBB:#RRGGBB>文本</gradient>` 渐变标签
  - 支持占位符如 `{time}`、`{entities}`、`{keep}` 等
- 预警广播：回收前指定秒数进行预警提醒（防止误清）
- 黑名单：在黑名单内的物品不会被扫地收走，也不会被本模组清空
- 虚空物品保护：掉入虚空的掉落物自动回拉至安全高度，避免丢失
- 原版兼容 GUI：客户端无需安装本模组即可打开“回收站”

## 安装

1. 服务器端放入 `mods/`：`cleardrops-x.y.z.jar`
2. 确保运行环境为 Forge 1.20.1（47.4.x）与 Java 17
3. 启动一次服务器以生成配置文件
4. 配置文件路径：`<世界目录>/serverconfig/cleardrops-server.toml`

## 命令

- `/cleardrops help` 显示帮助（带渐变色）
- `/cleardrops gui` 打开“回收站”原版箱子 GUI
- `/cleardrops status` 查看当前库存、倒计时与开关状态
- `/cleardrops collect` 立刻回收世界掉落物（需要权限 2）
- `/cleardrops purge` 立刻清空回收站（需要权限 2，黑名单物品跳过）
- `/cleardrops reload` 从配置文件应用参数（需要权限 2）

权限说明：命令默认遵循服务端权限等级（op 级别），`requires(src -> src.hasPermission(2))` 表示 OP Level ≥ 2。

## 配置与热重载

配置文件：`<世界>/serverconfig/cleardrops-server.toml`。保存后将自动热重载，也可执行 `/cleardrops reload`。

示例（片段）：
```toml
[general]
# 是否启用周期性自动回收世界掉落物。
enableAutoCollect = true
# 两次自动回收之间的间隔（tick）。20 tick = 1 秒。
ticksPerCollect = 6000
# 每次回收后，回收站物品保留的时间（tick），到时自动清空。20 tick = 1 秒。
ticksKeepAfterCollect = 2400
# 是否向在线玩家广播提示消息（预警、已回收、已清空等）。
announceMessages = true

[blacklist]
# 物品黑名单（物品 ID，形如 minecraft:nether_star）。
# 黑名单物品在“收集”阶段不会被扫走；在“清空”阶段也会被跳过。
items = ["minecraft:nether_star", "minecraft:totem_of_undying"]

[void_protection]
# 是否启用虚空物品保护（掉落物掉入虚空时，自动拉回到世界最低构建高度以上的安全位置）。
enableVoidProtect = true
# 向上寻找空气可站立位置的最大步数（以方块为单位）。
raiseMaxSteps = 6
# 拉回时是否将物品速度清零。
resetVelocity = true

[messages]
# 消息前缀（支持渐变标签），示例：
prefix = "<gradient:#00E5FF:#00FF7F>[清道夫]</gradient>"
# 自动回收前的预警秒数列表（到这些秒数时会发送预警消息）。
warnSeconds = [60, 30, 10, 5, 4, 3, 2, 1]

# 预警消息模板（占位符：{time}）
msgPreCollect = "即将在 {time}s 后回收世界掉落物，请及时拾取重要物品！"

# 自动回收完成消息模板（占位符：{entities}、{keep}）
msgCollected = "已回收掉落物，共 {entities} 个实体。使用 /cleardrops gui 领取，{keep}s 后清空。"

# 回收站清空消息模板（占位符：{slots}）
msgPurged = "回收站已到期，清空 {slots} 个槽位。"

# 状态消息模板（占位符：{slotsUsed}、{slotsTotal}、{items}、{keep}、{auto}、{interval}、{keepCfg}、{announce}）
msgStatus = "回收站：{slotsUsed}/{slotsTotal} 槽，物品 {items} 个；清空倒计时：{keep}；自动回收：{auto}，间隔：{interval}s，保留：{keepCfg}s，广播：{announce}。"

# help 文本（支持渐变与换行 \n）
msgHelp = "<gradient:#7AD7F0:#4FC3F7>Cleardrops 帮助</gradient>\n<gradient:#A8FF78:#78FFD6>/cleardrops gui</gradient> - 打开回收站\n<gradient:#A8FF78:#78FFD6>/cleardrops status</gradient> - 查看状态\n<gradient:#FFD54F:#FF8A00>/cleardrops collect</gradient> - 立即回收（权限2）\n<gradient:#FFD54F:#FF8A00>/cleardrops purge</gradient> - 立即清空（权限2）\n<gradient:#B388FF:#7C4DFF>/cleardrops reload</gradient> - 重载配置（权限2）"

# 手动命令反馈（占位符：见注释）
msgCmdCollect = "已回收掉落物，共 {entities} 个实体。{keep}s 内可在回收站领取。"
msgCmdPurge   = "已清空回收站，清除了 {slots} 个槽位中的物品（黑名单物品已跳过）。"
```

占位符总览：
- 预警：`{time}`
- 回收完成：`{entities}`、`{keep}`
- 清空回收站：`{slots}`
- 状态：`{slotsUsed}`、`{slotsTotal}`、`{items}`、`{keep}`、`{auto}`、`{interval}`、`{keepCfg}`、`{announce}`、`{blacklistedStacks}`（若模板中使用）

渐变标签：
- 语法：`<gradient:#RRGGBB:#RRGGBB>文本</gradient>`，将按字符线性插值上色
- 可嵌入到 prefix、help 或任意消息模板中

## 黑名单与虚空保护

- 黑名单物品：在“收集”时会被跳过，留在世界；在“清空回收站”时也会跳过，不被本模组销毁
- 虚空保护：检测世界最小构建高度以下的掉落物并拉回到安全位置（可配置是否清零速度与最大提升步数）

## 常见问题

- 客户端是否要装？不需要。本模组仅服务端，GUI 使用原版箱子容器。
- 与其它清理/回收类模组冲突？原则上可以并存，但可能出现重复回收。建议只保留一个主清理器或调整时间错峰。
- 配置不生效？确认编辑的是当前世界的 `serverconfig/cleardrops-server.toml`，保存后等待热重载或执行 `/cleardrops reload`。

## 开发与构建

- 依赖：JDK 17、Forge 1.20.1（47.4.x）
- 构建：`./gradlew build`，产物在 `build/libs/`
- 主要包结构：
  - `cn.alini.cleardrops.server.ServerScheduler` 核心逻辑（定时、收集/清空、预警、虚空保护、黑名单）
  - `cn.alini.cleardrops.config.CDConfig` 配置项与热重载事件
  - `cn.alini.cleardrops.command.CleardropsCommands` 命令入口
  - `cn.alini.cleardrops.util.MessageUtil` 渐变渲染与占位符替换
  - `cn.alini.cleardrops.storage.TrashStorage` 回收站容器

## 许可

本仓库采用 Creative Commons Attribution 4.0 International（CC BY 4.0）许可，详情见 [LICENSE](LICENSE)。

你可以在保留署名的前提下复制、修改与分发本项目。
