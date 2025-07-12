package EEssentials.commands.other;

import EEssentials.commands.AliasedCommand;
import EEssentials.lang.LangManager;
import EEssentials.settings.HatSettings;
import EEssentials.lang.ColorUtil;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.LiteralCommandNode;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.HashMap;
import java.util.Map;

import static net.minecraft.server.command.CommandManager.literal;

public class HatCommand {
    public static final String HAT_PERMISSION_NODE = "eessentials.hat";
    public static final String HAT_BLACKLIST_BYPASS_NODE = "eessentials.hat.bypass";

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        new AliasedCommand() {
            @Override
            public LiteralCommandNode<ServerCommandSource> register(CommandDispatcher<ServerCommandSource> dispatcher) {
                Map<String, String> replacements = new HashMap<>();
                return dispatcher.register(literal("hat")
                        .requires(source -> Permissions.check(source, HAT_PERMISSION_NODE, 2))
                        .executes(context -> {
                            ServerPlayerEntity player = context.getSource().getPlayer();
                            if(player != null) {
                                PlayerInventory playerInv = player.getInventory();
                                int selectedSlot = playerInv.getSelectedSlot();
                                ItemStack heldItem = playerInv.getStack(selectedSlot);
                                if(heldItem.isEmpty()) {
                                    LangManager.send(player, "Hat-Hand-Empty-Message");
                                } else if(
                                        Permissions.check(player, HAT_BLACKLIST_BYPASS_NODE, 2) ||
                                                !HatSettings.isBlacklisted(heldItem)){
                                    ItemStack headItem = playerInv.getStack(39);
                                    playerInv.setStack(39, heldItem);
                                    playerInv.setStack(selectedSlot, headItem);
                                    replacements.put("{item-hover}", ColorUtil.toMiniItemHover(heldItem));
                                    // Use the item's translation key for formatting
                                    replacements.put("{item-name-formatted}", "<lang:" + heldItem.getItem().getTranslationKey() + ">");
                                    replacements.put("{item}", heldItem.getName().getString());
                                    replacements.put("{item-type}", "<lang:" + heldItem.getItem().getTranslationKey() + ">");
                                    LangManager.send(player, "Hat-Equipped-Message", replacements);
                                } else {
                                    LangManager.send(player, "Hat-Blacklisted-Message");
                                }
                            } else {
                                LangManager.send(context.getSource().getPlayer(), "Invalid-Player-Only");
                            }
                            return Command.SINGLE_SUCCESS;
                        })
                );
            }

            @Override
            public String[] getCommandAliases() {
                return new String[]{"head"};
            }
        }.registerWithAliases(dispatcher);
    }
}
