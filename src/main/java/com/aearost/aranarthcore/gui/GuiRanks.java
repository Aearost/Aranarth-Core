package com.aearost.aranarthcore.gui;

import com.aearost.aranarthcore.abilities.airbending.soundbending.SoundAbility;
import com.aearost.aranarthcore.enums.Pronouns;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.Lang;
import com.projectkorra.projectkorra.Element;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;

/**
 * A chest GUI that appears when the user enters /ranks. This GUI displays all
 * available ranks on the server, including ranks available through earning
 * money on the server, as well as ranks available through donations.
 * 
 * @author Aearost
 *
 */
public class GuiRanks {

	private final Player player;
	private final Inventory initializedGui;

	public GuiRanks(Player player) {
		this.player = player;
		this.initializedGui = initializeGui(player);
	}

	public void openGui() {
		player.openInventory(initializedGui);
	}

	private Inventory initializeGui(Player player) {
		Inventory gui = Bukkit.getServer().createInventory(player, 45,
				Lang.getFor(player, "gui.ranks.title"));

		AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
		Pronouns pronouns = aranarthPlayer.getPronouns();
		int rank = aranarthPlayer.getRank();

		// Initialize Items
		ItemStack yellowPane = new ItemStack(Material.YELLOW_STAINED_GLASS_PANE);
		ItemStack blackPane = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
		ItemStack peasant = new ItemStack(Material.LIME_CONCRETE_POWDER);
		ItemStack esquire = new ItemStack(Material.PINK_CONCRETE_POWDER);
		ItemStack knight = new ItemStack(Material.WHITE_CONCRETE_POWDER);
		ItemStack baron = new ItemStack(Material.MAGENTA_CONCRETE_POWDER);
		ItemStack count = new ItemStack(Material.GRAY_CONCRETE_POWDER);
		ItemStack duke = new ItemStack(Material.YELLOW_CONCRETE_POWDER);
		ItemStack prince = new ItemStack(Material.LIGHT_BLUE_CONCRETE_POWDER);
		ItemStack king = new ItemStack(Material.BLUE_CONCRETE_POWDER);
		ItemStack emperor = new ItemStack(Material.RED_CONCRETE_POWDER);

		// Removing name of panes
		ItemMeta yellowPaneMeta = yellowPane.getItemMeta();
		yellowPaneMeta.setDisplayName(" ");
		yellowPane.setItemMeta(yellowPaneMeta);
		ItemMeta blackPaneMeta = blackPane.getItemMeta();
		blackPaneMeta.setDisplayName(" ");
		blackPane.setItemMeta(blackPaneMeta);

		// Peasant
		ItemMeta peasantMeta = peasant.getItemMeta();
		ArrayList<String> peasantLore = new ArrayList<>();
		if (rank == 0) {
			peasantMeta.setDisplayName(Lang.getFor(player, "gui.ranks.peasant") + Lang.getFor(player, "gui.ranks.current_suffix"));
		} else {
			peasantMeta.setDisplayName(Lang.getFor(player, "gui.ranks.peasant"));
		}
		peasantLore.add(Lang.getFor(player, "gui.ranks.bending"));
		peasantLore.add(Lang.getFor(player, "gui.ranks.peasant_bending1"));
		peasantLore.add(Lang.getFor(player, "gui.ranks.peasant_bending2"));
		peasantLore.add(Lang.getFor(player, "gui.ranks.perks"));
		peasantLore.add(Lang.getFor(player, "gui.ranks.peasant_perk1"));
		peasantLore.add(Lang.getFor(player, "gui.ranks.peasant_perk2"));
		peasantLore.add(Lang.getFor(player, "gui.ranks.peasant_perk3"));
		peasantLore.add(Lang.getFor(player, "gui.ranks.peasant_perk4"));
		peasantMeta.setLore(peasantLore);
		peasant.setItemMeta(peasantMeta);

		// Esquire
		ItemMeta esquireMeta = esquire.getItemMeta();
		ArrayList<String> esquireLore = new ArrayList<>();
		if (rank == 1) {
			esquireMeta.setDisplayName(Lang.getFor(player, "gui.ranks.esquire") + Lang.getFor(player, "gui.ranks.current_suffix"));
		} else {
			esquireMeta.setDisplayName(Lang.getFor(player, "gui.ranks.esquire"));
		}
		esquireLore.add(Lang.getFor(player, "gui.ranks.bending"));
		esquireLore.add(ChatUtils.translateToColor("&f&o- &3[&bW&3] " + Element.PLANT.getColor() + "&oBasic plantbending abilities"));
		esquireLore.add(ChatUtils.translateToColor("&f&o- &2[&aE&2] " + Element.SAND.getColor() + "&oBasic sandbending abilities"));
		esquireLore.add(Lang.getFor(player, "gui.ranks.esquire_bending1"));
		esquireLore.add(Lang.getFor(player, "gui.ranks.perks"));
		esquireLore.add(Lang.getFor(player, "gui.ranks.esquire_perk1"));
		esquireLore.add(Lang.getFor(player, "gui.ranks.esquire_perk2"));
		esquireLore.add(Lang.getFor(player, "gui.ranks.esquire_perk3"));
		esquireLore.add(Lang.getFor(player, "gui.ranks.esquire_perk4"));
		esquireLore.add(Lang.getFor(player, "gui.ranks.esquire_perk5"));
		esquireLore.add("");
		esquireLore.add(Lang.getFor(player, "gui.ranks.esquire_req1"));
		esquireLore.add(Lang.getFor(player, "gui.ranks.esquire_perk6"));
		esquireMeta.setLore(esquireLore);
		esquire.setItemMeta(esquireMeta);

		// Knight
		ItemMeta knightMeta = knight.getItemMeta();
		ArrayList<String> knightLore = new ArrayList<>();
		if (rank == 2) {
			knightMeta.setDisplayName(Lang.getFor(player, "gui.ranks.knight") + Lang.getFor(player, "gui.ranks.current_suffix"));
		} else {
			knightMeta.setDisplayName(Lang.getFor(player, "gui.ranks.knight"));
		}
		knightLore.add(Lang.getFor(player, "gui.ranks.bending"));
		knightLore.add(ChatUtils.translateToColor("&f&o- &8[&7A&8] &7&oAirSnipe"));
		knightLore.add(ChatUtils.translateToColor("&f&o- &3[&bW&3] &3&oBasic healing abilities"));
		knightLore.add(ChatUtils.translateToColor("&f&o- &2[&aE&2] " + Element.METAL.getColor() + "&oBasic metalbending abilities"));
		knightLore.add(ChatUtils.translateToColor("&f&o- &6[&eC&6] &6&oAcrobatStance, WarriorStance"));
		knightLore.add(Lang.getFor(player, "gui.ranks.knight_bending1"));
		knightLore.add(Lang.getFor(player, "gui.ranks.perks"));
		knightLore.add(Lang.getFor(player, "gui.ranks.knight_perk1"));
		knightLore.add(Lang.getFor(player, "gui.ranks.knight_perk2"));
		knightLore.add(Lang.getFor(player, "gui.ranks.knight_perk3"));
		knightLore.add(Lang.getFor(player, "gui.ranks.knight_perk4"));
		knightLore.add(Lang.getFor(player, "gui.ranks.knight_perk5"));
		knightLore.add("");
		knightLore.add(Lang.getFor(player, "gui.ranks.knight_req1"));
		knightLore.add(Lang.getFor(player, "gui.ranks.knight_perk6"));
		knightMeta.setLore(knightLore);
		knight.setItemMeta(knightMeta);

		// Baron
		ItemMeta baronMeta = baron.getItemMeta();
		ArrayList<String> baronLore = new ArrayList<>();
		if (pronouns == Pronouns.MALE) {
			if (rank == 3) {
				baronMeta.setDisplayName(Lang.getFor(player, "gui.ranks.baron") + Lang.getFor(player, "gui.ranks.current_suffix"));
			} else {
				baronMeta.setDisplayName(Lang.getFor(player, "gui.ranks.baron"));
			}
		} else {
			if (rank == 3) {
				baronMeta.setDisplayName(Lang.getFor(player, "gui.ranks.baroness") + Lang.getFor(player, "gui.ranks.current_suffix"));
			} else {
				baronMeta.setDisplayName(Lang.getFor(player, "gui.ranks.baroness"));
			}
		}
		baronLore.add(Lang.getFor(player, "gui.ranks.bending"));
		baronLore.add(ChatUtils.translateToColor("&f&o- &8[&7A&8] " + Element.SPIRITUAL.getColor() + "&oBasic spiritual abilities"));
		baronLore.add(ChatUtils.translateToColor("&f&o- &3[&bW&3] &b&oWaterArms, " + Element.PLANT.getColor() + "&oToxicSpores, "));
		baronLore.add(ChatUtils.translateToColor(Element.PLANT.getColor() + "       &oVineWhip, LeafScythe"));
		baronLore.add(ChatUtils.translateToColor("&f&o- &2[&aE&2] " + Element.SAND.getColor() + "&oSandstorm, Burial, " + Element.METAL.getColor() + "&oCableThrash,"));
		baronLore.add(ChatUtils.translateToColor(Element.METAL.getColor() + "       &oCableSlash, MetalBlade, MetalFragments"));
		baronLore.add(Lang.getFor(player, "gui.ranks.baron_bending1"));
		baronLore.add(Lang.getFor(player, "gui.ranks.perks"));
		baronLore.add(Lang.getFor(player, "gui.ranks.baron_perk1"));
		baronLore.add(Lang.getFor(player, "gui.ranks.baron_perk2"));
		baronLore.add(Lang.getFor(player, "gui.ranks.baron_perk3"));
		baronLore.add(Lang.getFor(player, "gui.ranks.baron_perk4"));
		baronLore.add(Lang.getFor(player, "gui.ranks.baron_perk5"));
		baronLore.add(Lang.getFor(player, "gui.ranks.baron_perk6"));
		baronLore.add("");
		baronLore.add(Lang.getFor(player, "gui.ranks.baron_req1"));
		baronLore.add(Lang.getFor(player, "gui.ranks.baron_req2"));
		baronLore.add(Lang.getFor(player, "gui.ranks.baron_perk7"));
		baronMeta.setLore(baronLore);
		baron.setItemMeta(baronMeta);

		// Count
		ItemMeta countMeta = count.getItemMeta();
		ArrayList<String> countLore = new ArrayList<>();
		if (pronouns == Pronouns.MALE) {
			if (rank == 4) {
				countMeta.setDisplayName(Lang.getFor(player, "gui.ranks.count") + Lang.getFor(player, "gui.ranks.current_suffix"));
			} else {
				countMeta.setDisplayName(Lang.getFor(player, "gui.ranks.count"));
			}
		} else {
			if (rank == 4) {
				countMeta.setDisplayName(Lang.getFor(player, "gui.ranks.countess") + Lang.getFor(player, "gui.ranks.current_suffix"));
			} else {
				countMeta.setDisplayName(Lang.getFor(player, "gui.ranks.countess"));
			}
		}
		countLore.add(Lang.getFor(player, "gui.ranks.bending"));
		countLore.add(ChatUtils.translateToColor("&f&o- &8[&7A&8] " + Element.SPIRITUAL.getColor() + "&oAngeredSpirits"));
		countLore.add(ChatUtils.translateToColor("&f&o- &3[&bW&3] &b&oIceShards, &3&oHealingHelix"));
		countLore.add(ChatUtils.translateToColor("&f&o- &2[&aE&2] " + Element.METAL.getColor() + "&oMetalShots, MetalStrips"));
		countLore.add(ChatUtils.translateToColor("&f&o- &6[&eC&6] &6&oDaggerVolley"));
		countLore.add(Lang.getFor(player, "gui.ranks.count_bending1"));
		countLore.add(Lang.getFor(player, "gui.ranks.perks"));
		countLore.add(Lang.getFor(player, "gui.ranks.count_perk1"));
		countLore.add(Lang.getFor(player, "gui.ranks.count_perk2"));
		countLore.add(Lang.getFor(player, "gui.ranks.count_perk3"));
		countLore.add(Lang.getFor(player, "gui.ranks.count_perk4"));
		countLore.add(Lang.getFor(player, "gui.ranks.count_perk5"));
		countLore.add(Lang.getFor(player, "gui.ranks.count_perk6"));
		countLore.add("");
		countLore.add(Lang.getFor(player, "gui.ranks.count_req1"));
		countLore.add(Lang.getFor(player, "gui.ranks.count_req2"));
		countLore.add(Lang.getFor(player, "gui.ranks.count_perk7"));
		countMeta.setLore(countLore);
		count.setItemMeta(countMeta);

		// Duke
		ItemMeta dukeMeta = duke.getItemMeta();
		ArrayList<String> dukeLore = new ArrayList<>();
		if (pronouns == Pronouns.MALE) {
			if (rank == 5) {
				dukeMeta.setDisplayName(Lang.getFor(player, "gui.ranks.duke") + Lang.getFor(player, "gui.ranks.current_suffix"));
			} else {
				dukeMeta.setDisplayName(Lang.getFor(player, "gui.ranks.duke"));
			}
		} else {
			if (rank == 5) {
				dukeMeta.setDisplayName(Lang.getFor(player, "gui.ranks.duchess") + Lang.getFor(player, "gui.ranks.current_suffix"));
			} else {
				dukeMeta.setDisplayName(Lang.getFor(player, "gui.ranks.duchess"));
			}
		}
		dukeLore.add(Lang.getFor(player, "gui.ranks.bending"));
		dukeLore.add(ChatUtils.translateToColor("&f&o- &8[&7A&8] &7&oSuffocate, " + Element.SPIRITUAL.getColor() + "&oAstralProjection, AstralShot,"));
		dukeLore.add(ChatUtils.translateToColor(SoundAbility.SOUND.getColor() + "       &oBasic soundbending abilities"));
		dukeLore.add(ChatUtils.translateToColor("&f&o- &2[&aE&2] " + Element.LAVA.getColor() + "&oBasic lavabending abilities"));
		dukeLore.add(Lang.getFor(player, "gui.ranks.duke_bending1"));
		dukeLore.add(Lang.getFor(player, "gui.ranks.perks"));
		dukeLore.add(Lang.getFor(player, "gui.ranks.duke_perk1"));
		dukeLore.add(Lang.getFor(player, "gui.ranks.duke_perk2"));
		dukeLore.add(Lang.getFor(player, "gui.ranks.duke_perk3"));
		dukeLore.add(Lang.getFor(player, "gui.ranks.duke_perk4"));
		dukeLore.add(Lang.getFor(player, "gui.ranks.duke_perk5"));
		dukeLore.add(Lang.getFor(player, "gui.ranks.duke_perk6"));
		dukeLore.add("");
		dukeLore.add(Lang.getFor(player, "gui.ranks.duke_req1"));
		dukeLore.add(Lang.getFor(player, "gui.ranks.duke_req2"));
		dukeLore.add(Lang.getFor(player, "gui.ranks.duke_perk7"));
		dukeMeta.setLore(dukeLore);
		duke.setItemMeta(dukeMeta);

		// Prince
		ItemMeta princeMeta = prince.getItemMeta();
		ArrayList<String> princeLore = new ArrayList<>();
		if (pronouns == Pronouns.MALE) {
			if (rank == 6) {
				princeMeta.setDisplayName(Lang.getFor(player, "gui.ranks.prince") + Lang.getFor(player, "gui.ranks.current_suffix"));
			} else {
				princeMeta.setDisplayName(Lang.getFor(player, "gui.ranks.prince"));
			}
		} else {
			if (rank == 6) {
				princeMeta.setDisplayName(Lang.getFor(player, "gui.ranks.princess") + Lang.getFor(player, "gui.ranks.current_suffix"));
			} else {
				princeMeta.setDisplayName(Lang.getFor(player, "gui.ranks.princess"));
			}
		}
		princeLore.add(Lang.getFor(player, "gui.ranks.bending"));
		princeLore.add(ChatUtils.translateToColor("&f&o- &4[&cF&4] " + Element.LIGHTNING.getColor() + "&oBasic lightningbending abilities, Static"));
		princeLore.add(ChatUtils.translateToColor("&f&o- &8[&7A&8] " + Element.SPIRITUAL.getColor() + "&oEnergyBurst, " + SoundAbility.SOUND.getColor() + "&oSonicPulse"));
		princeLore.add(ChatUtils.translateToColor("&f&o- &3[&bW&3] &3&oCorruptingHelix, " + Element.BLOOD.getColor() + "&oBasic blood abilities"));
		princeLore.add(ChatUtils.translateToColor("&f&o- &2[&aE&2] " + Element.LAVA.getColor() + "&oLavaDisc, MagmaBlast"));
		princeLore.add(Lang.getFor(player, "gui.ranks.prince_bending1"));
		princeLore.add(Lang.getFor(player, "gui.ranks.perks"));
		princeLore.add(Lang.getFor(player, "gui.ranks.prince_perk1"));
		princeLore.add(Lang.getFor(player, "gui.ranks.prince_perk2"));
		princeLore.add(Lang.getFor(player, "gui.ranks.prince_perk3"));
		princeLore.add(Lang.getFor(player, "gui.ranks.prince_perk4"));
		princeLore.add(Lang.getFor(player, "gui.ranks.prince_perk5"));
		princeLore.add(Lang.getFor(player, "gui.ranks.prince_perk6"));
		princeLore.add(Lang.getFor(player, "gui.ranks.prince_perk7"));
		princeLore.add("");
		princeLore.add(Lang.getFor(player, "gui.ranks.prince_req1"));
		princeLore.add(Lang.getFor(player, "gui.ranks.prince_req2"));
		princeLore.add(Lang.getFor(player, "gui.ranks.prince_perk8"));
		princeMeta.setLore(princeLore);
		prince.setItemMeta(princeMeta);

		// King
		ItemMeta kingMeta = king.getItemMeta();
		ArrayList<String> kingLore = new ArrayList<>();
		if (pronouns == Pronouns.MALE) {
			if (rank == 7) {
				kingMeta.setDisplayName(Lang.getFor(player, "gui.ranks.king") + Lang.getFor(player, "gui.ranks.current_suffix"));
			} else {
				kingMeta.setDisplayName(Lang.getFor(player, "gui.ranks.king"));
			}
		} else {
			if (rank == 7) {
				kingMeta.setDisplayName(Lang.getFor(player, "gui.ranks.queen") + Lang.getFor(player, "gui.ranks.current_suffix"));
			} else {
				kingMeta.setDisplayName(Lang.getFor(player, "gui.ranks.queen"));
			}
		}
		kingLore.add(Lang.getFor(player, "gui.ranks.bending"));
		kingLore.add(ChatUtils.translateToColor("&f&o- &4[&cF&4] &c&oFireComet, " + Element.LIGHTNING.getColor() + "&oBolt, LightningBurst,"));
		kingLore.add(ChatUtils.translateToColor(Element.LIGHTNING.getColor() + "       &oElectricStrike, JetBolt,"));
		kingLore.add(ChatUtils.translateToColor(Element.COMBUSTION.getColor() + "       &oBasic combustion abilities"));
		kingLore.add(ChatUtils.translateToColor("&f&o- &8[&7A&8] " + SoundAbility.SOUND.getColor() + "&oSonicBoom, DeafeningScream"));
		kingLore.add(ChatUtils.translateToColor("&f&o- &3[&bW&3] " + Element.BLOOD.getColor() + "&oBloodFreeze"));
		kingLore.add(ChatUtils.translateToColor("&f&o- &2[&aE&2] " + Element.LAVA.getColor() + "&oLavaFlux, MagmaWave,"));
		kingLore.add(ChatUtils.translateToColor(Element.LAVA.getColor() + "       &oMagmaGlaives, MoltenBlast"));
		kingLore.add(Lang.getFor(player, "gui.ranks.king_bending1"));
		kingLore.add(Lang.getFor(player, "gui.ranks.perks"));
		kingLore.add(Lang.getFor(player, "gui.ranks.king_perk1"));
		kingLore.add(Lang.getFor(player, "gui.ranks.king_perk2"));
		kingLore.add(Lang.getFor(player, "gui.ranks.king_perk3"));
		kingLore.add(Lang.getFor(player, "gui.ranks.king_perk4"));
		kingLore.add(Lang.getFor(player, "gui.ranks.king_perk5"));
		kingLore.add("");
		kingLore.add(Lang.getFor(player, "gui.ranks.king_req1"));
		kingLore.add(Lang.getFor(player, "gui.ranks.king_req2"));
		kingLore.add(Lang.getFor(player, "gui.ranks.king_perk6"));
		kingMeta.setLore(kingLore);
		king.setItemMeta(kingMeta);

		// Emperor
		ItemMeta emperorMeta = emperor.getItemMeta();
		ArrayList<String> emperorLore = new ArrayList<>();
		if (pronouns == Pronouns.MALE) {
			if (rank == 8) {
				emperorMeta.setDisplayName(Lang.getFor(player, "gui.ranks.emperor") + Lang.getFor(player, "gui.ranks.current_suffix"));
			} else {
				emperorMeta.setDisplayName(Lang.getFor(player, "gui.ranks.emperor"));
			}
		} else {
			if (rank == 8) {
				emperorMeta.setDisplayName(Lang.getFor(player, "gui.ranks.empress") + Lang.getFor(player, "gui.ranks.current_suffix"));
			} else {
				emperorMeta.setDisplayName(Lang.getFor(player, "gui.ranks.empress"));
			}
		}

		emperorLore.add(Lang.getFor(player, "gui.ranks.bending"));
		emperorLore.add(ChatUtils.translateToColor("&f&o- &4[&cF&4] " + Element.COMBUSTION.getColor() + "&oCombustion, CombustionStrike, Barrage"));
		emperorLore.add(ChatUtils.translateToColor("&f&o- &8[&7A&8] " + Element.FLIGHT.getColor() + "&oBasic flight abilities"));
		emperorLore.add(ChatUtils.translateToColor("&f&o- &3[&bW&3] " + Element.BLOOD.getColor() + "&oLifeRip, Disalignment"));
		emperorLore.add(ChatUtils.translateToColor("&f&o- &2[&aE&2] " + Element.LAVA.getColor() + "&oEruption, Fissure"));
		emperorLore.add(Lang.getFor(player, "gui.ranks.emperor_bending1"));
		emperorLore.add(Lang.getFor(player, "gui.ranks.perks"));
		emperorLore.add(Lang.getFor(player, "gui.ranks.emperor_perk1"));
		emperorLore.add(Lang.getFor(player, "gui.ranks.emperor_perk2"));
		emperorLore.add(Lang.getFor(player, "gui.ranks.emperor_perk3"));
		emperorLore.add(Lang.getFor(player, "gui.ranks.emperor_perk4"));
		emperorLore.add(Lang.getFor(player, "gui.ranks.emperor_perk5"));
		emperorLore.add("");
		emperorLore.add(Lang.getFor(player, "gui.ranks.emperor_req1"));
		emperorLore.add(Lang.getFor(player, "gui.ranks.emperor_req2"));
		emperorLore.add(Lang.getFor(player, "gui.ranks.emperor_perk6"));
		emperorMeta.setLore(emperorLore);
		emperor.setItemMeta(emperorMeta);

		// Initialize GUI
		// Line 1
		gui.setItem(0, yellowPane);
		gui.setItem(1, yellowPane);
		gui.setItem(2, blackPane);
		gui.setItem(3, blackPane);
		gui.setItem(4, peasant);
		gui.setItem(5, blackPane);
		gui.setItem(6, blackPane);
		gui.setItem(7, yellowPane);
		gui.setItem(8, yellowPane);
		// Line 2
		gui.setItem(9, blackPane);
		gui.setItem(10, yellowPane);
		gui.setItem(11, blackPane);
		gui.setItem(12, esquire);
		gui.setItem(13, blackPane);
		gui.setItem(14, knight);
		gui.setItem(15, blackPane);
		gui.setItem(16, yellowPane);
		gui.setItem(17, blackPane);
		// Line 3
		gui.setItem(18, blackPane);
		gui.setItem(19, yellowPane);
		gui.setItem(20, baron);
		gui.setItem(21, blackPane);
		gui.setItem(22, count);
		gui.setItem(23, blackPane);
		gui.setItem(24, duke);
		gui.setItem(25, yellowPane);
		gui.setItem(26, blackPane);
		// Line 4
		gui.setItem(27, blackPane);
		gui.setItem(28, yellowPane);
		gui.setItem(29, blackPane);
		gui.setItem(30, prince);
		gui.setItem(31, blackPane);
		gui.setItem(32, king);
		gui.setItem(33, blackPane);
		gui.setItem(34, yellowPane);
		gui.setItem(35, blackPane);
		// Line 5
		gui.setItem(36, yellowPane);
		gui.setItem(37, yellowPane);
		gui.setItem(38, blackPane);
		gui.setItem(39, blackPane);
		gui.setItem(40, emperor);
		gui.setItem(41, blackPane);
		gui.setItem(42, blackPane);
		gui.setItem(43, yellowPane);
		gui.setItem(44, yellowPane);

		return gui;
	}

}
