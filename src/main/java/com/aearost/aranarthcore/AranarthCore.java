package com.aearost.aranarthcore;

import com.aearost.aranarthcore.abilities.airbending.spiritual.AstralProjection;
import com.aearost.aranarthcore.abilities.airbending.spiritual.PastLives;
import com.aearost.aranarthcore.network.NetworkManager;
import com.aearost.aranarthcore.event.block.IncantationMagnetismBlockBreak;
import com.aearost.aranarthcore.abilities.airbending.soundbending.SoundAbility;
import com.aearost.aranarthcore.commands.council.CommandAC;
import com.aearost.aranarthcore.commands.council.CommandACCompleter;
import com.aearost.aranarthcore.commands.council.CommandTrash;
import com.aearost.aranarthcore.commands.general.*;
import com.aearost.aranarthcore.enums.Month;
import com.aearost.aranarthcore.enums.Weather;
import com.aearost.aranarthcore.event.listener.*;
import com.aearost.aranarthcore.event.listener.grouped.*;
import com.aearost.aranarthcore.event.listener.misc.*;
import com.aearost.aranarthcore.event.mob.MountListener;
import com.aearost.aranarthcore.items.InvisibleItemFrame;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.VoidChunkGenerator;
import com.aearost.aranarthcore.recipes.*;
import com.aearost.aranarthcore.recipes.aranarthium.*;
import com.aearost.aranarthcore.utils.*;
import com.aearost.aranarthcore.event.listener.grouped.QuestEventListener;
import com.projectkorra.projectkorra.Element;
import com.projectkorra.projectkorra.ability.CoreAbility;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.dependencies.jda.api.JDA;
import github.scarsz.discordsrv.dependencies.jda.api.events.guild.member.GuildMemberJoinEvent;
import github.scarsz.discordsrv.dependencies.jda.api.events.guild.member.GuildMemberLeaveEvent;
import github.scarsz.discordsrv.dependencies.jda.api.hooks.ListenerAdapter;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class AranarthCore extends JavaPlugin {

    public static final String LOG_PREFIX = "[AC] ";

    private static AranarthCore plugin;
    private DiscordChatListener discordChatListener;
    private ListenerAdapter discordMemberJoinListener;
    private ListenerAdapter discordMemberLeaveListener;
    private RoleReactionListener roleReactionListener;
    private volatile boolean savedOnDisable = false;

    /**
     * Called when the plugin is first enabled on server startup.
     * Responsible for initializing all functionality of AranarthCore.
     */
    @Override
    public void onEnable() {
        plugin = this;
        saveDefaultConfig();
        initializeWorlds();

        // Initialize MySQL before loading data so DB-primary loads can activate.
        // Only on the public server; test server skips this entirely.
        if (isPublicServer()) {
            String host       = getConfig().getString("network.mysql.host", "127.0.0.1");
            int    port       = getConfig().getInt("network.mysql.port", 3306);
            String database   = getConfig().getString("network.mysql.database", "");
            String username   = getConfig().getString("network.mysql.username", "");
            String password   = getConfig().getString("network.mysql.password", "");

            com.aearost.aranarthcore.database.DatabaseManager.initialize(host, port, database, username, password);
        }

        initializeUtils();
        initializeEvents();
        initializeRecipes();
        initializeCommands();
        initializeItems();

        Bukkit.getScheduler().runTask(this, () -> {
            SimpleCommandMap commandMap = (SimpleCommandMap) Bukkit.getServer().getCommandMap();
            commandMap.getKnownCommands().put("ac", getCommand("ac"));
            commandMap.getKnownCommands().put("aranarthcore:ac", getCommand("ac"));
            commandMap.getKnownCommands().put("mctop", getCommand("mctop"));
            commandMap.getKnownCommands().put("aranarthcore:mctop", getCommand("mctop"));
        });

        // Sets default storm values
        AranarthUtils.setWeather(Weather.CLEAR);
        AranarthUtils.setStormDelay(new Random().nextInt(18000));

        SoundAbility.SOUND = new Element.SubElement("Sound", Element.AIR, Element.ElementType.NO_SUFFIX, this);
        try {
            Field colorField = Element.class.getDeclaredField("color");
            colorField.setAccessible(true);
            colorField.set(SoundAbility.SOUND, net.md_5.bungee.api.ChatColor.of("#6644CC"));
            colorField.set(Element.LIGHTNING, net.md_5.bungee.api.ChatColor.of("#FFF050"));
        } catch (Exception e) {
            Bukkit.getLogger().warning("Failed to set Sound sub-element color: " + e.getMessage());
        }
        CoreAbility.registerPluginAbilities(AranarthCore.getInstance(), "com.aearost.aranarthcore.abilities");
        Bukkit.getLogger().info(LOG_PREFIX + "AranarthCore Bending has been loaded");

        runRepeatingTasks();

        // Start cross-server networking (NetworkManager) now that the DB is already connected.
        // Only on the public server.
        if (isPublicServer() && com.aearost.aranarthcore.database.DatabaseManager.isActive()) {
            String serverName = getConfig().getString("network.this-server", "survival");
            NetworkManager.initialize(serverName);
            if (NetworkManager.isActive()) {
                // Re-populate remote roster in case the other server was already running
                NetworkManager.getInstance().syncRosterFromDatabase();
                // Register BungeeCord plugin messaging channel for player transfers
                getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
                Bukkit.getLogger().info(LOG_PREFIX + "Cross-server networking enabled via MySQL");
            }
        }

        // Fallback shutdown hook for restarters (i.e UltimateAutoRestart) that may
        // terminate the JVM via System.exit() without triggering onDisable()
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (!savedOnDisable) {
                saveAll();
            }
        }, "AranarthCore-ShutdownHook"));
    }

    private void runRepeatingTasks() {
        // Update the persistence files every 30 minutes to protect from loss of data
        Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
            @Override
            public void run() {
                PersistenceUtils.saveHomepads();
                PersistenceUtils.saveSentinels();
                PersistenceUtils.saveAranarthPlayers();
                PersistenceUtils.saveVotes();
                PersistenceUtils.saveVoteKeys();
                PersistenceUtils.saveRareKeys();
                PersistenceUtils.saveEpicKeys();
                PersistenceUtils.saveGodlyKeys();
                PersistenceUtils.saveToggledFeatures();
                PersistenceUtils.saveLockedContainers();
                PersistenceUtils.saveServerDate();
                PersistenceUtils.saveShops();
                ShopUtils.removeAllHolograms();
                ShopUtils.initializeAllHolograms();
                PersistenceUtils.saveDominions();
                PersistenceUtils.saveDominionPermissions();
                PersistenceUtils.saveDominionPlayerPermissions();
                DominionUtils.checkAndProcessConquestDeadlines();
                PersistenceUtils.saveWarps();
                PersistenceUtils.savePunishments();
                PersistenceUtils.saveAvatars();
                PersistenceUtils.saveBoosts();
                PersistenceUtils.saveCompressible();
                PersistenceUtils.saveShopLocations();
                PersistenceUtils.saveShopIslandCounter();
                PersistenceUtils.saveShopCollaborators();
                PersistenceUtils.saveKillDeathCount();
                PersistenceUtils.saveQuestState();
                PersistenceUtils.saveQuestProgress();
                PersistenceUtils.saveLoginStreaks();
                PersistenceUtils.saveGates();
                MountUtils.syncAllActiveHealthToData();
                PersistenceUtils.saveMounts();
                PersistenceUtils.saveMail();
                DiscordUtils.updateAllDiscordRoles();
                Bukkit.getLogger().info(LOG_PREFIX + "Aranarth data has been saved");

                // Resets the two bending arenas
                Bukkit.dispatchCommand(Bukkit.getServer().getConsoleSender(), "arenas reset arena1");
                Bukkit.dispatchCommand(Bukkit.getServer().getConsoleSender(), "arenas reset arena2");

                AranarthUtils.removeInactiveLockedContainers();
            }
        }, 36000, 36000);

        // Run every 10 minutes
        Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
            @Override
            public void run() {
                ChatUtils.sendServerTips();

                // TODO Re-enable once we do the Avatar event
//				// Attempts to automatically assign an avatar if there currently is none
//				if (AvatarUtils.getCurrentAvatar() == null) {
//					boolean wasAvatarFound = AvatarUtils.selectAvatar(false);
//					if (!wasAvatarFound) {
//						Bukkit.getLogger().info("A new Avatar could not be selected");
//					}
//				} else {
//					// Automatically removes an Avatar once they've been inactive for 7 days
//					Avatar avatar = AvatarUtils.getCurrentAvatar();
//					OfflinePlayer player = Bukkit.getOfflinePlayer(avatar.getUuid());
//					if (!player.isOnline()) {
//						Instant lastPlayed = Instant.ofEpochMilli(player.getLastPlayed());
//						LocalDateTime playerLastPlayDate = LocalDateTime.ofInstant(lastPlayed, ZoneId.systemDefault());
//						LocalDateTime currentDate = LocalDateTime.now();
//						if (playerLastPlayDate.plusDays(7).isBefore(currentDate)) {
//							Bukkit.getLogger().info("Avatar " + player.getName() + " has been inactive for 7 days");
//							AvatarUtils.removeCurrentAvatar();
//						}
//					}
//				}

                for (Player player : Bukkit.getOnlinePlayers()) {
                    PermissionUtils.reEvaluateMonthlySaints(player);
                }
                DominionLevelUtils.runPeriodicScan();
            }
        }, 12000, 12000);

        // Check every 5 seconds to update Aranarthium effects, and to see if it is a new day
        Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
            @Override
            public void run() {
                AranarthUtils.applyArmourEffects();
                AranarthUtils.applySpawnBuffs();
                AranarthUtils.refreshMutes();
                AranarthUtils.refreshBans();
                AranarthUtils.refreshServerBoosts();
                AranarthUtils.refreshSentinels();
                AranarthUtils.updateAfkLocations();
                AranarthUtils.updateTab();
                QuestUtils.checkAndPerformResets();
                if (roleReactionListener != null) {
                    roleReactionListener.pollReactions();
                }

                // Seasons functionality
                DateUtils dateUtils = new DateUtils();
                dateUtils.calculateServerDate();

                // Use the updated date to refresh all player inventories
                for (Player player : Bukkit.getOnlinePlayers()) {
                    CropUtils.refreshInventory(player.getInventory(), player.getWorld());
                    // Fallback if the player goes offline while vanished
                    AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
                    if (!aranarthPlayer.isVanished() && player.isInvisible()) {
                        player.setInvisible(false);
                    }
                }
            }
        }, 0, 100);

        // Remind players every hour if their chat is toggled off
        Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
                    if (aranarthPlayer.isTogglingChat()) {
                        player.sendMessage(ChatUtils.chatMessage("&7Your chat is currently &cdisabled&7. Use &e/toggle chat &7to re-enable it."));
                    }
                }
            }
        }, 72000, 72000);

        // Pull drops harvested with Magnetism
        Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
            @Override
            public void run() {
                IncantationMagnetismBlockBreak.tickMagnetismPull();
            }
        }, 2L, 2L);

