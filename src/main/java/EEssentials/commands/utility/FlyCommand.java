package EEssentials.commands.utility;

import EEssentials.lang.LangManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.HashMap;
import java.util.Map;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

/**
 * Provides command to toggle flight mode for players.
 */
public class FlyCommand {

    // Permission node for the fly command.
    public static final String FLY_SELF_PERMISSION_NODE = "eessentials.fly.self";
    public static final String FLY_OTHER_PERMISSION_NODE = "eessentials.fly.other";

    /**
     * Registers the fly command.
     *
     * @param dispatcher The command dispatcher to register commands on.
     */
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
                literal("fly")
                        .requires(Permissions.require(FLY_SELF_PERMISSION_NODE, 2))
                        .executes(ctx -> toggleFlight(ctx))
                        .then(argument("target", EntityArgumentType.player())
                                .requires(Permissions.require(FLY_OTHER_PERMISSION_NODE, 2))
                                .suggests((ctx, builder) -> CommandSource.suggestMatching(ctx.getSource().getServer().getPlayerNames(), builder))
                                .executes(ctx -> {
                                    ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "target");
                                    return toggleFlight(ctx, target);
                                }))
        );
    }

    /**
     * Toggles the flight mode for target players.
     *
     * @param ctx The command context.
     * @param targets The target players.
     * @return 1 if successful, 0 otherwise.
     */
    private static int toggleFlight(CommandContext<ServerCommandSource> ctx, ServerPlayerEntity... targets) {
        ServerCommandSource source = ctx.getSource();
        ServerPlayerEntity targetPlayer = targets.length > 0 ? targets[0] : source.getPlayer();

        if (targetPlayer == null) return 0;

        // Toggle the ability to fly
        boolean newFlightAbilityStatus = !targetPlayer.getAbilities().allowFlying;
        targetPlayer.getAbilities().allowFlying = newFlightAbilityStatus;

        // If the ability to fly is turned off, ensure the player is not currently flying
        if (!newFlightAbilityStatus) {
            targetPlayer.getAbilities().flying = false;
        }

        targetPlayer.sendAbilitiesUpdate();

        if (targetPlayer.equals(source.getPlayer())) {
            // Player is toggling their own flight
            String key = newFlightAbilityStatus ? "Flight-Enabled-Self" : "Flight-Disabled-Self";
            LangManager.send(targetPlayer, key);
        } else {
            // Notify the target about their flight ability + the mf who applied it
            String notifyKey = newFlightAbilityStatus ? "Flight-Enabled-Other" : "Flight-Disabled-Other";
            Map<String, String> targetReplacements = new HashMap<>();
            targetReplacements.put("{source}", source.getName());
            LangManager.send(targetPlayer, notifyKey, targetReplacements);

            // Notify the user that the target's flight status
            String sourceKey = newFlightAbilityStatus ? "Flight-Enabled-Other-Notify" : "Flight-Disabled-Other-Notify";
            Map<String, String> sourceReplacements = new HashMap<>();
            sourceReplacements.put("{player}", targetPlayer.getName().getString());
            LangManager.send(source.getPlayer(), sourceKey, sourceReplacements);
        }

        return 1;
    }
}
