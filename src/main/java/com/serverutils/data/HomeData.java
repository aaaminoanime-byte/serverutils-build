package com.serverutils.data;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class HomeData {
    private String dimension;
    private double x;
    private double y;
    private double z;
    private float yaw;
    private float pitch;
    private UUID owner;
    private Set<UUID> invitedPlayers;

    public HomeData(String dimension, double x, double y, double z, float yaw, float pitch, UUID owner) {
        this.dimension = dimension;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
        this.owner = owner;
        this.invitedPlayers = new HashSet<>();
    }

    public String getDimension() {
        return dimension;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public float getYaw() {
        return yaw;
    }

    public float getPitch() {
        return pitch;
    }

    public UUID getOwner() {
        return owner;
    }

    public Set<UUID> getInvitedPlayers() {
        return invitedPlayers;
    }

    public boolean canAccess(UUID playerUuid) {
        return owner.equals(playerUuid) || invitedPlayers.contains(playerUuid);
    }

    public void invitePlayer(UUID playerUuid) {
        invitedPlayers.add(playerUuid);
    }

    public void removeInvite(UUID playerUuid) {
        invitedPlayers.remove(playerUuid);
    }
}
