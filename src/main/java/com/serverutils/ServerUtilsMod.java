package com.serverutils;

import com.mojang.logging.LogUtils;
import com.serverutils.commands.*;
import com.serverutils.config.ServerUtilsConfig;
import com.serverutils.data.DataManager;
import com.serverutils.data.MessageTracker;
import com.serverutils.data.RegionViewManager;
import com.serverutils.events.MobSpawnHandler;
import com.serverutils.events.RegionProtectionHandler;
import com.serverutils.events.RegionViewHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(ServerUtilsMod.MOD_ID)
public class ServerUtilsMod {
    public static final String MOD_ID = "serverutils";
    private static final Logger LOGGER = LogUtils.getLogger();
    private static DataManager dataManager;
    private static MessageTracker messageTracker;
    private static RegionViewManager regionViewManager;

    public ServerUtilsMod() {
        ServerUtilsConfig.register();
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::commonSetup);
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(new RegionProtectionHandler());
        MinecraftForge.EVENT_BUS.register(new MobSpawnHandler());
        MinecraftForge.EVENT_BUS.register(new RegionViewHandler());
        messageTracker = new MessageTracker();
        regionViewManager = new RegionViewManager();
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
    }

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        dataManager = new DataManager(event.getServer());
        dataManager.load();
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        if (dataManager != null) {
            dataManager.save();
        }
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        LOGGER.info("регистрация команд (версия 1.0.2)");
        HomeCommand.register(event.getDispatcher());
        SetHomeCommand.register(event.getDispatcher());
        WarpCommand.register(event.getDispatcher());
        RegionCommand.register(event.getDispatcher());
        TpaCommand.register(event.getDispatcher());
        TpAcceptCommand.register(event.getDispatcher());
        TpDenyCommand.register(event.getDispatcher());
        MessageCommand.register(event.getDispatcher());
        ReplyCommand.register(event.getDispatcher());
        SpawnCommand.register(event.getDispatcher());
        LOGGER.info("команды зарегистрированы");
    }

    public static DataManager getDataManager() {
        return dataManager;
    }

    public static MessageTracker getMessageTracker() {
        return messageTracker;
    }

    public static RegionViewManager getRegionViewManager() {
        return regionViewManager;
    }
}
