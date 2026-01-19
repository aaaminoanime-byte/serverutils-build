package com.serverutils.events;

import com.serverutils.ServerUtilsMod;
import com.serverutils.data.RegionData;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class MobSpawnHandler {

    @SubscribeEvent
    public void onMobSpawn(MobSpawnEvent.FinalizeSpawn event) {
        if (ServerUtilsMod.getDataManager() == null) return;
        if (!(event.getLevel() instanceof net.minecraft.server.level.ServerLevel serverLevel)) return;

        String dimension = serverLevel.dimension().location().toString();
        double x = event.getX();
        double y = event.getY();
        double z = event.getZ();

        for (RegionData region : ServerUtilsMod.getDataManager().getAllRegions()) {
            if (region.contains(dimension, x, y, z)) {
                if (!region.isMobSpawnAllowed()) {
                    event.setResult(Event.Result.DENY);
                    event.setCanceled(true);
                }
                return;
            }
        }
    }
}
