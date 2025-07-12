package EEssentials.storage;

import EEssentials.EEssentials;
//import EEssentials.util.importers.EssentialCommandsImporter;
import net.minecraft.server.network.ServerPlayerEntity;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.UUID;

public class StorageManager {

    public final Path storageDirectory;
    public static Path playerStorageDirectory;

    public final LocationManager locationManager;
    private final HashMap<UUID, PlayerStorage> playerStores = new HashMap<>();

    public StorageManager(Path storageDirectory) {
        this.storageDirectory = storageDirectory;
        initializePlayerStorageDirectory(storageDirectory);
        this.locationManager = new LocationManager(storageDirectory.resolve("Locations.json"));
    }

    // Static method to initialize playerStorageDirectory
    private static void initializePlayerStorageDirectory(Path storageDirectory) {
        if (playerStorageDirectory == null) {
            playerStorageDirectory = storageDirectory.resolve("player");
            playerStorageDirectory.toFile().mkdirs();
        }
    }

    public void serverStarted() {
        this.locationManager.load();
    }

    public PlayerStorage getPlayerStorage(UUID uuid) {
        // Don't create PlayerStorage if mod isn't fully initialized
        if (!EEssentials.isModFullyInitialized()) {
            return null;
        }
        return playerStores.getOrDefault(uuid, new PlayerStorage(uuid));
    }

    public PlayerStorage getPlayerStorage(ServerPlayerEntity player) {
        return getPlayerStorage(player.getUuid());
    }

    /**
     * Force load player storage data. This should be called when the mod is fully initialized
     * and we need to ensure player data is loaded.
     *
     * @param uuid the UUID of the player.
     * @return the PlayerStorage instance for the player.
     */
    public PlayerStorage forceLoadPlayerStorage(UUID uuid) {
        if (!EEssentials.isModFullyInitialized()) {
            EEssentials.LOGGER.warn("Attempted to force load PlayerStorage before mod initialization complete");
            return null;
        }
        
        PlayerStorage storage = new PlayerStorage(uuid);
        playerStores.put(uuid, storage);
        return storage;
    }

    public void playerJoined(ServerPlayerEntity player) {
        PlayerStorage pStorage = playerStores.getOrDefault(player.getUuid(), PlayerStorage.fromPlayer(player));
        if (!pStorage.modImports.contains("essential_commands")) {
            EEssentials.LOGGER.info("Importing data from EssentialCommands for player: " + player.getGameProfile().getName());
            //EssentialCommandsImporter.loadEssentialCommandsPlayerData(pStorage);
            pStorage.modImports.add("essential_commands");
            pStorage.save();
        }
        playerStores.putIfAbsent(player.getUuid(), pStorage);
    }

    public void playerLeft(ServerPlayerEntity player) {
        playerStores.remove(player.getUuid());
    }

}
