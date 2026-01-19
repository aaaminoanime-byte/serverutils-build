package com.serverutils.integration;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.ModList;

import java.lang.reflect.Method;


public class WorldEditIntegration {
    private static Boolean worldEditLoaded = null;
    private static Class<?> forgeAdapterClass = null;
    private static Class<?> worldEditClass = null;
    private static Class<?> blockVector3Class = null;
    private static Method adaptPlayerMethod = null;
    private static Method getInstanceMethod = null;
    private static Method getSessionManagerMethod = null;
    private static Method getSessionMethod = null;
    private static Method getWorldMethod = null;
    private static Method getSelectionMethod = null;
    private static Method getMinimumPointMethod = null;
    private static Method getMaximumPointMethod = null;
    private static Method getXMethod = null;
    private static Method getYMethod = null;
    private static Method getZMethod = null;

    
    public static boolean isWorldEditLoaded() {
        if (worldEditLoaded == null) {
            com.mojang.logging.LogUtils.getLogger().info("проверка загрузки WorldEdit...");
            worldEditLoaded = ModList.get().isLoaded("worldedit");
            com.mojang.logging.LogUtils.getLogger().info("WorldEdit загружен: {}", worldEditLoaded);

            if (worldEditLoaded) {
                try {
                    com.mojang.logging.LogUtils.getLogger().info("инициализация интеграции WorldEdit...");
                    initializeReflection();
                    com.mojang.logging.LogUtils.getLogger().info("интеграция WorldEdit успешна!");
                } catch (Exception e) {
                    com.mojang.logging.LogUtils.getLogger().error("ошибка инициализации WorldEdit: {}", e.getMessage(), e);
                    worldEditLoaded = false;
                }
            }
        }
        return worldEditLoaded;
    }

    private static void initializeReflection() throws Exception {
        forgeAdapterClass = Class.forName("com.sk89q.worldedit.forge.ForgeAdapter");
        worldEditClass = Class.forName("com.sk89q.worldedit.WorldEdit");
        blockVector3Class = Class.forName("com.sk89q.worldedit.math.BlockVector3");

        adaptPlayerMethod = forgeAdapterClass.getMethod("adaptPlayer", ServerPlayer.class);
        getInstanceMethod = worldEditClass.getMethod("getInstance");
        getSessionManagerMethod = worldEditClass.getMethod("getSessionManager");

        Class<?> sessionManagerClass = Class.forName("com.sk89q.worldedit.session.SessionManager");
        Class<?> wePlayerClass = Class.forName("com.sk89q.worldedit.entity.Player");
        Class<?> sessionOwnerClass = Class.forName("com.sk89q.worldedit.session.SessionOwner");
        Class<?> worldClass = Class.forName("com.sk89q.worldedit.world.World");

        getSessionMethod = sessionManagerClass.getMethod("get", sessionOwnerClass);
        getWorldMethod = wePlayerClass.getMethod("getWorld");

        Class<?> localSessionClass = Class.forName("com.sk89q.worldedit.LocalSession");
        getSelectionMethod = localSessionClass.getMethod("getSelection", worldClass);

        Class<?> regionClass = Class.forName("com.sk89q.worldedit.regions.Region");
        getMinimumPointMethod = regionClass.getMethod("getMinimumPoint");
        getMaximumPointMethod = regionClass.getMethod("getMaximumPoint");

        getXMethod = blockVector3Class.getMethod("getX");
        getYMethod = blockVector3Class.getMethod("getY");
        getZMethod = blockVector3Class.getMethod("getZ");
    }

    
    public static BlockPos[] getSelection(ServerPlayer player) {
        if (!isWorldEditLoaded()) {
            return null;
        }

        try {
            Object wePlayer = adaptPlayerMethod.invoke(null, player);
            Object worldEdit = getInstanceMethod.invoke(null);
            Object sessionManager = getSessionManagerMethod.invoke(worldEdit);
            Object session = getSessionMethod.invoke(sessionManager, wePlayer);
            Object world = getWorldMethod.invoke(wePlayer);
            Object region = getSelectionMethod.invoke(session, world);

            if (region == null) {
                return null;
            }

            Object minPoint = getMinimumPointMethod.invoke(region);
            Object maxPoint = getMaximumPointMethod.invoke(region);

            int minX = (int) getXMethod.invoke(minPoint);
            int minY = (int) getYMethod.invoke(minPoint);
            int minZ = (int) getZMethod.invoke(minPoint);

            int maxX = (int) getXMethod.invoke(maxPoint);
            int maxY = (int) getYMethod.invoke(maxPoint);
            int maxZ = (int) getZMethod.invoke(maxPoint);

            return new BlockPos[]{
                new BlockPos(minX, minY, minZ),
                new BlockPos(maxX, maxY, maxZ)
            };

        } catch (Exception e) {
            return null;
        }
    }
}
