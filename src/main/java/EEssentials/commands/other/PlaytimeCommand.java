package EEssentials.commands.other;

import EEssentials.lang.LangManager;
import EEssentials.storage.PlayerStorage;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.command.CommandSource;
import static net.minecraft.server.command.CommandManager.*;
import net.minecraft.stat.Stats;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Provides command to display player's playtime.
 */
public class PlaytimeCommand {

    // Permission nodes for the playtime command.
    public static final String PLAYTIME_SELF_PERMISSION_NODE = "eessentials.playtime.self";
    public static final String PLAYTIME_OTHER_PERMISSION_NODE = "eessentials.playtime.other";

    /**
     * Registers the playtime command.
     *
     * @param dispatcher The command dispatcher to register commands on.
     */
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
                literal("playtime")
                        .requires(Permissions.require(PLAYTIME_SELF_PERMISSION_NODE, 2))
                        .executes(PlaytimeCommand::showSelfPlaytime)  // Self playtime command
                        .then(argument("target", StringArgumentType.string())
                                .requires(Permissions.require(PLAYTIME_OTHER_PERMISSION_NODE, 2))
                                .suggests((ctx, builder) -> CommandSource.suggestMatching(ctx.getSource().getServer().getPlayerNames(), builder))
                                .executes(ctx -> {
                                    String targetName = StringArgumentType.getString(ctx, "target");
                                    return showPlaytime(ctx, targetName);  // Shows the specified player's playtime
                                }))
        );
    }

    /**
     * Displays the playtime for the executing player (self).
     *
     * @param ctx The command context.
     * @return 1 if successful, 0 otherwise.
     */
    private static int showSelfPlaytime(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource source = ctx.getSource();
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            LangManager.send(source.getPlayer(), "Invalid-Player", Map.of());
            return 0;
        }

        // Get playtime for the player
        int timePlayed = player.getStatHandler().getStat(Stats.CUSTOM.getOrCreateStat(Stats.PLAY_TIME));

        // Format and display the playtime
        String timeString = formatTime(timePlayed);
        Map<String, String> replacements = new HashMap<>();
        replacements.put("{playtime}", timeString);

        LangManager.send(player, "Playtime", replacements);
        return 1;
    }

    /**
     * Displays the playtime of the target player.
     *
     * @param ctx The command context.
     * @param targetName The target player's name.
     * @return 1 if successful, 0 otherwise.
     */
    private static int showPlaytime(CommandContext<ServerCommandSource> ctx, String targetName) {
        ServerCommandSource source = ctx.getSource();
        ServerPlayerEntity player = ctx.getSource().getServer().getPlayerManager().getPlayer(targetName);
        int timePlayed;

        // If the player is offline, fetch playtime from PlayerStorage
        if (player == null) {
            // Try to get the GameProfile for the offline player
            GameProfile profile = SeenCommand.getProfileForName(targetName);

            if (profile == null || profile.getId() == null) {
                LangManager.send(source.getPlayer(), "Invalid-Player", Map.of("{input}", targetName));
                return 0;
            }

            // Fetch the player storage for the offline player
            PlayerStorage storage = PlayerStorage.fromPlayerUUID(profile.getId());
            if (storage == null) {
                LangManager.send(source.getPlayer(), "Invalid-Player", Map.of("{input}", targetName));
                return 0;
            }

            // Get playtime from storage for offline players
            timePlayed = storage.getTotalPlaytime();
        } else {
            // Get playtime for online players
            timePlayed = player.getStatHandler().getStat(Stats.CUSTOM.getOrCreateStat(Stats.PLAY_TIME));
        }

        // Format and display the playtime
        String timeString = formatTime(timePlayed);
        Map<String, String> replacements = new HashMap<>();
        replacements.put("{playtime}", timeString);
        replacements.put("{player}", targetName);

        LangManager.send(source.getPlayer(), "Playtime-Other", replacements);

        return 1;
    }

    /**
     * Formats the given time (in ticks) to a readable format.
     *
     * @param ticks The time in ticks.
     * @return The formatted time string.
     */
    public static String formatTime(int ticks) {
        long seconds = ticks / 20;
        long days = TimeUnit.SECONDS.toDays(seconds);
        long hours = TimeUnit.SECONDS.toHours(seconds) - (days * 24);
        long minutes = TimeUnit.SECONDS.toMinutes(seconds) - (TimeUnit.SECONDS.toHours(seconds) * 60);
        return String.format("%d days, %d hours, %d minutes", days, hours, minutes);
    }
}
