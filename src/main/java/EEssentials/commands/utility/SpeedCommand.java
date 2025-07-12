package EEssentials.commands.utility;

import EEssentials.lang.LangManager;
import EEssentials.mixins.PlayerAbilitiesMixin;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerAbilities;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.Map;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class SpeedCommand {

    public static final String SPEED_PERMISSION_NODE = "eessentials.speed.set";
    public static final String FLY_SPEED_PERMISSION_NODE = "eessentials.speed.fly";
    public static final String WALK_SPEED_PERMISSION_NODE = "eessentials.speed.walk";
    public static final String SPEED_OTHER_PERMISSION_NODE = "eessentials.speed.other";

    private static final Identifier SPEED_MODIFIER_ID = Identifier.of("eessentials:movement_speed_boost");

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        LiteralArgumentBuilder<ServerCommandSource> speedCommand = literal("speed")
                .requires(src -> Permissions.check(src, SPEED_PERMISSION_NODE, 2));

        RequiredArgumentBuilder<ServerCommandSource, Float> speedArgument = argument("speedMultiplier", FloatArgumentType.floatArg(0.1f, 10.0f))
                .requires(src -> Permissions.check(src, FLY_SPEED_PERMISSION_NODE, 2))
                .executes(ctx -> executeSetFlightSpeed(ctx, FloatArgumentType.getFloat(ctx, "speedMultiplier")));

        speedArgument.then(argument("target", EntityArgumentType.player())
                .requires(src -> Permissions.check(src, SPEED_OTHER_PERMISSION_NODE, 2))
                .executes(ctx -> executeSetFlightSpeed(ctx, FloatArgumentType.getFloat(ctx, "speedMultiplier"))));

        speedCommand.then(literal("fly").then(speedArgument));

        RequiredArgumentBuilder<ServerCommandSource, Float> walkSpeedArgument = argument("speedMultiplier", FloatArgumentType.floatArg(0.1f, 10.0f))
                .requires(src -> Permissions.check(src, WALK_SPEED_PERMISSION_NODE, 2))
                .executes(ctx -> executeSetWalkSpeed(ctx, FloatArgumentType.getFloat(ctx, "speedMultiplier")));

        walkSpeedArgument.then(argument("target", EntityArgumentType.player())
                .requires(src -> Permissions.check(src, SPEED_OTHER_PERMISSION_NODE, 2))
                .executes(ctx -> executeSetWalkSpeed(ctx, FloatArgumentType.getFloat(ctx, "speedMultiplier"))));

        speedCommand.then(literal("walk").then(walkSpeedArgument));
        dispatcher.register(speedCommand);
    }

    private static int executeSetFlightSpeed(CommandContext<ServerCommandSource> ctx, float speedMultiplier) {
        ServerCommandSource source = ctx.getSource();
        ServerPlayerEntity player = source.getPlayer();

        // Access the player's abilities using the accessor
        PlayerAbilities abilities = player.getAbilities();
        ((PlayerAbilitiesMixin) abilities).setFlySpeed(0.05f * speedMultiplier);
        player.sendAbilitiesUpdate();

        Map<String, String> replacements = Map.of(
                "{speed-option}", "flight",
                "{speed-multiplier}", String.valueOf(speedMultiplier),
                "{player}", player.getName().getString()
        );
        LangManager.send(source.getPlayer(), "Speed-Set-Self", replacements);

        return 1;
    }

    private static int executeSetWalkSpeed(CommandContext<ServerCommandSource> ctx, float speedMultiplier) {
        ServerCommandSource source = ctx.getSource();
        ServerPlayerEntity player = source.getPlayer();

        if (player == null) return 1;
        EntityAttributeInstance attribute = player.getAttributeInstance(EntityAttributes.MOVEMENT_SPEED);
        if (attribute == null) return 1;

        EntityAttributeModifier modifier = new EntityAttributeModifier(
                SPEED_MODIFIER_ID,
                speedMultiplier - 1,
                EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE
        );


        attribute.removeModifier(SPEED_MODIFIER_ID);
        attribute.addPersistentModifier(modifier);

        Map<String, String> replacements = Map.of(
                "{speed-option}", "walking",
                "{speed-multiplier}", String.valueOf(speedMultiplier),
                "{player}", player.getName().getString()
        );
        LangManager.send(source.getPlayer(), "Speed-Set-Self", replacements);

        return 1;
    }
}
