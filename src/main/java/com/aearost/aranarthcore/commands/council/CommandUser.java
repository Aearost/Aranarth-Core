package com.aearost.aranarthcore.commands.council;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.enums.PunishmentCategory;
import com.aearost.aranarthcore.gui.GuiUser;
import com.aearost.aranarthcore.network.NetworkManager;
import com.aearost.aranarthcore.network.NetworkPlayer;
import com.aearost.aranarthcore.network.PendingTeleport;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.Punishment;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.CreativeSwapUtils;
import com.aearost.aranarthcore.utils.Lang;
import com.aearost.aranarthcore.utils.PunishmentUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Brings every Council tool for managing a player into one place.
 */
public class CommandUser {

	private static final int HISTORY_PAGE_SIZE = 8;
	private static final int MAX_REASON_LENGTH = 150;

	/**
	 * What a Council member's next chat message is used as.
	 */
	private enum Input {
		ISSUE_REASON, UNMUTE_REASON, UNBAN_REASON
	}

	/**
	 * A punishment, mute lift or ban lift being prepared by a Council member.
	 */
	private static final class Draft {
		private final UUID targetUuid;
		private final String username;
		private PunishmentCategory category;
		private int step;
		private String reason;
		private String liftReason;

		private Draft(UUID targetUuid, String username) {
			this.targetUuid = targetUuid;
			this.username = username;
		}
	}

	// Read from the async chat thread, so these must be thread-safe
	private static final Map<UUID, Draft> drafts = new ConcurrentHashMap<>();
	private static final Map<UUID, Input> awaitingInput = new ConcurrentHashMap<>();

