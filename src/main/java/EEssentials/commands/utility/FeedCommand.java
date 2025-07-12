package EEssentials.commands.utility;

import EEssentials.lang.LangManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Map;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

/**
 * Provides command to feed the player.
 */
public class FeedCommand {

    // Permission node for the feed command.
    public static final String FEED_SELF_PERMISSION_NODE = "eessentials.feed.self";
    public static final String FEED_OTHER_PERMISSION_NODE = "eessentials.feed.other";

    /**
     * Registers the feed command.
     *
     * @param dispatcher The command dispatcher to register commands on.
     */
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
                literal("feed")
                        .requires(Permissions.require(FEED_SELF_PERMISSION_NODE, 2))
                        .executes(ctx -> feedPlayer(ctx))  // Feeds the executing player
                        .then(argument("target", EntityArgumentType.player())
                                .requires(Permissions.require(FEED_OTHER_PERMISSION_NODE, 2))
                                .suggests((ctx, builder) -> CommandSource.suggestMatching(ctx.getSource().getServer().getPlayerNames(), builder))
                                .executes(ctx -> {
                                    ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "target");
                                    return feedPlayer(ctx, target);  // Feeds the specified player
                                }))
        );
    }

    /**
     * Feeds the target player.
     *
     * @param ctx The command context.
     * @param targets The target players.
     * @return 1 if successful, 0 otherwise.
     */
    private static int feedPlayer(CommandContext<ServerCommandSource> ctx, ServerPlayerEntity... targets) {
        ServerCommandSource source = ctx.getSource();
        ServerPlayerEntity player = targets.length > 0 ? targets[0] : source.getPlayer();

        if (player == null) return 0;

        // Set hunger and saturation to max
        player.getHungerManager().setFoodLevel(20); // Max hunger level is 20
        player.getHungerManager().setSaturationLevel(20); // Max saturation level is 20

        if (player.equals(source.getPlayer())) {
            LangManager.send(player, "Feed-Self");  // Player feeding themselves
        } else {
            LangManager.send(player, "Feed-Other-Notify", Map.of("{source}", source.getName()));  // Player being fed by someone else
            LangManager.send(source.getPlayer(), "Feed-Other", Map.of("{player}", player.getName().getString()));  // The feeder receives this message
        }

        return 1;
    }


}
