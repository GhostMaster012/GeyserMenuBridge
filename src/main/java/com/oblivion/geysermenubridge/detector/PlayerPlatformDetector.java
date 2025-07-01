package com.oblivion.geysermenubridge.detector;

import com.oblivion.geysermenubridge.GeyserMenuBridge;
import org.bukkit.entity.Player;
import org.geysermc.floodgate.api.FloodgateApi;

import java.util.UUID;

public class PlayerPlatformDetector {

    private final FloodgateApi floodgateApi;
    private final boolean floodgateAvailable; // Hacer también final para consistencia

    public PlayerPlatformDetector(GeyserMenuBridge plugin) {
        FloodgateApi tempApi = null;
        boolean tempAvailable = false;
        try {
            tempApi = FloodgateApi.getInstance();
            tempAvailable = true;
            plugin.getLogger().info("Floodgate API encontrada. La detección de jugadores Bedrock está activa.");
        } catch (NoClassDefFoundError | IllegalStateException e) {
            // Capturar específicamente NoClassDefFoundError si Floodgate no está en el classpath,
            // o IllegalStateException si la API no está lista o ya fue instanciada incorrectamente.
            plugin.getLogger().warning("Floodgate API no encontrada o no pudo ser inicializada. La detección de jugadores Bedrock no funcionará.");
            plugin.getLogger().warning("Detalles del error: " + e.getMessage());
            // tempApi ya es null, tempAvailable ya es false
        } catch (Exception e) { // Captura genérica para cualquier otro error inesperado
            plugin.getLogger().severe("Error inesperado al intentar inicializar Floodgate API: " + e.getMessage());
            e.printStackTrace(); // Loguear el stacktrace completo para errores inesperados
            // tempApi ya es null, tempAvailable ya es false
        }

        this.floodgateApi = tempApi;
        this.floodgateAvailable = tempAvailable;
    }

    /**
     * Verifica si un jugador es un jugador de Bedrock conectado a través de Geyser/Floodgate.
     *
     * @param player El jugador a verificar.
     * @return true si el jugador es de Bedrock, false en caso contrario o si Floodgate no está disponible.
     */
    public boolean isBedrockPlayer(Player player) {
        if (!this.floodgateAvailable || player == null || this.floodgateApi == null) { // Chequeo extra para floodgateApi
            return false;
        }
        return this.floodgateApi.isFloodgatePlayer(player.getUniqueId());
    }

    /**
     * Verifica si un jugador es un jugador de Bedrock conectado a través de Geyser/Floodgate, usando su UUID.
     *
     * @param playerUuid El UUID del jugador a verificar.
     * @return true si el jugador es de Bedrock, false en caso contrario o si Floodgate no está disponible.
     */
    public boolean isBedrockPlayer(UUID playerUuid) {
        if (!this.floodgateAvailable || playerUuid == null || this.floodgateApi == null) { // Chequeo extra para floodgateApi
            return false;
        }
        return this.floodgateApi.isFloodgatePlayer(playerUuid);
    }

    /**
     * Indica si la API de Floodgate está disponible y funcionando.
     * @return true si Floodgate está disponible, false en caso contrario.
     */
    public boolean isFloodgateAvailable() {
        return this.floodgateAvailable;
    }
}
