package com.serverutils.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.MessageArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class MessageCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("m")
            .then(Commands.argument("player", EntityArgument.player())
                .then(Commands.argument("message", MessageArgument.message())
                    .executes(context -> sendMessage(
                        context,
                        EntityArgument.getPlayer(context, "player"),
                        MessageArgument.getMessage(context, "message")
                    ))))
        );

        dispatcher.register(Commands.literal("ь")
            .then(Commands.argument("player", EntityArgument.player())
                .then(Commands.argument("message", MessageArgument.message())
                    .executes(context -> sendMessage(
                        context,
                        EntityArgument.getPlayer(context, "player"),
                        MessageArgument.getMessage(context, "message")
                    ))))
        );

        dispatcher.register(Commands.literal("сообщение")
            .then(Commands.argument("player", EntityArgument.player())
                .then(Commands.argument("message", MessageArgument.message())
                    .executes(context -> sendMessage(
                        context,
                        EntityArgument.getPlayer(context, "player"),
                        MessageArgument.getMessage(context, "message")
                    ))))
        );

        dispatcher.register(Commands.literal("msg")
            .then(Commands.argument("player", EntityArgument.player())
                .then(Commands.argument("message", MessageArgument.message())
                    .executes(context -> sendMessage(
                        context,
                        EntityArgument.getPlayer(context, "player"),
                        MessageArgument.getMessage(context, "message")
                    ))))
        );

        dispatcher.register(Commands.literal("tell")
            .then(Commands.argument("player", EntityArgument.player())
                .then(Commands.argument("message", MessageArgument.message())
                    .executes(context -> sendMessage(
                        context,
                        EntityArgument.getPlayer(context, "player"),
                        MessageArgument.getMessage(context, "message")
                    ))))
        );
    }

    private static int sendMessage(CommandContext<CommandSourceStack> context, ServerPlayer target, Component message) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer sender)) {
            context.getSource().sendFailure(Component.literal("Только игроки могут использовать эту команду!"));
            return 0;
        }

        if (sender.getUUID().equals(target.getUUID())) {
            sender.sendSystemMessage(Component.literal("§cВы не можете отправить сообщение самому себе!"));
            return 0;
        }

        String messageText = message.getString();

        target.sendSystemMessage(Component.literal("§7[§e" + sender.getName().getString() + " §7-> §eМне§7] §f" + messageText));
        sender.sendSystemMessage(Component.literal("§7[§eЯ §7-> §e" + target.getName().getString() + "§7] §f" + messageText));
        com.serverutils.ServerUtilsMod.getMessageTracker().recordMessage(target.getUUID(), sender.getUUID());

        return 1;
    }
}
