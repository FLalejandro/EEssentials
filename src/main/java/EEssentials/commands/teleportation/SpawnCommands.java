package EEssentials.commands.teleportation;

import EEssentials.EEssentials;
import EEssentials.lang.LangManager;
import EEssentials.util.Location;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Map;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

/**
 * Defines the /spawn related commands to manage a universal spawn location.
 */
public class SpawnCommands {

    public static final String SPAWN_SELF_PERMISSION_NODE = "eessentials.spawn.self";
    public static final String SPAWN_OTHER_PERMISSION_NODE = "eessentials.spawn.other";
    public static final String SETSPAWN_PERMISSION_NODE = "eessentials.setspawn";

    /**
     * Registers spawn related commands (/setspawn, /spawn).
     *
     * @param dispatcher The command dispatcher to register commands on.
     */
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        // Set a new universal spawn location.
        dispatcher.register(literal("setspawn")
                .requires(Permissions.require(SETSPAWN_PERMISSION_NODE, 2))
                .executes(ctx -> {
                    ServerPlayerEntity player = ctx.getSource().getPlayer();
                    if (player == null) return 0;

                    // Use these values to create the new Location
                    Location spawnLocation = new Location(player.getWorld(), player.getX(), player.getY(), player.getZ(), player.getPitch(), player.getYaw());
                    EEssentials.storage.locationManager.setSpawn(spawnLocation);
                    LangManager.send(player, "Spawn-Set");
                    return 1;
                })
        );

        // Teleport the player or the target to the universal spawn location.
        dispatcher.register(
                literal("spawn")
                        .requires(Permissions.require(SPAWN_SELF_PERMISSION_NODE, 2))
                        .executes(ctx -> {
                            ServerPlayerEntity player = ctx.getSource().getPlayer();
                            return teleportToSpawn(ctx, player);
                        })
                        .then(argument("target", EntityArgumentType.player())
                                .requires(Permissions.require(SPAWN_OTHER_PERMISSION_NODE, 2))
                                .executes(ctx -> {
                                    ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "target");
                                    return teleportToSpawn(ctx, target);
                                }))
        );
    }

    private static int teleportToSpawn(CommandContext<ServerCommandSource> ctx, ServerPlayerEntity... targets) {
        ServerCommandSource source = ctx.getSource();
        ServerPlayerEntity player = targets.length > 0 ? targets[0] : source.getPlayer();
        if (player == null) return 0;

        Location spawnLocation = EEssentials.storage.locationManager.serverSpawn;
        if (spawnLocation != null) {
            spawnLocation.teleport(player);

            if (player.equals(source.getPlayer())) {
                LangManager.send(player, "Teleporting-To-Spawn");
            } else {
                LangManager.send(source.getPlayer(), "Teleporting-Player-To-Spawn", Map.of("{player}", player.getName().getString()));
            }
            return 1;
        } else {
            LangManager.send(player, "Spawn-Not-Set");
            return 0;
        }
    }
}
