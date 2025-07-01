package com.oblivion.geysermenubridge;

import org.bukkit.plugin.java.JavaPlugin;
import java.util.logging.Logger;

public final class GeyserMenuBridge extends JavaPlugin {

    private static GeyserMenuBridge instance;
    private Logger logger;
    private com.oblivion.geysermenubridge.detector.PlayerPlatformDetector playerPlatformDetector;
    private com.oblivion.geysermenubridge.listener.PacketListener packetListener;
    private com.oblivion.geysermenubridge.bridge.BedrockMenuManager bedrockMenuManager;
    private com.oblivion.geysermenubridge.listener.InventoryListener inventoryListener;
    private com.oblivion.geysermenubridge.command.CommandManager commandManager;

    @Override
    public void onEnable() {
        instance = this;
        logger = getLogger();

        // Guardar configuración por defecto si no existe
        // saveDefaultConfig(); // Comentado para evitar error si config.yml no está en resources

        logger.info("==================================================");
        logger.info("GeyserMenuBridge ha sido HABILITADO!");
        logger.info("Desarrollado por: JulesAI y Oblivion");
        logger.info("Versión: " + getDescription().getVersion());
        logger.info("Detectando dependencias...");

        if (getServer().getPluginManager().getPlugin("ProtocolLib") == null) {
            logger.severe("**************************************************");
            logger.severe("¡ProtocolLib no encontrado! GeyserMenuBridge no puede funcionar sin ProtocolLib.");
            logger.severe("Por favor, descarga ProtocolLib e instálalo en tu servidor.");
            logger.severe("GeyserMenuBridge se desactivará.");
            logger.severe("**************************************************");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        logger.info("- ProtocolLib encontrado y cargado.");

        // Inicializar componentes principales
        playerPlatformDetector = new com.oblivion.geysermenubridge.detector.PlayerPlatformDetector(this);
        bedrockMenuManager = new com.oblivion.geysermenubridge.bridge.BedrockMenuManager(this);
        commandManager = new com.oblivion.geysermenubridge.command.CommandManager(this); // Asignar a la variable de instancia
        inventoryListener = new com.oblivion.geysermenubridge.listener.InventoryListener(this);

        // Inicializar el listener de paquetes si Floodgate está disponible
        if (playerPlatformDetector.isFloodgateAvailable()) {
            // Pasar BedrockMenuManager al PacketListener
            packetListener = new com.oblivion.geysermenubridge.listener.PacketListener(this, playerPlatformDetector, bedrockMenuManager);
            logger.info("PacketListener inicializado.");
        } else {
            logger.warning("PacketListener no se inicializará porque Floodgate API no está disponible.");
        }


        if (!playerPlatformDetector.isFloodgateAvailable()) {
            logger.severe("**************************************************");
            logger.severe("Floodgate API no está disponible. GeyserMenuBridge podría no funcionar como se espera para jugadores Bedrock.");
            logger.severe("Asegúrate de que Floodgate (parte de Geyser) está instalado y correctamente configurado.");
            logger.severe("**************************************************");
        }

        logger.info("GeyserMenuBridge cargado y listo para funcionar.");
        logger.info("==================================================");
    }

    @Override
    public void onDisable() {
        logger.info("==================================================");
        logger.info("GeyserMenuBridge ha sido DESHABILITADO.");

        if (packetListener != null) {
            packetListener.unregisterListeners();
        }

        logger.info("==================================================");
    }

    public static GeyserMenuBridge getInstance() {
        return instance;
    }

    public com.oblivion.geysermenubridge.detector.PlayerPlatformDetector getPlayerPlatformDetector() {
        return playerPlatformDetector;
    }

    public com.oblivion.geysermenubridge.bridge.BedrockMenuManager getBedrockMenuManager() {
        return bedrockMenuManager;
    }

    public com.oblivion.geysermenubridge.command.CommandManager getCommandManager() {
        return commandManager;
    }
}
