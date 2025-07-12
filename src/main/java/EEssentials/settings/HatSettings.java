package EEssentials.settings;

import EEssentials.config.Configuration;
import EEssentials.EEssentials;
import net.minecraft.component.ComponentMap;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;

import java.util.List;

public abstract class HatSettings {
    private static List<String> blacklistedItems;

    public static boolean isBlacklisted(ItemStack item) {
        String itemID = Registries.ITEM.getId(item.getItem()).toString();
        ComponentMap itemNbt = item.getComponents();
        if(itemNbt != null) {
            int customModelData = (int) item.getOrDefault(DataComponentTypes.CUSTOM_MODEL_DATA, 0);
            EEssentials.LOGGER.info(customModelData);
            return blacklistedItems.contains(itemID) || blacklistedItems.contains(itemID + ":" + customModelData);
        } else return blacklistedItems.contains(itemID);
    }

    public static void reload(Configuration hatConfig) {
        blacklistedItems = hatConfig.getStringList("Blacklisted-Items");
    }
}
