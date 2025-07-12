/**
 * Represents a specific location in the Minecraft world, including world, coordinates, and optional orientation.
 * Inspired by NeoAPI.
 */
package EEssentials.util;

import EEssentials.EEssentials;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;

import java.util.Map;
import java.util.Set;

public class Location {
    private final ServerWorld world;  // The world where the location resides
    private final double x;           // The X-coordinate of the location
    private final double y;           // The Y-coordinate of the location
    private final double z;           // The Z-coordinate of the location
    private float pitch = -1000;      // The pitch (vertical orientation) at the location, default unset value is -1000
    private float yaw = -1000;        // The yaw (horizontal orientation) at the location, default unset value is -1000

    /**
     * Constructs a Location with given world and coordinates.
     */
    public Location(ServerWorld world, double x, double y, double z) {
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    /**
     * Constructs a Location with given world, coordinates, and orientation.
     */
    public Location(ServerWorld world, double x, double y, double z, float pitch, float yaw) {
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.pitch = pitch;
        this.yaw = yaw;
    }

    public static Location fromPlayer(ServerPlayerEntity player) {
        return new Location(player.getWorld(), player.getX(), player.getY(), player.getZ());
    }

    public static Location fromEssentialCommandsNbt(NbtCompound tag) {
        return new Location(
                EEssentials.server.getWorld(RegistryKey.of(
                        RegistryKeys.WORLD, 
                        Identifier.tryParse(tag.getString("WorldRegistryKey", "minecraft:overworld"))
                )),
                tag.getDouble("x").orElseThrow(),
                tag.getDouble("y").orElseThrow(),
                tag.getDouble("z").orElseThrow(),
                tag.getFloat("headYaw", 0f),
                tag.getFloat("pitch", 0f)
        );
    }

    @Override
    public String toString() {
        String worldName = this.world.getRegistryKey().getValue().toString();
        return "Location: World(" + worldName + "), "
                + "X: " + this.x + ", "
                + "Y: " + this.y + ", "
                + "Z: " + this.z + ", "
                + "Pitch: " + (this.pitch == -1000 ? "unset" : this.pitch) + ", "
                + "Yaw: " + (this.yaw == -1000 ? "unset" : this.yaw);
    }

    // Basic getter methods below...

    public ServerWorld getWorld() {
        return world;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public float getPitch() {
        return pitch;
    }

    public float getYaw() {
        return yaw;
    }

    /**
     * Teleports a player to this location.
     * If yaw or pitch is unset (i.e., -1000), it retains the player's current orientation.
     */
    public void teleport(ServerPlayerEntity player) {
        float tpPitch = (pitch != -1000) ? pitch : player.getPitch();
        float tpYaw = (yaw != -1000) ? yaw : player.getYaw();
        player.teleport(world, x, y, z, Set.of(PositionFlag.X_ROT, PositionFlag.Y_ROT), tpYaw, tpPitch, false);
    }

    /**
     * Checks if this location is equal to another in terms of world, coordinates, and orientation.
     */
    public boolean isEqualTo(Location location) {
        return (world.equals(location.getWorld()))
                && (x == location.getX())
                && (y == location.getY())
                && (z == location.getZ())
                && (yaw == location.getYaw())
                && (pitch == location.getPitch());
    }

    /**
     * Checks if this location has the same coordinates (and world) as another, but ignoring pitch and yaw.
     */
    public boolean isEqualToCoordinatesOf(Location location) {
        return (world.equals(location.getWorld()))
                && (x == location.getX())
                && (y == location.getY())
                && (z == location.getZ());
    }

    /**
     * Adds replacements from a location.
     * @param replacements the map which replacements should be added to.
     */

    public void addReplacements(Map<String, String> replacements) {
        replacements.put("{x}", String.valueOf(x));
        replacements.put("{y}", String.valueOf(y));
        replacements.put("{z}", String.valueOf(z));
        replacements.put("{pitch}", String.valueOf(pitch));
        replacements.put("{yaw}", String.valueOf(yaw));
        replacements.put("{world}", world.getRegistryKey().getValue().toString());
    }
}

