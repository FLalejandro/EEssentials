package EEssentials.commands.teleportation;

import EEssentials.lang.LangManager;
import EEssentials.storage.PlayerStorage;
import EEssentials.storage.StorageManager;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.command.CommandSource;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import EEssentials.util.Location;

import java.io.File;
import java.util.Map;
import java.util.UUID;

/**
 * Provides a command to teleport to an offline player's last logout location.
 */
public class TPOfflineCommand {

    // Permission node required to use the tpoffline command.
    public static final String TP_OFFLINE_PERMISSION_NODE = "eessentials.tpoffline";

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
                CommandManager.literal("tpoffline")
                        .requires(Permissions.require(TP_OFFLINE_PERMISSION_NODE, 2))
                        .then(CommandManager.argument("target", StringArgumentType.string())
                                .suggests((ctx, builder) -> CommandSource.suggestMatching(ctx.getSource().getServer().getPlayerNames(), builder))
                                .executes(ctx -> {
                                    String targetName = StringArgumentType.getString(ctx, "target");
                                    return teleportToOfflinePlayer(ctx, targetName);
                                }))
        );
    }

    private static int teleportToOfflinePlayer(CommandContext<ServerCommandSource> ctx, String targetName) {
        ServerCommandSource source = ctx.getSource();
        ServerPlayerEntity executor = source.getPlayer();

        // If the target player is online, notify the executor
        if (source.getServer().getPlayerManager().getPlayer(targetName) != null) {
            LangManager.send(source.getPlayer(), "TPOffline-Online-Notify", Map.of("{player}", targetName));
            return 1;
        }

        GameProfile profile = getProfileForName(targetName);
        if (profile == null) {
            LangManager.send(source.getPlayer(), "Invalid-Player", Map.of("{input}", targetName));
            return 0;
        }

        PlayerStorage storage = PlayerStorage.fromPlayerUUID(profile.getId());
        if (storage == null) {
            LangManager.send(source.getPlayer(), "TPOffline-No-Data", Map.of("{player}", targetName));
            return 0;
        }

        Location lastLogoutLocation = storage.getLogoutLocation();
        if (lastLogoutLocation == null) {
            LangManager.send(source.getPlayer(), "TPOffline-No-Logout-Location", Map.of("{player}", targetName));
            return 1;
        }

        lastLogoutLocation.teleport(executor);
        LangManager.send(source.getPlayer(), "TPOffline-Success", Map.of("{player}", targetName));

        return 1;
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