//		Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
//			@Override
//			public void run() {
//				AranarthUtils.applyWaterfallEffect();
//			}
//		}, 1, 1);
    }

    public static AranarthCore getInstance() {
        return plugin;
    }

    public static boolean isPublicServer() {
        return plugin.getConfig().getBoolean("is-public-server", true);
    }

    /**
     * Returns true when this jar is running as the SMP server instance.
     * Always false on the test server (is-public-server: false).
     */
    public static boolean isSmpServer() {
        if (!isPublicServer()) return false;
        String thisServer = plugin.getConfig().getString("network.this-server", "survival");
        String smpServer  = plugin.getConfig().getString("network.servers.smp", "smp");
        return thisServer.equals(smpServer);
    }

    /** The Bukkit world name for the SMP overworld on whichever server is currently running. */
    public static String getSmpMainWorldName() {
        return isSmpServer() ? "world" : "smp";
    }

    /** The Bukkit world name for the SMP nether on whichever server is currently running. */
    public static String getSmpNetherWorldName() {
        return isSmpServer() ? "world_nether" : "smp_nether";
    }

    /** The Bukkit world name for the SMP end on whichever server is currently running. */
    public static String getSmpEndWorldName() {
        return isSmpServer() ? "world_the_end" : "smp_the_end";
    }

    /**
     * Initializes necessary Utilities functionality needed on server startup.
     */
    private void initializeUtils() {
        boolean db = com.aearost.aranarthcore.database.DatabaseManager.isActive();

        if (db) {
            PersistenceUtils.loadServerDateFromDatabase();
        } else {
            PersistenceUtils.loadServerDate();
        }
        if (db) {
            PersistenceUtils.loadHomepadsFromDatabase();
        } else {
            PersistenceUtils.loadHomepads();
        }
        if (db) {
            PersistenceUtils.loadAranarthPlayersFromDatabase();
        } else {
            PersistenceUtils.loadAranarthPlayers();
        }
        if (db) {
            PersistenceUtils.loadShopsFromDatabase();
            PersistenceUtils.loadServerShopsFromDatabase();
        } else {
            PersistenceUtils.loadShops();
        }
        ShopUtils.initializeAllHolograms();
        if (db) {
            PersistenceUtils.loadLockedContainersFromDatabase();
        } else {
            PersistenceUtils.loadLockedContainers();
        }
        // Dominions must load before permissions, outposts, and defenders
        if (db) {
            PersistenceUtils.loadDominionsFromDatabase();
        } else {
            PersistenceUtils.loadDominions();
        }
        if (db) {
            PersistenceUtils.loadDominionPermissionsFromDatabase();
        } else {
            PersistenceUtils.loadDominionPermissions();
        }
        if (db) {
            PersistenceUtils.loadDominionPlayerPermissionsFromDatabase();
        } else {
            PersistenceUtils.loadDominionPlayerPermissions();
        }
        if (db) {
            PersistenceUtils.loadOutpostsFromDatabase();
        } else {
            PersistenceUtils.loadOutposts();
        }
        if (db) {
            PersistenceUtils.loadDefendersFromDatabase();
        } else {
            PersistenceUtils.loadDefenders();
        }
        if (db) {
            PersistenceUtils.loadWarpsFromDatabase();
        } else {
            PersistenceUtils.loadWarps();
        }
        if (db) {
            PersistenceUtils.loadPunishmentsFromDatabase();
        } else {
            PersistenceUtils.loadPunishments();
        }
        if (db) {
            PersistenceUtils.loadAvatarsFromDatabase();
        } else {
            PersistenceUtils.loadAvatars();
        }
        if (db) {
            PersistenceUtils.loadBoostsFromDatabase();
        } else {
            PersistenceUtils.loadBoosts();
        }
        if (db) {
            PersistenceUtils.loadCompressibleFromDatabase();
        } else {
            PersistenceUtils.loadCompressible();
        }
        if (db) {
            PersistenceUtils.loadShopLocationsFromDatabase();
        } else {
            PersistenceUtils.loadShopLocations();
        }
        PersistenceUtils.loadShopIslandCounter();
        if (db) {
            PersistenceUtils.loadShopCollaboratorsFromDatabase();
        } else {
            PersistenceUtils.loadShopCollaborators();
        }
        // Sentinels and toggles must load after aranarth players
        if (db) {
            PersistenceUtils.loadSentinelsFromDatabase();
        } else {
            PersistenceUtils.loadSentinels();
        }
        if (db) {
            PersistenceUtils.loadVotesFromDatabase();
        } else {
            PersistenceUtils.loadVotes();
            PersistenceUtils.loadVoteKeys();
            PersistenceUtils.loadRareKeys();
            PersistenceUtils.loadEpicKeys();
            PersistenceUtils.loadGodlyKeys();
        }
        if (db) {
            PersistenceUtils.loadToggledFeaturesFromDatabase();
        } else {
            PersistenceUtils.loadToggledFeatures();
        }
        if (db) {
            PersistenceUtils.loadKillDeathCountFromDatabase();
        } else {
            PersistenceUtils.loadKillDeathCount();
        }
        if (db) {
            PersistenceUtils.loadQuestStateFromDatabase();
        } else {
            PersistenceUtils.loadQuestState();
        }
        QuestUtils.initialize(this);
        if (db) {
            PersistenceUtils.loadQuestProgressFromDatabase();
        } else {
            PersistenceUtils.loadQuestProgress();
        }
        if (db) {
            PersistenceUtils.loadLoginStreaksFromDatabase();
        } else {
            PersistenceUtils.loadLoginStreaks();
        }
        if (db) {
            PersistenceUtils.loadGatesFromDatabase();
        } else {
            PersistenceUtils.loadGates();
        }
        if (db) {
            PersistenceUtils.loadMountsFromDatabase();
        } else {
            PersistenceUtils.loadMounts();
        }
        if (db) {
            PersistenceUtils.loadMailFromDatabase();
        } else {
            PersistenceUtils.loadMail();
        }
        DefenderUtils.startRegenTask();
        DefenderUtils.startBoundaryTask();
        DefenderUtils.startTargetingTask();
        DefenderUtils.startFollowTask();
        DefenderUtils.startGuardTask();
    }

    /**
     * Initializes all AranarthCore events.
     */
    private void initializeEvents() {
        // Listeners that must run early
        new CropInfoEventListener(this);
        new QuestEventListener(this);

        // General listeners
        new BlockBreakEventListener(this);
        new BlockPlaceEventListener(this);
        new InventoryClickEventListener(this);
        new InventoryCloseEventListener(this);
        new InventoryOpenEventListener(this);
        new PlayerInteractEventListener(this);
        new PlayerInteractEntityEventListener(this);
        new PlayerMoveEventListener(this);
        new EntityChangeBlockEventListener(this);
        new EntityPickupItemEventListener(this);
        new CreatureSpawnEventListener(this);
        new BlockGrowEventListener(this);
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
        new ProjectileLaunchEventListener(this);
        new PlayerCommandPreprocessEventListener(this);
        new EntityBreedEventListener(this);
        new EntityTameEventListener(this);
        new PlayerDropItemEventListener(this);
        new PlayerItemDamageEventListener(this);
        new PlayerItemHeldEventListener(this);
        new ItemSpawnEventListener(this);

        // Multi-event listeners for single purpose
        new InvisibleItemFrameListener(this);
        new ExplosionListener(this);
        new SoilTrampleListener(this);
        new CraftingOverridesListener(this);
        new PotionConsumeListener(this);
        new PlayerRespawnEventListener(this);
        new DominionProtectionListener(this);
        new SpawnProtectionListener(this);
        new ShopProtectionListener(this);
        new PortalEventListener(this);
        new ArenaProtection(this);
        new FireProtectionListener(this);
        new SleepSkipListener(this);
        new BoostEffectsListener(this);
        new LeafDropsListener(this);
        new AranarthCoreBendingListener(this);
        new MountStatsListener(this);
        new MountListener(this);
        new PotionAlchemyExpListener(this);
        new TamedPetStealPreventListener(this);
        new RootingArrowMovePrevent(this);
        new GateListener(this);

        // Single-purpose and single-event event listeners
        new PlayerCommandSendEventListener(this);
        new PlayerServerJoinListener(this);
        new PlayerServerQuitListener(this);
        new PlayerChatListener(this);
        discordChatListener = new DiscordChatListener(this);
        new MobDestroyDoorListener(this);
        new PlayerTeleportBetweenWorldsListener(this);
        new DefenderFollowTeleportListener(this);
        new ExpGainPreventListener(this);
        new VillagerCamelDismountListener(this);
        new PotionEffectListener(this);
        new ShopCreateListener(this);
        new ShopHologramChunkListener(this);
        new WeatherChangeListener(this);
        new LeavesPreventBurnListener(this);
        new SnowballHitListener(this);
        new ArmorStandSwitchListener(this);
        new AnimalBreedingListener(this);
        if (Bukkit.getPluginManager().isPluginEnabled("VotifierPlus")) {
            new VotifierListener(this);
        }
        new ArmorStandInteractListener(this);
        new PlayerFishEventListener(this);
        new InvseeListener(this);

        // Discord server join and quit messages
        new BukkitRunnable() {
            @Override
            public void run() {
                JDA jda = DiscordSRV.getPlugin().getJda();
                discordMemberJoinListener = new ListenerAdapter() {
                    @Override
                    public void onGuildMemberJoin(GuildMemberJoinEvent event) {
                        DiscordUtils.discordServerJoin(event.getUser().getEffectiveName(), event.getUser().getId());
                    }
                };
                discordMemberLeaveListener = new ListenerAdapter() {
                    @Override
                    public void onGuildMemberLeave(GuildMemberLeaveEvent event) {
                        DiscordUtils.discordServerQuit(event.getUser().getEffectiveName(), event.getUser().getId());
                    }
                };
                jda.addEventListener(discordMemberJoinListener);
                jda.addEventListener(discordMemberLeaveListener);
                roleReactionListener = new RoleReactionListener();
                roleReactionListener.initReactions();
            }
        }.runTaskLater(AranarthCore.getInstance(), 1);
    }

    /**
     * Initializes all AranarthCore recipes.
     */
    private void initializeRecipes() {
        new RecipeHomePad(this);
        new RecipeChorusDiamond(this);
        new RecipeAmethystUncraft(this);
        new RecipeDeepslateA(this);
        new RecipeDeepslateB(this);
        new RecipeHorseArmorIron(this);
        new RecipeHorseArmorGolden(this);
        new RecipeHorseArmorDiamond(this);
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
        new RecipeQuartzBlockUncraft(this);
        new RecipeBambooBlockUncraft(this);
        new RecipeBambooPlanks(this);
        new RecipeGildedBlackstone(this);
        new RecipeRootedDirt(this);
        new RecipeTuffA(this);
        new RecipeTuffB(this);
        new RecipeDiamondOre(this);
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
        new RecipeHoneycombBlockUncraft(this);
        new RecipeBeeNest(this);
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
        new RecipeGreenDye(this);
        new RecipeMushroomStew(this);
    }

    /**
     * Initializes the AranarthCore command and tab completion.
     */
    private void initializeCommands() {
        getCommand("ac").setExecutor(new CommandAC());
        getCommand("ac").setTabCompleter(new CommandACCompleter());
        getCommand("afk").setExecutor(new CommandAfk());
        getCommand("anvil").setExecutor(new CommandAnvil());
        getCommand("aranarthium").setExecutor(new CommandAranarthium());
        getCommand("arena").setExecutor(new CommandArena());
        getCommand("avatar").setExecutor(new CommandAvatar());
        getCommand("back").setExecutor(new CommandBack());
        getCommand("balance").setExecutor(new CommandBalance());
        getCommand("balance").setTabCompleter(new CommandBalanceCompleter());
        getCommand("balancetop").setExecutor(new CommandBalanceTop());
        getCommand("blacklist").setExecutor(new CommandBlacklist());
        getCommand("blacklist").setTabCompleter(new CommandBlacklistCompleter());
        getCommand("boosts").setExecutor(new CommandBoosts());
        getCommand("boosts").setTabCompleter(new CommandBoostsCompleter());
        getCommand("calendar").setExecutor(new CommandCalendar());
        getCommand("cartography").setExecutor(new CommandCartography());
        getCommand("compressor").setExecutor(new CommandCompressor());
        getCommand("craft").setExecutor(new CommandCraft());
        getCommand("creative").setExecutor(new CommandCreative());
        getCommand("date").setExecutor(new CommandDate());
        getCommand("deaths").setExecutor(new CommandDeaths());
        getCommand("deaths").setTabCompleter(new CommandDeathsCompleter());
        getCommand("delhome").setExecutor(new CommandDelhome());
        getCommand("delhome").setTabCompleter(new CommandDelhomeCompleter());
        getCommand("dominion").setExecutor(new CommandDominion());
        getCommand("dominion").setTabCompleter(new CommandDominionCompleter());
        getCommand("enderchest").setExecutor(new CommandEnderchest());
        getCommand("fletching").setExecutor(new CommandFletching());
        getCommand("grindstone").setExecutor(new CommandGrindstone());
        getCommand("hat").setExecutor(new CommandHat());
        getCommand("home").setExecutor(new CommandHome());
        getCommand("home").setTabCompleter(new CommandHomeCompleter());
        getCommand("homepad").setExecutor(new CommandHomePad());
        getCommand("incantations").setExecutor(new CommandIncantations());
        getCommand("info").setExecutor(new CommandInfo());
        getCommand("info").setTabCompleter(new CommandInfoCompleter());
        getCommand("itemname").setExecutor(new CommandItemName());
        getCommand("itemname").setTabCompleter(new CommandItemNameCompleter());
        getCommand("keyclaim").setExecutor(new CommandKeyClaim());
        getCommand("kills").setExecutor(new CommandKills());
        getCommand("kills").setTabCompleter(new CommandKillsCompleter());
        getCommand("lock").setExecutor(new CommandLock());
        getCommand("loom").setExecutor(new CommandLoom());
        getCommand("message").setExecutor(new CommandMessage());
        getCommand("message").setTabCompleter(new CommandMessageCompleter());
        getCommand("motd").setExecutor(new CommandMotd());
        getCommand("nickname").setExecutor(new CommandNickname());
        getCommand("nickname").setTabCompleter(new CommandNicknameCompleter());
        getCommand("particles").setExecutor(new CommandParticles());
        getCommand("particles").setTabCompleter(new CommandParticlesCompleter());
        getCommand("pay").setExecutor(new CommandPay());
        getCommand("pay").setTabCompleter(new CommandPayCompleter());
        getCommand("pettransfer").setExecutor(new CommandPetTransfer());
        getCommand("pettransfer").setTabCompleter(new CommandPetTransferCompleter());
        getCommand("ping").setExecutor(new CommandPing());
        getCommand("ping").setTabCompleter(new CommandPingCompleter());
        getCommand("potions").setExecutor(new CommandPotions());
        getCommand("potions").setTabCompleter(new CommandPotionsCompleter());
        getCommand("pronouns").setExecutor(new CommandPronouns());
        getCommand("pronouns").setTabCompleter(new CommandPronounsCompleter());
        getCommand("randomizer").setExecutor(new CommandRandomizer());
        getCommand("randomizer").setTabCompleter(new CommandRandomizerCompleter());
        getCommand("ranks").setExecutor(new CommandRanks());
        getCommand("rankup").setExecutor(new CommandRankup());
        getCommand("reply").setExecutor(new CommandReply());
        getCommand("resource").setExecutor(new CommandResource());
        getCommand("rules").setExecutor(new CommandRules());
        getCommand("seen").setExecutor(new CommandSeen());
        getCommand("seen").setTabCompleter(new CommandSeenCompleter());
        getCommand("sethome").setExecutor(new CommandSethome());
        getCommand("sethome").setTabCompleter(new CommandSethomeCompleter());
        getCommand("shop").setExecutor(new CommandShop());
        getCommand("shop").setTabCompleter(new CommandShopCompleter());
        getCommand("smithing").setExecutor(new CommandSmithing());
        getCommand("smp").setExecutor(new CommandSMP());
        getCommand("spawn").setExecutor(new CommandSpawn());
        getCommand("stonecutter").setExecutor(new CommandStonecutter());
        getCommand("store").setExecutor(new CommandStore());
        getCommand("survival").setExecutor(new CommandSurvival());
        getCommand("tables").setExecutor(new CommandTables());
        getCommand("teleport").setExecutor(new CommandTeleport());
        getCommand("teleport").setTabCompleter(new CommandTeleportCompleter());
        getCommand("toggle").setExecutor(new CommandToggle());
        getCommand("toggle").setTabCompleter(new CommandToggleCompleter());
        getCommand("topdeaths").setExecutor(new CommandTopDeaths());
        getCommand("topkills").setExecutor(new CommandTopKills());
        getCommand("tpaccept").setExecutor(new CommandTpAccept());
        getCommand("tpdeny").setExecutor(new CommandTpDeny());
        getCommand("tphere").setExecutor(new CommandTpHere());
        getCommand("trash").setExecutor(new CommandTrash());
        getCommand("trust").setExecutor(new CommandTrust());
        getCommand("trust").setTabCompleter(new CommandTrustCompleter());
        getCommand("unlock").setExecutor(new CommandUnlock());
        getCommand("untrust").setExecutor(new CommandUntrust());
        getCommand("untrust").setTabCompleter(new CommandUntrustCompleter());
        getCommand("vote").setExecutor(new CommandVote());
        getCommand("votetop").setExecutor(new CommandVoteTop());
        getCommand("votetop").setTabCompleter(new CommandVoteTopCompleter());
        getCommand("voteshop").setExecutor(new CommandVoteShop());
        getCommand("warp").setExecutor(new CommandWarp());
        getCommand("warp").setTabCompleter(new CommandWarpCompleter());
        getCommand("quests").setExecutor(new CommandQuests());
        getCommand("streak").setExecutor(new CommandStreak());
        getCommand("mount").setExecutor(new CommandMount());
        getCommand("mount").setTabCompleter(new CommandMountCompleter());
        getCommand("mctop").setExecutor(new CommandMctop());
        getCommand("mctop").setTabCompleter(new CommandMctopCompleter());
        getCommand("mail").setExecutor(new CommandMail());
        getCommand("mail").setTabCompleter(new CommandMailCompleter());
        getCommand("map").setExecutor(new CommandMap());
    }

    /**
     * Initializes the AranarthCore worlds.
     */
    private void initializeWorlds() {
        if (isSmpServer()) {
            // SMP server: only load its three worlds; all other worlds live on the survival server.
            // The SMP server uses the default Minecraft world names (world/world_nether/world_the_end).
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

            // Disable advancement announcements and locator bar in SMP worlds
            for (World w : Bukkit.getWorlds()) {
                w.setGameRule(GameRules.SHOW_ADVANCEMENT_MESSAGES, false);
                w.setGameRule(GameRules.LOCATOR_BAR, false);
            }
            return;
        }

        // Survival server (public or test) — load all survival-side worlds.

        // Main survival worlds
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

        // SMP worlds — only on the test server (single-server setup).
        // On the public survival server the SMP worlds live on the separate SMP server.
        if (!isPublicServer()) {
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
        }

        // Resource worlds
        if (Bukkit.getWorld("resource") == null) {
            WorldCreator wc = new WorldCreator("resource");
            wc.environment(World.Environment.NORMAL);
            wc.type(WorldType.NORMAL);
            wc.createWorld();
        }
        if (Bukkit.getWorld("resource_nether") == null) {
            WorldCreator wc = new WorldCreator("resource_nether");
            wc.environment(World.Environment.NETHER);
            wc.type(WorldType.NORMAL);
            wc.createWorld();
        }
        if (Bukkit.getWorld("resource_the_end") == null) {
            WorldCreator wc = new WorldCreator("resource_the_end");
            wc.environment(World.Environment.THE_END);
            wc.type(WorldType.NORMAL);
            wc.createWorld();
        }

        // Arena and creative
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

        // Spawn (void world)
        if (Bukkit.getWorld("spawn") == null) {
            WorldCreator wc = new WorldCreator("spawn");
            wc.environment(World.Environment.NORMAL);
            wc.generator(new VoidChunkGenerator());
            wc.createWorld();

            World spawn = Bukkit.getWorld("spawn");
            spawn.setGameRule(GameRules.ADVANCE_TIME, false);
            spawn.setGameRule(GameRules.ADVANCE_WEATHER, false);
            spawn.setGameRule(GameRules.SPAWN_MOBS, false);
            Block block = spawn.getBlockAt(0, 100, 0);
            block.setType(Material.BEDROCK);
        }

        // Shops (void world)
        if (Bukkit.getWorld("shops") == null) {
            WorldCreator wc = new WorldCreator("shops");
            wc.environment(World.Environment.NORMAL);
            wc.generator(new VoidChunkGenerator());
            wc.createWorld();
        }
        World shops = Bukkit.getWorld("shops");
        if (shops != null) {
            shops.setGameRule(GameRules.ADVANCE_TIME, false);
            shops.setGameRule(GameRules.ADVANCE_WEATHER, false);
            shops.setGameRule(GameRules.SPAWN_MOBS, false);
            shops.setGameRule(GameRules.SHOW_ADVANCEMENT_MESSAGES, false);
            shops.setGameRule(GameRules.FIRE_DAMAGE, false);
        }

        // Disable advancement announcements and locator bar in every world
        for (World w : Bukkit.getWorlds()) {
            w.setGameRule(GameRules.SHOW_ADVANCEMENT_MESSAGES, false);
            w.setGameRule(GameRules.LOCATOR_BAR, false);
        }

        // Apply world borders (always set so they survive world resets)
        // Skip on SMP server — "world", "world_nether", "world_the_end" are SMP worlds there and have no border
        if (!isSmpServer()) {
            for (String worldName : new String[]{"world", "world_nether", "world_the_end"}) {
                World w = Bukkit.getWorld(worldName);
                if (w != null) {
                    w.getWorldBorder().setCenter(0, 0);
                    w.getWorldBorder().setSize(25250);
                }
            }
        }
        for (String worldName : new String[]{"resource", "resource_nether", "resource_the_end"}) {
            World w = Bukkit.getWorld(worldName);
            if (w != null) {
                w.getWorldBorder().setCenter(0, 0);
                w.getWorldBorder().setSize(5000);
            }
        }
    }

    /**
     * Resets the resource world automatically.
     * Called automatically on the 1st of Ignivór (new year).
     */
    public static void resetResourceWorlds() {
        String[] worldNames = {"resource", "resource_nether", "resource_the_end"};
        World.Environment[] environments = {
            World.Environment.NORMAL,
            World.Environment.NETHER,
            World.Environment.THE_END
        };

        Location spawnLocation = new Location(Bukkit.getWorld("spawn"), 0.5, 101, 0.5, 180, 0);

        Bukkit.broadcastMessage(ChatUtils.chatMessage(
            "&5A new year has dawned! The resource world is being reset..."));

        // Record the reset time so offline players can be detected on next login
        AranarthUtils.setLastResourceWorldResetTime(System.currentTimeMillis());
        PersistenceUtils.saveServerDate();

        // Teleport any players in a resource world to Spawn immediately
        for (Player player : Bukkit.getOnlinePlayers()) {
            String worldName = player.getWorld().getName();
            for (String rWorld : worldNames) {
                if (worldName.equals(rWorld)) {
                    player.teleport(spawnLocation);
                    player.sendMessage(ChatUtils.chatMessage(
                        "&7You have been teleported to &eSpawn &7due to the resource world reset!"));
                    break;
                }
            }
        }

        // Unload, delete, and recreate each resource world
        for (int i = 0; i < worldNames.length; i++) {
            String worldName = worldNames[i];
            World world = Bukkit.getWorld(worldName);

            File worldFolder = null;
            if (world != null) {
                worldFolder = world.getWorldFolder();
                Bukkit.unloadWorld(world, false);
            } else {
                worldFolder = new File(Bukkit.getWorldContainer(), worldName);
            }

            if (worldFolder.exists()) {
                deleteDirectory(worldFolder);
            }

            WorldCreator wc = new WorldCreator(worldName);
            wc.environment(environments[i]);
            wc.type(WorldType.NORMAL);
            World newWorld = wc.createWorld();

            if (newWorld != null) {
                newWorld.setGameRule(GameRules.SHOW_ADVANCEMENT_MESSAGES, false);
                newWorld.setGameRule(GameRules.LOCATOR_BAR, false);
                newWorld.getWorldBorder().setCenter(0, 0);
                newWorld.getWorldBorder().setSize(5000);
                preGenerateResourceWorld(newWorld);
            }
        }

        Bukkit.broadcastMessage(ChatUtils.chatMessage("&5The resource world has been reset - &dHappy New Year!"));
        launchNewYearFireworks();
    }

    /**
     * Asynchronously pre-generates all chunks within the resource world's 5000-block border.
     * Processes chunks in small batches per tick to avoid server lag.
     */
    private static void preGenerateResourceWorld(World world) {
        int borderHalf = 2500;
        int chunkMin = -(borderHalf / 16) - 1;
        int chunkMax = (borderHalf / 16) + 1;

        List<int[]> chunkCoords = new ArrayList<>();
        for (int cx = chunkMin; cx <= chunkMax; cx++) {
            for (int cz = chunkMin; cz <= chunkMax; cz++) {
                chunkCoords.add(new int[]{cx, cz});
            }
        }

        final int total = chunkCoords.size();
        final int batchSize = 8;
        final int[] index = {0};

        Bukkit.getLogger().info(LOG_PREFIX + "Pre-generating " + total + " chunks for world: " + world.getName());

        new BukkitRunnable() {
            @Override
            public void run() {
                if (Bukkit.getWorld(world.getName()) == null) {
                    cancel();
                    return;
                }
                if (index[0] >= total) {
                    cancel();
                    Bukkit.getLogger().info(LOG_PREFIX + "Finished pre-generating chunks for world: " + world.getName());
                    return;
                }
                for (int i = 0; i < batchSize && index[0] < total; i++, index[0]++) {
                    int[] coord = chunkCoords.get(index[0]);
                    int cx = coord[0];
                    int cz = coord[1];
                    world.getChunkAtAsync(cx, cz, true).thenAccept(chunk ->
                        world.unloadChunkRequest(chunk.getX(), chunk.getZ())
                    );
                }
            }
        }.runTaskTimer(getInstance(), 40L, 1L);
    }

    /**
     * Launches themed fireworks for each online player at the start of a new in-game month.
     * Fireworks use the month's signature color plus one lighter and one darker shade.
     * Runs for approximately 1 second (3 waves, 10 ticks apart).
     */
    public static void launchMonthFireworks(Month month) {
        Color[] colors = getMonthFireworkColors(month);
        Random random = new Random();
        FireworkEffect.Type[] types = FireworkEffect.Type.values();

        new BukkitRunnable() {
            int iterations = 0;

            @Override
            public void run() {
                if (iterations >= 3) {
                    cancel();
                    return;
                }
                for (Player player : Bukkit.getOnlinePlayers()) {
                    String worldName = player.getWorld().getName();
                    if (worldName.equals("arena") || worldName.equals("creative")) {
                        continue;
                    }
                    double offsetX = (random.nextDouble() - 0.5) * 5;
                    double offsetZ = (random.nextDouble() - 0.5) * 5;
                    double offsetY = random.nextDouble() * 2 + 2;
                    Location loc = player.getLocation().add(offsetX, offsetY, offsetZ);

                    Firework fw = player.getWorld().spawn(loc, Firework.class);
                    FireworkMeta meta = fw.getFireworkMeta();
                    Color mainColor = colors[random.nextInt(colors.length)];
                    Color fadeColor = colors[random.nextInt(colors.length)];
                    FireworkEffect effect = FireworkEffect.builder()
                        .with(types[random.nextInt(types.length)])
                        .withColor(mainColor)
                        .withFade(fadeColor)
                        .trail(random.nextBoolean())
                        .flicker(random.nextBoolean())
                        .build();
                    meta.addEffect(effect);
                    meta.setPower(0);
                    fw.setFireworkMeta(meta);
                    fw.setMetadata("newYearFirework", new FixedMetadataValue(plugin, true));
                    fw.detonate();
                }
                iterations++;
            }
        }.runTaskTimer(plugin, 0L, 10L);
    }

    private static Color[] getMonthFireworkColors(Month month) {
        return switch (month) {
            // #ffe082 — warm gold; lighter pastel gold + deeper amber
            case IGNIVOR   -> new Color[]{ Color.fromRGB(255,224,130), Color.fromRGB(255,242,185), Color.fromRGB(200,162,75) };
            // &3 (#00AAAA) — dark aqua; brighter teal + deep teal
            case AQUINVOR  -> new Color[]{ Color.fromRGB(0,170,170),   Color.fromRGB(50,215,215),  Color.fromRGB(0,105,105) };
            // #d1e5f4 — very light sky blue; only darken
            case VENTIVOR  -> new Color[]{ Color.fromRGB(209,229,244), Color.fromRGB(155,188,222), Color.fromRGB(95,140,185) };
            // #FDA4BA — soft pink; lighter blush + deeper rose
            case FLORIVOR  -> new Color[]{ Color.fromRGB(253,164,186), Color.fromRGB(255,205,218), Color.fromRGB(200,95,128) };
            // &e (#FFFF55) — bright yellow; lighter cream + deeper golden yellow
            case AESTIVOR  -> new Color[]{ Color.fromRGB(255,255,85),  Color.fromRGB(255,255,165), Color.fromRGB(195,195,0) };
            // &6 (#FFAA00) — gold; lighter golden + deep amber
            case CALORVOR  -> new Color[]{ Color.fromRGB(255,170,0),   Color.fromRGB(255,208,75),  Color.fromRGB(185,115,0) };
            // #ff4500 — orange-red; lighter orange + deep scarlet
            case ARDORVOR  -> new Color[]{ Color.fromRGB(255,69,0),    Color.fromRGB(255,125,55),  Color.fromRGB(175,25,0) };
            // #BD5745 — muted terracotta; lighter salmon + deep rust
            case SOLARVOR  -> new Color[]{ Color.fromRGB(189,87,69),   Color.fromRGB(230,135,112), Color.fromRGB(125,42,32) };
            // #a17100 — dark amber; lighter ochre + deep brown-gold
            case FOLLIVOR  -> new Color[]{ Color.fromRGB(161,113,0),   Color.fromRGB(210,158,45),  Color.fromRGB(95,60,0) };
            // #8a00c2 — vibrant purple; lighter violet + deep indigo-purple
            case STRIGAVOR -> new Color[]{ Color.fromRGB(138,0,194),   Color.fromRGB(182,55,242),  Color.fromRGB(78,0,125) };
            // #5b0001 — very dark crimson; only lighten
            case FAUNIVOR  -> new Color[]{ Color.fromRGB(91,0,1),      Color.fromRGB(145,28,28),   Color.fromRGB(205,72,72) };
            // #2B3856 — dark navy-grey; only lighten
            case UMBRAVOR  -> new Color[]{ Color.fromRGB(43,56,86),    Color.fromRGB(78,100,142),  Color.fromRGB(118,148,202) };
            // #DBE9FA — very pale ice blue; only darken
            case GLACIVOR  -> new Color[]{ Color.fromRGB(219,233,250), Color.fromRGB(155,188,228), Color.fromRGB(92,138,195) };
            // #79BAEC — medium cornflower blue; lighter sky + deeper cerulean
            case FRIGORVOR -> new Color[]{ Color.fromRGB(121,186,236), Color.fromRGB(178,220,255), Color.fromRGB(55,128,190) };
            // #2C041C — very dark maroon-purple; only lighten
            case OBSCURVOR -> new Color[]{ Color.fromRGB(44,4,28),     Color.fromRGB(102,18,68),   Color.fromRGB(162,58,118) };
        };
    }

    private static void launchNewYearFireworks() {
        Random random = new Random();
        Color[] brightColors = {
            Color.RED, Color.ORANGE, Color.YELLOW, Color.GREEN, Color.AQUA,
            Color.BLUE, Color.PURPLE, Color.FUCHSIA, Color.WHITE, Color.LIME
        };
        FireworkEffect.Type[] types = FireworkEffect.Type.values();

        new BukkitRunnable() {
            int iterations = 0;

            @Override
            public void run() {
                if (iterations >= 6) {
                    cancel();
                    return;
                }
                for (Player player : Bukkit.getOnlinePlayers()) {
                    for (int i = 0; i < 2; i++) {
                        double offsetX = (random.nextDouble() - 0.5) * 6;
                        double offsetZ = (random.nextDouble() - 0.5) * 6;
                        double offsetY = random.nextDouble() * 3 + 2;
                        Location loc = player.getLocation().add(offsetX, offsetY, offsetZ);

                        Firework fw = player.getWorld().spawn(loc, Firework.class);
                        FireworkMeta meta = fw.getFireworkMeta();
                        FireworkEffect effect = FireworkEffect.builder()
                            .with(types[random.nextInt(types.length)])
                            .withColor(brightColors[random.nextInt(brightColors.length)],
                                       brightColors[random.nextInt(brightColors.length)])
                            .withFade(brightColors[random.nextInt(brightColors.length)])
                            .trail(random.nextBoolean())
                            .flicker(random.nextBoolean())
                            .build();
                        meta.addEffect(effect);
                        meta.setPower(0);
                        fw.setFireworkMeta(meta);
                        fw.setMetadata("newYearFirework", new FixedMetadataValue(plugin, true));
                        fw.detonate();
                    }
                }
                iterations++;
            }
        }.runTaskTimer(plugin, 0L, 10L);
    }

    private static void deleteDirectory(File directory) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    boolean isFileDeleted = file.delete();
                }
            }
        }
        boolean isDirectoryDeleted = directory.delete();
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
        savedOnDisable = true;
        saveAll();

        NetworkManager.shutdown();
        com.aearost.aranarthcore.database.DatabaseManager.shutdown();

        Bukkit.resetRecipes();
        if (discordChatListener != null) discordChatListener.unsubscribe();

        JDA jda = DiscordSRV.getPlugin().getJda();
        if (jda != null) {
            if (discordMemberJoinListener != null) {
                jda.removeEventListener(discordMemberJoinListener);
            }
            if (discordMemberLeaveListener != null) {
                jda.removeEventListener(discordMemberLeaveListener);
            }
        }
    }

    /**
     * Saves all persistent data to disk.
     * Called from onDisable() and from the JVM shutdown hook (fallback for
     * restarters like UltimateAutoRestart that may bypass onDisable()).
     */
    private void saveAll() {
        // End all active AstralProjections/cancel the abilities
        AstralProjection.endAllProjections();
        PastLives.endAllInstances();

        ShopUtils.removeAllHolograms();
        PersistenceUtils.saveServerDate();
        PersistenceUtils.saveHomepads();
        PersistenceUtils.saveSentinels();
        PersistenceUtils.saveVotes();
        PersistenceUtils.saveVoteKeys();
        PersistenceUtils.saveRareKeys();
        PersistenceUtils.saveEpicKeys();
        PersistenceUtils.saveGodlyKeys();
        PersistenceUtils.saveToggledFeatures();
        PersistenceUtils.saveKillDeathCount();
        PersistenceUtils.saveAranarthPlayers();
        PersistenceUtils.saveShops();
        PersistenceUtils.saveLockedContainers();
        PersistenceUtils.saveDominions();
        PersistenceUtils.saveOutposts();
        PersistenceUtils.saveDefenders();
        PersistenceUtils.saveDominionPermissions();
        PersistenceUtils.saveDominionPlayerPermissions();
        PersistenceUtils.saveWarps();
        PersistenceUtils.savePunishments();
        PersistenceUtils.saveAvatars();
        PersistenceUtils.saveBoosts();
        PersistenceUtils.saveCompressible();
        PersistenceUtils.saveShopLocations();
        PersistenceUtils.saveShopIslandCounter();
        PersistenceUtils.saveShopCollaborators();
        PersistenceUtils.saveQuestState();
        PersistenceUtils.saveQuestProgress();
        PersistenceUtils.saveLoginStreaks();
        PersistenceUtils.saveGates();
        MountUtils.syncAllActiveHealthToData();
        PersistenceUtils.saveMounts();
        PersistenceUtils.saveMail();
        Bukkit.getLogger().info(LOG_PREFIX + "Aranarth data has been saved (shutdown)");
    }

}