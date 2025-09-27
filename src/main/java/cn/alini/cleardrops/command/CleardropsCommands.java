package cn.alini.cleardrops.command;

import cn.alini.cleardrops.Cleardrops;
import cn.alini.cleardrops.util.MessageUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider; // 1.20.1
import net.minecraft.world.inventory.ChestMenu;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class CleardropsCommands {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("cleardrops")
                        .then(Commands.literal("help")
                                .executes(ctx -> help(ctx.getSource())))
                        .then(Commands.literal("collect")
                                .requires(src -> src.hasPermission(2))
                                .executes(ctx -> collect(ctx.getSource())))
                        .then(Commands.literal("gui")
                                .executes(ctx -> openGui(ctx.getSource())))
                        .then(Commands.literal("purge")
                                .requires(src -> src.hasPermission(2))
                                .executes(ctx -> purge(ctx.getSource())))
                        .then(Commands.literal("status")
                                .executes(ctx -> status(ctx.getSource())))
                        .then(Commands.literal("reload")
                                .requires(src -> src.hasPermission(2))
                                .executes(ctx -> reload(ctx.getSource())))
        );
    }

    private static int help(CommandSourceStack src) {
        src.sendSuccess(() -> Cleardrops.SCHEDULER.helpMessage(), false);
        return 1;
    }

    private static int collect(CommandSourceStack src) {
        int moved = Cleardrops.SCHEDULER.collectNow();
        src.sendSuccess(() -> Cleardrops.SCHEDULER.cmdCollectFeedback(moved), true);
        return 1;
    }

    private static int openGui(CommandSourceStack src) {
        if (src.getEntity() instanceof ServerPlayer sp) {
            MenuProvider provider = new SimpleMenuProvider(
                    (containerId, playerInv, player) ->
                            ChestMenu.sixRows(containerId, playerInv, Cleardrops.SCHEDULER.getTrashStorage()),
                    Component.literal("回收站")
            );
            sp.openMenu(provider); // 原版容器，客户端无需安装模组
            return 1;
        }
        src.sendFailure(MessageUtil.render("只能由玩家执行。"));
        return 0;
    }

    private static int purge(CommandSourceStack src) {
        int cleared = Cleardrops.SCHEDULER.purgeNow();
        src.sendSuccess(() -> Cleardrops.SCHEDULER.cmdPurgeFeedback(cleared), true);
        return 1;
    }

    private static int status(CommandSourceStack src) {
        src.sendSuccess(() -> Cleardrops.SCHEDULER.statusMessage(), false);
        return 1;
    }

    private static int reload(CommandSourceStack src) {
        Cleardrops.SCHEDULER.applyConfig();
        src.sendSuccess(() -> MessageUtil.render("已从配置文件应用参数。"), true);
        return 1;
    }
}