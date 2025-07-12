package EEssentials.commands.teleportation;

import EEssentials.lang.LangManager;
import EEssentials.util.Location;
import EEssentials.util.TeleportUtil;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.HashMap;
import java.util.Map;

import static net.minecraft.server.command.CommandManager.literal;

public class TopCommand {
    public static final String TOP_PERMISSION_NODE = "eessentials.top";

    /**
     * Registers the /top command.
     *
     * @param dispatcher The command dispatcher to register commands on.
     */
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        Map<String, String> replacements = new HashMap<>();
        dispatcher.register(literal("top")
                .requires(source -> Permissions.check(source, TOP_PERMISSION_NODE, 2))
                .executes(context -> {
                    ServerPlayerEntity player = context.getSource().getPlayer();
                    if(player != null) {
                        double teleportY = TeleportUtil.findNextBelowNoCaves(player.getWorld(), player.getX(), 320, player.getZ());
                        if(teleportY == -1000) {
                            LangManager.send(context.getSource().getPlayer(), "Top-Unsafe-Message");
                        } else if(teleportY == player.getY()) {
                            LangManager.send(context.getSource().getPlayer(), "Top-Already-Highest");
                        } else {
                            Location tpLocation = new Location(player.getWorld(), player.getX(), teleportY, player.getZ());
                            tpLocation.teleport(player);
                            tpLocation.addReplacements(replacements);
                            LangManager.send(context.getSource().getPlayer(), "Top-Success-Message", replacements);
                        }
                    } else {
                        LangManager.send(context.getSource().getPlayer(), "Invalid-Player-Only");
                    }
                    return Command.SINGLE_SUCCESS;
                }));
    }
}
