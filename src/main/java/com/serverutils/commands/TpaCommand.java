package com.serverutils.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.serverutils.ServerUtilsMod;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;

public class TpaCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("tpa")
            .then(Commands.argument("player", EntityArgument.player())
                .executes(context -> sendTpaRequest(context, EntityArgument.getPlayer(context, "player"))))
        );
    }

    private static int sendTpaRequest(CommandContext<CommandSourceStack> context, ServerPlayer target) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer requester)) {
            context.getSource().sendFailure(Component.literal("Только игроки могут использовать эту команду!"));
            return 0;
        }

        if (requester.getUUID().equals(target.getUUID())) {
            requester.sendSystemMessage(Component.literal("§cВы не можете отправить запрос самому себе!"));
            return 0;
        }

        ServerUtilsMod.getDataManager().addTpaRequest(requester.getUUID(), target.getUUID());

        requester.sendSystemMessage(Component.literal("§aЗапрос на телепортацию отправлен игроку §e" + target.getName().getString()));

        Component acceptButton = Component.literal("§a[Принять]")
            .setStyle(Style.EMPTY
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tpaccept"))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Нажмите, чтобы принять"))));

        Component denyButton = Component.literal(" §c[Отклонить]")
            .setStyle(Style.EMPTY
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tpdeny"))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Нажмите, чтобы отклонить"))));

        target.sendSystemMessage(Component.literal("§e" + requester.getName().getString() + " §aхочет телепортироваться к вам. "));
        target.sendSystemMessage(acceptButton.copy().append(denyButton));

        return 1;
    }
}
