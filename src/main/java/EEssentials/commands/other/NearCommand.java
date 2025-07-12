package EEssentials.commands.other;

import EEssentials.lang.LangManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static net.minecraft.server.command.CommandManager.literal;

/**
 * Provides command to find nearby players.
 */
public class NearCommand {

    // Permission node for the near command.
    public static final String NEAR_PERMISSION_NODE = "eessentials.near";

    // TODO: make this configurable in config.yml
    private static final int MAX_RADIUS = 150;

    /**
     * Registers the near command.
     *
     * @param dispatcher The command dispatcher to register commands on.
     */
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
                literal("near")
                        .requires(Permissions.require(NEAR_PERMISSION_NODE, 2))
                        .executes(NearCommand::findNearbyPlayers)
        );
    }

    /**
     * Finds and lists nearby players within the specified radius.
     *
     * @param ctx The command context.
     * @return 1 if successful, 0 otherwise.
     */
    private static int findNearbyPlayers(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (player == null) return 0;

        BlockPos playerPos = player.getBlockPos();
        List<ServerPlayerEntity> nearbyPlayers = player.getServer().getPlayerManager().getPlayerList().stream()
                .filter(otherPlayer -> otherPlayer != player)
                .filter(otherPlayer -> otherPlayer.getBlockPos().isWithinDistance(playerPos, MAX_RADIUS))
                .collect(Collectors.toList());

        if (nearbyPlayers.isEmpty()) {
            Map<String, String> replacements = new HashMap<>();
            replacements.put("{radius}", String.valueOf(MAX_RADIUS));
            LangManager.send(player, "Near-No-Players", replacements);
        } else {
            StringBuilder playerList = new StringBuilder();
            nearbyPlayers.forEach(otherPlayer -> {
                double distance = Math.sqrt(playerPos.getSquaredDistance(otherPlayer.getBlockPos()));
                String entry = LangManager.getLang("Near-Player-Entry")
                        .replace("{player}", otherPlayer.getName().getString())
                        .replace("{distance}", String.format("%.0f", distance));
                playerList.append(entry).append("\n");
            });

            Map<String, String> replacements = new HashMap<>();
            replacements.put("{playerList}", playerList.toString().trim());
            LangManager.send(player, "Near-Player-List", replacements);
        }

        return 1;
    }

}
