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

public final class AscendCommand {
    public static final String ASCEND_PERMISSION_NODE = "eessentials.ascend";

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        Map<String, String> replacements = new HashMap<>();
        dispatcher.register(literal("ascend")
                .requires(source ->
                        Permissions.check(source, ASCEND_PERMISSION_NODE, 2))
                .executes(context -> {
                    ServerPlayerEntity player = context.getSource().getPlayer();
                    if(player != null) {
                        Pair<Integer, Location> aboveLocation = getOpenYLocationAbove(player, 1);
                        if(aboveLocation == null) {
                            LangManager.send(context.getSource().getPlayer(), "Ascend-Location-Not-Found");
                        } else {
                            aboveLocation.getRight().teleport(player);
                            aboveLocation.getRight().addReplacements(replacements);
                            LangManager.send(context.getSource().getPlayer(), "Ascended-Message", replacements);
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
                            if(player != null) {
                                Pair<Integer, Location> aboveLocation = getOpenYLocationAbove(player, levels);
                                if(aboveLocation == null) {
                                    LangManager.send(context.getSource().getPlayer(), "Ascend-Location-Not-Found");
                                } else {
                                    aboveLocation.getRight().teleport(player);
                                    aboveLocation.getRight().addReplacements(replacements);
                                    replacements.put("{levels}", String.valueOf(aboveLocation.getLeft()));
                                    LangManager.send(context.getSource().getPlayer(), "Ascended-Levels-Message", replacements);
                                }
                            } else {
                                LangManager.send(context.getSource().getPlayer(), "Invalid-Player-Only");
                            }
                            return Command.SINGLE_SUCCESS;
                        })));
    }

    private static Pair<Integer, Location> getOpenYLocationAbove(ServerPlayerEntity player, int levels) {
        double teleportY = player.getY();
        for(int l = 0; l < levels; l++) {
            double nextY = TeleportUtil.findNextAbove(player.getWorld(), player.getX(), teleportY, player.getZ());
            if(nextY == -1000D) {
                if(teleportY == player.getY()) return null;
                return new Pair<>(l, new Location(player.getWorld(), player.getX(), teleportY, player.getZ()));
            } else teleportY = nextY;
        }
        return new Pair<>(levels, new Location(player.getWorld(), player.getX(), teleportY, player.getZ()));
    }
}