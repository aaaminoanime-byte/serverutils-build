package com.serverutils.data;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class RegionViewManager {
    private final Set<UUID> viewingPlayers = new HashSet<>();

    public void enableView(UUID playerUuid) {
        viewingPlayers.add(playerUuid);
    }

    public void disableView(UUID playerUuid) {
        viewingPlayers.remove(playerUuid);
    }

    public boolean isViewing(UUID playerUuid) {
        return viewingPlayers.contains(playerUuid);
    }

    public Set<UUID> getViewingPlayers() {
        return new HashSet<>(viewingPlayers);
    }
}
