package com.aearost.aranarthcore.utils;

import com.aearost.aranarthcore.AranarthCore;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.type.Slab;
import org.bukkit.block.data.type.Stairs;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.LocalDate;
import java.util.*;

/**
 * Provides utility methods to facilitate the formatting of all date related content.
 */
public class DateUtils {

	private final int month;
	private final int day;
	
	public DateUtils() {
		this.month = getMonth();
		this.day = getDay();
	}

	/**
	 * Provides the current month as an integer.
	 *
	 * @return The current month as an integer.
	 */
	private int getMonth() {
		return LocalDate.now().getMonthValue();
	}

	/**
	 * Provides the current date of the month as an integer.
	 *
	 * @return The current date of the month as an integer.
	 */
	private int getDay() {
		return LocalDate.now().getDayOfMonth();
	}

	/**
	 * Confirms if the current date is within the general range of Valentine's Day.
	 *
	 * @return Confirmation of whether it is roughly Valentine's Day.
	 */
	public boolean isValentinesDay() {
		if (this.month == 2) {
            return this.day >= 4 && this.day <= 14;
		}
		return false;
	}

	/**
	 * Confirms if the current date is within the general range of Easter.
	 *
	 * @return Confirmation of whether it is roughly Easter.
	 */
	public boolean isEaster() {
		if (this.month == 3) {
            return this.day >= 22 && this.day <= 31;
		} else if (this.month == 4) {
            return this.day >= 1 && this.day <= 25;
		}
		return false;
	}

	/**
	 * Confirms if the current date is within the general range of Halloween.
	 *
	 * @return Confirmation of whether it is roughly Halloween.
	 */
	public boolean isHalloween() {
		if (this.month == 10) {
            return this.day >= 20 && this.day <= 31;
		}
		return false;
	}

	/**
	 * Confirms if the current date is within the general range of Christmas.
	 *
	 * @return Confirmation of whether it is roughly Christmas.
	 */
	public boolean isChristmas() {
		if (this.month == 12) {
            return this.day >= 15 && this.day <= 25;
		}
		return false;
	}

