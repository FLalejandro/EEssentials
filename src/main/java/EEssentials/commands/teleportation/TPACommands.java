package EEssentials.commands.teleportation;

import EEssentials.commands.AliasedCommand;
import EEssentials.lang.LangManager;
import EEssentials.util.IgnoreManager;
import EEssentials.util.Location;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.LiteralCommandNode;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.*;

/**
 * Handles teleportation commands allowing players to send, accept,
 * and deny teleportation requests.
 */

public class TPACommands {

    // Define permission nodes for various commands.
    public static final String TPA_PERMISSION_NODE = "eessentials.tpa";
    public static final String TPACCEPT_PERMISSION_NODE = "eessentials.tpaccept";
    public static final String TPACANCEL_PERMISSION_NODE = "eessentials.tpacancel";
    public static final String TPAHERE_PERMISSION_NODE = "eessentials.tpahere";
    public static final String TPTOGGLE_PERMISSION_NODE = "eessentials.tptoggle";

    /**
     * Inner class to represent a teleport request.
     */
    private static class TeleportRequest {
        final ServerPlayerEntity requester;  // Player who sent the request
        final RequestType type;  // Type of request (TPA or TPAHERE)
        final long timestamp;  // Time when the request was created

        public TeleportRequest(ServerPlayerEntity requester, RequestType type) {
            this.requester = requester;
            this.type = type;
            this.timestamp = System.currentTimeMillis();
        }

        // Enum to differentiate between TPA and TPAHERE requests.
        enum RequestType {
            TPA, TPAHERE
        }
    }

    // Set to store UUIDs of players who have turned off incoming teleport requests.
    private static final Set<UUID> teleportToggleOff = new HashSet<>();

    // Map to store teleport requests. Maps a target player to a list of requests they've received.
    private static final Map<ServerPlayerEntity, List<TeleportRequest>> teleportRequests = new HashMap<>();

    /**
     * Register commands related to teleportation.
     * This includes /tpa, /tpahere, /tpaccept, /tpdeny, /tpcancel, and /tptoggle.
     *
     * @param dispatcher The command dispatcher to register commands on.
     */
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {

        // Command to request teleportation to another player.
        dispatcher.register(CommandManager.literal("tpa")
                .requires(Permissions.require(TPA_PERMISSION_NODE, 2))
                .then(CommandManager.argument("target", EntityArgumentType.player())
                        .executes(ctx -> {
                            ServerPlayerEntity requester = ctx.getSource().getPlayer();
                            ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "target");

                            // Replace text messages with LangManager.send
                            Map<String, String> replacements = new HashMap<>();
                            replacements.put("{target}", target.getName().getString());
                            replacements.put("{requester}", requester.getName().getString());

                            if (IgnoreManager.hasIgnored(target, requester)) {
                                LangManager.send(requester, "Ignore-Teleport-Request", replacements);
                                return 0;
                            }

                            if (teleportToggleOff.contains(target.getUuid())) {
                                LangManager.send(requester, "TPA-Requests-Disabled", replacements);
                                return 0;
                            }

                            if (requester.equals(target)) {
                                LangManager.send(requester, "TPA-Request-Self");
                                return 0;
                            }

                            // Remove any existing requests from the requester to the target.
                            removeExistingRequestFrom(requester, target);

                            // Add the new request.
                            teleportRequests.computeIfAbsent(target, k -> new ArrayList<>())
                                    .add(new TeleportRequest(requester, TeleportRequest.RequestType.TPA));

                            // Notify both players.
                            LangManager.send(requester, "TPA-Request-Send", replacements);
                            LangManager.send(target, "TPA-Request-Receive", replacements);

                            return 1;
                        })));

