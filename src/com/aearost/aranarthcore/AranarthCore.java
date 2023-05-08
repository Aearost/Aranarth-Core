package com.aearost.aranarthcore;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import com.aearost.aranarthcore.commands.CommandHomePad;
import com.aearost.aranarthcore.commands.CommandHomePadCompleter;
import com.aearost.aranarthcore.commands.CommandNickname;
import com.aearost.aranarthcore.commands.CommandPing;
import com.aearost.aranarthcore.commands.CommandPrefix;
import com.aearost.aranarthcore.event.BuddingAmethystDestroy;
import com.aearost.aranarthcore.event.CraftingOverrides;
import com.aearost.aranarthcore.event.CreeperExplodeDeny;
import com.aearost.aranarthcore.event.EndermanPickupCancel;
import com.aearost.aranarthcore.event.EntityEggPickupCancel;
import com.aearost.aranarthcore.event.GuiClick;
import com.aearost.aranarthcore.event.HomePadDestroy;
import com.aearost.aranarthcore.event.HomePadPlace;
import com.aearost.aranarthcore.event.HomePadStep;
import com.aearost.aranarthcore.event.HorseSpawn;
import com.aearost.aranarthcore.event.LogStrip;
import com.aearost.aranarthcore.event.PlayerChat;
import com.aearost.aranarthcore.event.PlayerJoinServer;
import com.aearost.aranarthcore.event.SoilTrampleCancel;
import com.aearost.aranarthcore.recipes.RecipeAmethystUncraft;
import com.aearost.aranarthcore.recipes.RecipeCharcoalToCoal;
import com.aearost.aranarthcore.recipes.RecipeChorusDiamond;
import com.aearost.aranarthcore.recipes.RecipeDeepslateA;
import com.aearost.aranarthcore.recipes.RecipeDeepslateB;
import com.aearost.aranarthcore.recipes.RecipeHomePad;
import com.aearost.aranarthcore.recipes.RecipeHorseArmourDiamond;
import com.aearost.aranarthcore.recipes.RecipeHorseArmourGolden;
import com.aearost.aranarthcore.recipes.RecipeHorseArmourIron;
import com.aearost.aranarthcore.recipes.RecipeNametag;
import com.aearost.aranarthcore.recipes.RecipeSaddleA;
import com.aearost.aranarthcore.recipes.RecipeSaddleB;
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
	}
	
	private void initializeCommands() {
		getCommand("homepad").setExecutor(new CommandHomePad());
		getCommand("homepad").setTabCompleter(new CommandHomePadCompleter());
		getCommand("ping").setExecutor(new CommandPing());
		getCommand("nickname").setExecutor(new CommandNickname());
		getCommand("prefix").setExecutor(new CommandPrefix());
	}
	
	@Override
	public void onDisable() {
		PersistenceUtils.saveHomes();
		PersistenceUtils.saveAranarthPlayers();
	}

}