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
import com.aearost.aranarthcore.event.ArrowConsume;
import com.aearost.aranarthcore.event.AxolotlPreventFishDamage;
import com.aearost.aranarthcore.event.BewitchedMinecartPlace;
import com.aearost.aranarthcore.event.BewitchedMoveSpeedTest;
import com.aearost.aranarthcore.event.BlacklistItemPickupPrevent;
import com.aearost.aranarthcore.event.BuddingAmethystBreak;
import com.aearost.aranarthcore.event.ConcretePowderGravityPrevent;
import com.aearost.aranarthcore.event.CoralDry;
import com.aearost.aranarthcore.event.CraftingOverrides;
import com.aearost.aranarthcore.event.CreativeExpChangePrevent;
import com.aearost.aranarthcore.event.CreeperExplodeDeny;
import com.aearost.aranarthcore.event.CropHarvest;
import com.aearost.aranarthcore.event.DragonHeadClick;
import com.aearost.aranarthcore.event.DurabilityDecreaseWarning;
import com.aearost.aranarthcore.event.EndermanPickupCancel;
import com.aearost.aranarthcore.event.EntityEggPickupCancel;
import com.aearost.aranarthcore.event.GuiBlacklistClick;
import com.aearost.aranarthcore.event.GuiHomepadClick;
import com.aearost.aranarthcore.event.GuiPotionClose;
import com.aearost.aranarthcore.event.GuiPotionPreventNonPotionAdd;
import com.aearost.aranarthcore.event.GuiQuiverClose;
import com.aearost.aranarthcore.event.GuiQuiverPreventNonArrowAdd;
import com.aearost.aranarthcore.event.GuiVillagerClick;
import com.aearost.aranarthcore.event.HomePadBreak;
import com.aearost.aranarthcore.event.HomePadPlace;
import com.aearost.aranarthcore.event.HomePadStep;
import com.aearost.aranarthcore.event.HoneyGlazedHamEat;
import com.aearost.aranarthcore.event.InvisibleItemFrameInteract;
import com.aearost.aranarthcore.event.ItemPickupAddToShulker;
import com.aearost.aranarthcore.event.LogWoodStripPrevent;
import com.aearost.aranarthcore.event.MangroveRootShear;
import com.aearost.aranarthcore.event.MobDestroyDoorPrevent;
import com.aearost.aranarthcore.event.MountSpawn;
import com.aearost.aranarthcore.event.MountSwim;
import com.aearost.aranarthcore.event.ParrotJumpCancelDismount;
import com.aearost.aranarthcore.event.PetHurtPrevent;
import com.aearost.aranarthcore.event.PillagerOutpostSpawnCancel;
import com.aearost.aranarthcore.event.PitcherPlantBreak;
import com.aearost.aranarthcore.event.PitcherPlantPlace;
import com.aearost.aranarthcore.event.PlayerChat;
import com.aearost.aranarthcore.event.PlayerServerJoin;
import com.aearost.aranarthcore.event.PlayerServerQuit;
import com.aearost.aranarthcore.event.PlayerTeleportBetweenWorlds;
import com.aearost.aranarthcore.event.PotionConsume;
import com.aearost.aranarthcore.event.PotionEffectStack;
import com.aearost.aranarthcore.event.QuiverClick;
import com.aearost.aranarthcore.event.QuiverPreventAddToBundle;
import com.aearost.aranarthcore.event.RespawnCancel;
import com.aearost.aranarthcore.event.SoilTrampleCancel;
import com.aearost.aranarthcore.event.SugarcaneBlockPlace;
import com.aearost.aranarthcore.event.TorchflowerBreak;
import com.aearost.aranarthcore.event.TorchflowerGrow;
import com.aearost.aranarthcore.event.TorchflowerPlace;
import com.aearost.aranarthcore.event.VillagerCamelDismount;
import com.aearost.aranarthcore.event.VillagerCamelPickup;
import com.aearost.aranarthcore.event.VillagerInventoryViewClick;
import com.aearost.aranarthcore.event.VillagerTradeOverrides;
import com.aearost.aranarthcore.event.WanderingTraderSpawnAnnounce;
import com.aearost.aranarthcore.event.ZombieHorseSpawn;
import com.aearost.aranarthcore.items.InvisibleItemFrame;
import com.aearost.aranarthcore.recipes.RecipeAmethystUncraft;
import com.aearost.aranarthcore.recipes.RecipeBambooBlockUncraft;
import com.aearost.aranarthcore.recipes.RecipeBambooPlanks;
import com.aearost.aranarthcore.recipes.RecipeBell;
import com.aearost.aranarthcore.recipes.RecipeBewitchedMinecart;
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
import com.aearost.aranarthcore.recipes.RecipeDiamondOre;
import com.aearost.aranarthcore.recipes.RecipeDripstone;
import com.aearost.aranarthcore.recipes.RecipeGildedBlackstone;
import com.aearost.aranarthcore.recipes.RecipeGlowInkSac;
import com.aearost.aranarthcore.recipes.RecipeHomePad;
import com.aearost.aranarthcore.recipes.RecipeHoneyGlazedHam;
import com.aearost.aranarthcore.recipes.RecipeHorseArmourDiamond;
import com.aearost.aranarthcore.recipes.RecipeHorseArmourGolden;
import com.aearost.aranarthcore.recipes.RecipeHorseArmourIron;
import com.aearost.aranarthcore.recipes.RecipeInvisibleItemFrame;
import com.aearost.aranarthcore.recipes.RecipeLodestone;
import com.aearost.aranarthcore.recipes.RecipeMushroomBlockBrown;
import com.aearost.aranarthcore.recipes.RecipeMushroomBlockBrownUncraft;
import com.aearost.aranarthcore.recipes.RecipeMushroomBlockRed;
import com.aearost.aranarthcore.recipes.RecipeMushroomBlockRedUncraft;
import com.aearost.aranarthcore.recipes.RecipeMushroomStem;
import com.aearost.aranarthcore.recipes.RecipeNametag;
import com.aearost.aranarthcore.recipes.RecipeQuiver;
import com.aearost.aranarthcore.recipes.RecipeRootedDirt;
import com.aearost.aranarthcore.recipes.RecipeSaddleA;
import com.aearost.aranarthcore.recipes.RecipeSaddleB;
import com.aearost.aranarthcore.recipes.RecipeSugarcaneBlockCraft;
import com.aearost.aranarthcore.recipes.RecipeSugarcaneBlockUncraft;
import com.aearost.aranarthcore.recipes.RecipeTuffA;
import com.aearost.aranarthcore.recipes.RecipeTuffB;
import com.aearost.aranarthcore.recipes.RecipeWoolToString;
import com.aearost.aranarthcore.utils.AranarthUtils;
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
		initializeItems();

		// Update the files every 30 minutes to protect from loss of data
		Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
			@Override
			public void run() {
				PersistenceUtils.saveHomes();
				PersistenceUtils.saveAranarthPlayers();
				Bukkit.getLogger().info("Homes and aranarth players have been saved");
			}
		}, 36000, 36000);
		
		// Check every 5 seconds to update armour trim effects
		Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
			@Override
			public void run() {
				AranarthUtils.updateArmourTrimEffects();
			}
		}, 0, 100);
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
		new PlayerServerQuit(this);
		new GuiHomepadClick(this);
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
		new PlayerTeleportBetweenWorlds(this);
		new CreativeExpChangePrevent(this);
		new GuiBlacklistClick(this);
		new BlacklistItemPickupPrevent(this);
		new GuiVillagerClick(this);
		new VillagerInventoryViewClick(this);
		new VillagerCamelPickup(this);
		new VillagerCamelDismount(this);
		new DurabilityDecreaseWarning(this);
		new ParrotJumpCancelDismount(this);
		new PetHurtPrevent(this);
		new InvisibleItemFrameInteract(this);
		new HoneyGlazedHamEat(this);
		new GuiPotionClose(this);
		new GuiPotionPreventNonPotionAdd(this);
		new DragonHeadClick(this);
		new PotionConsume(this);
		new QuiverPreventAddToBundle(this);
		new QuiverClick(this);
		new GuiQuiverClose(this);
		new GuiQuiverPreventNonArrowAdd(this);
		new ArrowConsume(this);
		new BewitchedMinecartPlace(this);
		new WanderingTraderSpawnAnnounce(this);
		new AxolotlPreventFishDamage(this);
		new BewitchedMoveSpeedTest(this);
		new PotionEffectStack(this);
		new MangroveRootShear(this);
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
		new RecipeTuffA(this);
		new RecipeTuffB(this);
		new RecipeDiamondOre(this);
		new RecipeLodestone(this);
		new RecipeInvisibleItemFrame(this);
		new RecipeHoneyGlazedHam(this);
		new RecipeQuiver(this);
		new RecipeBewitchedMinecart(this);
		new RecipeMushroomBlockBrown(this);
		new RecipeMushroomBlockBrownUncraft(this);
		new RecipeMushroomBlockRed(this);
		new RecipeMushroomBlockRedUncraft(this);
		new RecipeMushroomStem(this);
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

	private void initializeItems() {
		new InvisibleItemFrame(this);
		
	}
	
	@Override
	public void onDisable() {
		PersistenceUtils.saveHomes();
		PersistenceUtils.saveAranarthPlayers();
	}

}