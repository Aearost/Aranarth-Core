package com.aearost.aranarthcore;

import com.aearost.aranarthcore.commands.CommandAC;
import com.aearost.aranarthcore.commands.CommandACCompleter;
import com.aearost.aranarthcore.enums.Weather;
import com.aearost.aranarthcore.event.listener.*;
import com.aearost.aranarthcore.event.listener.grouped.*;
import com.aearost.aranarthcore.event.listener.misc.*;
import com.aearost.aranarthcore.items.InvisibleItemFrame;
import com.aearost.aranarthcore.recipes.*;
import com.aearost.aranarthcore.recipes.aranarthium.*;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.DateUtils;
import com.aearost.aranarthcore.utils.PersistenceUtils;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Random;

public class AranarthCore extends JavaPlugin {

	private static AranarthCore plugin;

	/**
	 * Called when the plugin is first enabled on server startup.
	 * Responsible for initializing all functionality of AranarthCore.
	 */
	@Override
	public void onEnable() {
		plugin = this;
		initializeWorlds();
		initializeUtils();
		initializeEvents();
		initializeRecipes();
		initializeCommands();
		initializeItems();


		// Sets default storm values
		AranarthUtils.setWeather(Weather.CLEAR);
		AranarthUtils.setStormDelay(new Random().nextInt(18000));

		runRepeatingTasks();
	}

	private void runRepeatingTasks() {
		// Update the persistence files every 30 minutes to protect from loss of data
		Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
			@Override
			public void run() {
				PersistenceUtils.saveHomes();
				PersistenceUtils.saveAranarthPlayers();
				PersistenceUtils.saveLockedContainers();
				PersistenceUtils.saveServerDate();
				PersistenceUtils.savePlayerShops();
				PersistenceUtils.saveDominions();
				Bukkit.getLogger().info("Homes and aranarth players have been saved");
			}
		}, 36000, 36000);

		// Check every 5 seconds to update Aranarthium effects, and to see if it is a new day
		Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
			@Override
			public void run() {
				AranarthUtils.applyArmourEffects();
				AranarthUtils.refreshMutes();

				// Seasons functionality
				DateUtils dateUtils = new DateUtils();
				dateUtils.calculateServerDate();
			}
		}, 0, 100);

		Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
			@Override
			public void run() {
				AranarthUtils.applyWaterfallEffect();
			}
		}, 0, 1);

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
		PersistenceUtils.loadPlayerShops();
		PersistenceUtils.loadServerDate();
		PersistenceUtils.loadLockedContainers();
		PersistenceUtils.loadDominions();
	}

	/**
	 * Initializes all AranarthCore events.
	 */
	private void initializeEvents() {
		// General listeners
		new BlockBreakEventListener(this);
		new BlockPlaceEventListener(this);
		new InventoryClickEventListener(this);
		new InventoryCloseEventListener(this);
		new PlayerInteractEventListener(this);
		new PlayerInteractEntityEventListener(this);
		new PlayerMoveEventListener(this);
		new EntityChangeBlockEventListener(this);
		new EntityPickupItemEventListener(this);
		new CreatureSpawnEventListener(this);
		new BlockGrowEventListener(this);
		new ItemSpawnEventListener(this);
		new BlockFadeEventListener(this);
		new PlayerItemConsumeEventListener(this);
		new EntitySpawnEventListener(this);
		new EntityDamageByEntityEventListener(this);
		new EntityDamageEventListener(this);
		new EntityTargetEventListener(this);
		new EntityDeathEventListener(this);
		new EntityShootBowEventListener(this);
		new BlockFormEventListener(this);
		new BlockPhysicsEventListener(this);
		new ProjectileHitEventListener(this);

		// Multi-event listeners for single purpose
		new InvisibleItemFrameListener(this);
		new ExplosionListener(this);
		new SoilTrampleListener(this);
		new CraftingOverridesListener(this);
		new PotionConsumeListener(this);
		new PlayerRespawnEventListener(this);
		new DominionInteract(this);
		new SpawnInteract(this);

		// Single-purpose and single-event event listeners
		new PlayerServerJoinListener(this);
		new PlayerServerQuitListener(this);
		new PlayerChatListener(this);
		new ArenaHungerLossPreventListener(this);
		new MobDestroyDoorListener(this);
		new PlayerTeleportBetweenWorldsListener(this);
		new ExpGainPreventListener(this);
		new VillagerCamelDismountListener(this);
		new PotionEffectStackListener(this);
		new PlayerShopCreateListener(this);
		new WeatherChangeListener(this);
		new LeafDecayDropsListener(this);
		new LeavesPreventBurnListener(this);
		new SnowballHitListener(this);
		new ArmorStandSwitchListener(this);
		new TamingXPFromBreeding(this);
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
		new RecipeBlackDye(this);
		new RecipePaleMossBlock(this);
		new RecipePaleMossCarpet(this);
		new RecipeHoneyBlockUncraft(this);
		new RecipeCopperExposed(this);
		new RecipeCopperWeathered(this);
		new RecipeCopperOxidized(this);
		new RecipeAranarthiumIngot(this);
		new RecipeAquaticAranarthium(this);
		new RecipeDwarvenAranarthium(this);
		new RecipeElvenAranarthium(this);
		new RecipeScorchedAranarthium(this);
		new RecipeArdentAranarthium(this);
		new RecipeSoulboundAranarthium(this);
		new RecipeGodApple(this);
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
		if (Bukkit.getWorld("world") == null) {
			WorldCreator wc = new WorldCreator("world");
			wc.environment(World.Environment.NORMAL);
			wc.type(WorldType.NORMAL);
			wc.createWorld();
		}

		if (Bukkit.getWorld("world_nether") == null) {
			WorldCreator wc = new WorldCreator("world_nether");
			wc.environment(World.Environment.NETHER);
			wc.type(WorldType.NORMAL);
			wc.createWorld();
		}

		if (Bukkit.getWorld("world_the_end") == null) {
			WorldCreator wc = new WorldCreator("world_the_end");
			wc.environment(World.Environment.THE_END);
			wc.type(WorldType.NORMAL);
			wc.createWorld();
		}

		if (Bukkit.getWorld("smp") == null) {
			WorldCreator wc = new WorldCreator("smp");
			wc.environment(World.Environment.NORMAL);
			wc.type(WorldType.NORMAL);
			wc.createWorld();
		}

		if (Bukkit.getWorld("smp_nether") == null) {
			WorldCreator wc = new WorldCreator("smp_nether");
			wc.environment(World.Environment.NETHER);
			wc.type(WorldType.NORMAL);
			wc.createWorld();
		}

		if (Bukkit.getWorld("smp_the_end") == null) {
			WorldCreator wc = new WorldCreator("smp_the_end");
			wc.environment(World.Environment.THE_END);
			wc.type(WorldType.NORMAL);
			wc.createWorld();
		}

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
		new InvisibleItemFrame();
	}

	/**
	 * Called when the plugin is disabled on server shut down.
	 */
	@Override
	public void onDisable() {
		PersistenceUtils.saveHomes();
		PersistenceUtils.saveAranarthPlayers();
		PersistenceUtils.savePlayerShops();
		PersistenceUtils.saveServerDate();
		PersistenceUtils.saveLockedContainers();
		PersistenceUtils.saveDominions();

		Bukkit.resetRecipes();
	}

}