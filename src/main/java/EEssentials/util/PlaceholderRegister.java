package EEssentials.util;

import eu.pb4.placeholders.api.Placeholders;
import eu.pb4.placeholders.api.PlaceholderResult;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.stat.Stats;
import net.minecraft.util.Identifier;
import java.time.Duration;
import java.time.Instant;
import EEssentials.commands.other.PlaytimeCommand;
import EEssentials.commands.other.SeenCommand;
import EEssentials.storage.PlayerStorage;

public class PlaceholderRegister {

    public static void RegisterPlaceholders() {

        // PLAYTIME PLACEHOLDER
        Placeholders.register(Identifier.of("eessentials", "playtime"), (ctx, arg) -> {
            if (!ctx.hasPlayer()) {
                return PlaceholderResult.invalid("No player found!");
            }

            ServerPlayerEntity player = ctx.player();
            int timePlayed = player.getStatHandler().getStat(Stats.CUSTOM.getOrCreateStat(Stats.PLAY_TIME));
            String formattedTime = PlaytimeCommand.formatTime(timePlayed);

            return PlaceholderResult.value(formattedTime);
        });

        // LAST SEEN PLACEHOLDER
        Placeholders.register(Identifier.of("eessentials", "last_seen"), (ctx, arg) -> {
            if (!ctx.hasPlayer()) {
                return PlaceholderResult.invalid("No player found!");
            }

            ServerPlayerEntity player = ctx.player();
            PlayerStorage storage = PlayerStorage.fromPlayerUUID(player.getUuid());

            if (storage == null || storage.getLastTimeOnline() == null) {
                return PlaceholderResult.invalid("Player data not available!");
            }

            Instant lastOnline = storage.getLastTimeOnline();
            Duration duration = Duration.between(lastOnline, Instant.now());
            String timeString = SeenCommand.formatDuration(duration);

            return PlaceholderResult.value(timeString);
        });
    }
}
