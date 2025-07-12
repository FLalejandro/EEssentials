package EEssentials.commands.other;

import EEssentials.lang.LangManager;
import EEssentials.util.IgnoreManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import me.lucko.fabric.api.permissions.v0.Permissions;
import static net.minecraft.server.command.CommandManager.*;

import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Provides command to manage players ignore lists.
 */
public class IgnoreCommands {

    // Permission nodes for the ignore command.
    public static final String IGNORE_PERMISSION_NODE = "eessentials.ignore";
    public static final String UNIGNORABLE_PERMISSION_NODE = "eessentials.unignorable";

    /**
     * Registers the ignore and unignore commands.
     *
     * @param dispatcher The command dispatcher to register commands on.
     */
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
                literal("ignore")
                        .requires(Permissions.require(IGNORE_PERMISSION_NODE, 2))
                        .then(argument("target", EntityArgumentType.player())
                                .executes(ctx -> {
                                    ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "target");
                                    return ignorePlayer(ctx, target);
                                }))
        );

        dispatcher.register(
                literal("unignore")
                        .requires(Permissions.require(IGNORE_PERMISSION_NODE, 2))
                        .then(argument("target", EntityArgumentType.player())
                                .executes(ctx -> {
                                    ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "target");
                                    return unignorePlayer(ctx, target);
                                }))
        );

        dispatcher.register(
                literal("ignorelist")
                        .requires(Permissions.require(IGNORE_PERMISSION_NODE, 2))
                        .executes(IgnoreCommands::displayIgnoredPlayers)
        );
    }

    /**
     * Adds the specified player to the ignore list of the executor.
     *
     * @param ctx    The command context.
     * @param target The player to be ignored.
     * @return 1 if successful, 0 otherwise.
     */
    private static int ignorePlayer(CommandContext<ServerCommandSource> ctx, ServerPlayerEntity target) {
        ServerCommandSource source = ctx.getSource();
        ServerPlayerEntity player = source.getPlayer();

        if (player == null) return 0;

        // Check if the target is unignorable before proceeding
        if (Permissions.check(target, UNIGNORABLE_PERMISSION_NODE, 2)) {
            LangManager.send(player, "Ignore-Unignorable", Map.of("{player}", target.getName().getString()));
            return 1;
        }

        // Using the IgnoreManager to handle the actual ignore logic
        IgnoreManager.ignorePlayer(player, target);
        LangManager.send(player, "Ignore", Map.of("{player}", target.getName().getString()));
        return 1;
    }

    /**
     * Removes the specified player from the ignore list of the executor.
     *
     * @param ctx    The command context.
     * @param target The player to be unignored.
     * @return 1 if successful, 0 otherwise.
     */
    private static int unignorePlayer(CommandContext<ServerCommandSource> ctx, ServerPlayerEntity target) {
        ServerCommandSource source = ctx.getSource();
        ServerPlayerEntity player = source.getPlayer();

        if (player == null) return 0;

        // Using the IgnoreManager to handle the actual unignore logic
        IgnoreManager.unignorePlayer(player, target);
        LangManager.send(player, "Unignore", Map.of("{player}", target.getName().getString()));
        return 1;
    }

    /**
     * Displays a list of all the players that the executor has ignored.
     *
     * @param ctx The command context.
     * @return 1 if successful, 0 otherwise.
     */
    private static int displayIgnoredPlayers(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource source = ctx.getSource();
        ServerPlayerEntity player = source.getPlayer();

        if (player == null) return 0;

        Set<UUID> ignoredPlayerUUIDs = IgnoreManager.getIgnoredPlayers(player);

        if (ignoredPlayerUUIDs.isEmpty()) {
            LangManager.send(player, "Ignore-List-Empty");
            return 1;
        }

        StringBuilder ignoredPlayersList = new StringBuilder();
        for (UUID ignoredUUID : ignoredPlayerUUIDs) {
            ServerPlayerEntity ignoredPlayer = source.getServer().getPlayerManager().getPlayer(ignoredUUID);
            if (ignoredPlayer != null) {
                ignoredPlayersList.append(ignoredPlayer.getName().getString()).append(", ");
            }
        }

        if (ignoredPlayersList.length() > 0) {
            // Remove trailing comma and space
            ignoredPlayersList.setLength(ignoredPlayersList.length() - 2);
        }

        LangManager.send(player, "Ignore-List", Map.of("{players}", ignoredPlayersList.toString()));
        return 1;
    }
}