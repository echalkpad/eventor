package com.eventor.sample.simplecqrs.command;

import java.util.UUID;

public class CreateInventoryItem {
    public final UUID inventoryItemId;
    public final String name;

    public CreateInventoryItem(UUID inventoryItemId, String name) {
        this.inventoryItemId = inventoryItemId;
        this.name = name;
    }
}
