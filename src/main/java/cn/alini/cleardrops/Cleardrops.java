package cn.alini.cleardrops;

import com.mojang.logging.LogUtils;
import cn.alini.cleardrops.command.CleardropsCommands;
import cn.alini.cleardrops.config.CDConfig;
import cn.alini.cleardrops.server.ServerScheduler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(Cleardrops.MODID)
public class Cleardrops {

    public static final String MODID = "cleardrops";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final ServerScheduler SCHEDULER = new ServerScheduler();

    public Cleardrops() {
        // 配置注册与热重载监听
        CDConfig.register();
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        modBus.addListener(CDConfig::onLoad);
        modBus.addListener(CDConfig::onReload);

        // 仅服务端事件
        MinecraftForge.EVENT_BUS.register(CleardropsCommands.class);
        MinecraftForge.EVENT_BUS.addListener(SCHEDULER::onServerTick);

        // 启动时应用一次配置
        SCHEDULER.applyConfig();

        LOGGER.info("[Cleardrops] Initialized (Forge 1.20.1, server-only) with config hot-reload.");
    }
}