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
import com.oblivion.geysermenubridge.detector.PlayerPlatformDetector;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack; // Necesario para la lista de items

import java.util.List; // Necesario para la lista de items

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
        });

        // Listener para WINDOW_ITEMS
        protocolManager.addPacketListener(new PacketAdapter(plugin, ListenerPriority.NORMAL, PacketType.Play.Server.WINDOW_ITEMS) {
            @Override
            public void onPacketSending(PacketEvent event) {
                Player player = event.getPlayer();
                if (player == null || !platformDetector.isBedrockPlayer(player)) {
                    return;
                }

                PacketContainer packet = event.getPacket();
                int windowId = packet.getIntegers().read(0);

                if (bedrockMenuManager.getPendingMenu(player, windowId) != null) {
                    if (plugin.getCommandManager().isDebugMode()) {
                        plugin.getLogger().info(String.format("[PacketListener DEBUG] Interceptado WINDOW_ITEMS (inicial) para Bedrock player %s. Window ID: %d",
                                player.getName(), windowId));
                    }

                    List<ItemStack> items = packet.getItemListModifier().read(0);
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
            }
        });

        // Listener para CLOSE_WINDOW (tanto cliente como servidor)
        // Esto es importante para limpiar pendingMenus si el jugador cierra el menú ANTES de que lleguen los items,
        // o si el servidor cierra el menú.
        PacketAdapter closeWindowAdapter = new PacketAdapter(plugin, ListenerPriority.NORMAL,
                PacketType.Play.Client.CLOSE_WINDOW, PacketType.Play.Server.CLOSE_WINDOW) {
            @Override
            public void onPacketReceiving(PacketEvent event) { // Paquete del cliente al servidor
                handleCloseWindow(event.getPlayer(), event.getPacket());
            }

            @Override
            public void onPacketSending(PacketEvent event) { // Paquete del servidor al cliente
                handleCloseWindow(event.getPlayer(), event.getPacket());
                 // No cancelar el CLOSE_WINDOW del servidor, ya que podría ser legítimo.
                 // Si lo cancelamos, el cliente Java podría quedarse con un menú fantasma.
                 // Para Bedrock, si teníamos un menú pendiente, ya se habrá limpiado.
            }
        };
        protocolManager.addPacketListener(closeWindowAdapter);


        plugin.getLogger().info("PacketListeners para OPEN_WINDOW, WINDOW_ITEMS y CLOSE_WINDOW registrados.");
    }

    private void handleCloseWindow(Player player, PacketContainer packet) {
        if (player == null || !platformDetector.isBedrockPlayer(player)) {
            return;
        }
        int windowId = packet.getIntegers().read(0);

        // Si el jugador tenía un menú pendiente con este ID, lo eliminamos.
        com.oblivion.geysermenubridge.bridge.PendingMenu pending = bedrockMenuManager.getPendingMenu(player.getUniqueId());
        if (pending != null && pending.getOriginalWindowId() == windowId) {
            // Si el jugador cierra la ventana ANTES de que lleguen los items, limpiamos el pendingMenu.
            pendingMenus.remove(player.getUniqueId()); // Acceso directo al mapa de pendingMenus, o mejor un método en BedrockMenuManager
            plugin.getLogger().info(String.format("[PacketListener] CLOSE_WINDOW (ID: %d) detectado para %s mientras un menú estaba pendiente. Menú pendiente limpiado.", windowId, player.getName()));
        }
        // El cierre de inventarios activos ya se maneja en InventoryListener -> BedrockMenuManager.onBedrockPlayerCloseMenu
    }

    private void registerWindowUpdateListeners() {
        // Listener para SET_SLOT
        protocolManager.addPacketListener(new PacketAdapter(plugin, ListenerPriority.NORMAL, PacketType.Play.Server.SET_SLOT) {
            @Override
            public void onPacketSending(PacketEvent event) {
                Player player = event.getPlayer();
                if (player == null || !platformDetector.isBedrockPlayer(player)) {
                    return;
                }

                PacketContainer packet = event.getPacket();
                int packetWindowId = packet.getIntegers().read(0); // ID de la ventana original de Java
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
                    plugin.getLogger().info(String.format("[PacketListener INFO] SET_SLOT para player %s (OriginalWinID: %d) procesado (modificación espejo) y cancelado paquete original.", player.getName(), packetWindowId));
                }
            }
        });

        // Listener para WINDOW_ITEMS (actualizaciones a un menú ya activo)
        protocolManager.addPacketListener(new PacketAdapter(plugin, ListenerPriority.NORMAL, PacketType.Play.Server.WINDOW_ITEMS) {
            @Override
            public void onPacketSending(PacketEvent event) {
                Player player = event.getPlayer();
                 if (player == null || !platformDetector.isBedrockPlayer(player)) {
                    return;
                }

                PacketContainer packet = event.getPacket();
                int packetWindowId = packet.getIntegers().read(0);
                List<ItemStack> items = packet.getItemListModifier().read(0);

                BridgedMenuInfo activeMenuInfo = bedrockMenuManager.getActiveBridgedMenuInfo(player.getUniqueId());
                boolean isPending = bedrockMenuManager.getPendingMenu(player, packetWindowId) != null;

                if (!isPending && activeMenuInfo != null && activeMenuInfo.getOriginalWindowId() == packetWindowId) {
                    if (plugin.getCommandManager().isDebugMode()) {
                        plugin.getLogger().info(String.format("[PacketListener DEBUG] Interceptado WINDOW_ITEMS (actualización) para Bedrock player %s. Original Window ID: %d. Items: %d",
                                player.getName(), packetWindowId, items.size()));
                    }
                    Inventory bridgedInventory = activeMenuInfo.getBridgedInventory();
                    int changedSlots = 0;
                    if (bridgedInventory.getSize() == items.size()) {
                        for (int i = 0; i < items.size(); i++) {
                            if (!java.util.Objects.equals(bridgedInventory.getItem(i), items.get(i))) {
                                changedSlots++;
                            }
                        }
                    } else {
                        changedSlots = items.size();
                         plugin.getLogger().warning(String.format("[PacketListener WARN] WINDOW_ITEMS (actualización) para %s con Window ID %d tiene un tamaño de items (%d) que no coincide con el inventario puenteado activo (%d). Se considera cambio masivo.",
                                player.getName(), packetWindowId, items.size(), bridgedInventory.getSize()));
                    }

                    // Implementación simplificada de la estrategia híbrida: siempre modificar espejo por ahora.
                    // La lógica de >3 slots para reenvío explícito se omite por brevedad y se tratará como optimización si es necesario.
                    if (changedSlots > 0) {
                        plugin.getLogger().info(String.format("[PacketListener INFO] WINDOW_ITEMS (actualización) para %s (OriginalWinID: %d) con %d cambios. Actualizando espejo.", player.getName(), packetWindowId, changedSlots));
                        if (bridgedInventory.getSize() == items.size()) { // Solo actualizar si los tamaños son consistentes
                            for (int i = 0; i < items.size(); i++) { // Aplicar cambios individuales
                                if (!java.util.Objects.equals(bridgedInventory.getItem(i), items.get(i))) {
                                    bridgedInventory.setItem(i, items.get(i));
                                }
                            }
                        } else { // Si los tamaños no coinciden, reemplazar todo el contenido si es posible (podría ser un error de lógica)
                             bridgedInventory.setContents(items.toArray(new ItemStack[0]));
                        }
                        event.setCancelled(true);
                    } else {
                         if (plugin.getCommandManager().isDebugMode()) {
                            plugin.getLogger().info("[PacketListener DEBUG] WINDOW_ITEMS (actualización) para %s (OriginalWinID: %d) no contenía cambios visibles. Paquete cancelado igualmente.", player.getName(), packetWindowId);
                         }
                        event.setCancelled(true);
                    }
                     plugin.getLogger().info(String.format("[PacketListener INFO] WINDOW_ITEMS (actualización) para %s (OriginalWinID: %d) procesado y cancelado.", player.getName(), packetWindowId));
                }
            }
        });
         plugin.getLogger().info("[GeyserMenuBridge INFO] PacketListeners para SET_SLOT y WINDOW_ITEMS (actualizaciones) registrados.");
    }

    @Override
    public void onPacketSending(PacketEvent event) { // Paquete del servidor al cliente para CLOSE_WINDOW
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
                plugin.getLogger().info(String.format("[PacketListener DEBUG] Servidor envió CLOSE_WINDOW (ID: %d) para %s mientras un menú estaba pendiente. Menú pendiente limpiado.", windowId, player.getName()));
            }
        }
    }
};
protocolManager.addPacketListener(closeWindowAdapter);
registerWindowUpdateListeners();


plugin.getLogger().info("[GeyserMenuBridge INFO] PacketListeners para OPEN_WINDOW, WINDOW_ITEMS (inicial) y CLOSE_WINDOW registrados.");
}

private void handleCloseWindow(Player player, PacketContainer packet) { // Paquete del Cliente al Servidor
    if (player == null || !platformDetector.isBedrockPlayer(player)) {
        return;
    }
    int windowId = packet.getIntegers().read(0);

    com.oblivion.geysermenubridge.bridge.PendingMenu pending = bedrockMenuManager.getPendingMenu(player.getUniqueId());
    if (pending != null && pending.getOriginalWindowId() == windowId) {
        bedrockMenuManager.removePendingMenu(player.getUniqueId());
        if (plugin.getCommandManager().isDebugMode()) {
            plugin.getLogger().info(String.format("[PacketListener DEBUG] Cliente envió CLOSE_WINDOW (ID: %d) para %s mientras un menú estaba pendiente. Menú pendiente limpiado.", windowId, player.getName()));
        }
    }
}


public void unregisterListeners() {
        protocolManager.removePacketListeners(plugin);
        plugin.getLogger().info("Todos los PacketListeners de GeyserMenuBridge desregistrados.");
    }
}
