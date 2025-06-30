package com.oblivion.geysermenubridge.detector;

import com.oblivion.geysermenubridge.GeyserMenuBridge;
import org.bukkit.entity.Player;
import org.geysermc.floodgate.api.FloodgateApi;

import java.util.UUID;

public class PlayerPlatformDetector {

    private final FloodgateApi floodgateApi;
    private boolean floodgateAvailable;

    public PlayerPlatformDetector(GeyserMenuBridge plugin) {
        try {
            this.floodgateApi = FloodgateApi.getInstance();
            this.floodgateAvailable = true;
            plugin.getLogger().info("Floodgate API encontrada. La detección de jugadores Bedrock está activa.");
        } catch (Exception e) {
            // Esto puede ocurrir si Floodgate no está presente o falla al cargar su API.
            // No ClassDefFoundError si Floodgate no está, IllegalStateException si ya está instanciada o no.
            this.floodgateApi = null;
            this.floodgateAvailable = false;
            plugin.getLogger().warning("Floodgate API no encontrada. La detección de jugadores Bedrock no funcionará.");
            plugin.getLogger().warning("Asegúrate de que Floodgate está instalado y funcionando correctamente.");
        }
    }

    /**
     * Verifica si un jugador es un jugador de Bedrock conectado a través de Geyser/Floodgate.
     *
     * @param player El jugador a verificar.
     * @return true si el jugador es de Bedrock, false en caso contrario o si Floodgate no está disponible.
     */
    public boolean isBedrockPlayer(Player player) {
        if (!floodgateAvailable || player == null) {
            return false;
        }
        return floodgateApi.isFloodgatePlayer(player.getUniqueId());
    }

    /**
     * Verifica si un jugador es un jugador de Bedrock conectado a través de Geyser/Floodgate, usando su UUID.
     *
     * @param playerUuid El UUID del jugador a verificar.
     * @return true si el jugador es de Bedrock, false en caso contrario o si Floodgate no está disponible.
     */
    public boolean isBedrockPlayer(UUID playerUuid) {
        if (!floodgateAvailable || playerUuid == null) {
            return false;
        }
        return floodgateApi.isFloodgatePlayer(playerUuid);
    }

    /**
     * Indica si la API de Floodgate está disponible y funcionando.
     * @return true si Floodgate está disponible, false en caso contrario.
     */
    public boolean isFloodgateAvailable() {
        return floodgateAvailable;
    }
}
