package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.enums.SpecialDay;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.DateUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.permissions.PermissionAttachment;

public class PlayerServerJoin implements Listener {

	public PlayerServerJoin(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	/**
	 * Adds a new entry to the players HashMap if the player is not being tracked.
	 * Additionally, customizes the join/leave server message format.
	 * @param e The event.
	 */
	@EventHandler
	public void onPlayerJoin(final PlayerJoinEvent e) {
		Player player = e.getPlayer();
		if (!AranarthUtils.hasPlayedBefore(player)) {
			AranarthUtils.addPlayer(player.getUniqueId(), new AranarthPlayer(player.getName()));
		}
		// If the player changed their username
		else if (!AranarthUtils.getUsername(player).equals(player.getName())) {
			AranarthUtils.setUsername(player);
		}
		DateUtils dateUtils = new DateUtils();
		String nameToDisplay;
		
		if (!AranarthUtils.getNickname(player).isEmpty()) {
			nameToDisplay = AranarthUtils.getNickname(player);
		} else {
			nameToDisplay = AranarthUtils.getUsername(player);
		}
		
		if (dateUtils.isValentinesDay()) {
			e.setJoinMessage(ChatUtils.translateToColor("&8[&a+&8] &7" + ChatUtils.getSpecialJoinMessage(nameToDisplay, SpecialDay.VALENTINES)));
		} else if (dateUtils.isEaster()) {
			e.setJoinMessage(ChatUtils.translateToColor("&8[&a+&8] &7" + ChatUtils.getSpecialJoinMessage(nameToDisplay, SpecialDay.EASTER)));
		} else if (dateUtils.isHalloween()) {
			e.setJoinMessage(ChatUtils.translateToColor("&8[&a+&8] &7" + ChatUtils.getSpecialJoinMessage(nameToDisplay, SpecialDay.HALLOWEEN)));
		} else if (dateUtils.isChristmas()) {
			e.setJoinMessage(ChatUtils.translateToColor("&8[&a+&8] &7" + ChatUtils.getSpecialJoinMessage(nameToDisplay, SpecialDay.CHRISTMAS)));
		} else {
			e.setJoinMessage(ChatUtils.translateToColor("&8[&a+&8] &7" + nameToDisplay));
		}

		PermissionAttachment perms = player.addAttachment(AranarthCore.getInstance());
		perms.setPermission("bending.command.rechoose", true);

		// Add blue fire to players that want it
		if (player.getName().equalsIgnoreCase("_Breathtaking")) {
			Bukkit.dispatchCommand(Bukkit.getServer().getConsoleSender(), "b a BlueFire " + player.getName());
		}
	}
	
}
