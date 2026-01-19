package com.serverutils.events;

import com.serverutils.ServerUtilsMod;
import com.serverutils.data.RegionData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.*;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class RegionProtectionHandler {

    @SubscribeEvent
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        Player player = event.getPlayer();
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        String dimension = serverPlayer.level().dimension().location().toString();
        double x = event.getPos().getX();
        double y = event.getPos().getY();
        double z = event.getPos().getZ();

        for (RegionData region : ServerUtilsMod.getDataManager().getAllRegions()) {
            if (region.contains(dimension, x, y, z)) {
                if (!region.canBuild(serverPlayer.getUUID())) {
                    event.setCanceled(true);
                    serverPlayer.sendSystemMessage(Component.literal("§cВы не можете ломать блоки в регионе \"" + region.getName() + "\"!"));
                }
                return;
            }
        }
    }

    @SubscribeEvent
    public void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer)) {
            return;
        }

        String dimension = serverPlayer.level().dimension().location().toString();
        double x = event.getPos().getX();
        double y = event.getPos().getY();
        double z = event.getPos().getZ();

        for (RegionData region : ServerUtilsMod.getDataManager().getAllRegions()) {
            if (region.contains(dimension, x, y, z)) {
                if (!region.canBuild(serverPlayer.getUUID())) {
                    event.setCanceled(true);
                    serverPlayer.sendSystemMessage(Component.literal("§cВы не можете ставить блоки в регионе \"" + region.getName() + "\"!"));
                }
                return;
            }
        }
    }

    @SubscribeEvent
    public void onPvP(LivingAttackEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer victim)) {
            return;
        }

        if (!(event.getSource().getEntity() instanceof ServerPlayer attacker)) {
            return;
        }

        String dimension = victim.level().dimension().location().toString();
        double x = victim.getX();
        double y = victim.getY();
        double z = victim.getZ();

        for (RegionData region : ServerUtilsMod.getDataManager().getAllRegions()) {
            if (region.contains(dimension, x, y, z)) {
                if (!region.isPvpAllowed()) {
                    event.setCanceled(true);
                    attacker.sendSystemMessage(Component.literal("§cPvP запрещён в регионе \"" + region.getName() + "\"!"));
                }
                return;
            }
        }
    }

    @SubscribeEvent
    public void onBlockInteract(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer)) {
            return;
        }

        BlockPos pos = event.getPos();
        String dimension = serverPlayer.level().dimension().location().toString();
        double x = pos.getX();
        double y = pos.getY();
        double z = pos.getZ();

        var blockState = serverPlayer.level().getBlockState(pos);
        var block = blockState.getBlock();

        // проверка на блоки использования
        boolean isUseBlock = block instanceof DoorBlock ||
                             block instanceof TrapDoorBlock ||
                             block instanceof ButtonBlock ||
                             block instanceof LeverBlock ||
                             block instanceof FenceGateBlock ||
                             block instanceof CandleBlock ||
                             block instanceof CandleCakeBlock ||
                             block instanceof CakeBlock;

        // проверка на сундуки
        boolean isChestBlock = block instanceof ChestBlock ||
                               block instanceof BarrelBlock ||
                               block instanceof ShulkerBoxBlock ||
                               block instanceof EnderChestBlock;

        if (!isUseBlock && !isChestBlock) {
            return;
        }

        for (RegionData region : ServerUtilsMod.getDataManager().getAllRegions()) {
            if (region.contains(dimension, x, y, z)) {
                boolean denied = false;

                if (isChestBlock && !region.canOpenChests(serverPlayer.getUUID())) {
                    denied = true;
                } else if (isUseBlock && !region.canUse(serverPlayer.getUUID())) {
                    denied = true;
                }

                if (denied) {
                    event.setCanceled(true);

                    if (serverPlayer.level() instanceof ServerLevel serverLevel) {
                        serverLevel.sendParticles(
                            ParticleTypes.EXPLOSION_EMITTER,
                            pos.getX() + 0.5,
                            pos.getY() + 0.5,
                            pos.getZ() + 0.5,
                            1, // количество частиц
                            0.0, 0.0, 0.0, // смещение
                            0.0 // скорость
                        );
                    }

                    // красивое цветное сообщение
                    serverPlayer.sendSystemMessage(Component.literal("§c§l❗ §7Извините, Вы не можете использовать это в этом регионе"));
                }
                return;
            }
        }
    }
}
