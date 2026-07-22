package EEssentials.commands.other;

import EEssentials.lang.ColorUtil;
import EEssentials.lang.LangManager;
import EEssentials.util.MailboxManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.kyori.adventure.text.Component;

import java.util.HashMap;
import java.util.Map;

import static net.minecraft.server.command.CommandManager.argument;

/**
 * Provides commands to manage player mailboxes.
 */
public class MailCommands {

    private static final String MAIL_PERMISSION_NODE = "eessentials.mail";

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("mail")
                .requires(Permissions.require(MAIL_PERMISSION_NODE, 2))
                .then(CommandManager.literal("send")
                        .then(argument("target", EntityArgumentType.player())
                                .suggests((ctx, builder) -> CommandSource.suggestMatching(ctx.getSource().getServer().getPlayerNames(), builder))
                                .then(argument("message", StringArgumentType.greedyString())
                                        .executes(ctx -> sendMail(ctx, EntityArgumentType.getPlayer(ctx, "target"), StringArgumentType.getString(ctx, "message"))))))
                .then(CommandManager.literal("read")
                        .executes(MailCommands::readMail))
                .then(CommandManager.literal("clear")
                        .then(CommandManager.literal("all")
                                .executes(MailCommands::clearAllMail))
                        .then(argument("number", IntegerArgumentType.integer(1))
                                .executes(ctx -> clearSpecificMail(ctx, IntegerArgumentType.getInteger(ctx, "number"))))));
    }

    private static int sendMail(CommandContext<ServerCommandSource> ctx, ServerPlayerEntity target, String message) {
        ServerCommandSource source = ctx.getSource();
        ServerPlayerEntity sender = source.getPlayer();
        String senderName = (sender != null) ? sender.getName().getString() : "Console";

        // Use MailboxManager to send mail to the target player.
        MailboxManager mailboxManager = MailboxManager.getInstance();
        mailboxManager.sendMail(sender, target.getUuid(), message);

        // Send confirmation to sender
        Map<String, String> replacements = new HashMap<>();
        replacements.put("{receiver}", target.getName().getString());
        replacements.put("{message}", message);
        LangManager.send(source, "Mail-Sent", replacements);

        // Notify target if they are online
        if (target != null) {
            LangManager.send(target, "Mail-Receive-Notification", Map.of("sender", senderName));
        }

        // Social Spy
        if (sender != null) {
            sendToSocialSpies(ctx, sender, target, message);
        }

        return 1;
    }

    private static int readMail(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (player == null) {
            ctx.getSource().sendMessage(Component.text("Only players can use this command."));
            return 0;
        }

        // Fetch all messages from the player's mailbox
        MailboxManager mailboxManager = MailboxManager.getInstance();
        var messages = mailboxManager.getMail(player.getUuid());
        if (messages.isEmpty()) {
            LangManager.send(player, "Mail-No-Messages");
            return 1;
        }

        // Display each message to the player
        for (int i = 0; i < messages.size(); i++) {
            var message = messages.get(i);
            LangManager.send(player, "Mail-Read-Message", Map.of(
                    "{index}", String.valueOf(i + 1),
                    "{sender}", message.sender(),
                    "{message}", message.content()
            ));
        }

        return 1;
    }

    private static int clearAllMail(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (player == null) {
            ctx.getSource().sendMessage(Component.text("Only players can use this command."));
            return 0;
        }

        // Use MailboxManager to clear all mail from the player's mailbox
        MailboxManager mailboxManager = MailboxManager.getInstance();
        mailboxManager.clearAllMail(player.getUuid());
        LangManager.send(player, "Mail-Cleared-All");
        return 1;
    }

    private static int clearSpecificMail(CommandContext<ServerCommandSource> ctx, int index) {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (player == null) {
            ctx.getSource().sendMessage(Component.text("Only players can use this command."));
            return 0;
        }

        // Attempt to clear a specific message by index
        MailboxManager mailboxManager = MailboxManager.getInstance();
        boolean success = mailboxManager.clearMessage(player.getUuid(), index - 1);
        if (success) {
            LangManager.send(player, "Mail-Cleared-Specific", Map.of("{index}", String.valueOf(index)));
        } else {
            LangManager.send(player, "Mail-Clear-Failed", Map.of("{index}", String.valueOf(index)));
        }

        return success ? 1 : 0;
    }

    /**
     * Sends Social Spy messages to the Spy for `/mail send`.
     */
    private static void sendToSocialSpies(CommandContext<ServerCommandSource> ctx, ServerPlayerEntity sender, ServerPlayerEntity receiver, String message) {
        Map<String, String> replacements = new HashMap<>();
        replacements.put("{sender}", sender.getName().getString());
        replacements.put("{receiver}", receiver.getName().getString());
        replacements.put("{message}", message);

        String socialSpyMessage = LangManager.getLang("Prefix-Social-Spy");
        if (socialSpyMessage != null) {
            for (Map.Entry<String, String> entry : replacements.entrySet()) {
                socialSpyMessage = socialSpyMessage.replace(entry.getKey(), ColorUtil.escapeInput(entry.getValue()));
            }

            Component componentMessage = ColorUtil.parseColour(socialSpyMessage);
            for (ServerPlayerEntity spy : ctx.getSource().getServer().getPlayerManager().getPlayerList()) {
                if (spy.equals(sender) || spy.equals(receiver)) {
                    continue;
                }
                if (SocialSpyCommand.isSocialSpyEnabled(spy)) {
                    spy.sendMessage(componentMessage);
                }
            }
        }
    }
}
