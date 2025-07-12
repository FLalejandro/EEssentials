package EEssentials.commands.utility;

import EEssentials.lang.LangManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.GameMode;

import java.util.Map;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

/**
 * Provides commands for quick game mode switching using aliases.
 */
public class GamemodeAliasesCommands {

    // Permission nodes for each game mode command.
    public static final String ADVENTURE_PERMISSION_NODE = "eessentials.gamemode.adventure";
    public static final String CREATIVE_PERMISSION_NODE = "eessentials.gamemode.creative";
    public static final String SPECTATOR_PERMISSION_NODE = "eessentials.gamemode.spectator";
    public static final String SURVIVAL_PERMISSION_NODE = "eessentials.gamemode.survival";
    public static final String GAMEMODE_OTHER_PERMISSION_NODE = "eessentials.gamemode.other";

    /**
     * Registers game mode switching commands.
     *
     * @param dispatcher The command dispatcher to register commands on.
     */
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        // Registers the /gmc command for changing to Creative mode.
        dispatcher.register(
                literal("gmc")
                        .requires(Permissions.require(CREATIVE_PERMISSION_NODE, 2))
                        .then(argument("target", EntityArgumentType.player())  // Add player argument
                                .requires(Permissions.require(GAMEMODE_OTHER_PERMISSION_NODE, 2))
                                .suggests((ctx, builder) -> CommandSource.suggestMatching(ctx.getSource().getServer().getPlayerNames(), builder))
                                .executes(ctx -> {
                                    ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "target");
                                    return executeGameMode(ctx, GameMode.CREATIVE, target);  // Pass the target player to the execute method
                                }))
                        .executes(ctx -> executeGameMode(ctx, GameMode.CREATIVE))  // This is for when no player argument is provided.
        );


        // Registers the /gms command for changing to Survival mode.
        dispatcher.register(
                literal("gms")
                        .requires(Permissions.require(SURVIVAL_PERMISSION_NODE, 2))
                        .then(argument("target", EntityArgumentType.player())
                                .requires(Permissions.require(GAMEMODE_OTHER_PERMISSION_NODE, 2))
                                .suggests((ctx, builder) -> CommandSource.suggestMatching(ctx.getSource().getServer().getPlayerNames(), builder))
                                .executes(ctx -> {
                                    ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "target");
                                    return executeGameMode(ctx, GameMode.SURVIVAL, target);
                                }))
                        .executes(ctx -> executeGameMode(ctx, GameMode.SURVIVAL))
        );

        // Registers the /gmsp command for changing to Spectator mode.
        dispatcher.register(
                literal("gmsp")
                        .requires(Permissions.require(SPECTATOR_PERMISSION_NODE, 2))
                        .then(argument("target", EntityArgumentType.player())
                                .requires(Permissions.require(GAMEMODE_OTHER_PERMISSION_NODE, 2))
                                .suggests((ctx, builder) -> CommandSource.suggestMatching(ctx.getSource().getServer().getPlayerNames(), builder))
                                .executes(ctx -> {
                                    ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "target");
                                    return executeGameMode(ctx, GameMode.SPECTATOR, target);
                                }))
                        .executes(ctx -> executeGameMode(ctx, GameMode.SPECTATOR))
        );

        // Registers the /gma command for changing to Adventure mode.
        dispatcher.register(
                literal("gma")
                        .requires(Permissions.require(ADVENTURE_PERMISSION_NODE, 2))
                        .then(argument("target", EntityArgumentType.player())
                                .requires(Permissions.require(GAMEMODE_OTHER_PERMISSION_NODE, 2))
                                .suggests((ctx, builder) -> CommandSource.suggestMatching(ctx.getSource().getServer().getPlayerNames(), builder))
                                .executes(ctx -> {
                                    ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "target");
                                    return executeGameMode(ctx, GameMode.ADVENTURE, target);
                                }))
                        .executes(ctx -> executeGameMode(ctx, GameMode.ADVENTURE))
        );
    }

    /**
     * Changes the game mode of the player issuing the command.
     *
     * @param ctx      The command context.
     * @param gameMode The target game mode.
     * @return 1 if successful, 0 otherwise.
     */
    private static int executeGameMode(CommandContext<ServerCommandSource> ctx, GameMode gameMode, ServerPlayerEntity... targets) {
        ServerCommandSource source = ctx.getSource();
        ServerPlayerEntity player = targets.length > 0 ? targets[0] : source.getPlayer();

        if (player == null) return 0;

        player.changeGameMode(gameMode);
        String formattedGameModeName = formatGameModeName(gameMode);
        Map<String, String> replacements = Map.of("{gameMode}", formattedGameModeName, "{player}", player.getName().getString(), "{source}", source.getName());

        if (player.equals(source.getPlayer())) {
            LangManager.send(source.getPlayer(), "GameMode-Change-Self", replacements);
        } else {
            LangManager.send(player, "GameMode-Change-Other-Notify", replacements);
            LangManager.send(source.getPlayer(), "GameMode-Change-Other", replacements);
        }
        return 1;
    }

    /**
     * Formats the game mode name such that only the first letter is capitalized.
     *
     * @param gameMode The game mode to format.
     * @return The formatted game mode name.
     */
    private static String formatGameModeName(GameMode gameMode) {
        String name = gameMode.name().toLowerCase();
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }

}
