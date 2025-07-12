package EEssentials.lang;

import EEssentials.config.Configuration;
import net.minecraft.server.network.ServerPlayerEntity;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public class LangManager {
    private static final Map<String, String> lang = new HashMap<>();

    public static void loadConfig(Configuration langConfig) {
        if(langConfig != null) {
            for (String key : langConfig.getKeys()) {
                lang.put(key, langConfig.getString(key));
            }
        }
    }

    public static @Nullable String getLang(String langKey) {
        return lang.get(langKey);
    }

    public static void send(ServerPlayerEntity player, String langKey) {
        if(player == null) return;
        send(player, langKey, null);
    }

    public static void send(ServerPlayerEntity player, @NotNull String langKey,
                            @Nullable Map<String, String> replacements) {
        if(player == null) return;
        send(player, null, langKey, replacements);
    }

    public static void send(ServerPlayerEntity player, @Nullable String prefixKey,
                                    @NotNull String langKey, @Nullable Map<String, String> replacements) {
        if(player == null) return;
        String lang = getLang(langKey);
        if(lang == null) return;
        if(replacements != null && !replacements.isEmpty()) {
            for(Map.Entry<String, String> entry : replacements.entrySet()) {
                lang = lang.replace(entry.getKey(), entry.getValue());
            }
        }
        String prefix = getLang(prefixKey);
        if(prefix != null) lang = prefix + lang;
        // Parse to Kyori Component, serialize to legacy string, then send as Text.literal
        net.kyori.adventure.text.Component component = ColorUtil.parseColour(lang);
        String legacy = LegacyComponentSerializer.legacySection().serialize(component);
        player.sendMessage(net.minecraft.text.Text.literal(legacy));
    }
}
