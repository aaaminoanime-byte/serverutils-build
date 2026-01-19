package com.serverutils.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.serverutils.ServerUtilsMod;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

public class TpAcceptCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("tpaccept")
            .executes(TpAcceptCommand::acceptTpa)
        );

        dispatcher.register(Commands.literal("tpпринять")
            .executes(TpAcceptCommand::acceptTpa)
        );
    }

    private static int acceptTpa(CommandContext<CommandSourceStack> context) {
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
        if (requester == null) {
            target.sendSystemMessage(Component.literal("§cИгрок, отправивший запрос, не в сети!"));
            ServerUtilsMod.getDataManager().removeTpaRequest(target.getUUID());
            return 0;
        }

        requester.teleportTo(target.serverLevel(), target.getX(), target.getY(), target.getZ(), target.getYRot(), target.getXRot());
        requester.sendSystemMessage(Component.literal("§aТелепортация к игроку §e" + target.getName().getString() + " §aвыполнена!"));
        target.sendSystemMessage(Component.literal("§aВы приняли запрос от §e" + requester.getName().getString()));

        ServerUtilsMod.getDataManager().removeTpaRequest(target.getUUID());
        return 1;
    }
}
