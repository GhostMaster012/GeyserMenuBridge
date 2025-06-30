package com.oblivion.geysermenubridge.bridge;

import com.oblivion.geysermenubridge.GeyserMenuBridge;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.geysermc.geyser.api.GeyserApi;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class BedrockMenuManager {

    private final GeyserMenuBridge plugin;
    private final Map<UUID, PendingMenu> pendingMenus = new HashMap<>();
    // Mapa para rastrear la información del menú que se está mostrando actualmente a un jugador de Bedrock
    private final Map<UUID, BridgedMenuInfo> activeBridgedMenus = new HashMap<>();


    public BedrockMenuManager(GeyserMenuBridge plugin) {
        this.plugin = plugin;
    }

    public void registerPendingMenu(Player player, int originalJavaWindowId, String titleJson) {
        PendingMenu pendingMenu = new PendingMenu(originalJavaWindowId, titleJson);
        pendingMenus.put(player.getUniqueId(), pendingMenu);
        if (plugin.getCommandManager().isDebugMode()) {
            plugin.getLogger().info("[BedrockMenuManager DEBUG] Menú pendiente registrado para " + player.getName() + " (Original Window ID: " + originalJavaWindowId + ")");
        }
    }

    public void removePendingMenu(UUID playerUuid) {
        if (pendingMenus.remove(playerUuid) != null) {
            if (plugin.getCommandManager().isDebugMode()) {
                plugin.getLogger().info("[BedrockMenuManager DEBUG] Menú pendiente removido para UUID: " + playerUuid);
            }
        }
    }

    public PendingMenu getPendingMenu(Player player, int originalJavaWindowId) {
        PendingMenu menu = pendingMenus.get(player.getUniqueId());
        if (menu != null && menu.getOriginalWindowId() == originalJavaWindowId) {
            return menu;
        }
        return null;
    }

    public PendingMenu getPendingMenu(UUID playerUuid) {
        return pendingMenus.get(playerUuid);
    }

    public BridgedMenuInfo getActiveBridgedMenuInfo(UUID playerUuid) {
        return activeBridgedMenus.get(playerUuid);
    }


    public void completePendingMenuWithItems(Player bedrockPlayer, int originalJavaWindowId, List<ItemStack> itemsFromJavaMenu) {
        PendingMenu pendingMenu = getPendingMenu(bedrockPlayer, originalJavaWindowId);
        if (pendingMenu != null) {
            pendingMenu.setItems(itemsFromJavaMenu);
            plugin.getLogger().info(String.format("[BedrockMenuManager INFO] Items recibidos para menú pendiente de %s (Original Window ID: %d). Título: %s. Tamaño: %d",
                                   bedrockPlayer.getName(), originalJavaWindowId, pendingMenu.getTitle(), pendingMenu.getSize()));

            sendInventoryToBedrock(bedrockPlayer, pendingMenu, originalJavaWindowId);

            pendingMenus.remove(bedrockPlayer.getUniqueId()); // No necesita log de debug aquí, removePendingMenu ya lo tiene si está activo
            if (plugin.getCommandManager().isDebugMode()) {
                plugin.getLogger().info("[BedrockMenuManager DEBUG] Menú pendiente procesado (items recibidos y enviado a Bedrock) y removido para " + bedrockPlayer.getName());
            }
        } else {
            plugin.getLogger().warning(String.format("[BedrockMenuManager WARN] Se recibieron items para Original Window ID %d para el jugador %s pero no había un menú pendiente registrado o el ID no coincide.",
                                       originalJavaWindowId, bedrockPlayer.getName()));
        }
    }

    private void sendInventoryToBedrock(Player bedrockPlayer, PendingMenu menuData, int originalWindowId) {
        if (!GeyserApi.api().isBedrockPlayer(bedrockPlayer.getUniqueId())) {
            plugin.getLogger().warning("[BedrockMenuManager WARN] Intento de enviar inventario a un jugador que no es de Bedrock: " + bedrockPlayer.getName());
            return;
        }

        int inventorySize = menuData.getSize();
        if (inventorySize <= 0) {
            plugin.getLogger().warning(String.format("[BedrockMenuManager WARN] Tamaño de inventario inválido o cero para %s: %d. No se enviará el inventario.",
                                       bedrockPlayer.getName(), inventorySize));
            return;
        }

        if (inventorySize > 54) inventorySize = 54;
        else if (inventorySize % 9 != 0) {
            inventorySize = ((inventorySize / 9) + 1) * 9;
            if (inventorySize > 54) inventorySize = 54;
        }

        Component titleComponent = GsonComponentSerializer.gson().deserialize(menuData.getTitle());
        String plainTitle = LegacyComponentSerializer.legacySection().serialize(titleComponent);
        if (plainTitle.length() > 32) {
            plainTitle = plainTitle.substring(0, 32);
        }

        Inventory bukkitInventory = Bukkit.createInventory(null, inventorySize, plainTitle);
        List<ItemStack> items = menuData.getItems();
        for (int i = 0; i < menuData.getSize() && i < items.size() && i < inventorySize; i++) {
            ItemStack item = items.get(i);
            if (item != null) {
                bukkitInventory.setItem(i, item);
            }
        }

        long itemCount = 0;
        for(ItemStack item : bukkitInventory.getContents()) {
            if (item != null) itemCount++;
        }
        plugin.getLogger().info(String.format("[BedrockMenuManager INFO] Intentando enviar inventario a %s vía Geyser API. Título: '%s', Tamaño Bukkit: %d, Slots con items: %d, OriginalWinID: %d",
                                bedrockPlayer.getName(), plainTitle, bukkitInventory.getSize(), itemCount, originalWindowId));
        try {
            GeyserApi.api().sendInventory(bedrockPlayer.getUniqueId(), bukkitInventory);
            plugin.getLogger().info(String.format("[BedrockMenuManager INFO] Inventario falso enviado exitosamente a %s (Título: '%s', Tamaño: %d)",
                                   bedrockPlayer.getName(), plainTitle, inventorySize));

            BridgedMenuInfo bridgedInfo = new BridgedMenuInfo(bukkitInventory, originalWindowId, menuData.getTitle());
            activeBridgedMenus.put(bedrockPlayer.getUniqueId(), bridgedInfo);

        } catch (Exception e) {
            plugin.getLogger().severe("[BedrockMenuManager ERROR] Error al enviar inventario a jugador Bedrock " + bedrockPlayer.getName() + ": " + e.getMessage());
            e.printStackTrace();
            activeBridgedMenus.remove(bedrockPlayer.getUniqueId());
        }
    }

    public void playerDisconnected(Player player) {
        pendingMenus.remove(player.getUniqueId());
        activeBridgedMenus.remove(player.getUniqueId());
        plugin.getLogger().info("[BedrockMenuManager INFO] Limpiados datos de menú para jugador desconectado: " + player.getName());
    }

    public void onBedrockPlayerCloseMenu(Player player, Inventory closedInventory) {
        BridgedMenuInfo bridgedInfo = activeBridgedMenus.get(player.getUniqueId());
        if (bridgedInfo != null && bridgedInfo.getBridgedInventory().equals(closedInventory)) {
            activeBridgedMenus.remove(player.getUniqueId());
            plugin.getLogger().info("[BedrockMenuManager INFO] Jugador Bedrock " + player.getName() + " cerró el menú puenteado (Original Window ID: " + bridgedInfo.getOriginalWindowId() + ")");
            // TODO: Considerar enviar PacketPlayInCloseWindow al servidor con bridgedInfo.getOriginalWindowId()
        }
    }
}
