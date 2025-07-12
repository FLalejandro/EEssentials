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
 * Provides command to heal the player.
 */
public class HealCommand {

    // Permission node for the heal command.
    public static final String HEAL_SELF_PERMISSION_NODE = "eessentials.heal.self";
    public static final String HEAL_OTHER_PERMISSION_NODE = "eessentials.heal.other";

    /**
     * Registers the heal command.
     *
     * @param dispatcher The command dispatcher to register commands on.
     */
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
                literal("heal")
                        .requires(Permissions.require(HEAL_SELF_PERMISSION_NODE, 2))
                        .executes(ctx -> healPlayer(ctx))  // Heals the executing player
                        .then(argument("target", EntityArgumentType.player())
                                .requires(Permissions.require(HEAL_OTHER_PERMISSION_NODE, 2))
                                .suggests((ctx, builder) -> CommandSource.suggestMatching(ctx.getSource().getServer().getPlayerNames(), builder))
                                .executes(ctx -> {
                                    ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "target");
                                    return healPlayer(ctx, target);  // Heals the specified player
                                }))
        );
    }

    /**
     * Heals the target player.
     *
     * @param ctx The command context.
     * @param targets The target players.
     * @return 1 if successful, 0 otherwise.
     */
    private static int healPlayer(CommandContext<ServerCommandSource> ctx, ServerPlayerEntity... targets) {
        ServerCommandSource source = ctx.getSource();
        ServerPlayerEntity player = targets.length > 0 ? targets[0] : source.getPlayer();

        if (player == null) return 0;

        // Set health to max
        player.setHealth(player.getMaxHealth());

        if (player.equals(source.getPlayer())) {
            LangManager.send(player, "Heal-Self");  // Player healing themselves
        } else {
            LangManager.send(player, "Heal-Other-Notify", Map.of("{source}", source.getName()));  // Player being healed by someone else
            LangManager.send(source.getPlayer(), "Heal-Other", Map.of("{player}", player.getName().getString()));  // The healer receives this message
        }

        return 1;
    }


}
