package com.serverutils.commands;
    
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.serverutils.ServerUtilsMod;
import com.serverutils.config.ServerUtilsConfig;
import com.serverutils.data.RegionData;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class RegionCommand {
    private static SuggestionProvider<CommandSourceStack> suggestRegions() {
        return (context, builder) -> SharedSuggestionProvider.suggest(
            ServerUtilsMod.getDataManager().getRegionNames(),
            builder
        );
    }

    private static SuggestionProvider<CommandSourceStack> suggestPlayerRegions() {
        return (context, builder) -> {
            if (context.getSource().getEntity() instanceof ServerPlayer player) {
                return SharedSuggestionProvider.suggest(
                    ServerUtilsMod.getDataManager().getAllRegions().stream()
                        .filter(region -> region.getOwner().equals(player.getUUID()))
                        .map(RegionData::getName)
                        .toList(),
                    builder
                );
            }
            return builder.buildFuture();
        };
    }

    private static SuggestionProvider<CommandSourceStack> suggestBuildModes() {
        return (context, builder) -> SharedSuggestionProvider.suggest(
            new String[]{"any", "members", "none"},
            builder
        );
    }

    private static SuggestionProvider<CommandSourceStack> suggestFlags() {
        return (context, builder) -> SharedSuggestionProvider.suggest(
            new String[]{"pvp", "mob_spawn", "build", "use", "chests"},
            builder
        );
    }

    private static SuggestionProvider<CommandSourceStack> suggestBooleans() {
        return (context, builder) -> SharedSuggestionProvider.suggest(
            new String[]{"true", "false"},
            builder
        );
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("rg")
            .then(Commands.literal("claim")
                .then(Commands.argument("name", StringArgumentType.word())
                    .executes(context -> claimRegion(context, StringArgumentType.getString(context, "name")))))
            .then(Commands.literal("delete")
                .then(Commands.argument("name", StringArgumentType.word())
                    .suggests(suggestPlayerRegions())
                    .executes(context -> deleteRegion(context, StringArgumentType.getString(context, "name")))))
            .then(Commands.literal("addmember")
                .then(Commands.argument("name", StringArgumentType.word())
                    .suggests(suggestPlayerRegions())
                    .then(Commands.argument("player", EntityArgument.player())
                        .executes(context -> addMember(
                            context,
                            StringArgumentType.getString(context, "name"),
                            EntityArgument.getPlayer(context, "player")
                        )))))
            .then(Commands.literal("addowner")
                .then(Commands.argument("name", StringArgumentType.word())
                    .suggests(suggestPlayerRegions())
                    .then(Commands.argument("player", EntityArgument.player())
                        .executes(context -> addOwner(
                            context,
                            StringArgumentType.getString(context, "name"),
                            EntityArgument.getPlayer(context, "player")
                        )))))
            .then(Commands.literal("deletemember")
                .then(Commands.argument("name", StringArgumentType.word())
                    .suggests(suggestPlayerRegions())
                    .then(Commands.argument("player", EntityArgument.player())
                        .executes(context -> removeMember(
                            context,
                            StringArgumentType.getString(context, "name"),
                            EntityArgument.getPlayer(context, "player")
                        )))))
            .then(Commands.literal("deleteowner")
                .then(Commands.argument("name", StringArgumentType.word())
                    .suggests(suggestPlayerRegions())
                    .then(Commands.argument("player", EntityArgument.player())
                        .executes(context -> removeOwner(
                            context,
                            StringArgumentType.getString(context, "name"),
                            EntityArgument.getPlayer(context, "player")
                        )))))
            .then(Commands.literal("flag")
                .then(Commands.argument("region", StringArgumentType.word())
                    .suggests(suggestPlayerRegions())
                    .then(Commands.argument("flag", StringArgumentType.word())
                        .suggests(suggestFlags())
                        .then(Commands.argument("value", StringArgumentType.word())
                            .suggests((context, builder) -> {
                                String flag = StringArgumentType.getString(context, "flag");
                                if (flag.equals("build") || flag.equals("use") || flag.equals("chests")) {
                                    return SharedSuggestionProvider.suggest(new String[]{"any", "members", "none"}, builder);
                                } else {
                                    return SharedSuggestionProvider.suggest(new String[]{"true", "false"}, builder);
                                }
                            })
                            .executes(context -> setFlag(
                                context,
                                StringArgumentType.getString(context, "region"),
                                StringArgumentType.getString(context, "flag"),
                                StringArgumentType.getString(context, "value")
                            ))))))
            .then(Commands.literal("view")
                .executes(RegionCommand::toggleView))
            .then(Commands.literal("list")
                .executes(RegionCommand::listRegions))
            .then(Commands.literal("info")
                .then(Commands.argument("name", StringArgumentType.word())
                    .suggests(suggestRegions())
                    .executes(context -> regionInfo(context, StringArgumentType.getString(context, "name")))))
        );
    }

    private static int claimRegion(CommandContext<CommandSourceStack> context, String name) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            context.getSource().sendFailure(Component.literal("Только игроки могут использовать эту команду!"));
            return 0;
        }

        com.mojang.logging.LogUtils.getLogger().info("command /rg claim called");

        if (ServerUtilsMod.getDataManager().getRegion(name) != null) {
            player.sendSystemMessage(Component.literal("§cРегион \"" + name + "\" уже существует!"));
            return 0;
        }

        var sel = com.serverutils.data.SelectionManager.get(player.getUUID());
        if (sel.p1 == null || sel.p2 == null) {
            player.sendSystemMessage(Component.literal("§cSelect two points with an axe: LMB = point 1, RMB = point 2"));
            return 0;
        }

        if (!sel.p1.dimension.equals(sel.p2.dimension)) {
            player.sendSystemMessage(Component.literal("§cPoints must be in the same dimension!"));
            return 0;
        }

        BlockPos pos1 = sel.p1.pos;
        BlockPos pos2 = sel.p2.pos;

        int sizeX = Math.abs(pos2.getX() - pos1.getX()) + 1;
        int sizeY = Math.abs(pos2.getY() - pos1.getY()) + 1;
        int sizeZ = Math.abs(pos2.getZ() - pos1.getZ()) + 1;
        long volume = (long) sizeX * sizeY * sizeZ;

        int maxX = ServerUtilsConfig.CONFIG.maxRegionSizeX.get();
        int maxY = ServerUtilsConfig.CONFIG.maxRegionSizeY.get();
        int maxZ = ServerUtilsConfig.CONFIG.maxRegionSizeZ.get();
        int maxVolume = ServerUtilsConfig.CONFIG.maxRegionVolume.get();

        if (!context.getSource().hasPermission(2)) {
            if (sizeX > maxX || sizeY > maxY || sizeZ > maxZ) {
                player.sendSystemMessage(Component.literal("§cРегион слишком большой! Размер: §e" +
                    sizeX + "x" + sizeY + "x" + sizeZ));
                player.sendSystemMessage(Component.literal("§cМаксимальный размер: §e" +
                    maxX + "x" + maxY + "x" + maxZ + "§c. Выберите меньшую область!"));
                return 0;
            }

            if (volume > maxVolume) {
                player.sendSystemMessage(Component.literal("§cРегион слишком большой! Объём: §e" +
                    volume + " блоков"));
                player.sendSystemMessage(Component.literal("§cМаксимальный объём: §e" +
                    maxVolume + " блоков§c. Выберите меньшую область!"));
                return 0;
            }
        }

        String dimension = player.level().dimension().location().toString();

        RegionData region = new RegionData(
            name,
            dimension,
            pos1.getX(), pos1.getY(), pos1.getZ(),
            pos2.getX(), pos2.getY(), pos2.getZ(),
            player.getUUID()
        );

        ServerUtilsMod.getDataManager().addRegion(name, region);

        player.sendSystemMessage(Component.literal("§aRegion \"§e" + name + "§a\" claimed! Size: §e" +
            sizeX + "x" + sizeY + "x" + sizeZ));
        return 1;
    }

    private static int deleteRegion(CommandContext<CommandSourceStack> context, String name) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            context.getSource().sendFailure(Component.literal("Только игроки могут использовать эту команду!"));
            return 0;
        }

        RegionData region = ServerUtilsMod.getDataManager().getRegion(name);
        if (region == null) {
            player.sendSystemMessage(Component.literal("§cРегион \"" + name + "\" не найден!"));
            return 0;
        }

        if (!region.getOwner().equals(player.getUUID()) && !context.getSource().hasPermission(2)) {
            player.sendSystemMessage(Component.literal("§cВы не владелец этого региона!"));
            return 0;
        }

        ServerUtilsMod.getDataManager().removeRegion(name);
        player.sendSystemMessage(Component.literal("§aРегион \"§e" + name + "§a\" удалён!"));
        return 1;
    }

    private static int addMember(CommandContext<CommandSourceStack> context, String name, ServerPlayer target) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            context.getSource().sendFailure(Component.literal("Только игроки могут использовать эту команду!"));
            return 0;
        }

        RegionData region = ServerUtilsMod.getDataManager().getRegion(name);
        if (region == null) {
            player.sendSystemMessage(Component.literal("§cРегион \"" + name + "\" не найден!"));
            return 0;
        }

        if (!region.isOwner(player.getUUID())) {
            player.sendSystemMessage(Component.literal("§cВы не владелец этого региона!"));
            return 0;
        }

        // Проверка: нельзя добавить самого себя
        if (target.getUUID().equals(player.getUUID())) {
            player.sendSystemMessage(Component.literal("§cВы не можете добавить самого себя!"));
            return 0;
        }

        // Проверка: нельзя добавить если уже является владельцем
        if (region.isOwner(target.getUUID())) {
            player.sendSystemMessage(Component.literal("§cИгрок §e" + target.getName().getString() +
                " §cуже является владельцем этого региона!"));
            return 0;
        }

        // Проверка: уже является участником
        if (region.getMembers().contains(target.getUUID())) {
            player.sendSystemMessage(Component.literal("§cИгрок §e" + target.getName().getString() +
                " §cуже является участником этого региона!"));
            return 0;
        }

        region.addMember(target.getUUID());
        ServerUtilsMod.getDataManager().addRegion(name, region);

        player.sendSystemMessage(Component.literal("§aИгрок §e" + target.getName().getString() +
            " §aдобавлен в участники региона \"§e" + name + "§a\"!"));
        target.sendSystemMessage(Component.literal("§aВы стали участником региона \"§e" + name + "§a\"!"));
        return 1;
    }

    private static int addOwner(CommandContext<CommandSourceStack> context, String name, ServerPlayer target) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            context.getSource().sendFailure(Component.literal("Только игроки могут использовать эту команду!"));
            return 0;
        }

        RegionData region = ServerUtilsMod.getDataManager().getRegion(name);
        if (region == null) {
            player.sendSystemMessage(Component.literal("§cРегион \"" + name + "\" не найден!"));
            return 0;
        }

        if (!region.isOwner(player.getUUID()) && !context.getSource().hasPermission(2)) {
            player.sendSystemMessage(Component.literal("§cВы не владелец этого региона!"));
            return 0;
        }

        // Проверка: нельзя добавить самого себя
        if (target.getUUID().equals(player.getUUID())) {
            player.sendSystemMessage(Component.literal("§cВы не можете добавить самого себя!"));
            return 0;
        }

        // Проверка: уже является владельцем
        if (region.isOwner(target.getUUID())) {
            player.sendSystemMessage(Component.literal("§cИгрок §e" + target.getName().getString() +
                " §cуже является владельцем этого региона!"));
            return 0;
        }

        region.addOwner(target.getUUID());
        ServerUtilsMod.getDataManager().addRegion(name, region);

        player.sendSystemMessage(Component.literal("§aИгрок §e" + target.getName().getString() +
            " §aдобавлен в совладельцы региона \"§e" + name + "§a\"!"));
        target.sendSystemMessage(Component.literal("§aВы стали совладельцем региона \"§e" + name + "§a\"!"));
        return 1;
    }

    private static int removeMember(CommandContext<CommandSourceStack> context, String name, ServerPlayer target) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            context.getSource().sendFailure(Component.literal("Только игроки могут использовать эту команду!"));
            return 0;
        }

        RegionData region = ServerUtilsMod.getDataManager().getRegion(name);
        if (region == null) {
            player.sendSystemMessage(Component.literal("§cРегион \"" + name + "\" не найден!"));
            return 0;
        }

        if (!region.getOwner().equals(player.getUUID())) {
            player.sendSystemMessage(Component.literal("§cВы не владелец этого региона!"));
            return 0;
        }

        region.removeMember(target.getUUID());
        ServerUtilsMod.getDataManager().addRegion(name, region);

        player.sendSystemMessage(Component.literal("§aИгрок §e" + target.getName().getString() +
            " §aудалён из участников региона \"§e" + name + "§a\"!"));
        target.sendSystemMessage(Component.literal("§cВы были удалены из участников региона \"§e" + name + "§c\"!"));
        return 1;
    }

    private static int removeOwner(CommandContext<CommandSourceStack> context, String name, ServerPlayer target) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            context.getSource().sendFailure(Component.literal("Только игроки могут использовать эту команду!"));
            return 0;
        }

        RegionData region = ServerUtilsMod.getDataManager().getRegion(name);
        if (region == null) {
            player.sendSystemMessage(Component.literal("§cРегион \"" + name + "\" не найден!"));
            return 0;
        }

        if (!region.isOwner(player.getUUID()) && !context.getSource().hasPermission(2)) {
            player.sendSystemMessage(Component.literal("§cВы не владелец этого региона!"));
            return 0;
        }

        region.removeOwner(target.getUUID());
        ServerUtilsMod.getDataManager().addRegion(name, region);

        player.sendSystemMessage(Component.literal("§aИгрок §e" + target.getName().getString() +
            " §aудалён из совладельцев региона \"§e" + name + "§a\"!"));
        target.sendSystemMessage(Component.literal("§cВы были удалены из совладельцев региона \"§e" + name + "§c\"!"));
        return 1;
    }

    private static int setFlag(CommandContext<CommandSourceStack> context, String regionName, String flag, String value) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            context.getSource().sendFailure(Component.literal("Только игроки могут использовать эту команду!"));
            return 0;
        }

        RegionData region = ServerUtilsMod.getDataManager().getRegion(regionName);
        if (region == null) {
            player.sendSystemMessage(Component.literal("§cРегион \"" + regionName + "\" не найден!"));
            return 0;
        }

        if (!region.getOwner().equals(player.getUUID()) && !context.getSource().hasPermission(2)) {
            player.sendSystemMessage(Component.literal("§cВы не владелец этого региона!"));
            return 0;
        }

        switch (flag.toLowerCase()) {
            case "pvp" -> {
                if (!value.equals("true") && !value.equals("false")) {
                    player.sendSystemMessage(Component.literal("§cИспользуйте true или false"));
                    return 0;
                }
                boolean enabled = Boolean.parseBoolean(value);
                region.setPvpAllowed(enabled);
                player.sendSystemMessage(Component.literal("§aФлаг PvP в регионе \"§e" + regionName + "§a\" установлен: " +
                    (enabled ? "§2true" : "§cfalse")));
            }
            case "mob_spawn" -> {
                if (!value.equals("true") && !value.equals("false")) {
                    player.sendSystemMessage(Component.literal("§cИспользуйте true или false"));
                    return 0;
                }
                boolean enabled = Boolean.parseBoolean(value);
                region.setMobSpawnAllowed(enabled);
                player.sendSystemMessage(Component.literal("§aФлаг mob_spawn в регионе \"§e" + regionName + "§a\" установлен: " +
                    (enabled ? "§2true" : "§cfalse")));
            }
            case "build" -> {
                if (!value.equals("any") && !value.equals("members") && !value.equals("none")) {
                    player.sendSystemMessage(Component.literal("§cИспользуйте: any, members или none"));
                    return 0;
                }
                region.setBuildMode(value);
                String modeText = switch (value) {
                    case "any" -> "Все игроки";
                    case "members" -> "Только участники";
                    case "none" -> "Только владельцы";
                    default -> value;
                };
                player.sendSystemMessage(Component.literal("§aФлаг build в регионе \"§e" + regionName + "§a\" установлен: §e" + modeText));
            }
            case "use" -> {
                if (!value.equals("any") && !value.equals("members") && !value.equals("none")) {
                    player.sendSystemMessage(Component.literal("§cИспользуйте: any, members или none"));
                    return 0;
                }
                region.setUseMode(value);
                String modeText = switch (value) {
                    case "any" -> "Все игроки";
                    case "members" -> "Только участники";
                    case "none" -> "Только владельцы";
                    default -> value;
                };
                player.sendSystemMessage(Component.literal("§aФлаг use в регионе \"§e" + regionName + "§a\" установлен: §e" + modeText));
            }
            case "chests" -> {
                if (!value.equals("any") && !value.equals("members") && !value.equals("none")) {
                    player.sendSystemMessage(Component.literal("§cИспользуйте: any, members или none"));
                    return 0;
                }
                region.setChestsMode(value);
                String modeText = switch (value) {
                    case "any" -> "Все игроки";
                    case "members" -> "Только участники";
                    case "none" -> "Только владельцы";
                    default -> value;
                };
                player.sendSystemMessage(Component.literal("§aФлаг chests в регионе \"§e" + regionName + "§a\" установлен: §e" + modeText));
            }
            default -> {
                player.sendSystemMessage(Component.literal("§cНеизвестный флаг! Используйте: pvp, mob_spawn, build, use, chests"));
                return 0;
            }
        }

        ServerUtilsMod.getDataManager().addRegion(regionName, region);
        return 1;
    }

    private static int setPvp(CommandContext<CommandSourceStack> context, String name, boolean enabled) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            context.getSource().sendFailure(Component.literal("Только игроки могут использовать эту команду!"));
            return 0;
        }

        RegionData region = ServerUtilsMod.getDataManager().getRegion(name);
        if (region == null) {
            player.sendSystemMessage(Component.literal("§cРегион \"" + name + "\" не найден!"));
            return 0;
        }

        if (!region.getOwner().equals(player.getUUID())) {
            player.sendSystemMessage(Component.literal("§cВы не владелец этого региона!"));
            return 0;
        }

        region.setPvpAllowed(enabled);
        ServerUtilsMod.getDataManager().addRegion(name, region);

        player.sendSystemMessage(Component.literal("§aPvP в регионе \"§e" + name + "§a\" " +
            (enabled ? "§2включен" : "§cвыключен") + "§a!"));
        return 1;
    }

    private static int setBuildMode(CommandContext<CommandSourceStack> context, String name, String mode) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            context.getSource().sendFailure(Component.literal("Только игроки могут использовать эту команду!"));
            return 0;
        }

        if (!mode.equals("any") && !mode.equals("members") && !mode.equals("none")) {
            player.sendSystemMessage(Component.literal("§cНеверный режим! Используйте: any, members, none"));
            return 0;
        }

        RegionData region = ServerUtilsMod.getDataManager().getRegion(name);
        if (region == null) {
            player.sendSystemMessage(Component.literal("§cРегион \"" + name + "\" не найден!"));
            return 0;
        }

        if (!region.getOwner().equals(player.getUUID())) {
            player.sendSystemMessage(Component.literal("§cВы не владелец этого региона!"));
            return 0;
        }

        region.setBuildMode(mode);
        ServerUtilsMod.getDataManager().addRegion(name, region);

        String modeText = switch (mode) {
            case "any" -> "Все игроки";
            case "members" -> "Только участники";
            case "none" -> "Только владелец";
            default -> mode;
        };

        player.sendSystemMessage(Component.literal("§aРежим строительства в регионе \"§e" + name +
            "§a\" изменён на: §e" + modeText));
        return 1;
    }

    private static int setMobSpawn(CommandContext<CommandSourceStack> context, String name, boolean enabled) {
        RegionData region = ServerUtilsMod.getDataManager().getRegion(name);
        if (region == null) {
            context.getSource().sendFailure(Component.literal("§cРегион \"" + name + "\" не найден!"));
            return 0;
        }

        region.setMobSpawnAllowed(enabled);
        ServerUtilsMod.getDataManager().addRegion(name, region);

        context.getSource().sendSuccess(() -> Component.literal("§aСпаун мобов в регионе \"§e" + name + "§a\" " +
            (enabled ? "§2включен" : "§cвыключен") + "§a!"), true);
        return 1;
    }

    private static int toggleView(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            context.getSource().sendFailure(Component.literal("Только игроки могут использовать эту команду!"));
            return 0;
        }

        boolean isViewing = ServerUtilsMod.getRegionViewManager().isViewing(player.getUUID());

        if (isViewing) {
            ServerUtilsMod.getRegionViewManager().disableView(player.getUUID());
            player.sendSystemMessage(Component.literal("§cРежим просмотра регионов выключен"));
        } else {
            ServerUtilsMod.getRegionViewManager().enableView(player.getUUID());
            player.sendSystemMessage(Component.literal("§aРежим просмотра регионов включен! Границы регионов будут видны разными цветами"));
        }

        return 1;
    }

    private static int listRegions(CommandContext<CommandSourceStack> context) {
        var regions = ServerUtilsMod.getDataManager().getRegionNames();

        if (regions.isEmpty()) {
            context.getSource().sendSuccess(() -> Component.literal("§eНет созданных регионов"), false);
        } else {
            context.getSource().sendSuccess(() -> Component.literal("§aРегионы: §e" + String.join(", ", regions)), false);
        }
        return 1;
    }

    private static int listOwners(CommandContext<CommandSourceStack> context, String regionName) {
        RegionData region = ServerUtilsMod.getDataManager().getRegion(regionName);
        if (region == null) {
            context.getSource().sendFailure(Component.literal("§cРегион \"" + regionName + "\" не найден!"));
            return 0;
        }

        context.getSource().sendSuccess(() -> Component.literal("§6=== Владельцы региона §e" + regionName + " §6==="), false);

        // Главный владелец
        var server = context.getSource().getServer();
        var mainOwner = server.getPlayerList().getPlayer(region.getOwner());
        String mainOwnerName = mainOwner != null ? mainOwner.getName().getString() : region.getOwner().toString();
        context.getSource().sendSuccess(() -> Component.literal("§aГлавный владелец: §e" + mainOwnerName), false);

        // Дополнительные владельцы
        var owners = region.getOwners();
        if (owners.isEmpty()) {
            context.getSource().sendSuccess(() -> Component.literal("§7Нет дополнительных владельцев"), false);
        } else {
            context.getSource().sendSuccess(() -> Component.literal("§aДополнительные владельцы (" + owners.size() + "):"), false);
            for (var ownerUuid : owners) {
                var ownerPlayer = server.getPlayerList().getPlayer(ownerUuid);
                String ownerName = ownerPlayer != null ? ownerPlayer.getName().getString() : ownerUuid.toString();
                context.getSource().sendSuccess(() -> Component.literal("§e- " + ownerName), false);
            }
        }
        return 1;
    }

    private static int listMembers(CommandContext<CommandSourceStack> context, String regionName) {
        RegionData region = ServerUtilsMod.getDataManager().getRegion(regionName);
        if (region == null) {
            context.getSource().sendFailure(Component.literal("§cРегион \"" + regionName + "\" не найден!"));
            return 0;
        }

        context.getSource().sendSuccess(() -> Component.literal("§6=== Участники региона §e" + regionName + " §6==="), false);

        var members = region.getMembers();
        if (members.isEmpty()) {
            context.getSource().sendSuccess(() -> Component.literal("§7Нет участников"), false);
        } else {
            context.getSource().sendSuccess(() -> Component.literal("§aУчастники (" + members.size() + "):"), false);
            var server = context.getSource().getServer();
            for (var memberUuid : members) {
                var memberPlayer = server.getPlayerList().getPlayer(memberUuid);
                String memberName = memberPlayer != null ? memberPlayer.getName().getString() : memberUuid.toString();
                context.getSource().sendSuccess(() -> Component.literal("§e- " + memberName), false);
            }
        }
        return 1;
    }

    private static int regionInfo(CommandContext<CommandSourceStack> context, String name) {
        RegionData region = ServerUtilsMod.getDataManager().getRegion(name);
        if (region == null) {
            context.getSource().sendFailure(Component.literal("§cРегион \"" + name + "\" не найден!"));
            return 0;
        }

        context.getSource().sendSuccess(() -> Component.literal("§6=== Информация о регионе §e" + name + " §6==="), false);
        context.getSource().sendSuccess(() -> Component.literal("§aИзмерение: §e" + region.getDimension()), false);
        context.getSource().sendSuccess(() -> Component.literal("§aКоординаты: §e" +
            String.format("(%.1f, %.1f, %.1f) - (%.1f, %.1f, %.1f)",
                region.getX1(), region.getY1(), region.getZ1(),
                region.getX2(), region.getY2(), region.getZ2())), false);

        String buildModeText = switch (region.getBuildMode()) {
            case "any" -> "Все игроки";
            case "members" -> "Только участники";
            case "none" -> "Только владелец";
            default -> region.getBuildMode();
        };

        context.getSource().sendSuccess(() -> Component.literal("§aРежим строительства: §e" + buildModeText), false);
        context.getSource().sendSuccess(() -> Component.literal("§aPvP: " +
            (region.isPvpAllowed() ? "§2Включен" : "§cВыключен")), false);
        context.getSource().sendSuccess(() -> Component.literal("§aСпаун мобов: " +
            (region.isMobSpawnAllowed() ? "§2Включен" : "§cВыключен")), false);
        context.getSource().sendSuccess(() -> Component.literal("§aУчастников: §e" + region.getMembers().size()), false);

        return 1;
    }
}
