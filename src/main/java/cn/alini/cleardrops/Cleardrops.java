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
        CDConfig.register();
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        modBus.addListener(CDConfig::onLoad);
        modBus.addListener(CDConfig::onReload);

        MinecraftForge.EVENT_BUS.register(CleardropsCommands.class);
        MinecraftForge.EVENT_BUS.addListener(SCHEDULER::onServerTick);
        MinecraftForge.EVENT_BUS.addListener(SCHEDULER::onEntityJoinLevel);
        MinecraftForge.EVENT_BUS.addListener(SCHEDULER::onEntityLeaveLevel);

        SCHEDULER.applyConfig();
        LOGGER.info("[Cleardrops] Server-only mod initialized (Forge 1.20.1) with config hot-reload.");
    }
}