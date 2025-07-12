package EEssentials.config;

import com.google.common.base.Charsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

public class ConfigVersionUpdater {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigVersionUpdater.class);
    private final Configuration mainConfig;
    private final Configuration langConfig;
    private final String currentVersion;

    public ConfigVersionUpdater(Configuration mainConfig, Configuration langConfig, String currentVersion) {
        this.mainConfig = mainConfig;
        this.langConfig = langConfig;
        this.currentVersion = currentVersion;
    }

    public void updateConfig() {
        String configVersion = mainConfig.getString("Config-Version", "1.0.0");

        if (isOlderVersion(configVersion, currentVersion)) {
            LOGGER.info("Updating config from version " + configVersion + " to " + currentVersion);

            Configuration defaultConfig = YamlConfiguration.loadConfiguration(
                    getClass().getClassLoader().getResourceAsStream("eessentials/config.yml"));
            mergeConfigs(mainConfig, defaultConfig);

            mainConfig.set("Config-Version", currentVersion);

            if (!mainConfig.contains("Commands.broadcast")) {
                mainConfig.set("Commands.broadcast", true);
            }
            if (!mainConfig.contains("Commands.enchantmenttable")) {
                mainConfig.set("Commands.enchantmenttable", true);
            }
            if (!mainConfig.contains("Commands.mail")) {
                mainConfig.set("Commands.mail", true);
            }
            if (!mainConfig.contains("Commands.nightvision")) {
                mainConfig.set("Commands.nightvision", true);
            }

            try {
                // Save the config with comments
                saveConfigWithComments(mainConfig, "eessentials/config.yml", new File("config/EEssentials/config.yml"));
            } catch (IOException e) {
                LOGGER.error("Failed to save updated config!", e);
            }
        }

        String langVersion = langConfig.getString("Config-Version", "1.0.0");

        if (isOlderVersion(langVersion, currentVersion)) {
            LOGGER.info("Updating lang config from version " + langVersion + " to " + currentVersion);

            Configuration defaultLangConfig = YamlConfiguration.loadConfiguration(
                    getClass().getClassLoader().getResourceAsStream("eessentials/lang.yml"));
            mergeConfigs(langConfig, defaultLangConfig);

            langConfig.set("Config-Version", currentVersion);

            try {
                // Save the lang config with comments
                saveConfigWithComments(langConfig, "eessentials/lang.yml", new File("config/EEssentials/lang.yml"));
            } catch (IOException e) {
                LOGGER.error("Failed to save updated lang config!", e);
            }
        }
    }

    private void mergeConfigs(Configuration target, Configuration source) {
        for (String key : source.getKeys()) {
            if (source.get(key) instanceof Configuration) {
                if (!(target.get(key) instanceof Configuration)) {
                    target.set(key, new Configuration());
                }
                mergeConfigs(target.getSection(key), source.getSection(key));
            } else {
                if (!target.contains(key)) {
                    target.set(key, source.get(key));
                }
            }
        }
    }

    private boolean isOlderVersion(String currentVersion, String targetVersion) {
        try {
            int[] currentParts = parseVersion(currentVersion);
            int[] targetParts = parseVersion(targetVersion);

            // Compare version parts, handling different lengths
            int maxLength = Math.max(currentParts.length, targetParts.length);
            for (int i = 0; i < maxLength; i++) {
                int currentPart = i < currentParts.length ? currentParts[i] : 0;
                int targetPart = i < targetParts.length ? targetParts[i] : 0;
                
                if (currentPart < targetPart) {
                    return true;
                } else if (currentPart > targetPart) {
                    return false;
                }
            }
            return false; // Versions are equal
        } catch (Exception e) {
            LOGGER.warn("Failed to parse version strings: current='{}', target='{}'. Using string comparison as fallback.", currentVersion, targetVersion);
            // Fallback to string comparison if parsing fails
            return currentVersion.compareTo(targetVersion) < 0;
        }
    }

    private int[] parseVersion(String version) {
        if (version == null || version.trim().isEmpty()) {
            return new int[]{0};
        }
        
        String[] parts = version.split("\\.");
        int[] numbers = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            try {
                numbers[i] = Integer.parseInt(parts[i].trim());
            } catch (NumberFormatException e) {
                LOGGER.warn("Invalid version part '{}' in version '{}', treating as 0", parts[i], version);
                numbers[i] = 0;
            }
        }
        return numbers;
    }

    private void saveConfigWithComments(Configuration config, String resourcePath, File file) throws IOException {
        // Load the default configuration from the resource file
        InputStream resourceStream = getClass().getClassLoader().getResourceAsStream(resourcePath);
        String defaultConfigContent = new String(resourceStream.readAllBytes(), Charsets.UTF_8);

        // Save the configuration to the file
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), Charsets.UTF_8)) {
            writer.write(defaultConfigContent);
        }
    }
}
