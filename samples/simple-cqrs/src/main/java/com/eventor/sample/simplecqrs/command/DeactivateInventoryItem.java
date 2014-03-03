package com.eventor.sample.simplecqrs.command;

import java.util.UUID;

public class DeactivateInventoryItem {
    public final UUID inventoryItemId;
    public final int originalVersion;

    public DeactivateInventoryItem(UUID inventoryItemId, int originalVersion) {
        this.inventoryItemId = inventoryItemId;
        this.originalVersion = originalVersion;
    }
}
