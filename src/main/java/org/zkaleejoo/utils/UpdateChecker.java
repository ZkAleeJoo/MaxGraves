package org.zkaleejoo.utils;

import org.bukkit.Bukkit;
import org.zkaleejoo.MaxGraves;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.function.Consumer;

public class UpdateChecker {

    private static final String GITHUB_VERSION_URL = "https://gist.githubusercontent.com/ZkAleeJoo/33cdd64e6c58a490e77284cf9fb1ceca/raw/MaxGraves-Version.txt";
    private final MaxGraves plugin;

    public UpdateChecker(MaxGraves plugin) {
        this.plugin = plugin;
    }

    public void getVersion(final Consumer<String> consumer) {
        Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> {
            try {
                URL url = new URL(GITHUB_VERSION_URL);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                
                connection.setRequestMethod("GET");
                connection.setRequestProperty("User-Agent", "MaxGraves-UpdateChecker");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);

                int statusCode = connection.getResponseCode();
                if (statusCode < 200 || statusCode >= 300) {
                    plugin.getLogger().warning("Could not check updates. HTTP " + statusCode);
                    return;
                }

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                    String latestVersion = reader.readLine();
                    if (latestVersion != null && !latestVersion.isBlank()) {
                        consumer.accept(latestVersion.trim());
                    } else {
                        plugin.getLogger().info("The version is empty.");
                    }
                }
            } catch (Exception exception) {
                plugin.getLogger().info("Could not connect to check for updates: " + exception.getMessage());
            }
        });
    }
}