package com.aearost.aranarthcore;

import com.aearost.aranarthcore.event.block.*;
import com.aearost.aranarthcore.event.misc.PotionEffectStack;
import com.aearost.aranarthcore.event.mob.*;
import com.aearost.aranarthcore.event.player.*;
import com.aearost.aranarthcore.event.world.*;
import com.aearost.aranarthcore.recipes.*;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.plugin.java.JavaPlugin;

import com.aearost.aranarthcore.commands.CommandAC;
import com.aearost.aranarthcore.commands.CommandACCompleter;
import com.aearost.aranarthcore.items.InvisibleItemFrame;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.PersistenceUtils;

public class AranarthCore extends JavaPlugin {

	private static AranarthCore plugin;

	/**
	 * Called when the plugin is first enabled on server startup.
	 * Responsible for initializing all functionality of AranarthCore.
	 */
	@Override
	public void onEnable() {
		initializeUtils();
		initializeEvents();
		initializeRecipes();
		initializeCommands();
		initializeWorlds();
		initializeItems();

		plugin = this;

		// Update the persistence files every 30 minutes to protect from loss of data
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
				AranarthUtils.updateArmorTrimEffects();
			}
		}, 0, 100);
	}

	public static AranarthCore getInstance() {
		return plugin;
	}

	/**
	 * Initializes necessary Utilities functionality needed on server startup.
	 */
	private void initializeUtils() {
		PersistenceUtils.loadHomes();
		PersistenceUtils.loadAranarthPlayers();
	}

	/**
	 * Initializes all AranarthCore events.
	 */
	private void initializeEvents() {
		new HomePadStep(this);
		new HomePadPlace(this);
		new HomePadBreak(this);
		new PlayerServerJoin(this);
		new PlayerServerQuit(this);
		new GuiHomepadClick(this);
		new ExplosionPrevent(this);
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
		new ShulkerItemPickup(this);
		new ZombieHorseSpawn(this);
		new ArenaBlockBreak(this);
		new ArenaItemDrops(this);
		new ArenaMeltPrevent(this);
		new ArenaGrowPrevent(this);
		new ArenaDurabilityPrevent(this);
		new ArenaHungerLossPrevent(this);
		new ConcretePowderGravityPrevent(this);
		new VillagerTradeOverrides(this);
		new CoralDry(this);
		new MobDestroyDoorPrevent(this);
		new PlayerTeleportBetweenWorlds(this);
		new ExpGainPrevent(this);
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
		new WanderingTraderSpawnAnnounce(this);
		new PotionEffectStack(this);
		new MangroveRootShear(this);
		new ChestSort(this);
		new GoatDeath(this);
		new DoorDoubleOpen(this);
		new ArmorStandSwitch(this);
		new TippedArrowDamagePrevent(this);
		new NonSurvivalDeathRespawn(this);
		new EnderChestPlacePrevent(this);
		new ArenaPlayerKill(this);
		new ShulkerClick(this);
		new GuiShulkerClose(this);
		new GuiShulkerPreventDrop(this);
	}

	/**
	 * Initializes all AranarthCore recipes.
	 */
	private void initializeRecipes() {
		new RecipeHomePad(this);
		new RecipeChorusDiamond(this);
		new RecipeSaddleA(this);
		new RecipeSaddleB(this);
		new RecipeAmethystUncraft(this);
		new RecipeDeepslateA(this);
		new RecipeDeepslateB(this);
		new RecipeHorseArmorIron(this);
		new RecipeHorseArmorGolden(this);
		new RecipeHorseArmorDiamond(this);
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
		new RecipeWoolUncraft(this);
		new RecipeCalcite(this);
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
		new RecipeMushroomBlockBrown(this);
		new RecipeMushroomBlockBrownUncraft(this);
		new RecipeMushroomBlockRed(this);
		new RecipeMushroomBlockRedUncraft(this);
		new RecipeMushroomStem(this);
		new RecipeGlowstoneUncraft(this);
		new RecipeStick(this);
		new RecipeMossBlock(this);
		new RecipeMossCarpet(this);
	}

	/**
	 * Initializes the AranarthCore command and tab completion.
	 */
	private void initializeCommands() {
		getCommand("ac").setExecutor(new CommandAC());
		getCommand("ac").setTabCompleter(new CommandACCompleter());
	}

	/**
	 * Initializes the AranarthCore worlds.
	 */
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

	/**
	 * Initializes the AranarthCore custom items needing namespace keys.
	 */
	private void initializeItems() {
		new InvisibleItemFrame(this);
	}

	/**
	 * Called when the plugin is disabled on server shut down.
	 */
	@Override
	public void onDisable() {
		PersistenceUtils.saveHomes();
		PersistenceUtils.saveAranarthPlayers();
	}

}