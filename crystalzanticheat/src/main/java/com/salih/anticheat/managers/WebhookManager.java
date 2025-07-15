package com.salih.anticheat.managers; // CORRECTED PACKAGE NAME

import com.salih.anticheat.CrystalzPvP_Anticheat; // CORRECTED IMPORT
import org.bukkit.Bukkit; // Import Bukkit for asynchronous task scheduling

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class WebhookManager {

    private final CrystalzPvP_Anticheat plugin;
    private String webhookUrl;

    public WebhookManager(CrystalzPvP_Anticheat plugin) {
        this.plugin = plugin;
        this.webhookUrl = ""; // Default empty
    }

    public void setWebhookUrl(String webhookUrl) {
        this.webhookUrl = webhookUrl;
    }

    public String getWebhookUrl() {
        return webhookUrl;
    }

    /**
     * Sends a message to the configured webhook URL.
     * This method is asynchronous to prevent blocking the main server thread.
     * @param message The message to send. This should be a plain string.
     */
    public void sendMessage(String message) {
        if (webhookUrl == null || webhookUrl.isEmpty()) {
            plugin.getLogger().warning("Webhook URL is not set. Cannot send message: " + message);
            return;
        }

        // Run in a new thread to avoid blocking the server
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                URL url = new URL(webhookUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setDoOutput(true);

                // Basic JSON payload for Discord/Slack webhooks
                String jsonPayload = "{\"content\": \"" + escapeJson(message) + "\"}";

                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                int responseCode = connection.getResponseCode();
                if (responseCode >= 200 && responseCode < 300) {
                    plugin.getLogger().info("Webhook message sent successfully. Response Code: " + responseCode);
                } else {
                    plugin.getLogger().warning("Failed to send webhook message. Response Code: " + responseCode + ", Message: " + connection.getResponseMessage());
                }
                connection.disconnect();

            } catch (Exception e) {
                plugin.getLogger().severe("Error sending webhook message: " + e.getMessage());
            }
        });
    }

    // Helper to escape special characters for JSON
    private String escapeJson(String text) {
        return text.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }
}
