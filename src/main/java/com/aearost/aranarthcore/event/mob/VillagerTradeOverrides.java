package com.aearost.aranarthcore.event.mob;

import com.aearost.aranarthcore.enums.Month;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.google.common.collect.Lists;
import org.bukkit.Material;
import org.bukkit.entity.Villager;
import org.bukkit.entity.Villager.Profession;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Deals with overriding villager trade functionality to yield custom results.
 */
public class VillagerTradeOverrides {
	public void execute(PlayerInteractEntityEvent e) {
		Villager villager = (Villager) e.getRightClicked();
		List<MerchantRecipe> trades = Lists.newArrayList(villager.getRecipes());
		for (int i = 0; i < trades.size(); i++) {
			MerchantRecipe trade = trades.get(i);
			MerchantRecipe newTrade = null;
			if (villager.getProfession() == Profession.MASON) {
				// Novice trades
				if (trade.getResult().getType() == Material.BRICK) {
					newTrade = new MerchantRecipe(new ItemStack(selectTier1Stone(), 32), trade.getMaxUses());
					newTrade.addIngredient(new ItemStack(Material.EMERALD, 2));
				}
				// Journeyman trades
				else if (trade.getResult().getType() == Material.POLISHED_ANDESITE) {
					newTrade = new MerchantRecipe(new ItemStack(selectTier1Stone(), 32), trade.getMaxUses());
					newTrade.addIngredient(new ItemStack(Material.EMERALD, 2));
				} else if (trade.getResult().getType() == Material.POLISHED_GRANITE) {
					newTrade = new MerchantRecipe(new ItemStack(selectTier1Stone(), 32), trade.getMaxUses());
					newTrade.addIngredient(new ItemStack(Material.EMERALD, 1));
				} else if (trade.getResult().getType() == Material.POLISHED_DIORITE) {
					newTrade = new MerchantRecipe(new ItemStack(selectTier2Stone(), 32), trade.getMaxUses());
					newTrade.addIngredient(new ItemStack(Material.EMERALD, 4));
				} else if (trade.getResult().getType() == Material.DRIPSTONE_BLOCK) {
					newTrade = new MerchantRecipe(new ItemStack(selectTier3Stone(), 16), trade.getMaxUses());
					newTrade.addIngredient(new ItemStack(Material.EMERALD, 6));
				}
				// Expert trades
				else if (terracottaList().contains(trade.getResult().getType())) {
					newTrade = new MerchantRecipe(new ItemStack(selectTier3Stone(), 32), trade.getMaxUses());
					newTrade.addIngredient(new ItemStack(Material.EMERALD, 4));
				} else if (trade.getResult().getType() == Material.QUARTZ_BLOCK) {
					newTrade = new MerchantRecipe(new ItemStack(Material.QUARTZ_BLOCK, 32), trade.getMaxUses());
					newTrade.addIngredient(new ItemStack(Material.EMERALD, 8));
				} else if (trade.getResult().getType() == Material.QUARTZ_PILLAR) {
					newTrade = new MerchantRecipe(new ItemStack(Material.QUARTZ_PILLAR, 32), trade.getMaxUses());
					newTrade.addIngredient(new ItemStack(Material.EMERALD, 8));
				}
			} else if (villager.getProfession() == Profession.FARMER) {
				if (AranarthUtils.getMonth() == Month.FRUCTIVOR) {
					// All trades to yield 3 emeralds instead of 1
					if (trade.getIngredients().get(0).getType() == Material.WHEAT || trade.getIngredients().get(0).getType() == Material.POTATO
							|| trade.getIngredients().get(0).getType() == Material.CARROT || trade.getIngredients().get(0).getType() == Material.BEETROOT
							|| trade.getIngredients().get(0).getType() == Material.PUMPKIN || trade.getIngredients().get(0).getType() == Material.MELON) {
						newTrade = new MerchantRecipe(new ItemStack(trade.getResult().getType(), 3), trade.getMaxUses());
						newTrade.addIngredient(trade.getIngredients().get(0));
					}
				}
			}
			if (newTrade != null) {
				newTrade.setExperienceReward(true);
				newTrade.setVillagerExperience(trade.getVillagerExperience());
				newTrade.setUses(trade.getUses());
				newTrade.setMaxUses(trade.getMaxUses());
				newTrade.setSpecialPrice(newTrade.getSpecialPrice());
				newTrade.setDemand(newTrade.getDemand());
				trades.set(i, newTrade);
			}
		}
		villager.setRecipes(trades);
	}
	
	private Material selectTier1Stone() {
		Random r = new Random();
		int i = r.nextInt(7);
		Material[] stones = { Material.STONE, Material.COBBLESTONE, Material.GRANITE, Material.DIORITE,
				Material.ANDESITE, Material.SANDSTONE, Material.RED_SANDSTONE };
		return stones[i];
	}
	
	private Material selectTier2Stone() {
		Random r = new Random();
		int i = r.nextInt(4);
		Material[] stones = { Material.DEEPSLATE, Material.COBBLED_DEEPSLATE, Material.DRIPSTONE_BLOCK, Material.TUFF };
		return stones[i];
	}
	
	private Material selectTier3Stone() {
		Random r = new Random();
		int i = r.nextInt(4);
		Material[] stones = { Material.BASALT, Material.BLACKSTONE, Material.DRIPSTONE_BLOCK, Material.END_STONE };
		return stones[i];
	}
	
	private List<Material> terracottaList() {
		List<Material> terracottaList = new ArrayList<>();
		terracottaList.add(Material.BLACK_GLAZED_TERRACOTTA);
		terracottaList.add(Material.BLUE_GLAZED_TERRACOTTA);
		terracottaList.add(Material.BROWN_GLAZED_TERRACOTTA);
		terracottaList.add(Material.CYAN_GLAZED_TERRACOTTA);
		terracottaList.add(Material.GRAY_GLAZED_TERRACOTTA);
		terracottaList.add(Material.GREEN_GLAZED_TERRACOTTA);
		terracottaList.add(Material.LIGHT_BLUE_GLAZED_TERRACOTTA);
		terracottaList.add(Material.LIGHT_GRAY_GLAZED_TERRACOTTA);
		terracottaList.add(Material.LIME_GLAZED_TERRACOTTA);
		terracottaList.add(Material.MAGENTA_GLAZED_TERRACOTTA);
		terracottaList.add(Material.ORANGE_GLAZED_TERRACOTTA);
		terracottaList.add(Material.PINK_GLAZED_TERRACOTTA);
		terracottaList.add(Material.PURPLE_GLAZED_TERRACOTTA);
		terracottaList.add(Material.RED_GLAZED_TERRACOTTA);
		terracottaList.add(Material.WHITE_GLAZED_TERRACOTTA);
		terracottaList.add(Material.YELLOW_GLAZED_TERRACOTTA);
		return terracottaList;
	}
}
