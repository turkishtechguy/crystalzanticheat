package com.salih.anticheat.listeners; // CORRECTED PACKAGE NAME

import com.salih.anticheat.CrystalzPvP_Anticheat; // CORRECTED IMPORT
import com.salih.anticheat.managers.PunishmentManager; // CORRECTED IMPORT
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.Objects;
import java.util.UUID;

public class PlayerJoinListener implements Listener {

    private final CrystalzPvP_Anticheat plugin;
    private final PunishmentManager punishmentManager;

    public PlayerJoinListener(CrystalzPvP_Anticheat plugin, PunishmentManager punishmentManager) {
        this.plugin = plugin;
        this.punishmentManager = punishmentManager;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        UUID playerUUID = event.getPlayer().getUniqueId();
        String playerIP = Objects.requireNonNull(event.getPlayer().getAddress()).getAddress().getHostAddress();

        // Check for player ban
        if (punishmentManager.isBanned(playerUUID)) {
            PunishmentManager.BanEntry ban = punishmentManager.getBanInfo(playerUUID);
            if (ban != null) {
                event.getPlayer().kick(formatBanMessage(ban));
                plugin.getLogger().info("Kicked banned player " + event.getPlayer().getName() + " (UUID: " + playerUUID + ")");
                return; // Player is banned, no need to check IP ban for this player
            }
        }

        // Check for IP ban
        if (punishmentManager.isIpBanned(playerIP)) {
            PunishmentManager.BanEntry ipBan = punishmentManager.getIpBanInfo(playerIP);
            if (ipBan != null) {
                event.getPlayer().kick(formatBanMessage(ipBan));
                plugin.getLogger().info("Kicked IP banned player " + event.getPlayer().getName() + " (IP: " + playerIP + ")");
            }
        }
    }

    private Component formatBanMessage(PunishmentManager.BanEntry ban) {
        Component message = Component.text("You are banned from this server!").color(NamedTextColor.RED)
                .append(Component.newline())
                .append(Component.text("Reason: ").color(NamedTextColor.YELLOW).append(Component.text(ban.getReason()).color(NamedTextColor.WHITE)))
                .append(Component.newline())
                .append(Component.text("Banned by: ").color(NamedTextColor.YELLOW).append(Component.text(ban.getBanner()).color(NamedTextColor.WHITE)));

        if (ban.getExpires() != -1) {
            long remainingTime = ban.getExpires() - System.currentTimeMillis();
            if (remainingTime > 0) {
                long seconds = remainingTime / 1000;
                long minutes = seconds / 60;
                long hours = minutes / 60;
                long days = hours / 24;

                String timeLeft;
                if (days > 0) {
                    timeLeft = days + "d " + (hours % 24) + "h";
                } else if (hours > 0) {
                    timeLeft = hours + "h " + (minutes % 60) + "m";
                } else if (minutes > 0) {
                    timeLeft = minutes + "m " + (seconds % 60) + "s";
                } else {
                    timeLeft = seconds + "s";
                }
                message = message.append(Component.newline())
                        .append(Component.text("Expires in: ").color(NamedTextColor.YELLOW).append(Component.text(timeLeft).color(NamedTextColor.WHITE)));
            } else {
                // Ban has expired, but player was still kicked (should be handled by unban logic)
                // This case ideally shouldn't happen if unban is called on expiration.
                message = message.append(Component.newline())
                        .append(Component.text("This ban has expired. Please try again.").color(NamedTextColor.GREEN));
            }
        } else {
            message = message.append(Component.newline())
                    .append(Component.text("This is a permanent ban.").color(NamedTextColor.WHITE));
        }
        return message;
    }
}
