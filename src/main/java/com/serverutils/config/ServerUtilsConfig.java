package com.serverutils.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

public class ServerUtilsConfig {
    public static final ForgeConfigSpec SPEC;
    public static final Config CONFIG;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        CONFIG = new Config(builder);
        SPEC = builder.build();
    }

    public static void register() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, SPEC, "serverutils-server.toml");
    }

    public static class Config {
        public final ForgeConfigSpec.IntValue maxRegionSizeX;
        public final ForgeConfigSpec.IntValue maxRegionSizeY;
        public final ForgeConfigSpec.IntValue maxRegionSizeZ;
        public final ForgeConfigSpec.IntValue maxRegionVolume;
        public final ForgeConfigSpec.IntValue maxHomes;

        public Config(ForgeConfigSpec.Builder builder) {
            builder.push("regions");
            maxRegionSizeX = builder
                .comment("Максимальный размер региона по оси X (блоков)")
                .defineInRange("maxRegionSizeX", 256, 1, 10000);
            maxRegionSizeY = builder
                .comment("Максимальный размер региона по оси Y (блоков)")
                .defineInRange("maxRegionSizeY", 256, 1, 384);
            maxRegionSizeZ = builder
                .comment("Максимальный размер региона по оси Z (блоков)")
                .defineInRange("maxRegionSizeZ", 256, 1, 10000);
            maxRegionVolume = builder
                .comment("Максимальный объём региона (всего блоков, X*Y*Z)")
                .defineInRange("maxRegionVolume", 1000000, 1, 100000000);
            builder.pop();

            builder.push("homes");
            maxHomes = builder
                .comment("Максимальное количество домов на игрока")
                .defineInRange("maxHomes", 2, 1, 100);
            builder.pop();
        }
    }
}
