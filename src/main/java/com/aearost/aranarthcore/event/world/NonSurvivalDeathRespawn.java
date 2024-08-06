package com.aearost.aranarthcore.event.world;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

public class NonSurvivalDeathRespawn implements Listener {

	public NonSurvivalDeathRespawn(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	/**
	 * Saves the player's level and EXP before dying in the arena or creative world.
	 * @param e The event.
	 */
	@EventHandler
	public void onPlayerDeath(final PlayerDeathEvent e) {
		String world = e.getEntity().getWorld().getName();
		if (world.equalsIgnoreCase("arena") || world.equalsIgnoreCase("creative")) {
			Player player = e.getEntity();
			AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
			aranarthPlayer.setLevelBeforeDeath(player.getLevel());
			aranarthPlayer.setExpBeforeDeath(player.getExp());
			AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
		}
	}

	/**
	 * Reverts the player's level and EXP when dying in the arena or creative world.
	 * Automatically equips the player with iron armor when teleporting to the arena world.
	 * Additionally forces the player to respawn in the same non-survival world when they die in it.
	 * @param e The event.
	 */
	@EventHandler
	public void onPlayerRespawn(final PlayerRespawnEvent e) {
		String world = e.getPlayer().getWorld().getName();
		if (world.equalsIgnoreCase("arena") || world.equalsIgnoreCase("creative")) {
			Player player = e.getPlayer();
			player.getInventory().clear();
			if (world.equalsIgnoreCase("arena")) {
				e.setRespawnLocation(new Location(Bukkit.getWorld("arena"), 0.5, 105, 0.5, 180, 2));

				player.getInventory().setArmorContents(new ItemStack[] {
						new ItemStack(Material.IRON_BOOTS, 1),
						new ItemStack(Material.IRON_LEGGINGS, 1),
						new ItemStack(Material.IRON_CHESTPLATE, 1),
						new ItemStack(Material.IRON_HELMET, 1)});
			} else {
				e.setRespawnLocation(new Location(Bukkit.getWorld("creative"), 0, -60, 0, 0, 2));
				player.setGameMode(GameMode.CREATIVE);
			}

			AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
			int level = aranarthPlayer.getLevelBeforeDeath();
			float exp = aranarthPlayer.getExpBeforeDeath();
			new BukkitRunnable() {
				@Override
				public void run() {
					player.setLevel(level);
					player.setExp(exp);
				}
			}.runTaskLater(AranarthCore.getInstance(), 1L);
			aranarthPlayer.setLevelBeforeDeath(0);
			aranarthPlayer.setExpBeforeDeath(0);
			AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
		}
	}
}
