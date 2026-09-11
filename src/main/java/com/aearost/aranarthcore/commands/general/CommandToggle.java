package com.aearost.aranarthcore.commands.general;

import com.aearost.aranarthcore.enums.FireType;
import com.aearost.aranarthcore.event.listener.misc.InvisibleArmorManager;
import com.aearost.aranarthcore.gui.GuiToggle;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.Perk;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.GateUtils;
import com.aearost.aranarthcore.utils.Lang;
import com.aearost.aranarthcore.utils.PermissionUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Centralizes the logic for toggling different functionality in Aranarth.
 */
public class CommandToggle implements CommandExecutor {

    private static final Map<UUID, Long> pendingBarbarianToggle = new HashMap<>();
    private static final long BARBARIAN_CONFIRM_TIMEOUT_MS = 30_000L;
    private static final long BARBARIAN_COOLDOWN_MS = 72L * 60 * 60 * 1000; // 72 hours

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
			if (args.length >= 1) {
				AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
				if (args[0].equalsIgnoreCase("chat")) {
					if (player.hasPermission("aranarth.toggle.chat")) {
						if (aranarthPlayer.isTogglingChat()) {
							aranarthPlayer.setTogglingChat(false);
							player.sendMessage(ChatUtils.chatMessage(Lang.get("toggle.chat_enabled")));
						} else {
							aranarthPlayer.setTogglingChat(true);
							player.sendMessage(ChatUtils.chatMessage(Lang.get("toggle.chat_disabled")));
						}
						AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
					} else {
						player.sendMessage(ChatUtils.chatMessage(Lang.get("general.no_permission")));
						return true;
					}
				} else if (args[0].equalsIgnoreCase("messages")) {
					if (player.hasPermission("aranarth.toggle.msg")) {
						if (aranarthPlayer.isTogglingMessages()) {
							aranarthPlayer.setTogglingMessages(false);
							player.sendMessage(ChatUtils.chatMessage(Lang.get("toggle.messages_enabled")));
						} else {
							aranarthPlayer.setTogglingMessages(true);
							player.sendMessage(ChatUtils.chatMessage(Lang.get("toggle.messages_disabled")));
						}
						AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
					} else {
						player.sendMessage(ChatUtils.chatMessage(Lang.get("general.no_permission")));
						return true;
					}
				} else if (args[0].equalsIgnoreCase("teleport")) {
					if (player.hasPermission("aranarth.toggle.tp")) {
						if (aranarthPlayer.isTogglingTp()) {
							aranarthPlayer.setTogglingTp(false);
							player.sendMessage(ChatUtils.chatMessage(Lang.get("toggle.tp_enabled")));
						} else {
							aranarthPlayer.setTogglingTp(true);
							player.sendMessage(ChatUtils.chatMessage(Lang.get("toggle.tp_disabled")));
						}
						AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
					} else {
						player.sendMessage(ChatUtils.chatMessage(Lang.get("general.no_permission")));
						return true;
					}
				} else if (args[0].equalsIgnoreCase("spawnboost")) {
					// Everyone has access
					if (aranarthPlayer.isUsingSpawnBoost()) {
						aranarthPlayer.setUsingSpawnBoost(false);
						player.sendMessage(ChatUtils.chatMessage(Lang.get("toggle.spawnboost_disabled")));
						if (AranarthUtils.isSpawnLocation(player.getLocation())) {
							player.clearActivePotionEffects();
						}
					} else {
						aranarthPlayer.setUsingSpawnBoost(true);
						player.sendMessage(ChatUtils.chatMessage(Lang.get("toggle.spawnboost_enabled")));
					}
					AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
				} else if (args[0].equalsIgnoreCase("changeclaim")) {
					// Everyone has access
					if (aranarthPlayer.isTogglingChangeClaim()) {
						aranarthPlayer.setTogglingChangeClaim(false);
						player.sendMessage(ChatUtils.chatMessage(Lang.get("toggle.changeclaim_enabled")));
					} else {
						aranarthPlayer.setTogglingChangeClaim(true);
						player.sendMessage(ChatUtils.chatMessage(Lang.get("toggle.changeclaim_disabled")));
					}
					AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
				} else if (args[0].equalsIgnoreCase("inventory")) {
					if (player.hasPermission("aranarth.inventory")) {
						if (aranarthPlayer.isTogglingInventoryAssist()) {
							aranarthPlayer.setTogglingInventoryAssist(false);
							player.sendMessage(ChatUtils.chatMessage(Lang.get("toggle.inventory_enabled")));
						} else {
							aranarthPlayer.setTogglingInventoryAssist(true);
							player.sendMessage(ChatUtils.chatMessage(Lang.get("toggle.inventory_disabled")));
						}
						AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
					} else {
						player.sendMessage(ChatUtils.chatMessage(Lang.get("toggle.no_inventory_perk")));
					}
				} else if (args[0].equalsIgnoreCase("shulker")) {
					if (player.hasPermission("aranarth.shulker")) {
						if (aranarthPlayer.isAddingToShulker()) {
							aranarthPlayer.setAddingToShulker(false);
							player.sendMessage(ChatUtils.chatMessage(Lang.get("toggle.shulker_disabled")));
						} else {
							aranarthPlayer.setAddingToShulker(true);
							player.sendMessage(ChatUtils.chatMessage(Lang.get("toggle.shulker_enabled")));
						}
						AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
					} else {
						player.sendMessage(ChatUtils.chatMessage(Lang.get("toggle.no_shulker_perk")));
					}
				} else if (args[0].equalsIgnoreCase("blacklist")) {
					if (player.hasPermission("aranarth.blacklist")) {
						if (args.length == 1) {
							player.sendMessage(ChatUtils.chatMessage(Lang.get("general.invalid_syntax", "usage", "toggle blacklist <ignore|trash|off>")));
						} else {
							if (args[1].equals("ignore")) {
								aranarthPlayer.setBlacklistingMethod(0);
								player.sendMessage(ChatUtils.chatMessage(Lang.get("toggle.blacklist_ignore")));
							} else if (args[1].equals("trash")) {
								aranarthPlayer.setBlacklistingMethod(1);
								player.sendMessage(ChatUtils.chatMessage(Lang.get("toggle.blacklist_trash")));
							} else if (args[1].equals("off")) {
								aranarthPlayer.setBlacklistingMethod(-1);
								player.sendMessage(ChatUtils.chatMessage(Lang.get("toggle.blacklist_off")));
							} else {
								player.sendMessage(ChatUtils.chatMessage(Lang.get("general.invalid_syntax", "usage", "toggle blacklist <ignore|trash|off>")));
								return true;
							}
							AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
						}
					} else {
						player.sendMessage(ChatUtils.chatMessage(Lang.get("toggle.no_blacklist_perk")));
					}
				} else if (args[0].equalsIgnoreCase("compressor")) {
					if (player.hasPermission("aranarth.compressor")) {
						if (aranarthPlayer.isCompressingItems()) {
							aranarthPlayer.setCompressingItems(false);
							player.sendMessage(ChatUtils.chatMessage(Lang.get("toggle.compressor_disabled")));
						} else {
							aranarthPlayer.setCompressingItems(true);
							player.sendMessage(ChatUtils.chatMessage(Lang.get("toggle.compressor_enabled")));
						}
						AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
					} else {
						player.sendMessage(ChatUtils.chatMessage(Lang.get("toggle.no_compressor_perk")));
					}
				} else if (args[0].equalsIgnoreCase("chestlock")) {
					if (aranarthPlayer.isAutoLockingChests()) {
						aranarthPlayer.setAutoLockingChests(false);
						player.sendMessage(ChatUtils.chatMessage(Lang.get("toggle.chestlock_disabled")));
					} else {
						aranarthPlayer.setAutoLockingChests(true);
						player.sendMessage(ChatUtils.chatMessage(Lang.get("toggle.chestlock_enabled")));
					}
					AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
				} else if (args[0].equalsIgnoreCase("firetype")) {
					if (hasAnyFirePerk(aranarthPlayer)) {
						FireType oldType = aranarthPlayer.getFireType();
						FireType newType = nextAvailableType(oldType, aranarthPlayer);
						if (newType == oldType) {
							player.sendMessage(ChatUtils.chatMessage(Lang.get("toggle.firetype_no_others")));
							return true;
						}
						aranarthPlayer.setFireType(newType);
						player.sendMessage(ChatUtils.chatMessage(Lang.get("toggle.firetype_set", "type", newType.getDisplayName())));
						AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
						if (oldType == FireType.BLUE || newType == FireType.BLUE) {
							PermissionUtils.evaluatePlayerPermissions(player);
						}
					} else {
						player.sendMessage(ChatUtils.chatMessage(Lang.get("toggle.no_firetype_perks")));
					}
				} else if (args[0].equalsIgnoreCase("pethurt")) {
					if (aranarthPlayer.isHurtingOwnPets()) {
						aranarthPlayer.setHurtingOwnPets(false);
						player.sendMessage(ChatUtils.chatMessage(Lang.get("toggle.pethurt_disabled")));
					} else {
						aranarthPlayer.setHurtingOwnPets(true);
						player.sendMessage(ChatUtils.chatMessage(Lang.get("toggle.pethurt_enabled")));
					}
					AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
				} else if (args[0].equalsIgnoreCase("gradientchat")) {
					if (!aranarthPlayer.getPerks().containsKey(Perk.CHAT) && aranarthPlayer.getSaintRank() < 2) {
						player.sendMessage(ChatUtils.chatMessage(Lang.get("toggle.no_gradientchat_perk")));
						return true;
					}
					if (args.length >= 2) {
						if (args[1].equalsIgnoreCase("get")) {
							if (aranarthPlayer.getGradientChatColors().isEmpty()) {
								player.sendMessage(ChatUtils.chatMessage(Lang.get("toggle.no_gradient_colors")));
								return true;
							}
							player.sendMessage(ChatUtils.chatMessage(ChatUtils.formatGradientColorsDisplay(aranarthPlayer.getGradientChatColors())));
							return true;
						} else if (args[1].equalsIgnoreCase("bold")) {
							if (aranarthPlayer.isGradientChatBold()) {
								aranarthPlayer.setGradientChatBold(false);
								player.sendMessage(ChatUtils.chatMessage(Lang.get("toggle.gradientchat_bold_disabled")));
							} else {
								aranarthPlayer.setGradientChatBold(true);
								player.sendMessage(ChatUtils.chatMessage(Lang.get("toggle.gradientchat_bold_enabled")));
							}
							AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
							return true;
						}
						// Validate and save new color pattern, then enable
						String colors = args[1];
						String[] colorArray = colors.split(",");
						if (colorArray.length < 2) {
							player.sendMessage(ChatUtils.chatMessage(Lang.get("toggle.gradient_min_colors")));
							return true;
						}
						boolean validColors = true;
						for (String color : colorArray) {
							if (!ChatUtils.isValidGradientColor(color)) {
								validColors = false;
								break;
							}
						}
						if (!validColors) {
							player.sendMessage(ChatUtils.chatMessage(Lang.get("toggle.gradient_invalid_color")));
							return true;
						}
						aranarthPlayer.setGradientChatColors(colors);
						aranarthPlayer.setGradientChatEnabled(true);
						player.sendMessage(ChatUtils.chatMessage(ChatUtils.translateToGradient(colors, "Your gradient chat colors have been updated", false)));
					} else {
						// Toggle on/off using the saved pattern
						if (aranarthPlayer.isGradientChatEnabled()) {
							aranarthPlayer.setGradientChatEnabled(false);
							player.sendMessage(ChatUtils.chatMessage(Lang.get("toggle.gradientchat_disabled")));
						} else {
							if (aranarthPlayer.getGradientChatColors().isEmpty()) {
								player.sendMessage(ChatUtils.chatMessage(Lang.get("toggle.no_gradient_colors")));
								return true;
							}
							aranarthPlayer.setGradientChatEnabled(true);
							player.sendMessage(ChatUtils.chatMessage(Lang.get("toggle.gradientchat_enabled")));
						}
					}
					AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
				} else if (args[0].equalsIgnoreCase("gate")) {
					if (player.hasPermission("aranarth.gate")) {
						boolean enabled = GateUtils.toggleGatePlacementMode(player.getUniqueId());
						if (enabled) {
							player.sendMessage(ChatUtils.chatMessage(Lang.get("toggle.gate_enabled")));
						} else {
							player.sendMessage(ChatUtils.chatMessage(Lang.get("toggle.gate_disabled")));
						}
					} else {
						player.sendMessage(ChatUtils.chatMessage(Lang.get("general.no_permission")));
					}
				} else if (args[0].equalsIgnoreCase("daymessage")) {
					if (aranarthPlayer.isDayMessageDisabled()) {
						aranarthPlayer.setDayMessageDisabled(false);
						player.sendMessage(ChatUtils.chatMessage(Lang.get("toggle.daymessage_enabled")));
					} else {
						aranarthPlayer.setDayMessageDisabled(true);
						player.sendMessage(ChatUtils.chatMessage(Lang.get("toggle.daymessage_disabled")));
					}
					AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
				} else if (args[0].equalsIgnoreCase("weathermessage")) {
					if (aranarthPlayer.isWeatherMessageDisabled()) {
						aranarthPlayer.setWeatherMessageDisabled(false);
						player.sendMessage(ChatUtils.chatMessage(Lang.get("toggle.weathermessage_enabled")));
					} else {
						aranarthPlayer.setWeatherMessageDisabled(true);
						player.sendMessage(ChatUtils.chatMessage(Lang.get("toggle.weathermessage_disabled")));
					}
					AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
				} else if (args[0].equalsIgnoreCase("shulkerbulk")) {
					if (player.hasPermission("aranarth.shulker")) {
						if (aranarthPlayer.isBulkSellShulkerEnabled()) {
							aranarthPlayer.setBulkSellShulkerEnabled(false);
							player.sendMessage(ChatUtils.chatMessage(Lang.get("toggle.shulkerbulk_disabled")));
						} else {
							aranarthPlayer.setBulkSellShulkerEnabled(true);
							player.sendMessage(ChatUtils.chatMessage(Lang.get("toggle.shulkerbulk_enabled")));
						}
						AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
					} else {
						player.sendMessage(ChatUtils.chatMessage(Lang.get("toggle.no_shulker_perk")));
					}
				} else if (args[0].equalsIgnoreCase("dmsgcompact")) {
					if (aranarthPlayer.isDominionMsgCompact()) {
						aranarthPlayer.setDominionMsgCompact(false);
						player.sendMessage(ChatUtils.chatMessage(Lang.get("toggle.dmsgcompact_disabled")));
					} else {
						aranarthPlayer.setDominionMsgCompact(true);
						player.sendMessage(ChatUtils.chatMessage(Lang.get("toggle.dmsgcompact_enabled")));
					}
					AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
				} else if (args[0].equalsIgnoreCase("emoji")) {
					if (aranarthPlayer.isEmojiEnabled()) {
						aranarthPlayer.setEmojiEnabled(false);
						player.sendMessage(ChatUtils.chatMessage(Lang.get("toggle.emoji_disabled")));
					} else {
						aranarthPlayer.setEmojiEnabled(true);
						player.sendMessage(ChatUtils.chatMessage(Lang.get("toggle.emoji_enabled")));
					}
					AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
				} else if (args[0].equalsIgnoreCase("interactivechat")) {
					boolean hasPerm = aranarthPlayer.getSaintRank() >= 2 || aranarthPlayer.getCouncilRank() > 0;
					if (!hasPerm) {
						player.sendMessage(ChatUtils.chatMessage(Lang.get("general.no_permission")));
						return true;
					}
					if (aranarthPlayer.isInteractiveChatEnabled()) {
						aranarthPlayer.setInteractiveChatEnabled(false);
						player.sendMessage(ChatUtils.chatMessage(Lang.get("toggle.interactivechat_disabled")));
					} else {
						aranarthPlayer.setInteractiveChatEnabled(true);
						player.sendMessage(ChatUtils.chatMessage(Lang.get("toggle.interactivechat_enabled")));
					}
					AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
				} else if (args[0].equalsIgnoreCase("barbarian")) {
					long now = System.currentTimeMillis();
					long cooldownEnd = aranarthPlayer.getBarbarianCooldownEnd();
					if (cooldownEnd > now) {
						long remainingMs = cooldownEnd - now;
						long hours = remainingMs / 3_600_000L;
						long minutes = (remainingMs % 3_600_000L) / 60_000L;
						player.sendMessage(ChatUtils.chatMessage(Lang.get("toggle.barbarian_cooldown", "hours", String.valueOf(hours), "minutes", String.valueOf(minutes))));
						return true;
					}
					boolean becomingBarbarian = !aranarthPlayer.isBarbarian();
					Long pendingExpiry = pendingBarbarianToggle.get(player.getUniqueId());
					if (pendingExpiry == null || now > pendingExpiry) {
						// First run - show confirmation prompt
						pendingBarbarianToggle.put(player.getUniqueId(), now + BARBARIAN_CONFIRM_TIMEOUT_MS);
						if (becomingBarbarian) {
							player.sendMessage(ChatUtils.chatMessage(Lang.get("toggle.barbarian_confirm_enable")));
							player.sendMessage(ChatUtils.chatMessage(Lang.get("toggle.barbarian_confirm_hint")));
						} else {
							player.sendMessage(ChatUtils.chatMessage(Lang.get("toggle.barbarian_confirm_disable")));
							player.sendMessage(ChatUtils.chatMessage(Lang.get("toggle.barbarian_confirm_hint")));
						}
						return true;
					}
					// Second run within window - execute the toggle
					pendingBarbarianToggle.remove(player.getUniqueId());
					aranarthPlayer.setBarbarian(becomingBarbarian);
					aranarthPlayer.setBarbarianCooldownEnd(now + BARBARIAN_COOLDOWN_MS);
					AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
					if (becomingBarbarian) {
						player.sendMessage(ChatUtils.chatMessage(Lang.get("toggle.barbarian_enabled")));
					} else {
						player.sendMessage(ChatUtils.chatMessage(Lang.get("toggle.barbarian_disabled")));
					}
				} else if (args[0].equalsIgnoreCase("invisiblearmor")) {
					if (!player.hasPermission("aranarth.invisiblearmor")) {
						player.sendMessage(ChatUtils.chatMessage(Lang.get("toggle.no_invisiblearmor_perk")));
						return true;
					}
					if (InvisibleArmorManager.isArmorHidden(player.getUniqueId())) {
						InvisibleArmorManager.showArmor(player);
						player.sendMessage(ChatUtils.chatMessage(Lang.get("toggle.armor_visible")));
					} else {
						InvisibleArmorManager.hideArmor(player);
						player.sendMessage(ChatUtils.chatMessage(Lang.get("toggle.armor_hidden")));
					}
				} else if (args[0].equalsIgnoreCase("reaper")) {
					if (aranarthPlayer.isReaperDisabled()) {
						aranarthPlayer.setReaperDisabled(false);
						player.sendMessage(ChatUtils.chatMessage(Lang.get("toggle.reaper_enabled")));
					} else {
						aranarthPlayer.setReaperDisabled(true);
						player.sendMessage(ChatUtils.chatMessage(Lang.get("toggle.reaper_disabled")));
					}
					AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
				} else if (args[0].equalsIgnoreCase("size")) {
					if (aranarthPlayer.isSizeScaleEnabled()) {
						aranarthPlayer.setSizeScaleEnabled(false);
						player.sendMessage(ChatUtils.chatMessage(Lang.get("toggle.size_disabled")));
					} else {
						aranarthPlayer.setSizeScaleEnabled(true);
						player.sendMessage(ChatUtils.chatMessage(Lang.get("toggle.size_enabled")));
					}
					AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
					AranarthUtils.applyAranarthiumScale(player);
				} else if (args[0].equalsIgnoreCase("servertips")) {
					if (aranarthPlayer.isServerTipsDisabled()) {
						aranarthPlayer.setServerTipsDisabled(false);
						player.sendMessage(ChatUtils.chatMessage(Lang.get("toggle.servertips_enabled")));
					} else {
						aranarthPlayer.setServerTipsDisabled(true);
						player.sendMessage(ChatUtils.chatMessage(Lang.get("toggle.servertips_disabled")));
					}
					AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
				} else {
					player.sendMessage(ChatUtils.chatMessage(Lang.get("general.invalid_syntax", "usage", "toggle <option>")));
				}
				return true;
			} else {
				new GuiToggle(player).openGui();
				return true;
			}
		} else {
			sender.sendMessage(ChatUtils.chatMessage(Lang.get("general.no_permission_console")));
			return true;
		}
	}

	private boolean hasAnyFirePerk(AranarthPlayer aranarthPlayer) {
		return aranarthPlayer.getPerks().getOrDefault(Perk.BLUEFIRE, 0) == 1
				|| aranarthPlayer.getPerks().getOrDefault(Perk.WHITEFIRE, 0) == 1
				|| aranarthPlayer.getPerks().getOrDefault(Perk.PRISMATICFIRE, 0) == 1
;
	}

	private FireType nextAvailableType(FireType current, AranarthPlayer aranarthPlayer) {
		FireType[] values = FireType.values();
		int start = current.ordinal();
		for (int i = 1; i < values.length; i++) {
			FireType candidate = values[(start + i) % values.length];
			if (canUseFireType(candidate, aranarthPlayer)) {
				return candidate;
			}
		}
		return current;
	}

	private boolean canUseFireType(FireType type, AranarthPlayer aranarthPlayer) {
		return switch (type) {
			case DEFAULT -> true;
			case BLUE -> aranarthPlayer.getPerks().getOrDefault(Perk.BLUEFIRE, 0) == 1;
			case WHITE -> aranarthPlayer.getPerks().getOrDefault(Perk.WHITEFIRE, 0) == 1;
			case PRISMATIC -> aranarthPlayer.getPerks().getOrDefault(Perk.PRISMATICFIRE, 0) == 1;
		};
	}

}
