package com.aearost.aranarthcore.gui;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.destroystokyo.paper.profile.PlayerProfile;
import com.gmail.nossr50.datatypes.database.PlayerStat;
import com.gmail.nossr50.datatypes.skills.PrimarySkillType;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class GuiMctop {

	private final Player player;
	private final Inventory initializedGui;

	public GuiMctop(Player player, @org.jetbrains.annotations.Nullable PrimarySkillType skill, int pageNum,
					List<PlayerStat> leaderboard, Map<String, PlayerProfile> profiles) {
		this.player = player;
		this.initializedGui = initializeGui(player, skill, leaderboard, profiles);
	}

	public void openGui() {
		player.closeInventory();
		if (initializedGui != null) {
			player.openInventory(initializedGui);
		}
	}

	private Inventory initializeGui(Player player, @org.jetbrains.annotations.Nullable PrimarySkillType skill,
									List<PlayerStat> leaderboard, Map<String, PlayerProfile> profiles) {
		String skillDisplayName = skill == null ? "Overall" : skill.name().charAt(0) + skill.name().substring(1).toLowerCase();
		Inventory gui = Bukkit.getServer().createInventory(player, 54, "Top " + skillDisplayName);

		ItemStack previous = new ItemStack(Material.RED_WOOL);
		ItemStack barrier = new ItemStack(Material.BARRIER);
		ItemStack next = new ItemStack(Material.LIME_WOOL);
		ItemStack blank = new ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE);

		ItemMeta previousMeta = previous.getItemMeta();
		if (Objects.nonNull(previousMeta)) {
			previousMeta.setDisplayName(ChatUtils.translateToColor("&c&lPrevious"));
			previous.setItemMeta(previousMeta);
		}

		ItemMeta barrierMeta = barrier.getItemMeta();
		if (Objects.nonNull(barrierMeta)) {
			barrierMeta.setDisplayName(ChatUtils.translateToColor("&4&lExit"));
			barrier.setItemMeta(barrierMeta);
		}

		ItemMeta nextMeta = next.getItemMeta();
		if (Objects.nonNull(nextMeta)) {
			nextMeta.setDisplayName(ChatUtils.translateToColor("&a&lNext"));
			next.setItemMeta(nextMeta);
		}

		ItemMeta blankMeta = blank.getItemMeta();
		if (Objects.nonNull(blankMeta)) {
			blankMeta.setDisplayName(ChatUtils.translateToColor("&f"));
			blank.setItemMeta(blankMeta);
		}

		gui.setItem(45, previous);
		gui.setItem(46, blank);
		gui.setItem(47, blank);
		gui.setItem(48, blank);
		gui.setItem(49, barrier);
		gui.setItem(50, blank);
		gui.setItem(51, blank);
		gui.setItem(52, blank);
		gui.setItem(53, next);

		for (int i = 0; i < 45; i++) {
			if (i >= leaderboard.size()) {
				gui.setItem(i, blank);
				continue;
			}

			PlayerStat stat = leaderboard.get(i);
			ItemStack head = new ItemStack(Material.PLAYER_HEAD);
			SkullMeta skullMeta = (SkullMeta) head.getItemMeta();

			// Set the pre-loaded profile
			PlayerProfile profile = profiles.get(stat.playerName());
			if (profile != null) {
				skullMeta.setPlayerProfile(profile);
			}

			UUID uuid = profile != null && profile.getId() != null ? profile.getId() : null;
			AranarthPlayer aranarthPlayer = uuid != null ? AranarthUtils.getPlayer(uuid) : null;
			String displayName = aranarthPlayer != null ? aranarthPlayer.getNickname() : stat.playerName();
			skullMeta.setDisplayName(ChatUtils.translateToColor(displayName));

			List<String> lore = new ArrayList<>();
			lore.add(ChatUtils.translateToColor("&7Level " + stat.value()));
			skullMeta.setLore(lore);
			head.setItemMeta(skullMeta);
			gui.setItem(i, head);
		}
		return gui;
	}

}
