package EEssentials.commands.other;

import EEssentials.lang.LangManager;
import EEssentials.storage.PlayerStorage;
import com.mojang.brigadier.CommandDispatcher;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

public class SocialSpyCommand {
    // Permission node for the Social Spy command.
    public static final String SOCIALSPY_PERMISSION_NODE = "eessentials.socialspy";

    /**
     * Registers the social spy command.
     *
     * @param dispatcher The command dispatcher to register commands on.
     */
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("socialspy")
                .requires(Permissions.require(SOCIALSPY_PERMISSION_NODE, 2))
                .executes(ctx -> {
                    ServerPlayerEntity player = ctx.getSource().getPlayer();
                    PlayerStorage storage = PlayerStorage.fromPlayer(player);

                    if (storage != null) {
                        toggleSocialSpy(storage);
                        if (storage.getSocialSpyFlag()) {
                            LangManager.send(player, "SocialSpy-Enabled");
                        } else {
                            LangManager.send(player, "SocialSpy-Disabled");
                        }
                    }
                    return 1;
                }));
    }

    private static void toggleSocialSpy(PlayerStorage storage) {
        storage.socialSpyFlag = !storage.socialSpyFlag;
        storage.save();
    }

    public static boolean isSocialSpyEnabled(ServerPlayerEntity player) {
        PlayerStorage storage = PlayerStorage.fromPlayerUUID(player.getUuid());
        return storage != null && storage.getSocialSpyFlag();
    }
}
