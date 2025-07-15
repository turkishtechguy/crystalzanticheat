package com.salih.anticheat.managers; // CORRECTED PACKAGE NAME

import com.salih.anticheat.CrystalzPvP_Anticheat; // CORRECTED IMPORT
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PunishmentManager {

    private final CrystalzPvP_Anticheat plugin;
    private final File bansFile;
    private FileConfiguration bansConfig;
    private final File historyFile;
    private FileConfiguration historyConfig;

    // Stores active bans: UUID -> BanEntry
    private final Map<UUID, BanEntry> playerBans;
    // Stores active IP bans: IP Address -> BanEntry
    private final Map<String, BanEntry> ipBans;
    // Stores punishment history: UUID -> List of PunishmentRecord
    private final Map<UUID, List<PunishmentRecord>> punishmentHistory;

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public PunishmentManager(CrystalzPvP_Anticheat plugin) {
        this.plugin = plugin;
        this.bansFile = new File(plugin.getDataFolder(), "bans.yml");
        this.historyFile = new File(plugin.getDataFolder(), "history.yml");
        this.playerBans = new ConcurrentHashMap<>();
        this.ipBans = new ConcurrentHashMap<>();
        this.punishmentHistory = new ConcurrentHashMap<>();
    }

    public void loadBans() {
        if (!bansFile.exists()) {
            plugin.getLogger().info("Creating bans.yml...");
            try {
                bansFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create bans.yml: " + e.getMessage());
            }
        }
        bansConfig = YamlConfiguration.loadConfiguration(bansFile);

        playerBans.clear();
        ipBans.clear();

        // Load player bans
        if (bansConfig.isConfigurationSection("player-bans")) {
            for (String uuidStr : Objects.requireNonNull(bansConfig.getConfigurationSection("player-bans")).getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(uuidStr);
                    String path = "player-bans." + uuidStr + ".";
                    String playerName = bansConfig.getString(path + "name", "Unknown");
                    String reason = bansConfig.getString(path + "reason", "No reason specified.");
                    long expires = bansConfig.getLong(path + "expires", -1);
                    String banner = bansConfig.getString(path + "banner", "Console");
                    long date = bansConfig.getLong(path + "date", System.currentTimeMillis());

                    // Only load if not expired
                    if (expires == -1 || expires > System.currentTimeMillis()) {
                        playerBans.put(uuid, new BanEntry(uuid, playerName, reason, expires, banner, date));
                    } else {
                        // Clean up expired bans from file
                        bansConfig.set(path, null);
                    }
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid UUID in bans.yml: " + uuidStr);
                }
            }
        }

        // Load IP bans
        if (bansConfig.isConfigurationSection("ip-bans")) {
            for (String ip : Objects.requireNonNull(bansConfig.getConfigurationSection("ip-bans")).getKeys(false)) {
                String path = "ip-bans." + ip + ".";
                String playerName = bansConfig.getString(path + "name", "Unknown"); // Player associated with the IP ban
                String reason = bansConfig.getString(path + "reason", "No reason specified.");
                long expires = bansConfig.getLong(path + "expires", -1);
                String banner = bansConfig.getString(path + "banner", "Console");
                long date = bansConfig.getLong(path + "date", System.currentTimeMillis());

                // Only load if not expired
                if (expires == -1 || expires > System.currentTimeMillis()) {
                    ipBans.put(ip, new BanEntry(null, playerName, reason, expires, banner, date)); // UUID is null for IP bans
                } else {
                    // Clean up expired IP bans from file
                    bansConfig.set(path, null);
                }
            }
        }
        saveBans(); // Save to remove expired bans from file
        plugin.getLogger().info("Loaded " + playerBans.size() + " player bans and " + ipBans.size() + " IP bans.");
    }

    public void saveBans() {
        bansConfig.set("player-bans", null); // Clear existing to rewrite
        bansConfig.set("ip-bans", null); // Clear existing to rewrite

        for (Map.Entry<UUID, BanEntry> entry : playerBans.entrySet()) {
            String path = "player-bans." + entry.getKey().toString() + ".";
            BanEntry ban = entry.getValue();
            bansConfig.set(path + "name", ban.getPlayerName());
            bansConfig.set(path + "reason", ban.getReason());
            bansConfig.set(path + "expires", ban.getExpires());
            bansConfig.set(path + "banner", ban.getBanner());
            bansConfig.set(path + "date", ban.getDate());
        }

        for (Map.Entry<String, BanEntry> entry : ipBans.entrySet()) {
            String path = "ip-bans." + entry.getKey() + ".";
            BanEntry ban = entry.getValue();
            bansConfig.set(path + "name", ban.getPlayerName()); // Player associated with IP ban
            bansConfig.set(path + "reason", ban.getReason());
            bansConfig.set(path + "expires", ban.getExpires());
            bansConfig.set(path + "banner", ban.getBanner());
            bansConfig.set(path + "date", ban.getDate());
        }

        try {
            bansConfig.save(bansFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save bans.yml: " + e.getMessage());
        }
    }

    public void loadHistory() {
        if (!historyFile.exists()) {
            plugin.getLogger().info("Creating history.yml...");
            try {
                historyFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create history.yml: " + e.getMessage());
            }
        }
        historyConfig = YamlConfiguration.loadConfiguration(historyFile);
        punishmentHistory.clear();

        if (historyConfig.isConfigurationSection("history")) {
            for (String uuidStr : Objects.requireNonNull(historyConfig.getConfigurationSection("history")).getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(uuidStr);
                    List<PunishmentRecord> records = new ArrayList<>();
                    List<?> rawRecords = historyConfig.getList("history." + uuidStr);
                    if (rawRecords != null) {
                        for (Object obj : rawRecords) {
                            if (obj instanceof Map<?, ?> map) {
                                String type = (String) map.get("type");
                                String reason = (String) map.get("reason");
                                String banner = (String) map.get("banner");
                                long date = (long) map.get("date");
                                records.add(new PunishmentRecord(type, reason, banner, date));
                            }
                        }
                    }
                    punishmentHistory.put(uuid, records);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid UUID in history.yml: " + uuidStr);
                }
            }
        }
        plugin.getLogger().info("Loaded history for " + punishmentHistory.size() + " players.");
    }

    public void saveHistory() {
        historyConfig.set("history", null); // Clear existing to rewrite
        for (Map.Entry<UUID, List<PunishmentRecord>> entry : punishmentHistory.entrySet()) {
            List<Map<String, Object>> serializedRecords = new ArrayList<>();
            for (PunishmentRecord record : entry.getValue()) {
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("type", record.getType());
                map.put("reason", record.getReason());
                map.put("banner", record.getBanner());
                map.put("date", record.getDate());
                serializedRecords.add(map);
            }
            historyConfig.set("history." + entry.getKey().toString(), serializedRecords);
        }
        try {
            historyConfig.save(historyFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save history.yml: " + e.getMessage());
        }
    }


    /**
     * Adds a player ban.
     * @param uuid The UUID of the player.
     * @param playerName The name of the player.
     * @param reason The reason for the ban.
     * @param durationMillis The duration in milliseconds (-1 for permanent).
     * @param banner The name of the player or console who issued the ban.
     */
    public void addBan(UUID uuid, String playerName, String reason, long durationMillis, String banner) {
        long expires = durationMillis == -1 ? -1 : System.currentTimeMillis() + durationMillis;
        BanEntry ban = new BanEntry(uuid, playerName, reason, expires, banner, System.currentTimeMillis());
        playerBans.put(uuid, ban);
        addPunishmentRecord(uuid, "Player Ban", reason, banner);
        saveBans();
        saveHistory();
    }

    /**
     * Adds an IP ban.
     * @param ipAddress The IP address to ban.
     * @param playerName The name of the player associated with this IP ban (for context).
     * @param reason The reason for the ban.
     * @param durationMillis The duration in milliseconds (-1 for permanent).
     * @param banner The name of the player or console who issued the ban.
     */
    public void addIpBan(String ipAddress, String playerName, String reason, long durationMillis, String banner) {
        long expires = durationMillis == -1 ? -1 : System.currentTimeMillis() + durationMillis;
        BanEntry ban = new BanEntry(null, playerName, reason, expires, banner, System.currentTimeMillis()); // UUID is null for IP ban
        ipBans.put(ipAddress, ban);
        // We don't add IP bans to player history directly, as it's IP-based.
        // If you want to link it, you'd need to track player-IP associations.
        saveBans();
    }

    /**
     * Unbans a player by UUID.
     * @param uuid The UUID of the player to unban.
     * @return true if the player was banned and is now unbanned, false otherwise.
     */
    public boolean unban(UUID uuid) {
        if (playerBans.containsKey(uuid)) {
            playerBans.remove(uuid);
            addPunishmentRecord(uuid, "Unban", "Unbanned by staff", "Console/Staff"); // Record unban
            saveBans();
            saveHistory();
            return true;
        }
        return false;
    }

    /**
     * Unbans an IP address.
     * @param ipAddress The IP address to unban.
     * @return true if the IP was banned and is now unbanned, false otherwise.
     */
    public boolean unbanIp(String ipAddress) {
        if (ipBans.containsKey(ipAddress)) {
            ipBans.remove(ipAddress);
            saveBans();
            return true;
        }
        return false;
    }


    /**
     * Checks if a player is currently banned.
     * Also handles expiration of temporary bans.
     * @param uuid The UUID of the player.
     * @return true if the player is banned, false otherwise.
     */
    public boolean isBanned(UUID uuid) {
        BanEntry ban = playerBans.get(uuid);
        if (ban == null) {
            return false;
        }
        if (ban.getExpires() != -1 && ban.getExpires() <= System.currentTimeMillis()) {
            // Ban has expired, remove it
            playerBans.remove(uuid);
            saveBans(); // Save to update file
            return false;
        }
        return true;
    }

    /**
     * Checks if an IP address is currently banned.
     * @param ipAddress The IP address.
     * @return true if the IP is banned, false otherwise.
     */
    public boolean isIpBanned(String ipAddress) {
        BanEntry ban = ipBans.get(ipAddress);
        if (ban == null) {
            return false;
        }
        if (ban.getExpires() != -1 && ban.getExpires() <= System.currentTimeMillis()) {
            // Ban has expired, remove it
            ipBans.remove(ipAddress);
            saveBans(); // Save to update file
            return false;
        }
        return true;
    }

    public BanEntry getBanInfo(UUID uuid) {
        return playerBans.get(uuid);
    }

    public BanEntry getIpBanInfo(String ipAddress) {
        return ipBans.get(ipAddress);
    }

    private void addPunishmentRecord(UUID uuid, String type, String reason, String banner) {
        punishmentHistory.computeIfAbsent(uuid, k -> new ArrayList<>())
                .add(new PunishmentRecord(type, reason, banner, System.currentTimeMillis()));
    }

    public List<PunishmentRecord> getHistory(UUID uuid) {
        return punishmentHistory.getOrDefault(uuid, Collections.emptyList());
    }

    public static class BanEntry {
        private final UUID uuid; // Null for IP bans
        private final String playerName; // Associated player name (for IP bans too)
        private final String reason;
        private final long expires; // -1 for permanent, otherwise epoch milliseconds
        private final String banner;
        private final long date; // When the ban was issued

        public BanEntry(UUID uuid, String playerName, String reason, long expires, String banner, long date) {
            this.uuid = uuid;
            this.playerName = playerName;
            this.reason = reason;
            this.expires = expires;
            this.banner = banner;
            this.date = date;
        }

        public UUID getUuid() {
            return uuid;
        }

        public String getPlayerName() {
            return playerName;
        }

        public String getReason() {
            return reason;
        }

        public long getExpires() {
            return expires;
        }

        public String getBanner() {
            return banner;
        }

        public long getDate() {
            return date;
        }

        public String getFormattedDate() {
            return DATE_FORMAT.format(new Date(date));
        }
    }

    public static class PunishmentRecord {
        private final String type; // e.g., "Player Ban", "Temporary Ban", "IP Ban", "Unban"
        private final String reason;
        private final String banner;
        private final long date;

        public PunishmentRecord(String type, String reason, String banner, long date) {
            this.type = type;
            this.reason = reason;
            this.banner = banner;
            this.date = date;
        }

        public String getType() {
            return type;
        }

        public String getReason() {
            return reason;
        }

        public String getBanner() {
            return banner;
        }

        public String getDate() {
            return DATE_FORMAT.format(new Date(date));
        }
    }
}
