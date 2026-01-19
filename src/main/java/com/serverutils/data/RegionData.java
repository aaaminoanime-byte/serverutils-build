package com.serverutils.data;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class RegionData {
    private String name;
    private String dimension;
    private double x1, y1, z1;
    private double x2, y2, z2;
    private UUID owner;
    private Set<UUID> owners;
    private Set<UUID> members;
    private boolean pvpAllowed;
    private String buildMode; // "any", "members", "none"
    private boolean mobSpawnAllowed;
    private String useMode; // "any", "members", "none"
    private String chestsMode; // "any", "members", "none"

    public RegionData(String name, String dimension, double x1, double y1, double z1, double x2, double y2, double z2, UUID owner) {
        this.name = name;
        this.dimension = dimension;
        this.x1 = Math.min(x1, x2);
        this.y1 = Math.min(y1, y2);
        this.z1 = Math.min(z1, z2);
        this.x2 = Math.max(x1, x2);
        this.y2 = Math.max(y1, y2);
        this.z2 = Math.max(z1, z2);
        this.owner = owner;
        this.owners = new HashSet<>();
        this.members = new HashSet<>();
        this.pvpAllowed = false;
        this.buildMode = "none";
        this.mobSpawnAllowed = true;
        this.useMode = "none";
        this.chestsMode = "none";
    }

    public String getName() {
        return name;
    }

    public String getDimension() {
        return dimension;
    }

    public double getX1() {
        return x1;
    }

    public double getY1() {
        return y1;
    }

    public double getZ1() {
        return z1;
    }

    public double getX2() {
        return x2;
    }

    public double getY2() {
        return y2;
    }

    public double getZ2() {
        return z2;
    }

    public UUID getOwner() {
        return owner;
    }

    public Set<UUID> getOwners() {
        return owners;
    }

    public Set<UUID> getMembers() {
        return members;
    }

    public boolean isOwner(UUID playerUuid) {
        return owner.equals(playerUuid) || owners.contains(playerUuid);
    }

    public void addOwner(UUID playerUuid) {
        owners.add(playerUuid);
    }

    public void removeOwner(UUID playerUuid) {
        owners.remove(playerUuid);
    }

    public boolean isPvpAllowed() {
        return pvpAllowed;
    }

    public void setPvpAllowed(boolean pvpAllowed) {
        this.pvpAllowed = pvpAllowed;
    }

    public String getBuildMode() {
        return buildMode;
    }

    public void setBuildMode(String buildMode) {
        this.buildMode = buildMode;
    }

    public boolean isMobSpawnAllowed() {
        return mobSpawnAllowed;
    }

    public void setMobSpawnAllowed(boolean mobSpawnAllowed) {
        this.mobSpawnAllowed = mobSpawnAllowed;
    }

    public void addMember(UUID playerUuid) {
        members.add(playerUuid);
    }

    public void removeMember(UUID playerUuid) {
        members.remove(playerUuid);
    }

    public boolean contains(String dimension, double x, double y, double z) {
        return this.dimension.equals(dimension) &&
               x >= x1 && x <= x2 &&
               y >= y1 && y <= y2 &&
               z >= z1 && z <= z2;
    }

    public boolean canBuild(UUID playerUuid) {
        if (isOwner(playerUuid)) {
            return true;
        }

        switch (buildMode) {
            case "any":
                return true;
            case "members":
                return members.contains(playerUuid);
            case "none":
            default:
                return false;
        }
    }

    public boolean isMember(UUID playerUuid) {
        return isOwner(playerUuid) || members.contains(playerUuid);
    }

    public String getUseMode() {
        return useMode;
    }

    public void setUseMode(String useMode) {
        this.useMode = useMode;
    }

    public String getChestsMode() {
        return chestsMode;
    }

    public void setChestsMode(String chestsMode) {
        this.chestsMode = chestsMode;
    }

    public boolean canUse(UUID playerUuid) {
        if (isOwner(playerUuid)) {
            return true;
        }

        switch (useMode) {
            case "any":
                return true;
            case "members":
                return members.contains(playerUuid);
            case "none":
            default:
                return false;
        }
    }

    public boolean canOpenChests(UUID playerUuid) {
        if (isOwner(playerUuid)) {
            return true;
        }

        switch (chestsMode) {
            case "any":
                return true;
            case "members":
                return members.contains(playerUuid);
            case "none":
            default:
                return false;
        }
    }
}
