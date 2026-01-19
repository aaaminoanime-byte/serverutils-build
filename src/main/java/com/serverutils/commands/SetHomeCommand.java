package com.serverutils.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.serverutils.ServerUtilsMod;
import com.serverutils.config.ServerUtilsConfig;
import com.serverutils.data.HomeData;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class SetHomeCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("sethome")
            .then(Commands.argument("name", StringArgumentType.word())
                .executes(context -> setHome(context, StringArgumentType.getString(context, "name"))))
        );
    }

    private static int setHome(CommandContext<CommandSourceStack> context, String name) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            context.getSource().sendFailure(Component.literal("Только игроки могут использовать эту команду!"));
            return 0;
        }

        int maxHomes = ServerUtilsConfig.CONFIG.maxHomes.get();
        int currentHomes = ServerUtilsMod.getDataManager().getHomeNames(player.getUUID()).size();

        if (currentHomes >= maxHomes && ServerUtilsMod.getDataManager().getHome(player.getUUID(), name) == null) {
            player.sendSystemMessage(Component.literal("§cВы достигли максимального количества домов (§e" + maxHomes + "§c)!"));
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

        ServerUtilsMod.getDataManager().addHome(player.getUUID(), name, home);
        player.sendSystemMessage(Component.literal("§aДом \"§e" + name + "§a\" установлен!"));
        return 1;
    }
}
