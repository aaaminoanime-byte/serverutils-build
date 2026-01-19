package com.serverutils.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.serverutils.ServerUtilsMod;
import com.serverutils.data.WarpData;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

public class WarpCommand {
    private static SuggestionProvider<CommandSourceStack> suggestWarps() {
        return (context, builder) -> SharedSuggestionProvider.suggest(
            ServerUtilsMod.getDataManager().getWarpNames(),
            builder
        );
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("warp")
            .then(Commands.literal("list")
                .executes(WarpCommand::listWarps))
            .then(Commands.literal("create")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("name", StringArgumentType.word())
                    .executes(context -> createWarp(context, StringArgumentType.getString(context, "name")))))
            .then(Commands.literal("delete")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("name", StringArgumentType.word())
                    .suggests(suggestWarps())
                    .executes(context -> deleteWarp(context, StringArgumentType.getString(context, "name")))))
            .then(Commands.argument("name", StringArgumentType.word())
                .suggests(suggestWarps())
                .executes(context -> teleportWarp(context, StringArgumentType.getString(context, "name"))))
        );
    }

    private static int listWarps(CommandContext<CommandSourceStack> context) {
        var warps = ServerUtilsMod.getDataManager().getWarpNames();

        if (warps.isEmpty()) {
            context.getSource().sendSuccess(() -> Component.literal("§eНет доступных варпов"), false);
        } else {
            context.getSource().sendSuccess(() -> Component.literal("§aДоступные варпы: §e" + String.join(", ", warps)), false);
        }
        return 1;
    }

    private static int createWarp(CommandContext<CommandSourceStack> context, String warpName) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            context.getSource().sendFailure(Component.literal("Только игроки могут использовать эту команду!"));
            return 0;
        }

        if (ServerUtilsMod.getDataManager().getWarp(warpName) != null) {
            player.sendSystemMessage(Component.literal("§cВарп \"" + warpName + "\" уже существует!"));
            return 0;
        }

        String dimension = player.level().dimension().location().toString();
        WarpData warp = new WarpData(
            dimension,
            player.getX(),
            player.getY(),
            player.getZ(),
            player.getYRot(),
            player.getXRot()
        );

        ServerUtilsMod.getDataManager().addWarp(warpName, warp);
        player.sendSystemMessage(Component.literal("§aВарп \"§e" + warpName + "§a\" создан!"));
        return 1;
    }

    private static int deleteWarp(CommandContext<CommandSourceStack> context, String warpName) {
        if (ServerUtilsMod.getDataManager().getWarp(warpName) == null) {
            context.getSource().sendFailure(Component.literal("§cВарп \"" + warpName + "\" не найден!"));
            return 0;
        }

        ServerUtilsMod.getDataManager().removeWarp(warpName);
        context.getSource().sendSuccess(() -> Component.literal("§aВарп \"§e" + warpName + "§a\" удалён!"), true);
        return 1;
    }

    private static int teleportWarp(CommandContext<CommandSourceStack> context, String warpName) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            context.getSource().sendFailure(Component.literal("Только игроки могут использовать эту команду!"));
            return 0;
        }

        WarpData warp = ServerUtilsMod.getDataManager().getWarp(warpName);
        if (warp == null) {
            player.sendSystemMessage(Component.literal("§cВарп \"" + warpName + "\" не найден!"));
            return 0;
        }

        ResourceLocation dimensionLocation = ResourceLocation.tryParse(warp.getDimension());
        if (dimensionLocation == null) {
            player.sendSystemMessage(Component.literal("§cНеверное измерение!"));
            return 0;
        }
        ResourceKey<Level> dimensionKey = ResourceKey.create(Registries.DIMENSION, dimensionLocation);
        ServerLevel level = player.getServer().getLevel(dimensionKey);

        if (level == null) {
            player.sendSystemMessage(Component.literal("§cИзмерение не найдено!"));
            return 0;
        }

        player.teleportTo(level, warp.getX(), warp.getY(), warp.getZ(), warp.getYaw(), warp.getPitch());
        player.sendSystemMessage(Component.literal("§aТелепортировано на варп: §e" + warpName));
        return 1;
    }
}
