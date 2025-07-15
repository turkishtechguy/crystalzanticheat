package com.salih.anticheat.gui; // CORRECTED PACKAGE NAME

import com.salih.anticheat.CrystalzPvP_Anticheat; // CORRECTED IMPORT
import com.salih.anticheat.managers.SuspectManager; // CORRECTED IMPORT
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class SuspectsGUI implements Listener {

    private final CrystalzPvP_Anticheat plugin;
    private final SuspectManager suspectManager;

    public SuspectsGUI(CrystalzPvP_Anticheat plugin, SuspectManager suspectManager) {
        this.plugin = plugin;
        this.suspectManager = suspectManager;
    }

    public void openSuspectsGUI(Player player) {
        // Create a double chest inventory (54 slots)
        Inventory gui = Bukkit.createInventory(null, 54, Component.text("Suspicious Players").color(NamedTextColor.DARK_RED));

        Map<UUID, Map<String, Integer>> suspects = suspectManager.getAllSuspects();
        int slot = 0;

        for (Map.Entry<UUID, Map<String, Integer>> entry : suspects.entrySet()) {
            if (slot >= 54) break; // Stop if GUI is full (no pagination for now)

            UUID suspectUUID = entry.getKey();
            Map<String, Integer> reasons = entry.getValue();
            String playerName = suspectManager.getPlayerName(suspectUUID); // Get the stored name

            // Create player head item
            ItemStack playerHead = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) playerHead.getItemMeta();

            // Set player head owner
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(suspectUUID);
            meta.setOwningPlayer(offlinePlayer);

            // Set display name
            meta.displayName(Component.text(playerName).color(NamedTextColor.AQUA));

            // Set lore with detection reasons and counts
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("--- Detections ---").color(NamedTextColor.GRAY));
            if (reasons.isEmpty()) {
                lore.add(Component.text("No specific reasons recorded.").color(NamedTextColor.GRAY));
            } else {
                reasons.forEach((reason, count) -> {
                    lore.add(Component.text("- ").color(NamedTextColor.WHITE)
                            .append(Component.text(reason).color(NamedTextColor.RED))
                            .append(Component.text(" (" + count + "x)").color(NamedTextColor.YELLOW)));
                });
            }
            meta.lore(lore);

            playerHead.setItemMeta(meta);
            gui.setItem(slot, playerHead);
            slot++;
        }

        player.openInventory(gui);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        // Check if the clicked inventory is our Suspects GUI
        if (event.getView().title().equals(Component.text("Suspicious Players").color(NamedTextColor.DARK_RED))) {
            event.setCancelled(true); // Prevent players from taking items out of the GUI
            // You could add logic here for clicking on heads (e.g., to ban, check more info)
            // For now, it's just a display.
        }
    }
}
