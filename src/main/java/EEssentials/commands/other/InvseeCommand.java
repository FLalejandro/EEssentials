package EEssentials.commands.other;

import EEssentials.lang.LangManager;
import EEssentials.screens.InventoryScreen;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.command.argument.GameProfileArgumentType;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public class InvseeCommand {

    private static final String INVSEE_EDIT_PERMISSION_NODE = "eessentials.invsee.edit";
    private static final String INVSEE_VIEW_PERMISSION_NODE = "eessentials.invsee.view";

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("invsee")
                .requires(src -> Permissions.check(src, INVSEE_EDIT_PERMISSION_NODE, 2) ||
                        Permissions.check(src, INVSEE_VIEW_PERMISSION_NODE, 2))
                .then(CommandManager.argument("target", GameProfileArgumentType.gameProfile())
                        .executes(ctx -> openInventory(ctx))));
    }

    private static int openInventory(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        GameProfile targetProfile = GameProfileArgumentType.getProfileArgument(ctx, "target").iterator().next();
        ServerPlayerEntity targetPlayer = ctx.getSource().getServer().getPlayerManager().getPlayer(targetProfile.getName());

        if (targetPlayer == null) {
            LangManager.send(ctx.getSource().getPlayer(), "Invalid-Player");
            return 0;
        }

        if (player.getUuid().equals(targetPlayer.getUuid())) {
            LangManager.send(ctx.getSource().getPlayer(), "Invalid-Self-Target");
            return 0;
        }

        boolean canEdit = Permissions.check(player, INVSEE_EDIT_PERMISSION_NODE, 2);

        InventoryScreen gui = new InventoryScreen(ScreenHandlerType.GENERIC_9X5, player, targetPlayer);
        gui.setTitle(Text.literal(targetPlayer.getName().getString() + "'s Inventory"));
        gui.setEditMode(canEdit);
        gui.open();

        return 1;
    }
}
