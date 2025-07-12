package EEssentials.commands.other;

import EEssentials.lang.LangManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.command.CommandSource;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class EnchantCommand {

    public static final String ENCHANT_RESTRICTED_PERMISSION_NODE = "eessentials.enchant.restricted";
    public static final String ENCHANT_UNRESTRICTED_PERMISSION_NODE = "eessentials.enchant.unrestricted";

    private static final SuggestionProvider<ServerCommandSource> ENCHANTMENT_SUGGESTIONS = (context, builder) -> {

        ServerPlayerEntity player = context.getSource().getPlayer();
        boolean unrestricted = Permissions.check(player, ENCHANT_UNRESTRICTED_PERMISSION_NODE, 2);

        Registry<Enchantment> enchantmentRegistry = context.getSource().getWorld().getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT);

        return CommandSource.suggestMatching(
                enchantmentRegistry.stream()
                        .filter(enchantment -> unrestricted || enchantment.isAcceptableItem(player.getStackInHand(Hand.MAIN_HAND)))
                        .map(enchantment -> {
                            Optional<RegistryKey<Enchantment>> keyOptional = enchantmentRegistry.getKey(enchantment);
                            return keyOptional.map(key -> key.getValue().toString().replace("minecraft:", "")).orElse(null);
                        })
                        .filter(id -> id != null)
                        .collect(Collectors.toList()),
                builder);
    };

    private static final SuggestionProvider<ServerCommandSource> UNENCHANT_SUGGESTIONS = (context, builder) -> {
        ServerPlayerEntity player = context.getSource().getPlayer();
        ItemStack itemStack = player.getStackInHand(Hand.MAIN_HAND);

        if (itemStack.isEmpty()) {
            return builder.buildFuture();
        }

        // Get the enchantments component from the item stack
        ItemEnchantmentsComponent enchantmentsComponent = EnchantmentHelper.getEnchantments(itemStack);

        // Get the enchantment registry using the registry manager
        Registry<Enchantment> enchantmentRegistry = context.getSource().getWorld().getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT);

        return CommandSource.suggestMatching(
                enchantmentsComponent.getEnchantmentEntries().stream()
                        .map(entry -> {
                            Optional<RegistryKey<Enchantment>> keyOptional = enchantmentRegistry.getKey(entry.getKey().value());
                            return keyOptional.map(key -> key.getValue().toString().replace("minecraft:", "")).orElse(null);
                        })
                        .filter(id -> id != null) // Filter out any null values
                        .collect(Collectors.toList()),
                builder);
    };

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        // Unregister the vanilla /enchant command
        unregisterVanillaEnchantCommand(dispatcher);

        dispatcher.register(CommandManager.literal("enchant")
                .requires(src -> Permissions.check(src, ENCHANT_RESTRICTED_PERMISSION_NODE, 2) ||
                        Permissions.check(src, ENCHANT_UNRESTRICTED_PERMISSION_NODE, 2))
                .then(CommandManager.argument("enchantment", StringArgumentType.string())
                        .suggests(ENCHANTMENT_SUGGESTIONS)
                        .then(CommandManager.argument("level", IntegerArgumentType.integer(1))
                                .executes(ctx -> applyEnchantment(ctx, StringArgumentType.getString(ctx, "enchantment"), IntegerArgumentType.getInteger(ctx, "level"))))));

        dispatcher.register(CommandManager.literal("unenchant")
                .requires(src -> Permissions.check(src, ENCHANT_RESTRICTED_PERMISSION_NODE, 2) ||
                        Permissions.check(src, ENCHANT_UNRESTRICTED_PERMISSION_NODE, 2))
                .then(CommandManager.argument("enchantment", StringArgumentType.string())
                        .suggests(UNENCHANT_SUGGESTIONS)
                        .executes(ctx -> removeEnchantment(ctx, StringArgumentType.getString(ctx, "enchantment")))));
    }

    private static void unregisterVanillaEnchantCommand(CommandDispatcher<ServerCommandSource> dispatcher) {
        try {
            dispatcher.getRoot().getChildren().removeIf(literalCommandNode -> literalCommandNode.getName().equals("enchant"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static int applyEnchantment(CommandContext<ServerCommandSource> ctx, String enchantmentName, int level) {

        ServerPlayerEntity player = ctx.getSource().getPlayer();
        ItemStack itemStack = player.getStackInHand(Hand.MAIN_HAND);

        // Make sure player has item in hand
        if (itemStack.isEmpty()) {
            LangManager.send(player, "Invalid-Item-In-Hand");
            return 0;
        }

        // Get the enchantment registry using the registry manager and RegistryKey
        Registry<Enchantment> enchantmentRegistry = ctx.getSource().getWorld().getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT);
        Identifier enchantmentId = Identifier.of("minecraft", enchantmentName);
        Enchantment enchantment = enchantmentRegistry.get(enchantmentId);

        if (enchantment == null) {
            LangManager.send(player, "Invalid-Enchantment");
            return 0;
        }

        // Convert the Enchantment to a RegistryEntry<Enchantment>
        RegistryEntry<Enchantment> enchantmentEntry = enchantmentRegistry.getEntry(enchantmentRegistry.getKey(enchantment).orElseThrow().getValue()).orElseThrow();

        // Permission check
        boolean hasUnrestrictedPermission = Permissions.check(player, ENCHANT_UNRESTRICTED_PERMISSION_NODE, 2);

        if (!hasUnrestrictedPermission && (!enchantment.isAcceptableItem(itemStack) || level > enchantment.getMaxLevel())) {
            LangManager.send(player, "Invalid-Enchantment-Or-Level");
            return 0;
        }

        if (hasUnrestrictedPermission && level > 255) {
            level = 255;
        }

        try {
            // Get the current enchantments component
            ItemEnchantmentsComponent enchantmentsComponent = (ItemEnchantmentsComponent) itemStack.getOrDefault(
                    DataComponentTypes.ENCHANTMENTS, ItemEnchantmentsComponent.DEFAULT);

            // Create a builder for the enchantments component
            ItemEnchantmentsComponent.Builder builder = new ItemEnchantmentsComponent.Builder(enchantmentsComponent);

            // Set or update the enchantment level
            builder.set(enchantmentEntry, level);

            // Apply the updated enchantments back to the item stack
            itemStack.set(DataComponentTypes.ENCHANTMENTS, builder.build());

            Map<String, String> replacements = new HashMap<>();
            replacements.put("{enchantment}", enchantmentName);
            replacements.put("{level}", String.valueOf(level));

            LangManager.send(player, "Enchantment-Success", replacements);
            return 1;
        } catch (Exception e) {
            LangManager.send(player, "Enchantment-Error");
            e.printStackTrace();
            return 0;
        }
    }

    private static int removeEnchantment(CommandContext<ServerCommandSource> ctx, String enchantmentName) {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        ItemStack itemStack = player.getStackInHand(Hand.MAIN_HAND);

        if (itemStack.isEmpty()) {
            LangManager.send(player, "Invalid-Item-In-Hand");
            return 0;
        }

        // Get the enchantment registry using the registry manager and RegistryKey
        Registry<Enchantment> enchantmentRegistry = ctx.getSource().getWorld().getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT);
        Identifier enchantmentId = Identifier.of("minecraft", enchantmentName);
        Enchantment enchantment = enchantmentRegistry.get(enchantmentId);

        if (enchantment == null) {
            LangManager.send(player, "Invalid-Enchantment");
            return 0;
        }

        // Convert the Enchantment to a RegistryEntry<Enchantment>
        RegistryEntry<Enchantment> enchantmentEntry = enchantmentRegistry.getEntry(enchantmentRegistry.getKey(enchantment).orElseThrow().getValue()).orElseThrow();

        try {
            // Get the current enchantments component
            ItemEnchantmentsComponent enchantmentsComponent = (ItemEnchantmentsComponent) itemStack.getOrDefault(
                    DataComponentTypes.ENCHANTMENTS, ItemEnchantmentsComponent.DEFAULT);

            // Create a builder for the enchantments component
            ItemEnchantmentsComponent.Builder builder = new ItemEnchantmentsComponent.Builder(enchantmentsComponent);

            // Check if the enchantment exists and remove it
            if (builder.getLevel(enchantmentEntry) > 0) {
                builder.set(enchantmentEntry, 0);  // Setting the level to 0 removes the enchantment

                // Apply the updated enchantments back to the item stack
                itemStack.set(DataComponentTypes.ENCHANTMENTS, builder.build());

                Map<String, String> replacements = new HashMap<>();
                replacements.put("{enchantment}", enchantmentName);

                LangManager.send(player, "Unenchantment-Success", replacements);
                return 1;
            } else {
                Map<String, String> replacements = new HashMap<>();
                replacements.put("{enchantment}", enchantmentName);

                LangManager.send(player, "Enchantment-Not-Present", replacements);
                return 0;
            }
        } catch (Exception e) {
            LangManager.send(player, "Enchantment-Error");
            e.printStackTrace();
            return 0;
        }
    }
}
