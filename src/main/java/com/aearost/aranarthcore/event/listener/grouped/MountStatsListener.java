package com.aearost.aranarthcore.event.listener.grouped;

import com.aearost.aranarthcore.AranarthCore;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityBreedEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.persistence.PersistentDataType;

import java.util.Random;

import static com.aearost.aranarthcore.objects.CustomKeys.*;
import static org.bukkit.attribute.Attribute.JUMP_STRENGTH;
import static org.bukkit.attribute.Attribute.MOVEMENT_SPEED;

public class MountStatsListener implements Listener {

	private final double minHorseHealth = 16;
	private final double maxHorseHealth = 60;
	private final double minHorseJump = 0.57;
	private final double maxHorseJump = 1.28;
	private final double minHorseSpeed = 0.19;
	private final double maxHorseSpeed = 0.592417062;

	private final double minCamelHealth = 20;
	private final double maxCamelHealth = 80;
	private final double minCamelJump = 0.382;
	private final double maxCamelJump = 0.909;
	private final double minCamelSpeed = 0.19;
	private final double maxCamelSpeed = 0.428;

	public MountStatsListener(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	/**
	 * Deals with overriding the default spawn behaviour for horses and camels.
	 * Determines the values in brackets of probability, however fully randomized.
	 * @param e The event.
	 */
	@EventHandler
	public void onMountSpawn(final CreatureSpawnEvent e) {
		if (e.getSpawnReason() == CreatureSpawnEvent.SpawnReason.BREEDING) {
			return;
		}

		if (!(e.getEntity() instanceof Camel || e.getEntity() instanceof CamelHusk)) {
			AbstractHorse horse;
			if (e.getEntity() instanceof Horse) {
				horse = (Horse) e.getEntity();
			} else if (e.getEntity() instanceof SkeletonHorse) {
				horse = (SkeletonHorse) e.getEntity();
			} else if (e.getEntity() instanceof ZombieHorse) {
				horse = (ZombieHorse) e.getEntity();
			} else {
				// Donkeys, Mules, Llamas, etc
				return;
			}

			Random r = new Random();

			// A maximum limit of 30 hearts (60 half-hearts) --> 60
			// Will need a minimum of 8 hearts (16 half-hearts)
			final double healthBracket = r.nextInt(10) + 1;
			final double healthMin;
			final double healthMax;
			if (healthBracket < 5) {
				healthMin = minHorseHealth;
				healthMax = 34;
			} else if (healthBracket < 9) {
				healthMin = 35;
				healthMax = 44;
			} else {
				healthMin = 45;
				healthMax = maxHorseHealth;
			}
			final double healthValue = r.nextInt((int) ((healthMax - healthMin) + 1)) + healthMin;
			horse.getAttribute(Attribute.MAX_HEALTH).setBaseValue(healthValue);
			horse.getPersistentDataContainer().set(MOUNT_HEALTH, PersistentDataType.DOUBLE, healthValue);

			// A maximum limit of 8 blocks of jump --> 1.28
			// A minimum limit of 2 blocks of jump --> 0.57
			final int jumpBracket = r.nextInt(10) + 1;
			final double jumpMin;
			final double jumpMax;
			if (jumpBracket < 5) {
				jumpMin = minHorseJump;
				jumpMax = 0.84;
			} else if (jumpBracket < 9) {
				jumpMin = 0.85;
				jumpMax = 0.99;
			} else {
				jumpMin = 1.00;
				jumpMax = maxHorseJump;
			}
			final double jumpValue = jumpMin + (jumpMax - jumpMin) * r.nextDouble();
			horse.getAttribute(JUMP_STRENGTH).setBaseValue(jumpValue);
			horse.getPersistentDataContainer().set(MOUNT_JUMP, PersistentDataType.DOUBLE, jumpValue);

			// A maximum limit of 25 m/s --> 0.592417062
			// A minimum limit of 8 m/s --> 0.19
			final int speedBracket = r.nextInt(10) + 1;
			final double speedMin;
			final double speedMax;
			if (speedBracket < 5) {
				speedMin = minHorseSpeed;
				speedMax = 0.24;
			} else if (speedBracket < 9) {
				speedMin = 0.25;
				speedMax = 0.44;
			} else {
				speedMin = 0.45;
				speedMax = maxHorseSpeed;
			}
			final double speedValue = speedMin + (speedMax - speedMin) * r.nextDouble();
			horse.getAttribute(Attribute.MOVEMENT_SPEED).setBaseValue(speedValue);
			horse.getPersistentDataContainer().set(MOUNT_SPEED, PersistentDataType.DOUBLE, speedValue);

			// Without this, skeleton horses and zombie horses will not be rideable
			// and will spawn with very low health
			if (horse instanceof SkeletonHorse || horse instanceof ZombieHorse) {
				horse.setTamed(true);
				horse.setHealth(horse.getAttribute(Attribute.MAX_HEALTH).getValue());
			}
		} else {
			LivingEntity camel = e.getEntity();
			Random r = new Random();

			// A maximum limit of 40 hearts (80 half-hearts) --> 80
			// Will need a minimum of 8 hearts (16 half-hearts)
			final double healthBracket = r.nextInt(10) + 1;
			final double healthMin;
			final double healthMax;
			if (healthBracket < 5) {
				healthMin = minCamelHealth;
				healthMax = 34;
			} else if (healthBracket < 9) {
				healthMin = 35;
				healthMax = 54;
			} else {
				healthMin = 55;
				healthMax = maxCamelHealth;
			}
			final double healthValue = r.nextInt((int) ((healthMax - healthMin) + 1)) + healthMin;
			camel.getAttribute(Attribute.MAX_HEALTH).setBaseValue(healthValue);
			camel.getPersistentDataContainer().set(MOUNT_HEALTH, PersistentDataType.DOUBLE, healthValue);

			// A maximum limit of 4.5 blocks of jump --> 0.909
			// A minimum limit of 1 blocks of jump --> 0.382
			final int jumpBracket = r.nextInt(10) + 1;
			final double jumpMin;
			final double jumpMax;
			if (jumpBracket < 5) {
				jumpMin = minCamelJump;
				jumpMax = 0.44;
			} else if (jumpBracket < 9) {
				jumpMin = 0.45;
				jumpMax = 0.84;
			} else {
				jumpMin = 0.85;
				jumpMax = maxCamelJump;
			}
			final double jumpValue = jumpMin + (jumpMax - jumpMin) * r.nextDouble();
			camel.getAttribute(JUMP_STRENGTH).setBaseValue(jumpValue);
			camel.getPersistentDataContainer().set(MOUNT_JUMP, PersistentDataType.DOUBLE, jumpValue);

			// A maximum limit of 18 m/s --> 0.428
			// A minimum limit of 8 m/s --> 0.19
			final int speedBracket = r.nextInt(10) + 1;
			final double speedMin;
			final double speedMax;
			if (speedBracket < 5) {
				speedMin = minCamelSpeed;
				speedMax = 0.24;
			} else if (speedBracket < 9) {
				speedMin = 0.25;
				speedMax = 0.34;
			} else {
				speedMin = 0.35;
				speedMax = maxCamelSpeed;
			}
			final double speedValue = speedMin + (speedMax - speedMin) * r.nextDouble();
			camel.getAttribute(Attribute.MOVEMENT_SPEED).setBaseValue(speedValue);
			camel.getPersistentDataContainer().set(MOUNT_SPEED, PersistentDataType.DOUBLE, speedValue);
		}
	}

	/**
	 * Handles updating the mount stats when one is bred in.
	 * Takes stats and traits from both parents, with a chance of increasing and of decreasing the stats.
	 * @param e The event.
	 */
	@EventHandler
	public void onMountBreed(final EntityBreedEvent e) {
		boolean isMount = false;
		LivingEntity child = e.getEntity();
		if (child instanceof AbstractHorse || child instanceof Camel || child instanceof CamelHusk) {
			isMount = true;
		}

		if (isMount) {
			LivingEntity father = e.getFather();
			double fatherHealth = father.getAttribute(Attribute.MAX_HEALTH).getBaseValue();
			double fatherJump = father.getAttribute(JUMP_STRENGTH).getBaseValue();
			double fatherSpeed = father.getAttribute(Attribute.MOVEMENT_SPEED).getBaseValue();

			LivingEntity mother = e.getMother();
			double motherHealth = mother.getAttribute(Attribute.MAX_HEALTH).getBaseValue();
			double motherJump = mother.getAttribute(JUMP_STRENGTH).getBaseValue();
			double motherSpeed = mother.getAttribute(Attribute.MOVEMENT_SPEED).getBaseValue();

			double childHealth = calculateValue(MOUNT_HEALTH, child, fatherHealth, motherHealth);
			double childJump = calculateValue(MOUNT_JUMP, child, fatherJump, motherJump);
			double childSpeed = calculateValue(MOUNT_SPEED, child, fatherSpeed, motherSpeed);

			child.getAttribute(Attribute.MAX_HEALTH).setBaseValue(childHealth);
			child.getPersistentDataContainer().set(MOUNT_HEALTH, PersistentDataType.DOUBLE, childHealth);
			child.getAttribute(JUMP_STRENGTH).setBaseValue(childJump);
			child.getPersistentDataContainer().set(MOUNT_JUMP, PersistentDataType.DOUBLE, childJump);
			child.getAttribute(Attribute.MOVEMENT_SPEED).setBaseValue(childSpeed);
			child.getPersistentDataContainer().set(MOUNT_SPEED, PersistentDataType.DOUBLE, childSpeed);
		}
	}

	/**
	 * Provides a value that is either the same, slightly lower, or slightly higher than the two input values.
	 * @param key The type of parameter.
	 * @param child The LivingEntity that is being bred.
	 * @param fatherValue The value that the father has.
	 * @param motherValue The value that the mother has.
	 * @return The calculated value.
	 */
	private double calculateValue(NamespacedKey key, LivingEntity child, double fatherValue, double motherValue) {
		Random random = new Random();

		boolean isDecreasing = random.nextInt(5) == 0;
		if (isDecreasing) {
			if (child instanceof AbstractHorse horse) {
				if (key == MOUNT_HEALTH) {
					double healthAmountToReduce = random.nextInt(4);
					double calculated = child.getAttribute(Attribute.MAX_HEALTH).getBaseValue() - healthAmountToReduce;
					return calculated < minHorseHealth ? minHorseHealth : calculated;
				} else if (key == MOUNT_JUMP) {
					double jumpAmountToReduce = random.nextDouble(0.25);
					double calculated = child.getAttribute(JUMP_STRENGTH).getBaseValue() - jumpAmountToReduce;
					return calculated < minHorseJump ? minHorseJump : calculated;
				} else if (key == MOUNT_SPEED) {
					double speedAmountToReduce = random.nextDouble(0.05);
					double calculated = child.getAttribute(MOVEMENT_SPEED).getBaseValue() - speedAmountToReduce;
					return calculated < minHorseSpeed ? minHorseSpeed : calculated;
				}
			} else if (child instanceof Camel camel || child instanceof CamelHusk) {
				if (key == MOUNT_HEALTH) {
					double healthAmountToReduce = random.nextInt(4);
					double calculated = child.getAttribute(Attribute.MAX_HEALTH).getBaseValue() - healthAmountToReduce;
					return calculated < minCamelHealth ? minCamelHealth : calculated;
				} else if (key == MOUNT_JUMP) {
					double jumpAmountToReduce = random.nextDouble(0.25);
					double calculated = child.getAttribute(JUMP_STRENGTH).getBaseValue() - jumpAmountToReduce;
					return calculated < minCamelJump ? minCamelJump : calculated;
				} else if (key == MOUNT_SPEED) {
					double speedAmountToReduce = random.nextDouble(0.05);
					double calculated = child.getAttribute(MOVEMENT_SPEED).getBaseValue() - speedAmountToReduce;
					return calculated < minCamelSpeed ? minCamelSpeed : calculated;
				}
			}
		} else {
			boolean isIncreasing = random.nextInt(5) == 0;
			if (isIncreasing) {
				if (child instanceof AbstractHorse horse) {
					if (key == MOUNT_HEALTH) {
						double healthAmountToIncrease = random.nextInt(4);
						double calculated = child.getAttribute(Attribute.MAX_HEALTH).getBaseValue() + healthAmountToIncrease;
						return calculated > maxHorseHealth ? maxHorseHealth : calculated;
					} else if (key == MOUNT_JUMP) {
						double jumpAmountToIncrease = random.nextDouble(0.25);
						double calculated = child.getAttribute(JUMP_STRENGTH).getBaseValue() + jumpAmountToIncrease;
						return calculated > maxHorseJump ? maxHorseJump : calculated;
					} else if (key == MOUNT_SPEED) {
						double speedAmountToIncrease = random.nextDouble(0.05);
						double calculated = child.getAttribute(MOVEMENT_SPEED).getBaseValue() + speedAmountToIncrease;
						return calculated > maxHorseSpeed ? maxHorseSpeed : calculated;
					}
				} else if (child instanceof Camel camel || child instanceof CamelHusk) {
					if (key == MOUNT_HEALTH) {
						double healthAmountToIncrease = random.nextInt(4);
						double calculated = child.getAttribute(Attribute.MAX_HEALTH).getBaseValue() + healthAmountToIncrease;
						return calculated > maxCamelHealth ? maxCamelHealth : calculated;
					} else if (key == MOUNT_JUMP) {
						double jumpAmountToIncrease = random.nextDouble(0.25);
						double calculated = child.getAttribute(JUMP_STRENGTH).getBaseValue() + jumpAmountToIncrease;
						return calculated > maxCamelJump ? maxCamelJump : calculated;
					} else if (key == MOUNT_SPEED) {
						double speedAmountToIncrease = random.nextDouble(0.05);
						double calculated = child.getAttribute(MOVEMENT_SPEED).getBaseValue() + speedAmountToIncrease;
						return calculated > maxCamelSpeed ? maxCamelSpeed : calculated;
					}
				}
			}
		}

		// Provides the average of both stats if not decreasing or increasing
		return (fatherValue + motherValue) / 2;
	}

	/**
	 * Handles logic to refresh the mount's stats based on their PersistentDataContainer attributes.
	 * @param e The event.
	 */
	@EventHandler
	public void onMount(VehicleEnterEvent e) {
		if (e.getVehicle() instanceof AbstractHorse || e.getVehicle() instanceof Camel || e.getVehicle() instanceof CamelHusk) {
			if (e.getVehicle().getPersistentDataContainer().get(MOUNT_HEALTH, PersistentDataType.DOUBLE) == null
				|| e.getVehicle().getPersistentDataContainer().get(MOUNT_JUMP, PersistentDataType.DOUBLE) == null
				|| e.getVehicle().getPersistentDataContainer().get(MOUNT_SPEED, PersistentDataType.DOUBLE) == null) {
				return;
			}

			double health = e.getVehicle().getPersistentDataContainer().get(MOUNT_HEALTH, PersistentDataType.DOUBLE);
			double jump = e.getVehicle().getPersistentDataContainer().get(MOUNT_JUMP, PersistentDataType.DOUBLE);
			double speed = e.getVehicle().getPersistentDataContainer().get(MOUNT_SPEED, PersistentDataType.DOUBLE);
		}
	}

}
