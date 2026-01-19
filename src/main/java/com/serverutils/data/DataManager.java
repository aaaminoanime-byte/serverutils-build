package com.serverutils.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.LevelResource;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.*;

public class DataManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final MinecraftServer server;
    private final Path dataFolder;

    private Map<String, WarpData> warps = new HashMap<>();
    private Map<UUID, Map<String, HomeData>> homes = new HashMap<>();
    private Map<String, RegionData> regions = new HashMap<>();
    private Map<UUID, UUID> tpaRequests = new HashMap<>();
    private SpawnData spawn = null;

    public DataManager(MinecraftServer server) {
        this.server = server;
        ServerLevel overworld = server.getLevel(ServerLevel.OVERWORLD);
        this.dataFolder = overworld.getServer().getWorldPath(LevelResource.ROOT).resolve("serverutils");
        this.dataFolder.toFile().mkdirs();
    }

    public void load() {
        loadWarps();
        loadHomes();
        loadRegions();
        loadSpawn();
    }

    public void save() {
        saveWarps();
        saveHomes();
        saveRegions();
        saveSpawn();
    }

    public void addWarp(String name, WarpData warp) {
        warps.put(name, warp);
        saveWarps();
    }

    public void removeWarp(String name) {
        warps.remove(name);
        saveWarps();
    }

    public WarpData getWarp(String name) {
        return warps.get(name);
    }

    public Set<String> getWarpNames() {
        return warps.keySet();
    }

    private void loadWarps() {
        File file = dataFolder.resolve("warps.json").toFile();
        if (file.exists()) {
            try (Reader reader = new FileReader(file)) {
                Type type = new TypeToken<Map<String, WarpData>>(){}.getType();
                warps = GSON.fromJson(reader, type);
                if (warps == null) warps = new HashMap<>();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void saveWarps() {
        File file = dataFolder.resolve("warps.json").toFile();
        try (Writer writer = new FileWriter(file)) {
            GSON.toJson(warps, writer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void addHome(UUID playerUuid, String name, HomeData home) {
        homes.computeIfAbsent(playerUuid, k -> new HashMap<>()).put(name, home);
        saveHomes();
    }

    public void removeHome(UUID playerUuid, String name) {
        Map<String, HomeData> playerHomes = homes.get(playerUuid);
        if (playerHomes != null) {
            playerHomes.remove(name);
            saveHomes();
        }
    }

    public HomeData getHome(UUID playerUuid, String name) {
        Map<String, HomeData> playerHomes = homes.get(playerUuid);
        return playerHomes != null ? playerHomes.get(name) : null;
    }

    public Set<String> getHomeNames(UUID playerUuid) {
        Map<String, HomeData> playerHomes = homes.get(playerUuid);
        return playerHomes != null ? playerHomes.keySet() : Collections.emptySet();
    }

    public Map<UUID, Map<String, HomeData>> getAllHomes() {
        return homes;
    }

    private void loadHomes() {
        File file = dataFolder.resolve("homes.json").toFile();
        if (file.exists()) {
            try (Reader reader = new FileReader(file)) {
                Type type = new TypeToken<Map<UUID, Map<String, HomeData>>>(){}.getType();
                homes = GSON.fromJson(reader, type);
                if (homes == null) homes = new HashMap<>();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void saveHomes() {
        File file = dataFolder.resolve("homes.json").toFile();
        try (Writer writer = new FileWriter(file)) {
            GSON.toJson(homes, writer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void addRegion(String name, RegionData region) {
        regions.put(name, region);
        saveRegions();
    }

    public void removeRegion(String name) {
        regions.remove(name);
        saveRegions();
    }

    public RegionData getRegion(String name) {
        return regions.get(name);
    }

    public Set<String> getRegionNames() {
        return regions.keySet();
    }

    public Collection<RegionData> getAllRegions() {
        return regions.values();
    }

    private void loadRegions() {
        File file = dataFolder.resolve("regions.json").toFile();
        if (file.exists()) {
            try (Reader reader = new FileReader(file)) {
                Type type = new TypeToken<Map<String, RegionData>>(){}.getType();
                regions = GSON.fromJson(reader, type);
                if (regions == null) regions = new HashMap<>();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void saveRegions() {
        File file = dataFolder.resolve("regions.json").toFile();
        try (Writer writer = new FileWriter(file)) {
            GSON.toJson(regions, writer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void addTpaRequest(UUID requester, UUID target) {
        tpaRequests.put(target, requester);
    }

    public UUID getTpaRequest(UUID target) {
        return tpaRequests.get(target);
    }

    public void removeTpaRequest(UUID target) {
        tpaRequests.remove(target);
    }

    public void setSpawn(SpawnData spawnData) {
        this.spawn = spawnData;
        saveSpawn();
    }

    public SpawnData getSpawn() {
        return spawn;
    }

    private void loadSpawn() {
        File file = dataFolder.resolve("spawn.json").toFile();
        if (file.exists()) {
            try (Reader reader = new FileReader(file)) {
                spawn = GSON.fromJson(reader, SpawnData.class);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void saveSpawn() {
        File file = dataFolder.resolve("spawn.json").toFile();
        try (Writer writer = new FileWriter(file)) {
            GSON.toJson(spawn, writer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
