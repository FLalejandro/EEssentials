package EEssentials.commands.utility;

import EEssentials.lang.LangManager;
import EEssentials.settings.RepairSettings;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.collection.DefaultedList;

public class RepairCommand {

    // Permission node for the repair command.
    public static final String REPAIR_PERMISSION_NODE = "eessentials.repair";
    public static final String REPAIR_ALL_PERMISSION_NODE = "eessentials.repair.all";

    /**
     * Registers the repair command.
     *
     * @param dispatcher The command dispatcher to register commands on.
     */
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
                CommandManager.literal("repair")
                        .requires(Permissions.require(REPAIR_PERMISSION_NODE, 2))
                        .executes(RepairCommand::repairItem)
                        .then(CommandManager.literal("all")
                                .requires(Permissions.require(REPAIR_ALL_PERMISSION_NODE, 2))
                                .executes(RepairCommand::repairAllItems))
        );
    }

    /**
     * Repairs the item in the player's hand.
     *
     * @param ctx The command context.
     * @return 1 if successful, 0 otherwise.
     */
    private static int repairItem(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource source = ctx.getSource();
        ServerPlayerEntity player;

        try {
            player = source.getPlayer();
            if (player == null) {
                return 0;
            }

            ItemStack itemInHand = player.getMainHandStack();
            if (!itemInHand.isDamageable()) {
                LangManager.send(player, "Repair-Invalid");
                return 0;
            }

            if (RepairSettings.isBlacklisted(itemInHand)) {
                LangManager.send(player, "Repair-Invalid");
                return 0;
            }

            itemInHand.setDamage(0); // Repair the item
            LangManager.send(player, "Repair-Success");

            return 1;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * Repairs all non-blacklisted items in the player's inventory.
     *
     * @param ctx The command context.
     * @return 1 if successful, 0 otherwise.
     */
    private static int repairAllItems(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource source = ctx.getSource();
        ServerPlayerEntity player;

        try {
            player = source.getPlayer();
            if (player == null) {
                return 0;
            }

            DefaultedList<ItemStack> inventory = DefaultedList.ofSize(player.getInventory().size());
            for (int i = 0; i < player.getInventory().size(); i++) {
                inventory.add(player.getInventory().getStack(i));
            }

            boolean repairedAnyItem = false;

            for (ItemStack item : inventory) {
                if (item.isDamageable() && item.getDamage() > 0 && !RepairSettings.isBlacklisted(item)) {
                    item.setDamage(0);
                    repairedAnyItem = true;
                }
            }

            if (repairedAnyItem) {
                LangManager.send(player, "Repair-All-Success");
                return 1;
            } else {
                LangManager.send(player, "Repair-All-No-Valid-Items");
                return 0;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }
}
