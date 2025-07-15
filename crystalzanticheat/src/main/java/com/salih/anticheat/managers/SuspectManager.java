package com.salih.anticheat.managers; // CORRECTED PACKAGE NAME

import com.salih.anticheat.CrystalzPvP_Anticheat; // CORRECTED IMPORT
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SuspectManager {

    private final CrystalzPvP_Anticheat plugin;
    private final File suspectsFile;
    private FileConfiguration suspectsConfig;

    // Stores suspicious players: UUID -> Map<Reason, Count>
    private final Map<UUID, Map<String, Integer>> suspiciousPlayers;
    // Stores player names for UUIDs (for display purposes in GUI)
    private final Map<UUID, String> playerNames;

    public SuspectManager(CrystalzPvP_Anticheat plugin) {
        this.plugin = plugin;
        this.suspectsFile = new File(plugin.getDataFolder(), "suspects.yml");
        this.suspiciousPlayers = new ConcurrentHashMap<>();
        this.playerNames = new ConcurrentHashMap<>();
    }

    public void loadSuspects() {
        if (!suspectsFile.exists()) {
            plugin.getLogger().info("Creating suspects.yml...");
            try {
                suspectsFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create suspects.yml: " + e.getMessage());
            }
        }
        suspectsConfig = YamlConfiguration.loadConfiguration(suspectsFile);

        suspiciousPlayers.clear();
        playerNames.clear();

        if (suspectsConfig.isConfigurationSection("suspects")) {
            for (String uuidStr : Objects.requireNonNull(suspectsConfig.getConfigurationSection("suspects")).getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(uuidStr);
                    String path = "suspects." + uuidStr + ".";
                    String playerName = suspectsConfig.getString(path + "name", "Unknown");
                    playerNames.put(uuid, playerName);

                    Map<String, Integer> reasons = new HashMap<>();
                    if (suspectsConfig.isConfigurationSection(path + "reasons")) {
                        for (String reasonKey : Objects.requireNonNull(suspectsConfig.getConfigurationSection(path + "reasons")).getKeys(false)) {
                            reasons.put(reasonKey, suspectsConfig.getInt(path + "reasons." + reasonKey));
                        }
                    }
                    suspiciousPlayers.put(uuid, reasons);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid UUID in suspects.yml: " + uuidStr);
                }
            }
        }
        plugin.getLogger().info("Loaded " + suspiciousPlayers.size() + " suspicious players.");
    }

    public void saveSuspects() {
        suspectsConfig.set("suspects", null); // Clear existing to rewrite

        for (Map.Entry<UUID, Map<String, Integer>> entry : suspiciousPlayers.entrySet()) {
            String uuidStr = entry.getKey().toString();
            String path = "suspects." + uuidStr + ".";
            suspectsConfig.set(path + "name", playerNames.getOrDefault(entry.getKey(), "Unknown"));
            for (Map.Entry<String, Integer> reasonEntry : entry.getValue().entrySet()) {
                suspectsConfig.set(path + "reasons." + reasonEntry.getKey(), reasonEntry.getValue());
            }
        }

        try {
            suspectsConfig.save(suspectsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save suspects.yml: " + e.getMessage());
        }
    }

    /**
     * Records a detection for a player.
     * @param uuid The UUID of the player.
     * @param playerName The name of the player (for display).
     * @param reason The reason for the detection (e.g., "Killaura", "Fly").
     */
    public void addDetection(UUID uuid, String playerName, String reason) {
        playerNames.put(uuid, playerName); // Always update name in case it changed
        suspiciousPlayers.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>())
                .merge(reason, 1, Integer::sum); // Increment count for this reason
        saveSuspects();
    }

    /**
     * Gets all detection reasons and counts for a specific player.
     * @param uuid The UUID of the player.
     * @return A map of reasons to counts, or an empty map if no detections.
     */
    public Map<String, Integer> getDetections(UUID uuid) {
        return suspiciousPlayers.getOrDefault(uuid, Collections.emptyMap());
    }

    /**
     * Gets the count for a specific detection reason for a player.
     * @param uuid The UUID of the player.
     * @param reason The specific reason.
     * @return The count for that reason, or 0 if not detected for that reason.
     */
    public int getDetectionCount(UUID uuid, String reason) {
        return suspiciousPlayers.getOrDefault(uuid, Collections.emptyMap()).getOrDefault(reason, 0);
    }

    /**
     * Gets a map of all suspicious players with their detection reasons and counts.
     * @return A map of UUIDs to their detection maps.
     */
    public Map<UUID, Map<String, Integer>> getAllSuspects() {
        return new ConcurrentHashMap<>(suspiciousPlayers); // Return a copy
    }

    /**
     * Gets the last known name for a player UUID.
     * @param uuid The UUID of the player.
     * @return The player's name, or "Unknown" if not found.
     */
    public String getPlayerName(UUID uuid) {
        return playerNames.getOrDefault(uuid, "Unknown");
    }

    /**
     * Removes a player from the suspicious list.
     * @param uuid The UUID of the player to remove.
     * @return true if removed, false otherwise.
     */
    public boolean removeSuspect(UUID uuid) {
        if (suspiciousPlayers.remove(uuid) != null) {
            playerNames.remove(uuid);
            saveSuspects();
            return true;
        }
        return false;
    }
}
