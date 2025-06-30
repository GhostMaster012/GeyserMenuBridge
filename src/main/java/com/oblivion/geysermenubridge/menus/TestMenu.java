package com.oblivion.geysermenubridge.menus;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

public class TestMenu {

    public static void openTestMenu(Player player) {
        Inventory testInventory = Bukkit.createInventory(null, 27, "§1Menú de Prueba §bGeyserMB");

        ItemStack diamond = new ItemStack(Material.DIAMOND, 1);
        ItemMeta diamondMeta = diamond.getItemMeta();
        if (diamondMeta != null) {
            diamondMeta.setDisplayName("§bDiamante Brillante");
            diamondMeta.setLore(Arrays.asList("§7¡Un diamante de prueba!", "§7Haz clic para nada útil."));
            diamond.setItemMeta(diamondMeta);
        }

        ItemStack emerald = new ItemStack(Material.EMERALD, 64);
        ItemMeta emeraldMeta = emerald.getItemMeta();
        if (emeraldMeta != null) {
            emeraldMeta.setDisplayName("§aEsmeraldas Verdes");
            emeraldMeta.setLore(Arrays.asList("§7Intercambia con aldeanos (no aquí).", "§cCantidad: ¡Máxima!"));
            emerald.setItemMeta(emeraldMeta);
        }

        ItemStack redstone = new ItemStack(Material.REDSTONE);
        ItemMeta redstoneMeta = redstone.getItemMeta();
        if (redstoneMeta != null) {
            redstoneMeta.setDisplayName("§cPolvo de §lRedstone§r §4(Modelo)");
            redstoneMeta.setCustomModelData(12345); // Ejemplo de CustomModelData
            redstoneMeta.setLore(Arrays.asList("§7Modelo Personalizado: §e12345"));
            redstone.setItemMeta(redstoneMeta);
        }

        ItemStack enchantedBook = new ItemStack(Material.ENCHANTED_BOOK);
        ItemMeta bookMeta = enchantedBook.getItemMeta();
        if (bookMeta instanceof org.bukkit.inventory.meta.EnchantmentStorageMeta) {
            org.bukkit.inventory.meta.EnchantmentStorageMeta esm = (org.bukkit.inventory.meta.EnchantmentStorageMeta) bookMeta;
            esm.setDisplayName("§dLibro Mágico §5§k!!");
            esm.addStoredEnchant(org.bukkit.enchantments.Enchantment.DAMAGE_ALL, 5, true);
            esm.addStoredEnchant(org.bukkit.enchantments.Enchantment.PROTECTION_ENVIRONMENTAL, 10, true); // Nivel alto
            esm.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS); // Ocultar encantamientos estándar
            esm.setLore(Arrays.asList("§7Encantamientos ocultos...", "§7Sharp V", "§7Prot X (ilegal)"));
            enchantedBook.setItemMeta(esm);
        }

        ItemStack potion = new ItemStack(Material.POTION);
        ItemMeta potionMeta = potion.getItemMeta();
        if (potionMeta instanceof org.bukkit.inventory.meta.PotionMeta) {
            org.bukkit.inventory.meta.PotionMeta pm = (org.bukkit.inventory.meta.PotionMeta) potionMeta;
            pm.setDisplayName("§aPoción Curiosa");
            pm.setColor(org.bukkit.Color.fromRGB(0, 255, 0)); // Verde brillante
            pm.addCustomEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.SPEED, 600, 2), true);
            pm.setLore(Arrays.asList("§7Velocidad III (0:30)"));
            potion.setItemMeta(pm);
        }

        ItemStack playerHead = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta headMeta = playerHead.getItemMeta();
        if (headMeta instanceof org.bukkit.inventory.meta.SkullMeta) {
            org.bukkit.inventory.meta.SkullMeta sm = (org.bukkit.inventory.meta.SkullMeta) headMeta;
            sm.setDisplayName("§eCabeza de Steve?");
            // Para que muestre una skin, necesitaría un PlayerProfile o un owningPlayer (deprecated)
            // O un Base64 de textura. Por ahora, será una cabeza de Steve por defecto.
            // sm.setOwningPlayer(Bukkit.getOfflinePlayer("Steve")); // Ejemplo
            sm.setLore(Arrays.asList("§7Una cabeza... de alguien."));
            playerHead.setItemMeta(sm);
        }

        ItemStack barrier = new ItemStack(Material.BARRIER);
        ItemMeta barrierMeta = barrier.getItemMeta();
        if (barrierMeta != null) {
            barrierMeta.setDisplayName("§cCERRAR");
            barrier.setItemMeta(barrierMeta);
        }

        // Fila 1
        testInventory.setItem(1, diamond);
        testInventory.setItem(4, emerald);
        testInventory.setItem(7, redstone);

        // Fila 2
        testInventory.setItem(10, enchantedBook);
        testInventory.setItem(13, potion);
        testInventory.setItem(16, playerHead);

        // Fila 3
        testInventory.setItem(22, barrier); // Centro inferior

        player.openInventory(testInventory);
    }
}
