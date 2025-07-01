package com.oblivion.geysermenubridge.listener;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.oblivion.geysermenubridge.GeyserMenuBridge;
import com.oblivion.geysermenubridge.bridge.BedrockMenuManager;
import com.oblivion.geysermenubridge.bridge.BridgedMenuInfo;
import com.oblivion.geysermenubridge.detector.PlayerPlatformDetector;
import org.bukkit.inventory.Inventory;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class PacketListener {
    private final GeyserMenuBridge plugin;
    private final PlayerPlatformDetector platformDetector;
    private final BedrockMenuManager bedrockMenuManager;
    private final ProtocolManager protocolManager;

    public PacketListener(GeyserMenuBridge plugin, PlayerPlatformDetector platformDetector, BedrockMenuManager bedrockMenuManager) {
        this.plugin = plugin;
        this.platformDetector = platformDetector;
        this.bedrockMenuManager = bedrockMenuManager;
        this.protocolManager = ProtocolLibrary.getProtocolManager();

        registerPacketListeners();
    }

    private void registerPacketListeners() {
        if (!platformDetector.isFloodgateAvailable()) {
            plugin.getLogger().warning("No se registrarán PacketListeners ya que Floodgate no está disponible.");
            return;
        }

        // Listener para OPEN_WINDOW
        protocolManager.addPacketListener(new PacketAdapter(plugin, ListenerPriority.NORMAL, PacketType.Play.Server.OPEN_WINDOW) {
            @Override
            public void onPacketSending(PacketEvent event) {
                handleOpenWindow(event);
            }
        });

        // Listener para WINDOW_ITEMS
        protocolManager.addPacketListener(new PacketAdapter(plugin, ListenerPriority.NORMAL, PacketType.Play.Server.WINDOW_ITEMS) {
            @Override
            public void onPacketSending(PacketEvent event) {
                handleWindowItems(event);
            }
        });

        // Listener para CLOSE_WINDOW (desde el cliente)
        protocolManager.addPacketListener(new PacketAdapter(plugin, ListenerPriority.NORMAL, PacketType.Play.Client.CLOSE_WINDOW) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                handleClientCloseWindow(event);
            }
        });

        // Listener para CLOSE_WINDOW (desde el servidor)
        protocolManager.addPacketListener(new PacketAdapter(plugin, ListenerPriority.NORMAL, PacketType.Play.Server.CLOSE_WINDOW) {
            @Override
            public void onPacketSending(PacketEvent event) {
                handleServerCloseWindow(event);
            }
        });

        // Registrar listeners de actualización
        registerWindowUpdateListeners();

        plugin.getLogger().info("[GeyserMenuBridge INFO] PacketListeners registrados correctamente.");
    }

    private void handleOpenWindow(PacketEvent event) {
        Player player = event.getPlayer();
        if (player == null || !platformDetector.isBedrockPlayer(player)) {
            return;
        }

        PacketContainer packet = event.getPacket();
        int windowId = packet.getIntegers().read(0);
        WrappedChatComponent titleComponent = packet.getChatComponents().read(0);
        String titleJson = titleComponent.getJson();

        if (plugin.getCommandManager().isDebugMode()) {
            plugin.getLogger().info(String.format("[PacketListener DEBUG] Interceptado OPEN_WINDOW para Bedrock player %s. Window ID: %d, Title: %s",
                    player.getName(), windowId, titleJson));
        }

        bedrockMenuManager.registerPendingMenu(player, windowId, titleJson);
        event.setCancelled(true);

        if (plugin.getCommandManager().isDebugMode()) {
            plugin.getLogger().info("[PacketListener DEBUG] Cancelado OPEN_WINDOW para " + player.getName() + " y menú registrado como pendiente.");
        }
    }

    private void handleWindowItems(PacketEvent event) {
        Player player = event.getPlayer();
        if (player == null || !platformDetector.isBedrockPlayer(player)) {
            return;
        }

        PacketContainer packet = event.getPacket();
        int windowId = packet.getIntegers().read(0);
        List<ItemStack> items = packet.getItemListModifier().read(0);

        // Verificar si es un menú pendiente (inicial)
        if (bedrockMenuManager.getPendingMenu(player, windowId) != null) {
            if (plugin.getCommandManager().isDebugMode()) {
                plugin.getLogger().info(String.format("[PacketListener DEBUG] Interceptado WINDOW_ITEMS (inicial) para Bedrock player %s. Window ID: %d",
                        player.getName(), windowId));
            }

            int stateId = packet.getIntegers().read(1);

            if (plugin.getCommandManager().isDebugMode()) {
                plugin.getLogger().info(String.format("[PacketListener DEBUG] WINDOW_ITEMS (inicial) para %s, Window ID: %d, State ID: %d, Items count: %d",
                                        player.getName(), windowId, stateId, items.size()));
            }

            bedrockMenuManager.completePendingMenuWithItems(player, windowId, items);
            event.setCancelled(true);

            if (plugin.getCommandManager().isDebugMode()) {
                plugin.getLogger().info("[PacketListener DEBUG] Cancelado WINDOW_ITEMS (inicial) para " + player.getName() + " y menú completado.");
            }
        }
        // Verificar si es una actualización a un menú activo
        else {
            BridgedMenuInfo activeMenuInfo = bedrockMenuManager.getActiveBridgedMenuInfo(player.getUniqueId());

            if (activeMenuInfo != null && activeMenuInfo.getOriginalWindowId() == windowId) {
                handleWindowItemsUpdate(event, player, windowId, items, activeMenuInfo);
            }
        }
    }

    private void handleWindowItemsUpdate(PacketEvent event, Player player, int windowId, List<ItemStack> items, BridgedMenuInfo activeMenuInfo) {
        if (plugin.getCommandManager().isDebugMode()) {
            plugin.getLogger().info(String.format("[PacketListener DEBUG] Interceptado WINDOW_ITEMS (actualización) para Bedrock player %s. Original Window ID: %d. Items: %d",
                    player.getName(), windowId, items.size()));
        }

        Inventory bridgedInventory = activeMenuInfo.getBridgedInventory();
        int changedSlots = calculateChangedSlots(bridgedInventory, items);

        if (changedSlots > 0) {
            plugin.getLogger().info(String.format("[PacketListener INFO] WINDOW_ITEMS (actualización) para %s (OriginalWinID: %d) con %d cambios. Actualizando espejo.",
                    player.getName(), windowId, changedSlots));

            updateInventoryContents(bridgedInventory, items);
        } else if (plugin.getCommandManager().isDebugMode()) {
            plugin.getLogger().info(String.format("[PacketListener DEBUG] WINDOW_ITEMS (actualización) para %s (OriginalWinID: %d) no contenía cambios visibles.",
                    player.getName(), windowId));
        }

        event.setCancelled(true);
        // CORRECCIÓN DEL ERROR DE LOGGER: Usar String.format() o concatenación para un solo argumento.
        plugin.getLogger().info(String.format("[PacketListener INFO] WINDOW_ITEMS (actualización) para %s (OriginalWinID: %d) procesado y cancelado.",
                player.getName(), windowId));
    }

    private int calculateChangedSlots(Inventory bridgedInventory, List<ItemStack> items) {
        int changedSlots = 0;

        if (bridgedInventory.getSize() == items.size()) {
            for (int i = 0; i < items.size(); i++) {
                if (!java.util.Objects.equals(bridgedInventory.getItem(i), items.get(i))) {
                    changedSlots++;
                }
            }
        } else {
            changedSlots = items.size();
            plugin.getLogger().warning(String.format("[PacketListener WARN] WINDOW_ITEMS tamaño incompatible: inventario=%d, items=%d",
                    bridgedInventory.getSize(), items.size()));
        }

        return changedSlots;
    }

    private void updateInventoryContents(Inventory bridgedInventory, List<ItemStack> items) {
        if (bridgedInventory.getSize() == items.size()) {
            for (int i = 0; i < items.size(); i++) {
                if (!java.util.Objects.equals(bridgedInventory.getItem(i), items.get(i))) {
                    bridgedInventory.setItem(i, items.get(i));
                }
            }
        } else {
            bridgedInventory.setContents(items.toArray(new ItemStack[0]));
        }
    }

    private void handleClientCloseWindow(PacketEvent event) {
        Player player = event.getPlayer();
        if (player == null || !platformDetector.isBedrockPlayer(player)) {
            return;
        }

        PacketContainer packet = event.getPacket();
        int windowId = packet.getIntegers().read(0);

        com.oblivion.geysermenubridge.bridge.PendingMenu pending = bedrockMenuManager.getPendingMenu(player.getUniqueId());
        if (pending != null && pending.getOriginalWindowId() == windowId) {
            bedrockMenuManager.removePendingMenu(player.getUniqueId());

            if (plugin.getCommandManager().isDebugMode()) {
                plugin.getLogger().info(String.format("[PacketListener DEBUG] Cliente envió CLOSE_WINDOW (ID: %d) para %s mientras un menú estaba pendiente. Menú pendiente limpiado.",
                        windowId, player.getName()));
            }
        }
    }

    private void handleServerCloseWindow(PacketEvent event) {
        Player player = event.getPlayer();
        if (player == null || !platformDetector.isBedrockPlayer(player)) {
            return;
        }

        PacketContainer packet = event.getPacket();
        int windowId = packet.getIntegers().read(0);

        com.oblivion.geysermenubridge.bridge.PendingMenu pending = bedrockMenuManager.getPendingMenu(player.getUniqueId());
        if (pending != null && pending.getOriginalWindowId() == windowId) {
            bedrockMenuManager.removePendingMenu(player.getUniqueId());

            if (plugin.getCommandManager().isDebugMode()) {
                plugin.getLogger().info(String.format("[PacketListener DEBUG] Servidor envió CLOSE_WINDOW (ID: %d) para %s mientras un menú estaba pendiente. Menú pendiente limpiado.",
                        windowId, player.getName()));
            }
        }

        BridgedMenuInfo activeInfo = bedrockMenuManager.getActiveBridgedMenuInfo(player.getUniqueId());
        if (activeInfo != null && activeInfo.getOriginalWindowId() == windowId) {
            if (plugin.getCommandManager().isDebugMode()) {
                plugin.getLogger().info(String.format("[PacketListener DEBUG] Servidor envió CLOSE_WINDOW (ID: %d) para %s para un menú activo puenteado. Se espera que InventoryCloseEvent lo maneje.",
                        windowId, player.getName()));
            }
        }
    }

    private void registerWindowUpdateListeners() {
        // Listener para SET_SLOT
        protocolManager.addPacketListener(new PacketAdapter(plugin, ListenerPriority.NORMAL, PacketType.Play.Server.SET_SLOT) {
            @Override
            public void onPacketSending(PacketEvent event) {
                handleSetSlot(event);
            }
        });

        // Listener para WINDOW_ITEMS ya está manejado en el método principal
        plugin.getLogger().info("[GeyserMenuBridge INFO] PacketListeners para SET_SLOT registrados.");
    }

    private void handleSetSlot(PacketEvent event) {
        Player player = event.getPlayer();
        if (player == null || !platformDetector.isBedrockPlayer(player)) {
            return;
        }

        PacketContainer packet = event.getPacket();
        int packetWindowId = packet.getIntegers().read(0);
        int slot = packet.getIntegers().read(2);
        ItemStack itemStack = packet.getItemModifier().read(0);

        BridgedMenuInfo activeMenuInfo = bedrockMenuManager.getActiveBridgedMenuInfo(player.getUniqueId());

        if (activeMenuInfo != null && activeMenuInfo.getOriginalWindowId() == packetWindowId) {
            if (plugin.getCommandManager().isDebugMode()) {
                plugin.getLogger().info(String.format("[PacketListener DEBUG] Interceptado SET_SLOT para Bedrock player %s. Original Window ID: %d, Slot: %d, Item: %s",
                        player.getName(), packetWindowId, slot, itemStack.getType().name()));
            }

            activeMenuInfo.getBridgedInventory().setItem(slot, itemStack);
            event.setCancelled(true);

            plugin.getLogger().info(String.format("[PacketListener INFO] SET_SLOT para player %s (OriginalWinID: %d) procesado (modificación espejo) y cancelado paquete original.",
                    player.getName(), packetWindowId));
        }
    }

    public void unregisterListeners() {
        protocolManager.removePacketListeners(plugin);
        plugin.getLogger().info("Todos los PacketListeners de GeyserMenuBridge desregistrados.");
    }
}
