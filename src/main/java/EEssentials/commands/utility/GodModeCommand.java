package EEssentials.commands.utility;

import EEssentials.lang.LangManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

/**
 * Provides command to toggle god mode for a player.
 */
public class GodModeCommand {

    // Permission node for the god mode command.
    public static final String GOD_SELF_PERMISSION_NODE = "eessentials.godmode.self";
    public static final String GOD_OTHER_PERMISSION_NODE = "eessentials.godmode.other";
    private static final Set<UUID> godModePlayers = new HashSet<>();

    /**
     * Registers the god mode command.
     *
     * @param dispatcher The command dispatcher to register commands on.
     */
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
                literal("god")
                        .requires(Permissions.require(GOD_SELF_PERMISSION_NODE, 2))
                        .executes(ctx -> toggleGodMode(ctx))
                        .then(argument("target", EntityArgumentType.player())
                                .requires(Permissions.require(GOD_OTHER_PERMISSION_NODE, 2))
                                .suggests((ctx, builder) -> CommandSource.suggestMatching(ctx.getSource().getServer().getPlayerNames(), builder))
                                .executes(ctx -> {
                                    ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "target");
                                    return toggleGodMode(ctx, target);
                                }))
        );
    }

    /**
     * Toggles god mode for the target player.
     *
     * @param ctx The command context.
     * @param targets The target players.
     * @return 1 if successful, 0 otherwise.
     */
    private static int toggleGodMode(CommandContext<ServerCommandSource> ctx, ServerPlayerEntity... targets) {
        ServerCommandSource source = ctx.getSource();
        ServerPlayerEntity player = targets.length > 0 ? targets[0] : source.getPlayer();

        if (player == null) return 0;

        UUID playerId = player.getUuid();
        boolean isGodMode = godModePlayers.contains(playerId);

        if (isGodMode) {
            godModePlayers.remove(playerId);
            player.getAbilities().invulnerable = false;
            player.sendAbilitiesUpdate();
            LangManager.send(player, "God-Mode-Disabled");

            if (!player.equals(source.getPlayer())) {
                LangManager.send(source.getPlayer(), "God-Mode-Other-Disabled", Map.of("{player}", player.getName().getString()));
            }
        } else {
            godModePlayers.add(playerId);
            player.getAbilities().invulnerable = true;
            player.sendAbilitiesUpdate();
            LangManager.send(player, "God-Mode-Enabled");

            if (!player.equals(source.getPlayer())) {
                LangManager.send(source.getPlayer(), "God-Mode-Other-Enabled", Map.of("{player}", player.getName().getString()));
            }
        }

        return 1;
    }
}
