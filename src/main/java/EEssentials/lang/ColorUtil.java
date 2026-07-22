package EEssentials.lang;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minecraft.component.ComponentMap;
import net.minecraft.item.ItemStack;

import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ColorUtil {
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]){6}");
    private static final Pattern LEGACY_PATTERN = Pattern.compile("[&§]([0-9a-fA-fk-oK-OrR])");

    /**
     * Escapes MiniMessage tags in untrusted input (e.g. player names, chat messages)
     * so they are rendered as plain text instead of being parsed as formatting,
     * hover text, or clickable commands.
     */
    public static String escapeInput(String input) {
        return MiniMessage.miniMessage().escapeTags(input);
    }

    public static Component parseColour(String input) {
        input = replaceCodes(input);
        return MiniMessage.miniMessage().deserialize(input);
    }

    private static String replaceCodes(String input) {
        Matcher matcher = HEX_PATTERN.matcher(input);
        while (matcher.find()) {
            input = input.replace(matcher.group(), "<reset><c:" + matcher.group().substring(1) + ">");
            matcher = HEX_PATTERN.matcher(input);
        }
        return replaceLegacyCodes(input);
    }

    private static String replaceLegacyCodes(String input) {
        Matcher matcher = LEGACY_PATTERN.matcher(input);
        while (matcher.find()) {
            input = input.replace(matcher.group(), getLegacyReplacement(matcher.group().substring(1)));
            matcher = LEGACY_PATTERN.matcher(input);
        }
        return input;
    }

    private static String getLegacyReplacement(String input) {
        return switch (input.toUpperCase(Locale.ENGLISH)) {
            case "0" -> "<reset><c:#000000>";
            case "1" -> "<reset><c:#0000AA>";
            case "2" -> "<reset><c:#00AA00>";
            case "3" -> "<reset><c:#00AAAA>";
            case "4" -> "<reset><c:#AA0000>";
            case "5" -> "<reset><c:#AA00AA>";
            case "6" -> "<reset><c:#FFAA00>";
            case "7" -> "<reset><c:#AAAAAA>";
            case "8" -> "<reset><c:#555555>";
            case "9" -> "<reset><c:#5555FF>";
            case "A" -> "<reset><c:#55FF55>";
            case "B" -> "<reset><c:#55FFFF>";
            case "C" -> "<reset><c:#FF5555>";
            case "D" -> "<reset><c:#FF55FF>";
            case "E" -> "<reset><c:#FFFF55>";
            case "F" -> "<reset><c:#FFFFFF>";
            case "K" -> "<obf>";
            case "L" -> "<b>";
            case "M" -> "<st>";
            case "N" -> "<u>";
            case "O" -> "<i>";
            case "R" -> "<reset>";
            default -> input;
        };
    }

    public static String componentToString(ComponentLike component) {
        Component styledComponent = component.asComponent();
        StringBuilder sb = new StringBuilder();
        for(Map.Entry<TextDecoration, TextDecoration.State> entry : styledComponent.decorations().entrySet()) {
            if(entry.getValue().equals(TextDecoration.State.TRUE)) {
                sb.append("<").append(entry.getKey().toString()).append(">");
            }
        }
        TextColor color = styledComponent.color();
        if(color != null) sb.append("<c:").append(color.asHexString()).append(">");
        if(component instanceof TextComponent) {
            sb.append(((TextComponent)component).content());
        }
        if(component instanceof TranslatableComponent) {
            sb.append("<lang:").append(((TranslatableComponent) component).key()).append(">");
        }
        sb.append("<reset>");
        return sb.toString();
    }

    public static String toMiniItemHover(ItemStack item) {
        ComponentMap itemNBT = item.getComponents();
        if(itemNBT != null) {
            return "<hover:show_item:"
                    + item.getItem().getName().getString()
                    .toLowerCase(Locale.ENGLISH)
                    .replace(" ", "_") +
                    ":" + item.getCount() + ":'" +
                    itemNBT + "'>";
        } else {
            return "<hover:show_item:"
                    + item.getItem().getName().getString()
                    .toLowerCase(Locale.ENGLISH)
                    .replace(" ", "_")
                    + ":" + item.getCount() + ">";
        }
    }
}
