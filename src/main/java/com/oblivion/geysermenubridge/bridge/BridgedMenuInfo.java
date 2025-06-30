package com.oblivion.geysermenubridge.bridge;

import org.bukkit.inventory.Inventory;

public class BridgedMenuInfo {
    private final Inventory bridgedInventory;
    private final int originalWindowId;
    private final String originalTitleJson; // Podría ser útil para reconstruir o identificar

    public BridgedMenuInfo(Inventory bridgedInventory, int originalWindowId, String originalTitleJson) {
        this.bridgedInventory = bridgedInventory;
        this.originalWindowId = originalWindowId;
        this.originalTitleJson = originalTitleJson;
    }

    public Inventory getBridgedInventory() {
        return bridgedInventory;
    }

    public int getOriginalWindowId() {
        return originalWindowId;
    }

    public String getOriginalTitleJson() {
        return originalTitleJson;
    }
}
