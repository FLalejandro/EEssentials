package EEssentials.lang;

import EEssentials.config.Configuration;
import net.kyori.adventure.audience.Audience;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

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

    public static void send(Audience audience, String langKey) {
        send(audience, langKey, null);
    }

    public static void send(@NotNull Audience audience, @NotNull String langKey,
                            @Nullable Map<String, String> replacements) {
        send(audience, null, langKey, replacements);
    }

    public static void send(@NotNull Audience audience, @Nullable String prefixKey,
                                    @NotNull String langKey, @Nullable Map<String, String> replacements) {
        String lang = getLang(langKey);
        if(lang == null) return;
        if(replacements != null && !replacements.isEmpty()) {
            for(Map.Entry<String, String> entry : replacements.entrySet()) {
                lang = lang.replace(entry.getKey(), ColorUtil.escapeInput(entry.getValue()));
            }
        }
        String prefix = getLang(prefixKey);
        if(prefix != null) lang = prefix + lang;
        audience.sendMessage(ColorUtil.parseColour(lang));
    }
}
