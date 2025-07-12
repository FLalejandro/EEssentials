package EEssentials.util.importers;

import EEssentials.EEssentials;
import EEssentials.storage.PlayerStorage;
import EEssentials.util.Location;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.WorldSavePath;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.UUID;

public class EssentialCommandsImporter {

    /**
     * Returns either players EssentialCommands save data file (if it exists), or null otherwise.
     * @param uuid
     * @return File (EssentialCommands player save data) / Null if it doesn't exist
     */
    public static File getECPlayerFile(UUID uuid) {
        Path dataFilePath = EEssentials.server.getSavePath(WorldSavePath.ROOT).resolve("modplayerdata/" + uuid + ".dat");
        if (!dataFilePath.toFile().exists())  {
            // this is the backup location EC uses if it fails to create a folder inside the WorldSavePath.ROOT
            dataFilePath = Paths.get("./world/modplayerdata/" + uuid + ".dat");
        }
        if (!dataFilePath.toFile().exists()) {
            return null;
        }
        return dataFilePath.toFile();
    }

    public static File getECWorlDataFile() {
        File saveFile = EEssentials.server.getSavePath(WorldSavePath.ROOT).resolve("essentialcommands").resolve("world_data.dat").toFile();
        if (saveFile.exists()) {
            return saveFile;
        } else {
            return null;
        }
    }

    public static File getECPlayerFile(ServerPlayerEntity player) {
        return getECPlayerFile(player.getUuid());
    }

    public static HashMap<String, Location> loadHomesFromNbt(NbtElement nbt) {
        HashMap<String, Location> importedHomes = new HashMap<>();
        if (nbt.getType() == 9) {
            NbtList homesNbtList = (NbtList) nbt;
            for (NbtElement t : homesNbtList) {
                NbtCompound homeTag = (NbtCompound) t;
                String homeName = homeTag.getString("homeName").orElseThrow();
                Location location = Location.fromEssentialCommandsNbt(homeTag);
                importedHomes.put(homeName, location);
                EEssentials.LOGGER.info("Imported Home (" + homeName + ") with Location: " + location.toString());
            }
        } else {
            NbtCompound nbtCompound = (NbtCompound) nbt;
            nbtCompound.getKeys().forEach((key) -> {
                Location loc = Location.fromEssentialCommandsNbt(nbtCompound.getCompound(key).orElseThrow());
                importedHomes.put(key, loc);
                EEssentials.LOGGER.info("Imported Home (" + key + ") with Location: " + loc.toString());
            });
        }
        return importedHomes;
    }

    public static HashMap<String, Location> loadWarpsFromNbt(NbtElement nbt) {
        HashMap<String, Location> importedWarps = new HashMap<>();
        if (nbt.getType() == 9) {
            NbtList homesNbtList = (NbtList) nbt;
            for (NbtElement t : homesNbtList) {
                NbtCompound homeTag = (NbtCompound) t;
                String name = homeTag.getString("homeName").orElseThrow();
                Location loc = Location.fromEssentialCommandsNbt(homeTag);
                importedWarps.put(name, loc);
                EEssentials.LOGGER.info("Imported Warp (" + name + ") with Location: " + loc.toString());
            }
        } else {
            NbtCompound nbtCompound = (NbtCompound) nbt;
            nbtCompound.getKeys().forEach((key) -> {
                Location loc = Location.fromEssentialCommandsNbt(nbtCompound.getCompound(key).orElseThrow());
                importedWarps.put(key, loc);
                EEssentials.LOGGER.info("Imported Warp (" + key + ") with Location: " + loc.toString());
            });
        }
        return importedWarps;
    }

    public static void loadEssentialCommandsWorldData() {
        File dataFile = getECWorlDataFile();
        if (dataFile == null) {
            EEssentials.LOGGER.info("No Essential Commands World Data to import.");
            return;
        }

        /*
        try {
            NbtCompound nbtData = NbtIo.readCompressed(dataFile).getCompound("data");
            Location spawnLoc = Location.fromEssentialCommandsNbt(nbtData.getCompound("spawn"));
            if (!spawnLoc.getWorld().getRegistryKey().getValue().getPath().isEmpty()) {
                EEssentials.storage.locationManager.serverSpawn = spawnLoc;
                EEssentials.storage.locationManager.save();
                EEssentials.LOGGER.info("Imported Spawn Location from Essential Commands: " + spawnLoc.toString());
            }
            HashMap<String, Location> warps = loadWarpsFromNbt(nbtData.getCompound("warps"));
            for (String importedWarp: warps.keySet()) {
                if (EEssentials.storage.locationManager.warps.containsKey(importedWarp)) {
                    EEssentials.storage.locationManager.warps.put(importedWarp + "-imported", warps.get(importedWarp));
                } else {
                    EEssentials.storage.locationManager.warps.put(importedWarp, warps.get(importedWarp));
                }
            }
            EEssentials.storage.locationManager.save();
        } catch (IOException e) {
            EEssentials.LOGGER.error("Error when trying to read Essential Commands World Data...");
            return;
        }*/
    }

    /**
     * Loads relevant data from EssentialCommands into PlayerStorage if present
     * @param storage PlayerStorage
     */
    public static void loadEssentialCommandsPlayerData(PlayerStorage storage) {
        File ecFile = getECPlayerFile(storage.playerUUID);
        if (ecFile == null) {
            return;
        }
/*
        try {
            NbtCompound nbtData = NbtIo.readCompressed(new FileInputStream(ecFile));
            NbtElement homeData = nbtData.getCompound("data").get("homes");
            if (homeData == null) {
                return;
            }
            HashMap<String, Location> loadedHomes = loadHomesFromNbt(homeData);
            for (String home: loadedHomes.keySet()) {
                if (storage.homes.containsKey(home)) {
                    storage.homes.put(home + "-imported", loadedHomes.get(home));
                } else {
                    storage.homes.put(home, loadedHomes.get(home));
                }
            }
            storage.save();
        } catch (IOException e) {
            EEssentials.LOGGER.warn("Failed to load data from EssentialCommands for importing. Player File: " + storage.playerUUID);
        }*/

    }

}