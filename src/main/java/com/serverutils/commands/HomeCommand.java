package com.serverutils.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.serverutils.ServerUtilsMod;
import com.serverutils.config.ServerUtilsConfig;
import com.serverutils.data.HomeData;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

public class HomeCommand {
    private static SuggestionProvider<CommandSourceStack> suggestPlayerHomes() {
        return (context, builder) -> {
            if (context.getSource().getEntity() instanceof ServerPlayer player) {
                return SharedSuggestionProvider.suggest(
                    ServerUtilsMod.getDataManager().getHomeNames(player.getUUID()),
                    builder
                );
            }
            return builder.buildFuture();
        };
    }

    private static SuggestionProvider<CommandSourceStack> suggestAccessibleHomes() {
        return (context, builder) -> {
            if (context.getSource().getEntity() instanceof ServerPlayer player) {
                List<String> accessible = new ArrayList<>();
                accessible.addAll(ServerUtilsMod.getDataManager().getHomeNames(player.getUUID()));
                ServerUtilsMod.getDataManager().getAllHomes().forEach((ownerUuid, homes) -> {
                    homes.forEach((name, home) -> {
                        if (home.getInvitedPlayers().contains(player.getUUID())) {
                            accessible.add(name);
                        }
                    });
                });
                return SharedSuggestionProvider.suggest(accessible, builder);
            }
            return builder.buildFuture();
        };
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("home")
            .then(Commands.literal("create")
                .then(Commands.argument("name", StringArgumentType.word())
                    .executes(context -> createHome(context, StringArgumentType.getString(context, "name")))))
            .then(Commands.literal("tp")
                .then(Commands.argument("name", StringArgumentType.word())
                    .suggests(suggestAccessibleHomes())
                    .executes(context -> teleportHome(context, StringArgumentType.getString(context, "name")))))
            .then(Commands.literal("delete")
                .then(Commands.argument("name", StringArgumentType.word())
                    .suggests(suggestPlayerHomes())
                    .executes(context -> deleteHome(context, StringArgumentType.getString(context, "name")))))
            .then(Commands.literal("invite")
                .then(Commands.argument("home", StringArgumentType.word())
                    .suggests(suggestPlayerHomes())
                    .then(Commands.argument("player", EntityArgument.player())
                        .executes(context -> invitePlayer(
                            context,
                            StringArgumentType.getString(context, "home"),
                            EntityArgument.getPlayer(context, "player")
                        )))))
            .then(Commands.literal("uninvite")
                .then(Commands.argument("home", StringArgumentType.word())
                    .suggests(suggestPlayerHomes())
                    .then(Commands.argument("player", EntityArgument.player())
                        .executes(context -> uninvitePlayer(
                            context,
                            StringArgumentType.getString(context, "home"),
                            EntityArgument.getPlayer(context, "player")
                        )))))
            .then(Commands.literal("info")
                .then(Commands.argument("name", StringArgumentType.word())
                    .suggests(suggestPlayerHomes())
                    .executes(context -> homeInfo(context, StringArgumentType.getString(context, "name")))))
            .then(Commands.literal("list")
                .executes(HomeCommand::listHomes))
            .then(Commands.argument("name", StringArgumentType.word())
                .suggests(suggestAccessibleHomes())
                .executes(context -> teleportHome(context, StringArgumentType.getString(context, "name"))))
        );
    }

    private static int createHome(CommandContext<CommandSourceStack> context, String homeName) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            context.getSource().sendFailure(Component.literal("Только игроки могут использовать эту команду!"));
            return 0;
        }

        int currentHomes = ServerUtilsMod.getDataManager().getHomeNames(player.getUUID()).size();
        int maxHomes = ServerUtilsConfig.CONFIG.maxHomes.get();

        if (currentHomes >= maxHomes && !context.getSource().hasPermission(2)) {
            player.sendSystemMessage(Component.literal("§cВы достигли максимального количества домов! (" + maxHomes + ")"));
            return 0;
        }

        if (ServerUtilsMod.getDataManager().getHome(player.getUUID(), homeName) != null) {
            player.sendSystemMessage(Component.literal("§cДом \"" + homeName + "\" уже существует!"));
            return 0;
        }

        String dimension = player.level().dimension().location().toString();
        HomeData home = new HomeData(
            dimension,
            player.getX(),
            player.getY(),
            player.getZ(),
            player.getYRot(),
            player.getXRot(),
            player.getUUID()
        );

        ServerUtilsMod.getDataManager().addHome(player.getUUID(), homeName, home);
        player.sendSystemMessage(Component.literal("§aДом \"§e" + homeName + "§a\" создан! (" + (currentHomes + 1) + "/" + maxHomes + ")"));
        return 1;
    }

    private static int deleteHome(CommandContext<CommandSourceStack> context, String homeName) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            context.getSource().sendFailure(Component.literal("Только игроки могут использовать эту команду!"));
            return 0;
        }

        if (ServerUtilsMod.getDataManager().getHome(player.getUUID(), homeName) == null) {
            player.sendSystemMessage(Component.literal("§cДом \"" + homeName + "\" не найден!"));
            return 0;
        }

        ServerUtilsMod.getDataManager().removeHome(player.getUUID(), homeName);
        player.sendSystemMessage(Component.literal("§aДом \"§e" + homeName + "§a\" удалён!"));
        return 1;
    }

    private static int teleportHome(CommandContext<CommandSourceStack> context, String homeName) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            context.getSource().sendFailure(Component.literal("Только игроки могут использовать эту команду!"));
            return 0;
        }

        HomeData home = ServerUtilsMod.getDataManager().getHome(player.getUUID(), homeName);

        if (home == null) {
            for (var entry : ServerUtilsMod.getDataManager().getAllHomes().entrySet()) {
                HomeData foundHome = entry.getValue().get(homeName);
                if (foundHome != null && foundHome.canAccess(player.getUUID())) {
                    home = foundHome;
                    break;
                }
            }
        }

        if (home == null) {
            player.sendSystemMessage(Component.literal("§cДом \"" + homeName + "\" не найден или у вас нет доступа!"));
            return 0;
        }

        ResourceLocation dimensionLocation = ResourceLocation.tryParse(home.getDimension());
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

        player.teleportTo(level, home.getX(), home.getY(), home.getZ(), home.getYaw(), home.getPitch());
        player.sendSystemMessage(Component.literal("§aТелепортировано домой: §e" + homeName));
        return 1;
    }

    private static int invitePlayer(CommandContext<CommandSourceStack> context, String homeName, ServerPlayer target) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            context.getSource().sendFailure(Component.literal("Только игроки могут использовать эту команду!"));
            return 0;
        }

        HomeData home = ServerUtilsMod.getDataManager().getHome(player.getUUID(), homeName);
        if (home == null) {
            player.sendSystemMessage(Component.literal("§cДом \"" + homeName + "\" не найден!"));
            return 0;
        }

        if (home.getInvitedPlayers().contains(target.getUUID())) {
            player.sendSystemMessage(Component.literal("§cЭтот игрок уже приглашён в дом!"));
            return 0;
        }

        home.invitePlayer(target.getUUID());
        ServerUtilsMod.getDataManager().addHome(player.getUUID(), homeName, home);

        player.sendSystemMessage(Component.literal("§aИгрок §e" + target.getName().getString() + " §aприглашён в дом \"§e" + homeName + "§a\"!"));
        target.sendSystemMessage(Component.literal("§aВы были приглашены в дом \"§e" + homeName + "§a\" игрока §e" + player.getName().getString()));
        return 1;
    }

    private static int uninvitePlayer(CommandContext<CommandSourceStack> context, String homeName, ServerPlayer target) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            context.getSource().sendFailure(Component.literal("Только игроки могут использовать эту команду!"));
            return 0;
        }

        HomeData home = ServerUtilsMod.getDataManager().getHome(player.getUUID(), homeName);
        if (home == null) {
            player.sendSystemMessage(Component.literal("§cДом \"" + homeName + "\" не найден!"));
            return 0;
        }

        home.removeInvite(target.getUUID());
        ServerUtilsMod.getDataManager().addHome(player.getUUID(), homeName, home);

        player.sendSystemMessage(Component.literal("§aИгрок §e" + target.getName().getString() + " §aудалён из дома \"§e" + homeName + "§a\"!"));
        target.sendSystemMessage(Component.literal("§cВы были удалены из дома \"§e" + homeName + "§c\""));
        return 1;
    }

    private static int homeInfo(CommandContext<CommandSourceStack> context, String homeName) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            context.getSource().sendFailure(Component.literal("Только игроки могут использовать эту команду!"));
            return 0;
        }

        HomeData home = ServerUtilsMod.getDataManager().getHome(player.getUUID(), homeName);
        if (home == null) {
            player.sendSystemMessage(Component.literal("§cДом \"" + homeName + "\" не найден!"));
            return 0;
        }

        player.sendSystemMessage(Component.literal("§6=== Информация о доме §e" + homeName + " §6==="));
        player.sendSystemMessage(Component.literal("§aИзмерение: §e" + home.getDimension()));
        player.sendSystemMessage(Component.literal("§aКоординаты: §e" +
            String.format("%.1f, %.1f, %.1f", home.getX(), home.getY(), home.getZ())));
        player.sendSystemMessage(Component.literal("§aПриглашено игроков: §e" + home.getInvitedPlayers().size()));

        return 1;
    }

    private static int listHomes(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            context.getSource().sendFailure(Component.literal("Только игроки могут использовать эту команду!"));
            return 0;
        }

        var homes = ServerUtilsMod.getDataManager().getHomeNames(player.getUUID());
        int maxHomes = ServerUtilsConfig.CONFIG.maxHomes.get();

        if (homes.isEmpty()) {
            player.sendSystemMessage(Component.literal("§eУ вас нет сохранённых домов (0/" + maxHomes + ")"));
        } else {
            player.sendSystemMessage(Component.literal("§aВаши дома (" + homes.size() + "/" + maxHomes + "): §e" + String.join(", ", homes)));
        }
        return 1;
    }
}