	/**
	 * Identifies the current server date.
	 */
	public void calculateServerDate() {
		// Unformatted in-game day number
		int dayNum = (int) (Bukkit.getWorld("world").getGameTime() / 24000);
		int time = (int) (Bukkit.getWorld("world").getTime() / 20);


		// Last day in each season
		final int month1End = 147;
		final int month2End = 294;
		final int month3End = 440;
		final int month4End = 585;
		final int month5End = 730;
		final int month6End = 876;
		final int month7End = 1022;
		final int month8End = 1168;
		final int month9End = 1314;
		final int month10End = 1460;
		final int month11End = 1606;
		final int month12End = 1752;
		final int month13End = 1898;
		final int month14End = 2045;
		final int month15End = 2192;

		// Gets current server year
		int yearNum = 0;
		// If the amount is a clean multiple of 2192 (days in one year)
		if (dayNum % month15End == 0) {
			yearNum = dayNum / month1End;
		} else {
			yearNum = (int) (double) (dayNum / month1End) + 1;
		}

		// Gets current server day in the given year
		int dayNumInYear = (dayNum % month15End) + 1;
		int dayNumInMonth = 0;

		String monthName = null;
		// Gets the current server month
		if (dayNumInYear >= 1 && dayNumInYear < month1End) {
			monthName = "Ignivór";
		}
		else if (dayNumInYear > month1End && dayNumInYear <= month2End) {
			monthName = "Aquinvór";
			dayNumInMonth = dayNumInYear - month1End;
		} else if (dayNumInYear >= month2End && dayNumInYear <= month3End) {
			monthName = "Nebulivór";
			dayNumInMonth = dayNumInYear - month2End;
		} else if (dayNumInYear > month3End && dayNumInYear <= month4End) {
			monthName = "Ventirór";
			dayNumInMonth = dayNumInYear - month3End;
		} else if (dayNumInYear > month4End && dayNumInYear <= month5End) {
			monthName = "Florivór";
			dayNumInMonth = dayNumInYear - month4End;
		} else if (dayNumInYear > month5End && dayNumInYear <= month6End) {
			monthName = "Calorvór";
			dayNumInMonth = dayNumInYear - month5End;
		} else if (dayNumInYear > month6End && dayNumInYear <= month7End) {
			monthName = "Solarvór";
			dayNumInMonth = dayNumInYear - month6End;
		} else if (dayNumInYear > month7End && dayNumInYear <= month8End) {
			monthName = "Aestivór";
			dayNumInMonth = dayNumInYear - month7End;
		} else if (dayNumInYear > month8End && dayNumInYear <= month9End) {
			monthName = "Ardorvór";
			dayNumInMonth = dayNumInYear - month8End;
		} else if (dayNumInYear > month9End && dayNumInYear <= month10End) {
			monthName = "Fructivór";
			dayNumInMonth = dayNumInYear - month9End;
		} else if (dayNumInYear > month10End && dayNumInYear <= month11End) {
			monthName = "Follivór";
			dayNumInMonth = dayNumInYear - month10End;
		} else if (dayNumInYear > month11End && dayNumInYear <= month12End) {
			monthName = "Umbravór";
			dayNumInMonth = dayNumInYear - month11End;
		} else if (dayNumInYear > month12End && dayNumInYear <= month13End) {
			monthName = "Glacivór";
			dayNumInMonth = dayNumInYear - month12End;
		} else if (dayNumInYear > month13End && dayNumInYear <= month14End) {
			monthName = "Frigorvór";
			dayNumInMonth = dayNumInYear - month13End;
		} else if (dayNumInYear > month14End) {
			monthName = "Obscurvór";
			dayNumInMonth = dayNumInYear - month14End;
		} else {
			Bukkit.getLogger().info("Something went wrong with calculating the month name!");
			return;
		}

		// Gets current server weekday
		int weekdayNum = (dayNum % 8) + 1;
		String weekdayName = null;
		if (weekdayNum == 1) {
			weekdayName = "Hydris";
		} else if (weekdayNum == 2) {
			weekdayName = "Terris";
		}
		else if (weekdayNum == 3) {
			weekdayName = "Pyris";
		}
		else if (weekdayNum == 4) {
			weekdayName = "Aeris";
		}
		else if (weekdayNum == 5) {
			weekdayName = "Ferris";
		}
		else if (weekdayNum == 6) {
			weekdayName = "Sylvis";
		}
		else if (weekdayNum == 7) {
			weekdayName = "Umbris";
		}
		else if (weekdayNum == 8) {
			weekdayName = "Aethis";
		} else {
			Bukkit.getLogger().info("Something went wrong with calculating the weekday name!");
			return;
		}

		if (AranarthUtils.getMonthName() == null) {
			AranarthUtils.setMonthName(monthName);
		}

		// If it is a new day
		// First 5 seconds of a new day
		if (time >= 0 && time < 5) {
			displayServerDate(dayNumInMonth, yearNum, monthName, weekdayName);
		}
		determineMonthEffects();
	}

