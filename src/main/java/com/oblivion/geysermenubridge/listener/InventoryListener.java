package com.oblivion.geysermenubridge.listener;

import com.oblivion.geysermenubridge.GeyserMenuBridge;
import com.oblivion.geysermenubridge.bridge.BedrockMenuManager;
import com.oblivion.geysermenubridge.detector.PlayerPlatformDetector;
import com.oblivion.geysermenubridge.synchronization.ClickSynchronizer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;

public class InventoryListener implements Listener {
    private final GeyserMenuBridge plugin;
    private final BedrockMenuManager bedrockMenuManager;
    private final PlayerPlatformDetector platformDetector;
    private final ClickSynchronizer clickSynchronizer;

    public InventoryListener(GeyserMenuBridge plugin) {
        this.plugin = plugin;
        this.bedrockMenuManager = plugin.getBedrockMenuManager();
        this.platformDetector = plugin.getPlayerPlatformDetector();
        this.clickSynchronizer = new ClickSynchronizer(plugin); // Crear instancia de ClickSynchronizer

        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        plugin.getLogger().info("InventoryListener registrado (PlayerQuit, InventoryClick, InventoryClose).");
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (bedrockMenuManager != null) {
            bedrockMenuManager.playerDisconnected(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.NORMAL) // Prioridad normal para actuar antes que otros plugins si es necesario, o ajustar según se necesite
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getWhoClicked();

        if (!platformDetector.isBedrockPlayer(player)) {
            return; // Solo nos interesan los jugadores de Bedrock para la sincronización de clics
        }

        Inventory clickedInventory = event.getClickedInventory();
        Inventory topInventory = event.getView().getTopInventory();

        com.oblivion.geysermenubridge.bridge.BridgedMenuInfo activeMenuInfo = bedrockMenuManager.getActiveBridgedMenuInfo(player.getUniqueId());

        if (activeMenuInfo != null && topInventory.equals(activeMenuInfo.getBridgedInventory())) {
            if (clickedInventory != null && clickedInventory.equals(topInventory)) {
                if (plugin.getCommandManager().isDebugMode()) {
                    plugin.getLogger().info(String.format("[InventoryListener DEBUG] Clic de Bedrock player %s en slot %d del menú puenteado (Original WinID: %d). Tipo: %s. Item: %s",
                            player.getName(), event.getSlot(), activeMenuInfo.getOriginalWindowId(), event.getClick().name(), event.getCurrentItem() !=null ? event.getCurrentItem().getType() : "EMPTY"));
                }
                clickSynchronizer.handleBedrockInventoryClick(event, player, activeMenuInfo);
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getPlayer();

        if (!platformDetector.isBedrockPlayer(player)) {
            return;
        }

        // Notificar al BedrockMenuManager que el jugador Bedrock cerró un inventario.
        // El manager verificará si era uno de los nuestros.
        if (bedrockMenuManager != null) {
            bedrockMenuManager.onBedrockPlayerCloseMenu(player, event.getInventory());
        }
    }
}
