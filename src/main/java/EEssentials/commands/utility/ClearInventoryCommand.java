package EEssentials.commands.utility;

import EEssentials.lang.LangManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Map;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

/**
 * Provides command to clear the player's inventory.
 */
public class ClearInventoryCommand {

    // Permission node for the clear inventory command.
    public static final String CLEAR_INVENTORY_SELF_PERMISSION_NODE = "eessentials.clearinventory.self";
    public static final String CLEAR_INVENTORY_OTHER_PERMISSION_NODE = "eessentials.clearinventory.other";

    /**
     * Registers the clear inventory command.
     *
     * @param dispatcher The command dispatcher to register commands on.
     */
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
                literal("clearinventory")
                        .requires(Permissions.require(CLEAR_INVENTORY_SELF_PERMISSION_NODE, 2))
                        .executes(ClearInventoryCommand::clearInventory)  // Clears the inventory of the executing player
                        .then(argument("target", EntityArgumentType.player())
                                .requires(Permissions.require(CLEAR_INVENTORY_OTHER_PERMISSION_NODE, 2))
                                .suggests((ctx, builder) -> CommandSource.suggestMatching(ctx.getSource().getServer().getPlayerNames(), builder))
                                .executes(ctx -> {
                                    ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "target");
                                    return clearInventory(ctx, target);  // Clears the inventory of the specified player
                                }))
        );

        // CI is an alias for Clear Inventory
        dispatcher.register(
                literal("ci")
                        .requires(Permissions.require(CLEAR_INVENTORY_SELF_PERMISSION_NODE, 2))
                        .executes(ClearInventoryCommand::clearInventory)  // Clears the inventory of the executing player
                        .then(argument("target", EntityArgumentType.player())
                                .requires(Permissions.require(CLEAR_INVENTORY_OTHER_PERMISSION_NODE, 2))
                                .suggests((ctx, builder) -> CommandSource.suggestMatching(ctx.getSource().getServer().getPlayerNames(), builder))
                                .executes(ctx -> {
                                    ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "target");
                                    return clearInventory(ctx, target);  // Clears the inventory of the specified player
                                }))
        );
    }

    /**
     * Clears the inventory of the target player.
     *
     * @param ctx The command context.
     * @param targets The target players.
     * @return 1 if successful, 0 otherwise.
     */
    private static int clearInventory(CommandContext<ServerCommandSource> ctx, ServerPlayerEntity... targets) {
        ServerCommandSource source = ctx.getSource();
        ServerPlayerEntity player = targets.length > 0 ? targets[0] : source.getPlayer();

        if (player == null) return 0;

        PlayerInventory playerInv = player.getInventory();

        // Clearing main inventory
        playerInv.clear();

        // Clearing armor slots
        for (int i = 5; i <= 8; i++) {
            playerInv.setStack(i, ItemStack.EMPTY);
        }

        // Clearing off-hand slot
        playerInv.setStack(PlayerInventory.OFF_HAND_SLOT, ItemStack.EMPTY);

        if (player.equals(source.getPlayer())) {
            LangManager.send(player, "ClearInventory-Self");
        } else {
            LangManager.send(source.getPlayer(), "ClearInventory-Other", Map.of("{player}", player.getName().getString()));
        }
        return 1;
    }

}
