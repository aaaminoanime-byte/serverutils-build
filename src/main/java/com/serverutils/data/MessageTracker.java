package com.serverutils.data;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MessageTracker {
    private final Map<UUID, UUID> lastMessageFrom = new HashMap<>();

    public void recordMessage(UUID recipient, UUID sender) {
        lastMessageFrom.put(recipient, sender);
    }

    public UUID getLastSender(UUID recipient) {
        return lastMessageFrom.get(recipient);
    }
}
