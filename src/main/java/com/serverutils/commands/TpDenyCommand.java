package com.serverutils.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.serverutils.ServerUtilsMod;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

public class TpDenyCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("tpdeny")
            .executes(TpDenyCommand::denyTpa)
        );

        dispatcher.register(Commands.literal("tpотклонить")
            .executes(TpDenyCommand::denyTpa)
        );
    }

    private static int denyTpa(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer target)) {
            context.getSource().sendFailure(Component.literal("Только игроки могут использовать эту команду!"));
            return 0;
        }

        UUID requesterUuid = ServerUtilsMod.getDataManager().getTpaRequest(target.getUUID());
        if (requesterUuid == null) {
            target.sendSystemMessage(Component.literal("§cУ вас нет активных запросов на телепортацию!"));
            return 0;
        }

        ServerPlayer requester = target.getServer().getPlayerList().getPlayer(requesterUuid);
        if (requester != null) {
            requester.sendSystemMessage(Component.literal("§c" + target.getName().getString() + " отклонил ваш запрос на телепортацию"));
        }

        target.sendSystemMessage(Component.literal("§cВы отклонили запрос на телепортацию"));
        ServerUtilsMod.getDataManager().removeTpaRequest(target.getUUID());
        return 1;
    }
}