	/**
	 * @param sender The user that entered the command.
	 * @param args The arguments of the command.
	 * @return Confirmation of whether the command was a success or not.
	 */
	public static boolean onCommand(CommandSender sender, String[] args) {
		if (!(sender instanceof Player player)) {
			sender.sendMessage(ChatUtils.chatMessage(Lang.getFor(sender, "general.no_permission_console")));
			return true;
		}
		if (!player.hasPermission("aranarth.council.user")) {
			player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "general.no_permission")));
			return true;
		}
		if (args.length < 2) {
			player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "general.invalid_syntax", "usage", "ac user <player>")));
			return true;
		}

		UUID targetUuid = AranarthUtils.getUUIDFromUsername(args[1]);
		if (targetUuid == null) {
			player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "player.not_found", "name", args[1])));
			return true;
		}
		String username = getUsername(targetUuid, args[1]);

		if (args.length == 2) {
			new GuiUser(player, targetUuid, username).openGui();
			return true;
		}

		String option = args.length > 3 ? args[3] : "";
		switch (args[2].toLowerCase()) {
			case "location" -> showLocation(player, username);
			case "spectate" -> spectate(player, targetUuid, username);
			case "inventory" -> CommandInvsee.onCommand(player, new String[]{"invsee", username});
			case "enderchest" -> openEnderChest(player, targetUuid, username);
			case "history" -> showHistory(player, targetUuid, username, parsePage(option));
			case "delete" -> deletePunishment(player, targetUuid, username, option, args.length > 4 && args[4].equalsIgnoreCase("confirm"));
			case "issue" -> startIssue(player, targetUuid, username, option);
			case "category" -> cycleCategory(player, targetUuid, username, option.equalsIgnoreCase("prev") ? -1 : 1);
			case "step" -> cycleStep(player, targetUuid, username, option.equalsIgnoreCase("prev") ? -1 : 1);
			case "panel" -> cycleStep(player, targetUuid, username, 0);
			case "reason" -> promptReason(player, targetUuid, username, Input.ISSUE_REASON);
			case "submit" -> submit(player, targetUuid, username, option.equalsIgnoreCase("confirm"));
			case "unmute" -> lift(player, targetUuid, username, Input.UNMUTE_REASON, option.equalsIgnoreCase("confirm"));
			case "unban" -> lift(player, targetUuid, username, Input.UNBAN_REASON, option.equalsIgnoreCase("confirm"));
			case "cancel" -> {
				drafts.remove(player.getUniqueId());
				awaitingInput.remove(player.getUniqueId());
				player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "general.cancelled")));
			}
			default -> player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "general.invalid_syntax", "usage", "ac user <player>")));
		}
		return true;
	}

	// -------------------------------------------------------------------------
	// Location, inventory and ender chest
	// -------------------------------------------------------------------------

	private static void showLocation(Player player, String username) {
		CommandWhereIs.onCommand(player, new String[]{"whereis", username});
		if (player.hasPermission("aranarth.whereis") && player.hasPermission("aranarth.admintp")) {
			Component buttons = Component.text("  ").append(button(player, "user.button_teleport", "user.hover_teleport",
					"/ac tp " + username));
			if (canSpectate(player)) {
				buttons = buttons.append(Component.text("   "))
						.append(button(player, "user.button_spectate", "user.hover_spectate", "/ac user " + username + " spectate"));
			}
			player.sendMessage(buttons);
		}
	}

	private static boolean canSpectate(Player player) {
		return player.hasPermission("aranarth.whereis") && player.hasPermission("aranarth.admintp")
				&& player.hasPermission("aranarth.vanish") && AranarthUtils.hasUnrestrictedGameMode(player);
	}

	/**
	 * Vanishes and teleports to the player, then enters Creative mode, which swaps in the creative inventory.
	 */
	private static void spectate(Player player, UUID targetUuid, String username) {
		if (!canSpectate(player)) {
			player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "general.no_permission")));
			return;
		}
		if (player.getUniqueId().equals(targetUuid)) {
			player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "user.spectate_self")));
			return;
		}

		Player target = Bukkit.getPlayer(targetUuid);
		if (target == null) {
			NetworkPlayer remote = NetworkManager.isActive() ? NetworkManager.getInstance().getRemotePlayer(targetUuid) : null;
			if (remote == null) {
				player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "user.not_online", "player", username)));
				return;
			}
			// Vanished before leaving so they are never seen arriving on the other server
			vanish(player);
			String targetServer = AranarthCore.getInstance().getConfig()
					.getString("network.servers." + remote.getServer(), remote.getServer());
			PendingTeleport pending = PendingTeleport.forCommand("ac user " + username + " spectate",
					"&e&l" + remote.getServer().toUpperCase(), Lang.getFor(player, "teleport.teleported_to", "player", remote.getNickname()));
			player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "admin.teleporting_to_player", "player", remote.getNickname())));
			NetworkManager.getInstance().saveInventoryAndTransfer(player, targetServer, pending);
			return;
		}

		vanish(player);
		String nickname = AranarthUtils.getNickname(target);
		// The teleport can briefly return the player to their survival inventory, which they do not need to be told about
		CreativeSwapUtils.runQuietly(player.getUniqueId(), () -> AranarthUtils.teleportPlayer(player, player.getLocation(),
				target.getLocation(), true, nickname, Lang.getFor(player, "admin.teleport_to_player_success", "player", nickname), success -> {
			if (!success) {
				player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "admin.teleport_to_player_failed", "player", nickname)));
				return;
			}
			// Entered after the teleport, which can return the player to Survival mode.
			// The arena has its own inventory, so only Spectator mode is used there
			boolean isArena = AranarthUtils.getInventoryGroup(player.getWorld().getName()).equals("arena");
			player.setGameMode(isArena ? GameMode.SPECTATOR : GameMode.CREATIVE);
			if (player.getGameMode() == GameMode.CREATIVE || isArena) {
				player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "user.spectating", "player", nickname)));
			}
		}));
	}

	private static void vanish(Player player) {
		AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
		if (aranarthPlayer == null) {
			return;
		}
		if (!aranarthPlayer.isVanished()) {
			CommandVanish.onCommand(player, new String[]{"vanish"});
		} else {
			// Already vanished, possibly from the other server, so being invisible is applied on this one too
			player.setInvisible(true);
			AranarthUtils.updateTab();
		}
	}

	private static void openEnderChest(Player player, UUID targetUuid, String username) {
		if (!player.hasPermission("aranarth.invsee")) {
			player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "general.no_permission")));
			return;
		}
		// Creative mode items must never reach an ender chest
		if (player.getGameMode() == GameMode.CREATIVE) {
			player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "access.cannot_ender_creative")));
			return;
		}
		Player target = Bukkit.getPlayer(targetUuid);
		if (target != null) {
			// The real ender chest is opened, so items from another world's inventory could be put into it
			if (!AranarthUtils.isSurvivalWorld(player.getWorld().getName())) {
				player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "user.ender_blocked")));
				return;
			}
			player.openInventory(target.getEnderChest());
			return;
		}
		if (NetworkManager.isActive()) {
			NetworkPlayer remote = NetworkManager.getInstance().getRemotePlayer(targetUuid);
			if (remote != null) {
				NetworkManager.getInstance().publishInvseeRequest(player.getUniqueId(), targetUuid, NetworkManager.CONTAINER_ENDER_CHEST);
				player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "user.ender_loading")));
				return;
			}
		}
		player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "user.not_online", "player", username)));
	}

	// -------------------------------------------------------------------------
	// Punishment history
	// -------------------------------------------------------------------------

	private static void showHistory(Player player, UUID targetUuid, String username, int page) {
		if (!player.hasPermission("aranarth.punishments")) {
			player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "general.no_permission")));
			return;
		}
		String nickname = AranarthUtils.getNickname(Bukkit.getOfflinePlayer(targetUuid));
		List<Punishment> punishments = AranarthUtils.getPunishments(targetUuid);
		if (punishments == null || punishments.isEmpty()) {
			player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "punishment.none", "player", nickname)));
			player.sendMessage(Component.text("  ").append(button(player, "user.button_back", "guide.click_to_navigate", "/ac user " + username)));
			return;
		}

		int pages = (punishments.size() + HISTORY_PAGE_SIZE - 1) / HISTORY_PAGE_SIZE;
		page = Math.max(1, Math.min(page, pages));
		player.sendMessage(Component.empty());
		player.sendMessage(ChatUtils.translateToColor(Lang.getFor(player, "punishment.list_header", "player", nickname)));
		// Newest first, but numbered by their place in the whole history like /ac punishments
		int newest = punishments.size() - 1 - (page - 1) * HISTORY_PAGE_SIZE;
		for (int i = newest; i > newest - HISTORY_PAGE_SIZE && i >= 0; i--) {
			Punishment punishment = punishments.get(i);
			String line = Lang.getFor(player, "punishment.list_entry", "number", i + 1, "type", getTypeName(player, punishment.getType()),
					"by", getAppliedByName(player, punishment), "date", formatDate(punishment), "time", formatTime(punishment),
					"reason", punishment.getReason());
			PunishmentCategory category = PunishmentCategory.of(punishment);
			if (category != null) {
				line += " " + Lang.getFor(player, "user.history_category", "category", Lang.getFor(player, "user.category_" + category.getKey()));
			}
			Component entry = legacy(line);
			if (player.hasPermission("aranarth.punishments")) {
				entry = entry.append(Component.text(" ")).append(button(player, "user.button_delete", "user.hover_delete",
						"/ac user " + username + " delete " + punishment.getDateMillis()));
			}
			player.sendMessage(entry);
		}

		String back = "/ac user " + username;
		player.sendMessage(Component.text("  ")
				.append(button(player, "guide.nav_prev", "guide.click_to_navigate", back + " history " + (page == 1 ? pages : page - 1)))
				.append(Component.text(" "))
				.append(legacy(Lang.getFor(player, "user.history_page", "page", page, "pages", pages)))
				.append(Component.text(" "))
				.append(button(player, "guide.nav_next", "guide.click_to_navigate", back + " history " + (page == pages ? 1 : page + 1)))
				.append(Component.text("   "))
				.append(button(player, "user.button_back", "guide.click_to_navigate", back)));
	}

	private static void deletePunishment(Player player, UUID targetUuid, String username, String dateMillis, boolean confirmed) {
		if (!player.hasPermission("aranarth.punishments")) {
			player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "general.no_permission")));
			return;
		}
		if (!PunishmentUtils.outranks(player, targetUuid)) {
			player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "user.cannot_manage")));
			return;
		}
		List<Punishment> punishments = AranarthUtils.getPunishments(targetUuid);
		int index = -1;
		if (punishments != null) {
			for (int i = 0; i < punishments.size(); i++) {
				if (String.valueOf(punishments.get(i).getDateMillis()).equals(dateMillis)) {
					index = i;
					break;
				}
			}
		}
		// Deleted already, possibly by another Council member
		if (index == -1) {
			player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "punishment.invalid_number")));
			return;
		}

		Punishment punishment = punishments.get(index);
		String nickname = AranarthUtils.getNickname(Bukkit.getOfflinePlayer(targetUuid));
		if (!confirmed) {
			player.sendMessage(Component.empty());
			player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "user.confirm_delete", "number", index + 1,
					"type", getTypeName(player, punishment.getType()), "player", nickname, "reason", punishment.getReason())));
			player.sendMessage(Component.text("  ")
					.append(button(player, "user.button_confirm", "user.hover_confirm_delete",
							"/ac user " + username + " delete " + dateMillis + " confirm"))
					.append(Component.text("   "))
					.append(button(player, "user.button_back", "guide.click_to_navigate", "/ac user " + username + " history")));
			return;
		}

		PunishmentUtils.removed(punishment, player.getUniqueId());
		player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "punishment.removed", "number", String.valueOf(index + 1), "player", nickname)));
		showHistory(player, targetUuid, username, 1);
	}

	// -------------------------------------------------------------------------
	// Issuing punishments
	// -------------------------------------------------------------------------

	private static boolean canIssue(Player player) {
		return player.hasPermission("aranarth.warn") || player.hasPermission("aranarth.mute") || player.hasPermission("aranarth.ban");
	}

	/**
	 * Shows the offence categories to choose from, or starts a punishment of the chosen category at the
	 * player's next offence.
	 */
	private static void startIssue(Player player, UUID targetUuid, String username, String categoryKey) {
		if (!canIssue(player)) {
			player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "general.no_permission")));
			return;
		}
		if (!PunishmentUtils.outranks(player, targetUuid)) {
			player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "user.cannot_manage")));
			return;
		}
		PunishmentCategory category = PunishmentCategory.fromKey(categoryKey);
		List<Punishment> punishments = AranarthUtils.getPunishments(targetUuid);
		if (category == null) {
			String nickname = AranarthUtils.getNickname(Bukkit.getOfflinePlayer(targetUuid));
			player.sendMessage(Component.empty());
			player.sendMessage(ChatUtils.translateToColor(Lang.getFor(player, "guide.divider")));
			player.sendMessage(ChatUtils.translateToColor("  " + Lang.getFor(player, "user.picker_title", "player", nickname)));
			player.sendMessage(Component.empty());
			for (PunishmentCategory option : PunishmentCategory.values()) {
				PunishmentCategory.Step next = option.getSteps().get(option.getNextStepIndex(punishments));
				String hover = Lang.getFor(player, "user.category_" + option.getKey() + "_desc") + "\n"
						+ Lang.getFor(player, "user.picker_hover", "count", option.countOffences(punishments),
						"step", getStepName(player, next));
				player.sendMessage(Component.text("    ").append(buttonText(
						"&8[&e" + Lang.getFor(player, "user.category_" + option.getKey()) + "&8]", hover,
						"/ac user " + username + " issue " + option.getKey())));
			}
			player.sendMessage(Component.empty());
			player.sendMessage(Component.text("  ").append(button(player, "user.button_back", "guide.click_to_navigate", "/ac user " + username)));
			player.sendMessage(ChatUtils.translateToColor(Lang.getFor(player, "guide.divider")));
			return;
		}

		Draft draft = getDraft(player, targetUuid, username);
		draft.category = category;
		draft.step = category.getNextStepIndex(punishments);
		showIssuePanel(player, draft);
	}

	private static void cycleCategory(Player player, UUID targetUuid, String username, int direction) {
		Draft draft = drafts.get(player.getUniqueId());
		if (draft == null || !draft.targetUuid.equals(targetUuid) || draft.category == null) {
			startIssue(player, targetUuid, username, "");
			return;
		}
		PunishmentCategory[] categories = PunishmentCategory.values();
		draft.category = categories[Math.floorMod(draft.category.ordinal() + direction, categories.length)];
		draft.step = draft.category.getNextStepIndex(AranarthUtils.getPunishments(targetUuid));
		showIssuePanel(player, draft);
	}

	private static void cycleStep(Player player, UUID targetUuid, String username, int direction) {
		Draft draft = drafts.get(player.getUniqueId());
		if (draft == null || !draft.targetUuid.equals(targetUuid) || draft.category == null) {
			startIssue(player, targetUuid, username, "");
			return;
		}
		draft.step = Math.floorMod(draft.step + direction, draft.category.getSteps().size());
		showIssuePanel(player, draft);
	}

	private static void showIssuePanel(Player player, Draft draft) {
		if (!canIssue(player)) {
			player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "general.no_permission")));
			return;
		}
		String nickname = AranarthUtils.getNickname(Bukkit.getOfflinePlayer(draft.targetUuid));
		List<Punishment> punishments = AranarthUtils.getPunishments(draft.targetUuid);
		PunishmentCategory.Step step = draft.category.getSteps().get(draft.step);
		int nextStep = draft.category.getNextStepIndex(punishments);
		String base = "/ac user " + draft.username + " ";
		String categoryName = Lang.getFor(player, "user.category_" + draft.category.getKey());

		player.sendMessage(Component.empty());
		player.sendMessage(ChatUtils.translateToColor(Lang.getFor(player, "guide.divider")));
		player.sendMessage(ChatUtils.translateToColor("  " + Lang.getFor(player, "user.issue_title", "player", nickname)));
		player.sendMessage(Component.empty());
		player.sendMessage(legacy("  " + Lang.getFor(player, "user.issue_category_label") + " ")
				.append(button(player, "guide.nav_prev", "user.hover_cycle", base + "category prev"))
				.append(legacy(" &e" + categoryName + " "))
				.append(button(player, "guide.nav_next", "user.hover_cycle", base + "category next")));
		player.sendMessage(legacy("  " + Lang.getFor(player, "user.issue_offence_label") + " ")
				.append(button(player, "guide.nav_prev", "user.hover_cycle", base + "step prev"))
				.append(legacy(" " + Lang.getFor(player, "user.issue_offence", "number", draft.step + 1, "step", getStepName(player, step)) + " "))
				.append(button(player, "guide.nav_next", "user.hover_cycle", base + "step next")));
		player.sendMessage(ChatUtils.translateToColor("  " + Lang.getFor(player, "user.issue_previous",
				"category", categoryName, "count", draft.category.countOffences(punishments))));
		if (draft.step != nextStep) {
			player.sendMessage(ChatUtils.translateToColor("  " + Lang.getFor(player, "user.issue_not_next", "number", nextStep + 1)));
		}
		if (step.notifyCouncil()) {
			player.sendMessage(ChatUtils.translateToColor("  " + Lang.getFor(player, "user.issue_notify")));
		}
		if (!player.hasPermission(getPermission(step.type()))) {
			player.sendMessage(ChatUtils.translateToColor("  " + Lang.getFor(player, "user.issue_no_permission")));
		}

		String reason = draft.reason == null ? Lang.getFor(player, "user.issue_reason_none") : "&e" + draft.reason;
		player.sendMessage(legacy("  " + Lang.getFor(player, "user.issue_reason_label") + " " + reason + " ")
				.append(button(player, draft.reason == null ? "user.button_set_reason" : "user.button_edit_reason",
						"user.hover_set_reason", base + "reason")));
		player.sendMessage(Component.empty());
		player.sendMessage(Component.text("  ")
				.append(button(player, "user.button_submit", "user.hover_submit", base + "submit"))
				.append(Component.text("   "))
				.append(button(player, "user.button_cancel", "user.hover_cancel", base + "cancel"))
				.append(Component.text("   "))
				.append(button(player, "user.button_back", "guide.click_to_navigate", base + "issue")));
		player.sendMessage(ChatUtils.translateToColor(Lang.getFor(player, "guide.divider")));
	}

	/**
	 * Shows the punishment for a final confirmation, or issues it once confirmed.
	 */
	private static void submit(Player player, UUID targetUuid, String username, boolean confirmed) {
		Draft draft = drafts.get(player.getUniqueId());
		if (draft == null || !draft.targetUuid.equals(targetUuid) || draft.category == null) {
			startIssue(player, targetUuid, username, "");
			return;
		}
		if (draft.reason == null) {
			player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "user.no_reason")));
			return;
		}
		PunishmentCategory.Step step = draft.category.getSteps().get(draft.step);
		if (!player.hasPermission(getPermission(step.type()))) {
			player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "user.issue_no_permission")));
			return;
		}

		String nickname = AranarthUtils.getNickname(Bukkit.getOfflinePlayer(targetUuid));
		if (!confirmed) {
			showConfirmation(player, Lang.getFor(player, "user.confirm_issue", "step", getStepName(player, step),
					"player", nickname, "reason", draft.reason), "/ac user " + username + " submit confirm",
					"/ac user " + username + " reason", "/ac user " + username + " panel");
			return;
		}

		String category = draft.category.getKey();
		boolean issued = switch (step.type()) {
			case PunishmentUtils.WARN -> CommandWarn.warn(player, targetUuid, draft.reason, category, step.notifyCouncil());
			case PunishmentUtils.MUTE -> CommandMute.mute(player, targetUuid, step.duration(), draft.reason, category, step.notifyCouncil());
			case PunishmentUtils.BAN -> CommandBan.ban(player, targetUuid, step.duration(), draft.reason, category, step.notifyCouncil());
			default -> false;
		};
		if (issued) {
			drafts.remove(player.getUniqueId());
			player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "user.issued", "step", getStepName(player, step), "player", nickname)));
		}
	}

	// -------------------------------------------------------------------------
	// Lifting mutes and bans
	// -------------------------------------------------------------------------

	/**
	 * Asks for the reason for lifting the mute or ban, or lifts it once the reason was confirmed.
	 */
	private static void lift(Player player, UUID targetUuid, String username, Input input, boolean confirmed) {
		boolean isUnmute = input == Input.UNMUTE_REASON;
		if (!player.hasPermission(isUnmute ? "aranarth.unmute" : "aranarth.unban")) {
			player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "general.no_permission")));
			return;
		}
		if (!PunishmentUtils.outranks(player, targetUuid)) {
			player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "user.cannot_manage")));
			return;
		}
		String nickname = AranarthUtils.getNickname(Bukkit.getOfflinePlayer(targetUuid));
		AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(targetUuid);
		if (isUnmute && (aranarthPlayer == null || aranarthPlayer.getMuteEndDate().isEmpty())) {
			player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "unmute.not_muted", "name", nickname)));
			return;
		}
		if (!isUnmute && GuiUser.getBanEntry(targetUuid) == null) {
			player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "player.not_banned", "name", nickname)));
			return;
		}

		Draft draft = drafts.get(player.getUniqueId());
		if (!confirmed || draft == null || !draft.targetUuid.equals(targetUuid) || draft.liftReason == null) {
			promptReason(player, targetUuid, username, input);
			return;
		}

		List<String> commandArgs = new ArrayList<>(List.of(isUnmute ? "unmute" : "unban", username));
		commandArgs.addAll(List.of(draft.liftReason.split(" ")));
		draft.liftReason = null;
		if (isUnmute) {
			CommandUnmute.onCommand(player, commandArgs.toArray(new String[0]));
		} else {
			CommandUnban.onCommand(player, commandArgs.toArray(new String[0]));
		}
	}

	// -------------------------------------------------------------------------
	// Chat input
	// -------------------------------------------------------------------------

	private static void promptReason(Player player, UUID targetUuid, String username, Input input) {
		Draft draft = getDraft(player, targetUuid, username);
		if (input == Input.ISSUE_REASON && draft.category == null) {
			startIssue(player, targetUuid, username, "");
			return;
		}
		awaitingInput.put(player.getUniqueId(), input);
		String nickname = AranarthUtils.getNickname(Bukkit.getOfflinePlayer(targetUuid));
		String key = switch (input) {
			case ISSUE_REASON -> "user.reason_prompt";
			case UNMUTE_REASON -> "user.unmute_prompt";
			case UNBAN_REASON -> "user.unban_prompt";
		};
		player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, key, "player", nickname)));
		player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "general.type_cancel_abort")));
	}

	public static boolean isAwaitingInput(UUID uuid) {
		return awaitingInput.containsKey(uuid);
	}

	/**
	 * Processes the reason a Council member typed in chat.
	 *
	 * @param player The player who typed.
	 * @param message The raw chat message.
	 */
	public static void handleInput(Player player, String message) {
		Input input = awaitingInput.remove(player.getUniqueId());
		// Chat is handled asynchronously, so the change is made on the main thread
		Bukkit.getScheduler().runTask(AranarthCore.getInstance(), () -> {
			Draft draft = drafts.get(player.getUniqueId());
			if (input == null || draft == null) {
				return;
			}
			if (message.equalsIgnoreCase("cancel")) {
				player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "general.cancelled")));
				if (input == Input.ISSUE_REASON) {
					showIssuePanel(player, draft);
				}
				return;
			}
			String reason = ChatUtils.removeSpecialCharacters(message.trim());
			if (reason.isEmpty()) {
				player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "user.no_reason")));
				awaitingInput.put(player.getUniqueId(), input);
				return;
			}
			if (reason.length() > MAX_REASON_LENGTH) {
				player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "user.reason_too_long", "max", MAX_REASON_LENGTH)));
				awaitingInput.put(player.getUniqueId(), input);
				return;
			}

			String nickname = AranarthUtils.getNickname(Bukkit.getOfflinePlayer(draft.targetUuid));
			String base = "/ac user " + draft.username + " ";
			if (input == Input.ISSUE_REASON) {
				draft.reason = reason;
				submit(player, draft.targetUuid, draft.username, false);
			} else {
				draft.liftReason = reason;
				boolean isUnmute = input == Input.UNMUTE_REASON;
				String action = isUnmute ? "unmute" : "unban";
				showConfirmation(player, Lang.getFor(player, isUnmute ? "user.confirm_unmute" : "user.confirm_unban",
								"player", nickname, "reason", reason),
						base + action + " confirm", base + action, base.trim());
			}
		});
	}

	/**
	 * Clears anything the player was preparing, such as when they leave the server.
	 */
	public static void clear(UUID uuid) {
		drafts.remove(uuid);
		awaitingInput.remove(uuid);
	}

	// -------------------------------------------------------------------------
	// Helpers
	// -------------------------------------------------------------------------

	/**
	 * Sends a summary with buttons to confirm it, re-enter the reason, or go back.
	 */
	private static void showConfirmation(Player player, String summary, String confirmCommand, String editCommand,
										 String backCommand) {
		player.sendMessage(Component.empty());
		player.sendMessage(ChatUtils.translateToColor(Lang.getFor(player, "guide.divider")));
		player.sendMessage(ChatUtils.translateToColor("  " + summary));
		player.sendMessage(Component.empty());
		player.sendMessage(Component.text("  ")
				.append(button(player, "user.button_confirm", "user.hover_confirm", confirmCommand))
				.append(Component.text("   "))
				.append(button(player, "user.button_edit_reason", "user.hover_set_reason", editCommand))
				.append(Component.text("   "))
				.append(button(player, "user.button_back", "guide.click_to_navigate", backCommand)));
		player.sendMessage(ChatUtils.translateToColor(Lang.getFor(player, "guide.divider")));
	}

	/**
	 * Provides the player's draft for the target, starting a new one if they were preparing one for someone else.
	 */
	private static Draft getDraft(Player player, UUID targetUuid, String username) {
		Draft draft = drafts.get(player.getUniqueId());
		if (draft == null || !draft.targetUuid.equals(targetUuid)) {
			draft = new Draft(targetUuid, username);
			drafts.put(player.getUniqueId(), draft);
		}
		return draft;
	}

	private static String getPermission(String type) {
		return switch (type) {
			case PunishmentUtils.MUTE -> "aranarth.mute";
			case PunishmentUtils.BAN -> "aranarth.ban";
			default -> "aranarth.warn";
		};
	}

	private static String getStepName(Player player, PunishmentCategory.Step step) {
		if (step.type().equals(PunishmentUtils.WARN)) {
			return Lang.getFor(player, "user.step_warn");
		}
		return Lang.getFor(player, "user.step_" + step.type().toLowerCase() + "_" + step.getLengthKey());
	}

	private static String getTypeName(Player player, String type) {
		return Lang.getFor(player, switch (type) {
			case PunishmentUtils.WARN -> "punishment.type_warn";
			case PunishmentUtils.MUTE -> "punishment.type_mute";
			case PunishmentUtils.BAN -> "punishment.type_ban";
			case PunishmentUtils.UNMUTE -> "punishment.type_unmute";
			case PunishmentUtils.UNBAN -> "punishment.type_unban";
			default -> "punishment.type_other";
		});
	}

	private static String getAppliedByName(Player player, Punishment punishment) {
		if (punishment.getAppliedBy() == null) {
			return Lang.getFor(player, "punishment.console");
		}
		return AranarthUtils.getNickname(Bukkit.getOfflinePlayer(punishment.getAppliedBy()));
	}

	private static String formatDate(Punishment punishment) {
		return pad(punishment.getDate().getMonthValue()) + "/" + pad(punishment.getDate().getDayOfMonth()) + "/" + punishment.getDate().getYear();
	}

	private static String formatTime(Punishment punishment) {
		return pad(punishment.getDate().getHour()) + ":" + pad(punishment.getDate().getMinute());
	}

	private static String pad(int value) {
		return value < 10 ? "0" + value : String.valueOf(value);
	}

	private static int parsePage(String page) {
		try {
			return Integer.parseInt(page);
		} catch (NumberFormatException e) {
			return 1;
		}
	}

	/**
	 * Provides the target's username with its proper capitalisation, so the clickable commands stay valid.
	 */
	private static String getUsername(UUID uuid, String fallback) {
		AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(uuid);
		if (aranarthPlayer != null && aranarthPlayer.getUsername() != null && !aranarthPlayer.getUsername().isEmpty()) {
			return aranarthPlayer.getUsername();
		}
		OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
		return offlinePlayer.getName() != null ? offlinePlayer.getName() : fallback;
	}

	private static TextComponent legacy(String text) {
		return LegacyComponentSerializer.legacySection().deserialize(ChatUtils.translateToColor(text));
	}

	private static Component button(Player player, String labelKey, String hoverKey, String command) {
		return buttonText(Lang.getFor(player, labelKey), Lang.getFor(player, hoverKey), command);
	}

	private static Component buttonText(String label, String hover, String command) {
		return ChatUtils.clickableCommand(legacy(label), ChatUtils.translateToColor(hover), command, false);
	}
}
