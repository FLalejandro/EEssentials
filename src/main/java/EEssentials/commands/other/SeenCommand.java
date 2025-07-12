package EEssentials.commands.other;

import EEssentials.EEssentials;
import EEssentials.lang.LangManager;
import EEssentials.storage.PlayerStorage;
import EEssentials.storage.StorageManager;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.command.CommandSource;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import java.io.File;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Provides a command to view when a player was last seen online.
 */
public class SeenCommand {

    // Permission node required to use the seen command.
    public static final String SEEN_PERMISSION_NODE = "eessentials.seen";

    /**
     * Registers the seen command to the command dispatcher.
     *
     * @param dispatcher The command dispatcher on which the command will be registered.
     */
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
                CommandManager.literal("seen")
                        .requires(Permissions.require(SEEN_PERMISSION_NODE, 2))
                        .then(CommandManager.argument("target", StringArgumentType.string())
                                .suggests((ctx, builder) -> CommandSource.suggestMatching(ctx.getSource().getServer().getPlayerNames(), builder))
                                .executes(ctx -> {
                                    String targetName = StringArgumentType.getString(ctx, "target");
                                    ServerPlayerEntity targetPlayer = ctx.getSource().getServer().getPlayerManager().getPlayer(targetName);
                                    return showSeen(ctx, targetPlayer, targetName);
                                }))
        );
    }

    /**
     * Executes the seen command and displays appropriate messages to the sender.
     *
     * @param ctx        Context for the command.
     * @param target     The target player.
     * @param targetName The name of the target player.
     * @return 1 if successful, 0 otherwise.
     */

    private static int showSeen(CommandContext<ServerCommandSource> ctx, ServerPlayerEntity target, String targetName) {
        ServerCommandSource source = ctx.getSource();

        UUID playerUUID;
        if (target != null) {
            // Player is online
            LangManager.send(source.getPlayer(), "Seen-Online", Map.of("{player}", target.getName().getString()));
            return 1;
        } else {
            // Use GameProfile to resolve UUID
            GameProfile profile = getProfileForName(targetName);
            if (profile == null || profile.getId() == null) {
                LangManager.send(source.getPlayer(), "Invalid-Player", Map.of("{input}", targetName));
                return 0;
            }
            playerUUID = profile.getId();
        }

        PlayerStorage storage = PlayerStorage.fromPlayerUUID(playerUUID);
        if (storage == null) {
            LangManager.send(source.getPlayer(), "Invalid-Player", Map.of("{input}", targetName));
            return 0;
        }
        Instant lastOnline = storage.getLastTimeOnline();
        Duration duration = Duration.between(lastOnline, Instant.now());
        String timeString = formatDuration(duration);
        LangManager.send(source.getPlayer(), "Seen", Map.of("{player}", targetName, "{last-seen-time}", timeString));

        return 1;
    }

    static GameProfile getProfileForName(String name) {
        File[] files = StorageManager.playerStorageDirectory.toFile().listFiles();
        if (files != null) {
            for (File file : files) {
                EEssentials.LOGGER.debug("Processing file: " + file.getName());
                if (file.getName().endsWith(".json")) {
                    PlayerStorage storage = PlayerStorage.fromPlayerUUID(UUID.fromString(file.getName().replace(".json", "")));
                    if (storage != null && name.equals(storage.getPlayerName())) {
                        return new GameProfile(storage.getPlayerUUID(), name);
                    }
                }
            }
        }
        return null;
    }

    /**
     * Formats a duration to a human-readable string.
     *
     * @param duration The duration to format.
     * @return The formatted string.
     */
    public static String formatDuration(Duration duration) {
        long days = duration.toDays();
        long hours = duration.toHours() % 24;
        long minutes = duration.toMinutes() % 60;

        // If the duration is less than a minute, just show "1 minute"
        if (days == 0 && hours == 0 && minutes < 1) {
            return "1 minute";
        }

        StringBuilder sb = new StringBuilder();
        if (days > 0) {
            sb.append(days).append(" days");
        }
        if (hours > 0) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(hours).append(" hours");
        }
        if (minutes > 0) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(minutes).append(" minutes");
        }
        return sb.toString();
    }

}
