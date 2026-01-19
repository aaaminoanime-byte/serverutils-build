package com.serverutils.events;

import com.serverutils.ServerUtilsMod;
import com.serverutils.data.RegionData;
import com.serverutils.data.SelectionManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ServerUtilsMod.MOD_ID)
public class SelectionToolsHandler {

    private static boolean isSelectionAxe(ItemStack stack) {
        // Allow any axe (wooden, stone, iron, etc.) as a selection tool.
        return stack.getItem() instanceof AxeItem;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        ItemStack stack = player.getMainHandItem();
        if (!isSelectionAxe(stack)) return;

        BlockPos pos = event.getPos();
        var sel = SelectionManager.get(player.getUUID());
        sel.p1 = new SelectionManager.Point(player.level().dimension(), pos);

        player.sendSystemMessage(
            Component.literal("Point 1: " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ())
                .withStyle(ChatFormatting.AQUA)
        );

        // Prevent block damage when using the selection axe.
        event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide()) return;
        if (event.getHand() != InteractionHand.MAIN_HAND) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        ItemStack stack = player.getMainHandItem();

        // 1) Region inspect tool: stick
        if (stack.is(Items.STICK)) {
            BlockPos pos = event.getPos();
            String dimension = player.level().dimension().location().toString();

            RegionData found = null;
            for (RegionData region : ServerUtilsMod.getDataManager().getAllRegions()) {
                if (region.contains(dimension, pos.getX(), pos.getY(), pos.getZ())) {
                    found = region;
                    break;
                }
            }

            if (found != null) {
                player.sendSystemMessage(
                    Component.literal("Region: " + found.getName()).withStyle(ChatFormatting.GOLD)
                );
            } else {
                player.sendSystemMessage(
                    Component.literal("Region: none").withStyle(ChatFormatting.GRAY)
                );
            }

            // Do not cancel: allow normal interactions if permitted.
            return;
        }

        // 2) Selection axe: set point 2
        if (!isSelectionAxe(stack)) return;

        BlockPos pos = event.getPos();
        var sel = SelectionManager.get(player.getUUID());
        sel.p2 = new SelectionManager.Point(player.level().dimension(), pos);

        player.sendSystemMessage(
            Component.literal("Point 2: " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ())
                .withStyle(ChatFormatting.GREEN)
        );

        event.setCanceled(true);
        event.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);
    }
}
