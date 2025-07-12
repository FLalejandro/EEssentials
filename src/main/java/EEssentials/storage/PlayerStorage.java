package EEssentials.storage;

import EEssentials.EEssentials;
import EEssentials.util.Location;
import EEssentials.util.cereal.LocationDeserializer;
import EEssentials.util.cereal.LocationSerializer;
import EEssentials.util.cereal.ServerWorldDeserializer;
import EEssentials.util.cereal.ServerWorldSerializer;
import com.google.common.reflect.TypeToken;
import com.google.gson.*;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

import java.io.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class PlayerStorage {

    public final UUID playerUUID;
    private String playerName;
    public final HashMap<String, Location> homes = new HashMap<>();
    public Boolean playedBefore = false;
    public Boolean socialSpyFlag = false;
    private Location previousLocation = null;
    private Location logoutLocation = null;
    private int totalPlaytime = 0;
    private Instant lastTimeOnline = Instant.now();
    public List<String> modImports = new ArrayList<>();

    /**
     * Constructor to initialize PlayerStorage with a given UUID.
     *
     * @param uuid the UUID of the player.
     */
    public PlayerStorage(UUID uuid) {
        this.playerUUID = uuid;
        this.playedBefore = false;
        this.lastTimeOnline = Instant.now();
        
        // Only load data if the mod is fully initialized to prevent data corruption
        if (EEssentials.isModFullyInitialized()) {
            this.load();
        } else {
            EEssentials.LOGGER.info("Skipping PlayerStorage load for " + uuid + " - mod not fully initialized");
        }
    }


    /**
     * Fetch player storage for a given online player entity.
     *
     * @param player the player entity.
     * @return the PlayerStorage instance for the player.
     */
    public static PlayerStorage fromPlayer(ServerPlayerEntity player) {
        PlayerStorage storage = new PlayerStorage(player.getUuid());
        storage.playerName = player.getName().getString();
        storage.save();
        return storage;
    }

    /**
     * Fetch player storage using a player's UUID.
     *
     * @param uuid the UUID of the player.
     * @return the PlayerStorage instance if exists, null otherwise.
     */
    public static PlayerStorage fromPlayerUUID(UUID uuid) {
        File file = StorageManager.playerStorageDirectory.resolve(uuid.toString() + ".json").toFile();
        if (!file.exists()) {
            return null;  // Return null if there's no data file for the given UUID.
        }
        return new PlayerStorage(uuid);
    }

    /**
     * Get the storage file associated with the player's UUID.
     *
     * @return the storage file.
     */
    public File getSaveFile() {
        File file = StorageManager.playerStorageDirectory.resolve(playerUUID.toString() + ".json").toFile();
        try {
            playedBefore = !file.createNewFile();
        } catch (IOException e) {
            EEssentials.LOGGER.error("Failed to create file for PlayerStorage /w UUID: " + playerUUID.toString());
        }
        return file;
    }

    /**
     * Creates a custom GSON parser with required type adapters.
     *
     * @return the custom GSON parser.
     */
    private Gson createCustomGson() {
        GsonBuilder builder = new GsonBuilder();
        builder.setPrettyPrinting();
        builder.registerTypeAdapter(ServerWorld.class, new ServerWorldSerializer());
        builder.registerTypeAdapter(ServerWorld.class, new ServerWorldDeserializer());
        builder.registerTypeAdapter(Location.class, new LocationSerializer());
        builder.registerTypeAdapter(Location.class, new LocationDeserializer());
        return builder.create();
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public UUID getPlayerUUID() {
        return this.playerUUID;
    }


    public void setPreviousLocation(Location location) {
        this.previousLocation = location;
        this.save();
    }

    public Location getPreviousLocation() {
        return this.previousLocation;
    }

    public void setLogoutLocation(Location location) {
        this.logoutLocation = location;
        this.save();
    }

    public Location getLogoutLocation() {
        return this.logoutLocation;
    }

    public int getTotalPlaytime() {
        return totalPlaytime;
    }

    public void setTotalPlaytime(int totalPlaytime) {
        this.totalPlaytime = totalPlaytime;
        this.save();
    }
    public void setLastTimeOnline() {
        this.lastTimeOnline = Instant.now();
        this.save();
    }

    public Instant getLastTimeOnline() {
        return this.lastTimeOnline;
    }

    public Boolean getSocialSpyFlag(){ return this.socialSpyFlag; }

    /**
     * Save player data to storage.
     */
    public void save() {
        Gson gson = createCustomGson();

        try (Writer writer = new FileWriter(getSaveFile())) {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("playerName", playerName);  // Save the player name
            jsonObject.add("homes", gson.toJsonTree(homes));
            jsonObject.add("previousLocation", gson.toJsonTree(previousLocation));
            jsonObject.add("logoutLocation", gson.toJsonTree(logoutLocation));
            jsonObject.addProperty("totalPlaytime", totalPlaytime);
            jsonObject.add("lastTimeOnline", gson.toJsonTree(lastTimeOnline.toString()));
            jsonObject.addProperty("socialSpyFlag", socialSpyFlag);
            jsonObject.add("modImports", gson.toJsonTree(modImports));
            gson.toJson(jsonObject, writer);
        } catch (IOException e) {
            EEssentials.LOGGER.error("Failed to save data for UUID: " + playerUUID.toString(), e);
        }
    }

    public void load() {
        Gson gson = createCustomGson();

        try (Reader reader = new FileReader(getSaveFile())) {
            JsonObject jsonObject = gson.fromJson(reader, JsonObject.class);

            // Check if jsonObject is null and if so, initialize it
            if (jsonObject == null) {
                jsonObject = new JsonObject();
            }

            boolean requiresSave = false; // Flag to indicate if the data should be saved after loading

            // Load player name
            if (jsonObject.has("playerName")) {
                playerName = jsonObject.get("playerName").getAsString();
            } else {
                playerName = "Unknown"; // or some other default value
                requiresSave = true;    // Set the flag to save since playerName was missing
            }

            // Load homes data if available
            if (jsonObject.has("homes")) {
                HashMap<String, Location> loadedHomes = gson.fromJson(jsonObject.get("homes"), new TypeToken<HashMap<String, Location>>() {}.getType());
                if (loadedHomes != null) {
                    homes.clear();
                    homes.putAll(loadedHomes);
                }
            }

            if (jsonObject.has("previousLocation")) {
                previousLocation = gson.fromJson(jsonObject.get("previousLocation"), Location.class);
            } else {
                requiresSave = true;
            }

            if (jsonObject.has("logoutLocation")) {
                logoutLocation = gson.fromJson(jsonObject.get("logoutLocation"), Location.class);
            } else {
                requiresSave = true;
            }

            if (jsonObject.has("totalPlaytime")) {
                totalPlaytime = jsonObject.get("totalPlaytime").getAsInt();
            } else {
                totalPlaytime = 0;  // Default playtime for older files
                requiresSave = true;
            }

            if (jsonObject.has("lastTimeOnline")) {
                lastTimeOnline = Instant.parse(jsonObject.get("lastTimeOnline").getAsString());
            } else {
                lastTimeOnline = Instant.now();
                requiresSave = true;
            }

            // Load or initialize socialSpyFlag
            if (jsonObject.has("socialSpyFlag")) {
                socialSpyFlag = jsonObject.get("socialSpyFlag").getAsBoolean();
            } else {
                socialSpyFlag = false; // Default value if the flag isn't present
                requiresSave = true;
            }

            // Track MODIDs of mods that we've already imported data from for this player
            if (jsonObject.has("modImports")) {
                modImports.clear();
                modImports = gson.fromJson(jsonObject.get("modImports"), new TypeToken<ArrayList<String>>() {}.getType());
            }

            if (requiresSave) {
                save();
            }

        } catch (NullPointerException | IOException | JsonParseException e) {
            EEssentials.LOGGER.info("Failed to load data from file: " + getSaveFile().getName());
        }
    }

}


