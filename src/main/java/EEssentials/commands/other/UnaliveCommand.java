package EEssentials.commands.other;

import EEssentials.lang.LangManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import static net.minecraft.server.command.CommandManager.literal;

/**
 * Provides command to kill the player.
 */
public class UnaliveCommand {

    // Permission node for the unalive command.
    public static final String UNALIVE_PERMISSION_NODE = "eessentials.unalive";

    /**
     * Registers the unalive command.
     *
     * @param dispatcher The command dispatcher to register commands on.
     */
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
                literal("unalive")
                        .requires(Permissions.require(UNALIVE_PERMISSION_NODE, 2))
                        .executes(UnaliveCommand::unalivePlayer)
        );
    }

    /**
     * Kills the executing player.
     *
     * @param ctx The command context.
     * @return 1 if successful, 0 otherwise.
     */
    private static int unalivePlayer(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (player == null) return 0;

        player.damage(player.getWorld(), player.getDamageSources().generic(), Float.MAX_VALUE);
        player.setHealth(0);

        LangManager.send(player, "Unalive-Success");
        return 1;
    }
}
