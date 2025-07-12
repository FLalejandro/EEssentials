package EEssentials.commands.other;

import EEssentials.lang.LangManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.util.Hand;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

/**
 * Provides commands to edit the name and lore of the item in the player's hand.
 */
public class ItemEditorCommand {

    // Permission nodes for the item editor commands.
    public static final String ITEM_EDITOR_NAME_PERMISSION_NODE = "eessentials.itemeditor.name";
    public static final String ITEM_EDITOR_LORE_PERMISSION_NODE = "eessentials.itemeditor.lore";

    /**
     * Registers the item editor commands.
     *
     * @param dispatcher The command dispatcher to register commands on.
     */
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
                literal("itemeditor")
                        .then(literal("name")
                                .requires(Permissions.require(ITEM_EDITOR_NAME_PERMISSION_NODE, 2))
                                .then(argument("name", StringArgumentType.greedyString())
                                        .executes(ctx -> renameItem(ctx, StringArgumentType.getString(ctx, "name")))))
                        .then(literal("lore")
                                .requires(Permissions.require(ITEM_EDITOR_LORE_PERMISSION_NODE, 2))
                                .then(literal("add")
                                        .then(argument("lore", StringArgumentType.greedyString())
                                                .executes(ctx -> addItemLore(ctx, StringArgumentType.getString(ctx, "lore")))))
                                .then(literal("edit")
                                        .then(argument("line", IntegerArgumentType.integer(1))
                                                .then(argument("lore", StringArgumentType.greedyString())
                                                        .executes(ctx -> editItemLore(ctx, IntegerArgumentType.getInteger(ctx, "line"), StringArgumentType.getString(ctx, "lore"))))))
                                .then(literal("delete")
                                        .then(argument("line", IntegerArgumentType.integer(1))
                                                .executes(ctx -> deleteItemLore(ctx, IntegerArgumentType.getInteger(ctx, "line"))))))
                        .then(literal("reset")
                                .requires(Permissions.require(ITEM_EDITOR_NAME_PERMISSION_NODE, 2).or(Permissions.require(ITEM_EDITOR_LORE_PERMISSION_NODE, 2)))
                                .executes(ItemEditorCommand::resetItem))
        );
    }

    /**
     * Renames the item in the player's hand.
     *
     * @param ctx  The command context.
     * @param name The new name for the item.
     * @return 1 if successful, 0 otherwise.
     */
    private static int renameItem(CommandContext<ServerCommandSource> ctx, String name) {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (player == null) {
            LangManager.send(ctx.getSource().getPlayer(), "Invalid-Player-Only");
            return 0;
        }

        ItemStack itemStack = player.getStackInHand(Hand.MAIN_HAND);
        if (itemStack.isEmpty()) {
            LangManager.send(ctx.getSource().getPlayer(), "Invalid-Item-In-Hand");
            return 0;
        }

        if (!Permissions.check(player, ITEM_EDITOR_NAME_PERMISSION_NODE, false)) {
            LangManager.send(ctx.getSource().getPlayer(), "Invalid-No-Permission");
            return 0;
        }

        try {
            MutableText newName = net.minecraft.text.Text.literal(name);
            itemStack.set(DataComponentTypes.CUSTOM_NAME, newName);
            LangManager.send(ctx.getSource().getPlayer(), "Item-Name-Success");
            return 1;
        } catch (Exception e) {
            LangManager.send(ctx.getSource().getPlayer(), "Item-Name-Error");
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * Adds lore to the item in the player's hand.
     *
     * @param ctx  The command context.
     * @param lore The new lore for the item.
     * @return 1 if successful, 0 otherwise.
     * @throws CommandSyntaxException If the command execution fails.
     */
    private static int addItemLore(CommandContext<ServerCommandSource> ctx, String lore) throws CommandSyntaxException {
        return modifyLore(ctx, -1, lore);
    }

    /**
     * Edits the lore of the item in the player's hand.
     *
     * @param ctx  The command context.
     * @param line The line number to edit.
     * @param lore The new lore for the item.
     * @return 1 if successful, 0 otherwise.
     * @throws CommandSyntaxException If the command execution fails.
     */
    private static int editItemLore(CommandContext<ServerCommandSource> ctx, int line, String lore) throws CommandSyntaxException {
        return modifyLore(ctx, line, lore);
    }

    /**
     * Deletes lore from the item in the player's hand.
     *
     * @param ctx  The command context.
     * @param line The line number to delete.
     * @return 1 if successful, 0 otherwise.
     * @throws CommandSyntaxException If the command execution fails.
     */
    private static int deleteItemLore(CommandContext<ServerCommandSource> ctx, int line) throws CommandSyntaxException {
        return modifyLore(ctx, line, null);
    }

    /**
     * Modifies the lore of the item in the player's hand.
     *
     * @param ctx  The command context.
     * @param line The line number to modify (-1 for add).
     * @param lore The new lore for the item (null for delete).
     * @return 1 if successful, 0 otherwise.
     * @throws CommandSyntaxException If the command execution fails.
     */
    private static int modifyLore(CommandContext<ServerCommandSource> ctx, int line, String lore) throws CommandSyntaxException {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (player == null) {
            LangManager.send(ctx.getSource().getPlayer(), "Invalid-Player-Only");
            return 0;
        }

        ItemStack itemStack = player.getStackInHand(Hand.MAIN_HAND);
        if (itemStack.isEmpty()) {
            LangManager.send(ctx.getSource().getPlayer(), "Invalid-Item-In-Hand");
            return 0;
        }

        if (!Permissions.check(player, ITEM_EDITOR_LORE_PERMISSION_NODE, false)) {
            LangManager.send(ctx.getSource().getPlayer(), "Invalid-No-Permission");
            return 0;
        }

        try {
            // Get the current lore component or create a new one
            net.minecraft.component.type.LoreComponent loreComponent = itemStack.get(DataComponentTypes.LORE);
            java.util.List<net.minecraft.text.Text> loreList;
            
            if (loreComponent != null) {
                loreList = new java.util.ArrayList<>(loreComponent.lines());
            } else {
                loreList = new java.util.ArrayList<>();
            }

            if (lore == null) {
                // Delete operation
                if (line > 0 && line <= loreList.size()) {
                    loreList.remove(line - 1);
                } else {
                    LangManager.send(ctx.getSource().getPlayer(), "Item-Lore-Error");
                    return 0;
                }
            } else {
                // Add or edit operation
                net.minecraft.text.Text loreText = net.minecraft.text.Text.literal(lore);
                
                if (line == -1) {
                    // Add new lore line
                    loreList.add(loreText);
                } else if (line > 0 && line <= loreList.size()) {
                    // Edit existing lore line
                    loreList.set(line - 1, loreText);
                } else {
                    LangManager.send(ctx.getSource().getPlayer(), "Item-Lore-Error");
                    return 0;
                }
            }
            
            // Create new lore component and set it on the item
            net.minecraft.component.type.LoreComponent newLoreComponent = new net.minecraft.component.type.LoreComponent(loreList);
            itemStack.set(DataComponentTypes.LORE, newLoreComponent);
            
            LangManager.send(ctx.getSource().getPlayer(), "Item-Lore-Updated");
            return 1;
        } catch (Exception e) {
            LangManager.send(ctx.getSource().getPlayer(), "Item-Lore-Error");
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * Resets the item in the player's hand to its original state.
     *
     * @param ctx The command context.
     * @return 1 if successful, 0 otherwise.
     * @throws CommandSyntaxException If the command execution fails.
     */
    private static int resetItem(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (player == null) {
            LangManager.send(player, "Invalid-Player-Only");
            return 0;
        }

        ItemStack itemStack = player.getStackInHand(Hand.MAIN_HAND);
        if (itemStack.isEmpty()) {
            LangManager.send(player, "Invalid-Player-Only");
            return 0;
        }

        if (!Permissions.check(player, ITEM_EDITOR_NAME_PERMISSION_NODE, false) && !Permissions.check(player, ITEM_EDITOR_LORE_PERMISSION_NODE, false)) {
            LangManager.send(player, "Invalid-No-Permission");
            return 0;
        }

        try {
            // Remove custom name
            itemStack.remove(DataComponentTypes.CUSTOM_NAME);
            
            // Remove lore
            itemStack.remove(DataComponentTypes.LORE);

            LangManager.send(player, "Item-Reset-Success");
            return 1;
        } catch (Exception e) {
            LangManager.send(player, "Item-Reset-Error");
            e.printStackTrace();
            return 0;
        }
    }
}
