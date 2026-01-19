package com.serverutils.events;

import com.serverutils.ServerUtilsMod;
import com.serverutils.data.RegionData;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.List;

public class RegionViewHandler {
    private int tickCounter = 0;

    // Массив разных частиц для визуального различия регионов
    private static final ParticleOptions[] PARTICLE_COLORS = {
        ParticleTypes.FLAME,           // Красный/оранжевый
        ParticleTypes.SOUL_FIRE_FLAME, // Синий
        ParticleTypes.END_ROD,         // Белый
        ParticleTypes.ELECTRIC_SPARK,  // Желтый
        ParticleTypes.WAX_ON,          // Зеленоватый
        ParticleTypes.SCRAPE,          // Коричневый
        ParticleTypes.CHERRY_LEAVES,   // Розовый
        ParticleTypes.GLOW,            // Светящийся
    };

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        // Отрисовываем частицы каждые 5 тиков (4 раза в секунду)
        tickCounter++;
        if (tickCounter < 5) return;
        tickCounter = 0;

        if (ServerUtilsMod.getDataManager() == null) return;
        if (ServerUtilsMod.getRegionViewManager() == null) return;

        // Получаем список регионов
        List<RegionData> regions = new ArrayList<>(ServerUtilsMod.getDataManager().getAllRegions());

        // Для каждого игрока с включенным режимом просмотра
        for (var playerUuid : ServerUtilsMod.getRegionViewManager().getViewingPlayers()) {
            ServerPlayer player = event.getServer().getPlayerList().getPlayer(playerUuid);
            if (player == null) continue;

            ServerLevel level = player.serverLevel();
            String dimension = level.dimension().location().toString();
            double playerX = player.getX();
            double playerY = player.getY();
            double playerZ = player.getZ();
            double renderDistance = 64.0;

            // Отрисовываем каждый регион своим цветом
            for (int i = 0; i < regions.size(); i++) {
                RegionData region = regions.get(i);

                // Пропускаем регионы из других измерений
                if (!region.getDimension().equals(dimension)) continue;

                // Выбираем цвет частиц для региона (циклически)
                ParticleOptions particleType = PARTICLE_COLORS[i % PARTICLE_COLORS.length];

                // Границы региона
                double x1 = region.getX1();
                double y1 = region.getY1();
                double z1 = region.getZ1();
                double x2 = region.getX2();
                double y2 = region.getY2();
                double z2 = region.getZ2();

                // Проверяем, находится ли регион в зоне видимости
                if (Math.abs(x1 - playerX) > renderDistance && Math.abs(x2 - playerX) > renderDistance) continue;
                if (Math.abs(z1 - playerZ) > renderDistance && Math.abs(z2 - playerZ) > renderDistance) continue;

                // Рисуем рёбра куба с оптимизацией
                double step = 2.0; // Шаг между частицами

                // Нижние 4 рёбра
                drawLine(level, particleType, x1, y1, z1, x2, y1, z1, step, playerX, playerY, playerZ, renderDistance);
                drawLine(level, particleType, x1, y1, z2, x2, y1, z2, step, playerX, playerY, playerZ, renderDistance);
                drawLine(level, particleType, x1, y1, z1, x1, y1, z2, step, playerX, playerY, playerZ, renderDistance);
                drawLine(level, particleType, x2, y1, z1, x2, y1, z2, step, playerX, playerY, playerZ, renderDistance);

                // Верхние 4 рёбра
                drawLine(level, particleType, x1, y2, z1, x2, y2, z1, step, playerX, playerY, playerZ, renderDistance);
                drawLine(level, particleType, x1, y2, z2, x2, y2, z2, step, playerX, playerY, playerZ, renderDistance);
                drawLine(level, particleType, x1, y2, z1, x1, y2, z2, step, playerX, playerY, playerZ, renderDistance);
                drawLine(level, particleType, x2, y2, z1, x2, y2, z2, step, playerX, playerY, playerZ, renderDistance);

                // Вертикальные 4 рёбра
                drawLine(level, particleType, x1, y1, z1, x1, y2, z1, step, playerX, playerY, playerZ, renderDistance);
                drawLine(level, particleType, x2, y1, z1, x2, y2, z1, step, playerX, playerY, playerZ, renderDistance);
                drawLine(level, particleType, x1, y1, z2, x1, y2, z2, step, playerX, playerY, playerZ, renderDistance);
                drawLine(level, particleType, x2, y1, z2, x2, y2, z2, step, playerX, playerY, playerZ, renderDistance);
            }
        }
    }

    private void drawLine(ServerLevel level, ParticleOptions particle,
                         double x1, double y1, double z1,
                         double x2, double y2, double z2,
                         double step, double playerX, double playerY, double playerZ, double maxDistance) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        double dz = z2 - z1;
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);

        if (distance < 0.1) return; // Слишком короткая линия

        int steps = (int) (distance / step) + 1;

        for (int i = 0; i <= steps; i++) {
            double t = steps > 0 ? (double) i / steps : 0;
            double x = x1 + dx * t;
            double y = y1 + dy * t;
            double z = z1 + dz * t;

            // Проверяем расстояние до игрока
            double distX = x - playerX;
            double distY = y - playerY;
            double distZ = z - playerZ;
            double distSq = distX * distX + distY * distY + distZ * distZ;

            if (distSq <= maxDistance * maxDistance) {
                level.sendParticles(particle, x + 0.5, y + 0.5, z + 0.5, 1, 0, 0, 0, 0);
            }
        }
    }
}
