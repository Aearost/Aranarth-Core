package com.aearost.aranarthcore.event.listener.grouped;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.abilities.airbending.AstralProjection;
import com.aearost.aranarthcore.abilities.waterbending.VineWhip;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.projectkorra.projectkorra.BendingPlayer;
import com.projectkorra.projectkorra.Element;
import com.projectkorra.projectkorra.ability.*;
import com.projectkorra.projectkorra.event.BendingReloadEvent;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.UUID;

/**
 * Handles all logic regarding the use of AranarthCore bending abilities.
 */
public class AranarthCoreBendingListener implements Listener {

	private final HashMap<UUID, AstralProjection> activeProjections = new HashMap<>();

	public AranarthCoreBendingListener(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	/**
	 * Handles reloading the abilities when ProjectKorra is reloaded.
	 * @param event The event.
	 */
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onPKReload(final BendingReloadEvent event) {
		new BukkitRunnable() {
			@Override
			public void run() {
				CoreAbility.registerPluginAbilities(AranarthCore.getInstance(), "com.aearost.aranarthcore.abilities");
				Bukkit.getLogger().info("AranarthCore Bending Reloaded");
			}
		}.runTaskLater(AranarthCore.getInstance(), 1L);
	}
	
	/**
	 * Deals with cancelling explosion block damage.
	 * @param e The event.
	 */
	@EventHandler
	public void onPlayerSneak(final PlayerToggleSneakEvent e) {
		Player player = e.getPlayer();
		BendingPlayer bendingPlayer = BendingPlayer.getBendingPlayer(player);
		if (bendingPlayer == null) {
			return;
		}

		CoreAbility ability = bendingPlayer.getBoundAbility();
		String abilityName = bendingPlayer.getBoundAbilityName();

		if (e.isSneaking() && player.getGameMode() == GameMode.SURVIVAL) {
			if (!bendingPlayer.canCurrentlyBendWithWeapons()) {
				return;
			}

			// Airbending
			if (ability instanceof AirAbility && bendingPlayer.isElementToggled(Element.AIR)) {
				if (ability instanceof SpiritualAbility) {
					if (abilityName.equalsIgnoreCase("astralprojection")) {
						AstralProjection astralProjection = new AstralProjection(e.getPlayer());
						activeProjections.put(player.getUniqueId(), astralProjection);
					}
				}
			}
			// Waterbending
			else if (ability instanceof WaterAbility && bendingPlayer.isElementToggled(Element.WATER)) {
				if (ability instanceof PlantAbility) {
					if (abilityName.equalsIgnoreCase("vinewhip")) {
						new VineWhip(e.getPlayer());
					}
				}
			}
		}
	}

	//  Below for AstralProjection overrides
	@EventHandler
	public void onMannequinDamage(final EntityDamageEvent e) {
		if (e.getEntityType() == EntityType.MANNEQUIN) {
			if (e.getCause() == EntityDamageEvent.DamageCause.KILL) {
				return;
			}

			e.setDamage(0);
			UUID toRemove = null;
			for (UUID uuid : activeProjections.keySet()) {
				AstralProjection projection = activeProjections.get(uuid);
				if (projection.getMannequin().equals(e.getEntity())) {
					toRemove = uuid;
				}
			}

			if (activeProjections.get(toRemove) != null) {
				activeProjections.get(toRemove).endAbility();
				activeProjections.remove(toRemove);
			}
		}
	}

	@EventHandler
	public void onGamemodeChange(PlayerGameModeChangeEvent e) {
		if (e.getPlayer().getGameMode() == GameMode.SPECTATOR && e.getNewGameMode() == GameMode.SURVIVAL) {
			if (e.getCause() == PlayerGameModeChangeEvent.Cause.PLUGIN) {
                activeProjections.remove(e.getPlayer().getUniqueId());
			}
		}
	}

	@EventHandler
	public void onSpectateEntity(PlayerInteractAtEntityEvent e) {
		if (e.getPlayer().getGameMode() == GameMode.SPECTATOR) {
			e.setCancelled(true);
		}
	}

	@EventHandler
	public void onSpectatePlayerAttempt(PlayerTeleportEvent e) {
		Player player = e.getPlayer();
		if (player.getGameMode() == GameMode.SPECTATOR) {
			if (e.getCause() == PlayerTeleportEvent.TeleportCause.SPECTATE) {
				e.setCancelled(true);
				player.setSpectatorTarget(null);
			}
		}
	}

	@EventHandler
	public void onCommand(PlayerCommandPreprocessEvent e) {
		if (e.getPlayer().getGameMode() == GameMode.SPECTATOR) {
			e.setCancelled(true);
			e.getPlayer().sendMessage(ChatUtils.chatMessage("&cYou cannot run commands while Astral Projecting!"));
		}
	}

	@EventHandler
	public void onInteract(PlayerInteractEvent e) {
		if (e.getPlayer().getGameMode() == GameMode.SPECTATOR) {
			e.setCancelled(true);
		}
	}

	@EventHandler
	public void onInteract(InventoryClickEvent e) {
		if (e.getWhoClicked() instanceof Player player && player.getGameMode() == GameMode.SPECTATOR) {
			e.setCancelled(true);
		}
	}

	@EventHandler
	public void onHotbarUse(PlayerItemHeldEvent e) {
		if (e.getPlayer().getGameMode() == GameMode.SPECTATOR) {
			e.setCancelled(true);
		}
	}
}