        // Command to request another player to teleport to the requester.
        dispatcher.register(CommandManager.literal("tpahere")
                .requires(Permissions.require(TPAHERE_PERMISSION_NODE, 2))
                .then(CommandManager.argument("target", EntityArgumentType.player())
                        .executes(ctx -> {
                            ServerPlayerEntity requester = ctx.getSource().getPlayer();
                            ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "target");

                            Map<String, String> replacements = new HashMap<>();
                            replacements.put("{target}", target.getName().getString());
                            replacements.put("{requester}", requester.getName().getString());

                            // Check if the target has ignored the requester
                            if (IgnoreManager.hasIgnored(target, requester)) {
                                LangManager.send(requester, "Ignore-Teleport-Request", replacements);
                                return 0;
                            }

                            // Check if the target has teleport requests toggled off.
                            if (teleportToggleOff.contains(target.getUuid())) {
                                LangManager.send(requester, "Teleport-Requests-Disabled", replacements);
                                return 0;
                            }

                            // Check if requester is the same as target.
                            if (requester.equals(target)) {
                                LangManager.send(requester, "Teleport-Request-Self");
                                return 0;
                            }

                            // Remove any existing requests from the requester to the target.
                            removeExistingRequestFrom(requester, target);

                            // Add the new request.
                            teleportRequests.computeIfAbsent(target, k -> new ArrayList<>())
                                    .add(new TeleportRequest(requester, TeleportRequest.RequestType.TPAHERE));

                            // Notify both players.
                            LangManager.send(requester, "TPAHere-Request-Send", replacements);
                            LangManager.send(target, "TPAHere-Request-Receive", replacements);

                            return 1;
                        })));

        // Register /tpaccept, /tpyes
        // Command to accept a pending teleport request.
        new AliasedCommand() {
            @Override
            public LiteralCommandNode<ServerCommandSource> register(CommandDispatcher<ServerCommandSource> dispatcher) {
                return dispatcher.register(CommandManager.literal("tpaccept")
                        .requires(Permissions.require(TPACCEPT_PERMISSION_NODE, 2))
                        .executes(ctx -> {
                            ServerPlayerEntity target = ctx.getSource().getPlayer();

                            // If there's a request pending for the target player
                            if (teleportRequests.containsKey(target) && !teleportRequests.get(target).isEmpty()) {
                                TeleportRequest request = teleportRequests.get(target).remove(0); // Remove the oldest request
                                ServerPlayerEntity requester = request.requester;

                                Map<String, String> replacements = new HashMap<>();
                                replacements.put("{requester}", requester.getName().getString());
                                replacements.put("{target}", target.getName().getString());

                                // Depending on the type of request, perform the appropriate teleportation
                                if (request.type == TeleportRequest.RequestType.TPA) {
                                    Location targetLocation = new Location(target.getWorld(), target.getX(), target.getY(), target.getZ());
                                    targetLocation.teleport(requester);

                                    LangManager.send(requester, "Teleporting-Players");
                                    LangManager.send(target, "TPA-Accept", replacements);
                                } else {  // TeleportRequest.RequestType.TPAHERE
                                    Location requesterLocation = new Location(requester.getWorld(), requester.getX(), requester.getY(), requester.getZ());
                                    requesterLocation.teleport(target);

                                    LangManager.send(target, "Teleporting-Players");
                                    LangManager.send(requester, "TPA-Accept", replacements);
                                }

                                // If there are no more requests pending for the target, remove them from the map
                                if (teleportRequests.get(target).isEmpty()) {
                                    teleportRequests.remove(target);
                                }
                            } else {
                                LangManager.send(target, "TPA-Request-None");
                            }

                            return 1;
                        }));
            }

            @Override
            public String[] getCommandAliases() {
                return new String[]{"tpyes"};
            }
        }.registerWithAliases(dispatcher);

        // Register /tpdeny, /tpno
        // Allows a player to deny a pending teleportation request.
        new AliasedCommand() {
            @Override
            public LiteralCommandNode<ServerCommandSource> register(CommandDispatcher<ServerCommandSource> dispatcher) {
                return dispatcher.register(CommandManager.literal("tpdeny")
                        .executes(ctx -> {
                            ServerPlayerEntity target = ctx.getSource().getPlayer();

                            if (teleportRequests.containsKey(target) && !teleportRequests.get(target).isEmpty()) {
                                TeleportRequest request = teleportRequests.get(target).remove(0); // Remove and get the first request.
                                ServerPlayerEntity requester = request.requester;

                                Map<String, String> replacements = new HashMap<>();
                                replacements.put("{target}", target.getName().getString());
                                replacements.put("{requester}", requester.getName().getString());

                                LangManager.send(requester, "TPA-Deny", replacements);
                                LangManager.send(target, "TPA-Deny", replacements);

                                if (teleportRequests.get(target).isEmpty()) {
                                    teleportRequests.remove(target);
                                }
                            } else {
                                LangManager.send(target, "TPA-Request-None");
                            }

                            return 1;
                        }));
            }

            @Override
            public String[] getCommandAliases() {
                return new String[]{"tpno"};
            }
        }.registerWithAliases(dispatcher);

        // Register /tpacancel
        // Allows a player to cancel an outgoing teleportation request.
        dispatcher.register(CommandManager.literal("tpacancel")
                .requires(Permissions.require(TPACANCEL_PERMISSION_NODE, 2))
                .executes(ctx -> {
                    ServerPlayerEntity requester = ctx.getSource().getPlayer();
                    boolean requestCancelled = false;

                    // Iterate through all teleport requests.
                    for (Map.Entry<ServerPlayerEntity, List<TeleportRequest>> entry : teleportRequests.entrySet()) {
                        List<TeleportRequest> requests = entry.getValue();

                        // Find a matching TeleportRequest for the requester and remove it.
                        for (int i = 0; i < requests.size(); i++) {
                            if (requests.get(i).requester.equals(requester)) {
                                requests.remove(i);
                                requestCancelled = true;
                                LangManager.send(requester, "TPA-Cancel-Self", Map.of("{target}", entry.getKey().getName().getString()));
                                LangManager.send(entry.getKey(), "TPA-Cancel-Target", Map.of("{requester}", requester.getName().getString()));
                                if (requests.isEmpty()) {
                                    teleportRequests.remove(entry.getKey());
                                }
                                break;
                            }
                        }

                        if (requestCancelled) {
                            break;
                        }
                    }

                    if (!requestCancelled) {
                        LangManager.send(requester, "TPA-Request-None");
                    }
                    return 1;
                }));

        // Register /tptoggle
        // Allows a player to toggle incoming teleportation requests.
        dispatcher.register(CommandManager.literal("tptoggle")
                .requires(Permissions.require(TPTOGGLE_PERMISSION_NODE, 2))
                .executes(ctx -> {
                    ServerPlayerEntity player = ctx.getSource().getPlayer();
                    UUID playerId = player.getUuid();

                    if (teleportToggleOff.contains(playerId)) {
                        teleportToggleOff.remove(playerId);
                        LangManager.send(player, "TP-Toggle-On");
                    } else {
                        teleportToggleOff.add(playerId);
                        LangManager.send(player, "TP-Toggle-Off");
                    }

                    return 1;
                }));
    }

    /**
     * Helper method to remove any existing teleport request from a given requester to a target.
     *
     * @param requester Player sending the request.
     * @param target    Player receiving the request.
     */
    private static void removeExistingRequestFrom(ServerPlayerEntity requester, ServerPlayerEntity target) {
        if (teleportRequests.containsKey(target)) {
            List<TeleportRequest> requests = teleportRequests.get(target);
            requests.removeIf(request -> request.requester.equals(requester));
            if (requests.isEmpty()) {
                teleportRequests.remove(target);
            }
        }
    }

    // Duration after which a teleport request is considered expired (2 minutes)
    private static final long TIMEOUT_DURATION = 2 * 60 * 1000;

    /**
     * Checks and removes expired teleport requests.
     */
    public static void checkForExpiredRequests() {
        long currentTimestamp = System.currentTimeMillis();
        List<ServerPlayerEntity> emptyKeys = new ArrayList<>();  // Players with no active requests

        teleportRequests.forEach((target, requests) -> {
            Iterator<TeleportRequest> iterator = requests.iterator();

            while (iterator.hasNext()) {
                TeleportRequest request = iterator.next();

                // If request has expired
                if ((currentTimestamp - request.timestamp) > TIMEOUT_DURATION) {
                    Map<String, String> replacements = new HashMap<>();
                    replacements.put("{receiver}", target.getName().getString());
                    replacements.put("{requester}", request.requester.getName().getString());

                    LangManager.send(request.requester, "TPA-Requester-Timeout", replacements);
                    LangManager.send(target, "TPA-Receiver-Timeout", replacements);
                    iterator.remove();
                }
            }

            if (requests.isEmpty()) {
                emptyKeys.add(target);
            }
        });

        // Clean up the map by removing players with no active requests
        for (ServerPlayerEntity key : emptyKeys) {
            teleportRequests.remove(key);
        }
    }

}