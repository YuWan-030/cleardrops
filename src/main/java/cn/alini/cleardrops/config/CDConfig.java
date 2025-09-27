package cn.alini.cleardrops.config;

import cn.alini.cleardrops.Cleardrops;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;

import java.util.Arrays;
import java.util.List;

public final class CDConfig {

    public static final ForgeConfigSpec SERVER_SPEC;

    public static final ForgeConfigSpec.BooleanValue ENABLE_AUTO_COLLECT;
    public static final ForgeConfigSpec.IntValue TICKS_PER_COLLECT;
    public static final ForgeConfigSpec.IntValue TICKS_KEEP_AFTER_COLLECT;
    public static final ForgeConfigSpec.BooleanValue ANNOUNCE_MESSAGES;

    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> ITEM_BLACKLIST;

    public static final ForgeConfigSpec.BooleanValue ENABLE_VOID_PROTECT;
    public static final ForgeConfigSpec.IntValue VOID_RAISE_MAX_STEPS;
    public static final ForgeConfigSpec.BooleanValue VOID_RESET_VELOCITY;

    public static final ForgeConfigSpec.ConfigValue<String> PREFIX;
    public static final ForgeConfigSpec.ConfigValue<List<? extends Integer>> WARN_SECONDS;

    public static final ForgeConfigSpec.ConfigValue<String> MSG_PRE_COLLECT;
    public static final ForgeConfigSpec.ConfigValue<String> MSG_COLLECTED;
    public static final ForgeConfigSpec.ConfigValue<String> MSG_PURGED;
    public static final ForgeConfigSpec.ConfigValue<String> MSG_STATUS;
    public static final ForgeConfigSpec.ConfigValue<String> MSG_HELP;
    public static final ForgeConfigSpec.ConfigValue<String> MSG_CMD_COLLECT;
    public static final ForgeConfigSpec.ConfigValue<String> MSG_CMD_COLLECT_NONE;
    public static final ForgeConfigSpec.ConfigValue<String> MSG_CMD_PURGE;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();

        b.push("general");
        ENABLE_AUTO_COLLECT = b.comment("是否启用周期性自动回收世界掉落物。").define("enableAutoCollect", true);
        TICKS_PER_COLLECT = b.comment("两次自动回收之间的间隔（tick）。20 tick = 1 秒。").defineInRange("ticksPerCollect", 20 * 60 * 5, 20, 20 * 60 * 60);
        TICKS_KEEP_AFTER_COLLECT = b.comment("每次回收后，回收站物品保留的时间（tick），到时自动清空。20 tick = 1 秒。").defineInRange("ticksKeepAfterCollect", 20 * 60 * 2, 0, 20 * 60 * 60);
        ANNOUNCE_MESSAGES = b.comment("是否向在线玩家广播提示消息（预警、已回收、已清空等）。").define("announceMessages", true);
        b.pop();

        b.push("blacklist");
        ITEM_BLACKLIST = b.comment("物品黑名单（物品 ID，形如 minecraft:nether_star）。黑名单内的物品不会被本模组清空/销毁。").defineList("items", Arrays.asList("minecraft:nether_star", "minecraft:totem_of_undying"), o -> o instanceof String && ((String) o).contains(":"));
        b.pop();

        b.push("void_protection");
        ENABLE_VOID_PROTECT = b.comment("是否启用虚空物品保护（掉落物掉入虚空时，自动拉回到世界最低构建高度以上的安全位置）。").define("enableVoidProtect", true);
        VOID_RAISE_MAX_STEPS = b.comment("向上寻找空气可站立位置的最大步数（以方块为单位）。").defineInRange("raiseMaxSteps", 6, 1, 32);
        VOID_RESET_VELOCITY = b.comment("拉回时是否将物品速度清零。").define("resetVelocity", true);
        b.pop();

        b.push("messages");
        PREFIX = b.comment("消息前缀（支持渐变标签）。示例：<gradient:#00E5FF:#00FF7F>[清道夫]</gradient>").define("prefix", "<gradient:#00E5FF:#00FF7F>[清道夫]</gradient>");
        WARN_SECONDS = b.comment("自动回收前的预警秒数列表（到这些秒数时会发送预警消息）。").defineList("warnSeconds", Arrays.asList(60, 30, 10, 5, 4, 3, 2, 1), o -> o instanceof Integer && (Integer) o >= 0);

        MSG_PRE_COLLECT = b.comment("预警消息模板（支持占位符与渐变标签）。可用占位符：{time}（剩余秒数）。").define("msgPreCollect", "即将在 {time}s 后回收世界掉落物，请及时拾取重要物品！");
        MSG_COLLECTED = b.comment("自动回收完成消息模板。占位符：{entities}（实体数量）、{keep}（回收站保留秒数）。").define("msgCollected", "已回收掉落物，共 {entities} 个实体。使用 /cleardrops gui 领取，{keep}s 后清空。");
        MSG_PURGED = b.comment("回收站清空消息模板。占位符：{slots}（被清空的槽位数）。").define("msgPurged", "回收站已到期，清空 {slots} 个槽位。");
        MSG_STATUS = b.comment("状态消息模板。占位符：{slotsUsed}、{slotsTotal}、{items}、{keep}（倒计时或'未安排'）、{auto}、{interval}（秒）、{keepCfg}（秒）、{announce}。").define("msgStatus", "回收站：{slotsUsed}/{slotsTotal} 槽，物品 {items} 个；清空倒计时：{keep}；自动回收：{auto}，间隔：{interval}s，保留：{keepCfg}s，广播：{announce}。");
        MSG_HELP = b.comment("help 文本（支持渐变标签与换行 \\n）。建议给标题或命令加一个好看的渐变色。").define("msgHelp", "<gradient:#7AD7F0:#4FC3F7>Cleardrops 帮助</gradient>\\n<gradient:#A8FF78:#78FFD6>/cleardrops gui</gradient> - 打开回收站\\n<gradient:#A8FF78:#78FFD6>/cleardrops status</gradient> - 查看状态\\n<gradient:#FFD54F:#FF8A00>/cleardrops collect</gradient> - 立即回收（权限2）\\n<gradient:#FFD54F:#FF8A00>/cleardrops purge</gradient> - 立即清空（权限2）\\n<gradient:#B388FF:#7C4DFF>/cleardrops reload</gradient> - 重载配置（权限2）");
        MSG_CMD_COLLECT = b.comment("手动执行 /cleardrops collect 时的反馈消息。占位符：{entities}、{keep}。").define("msgCmdCollect", "已回收掉落物，共 {entities} 个实体。{keep}s 内可在回收站领取。");
        MSG_CMD_COLLECT_NONE = b.comment("手动执行 /cleardrops collect 时如没有可清理的掉落物时的反馈消息。").define("msgCmdCollectNone", "没有可清理的掉落物！");
        MSG_CMD_PURGE = b.comment("手动执行 /cleardrops purge 时的反馈消息。占位符：{slots}。").define("msgCmdPurge", "已清空回收站，清除了 {slots} 个槽位中的物品（黑名单物品已跳过）。");
        b.pop();

        SERVER_SPEC = b.build();
    }

    private CDConfig() {}

    public static void register() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, SERVER_SPEC);
    }

    public static void onLoad(ModConfigEvent.Loading event) {
        if (event.getConfig().getSpec() == SERVER_SPEC) {
            Cleardrops.SCHEDULER.applyConfig();
        }
    }

    public static void onReload(ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() == SERVER_SPEC) {
            Cleardrops.SCHEDULER.applyConfig();
        }
    }
}