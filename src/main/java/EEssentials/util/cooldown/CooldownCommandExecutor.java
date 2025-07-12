package EEssentials.util.cooldown;

import EEssentials.config.Configuration;
import EEssentials.lang.LangManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import com.mojang.brigadier.context.CommandContext;
import me.lucko.fabric.api.permissions.v0.Permissions;

import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

public class CooldownCommandExecutor {

    private final Configuration config;

    public CooldownCommandExecutor(Configuration config) {
        this.config = config;
    }

    public int execute(CommandContext<ServerCommandSource> context, String commandName, Function<CommandContext<ServerCommandSource>, Integer> commandLogic) {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();

        if (player == null) {
            LangManager.send(source.getPlayer(), "Invalid-Player-Only");
            return 1;
        }

        // Check if the player is OP or has the bypass permission
        if (source.hasPermissionLevel(2) || Permissions.check(source, "eessentials.ignorecooldowns", 2)) {
            return commandLogic.apply(context);
        }

        UUID playerUUID = player.getUuid();
        int cooldownTime = config.getInt("Command-Cooldowns." + commandName, 0);

        long remainingCooldown = CooldownHelper.getCooldown(playerUUID, commandName);

        if (remainingCooldown > 0) {
            LangManager.send(source.getPlayer(), "Cooldown-Active", Map.of("{command}", commandName, "{cooldown}", String.valueOf(remainingCooldown)));
            return 1;
        }

        int result = commandLogic.apply(context);
        CooldownHelper.startCooldown(playerUUID, commandName, cooldownTime);
        return result;
    }
}
