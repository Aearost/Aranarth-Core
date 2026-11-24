package com.aearost.aranarthcore.commands.council;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.network.NetworkManager;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.Lang;
import com.aearost.aranarthcore.utils.SchematicUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Base64;
import java.util.UUID;

/**
 * AC subcommand for copying a WorldEdit schematic to the other server.
 */
public class CommandSchemTransfer {

    private static final String USAGE = "ac schemtransfer <name> [override]";

    public static boolean onCommand(CommandSender sender, String[] args) {
        if (sender instanceof Player player && !player.hasPermission("aranarth.schemtransfer")) {
            player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "general.no_permission")));
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(ChatUtils.chatMessage(Lang.getFor(sender, "general.invalid_syntax", "usage", USAGE)));
            return true;
        }

        // Schematic names can contain spaces, so everything after the sub-command is the name, unless it ends with override
        boolean override = args.length > 2 && args[args.length - 1].equalsIgnoreCase("override");
        String name = String.join(" ", Arrays.copyOfRange(args, 1, override ? args.length - 1 : args.length));
        String otherServer = AranarthCore.isSmpServer() ? "Survival" : "SMP";

        Path folder = SchematicUtils.getSchematicsFolder();
        if (folder == null || !NetworkManager.isActive()) {
            sender.sendMessage(ChatUtils.chatMessage(Lang.getFor(sender, "ac.schemtransfer_unavailable")));
            return true;
        }
        if (!NetworkManager.getInstance().isOtherServerOnline()) {
            sender.sendMessage(ChatUtils.chatMessage(Lang.getFor(sender, "ac.schemtransfer_offline", "server", otherServer)));
            return true;
        }
        Path file = SchematicUtils.findSchematic(folder, name);
        if (file == null) {
            sender.sendMessage(ChatUtils.chatMessage(Lang.getFor(sender, "ac.schemtransfer_not_found", "name", name)));
            return true;
        }
        String fileName = folder.relativize(file).toString().replace('\\', '/');
        String displayName = SchematicUtils.stripExtension(fileName);

        UUID senderUuid = sender instanceof Player player ? player.getUniqueId() : null;
        sender.sendMessage(ChatUtils.chatMessage(Lang.getFor(sender, "ac.schemtransfer_sending", "name", displayName, "server", otherServer)));
        Bukkit.getScheduler().runTaskAsynchronously(AranarthCore.getInstance(), () -> {
            String failureKey = null;
            try {
                if (Files.size(file) > SchematicUtils.MAX_TRANSFER_BYTES) {
                    failureKey = "ac.schemtransfer_too_large";
                } else {
                    String data = Base64.getEncoder().encodeToString(Files.readAllBytes(file));
                    if (!NetworkManager.getInstance().publishSchematicTransfer(senderUuid, fileName, data, override)) {
                        failureKey = "ac.schemtransfer_failed";
                    }
                }
            } catch (IOException e) {
                Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "[SchemTransfer] Could not read " + fileName + ": " + e.getMessage());
                failureKey = "ac.schemtransfer_failed";
            }
            if (failureKey != null) {
                String key = failureKey;
                Bukkit.getScheduler().runTask(AranarthCore.getInstance(), () -> {
                    CommandSender recipient = senderUuid == null ? Bukkit.getConsoleSender() : Bukkit.getPlayer(senderUuid);
                    if (recipient != null) {
                        recipient.sendMessage(ChatUtils.chatMessage(Lang.getFor(recipient, key, "name", displayName,
                                "server", otherServer, "max", String.valueOf(SchematicUtils.MAX_TRANSFER_BYTES / (1024 * 1024)))));
                    }
                });
            }
        });
        return true;
    }
}
