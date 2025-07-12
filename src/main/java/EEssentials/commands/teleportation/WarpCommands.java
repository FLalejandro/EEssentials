package EEssentials.commands.teleportation;

import EEssentials.EEssentials;
import EEssentials.lang.LangManager;
import EEssentials.util.Location;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.command.CommandSource;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Map;
import java.util.Set;

import static net.minecraft.server.command.CommandManager.*;

/**
 * Defines the /warp related commands to manage global warps.
 */
public class WarpCommands {

    // Permission Nodes for Warp Commands
    public static final String WARP_PERMISSION_NODE = "eessentials.warp.self";
    public static final String WARP_SET_PERMISSION_NODE = "eessentials.warp.set";
    public static final String WARP_DELETE_PERMISSION_NODE = "eessentials.warp.delete";

    public static final String WARP_LIST_PERMISSION_NODE = "eessentials.warp.list";

    /**
     * Registers warp related commands (/setwarp, /delwarp, /warp, /warps).
     *
     * @param dispatcher The command dispatcher to register commands on.
     */
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        // Set a new warp or overwrite an existing one.
        dispatcher.register(literal("setwarp")
                .requires(Permissions.require(WARP_SET_PERMISSION_NODE, 2))
                .then(argument("name", StringArgumentType.word())
                        .executes(ctx -> {
                            ServerPlayerEntity player = ctx.getSource().getPlayer();
                            String warpName = StringArgumentType.getString(ctx, "name");

                            // Use these values to create the new Location
                            Location warpLocation = new Location(player.getWorld(), player.getX(), player.getY(), player.getZ(), player.getPitch(), player.getYaw());

                            EEssentials.storage.locationManager.setWarp(warpName, warpLocation);
                            LangManager.send(player, "Warp-Set", Map.of("{warp}", warpName));
                            return 1;
                        })
                )
        );


        // Delete a warp.
        dispatcher.register(literal("delwarp")
                .requires(Permissions.require(WARP_DELETE_PERMISSION_NODE, 2))
                .then(argument("name", StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            String input = builder.getRemaining().toLowerCase();
                            return CommandSource.suggestMatching(
                                    EEssentials.storage.locationManager.getWarpNames().stream()
                                            .filter(warp -> warp.toLowerCase().startsWith(input))
                                            .distinct()
                                            .sorted(),
                                    builder
                            );
                        })
                        .executes(ctx -> {
                            ServerPlayerEntity player = ctx.getSource().getPlayer();
                            String warpName = StringArgumentType.getString(ctx, "name");

                            if (EEssentials.storage.locationManager.getWarp(warpName) != null) {
                                EEssentials.storage.locationManager.deleteWarp(warpName);
                                LangManager.send(player, "Warp-Delete", Map.of("{warp}", warpName));
                                return 1;
                            } else {
                                LangManager.send(player, "Invalid-Warp", Map.of("{input}", warpName));
                                return 0;
                            }
                        })
                )
        );


        // Teleport the player to a specified warp.
        dispatcher.register(literal("warp")
                .requires(Permissions.require(WARP_PERMISSION_NODE, 2))
                .then(argument("name", StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            String input = builder.getRemaining().toLowerCase();
                            return CommandSource.suggestMatching(
                                    EEssentials.storage.locationManager.getWarpNames().stream()
                                            .filter(warp -> warp.toLowerCase().startsWith(input))
                                            .distinct()
                                            .sorted(),
                                    builder
                            );
                        })                        .executes(ctx -> {
                            ServerPlayerEntity player = ctx.getSource().getPlayer();
                            String warpName = StringArgumentType.getString(ctx, "name");

                            return teleportToWarp(player, warpName);
                        })
                )
                .executes(ctx -> {
                    LangManager.send(ctx.getSource().getPlayer(), "Invalid-Warp-Command");
                    return 0;
                })
        );

        // List all global warps.
        dispatcher.register(literal("warps")
                .requires(Permissions.require(WARP_LIST_PERMISSION_NODE, 2))
                .executes(ctx -> {
                    ServerPlayerEntity player = ctx.getSource().getPlayer();

                    // Fetch warp names from the new storage system
                    Set<String> warpNames = EEssentials.storage.locationManager.getWarpNames();

                    if (!warpNames.isEmpty()) {
                        String warpList = String.join(", ", warpNames);
                        LangManager.send(player, "Warp-List", Map.of("{warps}", warpList));
                    } else {
                        LangManager.send(player, "Warp-List-Empty");
                    }

                    return 1;
                })
        );
    }

    private static int teleportToWarp(ServerPlayerEntity player, String warpName) {
        Location location = EEssentials.storage.locationManager.getWarp(warpName);
        if (location != null) {
            location.teleport(player);
            LangManager.send(player, "Teleporting-Players");
            return 1;
        } else {
            LangManager.send(player, "Invalid-Warp", Map.of("{input}", warpName));
            return 0;
        }
    }
}
