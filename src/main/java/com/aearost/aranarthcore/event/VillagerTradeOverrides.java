package com.aearost.aranarthcore.event;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Villager;
import org.bukkit.entity.Villager.Profession;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;

import com.aearost.aranarthcore.AranarthCore;
import com.google.common.collect.Lists;

public class VillagerTradeOverrides implements Listener {

	public VillagerTradeOverrides(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	/**
	 * Deals with overriding villager trade functionality to yield custom results.
	 * @param e The event.
	 */
	@EventHandler
	public void onVillagerTrade(final PlayerInteractAtEntityEvent e) {
		if (e.getRightClicked() instanceof Villager villager) {
            List<MerchantRecipe> trades = Lists.newArrayList(villager.getRecipes());
			for (int i = 0; i < trades.size(); i++) {
				MerchantRecipe trade = trades.get(i);
				
				if (villager.getProfession() == Profession.MASON) {
					// Novice trades
					if (trade.getResult().getType() == Material.BRICK) {
						MerchantRecipe newTrade = new MerchantRecipe(new ItemStack(selectTier1Stone(), 32), trade.getMaxUses());
						newTrade.addIngredient(new ItemStack(Material.EMERALD, 2));
						newTrade.setExperienceReward(true);
						newTrade.setVillagerExperience(trade.getVillagerExperience());
						trades.set(i, newTrade);
					}
					// Journeyman trades
					else if (trade.getResult().getType() == Material.POLISHED_ANDESITE) {
						MerchantRecipe newTrade = new MerchantRecipe(new ItemStack(selectTier1Stone(), 32), trade.getMaxUses());
						newTrade.addIngredient(new ItemStack(Material.EMERALD, 2));
						newTrade.setExperienceReward(true);
						newTrade.setVillagerExperience(trade.getVillagerExperience());
						trades.set(i, newTrade);
					} else if (trade.getResult().getType() == Material.POLISHED_GRANITE) {
						MerchantRecipe newTrade = new MerchantRecipe(new ItemStack(selectTier1Stone(), 32), trade.getMaxUses());
						newTrade.addIngredient(new ItemStack(Material.EMERALD, 1));
						newTrade.setExperienceReward(true);
						newTrade.setVillagerExperience(trade.getVillagerExperience());
						trades.set(i, newTrade);
					} else if (trade.getResult().getType() == Material.POLISHED_DIORITE) {
						MerchantRecipe newTrade = new MerchantRecipe(new ItemStack(selectTier2Stone(), 32), trade.getMaxUses());
						newTrade.addIngredient(new ItemStack(Material.EMERALD, 4));
						newTrade.setExperienceReward(true);
						newTrade.setVillagerExperience(trade.getVillagerExperience());
						trades.set(i, newTrade);
					} else if (trade.getResult().getType() == Material.DRIPSTONE_BLOCK) {
						MerchantRecipe newTrade = new MerchantRecipe(new ItemStack(selectTier3Stone(), 16), trade.getMaxUses());
						newTrade.addIngredient(new ItemStack(Material.EMERALD, 6));
						newTrade.setExperienceReward(true);
						newTrade.setVillagerExperience(trade.getVillagerExperience());
						trades.set(i, newTrade);
					}
					// Expert trades
					else if (terracottaList().contains(trade.getResult().getType())) {
						MerchantRecipe newTrade = new MerchantRecipe(new ItemStack(selectTier3Stone(), 32), trade.getMaxUses());
						newTrade.addIngredient(new ItemStack(Material.EMERALD, 4));
						newTrade.setExperienceReward(true);
						newTrade.setVillagerExperience(trade.getVillagerExperience());
						trades.set(i, newTrade);
					} else if (trade.getResult().getType() == Material.QUARTZ_BLOCK) {
						MerchantRecipe newTrade = new MerchantRecipe(new ItemStack(Material.QUARTZ_BLOCK, 32), trade.getMaxUses());
						newTrade.addIngredient(new ItemStack(Material.EMERALD, 8));
						newTrade.setExperienceReward(true);
						newTrade.setVillagerExperience(trade.getVillagerExperience());
						trades.set(i, newTrade);
					} else if (trade.getResult().getType() == Material.QUARTZ_PILLAR) {
						MerchantRecipe newTrade = new MerchantRecipe(new ItemStack(Material.QUARTZ_PILLAR, 32), trade.getMaxUses());
						newTrade.addIngredient(new ItemStack(Material.EMERALD, 8));
						newTrade.setExperienceReward(true);
						newTrade.setVillagerExperience(trade.getVillagerExperience());
						trades.set(i, newTrade);
					}
				}
			}
			villager.setRecipes(trades);
		}
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
		int i = r.nextInt(3);
		Material[] stones = { Material.DEEPSLATE, Material.COBBLED_DEEPSLATE, Material.DRIPSTONE_BLOCK };
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
