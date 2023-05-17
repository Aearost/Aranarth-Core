package com.aearost.aranarthcore.event;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Horse;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;

public class HorseSwim implements Listener {

	private AranarthCore plugin;

	public HorseSwim(AranarthCore plugin) {
		this.plugin = plugin;
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	/**
	 * Allows horses to swim in water and staying on the surface
	 * Also makes their swim speed relative to their movement speed
	 * 
	 * @author Aearost
	 *
	 */
	@EventHandler
	public void onHorseSwim(final PlayerInteractEvent e) {

		Player player = e.getPlayer();
		if (player.isInsideVehicle() && player.getVehicle() instanceof Horse) {

			// Code based on https://www.spigotmc.org/resources/swimminghorses.72920/
			Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable() {
				@Override
				public void run() {
					Player p = e.getPlayer();
					if (p.getVehicle() instanceof Horse) {
						Horse horse = (Horse) p.getVehicle();
						
						if (horse != null && isInLiquid(horse)) {
							AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
							if (aranarthPlayer.getIsHorseSwimEnabled()) {
								// Try to find the way to make this speed relative to the speed of the horse
								horse.setVelocity(horse.getLocation().getDirection().multiply(0.5));
							}
							// Must be called in order to float regardless if the value is enabled
							if (hasLand(horse)) {
								jump(horse);
							} else {
								swim(horse);
							}
						}
					}
				}
			}, 0L, 0L);
		}
	}

	// Controls how high the horse will be brought up when the timer runs
	// We want this very low so that it refreshes more often
	private void jump(LivingEntity livingEntity) {
		livingEntity.setVelocity(livingEntity.getVelocity().setY(0.01));
		//livingEntity.addPotionEffect(new PotionEffect(PotionEffectType.DOLPHINS_GRACE, 10, 100));
	}

	// Called whenever the horse goes underwater, this way it will send it back above
	private void swim(LivingEntity livingEntity) {
		livingEntity.setVelocity(livingEntity.getVelocity().setY(0.01));
		//livingEntity.addPotionEffect(new PotionEffect(PotionEffectType.DOLPHINS_GRACE, 10, 100));
	}

	private boolean hasLand(LivingEntity livingEntity) {
		return livingEntity.getEyeLocation().add(livingEntity.getLocation().getDirection())
				.getBlock().getType() != Material.WATER;
	}

	private boolean isInLiquid(LivingEntity livingEntity) {
		// First and Last values don't seem to do anything
		// Middle value is the frequency of how often it will refresh
		Block block = livingEntity.getLocation().clone().add(0, 0.1, 0).getBlock();

		return block.getType() == Material.WATER || block.getType() == Material.LAVA;
	}

}
