package com.oblivion.geysermenubridge.command;

import com.oblivion.geysermenubridge.GeyserMenuBridge;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;

public class CommandManager implements CommandExecutor {

    private final GeyserMenuBridge plugin;
    private boolean debugMode = false; // Estado inicial del modo debug

    public CommandManager(GeyserMenuBridge plugin) {
        this.plugin = plugin;
        PluginCommand gmbDebugCommand = plugin.getCommand("gmbdebug");
        if (gmbDebugCommand != null) {
            gmbDebugCommand.setExecutor(this);
        } else {
            plugin.getLogger().warning("El comando gmbdebug no está definido en plugin.yml");
        }

        PluginCommand testMenuCommand = plugin.getCommand("testmenu");
        if (testMenuCommand != null) {
            testMenuCommand.setExecutor(this);
        } else {
            plugin.getLogger().warning("El comando testmenu no está definido en plugin.yml");
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("gmbdebug")) {
            if (!sender.hasPermission("geysermenubridge.debug")) {
                sender.sendMessage(plugin.getConfig().getString("messages.no-permission", "§cNo tienes permiso para usar este comando."));
                return true;
            }

            debugMode = !debugMode;
            String statusMessage = plugin.getConfig().getString(debugMode ? "messages.debug-enabled" : "messages.debug-disabled",
                                    "§aGeyserMenuBridge debug mode: " + (debugMode ? "§2ACTIVADO" : "§cDESACTIVADO"));
            sender.sendMessage(statusMessage);
            plugin.getLogger().info("GeyserMenuBridge debug mode: " + (debugMode ? "ACTIVADO" : "DESACTIVADO") + " por " + sender.getName());

            // Debugging extra para detección de plataforma
            if (sender instanceof Player) {
                Player player = (Player) sender;
                boolean isBedrock = plugin.getPlayerPlatformDetector().isBedrockPlayer(player);
                boolean floodgateAvailable = plugin.getPlayerPlatformDetector().isFloodgateAvailable();

                String bedrockStatus = isBedrock ? "§aBEDROCK" : "§cJAVA";
                String floodgateStatus = floodgateAvailable ? "§aSÍ" : "§cNO";

                sender.sendMessage("§e[GMB DEBUG] Tu detección de plataforma: " + bedrockStatus);
                sender.sendMessage("§e[GMB DEBUG] Floodgate API disponible: " + floodgateStatus);

                plugin.getLogger().info(String.format("[GMB DEBUG] Chequeo de plataforma para %s: Es Bedrock: %b, Floodgate API Disponible: %b",
                                        player.getName(), isBedrock, floodgateAvailable));
            }
            return true;
        }

        if (command.getName().equalsIgnoreCase("testmenu")) {
            if (!sender.hasPermission("geysermenubridge.testmenu")) {
                sender.sendMessage(plugin.getConfig().getString("messages.no-permission", "§cNo tienes permiso para usar este comando."));
                return true;
            }
            if (!(sender instanceof Player)) {
                sender.sendMessage(plugin.getConfig().getString("messages.player-only", "§cEste comando solo puede ser ejecutado por un jugador."));
                return true;
            }
            Player player = (Player) sender;

            plugin.getLogger().info(String.format("[CommandManager DEBUG] Comando /testmenu ejecutado por %s.", player.getName()));
            if (plugin.getPlayerPlatformDetector() != null) {
                 boolean isBedrock = plugin.getPlayerPlatformDetector().isBedrockPlayer(player);
                 plugin.getLogger().info(String.format("[CommandManager DEBUG] Verificación de plataforma para %s en /testmenu: Es Bedrock: %b", player.getName(), isBedrock));
            } else {
                plugin.getLogger().warning("[CommandManager DEBUG] PlayerPlatformDetector es null en /testmenu.");
            }

            player.sendMessage("§e[GeyserMenuBridge] §7Abriendo menú de prueba...");

            plugin.getLogger().info(String.format("[CommandManager DEBUG] A punto de llamar a TestMenu.openTestMenu() para %s.", player.getName()));
            com.oblivion.geysermenubridge.menus.TestMenu.openTestMenu(player, plugin); // Pasar instancia del plugin
            plugin.getLogger().info(String.format("[CommandManager DEBUG] TestMenu.openTestMenu() ejecutado para %s.", player.getName()));
            return true;
        }
        return false;
    }

    public boolean isDebugMode() {
        return debugMode;
    }
}
