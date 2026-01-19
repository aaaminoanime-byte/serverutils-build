package com.serverutils.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.serverutils.ServerUtilsMod;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.MessageArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

public class ReplyCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("r")
            .then(Commands.argument("message", MessageArgument.message())
                .executes(context -> reply(context, MessageArgument.getMessage(context, "message"))))
        );

        dispatcher.register(Commands.literal("к")
            .then(Commands.argument("message", MessageArgument.message())
                .executes(context -> reply(context, MessageArgument.getMessage(context, "message"))))
        );
    }

    private static int reply(CommandContext<CommandSourceStack> context, Component message) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer sender)) {
            context.getSource().sendFailure(Component.literal("Только игроки могут использовать эту команду!"));
            return 0;
        }

        UUID lastSenderUuid = ServerUtilsMod.getMessageTracker().getLastSender(sender.getUUID());
        if (lastSenderUuid == null) {
            sender.sendSystemMessage(Component.literal("§cНикто вам еще не писал!"));
            return 0;
        }

        ServerPlayer target = sender.getServer().getPlayerList().getPlayer(lastSenderUuid);
        if (target == null) {
            sender.sendSystemMessage(Component.literal("§cЭтот игрок не в сети!"));
            return 0;
        }

        String messageText = message.getString();

        target.sendSystemMessage(Component.literal("§7[§e" + sender.getName().getString() + " §7-> §eМне§7] §f" + messageText));
        sender.sendSystemMessage(Component.literal("§7[§eЯ §7-> §e" + target.getName().getString() + "§7] §f" + messageText));
        ServerUtilsMod.getMessageTracker().recordMessage(target.getUUID(), sender.getUUID());

        return 1;
    }
}
