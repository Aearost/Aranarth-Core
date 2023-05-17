package com.aearost.aranarthcore;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import com.aearost.aranarthcore.commands.CommandAC;
import com.aearost.aranarthcore.commands.CommandACCompleter;
import com.aearost.aranarthcore.commands.CommandHomePad;
import com.aearost.aranarthcore.commands.CommandHomePadCompleter;
import com.aearost.aranarthcore.commands.CommandNickname;
import com.aearost.aranarthcore.event.BuddingAmethystDestroy;
import com.aearost.aranarthcore.event.CraftingOverrides;
import com.aearost.aranarthcore.event.CreeperExplodeDeny;
import com.aearost.aranarthcore.event.CropHarvest;
import com.aearost.aranarthcore.event.EndermanPickupCancel;
import com.aearost.aranarthcore.event.EntityEggPickupCancel;
import com.aearost.aranarthcore.event.GuiClick;
import com.aearost.aranarthcore.event.HomePadDestroy;
import com.aearost.aranarthcore.event.HomePadPlace;
import com.aearost.aranarthcore.event.HomePadStep;
import com.aearost.aranarthcore.event.HorseSpawn;
import com.aearost.aranarthcore.event.HorseSwim;
import com.aearost.aranarthcore.event.LogStrip;
import com.aearost.aranarthcore.event.PlayerChat;
import com.aearost.aranarthcore.event.PlayerJoinServer;
import com.aearost.aranarthcore.event.SoilTrampleCancel;
import com.aearost.aranarthcore.recipes.RecipeAmethystUncraft;
import com.aearost.aranarthcore.recipes.RecipeBell;
import com.aearost.aranarthcore.recipes.RecipeChainmailBootsA;
import com.aearost.aranarthcore.recipes.RecipeChainmailBootsB;
import com.aearost.aranarthcore.recipes.RecipeChainmailChestplate;
import com.aearost.aranarthcore.recipes.RecipeChainmailHelmetA;
import com.aearost.aranarthcore.recipes.RecipeChainmailHelmetB;
import com.aearost.aranarthcore.recipes.RecipeChainmailLeggings;
import com.aearost.aranarthcore.recipes.RecipeCharcoalToCoal;
import com.aearost.aranarthcore.recipes.RecipeChorusDiamond;
import com.aearost.aranarthcore.recipes.RecipeCobweb;
import com.aearost.aranarthcore.recipes.RecipeDeepslateA;
import com.aearost.aranarthcore.recipes.RecipeDeepslateB;
import com.aearost.aranarthcore.recipes.RecipeGlowInkSac;
import com.aearost.aranarthcore.recipes.RecipeHomePad;
import com.aearost.aranarthcore.recipes.RecipeHorseArmourDiamond;
import com.aearost.aranarthcore.recipes.RecipeHorseArmourGolden;
import com.aearost.aranarthcore.recipes.RecipeHorseArmourIron;
import com.aearost.aranarthcore.recipes.RecipeNametag;
import com.aearost.aranarthcore.recipes.RecipeSaddleA;
import com.aearost.aranarthcore.recipes.RecipeSaddleB;
import com.aearost.aranarthcore.recipes.RecipeWoolToString;
import com.aearost.aranarthcore.utils.ItemUtils;
import com.aearost.aranarthcore.utils.PersistenceUtils;

public class AranarthCore extends JavaPlugin {

	@Override
	public void onEnable() {

		initializeUtils();
		initializeEvents();
		initializeRecipes();
		initializeCommands();

		// Update the files every 30 minutes to protect from loss of data
		Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
			@Override
			public void run() {
				PersistenceUtils.saveHomes();
				PersistenceUtils.saveAranarthPlayers();
				Bukkit.getLogger().info("Homes and aranarth players have been saved");
			}
		}, 36000, 36000);

	}

	


	private void initializeUtils() {
		PersistenceUtils.loadHomes();
		PersistenceUtils.loadAranarthPlayers();
		new ItemUtils();
	}

	private void initializeEvents() {
		new HomePadStep(this);
		new HomePadPlace(this);
		new HomePadDestroy(this);
		new PlayerJoinServer(this);
		new GuiClick(this);
		new CreeperExplodeDeny(this);
		new SoilTrampleCancel(this);
		new CraftingOverrides(this);
		new LogStrip(this);
		new PlayerChat(this);
		new EndermanPickupCancel(this);
		new EntityEggPickupCancel(this);
		new BuddingAmethystDestroy(this);
		new HorseSpawn(this);
		new HorseSwim(this);
		new CropHarvest(this);
	}

	private void initializeRecipes() {
		new RecipeHomePad(this);
		new RecipeChorusDiamond(this);
		new RecipeSaddleA(this);
		new RecipeSaddleB(this);
		new RecipeAmethystUncraft(this);
		new RecipeDeepslateA(this);
		new RecipeDeepslateB(this);
		new RecipeHorseArmourIron(this);
		new RecipeHorseArmourGolden(this);
		new RecipeHorseArmourDiamond(this);
		new RecipeNametag(this);
		new RecipeCharcoalToCoal(this);
		new RecipeBell(this);
		new RecipeGlowInkSac(this);
		new RecipeChainmailHelmetA(this);
		new RecipeChainmailHelmetB(this);
		new RecipeChainmailChestplate(this);
		new RecipeChainmailLeggings(this);
		new RecipeChainmailBootsA(this);
		new RecipeChainmailBootsB(this);
		new RecipeCobweb(this);
		new RecipeWoolToString(this);
	}

	private void initializeCommands() {
		getCommand("ac").setExecutor(new CommandAC());
		getCommand("ac").setTabCompleter(new CommandACCompleter());
		getCommand("homepad").setExecutor(new CommandHomePad());
		getCommand("homepad").setTabCompleter(new CommandHomePadCompleter());
		getCommand("nickname").setExecutor(new CommandNickname());
	}

	@Override
	public void onDisable() {
		PersistenceUtils.saveHomes();
		PersistenceUtils.saveAranarthPlayers();
	}

}