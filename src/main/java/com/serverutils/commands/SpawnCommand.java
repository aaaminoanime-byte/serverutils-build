package com.serverutils.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.serverutils.ServerUtilsMod;
import com.serverutils.data.SpawnData;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

public class SpawnCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("setspawn")
            .requires(source -> source.hasPermission(2))
            .executes(context -> {
                if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
                    context.getSource().sendSystemMessage(Component.literal("§cЭту команду может использовать только игрок!"));
                    return 0;
                }

                String dimension = player.level().dimension().location().toString();
                double x = player.getX();
                double y = player.getY();
                double z = player.getZ();
                float yaw = player.getYRot();
                float pitch = player.getXRot();

                SpawnData spawnData = new SpawnData(dimension, x, y, z, yaw, pitch);
                ServerUtilsMod.getDataManager().setSpawn(spawnData);

                player.sendSystemMessage(Component.literal("§aТочка спавна установлена: §e" +
                    String.format("%.1f, %.1f, %.1f §aв измерении §e%s", x, y, z, dimension)));
                return 1;
            }));

        dispatcher.register(Commands.literal("spawn")
            .executes(context -> {
                if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
                    context.getSource().sendSystemMessage(Component.literal("§cЭту команду может использовать только игрок!"));
                    return 0;
                }

                SpawnData spawn = ServerUtilsMod.getDataManager().getSpawn();
                if (spawn == null) {
                    player.sendSystemMessage(Component.literal("§cТочка спавна не установлена! Попросите оператора установить её с помощью /setspawn"));
                    return 0;
                }

                ResourceLocation dimensionLocation = ResourceLocation.tryParse(spawn.getDimension());
                if (dimensionLocation == null) {
                    player.sendSystemMessage(Component.literal("§cНеверное измерение!"));
                    return 0;
                }
                ResourceKey<Level> dimensionKey = ResourceKey.create(Registries.DIMENSION, dimensionLocation);
                ServerLevel targetLevel = player.getServer().getLevel(dimensionKey);

                if (targetLevel == null) {
                    player.sendSystemMessage(Component.literal("§cИзмерение спавна не найдено!"));
                    return 0;
                }

                player.teleportTo(targetLevel, spawn.getX(), spawn.getY(), spawn.getZ(), spawn.getYaw(), spawn.getPitch());
                player.sendSystemMessage(Component.literal("§aВы телепортированы на спавн!"));
                return 1;
            }));
    }
}
