package com.aearost.aranarthcore;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.plugin.java.JavaPlugin;

import com.aearost.aranarthcore.commands.CommandAC;
import com.aearost.aranarthcore.commands.CommandACCompleter;
import com.aearost.aranarthcore.event.ArenaBlockBreak;
import com.aearost.aranarthcore.event.ArenaDurabilityPrevent;
import com.aearost.aranarthcore.event.ArenaGrowPrevent;
import com.aearost.aranarthcore.event.ArenaHungerLossPrevent;
import com.aearost.aranarthcore.event.ArenaInventoryItemDropPrevent;
import com.aearost.aranarthcore.event.ArenaItemDrops;
import com.aearost.aranarthcore.event.ArenaMeltPrevent;
import com.aearost.aranarthcore.event.ArenaPlayerDeath;
import com.aearost.aranarthcore.event.BuddingAmethystBreak;
import com.aearost.aranarthcore.event.ConcretePowderGravityPrevent;
import com.aearost.aranarthcore.event.CoralDry;
import com.aearost.aranarthcore.event.CraftingOverrides;
import com.aearost.aranarthcore.event.CreeperExplodeDeny;
import com.aearost.aranarthcore.event.CropHarvest;
import com.aearost.aranarthcore.event.EndermanPickupCancel;
import com.aearost.aranarthcore.event.EntityEggPickupCancel;
import com.aearost.aranarthcore.event.GuiTeleportClick;
import com.aearost.aranarthcore.event.HomePadBreak;
import com.aearost.aranarthcore.event.HomePadPlace;
import com.aearost.aranarthcore.event.HomePadStep;
import com.aearost.aranarthcore.event.ItemPickupAddToShulker;
import com.aearost.aranarthcore.event.LogWoodStripPrevent;
import com.aearost.aranarthcore.event.MobDestroyDoorPrevent;
import com.aearost.aranarthcore.event.MountSpawn;
import com.aearost.aranarthcore.event.MountSwim;
import com.aearost.aranarthcore.event.PillagerOutpostSpawnCancel;
import com.aearost.aranarthcore.event.PitcherPlantBreak;
import com.aearost.aranarthcore.event.PitcherPlantPlace;
import com.aearost.aranarthcore.event.PlayerChat;
import com.aearost.aranarthcore.event.PlayerServerJoin;
import com.aearost.aranarthcore.event.PlayerServerLeave;
import com.aearost.aranarthcore.event.RespawnCancel;
import com.aearost.aranarthcore.event.SoilTrampleCancel;
import com.aearost.aranarthcore.event.SugarcaneBlockPlace;
import com.aearost.aranarthcore.event.TorchflowerBreak;
import com.aearost.aranarthcore.event.TorchflowerGrow;
import com.aearost.aranarthcore.event.TorchflowerPlace;
import com.aearost.aranarthcore.event.VillagerTradeOverrides;
import com.aearost.aranarthcore.event.ZombieHorseSpawn;
import com.aearost.aranarthcore.recipes.RecipeAmethystUncraft;
import com.aearost.aranarthcore.recipes.RecipeBambooBlockUncraft;
import com.aearost.aranarthcore.recipes.RecipeBambooPlanks;
import com.aearost.aranarthcore.recipes.RecipeBell;
import com.aearost.aranarthcore.recipes.RecipeBundle;
import com.aearost.aranarthcore.recipes.RecipeCalcite;
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
import com.aearost.aranarthcore.recipes.RecipeDripstone;
import com.aearost.aranarthcore.recipes.RecipeGildedBlackstone;
import com.aearost.aranarthcore.recipes.RecipeGlowInkSac;
import com.aearost.aranarthcore.recipes.RecipeHomePad;
import com.aearost.aranarthcore.recipes.RecipeHorseArmourDiamond;
import com.aearost.aranarthcore.recipes.RecipeHorseArmourGolden;
import com.aearost.aranarthcore.recipes.RecipeHorseArmourIron;
import com.aearost.aranarthcore.recipes.RecipeNametag;
import com.aearost.aranarthcore.recipes.RecipeRootedDirt;
import com.aearost.aranarthcore.recipes.RecipeSaddleA;
import com.aearost.aranarthcore.recipes.RecipeSaddleB;
import com.aearost.aranarthcore.recipes.RecipeSugarcaneBlockCraft;
import com.aearost.aranarthcore.recipes.RecipeSugarcaneBlockUncraft;
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
		initializeWorlds();

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
		new HomePadBreak(this);
		new PlayerServerJoin(this);
		new PlayerServerLeave(this);
		new GuiTeleportClick(this);
		new CreeperExplodeDeny(this);
		new SoilTrampleCancel(this);
		new CraftingOverrides(this);
		new LogWoodStripPrevent(this);
		new PlayerChat(this);
		new EndermanPickupCancel(this);
		new EntityEggPickupCancel(this);
		new BuddingAmethystBreak(this);
		new MountSpawn(this);
		new MountSwim(this);
		new CropHarvest(this);
		new RespawnCancel(this);
		new SugarcaneBlockPlace(this);
		new TorchflowerPlace(this);
		new TorchflowerBreak(this);
		new TorchflowerGrow(this);
		new PitcherPlantPlace(this);
		new PitcherPlantBreak(this);
		new PillagerOutpostSpawnCancel(this);
		new ItemPickupAddToShulker(this);
		new ZombieHorseSpawn(this);
		new ArenaBlockBreak(this);
		new ArenaItemDrops(this);
		new ArenaMeltPrevent(this);
		new ArenaGrowPrevent(this);
		new ArenaInventoryItemDropPrevent(this);
		new ArenaPlayerDeath(this);
		new ArenaDurabilityPrevent(this);
		new ArenaHungerLossPrevent(this);
		new ConcretePowderGravityPrevent(this);
		new VillagerTradeOverrides(this);
		new CoralDry(this);
		new MobDestroyDoorPrevent(this);
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
		new RecipeCalcite(this);
		new RecipeBundle(this);
		new RecipeDripstone(this);
		new RecipeSugarcaneBlockCraft(this);
		new RecipeSugarcaneBlockUncraft(this);
		new RecipeBambooBlockUncraft(this);
		new RecipeBambooPlanks(this);
		new RecipeGildedBlackstone(this);
		new RecipeRootedDirt(this);
	}

	private void initializeCommands() {
		getCommand("ac").setExecutor(new CommandAC());
		getCommand("ac").setTabCompleter(new CommandACCompleter());
	}
	
	private void initializeWorlds() {
		// Loads the world if it isn't yet loaded
		if (Bukkit.getWorld("arena") == null) {
			WorldCreator wc = new WorldCreator("arena");
			wc.environment(World.Environment.NORMAL);
			wc.type(WorldType.FLAT);
			wc.createWorld();
		}
		
		if (Bukkit.getWorld("creative") == null) {
			WorldCreator wc = new WorldCreator("creative");
			wc.environment(World.Environment.NORMAL);
			wc.type(WorldType.FLAT);
			wc.createWorld();
		}
	}

	@Override
	public void onDisable() {
		PersistenceUtils.saveHomes();
		PersistenceUtils.saveAranarthPlayers();
	}

}