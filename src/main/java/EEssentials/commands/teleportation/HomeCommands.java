package EEssentials.commands.teleportation;

import EEssentials.EEssentials;
import EEssentials.lang.LangManager;
import EEssentials.storage.PlayerStorage;
import EEssentials.storage.StorageManager;
import EEssentials.util.Location;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.command.CommandSource;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

/**
 * Defines the /home related commands to manage player homes.
 */
public class HomeCommands {

    public static final String SET_HOME_PERMISSION_NODE = "eessentials.sethome";
    public static final String DELETE_HOME_PERMISSION_NODE = "eessentials.delhome";
    public static final String HOME_PERMISSION_NODE = "eessentials.home.self";
    public static final String HOME_OTHER_PERMISSION_NODE = "eessentials.home.other";


    /**
     * Registers home related commands (/sethome, /delhome, /home, /homes).
     *
     * @param dispatcher The command dispatcher to register commands on.
     */
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        // Set a new home or overwrite an existing one for the player.
        dispatcher.register(literal("sethome")
                .requires(src -> Permissions.check(src, SET_HOME_PERMISSION_NODE, 2))
                .then(argument("name", StringArgumentType.word())
                        .executes(ctx -> {
                            ServerPlayerEntity player = ctx.getSource().getPlayer();
                            String homeName = StringArgumentType.getString(ctx, "name");
                            if (player == null) return 0;

                            PlayerStorage playerStorage = EEssentials.storage.getPlayerStorage(player);

                            // If the new home name isn't already a home, we should check if the player has enough sethomes left
                            if (!playerStorage.homes.containsKey(homeName)) {
                                if (playerStorage.homes.keySet().size() >= EEssentials.perms.getMaxHomes(player)) {
                                    Map<String, String> replacements = new HashMap<>();
                                    replacements.put("{maxhomes}", String.valueOf(EEssentials.perms.getMaxHomes(player)));
                                    LangManager.send(player, "Home-Max-Limit-Message", replacements);
                                    return 0;
                                }
                            }

                            playerStorage.homes.put(
                                    homeName,
                                    Location.fromPlayer(player)
                            );
                            playerStorage.save();

                            LangManager.send(player, "Home-Set", Map.of("{home}", homeName));
                            return 1;
                        })
                )
        );

        // Delete a home for the player.
        dispatcher.register(literal("delhome")
                .then(argument("name", StringArgumentType.word())
                        .requires(src -> Permissions.check(src, DELETE_HOME_PERMISSION_NODE, 2))
                        .suggests(HomeCommands::suggestHomes)
                        .executes(ctx -> {
                            ServerPlayerEntity player = ctx.getSource().getPlayer();
                            String homeName = StringArgumentType.getString(ctx, "name");
                            if (player == null) return 0;

                            PlayerStorage playerStorage = EEssentials.storage.getPlayerStorage(player);

                            Location location = playerStorage.homes.remove(homeName);
                            playerStorage.save();

                            if (location != null) {
                                LangManager.send(player, "Home-Delete", Map.of("{home}", homeName));
                                return 1;
                            } else {
                                LangManager.send(player, "Invalid-Home", Map.of("{input}", homeName));
                                return 0;
                            }
                        })
                )
        );

        // Regular /home command
        dispatcher.register(literal("home")
                .requires(src -> Permissions.check(src, HOME_PERMISSION_NODE, 2))
                .executes(HomeCommands::listHomes) // If just /home is executed
                .then(argument("name", StringArgumentType.word())
                        .suggests(HomeCommands::suggestHomes)
                        .executes(ctx -> {
                            ServerPlayerEntity player = ctx.getSource().getPlayer();
                            String homeName = StringArgumentType.getString(ctx, "name");
                            return teleportToHome(player, homeName);

                        }))
        );

