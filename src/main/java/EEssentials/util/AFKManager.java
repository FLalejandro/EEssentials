package EEssentials.util;

import EEssentials.config.Configuration;
import EEssentials.lang.LangManager;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.Vec3i;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AFKManager {

    // Permission node to check if a player is exempt from AFK kicking.
    public static final String AFK_KICKEXEMPT_PERMISSION_NODE = "eessentials.afk.kickexempt";

    // Storing players' AFK status, last activity time, and last known positions.
    private static final HashMap<UUID, Boolean> afkStatus = new HashMap<>();
    private static final HashMap<UUID, Long> lastActivity = new HashMap<>();
    private static final HashMap<UUID, Vec3i> lastKnownPositions = new HashMap<>();

    // AFK time thresholds in ticks (20 ticks = 1 second)
    private static long AFK_THRESHOLD = 30 * 60 * 20;
    private static long KICK_AFK_THRESHOLD = 60 * 60 * 20;
    private static String AFK_COMMAND = "";
    private static boolean afkNotificationsEnabled = true;
    private static final Logger LOGGER = LoggerFactory.getLogger("AFKManager");


    /**
     * Reload the AFKManager settings.
     * @param config The configuration map.
     */
    public static void reload(Configuration config) {
        if (config.contains("Notifications")) {
            afkNotificationsEnabled = config.getBoolean("Notifications");
        } else {
            LOGGER.warn("Notifications not found in config");
        }

        if (config.contains("AFK-Timer")) {
            int afkTimerConfig = config.getInt("AFK-Timer");
            AFK_THRESHOLD = afkTimerConfig == -1 ? -1 : afkTimerConfig * 20L;
        } else {
            LOGGER.warn("AFK-Timer not found in config");
        }

        if (config.contains("AFK-Kick-Timer")) {
            int afkKickTimerConfig = config.getInt("AFK-Kick-Timer");
            KICK_AFK_THRESHOLD = afkKickTimerConfig == -1 ? -1 : afkKickTimerConfig * 20L;
        } else {
            LOGGER.warn("AFK-Kick-Timer not found in config");
        }

        if (config.contains("AFK-Command")) {
            AFK_COMMAND = config.getString("AFK-Command");
        } else {
            LOGGER.warn("AFK-Command not found in config");
        }
    }


    /**
     * Check the AFK status of all players on the server.
     * @param server The Minecraft server instance.
     */
    public static void checkAFKStatuses(MinecraftServer server) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            long currentTime = player.getWorld().getTime();

            if (Permissions.check(player, AFK_KICKEXEMPT_PERMISSION_NODE, 2)) {
                continue;
            }

            if (!lastActivity.containsKey(player.getUuid())) {
                resetActivity(player);
            }

            long lastActiveTime = lastActivity.getOrDefault(player.getUuid(), currentTime);
            Vec3i currentBlockPos = new Vec3i(player.getBlockX(), player.getBlockY(), player.getBlockZ());
            Vec3i lastBlockPos = lastKnownPositions.getOrDefault(player.getUuid(), currentBlockPos);

            if (KICK_AFK_THRESHOLD != -1 && currentTime - lastActiveTime >= KICK_AFK_THRESHOLD && isAFK(player)) {
                if (afkNotificationsEnabled) {
                    Map<String, String> kickReplacements = Map.of("{player}", player.getName().getString());
                    player.getServer().getPlayerManager().getPlayerList().forEach(onlinePlayer ->
                            LangManager.send(onlinePlayer, "Player-Got-AFK-Kicked-Message", kickReplacements)
                    );
                    LangManager.send(player, "AFK-Kick-Message");
                }
                setAFK(player, false, false);
                player.networkHandler.disconnect(Text.literal("You have been kicked for being AFK too long."));
            }

            if (!currentBlockPos.equals(lastBlockPos)) {
                resetActivity(player);
                if (isAFK(player)) {
                    setAFK(player, false, true);
                }
            } else if (AFK_THRESHOLD != -1 && currentTime - lastActiveTime >= AFK_THRESHOLD && !isAFK(player)) {
                setAFK(player, true, true);
            }

            lastKnownPositions.put(player.getUuid(), currentBlockPos);
        }
    }

    // Static block to register event callbacks related to player activity.
    static {
        // Callback for block interaction
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            // Reset AFK status when a player interacts with a block.
            if (player instanceof ServerPlayerEntity && isAFK((ServerPlayerEntity) player)) {
                setAFK((ServerPlayerEntity) player, false, true);
            }
            return ActionResult.PASS;
        });

        // Callback for block "punching"
        AttackBlockCallback.EVENT.register((player, world, hand, blockPos, direction) -> {
            // Reset AFK status when a player starts to break a block.
            if (player instanceof ServerPlayerEntity && isAFK((ServerPlayerEntity) player)) {
                setAFK((ServerPlayerEntity) player, false, true);
            }
            return ActionResult.PASS;
        });

        // Callback for chat messages
        ServerMessageEvents.ALLOW_CHAT_MESSAGE.register((message, sender, params) -> {
            // Reset AFK status when a player sends a chat message.
            if (isAFK(sender)) {
                setAFK(sender, false, true);
            }
            return true;
        });
    }

    /**
     * Check if a player is marked as AFK.
     * @param player The player to check.
     * @return true if the player is AFK, false otherwise.
     */
    public static boolean isAFK(ServerPlayerEntity player) {
        return afkStatus.getOrDefault(player.getUuid(), false);
    }

    /**
     * Set or unset a player's AFK status and optionally notify other players.
     * @param player The player whose AFK status will be set.
     * @param afk The desired AFK status (true for AFK, false for not AFK).
     * @param notifyOthers If true, notify other players of this player's AFK status change.
     */
    public static void setAFK(ServerPlayerEntity player, boolean afk, boolean notifyOthers) {
        boolean wasAFK = isAFK(player);
        if (afk != wasAFK) {
            if (afk) {
                afkStatus.put(player.getUuid(), true);
                if (!AFK_COMMAND.isEmpty()) {
                    String command = AFK_COMMAND.replace("{player}", player.getName().getString());
                    ServerCommandSource Console = player.getServer().getCommandSource().withLevel(4).withSilent();
                    player.getServer().getCommandManager().executeWithPrefix(Console, command);
                }
            } else {
                afkStatus.remove(player.getUuid());
            }
            if (notifyOthers && afkNotificationsEnabled) {
                notifyAFKStatusChange(player, afk);
            }
            resetActivity(player);
        }
    }

    /**
     * Toggle the AFK status of a player and notify others of the change.
     * @param player The player whose AFK status will be toggled.
     */
    public static void toggleAFK(ServerPlayerEntity player) {
        setAFK(player, !isAFK(player), true);
    }

    /**
     * Reset the activity timer for a player.
     * This method is called when a player performs any action that should reset their AFK timer.
     * @param player The player whose activity will be reset.
     */
    public static void resetActivity(ServerPlayerEntity player) {
        lastActivity.put(player.getUuid(), player.getWorld().getTime());
    }

    /**
     * Notify all online players of a player's AFK status change.
     * @param player The player whose AFK status changed.
     * @param afk The new AFK status.
     */
    private static void notifyAFKStatusChange(ServerPlayerEntity player, boolean afk) {
        String messageKey = afk ? "Other-Player-Now-AFK" : "Other-Player-No-Longer-AFK";
        Map<String, String> replacements = Map.of("{player}", player.getName().getString());

        player.getServer().getPlayerManager().getPlayerList().forEach(onlinePlayer ->
                LangManager.send(onlinePlayer, messageKey, replacements)
        );
    }
}