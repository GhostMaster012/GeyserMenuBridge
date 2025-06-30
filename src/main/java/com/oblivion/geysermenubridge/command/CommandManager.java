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
                sender.sendMessage("§cNo tienes permiso para usar este comando.");
                return true;
            }
            // Aquí podríamos añadir lógica para debuggear a un jugador específico si se proporciona args[0]
            debugMode = !debugMode;
            sender.sendMessage("§aGeyserMenuBridge debug mode: " + (debugMode ? "§2ACTIVADO" : "§cDESACTIVADO"));
            plugin.getLogger().info("GeyserMenuBridge debug mode: " + (debugMode ? "ACTIVADO" : "DESACTIVADO") + " por " + sender.getName());
            return true;
        }

        if (command.getName().equalsIgnoreCase("testmenu")) {
            if (!sender.hasPermission("geysermenubridge.testmenu")) {
                sender.sendMessage("§cNo tienes permiso para usar este comando.");
                return true;
            }
            if (!(sender instanceof Player)) {
                sender.sendMessage("§cEste comando solo puede ser ejecutado por un jugador.");
                return true;
            }
            Player player = (Player) sender;
            player.sendMessage("§e[GeyserMenuBridge] §7Abriendo menú de prueba...");
            com.oblivion.geysermenubridge.menus.TestMenu.openTestMenu(player);
            return true;
        }
        return false;
    }

    public boolean isDebugMode() {
        return debugMode;
    }
}
