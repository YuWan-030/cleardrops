package cn.alini.cleardrops.server;

import cn.alini.cleardrops.config.CDConfig;
import cn.alini.cleardrops.storage.TrashStorage;
import cn.alini.cleardrops.util.MessageUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.*;

public class ServerScheduler {

    // 运行时参数（支持热重载）
    private int intervalTicks = 20 * 60 * 5;   // 默认 5 分钟
    private int keepTicks = 20 * 60 * 2;       // 默认 2 分钟
    private boolean autoCollect = true;
    private boolean announce = true;

    private String prefixRaw = "[清道夫]";
    private List<Integer> warnSeconds = Arrays.asList(60, 30, 10, 5, 4, 3, 2, 1);

    private String msgPreCollect = "即将在 {time}s 后回收世界掉落物，请及时拾取重要物品！";
    private String msgCollected = "已回收掉落物，共 {entities} 个实体。使用 /cleardrops gui 领取，{keep}s 后清空。";
    private String msgPurged = "回收站已到期，清空 {slots} 个槽位。";
    private String msgStatus = "回收站：{slotsUsed}/{slotsTotal} 槽，物品 {items} 个；清空倒计时：{keep}；自动回收：{auto}，间隔：{interval}s，保留：{keepCfg}s，广播：{announce}。";
    private String msgHelp = "<gradient:#7AD7F0:#4FC3F7>Cleardrops 帮助</gradient>\\n..."
            .replace("\\n", "\n");
    private String msgCmdCollect = "已回收掉落物，共 {entities} 个实体。{keep}s 内可在回收站领取。";
    private String msgCmdPurge = "已清空回收站，清除了 {slots} 个槽位中的物品（黑名单物品已跳过）。";

    // 虚空保护运行时
    private boolean voidProtect = true;
    private int voidRaiseMax = 6;
    private boolean voidResetVel = true;

    // 黑名单（物品 ID，如 minecraft:nether_star）
    private final Set<ResourceLocation> blacklist = new HashSet<>();

    // 计时器
    private int collectCountdown = intervalTicks;
    private int purgeCountdown = -1; // <0 表示未安排清空
    private final Set<Integer> warnedThisCycle = new HashSet<>();

    private final TrashStorage trashStorage = new TrashStorage(54);

    public TrashStorage getTrashStorage() {
        return trashStorage;
    }

    // 从配置应用到运行时（加载/热重载/命令触发）
    public synchronized void applyConfig() {
        int newInterval = CDConfig.TICKS_PER_COLLECT.get();
        int newKeep = CDConfig.TICKS_KEEP_AFTER_COLLECT.get();
        boolean newAuto = CDConfig.ENABLE_AUTO_COLLECT.get();
        boolean newAnnounce = CDConfig.ANNOUNCE_MESSAGES.get();

        this.prefixRaw = CDConfig.PREFIX.get();

        // 预警秒数
        List<? extends Integer> ws = CDConfig.WARN_SECONDS.get();
        this.warnSeconds = new ArrayList<>();
        for (Object o : ws) {
            if (o instanceof Integer i && i >= 0) this.warnSeconds.add(i);
        }
        this.warnSeconds.sort(Comparator.reverseOrder());

        // 消息模板
        this.msgPreCollect = CDConfig.MSG_PRE_COLLECT.get();
        this.msgCollected = CDConfig.MSG_COLLECTED.get();
        this.msgPurged = CDConfig.MSG_PURGED.get();
        this.msgStatus = CDConfig.MSG_STATUS.get();
        this.msgHelp = CDConfig.MSG_HELP.get().replace("\\n", "\n");
        this.msgCmdCollect = CDConfig.MSG_CMD_COLLECT.get();
        this.msgCmdPurge = CDConfig.MSG_CMD_PURGE.get();

        // 虚空保护
        this.voidProtect = CDConfig.ENABLE_VOID_PROTECT.get();
        this.voidRaiseMax = CDConfig.VOID_RAISE_MAX_STEPS.get();
        this.voidResetVel = CDConfig.VOID_RESET_VELOCITY.get();

        // 黑名单
        this.blacklist.clear();
        for (Object o : CDConfig.ITEM_BLACKLIST.get()) {
            if (o instanceof String s && s.contains(":")) {
                try {
                    this.blacklist.add(new ResourceLocation(s));
                } catch (Exception ignored) {}
            }
        }

        int oldInterval = this.intervalTicks;
        int oldKeep = this.keepTicks;

        this.intervalTicks = newInterval;
        this.keepTicks = newKeep;
        this.autoCollect = newAuto;
        this.announce = newAnnounce;

        // 平滑调整倒计时
        if (collectCountdown > newInterval || oldInterval != newInterval) {
            collectCountdown = Math.min(collectCountdown, newInterval);
            if (collectCountdown <= 0) collectCountdown = newInterval;
            warnedThisCycle.clear();
        }
        if (purgeCountdown >= 0 && (purgeCountdown > newKeep || oldKeep != newKeep)) {
            purgeCountdown = Math.min(purgeCountdown, newKeep);
            if (purgeCountdown <= 0 && newKeep > 0) purgeCountdown = newKeep;
            if (newKeep == 0) purgeCountdown = 0; // 立即清空
        }
    }

    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;

