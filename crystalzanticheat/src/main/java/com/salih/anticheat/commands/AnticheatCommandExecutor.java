package com.salih.anticheat.commands; // CORRECTED PACKAGE NAME

import com.salih.anticheat.CrystalzPvP_Anticheat; // CORRECTED IMPORT
import com.salih.anticheat.managers.ConfigManager; // CORRECTED IMPORT
import com.salih.anticheat.managers.PunishmentManager; // CORRECTED IMPORT
import com.salih.anticheat.managers.SuspectManager; // CORRECTED IMPORT
import com.salih.anticheat.managers.WebhookManager; // CORRECTED IMPORT
import com.salih.anticheat.gui.SuspectsGUI; // CORRECTED IMPORT
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class AnticheatCommandExecutor implements CommandExecutor {

    private final CrystalzPvP_Anticheat plugin;
    private final PunishmentManager punishmentManager;
    private final SuspectManager suspectManager;
    private final WebhookManager webhookManager;
    private final ConfigManager configManager;
    private final SuspectsGUI suspectsGUI;

    public AnticheatCommandExecutor(CrystalzPvP_Anticheat plugin, PunishmentManager punishmentManager, SuspectManager suspectManager, WebhookManager webhookManager, ConfigManager configManager, SuspectsGUI suspectsGUI) {
        this.plugin = plugin;
        this.punishmentManager = punishmentManager;
        this.suspectManager = suspectManager;
        this.webhookManager = webhookManager;
        this.configManager = configManager;
        this.suspectsGUI = suspectsGUI;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String commandName = command.getName().toLowerCase();

        switch (commandName) {
            case "ban":
                return handleBanCommand(sender, args, false);
            case "tempban":
                return handleBanCommand(sender, args, true);
            case "banip":
                return handleIpBanCommand(sender, args, false);
            case "tempbanip":
                return handleIpBanCommand(sender, args, true);
            case "unban":
                return handleUnbanCommand(sender, args);
            case "check":
                return handleCheckCommand(sender, args);
            case "sus":
                return handleSusCommand(sender);
            case "setup":
                return handleSetupCommand(sender, args);
            case "webhookcheck":
                return handleWebhookCheckCommand(sender);
            case "addadmin":
                return handleAddAdminCommand(sender, args);
            case "anticheat": // Handle subcommands for /anticheat
                if (args.length > 0 && args[0].equalsIgnoreCase("notify")) {
                    return handleNotifyCommand(sender, args);
                } else {
                    sender.sendMessage(Component.text("Usage: /anticheat notify <player> <reason>").color(NamedTextColor.RED));
                    return true;
                }
            default:
                return false; // Should not happen if commands are registered correctly
        }
    }

    private boolean handleBanCommand(CommandSender sender, String[] args, boolean isTemp) {
        if (args.length < (isTemp ? 2 : 1)) {
            sender.sendMessage(Component.text("Usage: /" + (isTemp ? "tempban" : "ban") + " <username> " + (isTemp ? "<duration> " : "") + "[reason]").color(NamedTextColor.RED));
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        if (target == null) {
            sender.sendMessage(Component.text("Player not found: " + args[0]).color(NamedTextColor.RED));
            return true;
        }

        String reason = "No reason specified.";
        long durationMillis = -1; // -1 for permanent

        if (isTemp) {
            try {
                durationMillis = parseDuration(args[1]);
            } catch (IllegalArgumentException e) {
                sender.sendMessage(Component.text("Invalid duration format. Use e.g., 1h, 2d, 3m.").color(NamedTextColor.RED));
                return true;
            }
            if (args.length > 2) {
                reason = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
            }
        } else {
            if (args.length > 1) {
                reason = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
            }
        }

        String banner = sender.getName();
        punishmentManager.addBan(target.getUniqueId(), target.getName(), reason, durationMillis, banner);
        sender.sendMessage(Component.text("Player " + target.getName() + " has been " + (isTemp ? "temporarily " : "") + "banned.").color(NamedTextColor.GREEN));

        // Notify webhook
        String banType = isTemp ? "Temporary Ban" : "Permanent Ban";
        String durationText = isTemp ? " for " + args[1] : "";
        Component webhookMessageComponent = Component.text("[CrystalzPvP] ")
                .color(NamedTextColor.DARK_PURPLE)
                .append(Component.text(target.getName()))
                .color(NamedTextColor.AQUA)
                .append(Component.text(" has been " + banType + " by " + banner + durationText + " for: "))
                .append(Component.text(reason).color(NamedTextColor.RED).decorate(TextDecoration.BOLD));
        webhookManager.sendMessage(PlainTextComponentSerializer.plainText().serialize(webhookMessageComponent));
        return true;
    }

    private boolean handleIpBanCommand(CommandSender sender, String[] args, boolean isTemp) {
        if (args.length < (isTemp ? 2 : 1)) {
            sender.sendMessage(Component.text("Usage: /" + (isTemp ? "tempbanip" : "banip") + " <username> " + (isTemp ? "<duration> " : "") + "[reason]").color(NamedTextColor.RED));
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        if (target == null) {
            sender.sendMessage(Component.text("Player not found: " + args[0]).color(NamedTextColor.RED));
            return true;
        }

        // Get the last known IP address of the player
        String ipAddress = null;
        if (target.isOnline()) {
            ipAddress = Objects.requireNonNull(target.getPlayer()).getAddress().getAddress().getHostAddress();
        } else {
            sender.sendMessage(Component.text("Cannot get IP for offline player " + target.getName() + ". Player must be online to ban IP.").color(NamedTextColor.RED));
            return true;
        }

        if (ipAddress == null) {
            sender.sendMessage(Component.text("Could not determine IP address for " + target.getName()).color(NamedTextColor.RED));
            return true;
        }

        String reason = "No reason specified.";
        long durationMillis = -1; // -1 for permanent

        if (isTemp) {
            try {
                durationMillis = parseDuration(args[1]);
            } catch (IllegalArgumentException e) {
                sender.sendMessage(Component.text("Invalid duration format. Use e.g., 1h, 2d, 3m.").color(NamedTextColor.RED));
                return true;
            }
            if (args.length > 2) {
                reason = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
            }
        } else {
            if (args.length > 1) {
                reason = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
            }
        }

        String banner = sender.getName();
        punishmentManager.addIpBan(ipAddress, target.getName(), reason, durationMillis, banner);
        sender.sendMessage(Component.text("IP " + ipAddress + " (associated with " + target.getName() + ") has been " + (isTemp ? "temporarily " : "") + "banned.").color(NamedTextColor.GREEN));

        // Notify webhook
        String banType = isTemp ? "Temporary IP Ban" : "Permanent IP Ban";
        String durationText = isTemp ? " for " + args[1] : "";
        Component webhookMessageComponent = Component.text("[CrystalzPvP] ")
                .color(NamedTextColor.DARK_PURPLE)
                .append(Component.text(target.getName()))
                .color(NamedTextColor.AQUA)
                .append(Component.text(" (IP: " + ipAddress + ") has been " + banType + " by " + banner + durationText + " for: "))
                .append(Component.text(reason).color(NamedTextColor.RED).decorate(TextDecoration.BOLD));
        webhookManager.sendMessage(PlainTextComponentSerializer.plainText().serialize(webhookMessageComponent));
        return true;
    }

    private boolean handleUnbanCommand(CommandSender sender, String[] args) {
        if (args.length != 1) {
            sender.sendMessage(Component.text("Usage: /unban <username>").color(NamedTextColor.RED));
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        if (target == null) {
            sender.sendMessage(Component.text("Player not found: " + args[0]).color(NamedTextColor.RED));
            return true;
        }

        if (punishmentManager.unban(target.getUniqueId())) {
            sender.sendMessage(Component.text("Player " + target.getName() + " has been unbanned.").color(NamedTextColor.GREEN));
            Component webhookMessageComponent = Component.text("[CrystalzPvP] ")
                    .color(NamedTextColor.DARK_PURPLE)
                    .append(Component.text(target.getName()))
                    .color(NamedTextColor.AQUA)
                    .append(Component.text(" has been unbanned by " + sender.getName() + "."));
            webhookManager.sendMessage(PlainTextComponentSerializer.plainText().serialize(webhookMessageComponent));
        } else {
            sender.sendMessage(Component.text("Player " + target.getName() + " is not currently banned.").color(NamedTextColor.RED));
        }
        return true;
    }

    private boolean handleCheckCommand(CommandSender sender, String[] args) {
        if (args.length != 1) {
            sender.sendMessage(Component.text("Usage: /check <username>").color(NamedTextColor.RED));
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        if (target == null) {
            sender.sendMessage(Component.text("Player not found: " + args[0]).color(NamedTextColor.RED));
            return true;
        }

        sender.sendMessage(Component.text("--- Punishment History for " + target.getName() + " ---").color(NamedTextColor.GOLD));
        punishmentManager.getHistory(target.getUniqueId()).forEach(record -> {
            sender.sendMessage(Component.text("- Type: " + record.getType() + ", Reason: " + record.getReason() + ", By: " + record.getBanner() + ", Date: " + record.getDate()));
        });
        if (punishmentManager.getHistory(target.getUniqueId()).isEmpty()) {
            sender.sendMessage(Component.text("No punishment history found for " + target.getName() + ".").color(NamedTextColor.GRAY));
        }
        return true;
    }

    private boolean handleSusCommand(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use this command.").color(NamedTextColor.RED));
            return true;
        }
        suspectsGUI.openSuspectsGUI(player);
        return true;
    }

    private boolean handleSetupCommand(CommandSender sender, String[] args) {
        if (args.length != 1) {
            sender.sendMessage(Component.text("Usage: /setup <webhookurl>").color(NamedTextColor.RED));
            return true;
        }
        String url = args[0];
        configManager.setWebhookUrl(url);
        webhookManager.setWebhookUrl(url);
        sender.sendMessage(Component.text("Webhook URL set successfully!").color(NamedTextColor.GREEN));
        return true;
    }

    private boolean handleWebhookCheckCommand(CommandSender sender) {
        String url = configManager.getWebhookUrl();
        if (url == null || url.isEmpty()) {
            sender.sendMessage(Component.text("Webhook URL is not set. Use /setup <webhookurl>.").color(NamedTextColor.RED));
            return true;
        }
        sender.sendMessage(Component.text("Attempting to send a test message to the webhook...").color(NamedTextColor.YELLOW));
        Component webhookMessageComponent = Component.text("[CrystalzPvP] ")
                .color(NamedTextColor.DARK_PURPLE)
                .append(Component.text("This is a test message from CrystalzPvP_Anticheat!"));
        webhookManager.sendMessage(PlainTextComponentSerializer.plainText().serialize(webhookMessageComponent));
        sender.sendMessage(Component.text("Test message sent. Check your webhook channel.").color(NamedTextColor.GREEN));
        return true;
    }

    private boolean handleAddAdminCommand(CommandSender sender, String[] args) {
        if (args.length != 1) {
            sender.sendMessage(Component.text("Usage: /addadmin <username>").color(NamedTextColor.RED));
            return true;
        }
        if (!(sender instanceof Player player) || !player.isOp()) {
            sender.sendMessage(Component.text("Only OPs can use this command.").color(NamedTextColor.RED));
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        if (target == null || target.getUniqueId() == null) {
            sender.sendMessage(Component.text("Player not found: " + args[0]).color(NamedTextColor.RED));
            return true;
        }

        if (configManager.addAdmin(target.getUniqueId())) {
            sender.sendMessage(Component.text(target.getName() + " has been added as an Anticheat admin.").color(NamedTextColor.GREEN));
        } else {
            sender.sendMessage(Component.text(target.getName() + " is already an Anticheat admin.").color(NamedTextColor.YELLOW));
        }
        return true;
    }

    private boolean handleNotifyCommand(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(Component.text("Usage: /anticheat notify <player> <reason>").color(NamedTextColor.RED));
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        if (target == null || target.getUniqueId() == null) {
            sender.sendMessage(Component.text("Player not found: " + args[1]).color(NamedTextColor.RED));
            return true;
        }

        String reason = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
        suspectManager.addDetection(target.getUniqueId(), target.getName(), reason);
        int detectionCount = suspectManager.getDetectionCount(target.getUniqueId(), reason);

        // Broadcast notification to online admins
        Component notification = Component.text("[CrystalzPvP] ")
                .color(NamedTextColor.DARK_PURPLE)
                .append(Component.text(target.getName()))
                .color(NamedTextColor.AQUA)
                .append(Component.text(" has been detected for "))
                .append(Component.text(reason).color(NamedTextColor.RED).decorate(TextDecoration.BOLD))
                .append(Component.text(". [" + detectionCount + "x]"));

        for (UUID adminUUID : configManager.getAdminUUIDs()) {
            Player adminPlayer = Bukkit.getPlayer(adminUUID);
            if (adminPlayer != null && adminPlayer.isOnline()) {
                adminPlayer.sendMessage(notification);
            }
        }
        sender.sendMessage(Component.text("Notification sent for " + target.getName() + " (" + reason + ").").color(NamedTextColor.GREEN));

        // Send to webhook
        webhookManager.sendMessage(PlainTextComponentSerializer.plainText().serialize(notification));
        return true;
    }

    /**
     * Parses a duration string (e.g., "1h", "2d", "3m") into milliseconds.
     * Throws IllegalArgumentException for invalid formats.
     */
    private long parseDuration(String durationString) {
        long duration = 0;
        char unit = durationString.charAt(durationString.length() - 1);
        int value = Integer.parseInt(durationString.substring(0, durationString.length() - 1));

        switch (unit) {
            case 'h':
                duration = TimeUnit.HOURS.toMillis(value);
                break;
            case 'd':
                duration = TimeUnit.DAYS.toMillis(value);
                break;
            case 'm': // months - approximate 30 days per month
                duration = TimeUnit.DAYS.toMillis(value * 30L);
                break;
            default:
                throw new IllegalArgumentException("Invalid duration unit: " + unit);
        }
        return duration;
    }
}
