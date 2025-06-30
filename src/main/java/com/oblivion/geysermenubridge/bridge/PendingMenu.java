package com.oblivion.geysermenubridge.bridge;

import org.bukkit.inventory.ItemStack;
import java.util.List;

public class PendingMenu {
    private final int originalWindowId; // ID de la ventana Java original
    private final String title;
    private List<ItemStack> items;
    private int size; // Número de slots del menú

    public PendingMenu(int originalWindowId, String title) {
        this.originalWindowId = originalWindowId;
        this.title = title;
    }

    public int getOriginalWindowId() {
        return originalWindowId;
    }

    public String getTitle() {
        return title;
    }

    public List<ItemStack> getItems() {
        return items;
    }

    public void setItems(List<ItemStack> items) {
        this.items = items;
        if (items != null) {
            this.size = items.size(); // El tamaño es la cantidad de items en el paquete WINDOW_ITEMS
        } else {
            this.size = 0;
        }
    }

    public int getSize() {
        return size;
    }
}