        // 自动收集与预警
        if (autoCollect) {
            int secBefore = (collectCountdown + 19) / 20;
            if (announce && warnSeconds.contains(secBefore) && !warnedThisCycle.contains(secBefore)) {
                Map<String, String> ph = Map.of("time", String.valueOf(secBefore));
                broadcastPrefixed(server, msgPreCollect, ph);
                warnedThisCycle.add(secBefore);
            }

            if (--collectCountdown <= 0) {
                int moved = collectNow(server);
                if (moved > 0) {
                    purgeCountdown = keepTicks;
                    if (announce) {
                        Map<String, String> ph = Map.of(
                                "entities", String.valueOf(moved),
                                "keep", String.valueOf(keepTicks / 20)
                        );
                        broadcastPrefixed(server, msgCollected, ph);
                    }
                }
                collectCountdown = intervalTicks;
                warnedThisCycle.clear();
            }
        }

        // 自动清空
        if (purgeCountdown >= 0) {
            if (--purgeCountdown == 0) {
                int cleared = purgeNow();
                if (announce) {
                    Map<String, String> ph = Map.of("slots", String.valueOf(cleared));
                    broadcastPrefixed(server, msgPurged, ph);
                }
                purgeCountdown = -1;
            }
        }

        // 虚空物品保护
        if (voidProtect) {
            protectVoidItems(server);
        }
    }

    // 便于命令调用的无参版本
    public int collectNow() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return 0;
        return collectNow(server);
    }

    // 真正执行收集（已修正：黑名单物品不参与收集，留在世界中）
    public int collectNow(MinecraftServer server) {
        int entitiesMoved = 0;

        for (ServerLevel level : server.getAllLevels()) {
            // 仅遍历已加载区块内实体
            AABB box = new AABB(-3.0E7, level.getMinBuildHeight(), -3.0E7,
                    3.0E7, level.getMaxBuildHeight(), 3.0E7);

            List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, box, e ->
                    !e.isRemoved() && !e.getItem().isEmpty());

            for (ItemEntity e : items) {
                ItemStack stack = e.getItem();

                // 关键修正：黑名单物品不收集，不移除实体
                if (isBlacklisted(stack)) {
                    continue;
                }

                ItemStack rem = trashStorage.insertStack(stack);
                if (rem.isEmpty()) {
                    e.discard();
                } else if (rem.getCount() != stack.getCount()) {
                    e.setItem(rem);
                } else {
                    // 仓库满，收不进，保持实体不变
                    continue;
                }
                entitiesMoved++;
            }
        }
        if (entitiesMoved > 0) {
            purgeCountdown = keepTicks;
        }
        return entitiesMoved;
    }

    public int purgeNow() {
        int cleared = 0;
        for (int i = 0; i < trashStorage.getContainerSize(); i++) {
            ItemStack s = trashStorage.getItem(i);
            if (!s.isEmpty()) {
                if (isBlacklisted(s)) {
                    // 黑名单物品不清空
                    continue;
                }
                trashStorage.setItem(i, ItemStack.EMPTY);
                cleared++;
            }
        }
        trashStorage.setChanged();
        return cleared;
    }

    public Component statusMessage() {
        int slotsTotal = trashStorage.getContainerSize();
        int slotsUsed = 0;
        int totalItems = 0;
        int blacklistedStacks = 0;

        for (int i = 0; i < slotsTotal; i++) {
            ItemStack s = trashStorage.getItem(i);
            if (!s.isEmpty()) {
                slotsUsed++;
                totalItems += s.getCount();
                if (isBlacklisted(s)) blacklistedStacks++;
            }
        }
        String keepDisp = purgeCountdown >= 0 ? (purgeCountdown / 20) + "s" : "未安排";

        Map<String, String> ph = new HashMap<>();
        ph.put("slotsUsed", String.valueOf(slotsUsed));
        ph.put("slotsTotal", String.valueOf(slotsTotal));
        ph.put("items", String.valueOf(totalItems));
        ph.put("keep", keepDisp);
        ph.put("auto", String.valueOf(autoCollect));
        ph.put("interval", String.valueOf(intervalTicks / 20));
        ph.put("keepCfg", String.valueOf(keepTicks / 20));
        ph.put("announce", String.valueOf(announce));
        ph.put("blacklistedStacks", String.valueOf(blacklistedStacks));

        Component content = MessageUtil.render(msgStatus, ph);
        Component prefix = MessageUtil.render(prefixRaw);
        return MessageUtil.withPrefix(prefix, content);
    }

    public Component helpMessage() {
        Component prefix = MessageUtil.render(prefixRaw);
        Component content = MessageUtil.render(msgHelp);
        return MessageUtil.withPrefix(prefix, content);
    }

    public Component cmdCollectFeedback(int moved) {
        Map<String, String> ph = Map.of(
                "entities", String.valueOf(moved),
                "keep", String.valueOf(keepTicks / 20)
        );
        return MessageUtil.withPrefix(
                MessageUtil.render(prefixRaw),
                MessageUtil.render(msgCmdCollect, ph)
        );
    }

    public Component cmdPurgeFeedback(int cleared) {
        Map<String, String> ph = Map.of("slots", String.valueOf(cleared));
        return MessageUtil.withPrefix(
                MessageUtil.render(prefixRaw),
                MessageUtil.render(msgCmdPurge, ph)
        );
    }

    private void broadcastPrefixed(MinecraftServer server, String template, Map<String, String> ph) {
        Component prefix = MessageUtil.render(prefixRaw);
        Component content = MessageUtil.render(template, ph);
        Component msg = MessageUtil.withPrefix(prefix, content);
        server.getPlayerList().getPlayers().forEach(p -> p.sendSystemMessage(msg));
    }

    private boolean isBlacklisted(ItemStack stack) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return id != null && blacklist.contains(id);
    }

    // 虚空物品保护：把 minY 以下的物品拉回到 minY+ 附近的可生存空气位置
    private void protectVoidItems(MinecraftServer server) {
        for (ServerLevel level : server.getAllLevels()) {
            int minY = level.getMinBuildHeight();
            AABB box = new AABB(-3.0E7, Integer.MIN_VALUE / 4.0, -3.0E7,
                    3.0E7, minY, 3.0E7);
            List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, box, e ->
                    !e.isRemoved() && !e.getItem().isEmpty());

            for (ItemEntity e : items) {
                double x = Math.floor(e.getX()) + 0.5;
                double z = Math.floor(e.getZ()) + 0.5;
                int y = minY + 1;

                BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos((int)Math.floor(x), y, (int)Math.floor(z));

                // 若当前位置非空气，则向上寻找空气位置，最多 voidRaiseMax 步
                int steps = 0;
                while (steps < voidRaiseMax && !level.isEmptyBlock(pos)) {
                    pos.setY(pos.getY() + 1);
                    steps++;
                }
                // 再确保脚下非空气（避免继续下落）
                BlockPos below = pos.below();
                if (level.isEmptyBlock(below)) {
                    pos.setY(pos.getY() + 1);
                }

                e.setPos(x, pos.getY() + 0.25, z);
                if (voidResetVel) e.setDeltaMovement(0, 0, 0);
                e.setOnGround(true);
            }
        }
    }
}