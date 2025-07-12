package EEssentials.commands.teleportation;

import EEssentials.commands.AliasedCommand;
import EEssentials.lang.LangManager;
import EEssentials.settings.randomteleport.RTPSettings;
import EEssentials.settings.randomteleport.RTPWorldSettings;
import EEssentials.util.AsynchronousUtil;
import EEssentials.util.Location;
import EEssentials.util.TeleportUtil;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.command.CommandSource;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;

import java.util.*;
import java.util.concurrent.CompletableFuture;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class RTPCommand {
    public static final String RTP_PERMISSION_NODE = "eessentials.rtp";
    public static final String RTP_SPECIFIC_PERMISSION_NODE = "eessentials.rtp.specific";
    public static final String RTP_COOLDOWN_BYPASS_PERMISSION_NODE = "eessentials.rtp.bypasscooldown";

    public static final List<String> queuedPlayerNames = new ArrayList<>();

    /**
     * Registers rtp commands.
     *
     * @param dispatcher The command dispatcher to register commands on.
     */
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        new AliasedCommand() {
            @Override
            public LiteralCommandNode<ServerCommandSource> register(CommandDispatcher<ServerCommandSource> dispatcher) {
                return dispatcher.register(literal("randomteleport")
                        .requires(source -> Permissions.check(source, RTP_PERMISSION_NODE, 2))
                        .executes(context -> {
                            ServerPlayerEntity player = context.getSource().getPlayer();
                            if(player != null) {
                                RTPWorldSettings worldSettings = RTPSettings.getWorldSettings(player.getWorld());
                                if(worldSettings != null) {
                                    long playerCooldown = worldSettings.getPlayerCooldown(player);
                                    if(playerCooldown <= 0 || Permissions.check(player, RTP_COOLDOWN_BYPASS_PERMISSION_NODE, 2)) {
                                        if(!queuedPlayerNames.contains(player.getName().getString())) {
                                            LangManager.send(context.getSource().getPlayer(), "RTP-Queued-Message");
                                            queuedPlayerNames.add(player.getName().getString());
                                            CompletableFuture<Void> rtp = teleportToRandomLocation(player, worldSettings);
                                            rtp.whenComplete((location, throwable) ->
                                                    queuedPlayerNames.remove(player.getName().getString()));
                                        } else {
                                            LangManager.send(context.getSource().getPlayer(), "RTP-Already-Queued-Message");
                                        }
                                    } else {
                                        Map<String, String> replacements = new HashMap<>();
                                        replacements.put("{cooldown}", String.valueOf(playerCooldown));
                                        LangManager.send(context.getSource().getPlayer(), "RTP-Cooldown-Message", replacements);
                                    }
                                } else {
                                    LangManager.send(context.getSource().getPlayer(), "RTP-World-Blacklisted");
                                }
                            } else {
                                LangManager.send(context.getSource().getPlayer(), "Invalid-Player-Only");
                            }
                            return Command.SINGLE_SUCCESS;
                        }).then(argument("world", StringArgumentType.greedyString())
                                .requires(Permissions.require(RTP_SPECIFIC_PERMISSION_NODE, 2))
                                .suggests(RTPCommand::suggestWorlds)
                                .executes(context -> {
                                    ServerPlayerEntity player = context.getSource().getPlayer();
                                    if(player != null) {
                                        String world = context.getArgument("world", String.class);
                                        RTPWorldSettings worldSettings = RTPSettings.getWorldSettings(world);
                                        if(worldSettings != null) {
                                            long playerCooldown = worldSettings.getPlayerCooldown(player);
                                            if(playerCooldown <= 0 || Permissions.check(player, RTP_COOLDOWN_BYPASS_PERMISSION_NODE, 2)) {
                                                if(!queuedPlayerNames.contains(player.getName().getString())) {
                                                    LangManager.send(context.getSource().getPlayer(), "RTP-Queued-Message");
                                                    queuedPlayerNames.add(player.getName().getString());
                                                    CompletableFuture<Void> rtp = teleportToRandomLocation(player, worldSettings);
                                                    rtp.whenComplete((location, throwable) ->
                                                            queuedPlayerNames.remove(player.getName().getString()));
                                                } else {
                                                    LangManager.send(context.getSource().getPlayer(), "RTP-Already-Queued-Message");
                                                }
                                            } else {
                                                Map<String, String> replacements = new HashMap<>();
                                                replacements.put("{cooldown}", String.valueOf(playerCooldown));
                                                LangManager.send(context.getSource().getPlayer(), "RTP-Cooldown-Message", replacements);
                                            }
                                        } else {
                                            LangManager.send(context.getSource().getPlayer(), "RTP-World-Blacklisted");
                                        }
                                    } else {
                                        LangManager.send(context.getSource().getPlayer(), "Invalid-Player-Only");
                                    }
                                    return Command.SINGLE_SUCCESS;
                                })));
            }

            @Override
            public String[] getCommandAliases() {
                return new String[]{"rtp"};
            }
        }.registerWithAliases(dispatcher);
    }

    public static CompletableFuture<Suggestions> suggestWorlds(CommandContext<ServerCommandSource> ctx, SuggestionsBuilder builder) {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (player == null) return CommandSource.suggestMatching(new String[]{""}, builder);
        return CommandSource.suggestMatching(RTPSettings.getAllWorlds(), builder);
    }

    private static CompletableFuture<Void> teleportToRandomLocation(ServerPlayerEntity player, RTPWorldSettings worldSettings) {
        return AsynchronousUtil.runTaskAsynchronously(() -> {
            // Generate random coordinates asynchronously
            List<BlockPos> candidatePositions = generateCandidatePositions(worldSettings);
            return candidatePositions;
        }).thenCompose(candidatePositions -> {
            // Process candidates on main thread in chunks to avoid blocking
            return processCandidatesOnMainThread(player, worldSettings, candidatePositions);
        });
    }

    private static List<BlockPos> generateCandidatePositions(RTPWorldSettings settings) {
        List<BlockPos> candidates = new ArrayList<>();
        for(int i = 0; i < RTPSettings.getMaxAttempts(); i++) {
            int x = settings.getRandomIntInBounds();
            int z = settings.getRandomIntInBounds();
            int y = settings.getHighestY();
            candidates.add(new BlockPos(x, y, z));
        }
        return candidates;
    }

    private static CompletableFuture<Void> processCandidatesOnMainThread(ServerPlayerEntity player, RTPWorldSettings worldSettings, List<BlockPos> candidates) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        
        // Process candidates in chunks to avoid blocking the main thread for too long
        processCandidatesChunk(player, worldSettings, candidates, 0, future);
        
        return future;
    }

    private static void processCandidatesChunk(ServerPlayerEntity player, RTPWorldSettings worldSettings, List<BlockPos> candidates, int startIndex, CompletableFuture<Void> future) {
        if (player.isDisconnected()) {
            future.complete(null);
            return;
        }

        MinecraftServer server = player.getServer();
        if (server == null) {
            future.complete(null);
            return;
        }

        // Process a chunk of candidates (10 at a time to avoid blocking)
        int chunkSize = 10;
        int endIndex = Math.min(startIndex + chunkSize, candidates.size());

        server.execute(() -> {
            for (int i = startIndex; i < endIndex; i++) {
                BlockPos candidate = candidates.get(i);
                Location location = validateAndCreateLocation(worldSettings, candidate);
                if (location != null) {
                    // Found a valid location
                    Map<String, String> replacements = new HashMap<>();
                    location.addReplacements(replacements);
                    location.teleport(player);
                    LangManager.send(player, "RTP-Success-Message", replacements);
                    worldSettings.startPlayerCooldown(player);
                    future.complete(null);
                    return;
                }
            }

            // If we haven't found a location yet and there are more candidates
            if (endIndex < candidates.size()) {
                // Schedule the next chunk for the next tick
                server.execute(() -> processCandidatesChunk(player, worldSettings, candidates, endIndex, future));
            } else {
                // No valid location found after all attempts
                Map<String, String> replacements = new HashMap<>();
                replacements.put("{attempts}", String.valueOf(RTPSettings.getMaxAttempts()));
                LangManager.send(player, "RTP-Location-Not-Found", replacements);
                future.complete(null);
            }
        });
    }

    private static Location validateAndCreateLocation(RTPWorldSettings settings, BlockPos candidate) {
        ServerWorld world = settings.getWorld();
        if(world == null) return null;

        int x = candidate.getX();
        int z = candidate.getZ();
        int y;

        if(settings.allowCaveTeleports()) {
            y = (int) TeleportUtil.findNextBelow(world, x, settings.getHighestY(), z);
        } else {
            y = (int) TeleportUtil.findNextBelowNoCaves(world, x, settings.getHighestY(), z);
        }

        if(y != -1000) {
            Optional<RegistryKey<Biome>> optionalBiome = world.getBiome(new BlockPos(x, y, z)).getKey();
            if(optionalBiome.isPresent()) {
                String biomeKey = optionalBiome.get().getValue().toString();
                if (!RTPSettings.isBiomeBlacklisted(biomeKey)) {
                    return new Location(world, x, y, z);
                }
            }
        }
        return null;
    }
}
