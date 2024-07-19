package com.aearost.aranarthcore.event;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Camel;
import org.bukkit.entity.Horse;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;

public class MountSwim implements Listener {

	private AranarthCore plugin;

	public MountSwim(AranarthCore plugin) {
		this.plugin = plugin;
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	/**
	 * Allows mounts to swim in water and staying on the surface
	 * Also makes their swim speed relative to their movement speed
	 * 
	 * @author Aearost
	 *
	 */
	@EventHandler
	public void onMountSwim(final PlayerInteractEvent e) {

		Player player = e.getPlayer();
		if (player.isInsideVehicle() && (player.getVehicle() instanceof Horse || player.getVehicle() instanceof Camel)) {

			// Code based on https://www.spigotmc.org/resources/swimminghorses.72920/
			Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable() {
				@Override
				public void run() {
					Player p = e.getPlayer();
					if (p.getVehicle() instanceof AbstractHorse) {
						AbstractHorse mount = (AbstractHorse) p.getVehicle();
						
						if (mount != null && isInLiquid(mount)) {
							AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
							if (aranarthPlayer.getIsMountSwimEnabled()) {
								// Try to find the way to make this speed relative to the speed of the mount
								mount.setVelocity(mount.getLocation().getDirection().multiply(0.5));
							}
							// Must be called in order to float regardless if the value is enabled
							if (hasLand(mount)) {
								jump(mount);
							} else {
								swim(mount);
							}
						}
					}
				}
			}, 0L, 0L);
		}
	}

	// Controls how high the mount will be brought up when the timer runs
	// We want this very low so that it refreshes more often
	private void jump(LivingEntity livingEntity) {
		livingEntity.setVelocity(livingEntity.getVelocity().setY(0.01));
		//livingEntity.addPotionEffect(new PotionEffect(PotionEffectType.DOLPHINS_GRACE, 10, 100));
	}

	// Called whenever the mount goes underwater, this way it will send it back above
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
