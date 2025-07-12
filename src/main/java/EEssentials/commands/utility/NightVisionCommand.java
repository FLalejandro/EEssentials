package EEssentials.commands.utility;

import EEssentials.commands.AliasedCommand;
import EEssentials.lang.LangManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.HashMap;
import java.util.Map;

import static net.minecraft.server.command.CommandManager.argument;

/**
 * Provides command to toggle night vision for players.
 */
public class NightVisionCommand {

    // Permission nodes for the night vision command.
    public static final String NIGHTVISION_SELF_PERMISSION_NODE = "eessentials.nightvision.self";
    public static final String NIGHTVISION_OTHER_PERMISSION_NODE = "eessentials.nightvision.other";

    /**
     * Registers the night vision command.
     *
     * @param dispatcher The command dispatcher to register commands on.
     */
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        // Register /nightvision and /nv
        new AliasedCommand() {
            @Override
            public LiteralCommandNode<ServerCommandSource> register(CommandDispatcher<ServerCommandSource> dispatcher) {
                return dispatcher.register(CommandManager.literal("nightvision")
                        .requires(Permissions.require(NIGHTVISION_SELF_PERMISSION_NODE, 2))
                        .executes(ctx -> toggleNightVision(ctx))
                            .then(argument("target", EntityArgumentType.player())
                                .requires(Permissions.require(NIGHTVISION_OTHER_PERMISSION_NODE, 2))
                                .suggests((ctx, builder) -> CommandSource.suggestMatching(ctx.getSource().getServer().getPlayerNames(), builder))
                                .executes(ctx -> {
                                    ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "target");
                                    return toggleNightVision(ctx, target);
                                })));
            }
            @Override
            public String[] getCommandAliases() {
                return new String[]{"nv"};
            }
        }.registerWithAliases(dispatcher);
    }

    /**
     * Toggles the night vision effect for the target players.
     *
     * @param ctx The command context.
     * @param targets The target players.
     * @return 1 if successful, 0 otherwise.
     */
    private static int toggleNightVision(CommandContext<ServerCommandSource> ctx, ServerPlayerEntity... targets) {
        ServerCommandSource source = ctx.getSource();
        ServerPlayerEntity targetPlayer = targets.length > 0 ? targets[0] : source.getPlayer();

        if (targetPlayer == null) return 0;

        // Toggle the night vision effect
        boolean hasNightVision = targetPlayer.hasStatusEffect(StatusEffects.NIGHT_VISION);

        if (hasNightVision) {
            targetPlayer.removeStatusEffect(StatusEffects.NIGHT_VISION);
        } else {
            StatusEffectInstance nightVisionEffect = new StatusEffectInstance(StatusEffects.NIGHT_VISION, 1_000_000, 0, false, false);
            targetPlayer.addStatusEffect(nightVisionEffect);
        }

        if (targetPlayer.equals(source.getPlayer())) {
            // Player is toggling their own night vision
            String key = hasNightVision ? "NightVision-Disabled-Self" : "NightVision-Enabled-Self";
            LangManager.send(targetPlayer, key);
        } else {
            // Notify the target about their night vision status change
            String notifyKey = hasNightVision ? "NightVision-Disabled-Other" : "NightVision-Enabled-Other";
            Map<String, String> targetReplacements = new HashMap<>();
            targetReplacements.put("{source}", source.getName());
            LangManager.send(targetPlayer, notifyKey, targetReplacements);

            // Notify the source that they toggled night vision for the target
            String sourceKey = hasNightVision ? "NightVision-Disabled-Other-Notify" : "NightVision-Enabled-Other-Notify";
            Map<String, String> sourceReplacements = new HashMap<>();
            sourceReplacements.put("{player}", targetPlayer.getName().getString());
            LangManager.send(source.getPlayer(), sourceKey, sourceReplacements);
        }

        return 1;
    }
}
