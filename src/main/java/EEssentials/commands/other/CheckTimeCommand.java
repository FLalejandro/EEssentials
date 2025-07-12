package EEssentials.commands.other;

import EEssentials.lang.LangManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.server.command.ServerCommandSource;

import java.util.Map;

import static net.minecraft.server.command.CommandManager.literal;

/**
 * Provides command to check the current world time.
 */
public class CheckTimeCommand {

    // Permission node for the time command.
    public static final String TIME_PERMISSION_NODE = "eessentials.time";

    /**
     * Registers the time command.
     *
     * @param dispatcher The command dispatcher to register commands on.
     */
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
                literal("time")
                        .requires(Permissions.require(TIME_PERMISSION_NODE, 2))
                        .executes(CheckTimeCommand::checkTime)
        );
    }

    /**
     * Checks and sends the current world time to the player.
     *
     * @param ctx The command context.
     * @return 1 if successful, 0 otherwise.
     */
    private static int checkTime(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource source = ctx.getSource();

        long timeOfDay = source.getWorld().getTimeOfDay() % 24000;
        String formattedTime = formatTime(timeOfDay);

        // Use LangManager to send the formatted time
        LangManager.send(source.getPlayer(), "CheckTime", Map.of("{world-time}", formattedTime));

        return 1;
    }

    /**
     * Converts the in-game time to a readable format.
     *
     * @param ticks The in-game time in ticks.
     * @return A formatted string representing the time.
     */
    private static String formatTime(long ticks) {
        long hours = (ticks / 1000 + 6) % 24;
        long minutes = (ticks % 1000) * 60 / 1000;
        long seconds = ((ticks % 1000) * 60 % 1000) * 60 / 1000;
        String ampm = hours < 12 ? "AM" : "PM";

        if (hours >= 12) hours -= 12;
        if (hours == 0) hours = 12;

        return String.format("%02d:%02d:%02d %s", hours, minutes, seconds, ampm);
    }


}
