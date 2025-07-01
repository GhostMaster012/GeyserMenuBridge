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

            pendingMenus.remove(bedrockPlayer.getUniqueId());
            if (plugin.getCommandManager().isDebugMode()) {
                plugin.getLogger().info("[BedrockMenuManager DEBUG] Menú pendiente procesado (items recibidos y enviado a Bedrock) y removido para " + bedrockPlayer.getName());
            }
        } else {
            plugin.getLogger().warning(String.format("[BedrockMenuManager WARN] Se recibieron items para Original Window ID %d para el jugador %s pero no había un menú pendiente registrado o el ID no coincide.",
                    originalJavaWindowId, bedrockPlayer.getName()));
        }
    }

    private void sendInventoryToBedrock(Player bedrockPlayer, PendingMenu menuData, int originalWindowId) {
        // Verificar que es jugador de Bedrock usando GeyserApi
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

        // Ajustar tamaño a múltiplos de 9 (filas completas)
        if (inventorySize > 54) inventorySize = 54;
        else if (inventorySize % 9 != 0) {
            inventorySize = ((inventorySize / 9) + 1) * 9;
            if (inventorySize > 54) inventorySize = 54;
        }

        // Procesar título del menú
        Component titleComponent = GsonComponentSerializer.gson().deserialize(menuData.getTitle());
        String plainTitle = LegacyComponentSerializer.legacySection().serialize(titleComponent);
        if (plainTitle.length() > 32) {
            plainTitle = plainTitle.substring(0, 32);
        }

        // Crear inventario Bukkit que será mostrado al jugador Bedrock
        Inventory bukkitInventory = Bukkit.createInventory(null, inventorySize, plainTitle);
        List<ItemStack> items = menuData.getItems();

        // Llenar el inventario con los items del menú original
        for (int i = 0; i < menuData.getSize() && i < items.size() && i < inventorySize; i++) {
            ItemStack item = items.get(i);
            if (item != null) {
                bukkitInventory.setItem(i, item);
            }
        }

        // Contar items para logging
        long itemCount = 0;
        for(ItemStack item : bukkitInventory.getContents()) {
            if (item != null) itemCount++;
        }

        plugin.getLogger().info(String.format("[BedrockMenuManager INFO] Creando inventario para %s. Título: '%s', Tamaño: %d, Slots con items: %d, OriginalWinID: %d",
                bedrockPlayer.getName(), plainTitle, bukkitInventory.getSize(), itemCount, originalWindowId));

        try {
            // CORRECCIÓN: En lugar de usar sendInventory() que no existe,
            // abrimos el inventario directamente usando Bukkit
            // Geyser automáticamente manejará la conversión para el cliente Bedrock

            // Variables finales para usar en la lambda
            final String finalTitle = plainTitle;
            final int finalInventorySize = inventorySize;
            final UUID playerUUID = bedrockPlayer.getUniqueId();
            final String playerName = bedrockPlayer.getName();

            // Programar la apertura del inventario en el hilo principal
            Bukkit.getScheduler().runTask(plugin, () -> {
                try {
                    bedrockPlayer.openInventory(bukkitInventory);
                    plugin.getLogger().info(String.format("[BedrockMenuManager INFO] Inventario abierto exitosamente para %s (Título: '%s', Tamaño: %d)",
                            playerName, finalTitle, finalInventorySize));

                    // Registrar el menú como activo
                    BridgedMenuInfo bridgedInfo = new BridgedMenuInfo(bukkitInventory, originalWindowId, menuData.getTitle());
                    activeBridgedMenus.put(playerUUID, bridgedInfo);

                } catch (Exception e) {
                    plugin.getLogger().severe("[BedrockMenuManager ERROR] Error al abrir inventario para jugador Bedrock " + playerName + ": " + e.getMessage());
                    e.printStackTrace();
                    activeBridgedMenus.remove(playerUUID);
                }
            });

        } catch (Exception e) {
            plugin.getLogger().severe("[BedrockMenuManager ERROR] Error al programar apertura de inventario para jugador Bedrock " + bedrockPlayer.getName() + ": " + e.getMessage());
            e.printStackTrace();
            final UUID playerUUID = bedrockPlayer.getUniqueId();
            activeBridgedMenus.remove(playerUUID);
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
            // Esto podría ser necesario para notificar al plugin original que el menú se cerró
        }
    }
}
