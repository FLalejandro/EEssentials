package EEssentials.commands.utility;

import EEssentials.commands.AliasedCommand;
import EEssentials.lang.LangManager;
import EEssentials.screens.EnderchestScreen;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.stat.Stats;

import java.util.HashMap;
import java.util.Map;

import static net.minecraft.server.command.CommandManager.argument;

/**
 * Provides command to open the enderchest.
 */
public class EnderchestCommand {

    // Permission node for the enderchest command.
    public static final String ENDERCHEST_SELF_PERMISSION_NODE = "eessentials.enderchest.self";
    public static final String ENDERCHEST_OTHER_PERMISSION_NODE = "eessentials.enderchest.other";

    /**
     * Registers the enderchest command.
     *
     * @param dispatcher The command dispatcher to register commands on.
     */
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        // Register /enderchest, /echest, and /ec
        new AliasedCommand() {
            @Override
            public LiteralCommandNode<ServerCommandSource> register(CommandDispatcher<ServerCommandSource> dispatcher) {
                return dispatcher.register(CommandManager.literal("enderchest")
                        .requires(Permissions.require(ENDERCHEST_SELF_PERMISSION_NODE, 2))
                        .executes(ctx -> openEnderchest(ctx))
                                .then(argument("target", EntityArgumentType.player())
                                        .requires(Permissions.require(ENDERCHEST_OTHER_PERMISSION_NODE, 2))
                                        .suggests((ctx, builder) -> CommandSource.suggestMatching(ctx.getSource().getServer().getPlayerNames(), builder))
                                        .executes(ctx -> {
                                            ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "target");
                                            return openEnderchest(ctx, target);
                                        })));
            }
            @Override
            public String[] getCommandAliases() {
                return new String[]{"echest", "ec"};
            }
        }.registerWithAliases(dispatcher);
    }
    /**
     * Opens the target player's enderchest.
     *
     * @param ctx The command context.
     * @param targets The target players.
     * @return 1 if successful, 0 otherwise.
     */
    private static int openEnderchest(CommandContext<ServerCommandSource> ctx, ServerPlayerEntity... targets) {
        ServerCommandSource source = ctx.getSource();
        ServerPlayerEntity executingPlayer = source.getPlayer();
        ServerPlayerEntity targetPlayer = targets.length > 0 ? targets[0] : executingPlayer;

        if (executingPlayer == null || targetPlayer == null) return 0;

        EnderchestScreen screenHandlerFactory = new EnderchestScreen(targetPlayer);
        executingPlayer.openHandledScreen(screenHandlerFactory);
        executingPlayer.incrementStat(Stats.OPEN_ENDERCHEST);

        if (!executingPlayer.equals(targetPlayer)) {
            // Replace text messages with LangManager.send
            Map<String, String> replacements = new HashMap<>();
            replacements.put("{player}", targetPlayer.getName().getString());
            LangManager.send(source.getPlayer(), "Enderchest-Other",replacements);
        }

        return 1;
    }

}
