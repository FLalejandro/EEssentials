package EEssentials.commands.other;

import EEssentials.EEssentials;
import EEssentials.commands.suggestionproviders.PlayerSuggestionProvider;
import EEssentials.lang.LangManager;
import EEssentials.util.Location;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class SmiteCommand {
    public static final String SMITE_PERMISSION_NODE = "";
    public static final String SMITE_PLAYER_PERMISSION_NODE = "";
    public static final String SMITE_ALL_PERMISSION_NODE = "";

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        Map<String, String> replacements = new HashMap<>();
        dispatcher.register(literal("smite")
                .requires(source ->
                        Permissions.check(source, SMITE_PERMISSION_NODE, 4))
                .executes(context -> {
                    ServerPlayerEntity player = context.getSource().getPlayer();
                    if(player != null) {
                        Location lookingAt = getLookingAt(player);
                        summonLightningAt(player.getWorld(), lookingAt.getX(), lookingAt.getY(), lookingAt.getZ());
                        LangManager.send(context.getSource().getPlayer(), "Smite-Message");
                    } else {
                        LangManager.send(context.getSource().getPlayer(), "Invalid-Player-Only");
                    }
                    return Command.SINGLE_SUCCESS;
                })
                .then(argument("player", StringArgumentType.string())
                        .requires(source ->
                                Permissions.check(source, SMITE_PLAYER_PERMISSION_NODE, 4)
                        )
                        .suggests(((context, builder) -> new PlayerSuggestionProvider(Permissions.check(context.getSource(), SMITE_ALL_PERMISSION_NODE, 4)).getSuggestions(context, builder)))
                        .executes(context -> {
                            String playerName = context.getArgument("player", String.class);
                            if(!playerName.equals("all")) {
                                ServerPlayerEntity target = EEssentials.server.getPlayerManager().getPlayer(playerName);
                                if(target != null) {
                                    summonLightningAt(target.getWorld(), target.getX(), target.getY(), target.getZ());
                                    replacements.put("{player}", target.getName().getString());
                                    LangManager.send(context.getSource().getPlayer(), "Smite-Player-Message", replacements);
                                    LangManager.send(target, "Smited-Message");
                                } else {
                                    replacements.put("{input}", playerName);
                                    LangManager.send(context.getSource().getPlayer(), "Invalid-Player", replacements);
                                }
                            } else {
                                if(Permissions.check(context.getSource(), SMITE_ALL_PERMISSION_NODE, 4)) {
                                    int playerCount = 0;
                                    List<ServerPlayerEntity> onlinePlayers = EEssentials.server.getPlayerManager().getPlayerList();
                                    for(ServerPlayerEntity target : onlinePlayers) {
                                        summonLightningAt(target.getWorld(), target.getX(), target.getY(), target.getZ());
                                        LangManager.send(target, "Smited-Message");
                                        playerCount++;
                                    }
                                    replacements.put("{amount}", String.valueOf(playerCount));
                                    LangManager.send(context.getSource().getPlayer(), "Smite-All-Message", replacements);
                                } else {
                                    LangManager.send(context.getSource().getPlayer(), "Invalid-Permission-All-Target");
                                }
                            }
                            return Command.SINGLE_SUCCESS;
                        })));
    }

    private static Location getLookingAt(ServerPlayerEntity player) {
        HitResult hitResult = player.raycast(500, 0, false);
        Vec3d target = hitResult.getPos();
        return new Location(player.getWorld(), target.x, target.y, target.z);
    }

    private static void summonLightningAt(ServerWorld world, double x, double y, double z) {
        LightningEntity lightning = new LightningEntity(EntityType.LIGHTNING_BOLT, world);
        lightning.setPos(x, y, z);
        world.spawnEntity(lightning);
    }
}
