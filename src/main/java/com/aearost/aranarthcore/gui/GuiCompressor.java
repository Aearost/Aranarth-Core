package com.aearost.aranarthcore.gui;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.UUID;

public class GuiCompressor {

	private final Player player;
	private final Inventory initializedGui;

	public GuiCompressor(Player player) {
		this.player = player;
		this.initializedGui = initializeGui(player);
	}

	public void openGui() {
		player.closeInventory();
		if (initializedGui != null) {
			player.openInventory(initializedGui);
		}
	}
	
	private Inventory initializeGui(Player player) {
		UUID uuid = player.getUniqueId();
		AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
		Inventory gui = null;
		int guiSize = 45;
		String guiName = "Compressible Items";
		gui = Bukkit.getServer().createInventory(player, guiSize, guiName);

		ItemStack blank = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
		ItemMeta blankMeta = blank.getItemMeta();
		blankMeta.setDisplayName(ChatUtils.translateToColor("&f"));
		blank.setItemMeta(blankMeta);
		gui.setItem(0, blank);
		gui.setItem(1, blank);
		gui.setItem(2, blank);
		gui.setItem(3, blank);
		gui.setItem(5, blank);
		gui.setItem(6, blank);
		gui.setItem(7, blank);
		gui.setItem(8, blank);

		Material toggledType = Material.LIME_STAINED_GLASS_PANE;
		if (!aranarthPlayer.getIsCompressingItems()) {
			toggledType = Material.RED_STAINED_GLASS_PANE;
		}
		ItemStack toggled = new ItemStack(toggledType);
		ItemMeta toggledMeta = toggled.getItemMeta();
		if (aranarthPlayer.getIsCompressingItems()) {
			toggledMeta.setDisplayName(ChatUtils.translateToColor("&a&lCompressor is toggled on"));
		} else {
			toggledMeta.setDisplayName(ChatUtils.translateToColor("&c&lCompressor is toggled off"));
		}
		toggled.setItemMeta(toggledMeta);
		gui.setItem(4, toggled);

		ItemStack coal = new ItemStack(Material.COAL);
		String coalActive = getStatusOfItem(uuid, coal.getType());
		ItemMeta coalMeta = coal.getItemMeta();
		coalMeta.setDisplayName(ChatUtils.translateToColor("&8&lCoal &7&l- " + coalActive));
		coal.setItemMeta(coalMeta);
		gui.setItem(9, coal);

		ItemStack rawCopper = new ItemStack(Material.RAW_COPPER);
		String rawCopperActive = getStatusOfItem(uuid, rawCopper.getType());
		ItemMeta rawCopperMeta = rawCopper.getItemMeta();
		rawCopperMeta.setDisplayName(ChatUtils.translateToColor("#b87333&lRaw Copper &7&l- " + rawCopperActive));
		rawCopper.setItemMeta(rawCopperMeta);
		gui.setItem(10, rawCopper);

		ItemStack copperNugget = new ItemStack(Material.COPPER_NUGGET);
		String copperNuggetActive = getStatusOfItem(uuid, copperNugget.getType());
		ItemMeta copperNuggetMeta = copperNugget.getItemMeta();
		copperNuggetMeta.setDisplayName(ChatUtils.translateToColor("#b87333&lCopper Nugget &7&l- " + copperNuggetActive));
		copperNugget.setItemMeta(copperNuggetMeta);
		gui.setItem(11, copperNugget);

		ItemStack copperIngot = new ItemStack(Material.COPPER_INGOT);
		String copperIngotActive = getStatusOfItem(uuid, copperIngot.getType());
		ItemMeta copperIngotMeta = copperIngot.getItemMeta();
		copperIngotMeta.setDisplayName(ChatUtils.translateToColor("#b87333&lCopper Ingot &7&l- " + copperIngotActive));
		copperIngot.setItemMeta(copperIngotMeta);
		gui.setItem(12, copperIngot);

		ItemStack rawIron = new ItemStack(Material.RAW_IRON);
		String rawIronActive = getStatusOfItem(uuid, rawIron.getType());
		ItemMeta rawIronMeta = rawIron.getItemMeta();
		rawIronMeta.setDisplayName(ChatUtils.translateToColor("#eeeeee&lRaw Iron &7&l- " + rawIronActive));
		rawIron.setItemMeta(rawIronMeta);
		gui.setItem(13, rawIron);

		ItemStack ironNugget = new ItemStack(Material.IRON_NUGGET);
		String ironNuggetActive = getStatusOfItem(uuid, ironNugget.getType());
		ItemMeta ironNuggetMeta = ironNugget.getItemMeta();
		ironNuggetMeta.setDisplayName(ChatUtils.translateToColor("#eeeeee&lIron Nugget &7&l- " + ironNuggetActive));
		ironNugget.setItemMeta(ironNuggetMeta);
		gui.setItem(14, ironNugget);

		ItemStack ironIngot = new ItemStack(Material.IRON_INGOT);
		String ironIngotActive = getStatusOfItem(uuid, ironIngot.getType());
		ItemMeta ironIngotMeta = ironIngot.getItemMeta();
		ironIngotMeta.setDisplayName(ChatUtils.translateToColor("#eeeeee&lIron Ingot &7&l- " + ironIngotActive));
		ironIngot.setItemMeta(ironIngotMeta);
		gui.setItem(15, ironIngot);

		ItemStack rawGold = new ItemStack(Material.RAW_GOLD);
		String rawGoldActive = getStatusOfItem(uuid, rawGold.getType());
		ItemMeta rawGoldMeta = rawGold.getItemMeta();
		rawGoldMeta.setDisplayName(ChatUtils.translateToColor("#fcd34d&lRaw Gold &7&l- " + rawGoldActive));
		rawGold.setItemMeta(rawGoldMeta);
		gui.setItem(16, rawGold);

		ItemStack goldNugget = new ItemStack(Material.GOLD_NUGGET);
		String goldNuggetActive = getStatusOfItem(uuid, goldNugget.getType());
		ItemMeta goldNuggetMeta = goldNugget.getItemMeta();
		goldNuggetMeta.setDisplayName(ChatUtils.translateToColor("#fcd34d&lGold Nugget &7&l- " + goldNuggetActive));
		goldNugget.setItemMeta(goldNuggetMeta);
		gui.setItem(17, goldNugget);

		ItemStack goldIngot = new ItemStack(Material.GOLD_INGOT);
		String goldIngotActive = getStatusOfItem(uuid, goldIngot.getType());
		ItemMeta goldIngotMeta = goldIngot.getItemMeta();
		goldIngotMeta.setDisplayName(ChatUtils.translateToColor("#fcd34d&lGold Ingot &7&l- " + goldIngotActive));
		goldIngot.setItemMeta(goldIngotMeta);
		gui.setItem(18, goldIngot);

		ItemStack redstone = new ItemStack(Material.REDSTONE);
		String redstoneActive = getStatusOfItem(uuid, redstone.getType());
		ItemMeta redstoneMeta = redstone.getItemMeta();
		redstoneMeta.setDisplayName(ChatUtils.translateToColor("#aa0000&lRedstone &7&l- " + redstoneActive));
		redstone.setItemMeta(redstoneMeta);
		gui.setItem(19, redstone);

		ItemStack lapis = new ItemStack(Material.LAPIS_LAZULI);
		String lapisActive = getStatusOfItem(uuid, lapis.getType());
		ItemMeta lapisMeta = lapis.getItemMeta();
		lapisMeta.setDisplayName(ChatUtils.translateToColor("#4169e1&lLapis Lazuli &7&l- " + lapisActive));
		lapis.setItemMeta(lapisMeta);
		gui.setItem(20, lapis);

		ItemStack diamond = new ItemStack(Material.DIAMOND);
		String diamondActive = getStatusOfItem(uuid, diamond.getType());
		ItemMeta diamondMeta = diamond.getItemMeta();
		diamondMeta.setDisplayName(ChatUtils.translateToColor("#a0f0ed&lDiamond &7&l- " + diamondActive));
		diamond.setItemMeta(diamondMeta);
		gui.setItem(21, diamond);

		ItemStack emerald = new ItemStack(Material.EMERALD);
		String emeraldActive = getStatusOfItem(uuid, emerald.getType());
		ItemMeta emeraldMeta = emerald.getItemMeta();
		emeraldMeta.setDisplayName(ChatUtils.translateToColor("#50c878&lEmerald &7&l- " + emeraldActive));
		emerald.setItemMeta(emeraldMeta);
		gui.setItem(22, emerald);

		ItemStack netheriteIngot = new ItemStack(Material.NETHERITE_INGOT);
		String netheriteIngotActive = getStatusOfItem(uuid, netheriteIngot.getType());
		ItemMeta netheriteIngotMeta = netheriteIngot.getItemMeta();
		netheriteIngotMeta.setDisplayName(ChatUtils.translateToColor("#3b3131&lNetherite Ingot &7&l- " + netheriteIngotActive));
		netheriteIngot.setItemMeta(netheriteIngotMeta);
		gui.setItem(23, netheriteIngot);

		ItemStack amethystShard = new ItemStack(Material.AMETHYST_SHARD);
		String amethystShardActive = getStatusOfItem(uuid, amethystShard.getType());
		ItemMeta amethystShardMeta = amethystShard.getItemMeta();
		amethystShardMeta.setDisplayName(ChatUtils.translateToColor("#A020F0&lAmethyst Shard &7&l- " + amethystShardActive));
		amethystShard.setItemMeta(amethystShardMeta);
		gui.setItem(24, amethystShard);

		ItemStack resinClump = new ItemStack(Material.RESIN_CLUMP);
		String resinClumpActive = getStatusOfItem(uuid, resinClump.getType());
		ItemMeta resinClumpMeta = resinClump.getItemMeta();
		resinClumpMeta.setDisplayName(ChatUtils.translateToColor("#e5640e&lResin Clump &7&l- " + resinClumpActive));
		resinClump.setItemMeta(resinClumpMeta);
		gui.setItem(25, resinClump);

		ItemStack glowstoneDust = new ItemStack(Material.GLOWSTONE_DUST);
		String glowstoneDustActive = getStatusOfItem(uuid, glowstoneDust.getType());
		ItemMeta glowstoneDustMeta = glowstoneDust.getItemMeta();
		glowstoneDustMeta.setDisplayName(ChatUtils.translateToColor("&6&lGlowstone Dust &7&l- " + glowstoneDustActive));
		glowstoneDust.setItemMeta(glowstoneDustMeta);
		gui.setItem(26, glowstoneDust);

		ItemStack wheat = new ItemStack(Material.WHEAT);
		String wheatActive = getStatusOfItem(uuid, wheat.getType());
		ItemMeta wheatMeta = wheat.getItemMeta();
		wheatMeta.setDisplayName(ChatUtils.translateToColor("#cbaf58&lWheat &7&l- " + wheatActive));
		wheat.setItemMeta(wheatMeta);
		gui.setItem(27, wheat);

		ItemStack melonSlice = new ItemStack(Material.MELON_SLICE);
		String melonSliceActive = getStatusOfItem(uuid, melonSlice.getType());
		ItemMeta melonSliceMeta = melonSlice.getItemMeta();
		melonSliceMeta.setDisplayName(ChatUtils.translateToColor("&c&lMelon Slice &7&l- " + melonSliceActive));
		melonSlice.setItemMeta(melonSliceMeta);
		gui.setItem(28, melonSlice);

		ItemStack driedKelp = new ItemStack(Material.DRIED_KELP);
		String driedKelpActive = getStatusOfItem(uuid, driedKelp.getType());
		ItemMeta driedKelpMeta = driedKelp.getItemMeta();
		driedKelpMeta.setDisplayName(ChatUtils.translateToColor("#3b3224&lDried Kelp &7&l- " + driedKelpActive));
		driedKelp.setItemMeta(driedKelpMeta);
		gui.setItem(29, driedKelp);

		ItemStack sugarcane = new ItemStack(Material.SUGAR_CANE);
		String sugarcaneActive = getStatusOfItem(uuid, sugarcane.getType());
		ItemMeta sugarcaneMeta = sugarcane.getItemMeta();
		sugarcaneMeta.setDisplayName(ChatUtils.translateToColor("#7bca34&lSugar Cane &7&l- " + sugarcaneActive));
		sugarcane.setItemMeta(sugarcaneMeta);
		gui.setItem(30, sugarcane);

		ItemStack honeycomb = new ItemStack(Material.HONEYCOMB);
		String honeycombActive = getStatusOfItem(uuid, honeycomb.getType());
		ItemMeta honeycombMeta = honeycomb.getItemMeta();
		honeycombMeta.setDisplayName(ChatUtils.translateToColor("#f7bd28&lHoneycomb &7&l- " + honeycombActive));
		honeycomb.setItemMeta(honeycombMeta);
		gui.setItem(31, honeycomb);

		ItemStack slimeBall = new ItemStack(Material.SLIME_BALL);
		String slimeBallActive = getStatusOfItem(uuid, slimeBall.getType());
		ItemMeta slimeBallMeta = slimeBall.getItemMeta();
		slimeBallMeta.setDisplayName(ChatUtils.translateToColor("&a&lSlime Ball &7&l- " + slimeBallActive));
		slimeBall.setItemMeta(slimeBallMeta);
		gui.setItem(32, slimeBall);

		ItemStack boneMeal = new ItemStack(Material.BONE_MEAL);
		String boneMealActive = getStatusOfItem(uuid, boneMeal.getType());
		ItemMeta boneMealMeta = boneMeal.getItemMeta();
		boneMealMeta.setDisplayName(ChatUtils.translateToColor("#c9c4a3&lBone Meal &7&l- " + boneMealActive));
		boneMeal.setItemMeta(boneMealMeta);
		gui.setItem(33, boneMeal);

		ItemStack snowball = new ItemStack(Material.SNOWBALL);
		String snowballActive = getStatusOfItem(uuid, snowball.getType());
		ItemMeta snowballMeta = snowball.getItemMeta();
		snowballMeta.setDisplayName(ChatUtils.translateToColor("#e5f5f5&lSnowball &7&l- " + snowballActive));
		snowball.setItemMeta(snowballMeta);
		gui.setItem(34, snowball);

		ItemStack clayBall = new ItemStack(Material.CLAY_BALL);
		String clayBallActive = getStatusOfItem(uuid, clayBall.getType());
		ItemMeta clayBallMeta = clayBall.getItemMeta();
		clayBallMeta.setDisplayName(ChatUtils.translateToColor("#adb7d3&lClay Ball &7&l- " + clayBallActive));
		clayBall.setItemMeta(clayBallMeta);
		gui.setItem(35, clayBall);

		ItemStack exit = new ItemStack(Material.BARRIER);
		ItemMeta exitMeta = exit.getItemMeta();
		exitMeta.setDisplayName(ChatUtils.translateToColor("&4&lExit"));
		exit.setItemMeta(exitMeta);

		ItemStack enableAll = new ItemStack(Material.LIME_WOOL);
		ItemMeta enableAllMeta = enableAll.getItemMeta();
		enableAllMeta.setDisplayName(ChatUtils.translateToColor("&a&lEnable All"));
		enableAll.setItemMeta(enableAllMeta);

		ItemStack disableAll = new ItemStack(Material.RED_WOOL);
		ItemMeta disableAllMeta = disableAll.getItemMeta();
		disableAllMeta.setDisplayName(ChatUtils.translateToColor("&c&lDisable All"));
		disableAll.setItemMeta(disableAllMeta);

		gui.setItem(36, enableAll);
		gui.setItem(40, exit);
		gui.setItem(44, disableAll);

		return gui;
	}

	/**
	 * Provides the text value stating whether the item is being compressed by the player or not.
	 * @param uuid The player's UUID.
	 * @param material The Material that is being iterated.
	 * @return The text value stating whether the item is being compressed or not.
	 */
	private String getStatusOfItem(UUID uuid, Material material) {
		boolean isBeingCompressed = AranarthUtils.isItemBeingCompressed(uuid, material);
		return isBeingCompressed ? "&a&lYes" : "&c&lNo";
	}

}
