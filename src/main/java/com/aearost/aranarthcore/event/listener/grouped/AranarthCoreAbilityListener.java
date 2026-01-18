package com.aearost.aranarthcore.event.listener.grouped;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.abilities.AstralProjection;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.projectkorra.projectkorra.BendingPlayer;
import com.projectkorra.projectkorra.ability.CoreAbility;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;

import java.util.HashMap;
import java.util.UUID;

/**
 * Handles all logic regarding the use of AranarthCore bending abilities.
 */
public class AranarthCoreAbilityListener implements Listener {

	private final HashMap<UUID, AstralProjection> activeProjections = new HashMap<>();

	public AranarthCoreAbilityListener(AranarthCore plugin) {
		CoreAbility.registerPluginAbilities(AranarthCore.getInstance(), "com.aearost.aranarthcore.abilities");

		Bukkit.getPluginManager().registerEvents(this, plugin);
	}
	
	/**
	 * Deals with cancelling explosion block damage.
	 * @param e The event.
	 */
	@EventHandler
	public void onPlayerSneak(final PlayerToggleSneakEvent e) {
		Player player = e.getPlayer();
		BendingPlayer bendingPlayer = BendingPlayer.getBendingPlayer(player);
		String abilityName = bendingPlayer.getBoundAbilityName();
		if (e.isSneaking() && player.getGameMode() == GameMode.SURVIVAL) {
			if (abilityName.equalsIgnoreCase("astralprojection")) {
				AstralProjection astralProjection = new AstralProjection(e.getPlayer());
				activeProjections.put(player.getUniqueId(), astralProjection);
			}
		}
	}

	//  Below for AstralProjection overrides
	@EventHandler
	public void onMannequinDamage(final EntityDamageEvent e) {
		if (e.getEntityType() == EntityType.MANNEQUIN) {
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
	public void onSpectate(PlayerInteractAtEntityEvent e) {
		if (e.getPlayer().getGameMode() == GameMode.SPECTATOR) {
			e.setCancelled(true);
		}
	}

	@EventHandler
	public void onSpectate(PlayerTeleportEvent e) {
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
