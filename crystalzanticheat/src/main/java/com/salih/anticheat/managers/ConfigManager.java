package com.salih.anticheat.managers; // CORRECTED PACKAGE NAME

import com.salih.anticheat.CrystalzPvP_Anticheat; // CORRECTED IMPORT
import org.bukkit.configuration.file.FileConfiguration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class ConfigManager {

    private final CrystalzPvP_Anticheat plugin;
    private FileConfiguration config;

    private String webhookUrl;
    private List<UUID> adminUUIDs;

    public ConfigManager(CrystalzPvP_Anticheat plugin) {
        this.plugin = plugin;
        this.adminUUIDs = new ArrayList<>();
    }

    public void loadConfig() {
        plugin.saveDefaultConfig(); // Creates config.yml if it doesn't exist
        this.config = plugin.getConfig();

        this.webhookUrl = config.getString("webhook-url", "");
        List<String> adminStrings = config.getStringList("admins");
        this.adminUUIDs = adminStrings.stream()
                .map(UUID::fromString)
                .collect(Collectors.toList());

        plugin.getLogger().info("Config loaded. Webhook: " + (webhookUrl.isEmpty() ? "Not Set" : "Set") + ", Admins: " + adminUUIDs.size());
    }

    public void saveConfig() {
        config.set("webhook-url", webhookUrl);
        config.set("admins", adminUUIDs.stream().map(UUID::toString).collect(Collectors.toList()));
        plugin.saveConfig();
        plugin.getLogger().info("Config saved.");
    }

    public String getWebhookUrl() {
        return webhookUrl;
    }

    public void setWebhookUrl(String webhookUrl) {
        this.webhookUrl = webhookUrl;
        saveConfig(); // Save immediately when webhook changes
    }

    public List<UUID> getAdminUUIDs() {
        return new ArrayList<>(adminUUIDs); // Return a copy to prevent external modification
    }

    public boolean addAdmin(UUID uuid) {
        if (!adminUUIDs.contains(uuid)) {
            adminUUIDs.add(uuid);
            saveConfig(); // Save immediately when admin list changes
            return true;
        }
        return false;
    }

    public boolean removeAdmin(UUID uuid) {
        if (adminUUIDs.remove(uuid)) {
            saveConfig(); // Save immediately when admin list changes
            return true;
        }
        return false;
    }

    public boolean isAdmin(UUID uuid) {
        return adminUUIDs.contains(uuid);
    }
}
