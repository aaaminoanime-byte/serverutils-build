package com.serverutils.data;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stores two selection points per player (server-side).
 */
public final class SelectionManager {

    public static final class Point {
        public final ResourceKey<Level> dimension;
        public final BlockPos pos;

        public Point(ResourceKey<Level> dimension, BlockPos pos) {
            this.dimension = dimension;
            this.pos = pos.immutable();
        }
    }

    public static final class Selection {
        public Point p1;
        public Point p2;
    }

    private static final Map<UUID, Selection> SELECTIONS = new ConcurrentHashMap<>();

    private SelectionManager() {}

    public static Selection get(UUID uuid) {
        return SELECTIONS.computeIfAbsent(uuid, u -> new Selection());
    }

    public static void clear(UUID uuid) {
        SELECTIONS.remove(uuid);
    }
}