        // Command for teleporting to another player's home
        dispatcher.register(
                literal("home:")
                .requires(src -> Permissions.check(src, HOME_OTHER_PERMISSION_NODE, 2))
                .then(argument("target", StringArgumentType.word())
                        .suggests((ctx, builder) -> CommandSource.suggestMatching(ctx.getSource().getServer().getPlayerNames(), builder))
                        .then(argument("homeName", StringArgumentType.word())
                                .suggests(HomeCommands::suggestTargetHomes)
                                .executes(ctx -> {
                                    String targetName = StringArgumentType.getString(ctx, "target");
                                    String homeName = StringArgumentType.getString(ctx, "homeName");
                                    return teleportToTargetHome(ctx, targetName, homeName);
                                })
                        ))
        );
    }

    private static int listHomes(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (player == null) return 0;

        PlayerStorage playerStorage = EEssentials.storage.getPlayerStorage(player);
        if (playerStorage.homes.isEmpty()) {
            LangManager.send(player, "Home-List-Empty");
        } else {
            LangManager.send(player, "Home-List", Map.of("{homes}", String.join(", ", playerStorage.homes.keySet())));
        }

        return 1;
    }

    private static int teleportToHome(ServerPlayerEntity player, String homeName) {
        if (player == null) return 0;

        PlayerStorage playerStorage = EEssentials.storage.getPlayerStorage(player);
        Location location = playerStorage.homes.get(homeName);

        if (location != null) {
            location.teleport(player);
            LangManager.send(player, "Teleporting-To-Home", Map.of("{home}", homeName));
            return 1;
        } else {
            LangManager.send(player, "Invalid-Home", Map.of("{input}", homeName));
            return 0;
        }
    }
    public static CompletableFuture<Suggestions> suggestHomes(CommandContext<ServerCommandSource> ctx, SuggestionsBuilder builder) {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (player == null) return CommandSource.suggestMatching(new String[]{""}, builder);
        return CommandSource.suggestMatching(EEssentials.storage.getPlayerStorage(ctx.getSource().getPlayer().getUuid()).homes.keySet(), builder);
    }

    private static int teleportToTargetHome(CommandContext<ServerCommandSource> ctx, String targetName, String homeName) {
        ServerCommandSource source = ctx.getSource();
        UUID targetUUID;
        GameProfile profile = getProfileForName(targetName);

        if (profile != null) {
            targetUUID = profile.getId();
        } else {
            LangManager.send(source.getPlayer(), "Invalid-Player", Map.of("{input}", targetName));
            return 0;
        }

        PlayerStorage targetStorage = PlayerStorage.fromPlayerUUID(targetUUID);
        Location location = targetStorage.homes.get(homeName);

        if (location != null) {
            location.teleport(source.getPlayer());
            LangManager.send(source.getPlayer(), "Teleporting-To-Other-Home", Map.of("{home}", homeName, "{target}", targetName));
            return 1;
        } else {
            LangManager.send(source.getPlayer(), "Invalid-Home", Map.of("{input}", homeName));
            return 0;
        }
    }

    public static CompletableFuture<Suggestions> suggestTargetHomes(CommandContext<ServerCommandSource> ctx, SuggestionsBuilder builder) {
        String targetName = StringArgumentType.getString(ctx, "target");
        GameProfile profile = getProfileForName(targetName);

        if (profile == null || profile.getId() == null) {
            return CommandSource.suggestMatching(new String[]{}, builder);
        }

        UUID targetUUID = profile.getId();
        return CommandSource.suggestMatching(PlayerStorage.fromPlayerUUID(targetUUID).homes.keySet(), builder);
    }

    private static GameProfile getProfileForName(String name) {
        File[] files = StorageManager.playerStorageDirectory.toFile().listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.getName().endsWith(".json")) {
                    PlayerStorage storage = PlayerStorage.fromPlayerUUID(UUID.fromString(file.getName().replace(".json", "")));
                    if (storage != null && name.equals(storage.getPlayerName())) {
                        return new GameProfile(storage.getPlayerUUID(), name);
                    }
                }
            }
        }
        return null;
    }

}
