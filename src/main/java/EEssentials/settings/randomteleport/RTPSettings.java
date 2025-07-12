package EEssentials.settings.randomteleport;

import EEssentials.EEssentials;
import EEssentials.config.Configuration;
import net.minecraft.server.world.ServerWorld;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class RTPSettings {
    private static final Map<String, RTPWorldSettings> worldSettings = new HashMap<>();
    private static int maxAttempts = 10;
    private static List<String> blacklistedBiomes;
    private static List<String> unsafeBlocks;
    private static List<String> airBlocks;

    public static void reload(Configuration rtpConfig, Configuration mainConfig) {
        EEssentials.LOGGER.info("Reloading RTP settings...");
        worldSettings.clear();
        maxAttempts = rtpConfig.getInt("Random-Teleport.Max-Attempts", 10);
        EEssentials.LOGGER.info("Max Attempts: " + maxAttempts);

        unsafeBlocks = mainConfig.getStringList("Unsafe-Blocks");
        airBlocks = mainConfig.getStringList("Air-Blocks");
        EEssentials.LOGGER.info("Loaded unsafe blocks: " + unsafeBlocks);
        EEssentials.LOGGER.info("Loaded air blocks: " + airBlocks);


        Configuration randomTeleportConfig = rtpConfig.getSection("Random-Teleport");
        if (randomTeleportConfig == null) {
            EEssentials.LOGGER.error("No 'Random-Teleport' section found in RTP config.");
            return;
        }

        Configuration worldsConfig = randomTeleportConfig.getSection("Worlds");
        if (worldsConfig == null) {
            EEssentials.LOGGER.error("No 'Worlds' section found in RTP config.");
            return;
        }

        EEssentials.LOGGER.info("Worlds section keys: " + worldsConfig.getKeys());

        Map<String, String> redirectedWorlds = new HashMap<>();
        for (String worldName : worldsConfig.getKeys()) {
            EEssentials.LOGGER.info("Loading config for world: " + worldName);
            Configuration worldConfig = worldsConfig.getSection(worldName);
            if (worldConfig != null) {
                if (!worldConfig.contains("Redirect-To")) {
                    worldSettings.put(worldName, new RTPWorldSettings(worldName, worldConfig));
                    EEssentials.LOGGER.info("Loaded settings for world: " + worldName);
                } else {
                    String redirectTo = worldConfig.getString("Redirect-To");
                    redirectedWorlds.put(worldName, redirectTo);
                    EEssentials.LOGGER.info("World " + worldName + " redirects to " + redirectTo);
                }
            }
        }

        for (Map.Entry<String, String> redirect : redirectedWorlds.entrySet()) {
            worldSettings.put(redirect.getKey(), worldSettings.get(redirect.getValue()));
            EEssentials.LOGGER.info("Applied redirection for world: " + redirect.getKey() + " to " + redirect.getValue());
        }

        EEssentials.LOGGER.info("World settings loaded: " + worldSettings.keySet());

        blacklistedBiomes = randomTeleportConfig.getStringList("Blacklisted-Biomes");
        EEssentials.LOGGER.info("Loaded blacklisted biomes: " + blacklistedBiomes);
    }


    public static int getMaxAttempts() {
        return maxAttempts;
    }

    public static RTPWorldSettings getWorldSettings(ServerWorld world) {
        String worldName = world.getRegistryKey().getValue().toString();
        return getWorldSettings(worldName);
    }

    public static RTPWorldSettings getWorldSettings(String worldName) {
        RTPWorldSettings settings = worldSettings.get(worldName);
        if (settings == null) {
            EEssentials.LOGGER.error("No settings found for world: " + worldName);
        }
        return settings;
    }


    public static boolean isBiomeBlacklisted(String biomeKey) {
        return blacklistedBiomes.contains(biomeKey);
    }

    public static Set<String> getAllWorlds() {
        return worldSettings.keySet();
    }
}
