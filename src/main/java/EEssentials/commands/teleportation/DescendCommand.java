package EEssentials.commands.teleportation;

import EEssentials.lang.LangManager;
import EEssentials.util.Location;
import EEssentials.util.TeleportUtil;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Pair;

import java.util.HashMap;
import java.util.Map;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public final class DescendCommand {
    public static final String DESCEND_PERMISSION_NODE = "eessentials.descend";

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        Map<String, String> replacements = new HashMap<>();
        dispatcher.register(literal("descend")
                .requires(source ->
                        Permissions.check(source, DESCEND_PERMISSION_NODE, 2))
                .executes(context -> {
                    ServerPlayerEntity player = context.getSource().getPlayer();
                    if (player != null) {
                        Pair<Integer, Location> belowLocation = getOpenYLocationBelow(player, 1);
                        if (belowLocation == null) {
                            LangManager.send(context.getSource().getPlayer(), "Descend-Location-Not-Found");
                        } else {
                            belowLocation.getRight().teleport(player);
                            belowLocation.getRight().addReplacements(replacements);
                            LangManager.send(context.getSource().getPlayer(), "Descended-Message", replacements);
                        }
                    } else {
                        LangManager.send(context.getSource().getPlayer(), "Invalid-Player-Only");
                    }
                    return Command.SINGLE_SUCCESS;
                })
                .then(argument("levels", IntegerArgumentType.integer(1))
                        .executes(context -> {
                            int levels = context.getArgument("levels", Integer.class);
                            ServerPlayerEntity player = context.getSource().getPlayer();
                            if (player != null) {
                                Pair<Integer, Location> belowLocation = getOpenYLocationBelow(player, levels);
                                if (belowLocation == null) {
                                    LangManager.send(context.getSource().getPlayer(), "Descend-Location-Not-Found");
                                } else {
                                    belowLocation.getRight().teleport(player);
                                    belowLocation.getRight().addReplacements(replacements);
                                    replacements.put("{levels}", String.valueOf(belowLocation.getLeft()));
                                    LangManager.send(context.getSource().getPlayer(), "Descended-Levels-Message", replacements);
                                }
                            } else {
                                LangManager.send(context.getSource().getPlayer(), "Invalid-Player-Only");
                            }
                            return Command.SINGLE_SUCCESS;
                        })));
    }

    private static Pair<Integer, Location> getOpenYLocationBelow(ServerPlayerEntity player, int levels) {
        double teleportY = player.getY();
        for(int l = 0; l < levels; l++) {
            double nextY = TeleportUtil.findNextBelow(player.getWorld(), player.getX(), teleportY, player.getZ());
            if(nextY == -1000D) {
                if(teleportY == player.getY()) return null;
                return new Pair<>(l, new Location(player.getWorld(), player.getX(), teleportY, player.getZ()));
            } else teleportY = nextY;
        }
        return new Pair<>(levels, new Location(player.getWorld(), player.getX(), teleportY, player.getZ()));
    }
}