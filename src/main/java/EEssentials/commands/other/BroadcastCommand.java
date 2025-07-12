package EEssentials.commands.other;

import EEssentials.lang.*;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.kyori.adventure.text.Component;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.Collection;

/**
 * Provides a command to broadcast messages to all players on the server.
 */
public class BroadcastCommand {

    // Permission node for the broadcast command.
    public static final String BROADCAST_PERMISSION_NODE = "eessentials.broadcast";

    /**
     * Registers the broadcast command.
     *
     * @param dispatcher The command dispatcher to register commands on.
     */
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
                CommandManager.literal("broadcast")
                        .requires(Permissions.require(BROADCAST_PERMISSION_NODE, 2))
                        .then(CommandManager.argument("message", StringArgumentType.greedyString())
                                .executes(ctx -> {
                                    String message = StringArgumentType.getString(ctx, "message");
                                    return broadcastMessage(ctx.getSource(), message);
                                }))
        );
    }

    /**
     * Broadcasts a message to all players on the server.
     *
     * @param source  The source of the command.
     * @param message The message to broadcast.
     * @return 1 if successful, 0 otherwise.
     */
    private static int broadcastMessage(ServerCommandSource source, String message) {
        Collection<ServerPlayerEntity> players = source.getServer().getPlayerManager().getPlayerList();

        // [Broadcast] prefix
        String prefix = LangManager.getLang("Broadcast-Prefix");
        if (prefix == null) {
            prefix = "";
        }

        // Combine prefix and message, supporting modern text formatting
        String formattedMessage = prefix + message;
        Component messageComponent = ColorUtil.parseColour(formattedMessage);

        for (ServerPlayerEntity player : players) {
            String legacy = LegacyComponentSerializer.legacySection().serialize(messageComponent);
            player.sendMessage(net.minecraft.text.Text.literal(legacy));
        }

        return 1;
    }
}
