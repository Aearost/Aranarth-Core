package com.aearost.aranarthcore.commands.general;

import com.aearost.aranarthcore.network.NetworkManager;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.Lang;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Objects;

/**
 * Allows players to add nicknames for themselves.
 */
public class CommandNickname implements CommandExecutor {

	/**
	 * @param sender The user that entered the command.
	 * @param command The command itself.
	 * @param alias The alias of the command.
	 * @param args The arguments of the command.
	 * @return Confirmation of whether the command was a success or not.
	 */
	@Override
	public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {

		if (sender instanceof Player player) {
			if (!player.hasPermission("aranarth.nick")) {
				player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "general.no_permission")));
				return true;
			}

			if (args.length == 0) {
				updateNickname(player, "");
				player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "nickname.removed")));
				return true;

			}

			// The typed text, which excludes the gradient keyword and colors
			boolean isGradient = args[0].equalsIgnoreCase("gradient") || args[0].equalsIgnoreCase("gradientbold");
			int textStart = isGradient ? 2 : 0;
			String typedText = String.join(" ", Arrays.copyOfRange(args, Math.min(textStart, args.length), args.length));
			if (!ChatUtils.isSafeStoredText(typedText)) {
				player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "nickname.invalid_characters")));
				return true;
			}
			if (!isGradient) {
				if (typedText.contains("&") && !player.hasPermission("aranarth.nick.color")) {
					player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "general.no_permission")));
					return true;
				}
				if (typedText.contains("#") && !player.hasPermission("aranarth.nick.hex")) {
					player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "general.no_permission")));
					return true;
				}
			}
			// Nobody can take another player's name
			if (AranarthUtils.isNameTakenByOther(ChatUtils.stripColorFormatting(typedText), player.getUniqueId())) {
				player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "nickname.taken")));
				return true;
			}

			if (args.length == 1) {
				AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
				if (ChatUtils.stripColorFormatting(args[0]).length() > 20) {
					player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "nickname.too_long")));
					return true;
				} else if (ChatUtils.stripColorFormatting(args[0]).isEmpty()) {
					player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "nickname.must_enter")));
					return true;
				}

				updateNickname(player, args[0]);
				sender.sendMessage(ChatUtils.chatMessage(Lang.getFor(sender, "nickname.set", "name", args[0])));
				return true;
			} else {
				AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
				int stringStart = 0;
				if (args[0].equalsIgnoreCase("gradient") || args[0].equalsIgnoreCase("gradientbold")) {
					if (!player.hasPermission("aranarth.nick.gradient")) {
						player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "general.no_permission")));
						return true;
					}

					stringStart = 2;
				}

				// Gets the full item name
				StringBuilder nicknameSB = new StringBuilder();
				for (int i = stringStart; i < args.length; i++) {
					nicknameSB.append(args[i]);
					if (i == args.length - 1) {
						break;
					} else {
						nicknameSB.append(" ");
					}
				}

				String nickname = nicknameSB.toString();
				if (args[0].startsWith("gradient")) {
					if (args[0].equalsIgnoreCase("gradient")) {
						nickname = ChatUtils.translateToGradient(args[1], nickname, false);
					} else if (args[0].equalsIgnoreCase("gradientbold")) {
						nickname = ChatUtils.translateToGradient(args[1], nickname, true);
					}

					if (Objects.nonNull(nickname)) {
						if (nicknameSB.toString().length() > 20) {
							player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "nickname.too_long")));
							return true;
						} else if (nicknameSB.toString().isEmpty()) {
							player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "nickname.must_enter")));
							return true;
						}

						updateNickname(player, nickname);
						player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "nickname.set", "name", nickname)));
						return true;
					}
					player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "nickname.gradient_failed")));
					return true;
				}

				if (ChatUtils.stripColorFormatting(nickname).length() > 20) {
					player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "nickname.too_long")));
					return true;
				} else if (ChatUtils.stripColorFormatting(nickname).isEmpty()) {
					player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "nickname.must_enter")));
					return true;
				}

				updateNickname(player, nickname);
				sender.sendMessage(ChatUtils.chatMessage(Lang.getFor(sender, "nickname.set", "name", nickname)));
				return true;
			}
		} else {
			sender.sendMessage(ChatUtils.chatMessage(Lang.getFor(sender, "general.no_permission_console")));
			return true;
		}
	}

	/**
	 * Sets the player's nickname and propagates it to the other server.
	 *
	 * @param player The player.
	 * @param nickname The new nickname, or an empty string to remove it.
	 */
	private void updateNickname(Player player, String nickname) {
		AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
		aranarthPlayer.setNickname(nickname);
		AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
		if (NetworkManager.isActive()) {
			// An empty nickname is sent as-is so the other server also falls back to the username
			String storedNickname = nickname.isEmpty() ? "" : aranarthPlayer.getNickname();
			NetworkManager.getInstance().publishNicknameUpdate(player.getUniqueId(), player.getName(), storedNickname);
		}
	}
}