	/**
	 * Displays the server date on new days.
	 * @param dayNumInMonth The day in the server month.
	 * @param yearNum The current server year.
	 * @param monthName The current server month.
	 * @param weekdayName The current server day of the week.
	 */
	private void displayServerDate(int dayNumInMonth, int yearNum, String monthName, String weekdayName) {
		String dayNumAsString = dayNumInMonth + "";
		if (dayNumAsString.length() > 1) {
			if (dayNumAsString.equals("11")) {
				dayNumAsString += "th";
			} else if (dayNumAsString.endsWith("12")) {
				dayNumAsString += "th";
			} else if (dayNumAsString.endsWith("13")) {
				dayNumAsString += "th";
			} else {
				if (dayNumAsString.endsWith("1")) {
					dayNumAsString += "st";
				} else if (dayNumAsString.endsWith("2")) {
					dayNumAsString += "nd";
				} else if (dayNumAsString.endsWith("3")) {
					dayNumAsString += "rd";
				} else {
					dayNumAsString += "th";
				}
			}
		} else {
			if (dayNumAsString.endsWith("1")) {
				dayNumAsString += "st";
			} else if (dayNumAsString.endsWith("2")) {
				dayNumAsString += "nd";
			} else if (dayNumAsString.endsWith("3")) {
				dayNumAsString += "rd";
			} else {
				dayNumAsString += "th";
			}
		}

		Bukkit.broadcastMessage(ChatUtils.chatMessage("&6&l------------------------------"));
		Bukkit.broadcastMessage(ChatUtils.chatMessage("\n"));
		Bukkit.broadcastMessage(ChatUtils.chatMessage("&e&l" + weekdayName + ", &f&lthe " + dayNumAsString + " of " + monthName + ", &e&l" + yearNum));
		Bukkit.broadcastMessage(ChatUtils.chatMessage("\n"));
		Bukkit.broadcastMessage(ChatUtils.chatMessage("&6&l------------------------------"));

		for (Player player : Bukkit.getOnlinePlayers()) {
			player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 5f, 0.5f);
		}
	}

	/**
	 * Applies the effects of the given month on Aranarth.
	 */
	private void determineMonthEffects() {
        switch (AranarthUtils.getMonthName()) {
            case "Ignivór" -> applyIgnivorEffects();
            case "Aquinvór" -> applyAquinvorEffects();
            case "Nebulivór" -> applyNebulivorEffects();
            case "Ventirór" -> applyVentirorEffects();
            case "Florivór" -> applyFlorivorEffects();
            case "Calorvór" -> applyCalorvorEffects();
            case "Solarvór" -> applySolarvorEffects();
            case "Aestivór" -> applyAestivorEffects();
            case "Ardorvór" -> applyArdorvorEffects();
            case "Fructivór" -> applyFructivorEffects();
            case "Follivór" -> applyFollivorEffects();
            case "Umbravór" -> applyUmbravorEffects();
            case "Glacivór" -> applyGlacivorEffects();
            case "Frigorvór" -> applyFrigorvorEffects();
            case "Obscurvór" -> applyObscurvorEffects();
            default -> Bukkit.getLogger().info("Something went wrong with applying " + AranarthUtils.getMonthName() + "'s effects!");
        }
	}

	/**
	 * Apply the effects during the first month of Ignivor.
	 */
	private void applyIgnivorEffects() {
		List<PotionEffect> effects = new ArrayList<>();
		effects.add(new PotionEffect(PotionEffectType.LUCK, 320, 0));
		effects.add(new PotionEffect(PotionEffectType.REGENERATION, 320, 0));
		applyEffectToAllPlayers(effects);
		meltSnow();
	}

	/**
	 * Apply the effects during the second month of Aquinvor.
	 */
	private void applyAquinvorEffects() {
		List<PotionEffect> effects = new ArrayList<>();
		effects.add(new PotionEffect(PotionEffectType.DOLPHINS_GRACE, 320, 0));
		effects.add(new PotionEffect(PotionEffectType.WATER_BREATHING, 320, 0));
		applyEffectToAllPlayers(effects);
		meltSnow();
	}

	/**
	 * Apply the effects during the third month of Nebulivor.
	 */
	private void applyNebulivorEffects() {
		meltSnow();
	}

	/**
	 * Apply the effects during the fourth month of Ventiror.
	 */
	private void applyVentirorEffects() {
		List<PotionEffect> effects = new ArrayList<>();
		effects.add(new PotionEffect(PotionEffectType.SPEED, 320, 0));
		applyEffectToAllPlayers(effects);
		meltSnow();
	}

	/**
	 * Apply the effects during the fifth month of Florivor.
	 */
	private void applyFlorivorEffects() {
		meltSnow();
	}

	/**
	 * Apply the effects during the sixth month of Calorvor.
	 */
	private void applyCalorvorEffects() {
		meltSnow();
	}

	/**
	 * Apply the effects during the seventh month of Solarvor.
	 */
	private void applySolarvorEffects() {
		meltSnow();
	}

	/**
	 * Apply the effects during the eighth month of Aestivor.
	 */
	private void applyAestivorEffects() {
		meltSnow();
	}

	/**
	 * Apply the effects during the ninth month of Ardorvor.
	 */
	private void applyArdorvorEffects() {
		meltSnow();
	}

	/**
	 * Apply the effects during the tenth month of Fructivor.
	 */
	private void applyFructivorEffects() {
		meltSnow();
	}

	/**
	 * Apply the effects during the eleventh month of Follivor.
	 */
	private void applyFollivorEffects() {
		meltSnow();
	}

	/**
	 * Apply the effects during the twelfth month of Umbravor.
	 */
	private void applyUmbravorEffects() {
		applySnow(5, 100);
		meltSnow();
	}

	/**
	 * Apply the effects during the thirteenth month of Glacivor.
	 */
	private void applyGlacivorEffects() {
		List<PotionEffect> effects = new ArrayList<>();
		effects.add(new PotionEffect(PotionEffectType.SLOWNESS, 320, 0));
		applyEffectToAllPlayers(effects);
		applySnow(15, 400);
	}

	/**
	 * Apply the effects during the fourteenth month of Frigorvor.
	 */
	private void applyFrigorvorEffects() {
		List<PotionEffect> effects = new ArrayList<>();
		effects.add(new PotionEffect(PotionEffectType.SLOWNESS, 320, 1));
		applyEffectToAllPlayers(effects);
		applySnow(50, 1000);
	}

	/**
	 * Apply the effects during the fifteenth month of Obscurvor.
	 */
	private void applyObscurvorEffects() {
		List<PotionEffect> effects = new ArrayList<>();
		effects.add(new PotionEffect(PotionEffectType.MINING_FATIGUE, 320, 0));
		effects.add(new PotionEffect(PotionEffectType.SLOWNESS, 320, 0));
		applyEffectToAllPlayers(effects);
		applySnow(15, 400);
	}

	/**
	 * Applies potion effects to all online players.
	 * The effects will always be for the same fixed duration and same amplifier.
	 * @param effects The effects to be applied.
	 */
	private void applyEffectToAllPlayers(List<PotionEffect> effects) {
		for (Player player : Bukkit.getOnlinePlayers()) {
			player.addPotionEffects(effects);
		}
	}

	/**
	 * Applies the manual snow particles in the winter months of Aranarth.
	 * @param bigFlakeDensity The density of the larger snowflakes, being end rod particles.
	 * @param smallFlakeDensity The density of the smaller snowflakes, being white ash particles.
	 */
	private void applySnow(int bigFlakeDensity, int smallFlakeDensity) {
		new BukkitRunnable() {
			int runs = 0;

			@Override
			public void run() {
				// 20 executions * 5 ticks is 100 ticks, which is 5 seconds
				if (runs == 20) {
					// Determines if it is currently storming
					if (AranarthUtils.getIsStorming()) {
						// Determines if the storm ended
						if (AranarthUtils.getStormDuration() <= 0) {
							Random random = new Random();
							Bukkit.broadcastMessage(ChatUtils.chatMessage("&7&oThe storm has subsided..."));
							AranarthUtils.setIsStorming(false);
							String monthName = AranarthUtils.getMonthName();
							// Updates the delay until the next storm
                            switch (monthName) {
                                case "Umbravór" ->
                                    // At least 0.75 days, no more than 5 days
									AranarthUtils.setStormDelay(random.nextInt(102000) + 18000);
                                case "Glacivór" ->
                                    // At least 0.5 days, no more than 2 days
									AranarthUtils.setStormDelay(random.nextInt(48000) + 12000);
                                case "Frigorvór" ->
                                    // At least 0.25 days, no more than 1 day
									AranarthUtils.setStormDelay(random.nextInt(18000) + 6000);
                                case "Obscurvór" ->
                                    // At least 0.5 days, no more than 1.5 days
									AranarthUtils.setStormDelay(random.nextInt(36000) + 12000);
                            }
						} else {
							// 100 ticks per execution
							AranarthUtils.setStormDuration(AranarthUtils.getStormDuration() - 100);
						}
					}
					// If it is not storming
					else {
						// If it is time for the next storm
						if (AranarthUtils.getStormDelay() <= 0) {
							Random random = new Random();
							Bukkit.broadcastMessage(ChatUtils.chatMessage("&7&oA storm has started..."));
							AranarthUtils.setIsStorming(true);
							String monthName = AranarthUtils.getMonthName();
							// Updates the duration of the storm
                            switch (monthName) {
                                case "Umbravór" ->
                                    // At least 0.125 days, no more than 0.75 days
									AranarthUtils.setStormDuration(random.nextInt(15000) + 3000);
                                case "Glacivór" ->
                                    // At least 0.5 days, no more than 1.5 days
									AranarthUtils.setStormDuration(random.nextInt(24000) + 12000);
                                case "Frigorvór" ->
                                    // At least 0.75 days, no more than 2 days
									AranarthUtils.setStormDuration(random.nextInt(30000) + 18000);
                                case "Obscurvór" ->
                                    // At least 0.25 days, no more than 1 day
									AranarthUtils.setStormDuration(random.nextInt(18000) + 6000);
                            }
						} else {
							// 100 ticks per execution
							AranarthUtils.setStormDelay(AranarthUtils.getStormDelay() - 100);
						}
					}

					this.cancel();
					return;
				}

				// Handles applying the snow functionality
				if (AranarthUtils.getIsStorming()) {
					// Applies snow nearby all online players
					for (Player player : Bukkit.getOnlinePlayers()) {
						if (player != null) {
							Location loc = player.getLocation();
							// Only snows in the survival world
							if (!loc.getWorld().getName().equals("world")) {
								continue;
							}

							// Determines if the player is underground or not
							boolean areAllBlocksAir = true;
							Block highestBlock = loc.getWorld().getHighestBlockAt(loc.getBlockX(), loc.getBlockZ());
							if (loc.getBlockY() + 2 < (highestBlock.getLocation().getBlockY())) {
								areAllBlocksAir = false;
							}

							// Only apply particles if the player is exposed to air
							if (areAllBlocksAir) {
								// If it is a temperate or cold biome
								if (loc.getWorld().getTemperature(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()) <= 1) {
									loc.getWorld().spawnParticle(Particle.END_ROD, loc, bigFlakeDensity, 9, 12, 9, 0.05);
									loc.getWorld().spawnParticle(Particle.WHITE_ASH, loc, smallFlakeDensity, 9, 12, 9, 0.05);
								}
							}

							// Attempts to generate snow only once per second
							if (runs % 5 == 0) {
								generateSnow(loc, bigFlakeDensity);
							}
						}
					}
				}
				runs++;
			}
		}.runTaskTimer(AranarthCore.getInstance(), 0, 5); // Runs every 5 ticks
	}

	/**
	 * Handles the generation of snow nearby online players.
	 * @param loc The current location of the player.
	 * @param bigFlakeDensity The density of the large snowflakes to base the snowfall chance on.
	 */
	private void generateSnow(Location loc, int bigFlakeDensity) {
		// Blocks that shouldn't have snow placed on them
		final Set<Material> INVALID_SURFACE_BLOCKS = EnumSet.of(
				Material.WATER, Material.LAVA, Material.SEAGRASS, Material.TALL_SEAGRASS, Material.KELP, Material.KELP_PLANT,
				Material.SEA_PICKLE, Material.CAMPFIRE, Material.SOUL_CAMPFIRE, Material.CACTUS, Material.SUGAR_CANE,
				Material.BAMBOO, Material.BAMBOO_SAPLING, Material.TORCH, Material.WALL_TORCH, Material.REDSTONE_TORCH,
				Material.SOUL_TORCH, Material.RAIL, Material.ACTIVATOR_RAIL, Material.DETECTOR_RAIL, Material.POWERED_RAIL,
				Material.LADDER, Material.VINE, Material.SLIME_BLOCK, Material.HONEY_BLOCK,
				Material.LILY_PAD, Material.ANVIL, Material.BELL, Material.CHAIN, Material.LECTERN, Material.LIGHTNING_ROD,
				Material.RESPAWN_ANCHOR, Material.TRIPWIRE, Material.TRIPWIRE_HOOK, Material.LANTERN, Material.SOUL_LANTERN,
				Material.END_ROD, Material.SCAFFOLDING, Material.FLOWER_POT, Material.CANDLE, Material.CANDLE_CAKE,
				Material.AMETHYST_CLUSTER, Material.SMALL_AMETHYST_BUD, Material.MEDIUM_AMETHYST_BUD, Material.LARGE_AMETHYST_BUD,
				Material.POINTED_DRIPSTONE, Material.TURTLE_EGG, Material.SCULK_SENSOR, Material.SCULK_SHRIEKER, Material.BEACON, Material.DIRT_PATH, Material.FARMLAND
		);

		// Adding other variants
		for (Material material : Material.values()) {
			String name = material.name();
			if (name.endsWith("_WALL") || name.endsWith("_FENCE") || name.endsWith("_FENCE_GATE") || name.endsWith("_BUTTON")
					|| name.endsWith("_DOOR") || name.endsWith("_PLATE") || name.endsWith("_CARPET") || name.endsWith("_PANE")
					|| name.endsWith("_BED") || name.endsWith("_CANDLE") || name.endsWith("_BANNER")) {
				INVALID_SURFACE_BLOCKS.add(material);
			}
		}

		Random random = new Random();
		// Adds snow to the surrounding blocks from the player
		int centerX = loc.getBlockX();
		int centerZ = loc.getBlockZ();
		World world = loc.getWorld();

		// Loop over columns within a 75 block radius
		for (int x = centerX - 75; x <= centerX + 75; x++) {
			for (int z = centerZ - 75; z <= centerZ + 75; z++) {

				// Check that the column is within a 75-block circle (optional, for a round area)
				if (loc.distance(new Location(world, x, loc.getY(), z)) > 75) {
					continue;
				}

				Block surfaceBlock = world.getHighestBlockAt(x, z);
				double temperature = surfaceBlock.getWorld().getTemperature(surfaceBlock.getX(), surfaceBlock.getY(), surfaceBlock.getZ());
				// Hot biomes do not get snow
				if (temperature > 1) {
					continue;
				}
				// Frozen biomes have the highest snow rates
				else if (temperature <= 0) {
					bigFlakeDensity = bigFlakeDensity / 2;
				}
				// Cold biomes have high snow rates
				else if (temperature < 0.25) {
					bigFlakeDensity = bigFlakeDensity / 5;
				}

				// Temperate biomes have standard snow rates
				else {
					bigFlakeDensity = bigFlakeDensity / 10;
				}

				// Determines if snow will generate at this block
				int rand = random.nextInt(5000);
				// Proportionate snow amount to the snow density
				if (rand > bigFlakeDensity) {
					continue;
				}

				// If the surface block is invalid, skip this column
				if (INVALID_SURFACE_BLOCKS.contains(surfaceBlock.getType())) {
					continue;
				}

				// Ensures that snow only goes on flat parts of stairs/slabs
				if (surfaceBlock.getBlockData() instanceof Stairs stairs) {
					if (stairs.getHalf() == Bisected.Half.BOTTOM) {
						continue;
					}
				} else if (surfaceBlock.getBlockData() instanceof Slab slab) {
					if (slab.getType() == Slab.Type.BOTTOM) {
						continue;
					}
				}
				Block above = surfaceBlock.getRelative(BlockFace.UP);
				if (above.getType() != Material.AIR && above.getType() != Material.SHORT_GRASS && above.getType() != Material.FERN) {
					continue;
				}

				// Check that there is at least 25 blocks of air above.
				boolean clearAbove = true;
				for (int i = 1; i <= 25; i++) {
					Block checkBlock = surfaceBlock.getRelative(BlockFace.UP, i);
					if (checkBlock.getType() != Material.AIR && checkBlock.getType() != Material.SHORT_GRASS && checkBlock.getType() != Material.FERN) {
						clearAbove = false;
						break;
					}
				}
				if (!clearAbove) {
					continue;
				}

				// Places the snow
				above.setType(Material.SNOW);
			}
		}

	}

	private void meltSnow() {
		if (!isWinterMonth(AranarthUtils.getMonthName())) {
			new BukkitRunnable() {
				int runs = 0;

				@Override
				public void run() {
					// 20 executions * 5 ticks is 100 ticks, which is 5 seconds
					if (runs == 20) {
						this.cancel();
						return;
					}

					// Melts snow nearby all online players
					for (Player player : Bukkit.getOnlinePlayers()) {
						if (player != null) {
							Location loc = player.getLocation();
							// Only melt snow in the survival world
							if (!loc.getWorld().getName().equals("world")) {
								continue;
							}

							// Attempts to generate snow only once per second
							if (runs % 4 == 0) {
								Random random = new Random();
								// Adds snow to the surrounding blocks from the player
								int centerX = loc.getBlockX();
								int centerZ = loc.getBlockZ();
								World world = loc.getWorld();

								// Loop over columns within a 75 block radius
								for (int x = centerX - 75; x <= centerX + 75; x++) {
									for (int z = centerZ - 75; z <= centerZ + 75; z++) {

										// Check that the column is within a 75-block circle (optional, for a round area)
										if (loc.distance(new Location(world, x, loc.getY(), z)) > 75) {
											continue;
										}

										Block surfaceBlock = world.getHighestBlockAt(x, z);
										Block above = surfaceBlock.getRelative(BlockFace.UP);
										if (above.getType() != Material.SNOW) {
											continue;
										}

										double temperature = surfaceBlock.getWorld().getTemperature(surfaceBlock.getX(), surfaceBlock.getY(), surfaceBlock.getZ());
										int meltRate = 0;
										int grassReplaceRate = 0;
										String biome = surfaceBlock.getBiome().toString();

										// Hot biomes never have snow
										if (temperature > 1) {
											continue;
										}
										// Frozen biomes never melt
										else if (temperature <= 0) {
											continue;
										}
										// Cold biomes have higher snow rates so it should melt slower
										else if (temperature < 0.25) {
											meltRate = 1;
											grassReplaceRate = random.nextInt(100) + 1;
										}
										// Temperate biomes have standard melting rates
										else {
											meltRate = 3;
											grassReplaceRate = random.nextInt(100) + 1;
										}

										// Determines if snow will generate at this block
										int rand = random.nextInt(10000);

										// Proportionate melting rate for the given temperature
										if (rand > meltRate) {
											continue;
										}
										// Removes the snow
										above.setType(Material.AIR);

										// Adds short grass depending on biome
										switch (biome) {
											case "MEADOW":
												if (grassReplaceRate > 80) {
													return;
												}
												above.setType(Material.SHORT_GRASS);
												break;
											case "PLAINS":
												if (grassReplaceRate > 30) {
													return;
												}
												above.setType(Material.SHORT_GRASS);
												break;
											case "SUNFLOWER_PLAINS":
												if (grassReplaceRate > 70) {
													return;
												}
												above.setType(Material.SHORT_GRASS);
												break;
											case "SAVANNA":
											case "SAVANNA_PLATEAU":
												if (grassReplaceRate > 85) {
													return;
												}
												above.setType(Material.SHORT_GRASS);
												break;
											case "WINDSWEPT_HILLS":
											case "WINDSWEPT_FOREST":
												if (grassReplaceRate > 10) {
													return;
												}
												above.setType(Material.SHORT_GRASS);
												break;
											default:
												// For other biomes that are not excluded, randomly place grass but at a low rate
												if (grassReplaceRate > 7) {
													return;
												}
												above.setType(Material.SHORT_GRASS);
												break;
										}
									}
								}
							}
						}
					}
					runs++;
				}
			}.runTaskTimer(AranarthCore.getInstance(), 0, 5); // Runs every 5 ticks
		}
	}

	/**
	 * Confirms if the current month is a winter month.
	 * @param monthName The name of the month.
	 * @return Confirmation whether the current month is a winter month.
	 */
	public static boolean isWinterMonth(String monthName) {
		return monthName.equals("Umbravór") || monthName.equals("Glacivór") || monthName.equals("Frigorvór") || monthName.equals("Obscurvór");
	}
}
