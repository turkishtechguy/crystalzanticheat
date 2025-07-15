package com.salih.anticheat; // CORRECTED PACKAGE NAME

import com.salih.anticheat.commands.AnticheatCommandExecutor; // CORRECTED IMPORT
import com.salih.anticheat.listeners.PlayerJoinListener; // CORRECTED IMPORT
import com.salih.anticheat.managers.ConfigManager; // CORRECTED IMPORT
import com.salih.anticheat.managers.PunishmentManager; // CORRECTED IMPORT
import com.salih.anticheat.managers.SuspectManager; // CORRECTED IMPORT
import com.salih.anticheat.managers.WebhookManager; // CORRECTED IMPORT
import com.salih.anticheat.gui.SuspectsGUI; // CORRECTED IMPORT
import org.bukkit.plugin.java.JavaPlugin;
import java.util.Objects;

public class CrystalzPvP_Anticheat extends JavaPlugin {

    private ConfigManager configManager;
    private PunishmentManager punishmentManager;
    private SuspectManager suspectManager;
    private WebhookManager webhookManager;
    private SuspectsGUI suspectsGUI;

    @Override
    public void onEnable() {
        getLogger().info("CrystalzPvP_Anticheat plugin has been enabled!");

        // Initialize managers
        this.configManager = new ConfigManager(this);
        this.punishmentManager = new PunishmentManager(this);
        this.suspectManager = new SuspectManager(this);
        this.webhookManager = new WebhookManager(this);
        this.suspectsGUI = new SuspectsGUI(this, suspectManager);

        // Load data from files
        configManager.loadConfig();
        punishmentManager.loadBans();
        punishmentManager.loadHistory();
        suspectManager.loadSuspects();

        // Pass loaded webhook URL to WebhookManager
        webhookManager.setWebhookUrl(configManager.getWebhookUrl());

        // Register commands
        AnticheatCommandExecutor commandExecutor = new AnticheatCommandExecutor(this, punishmentManager, suspectManager, webhookManager, configManager, suspectsGUI);
        Objects.requireNonNull(this.getCommand("ban")).setExecutor(commandExecutor);
        Objects.requireNonNull(this.getCommand("tempban")).setExecutor(commandExecutor);
        Objects.requireNonNull(this.getCommand("banip")).setExecutor(commandExecutor);
        Objects.requireNonNull(this.getCommand("tempbanip")).setExecutor(commandExecutor);
        Objects.requireNonNull(this.getCommand("unban")).setExecutor(commandExecutor);
        Objects.requireNonNull(this.getCommand("check")).setExecutor(commandExecutor);
        Objects.requireNonNull(this.getCommand("sus")).setExecutor(commandExecutor);
        Objects.requireNonNull(this.getCommand("setup")).setExecutor(commandExecutor);
        Objects.requireNonNull(this.getCommand("webhookcheck")).setExecutor(commandExecutor);
        Objects.requireNonNull(this.getCommand("addadmin")).setExecutor(commandExecutor);
        Objects.requireNonNull(this.getCommand("anticheat")).setExecutor(commandExecutor); // For the 'notify' subcommand

        // Register event listeners
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this, punishmentManager), this);
        getServer().getPluginManager().registerEvents(suspectsGUI, this); // For GUI click events
    }

    @Override
    public void onDisable() {
        getLogger().info("CrystalzPvP_Anticheat plugin has been disabled!");

        // Save all data before shutdown
        configManager.saveConfig();
        punishmentManager.saveBans();
        punishmentManager.saveHistory();
        suspectManager.saveSuspects();
    }

    // Getters for managers (useful if other classes need access)
    public ConfigManager getConfigManager() {
        return configManager;
    }

    public PunishmentManager getPunishmentManager() {
        return punishmentManager;
    }

    public SuspectManager getSuspectManager() {
        return suspectManager;
    }

    public WebhookManager getWebhookManager() {
        return webhookManager;
    }

    public SuspectsGUI getSuspectsGUI() {
        return suspectsGUI;
    }
}
