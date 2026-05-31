package com.aearost.aranarthcore.event.mob;

import org.bukkit.Location;
import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityBreedEvent;

import java.util.Random;

/**
 * Handles extra baby spawn rates during the month of Calorvor.
 */
public class BabyMobSpawn {
	public void execute(EntityBreedEvent e) {
		if (e.getEntity() instanceof Ageable ageable) {
			if (ageable instanceof Animals animal) {
				if (!animal.isAdult()) {
					int rand = new Random().nextInt(2);
					// 50% chance of having a twin
					if (rand == 0) {
						Location loc = e.getEntity().getLocation();

						if (e.getEntity() instanceof Tameable babyTameable) {
							if (e.getMother() instanceof Tameable mother && e.getFather() instanceof Tameable father) {
								if (mother.isTamed() && father.isTamed()) {
									babyTameable.setTamed(true);
									Tameable twin = (Tameable) loc.getWorld().spawnEntity(loc, e.getEntityType());
									twin.setTamed(true);
									twin.setBaby();
									twin.setOwner(babyTameable.getOwner());
									if (twin instanceof Animals twinAnimal) {
										copyAppearance(animal, twinAnimal);
									}
									return;
								}
							}
						}

						Animals twin = (Animals) loc.getWorld().spawnEntity(loc, e.getEntityType());
						twin.setBaby();
						copyAppearance(animal, twin);
					}
				}
			}
		}
	}

	/**
	 * Copies color and variant from one animal to another so twins look identical.
	 */
	private void copyAppearance(Animals source, Animals target) {
		if (source instanceof Sheep sheepSource && target instanceof Sheep sheepTarget) {
			sheepTarget.setColor(sheepSource.getColor());
		} else if (source instanceof Chicken chickenSource && target instanceof Chicken chickenTarget) {
			chickenTarget.setVariant(chickenSource.getVariant());
		} else if (source instanceof Cat catSource && target instanceof Cat catTarget) {
			catTarget.setCatType(catSource.getCatType());
		} else if (source instanceof Rabbit rabbitSource && target instanceof Rabbit rabbitTarget) {
			rabbitTarget.setRabbitType(rabbitSource.getRabbitType());
		} else if (source instanceof Horse horseSource && target instanceof Horse horseTarget) {
			horseTarget.setColor(horseSource.getColor());
			horseTarget.setStyle(horseSource.getStyle());
		} else if (source instanceof Llama llamaSource && target instanceof Llama llamaTarget) {
			llamaTarget.setColor(llamaSource.getColor());
		} else if (source instanceof Fox foxSource && target instanceof Fox foxTarget) {
			foxTarget.setFoxType(foxSource.getFoxType());
		} else if (source instanceof Frog frogSource && target instanceof Frog frogTarget) {
			frogTarget.setVariant(frogSource.getVariant());
		} else if (source instanceof Parrot parrotSource && target instanceof Parrot parrotTarget) {
			parrotTarget.setVariant(parrotSource.getVariant());
		} else if (source instanceof Axolotl axolotlSource && target instanceof Axolotl axolotlTarget) {
			axolotlTarget.setVariant(axolotlSource.getVariant());
		} else if (source instanceof Wolf wolfSource && target instanceof Wolf wolfTarget) {
			wolfTarget.setVariant(wolfSource.getVariant());
		} else if (source instanceof Salmon salmonSource && target instanceof Salmon salmonTarget) {
			salmonTarget.setVariant(salmonSource.getVariant());
		} else if (source instanceof TropicalFish fishSource && target instanceof TropicalFish fishTarget) {
			fishTarget.setPattern(fishSource.getPattern());
			fishTarget.setPatternColor(fishSource.getPatternColor());
			fishTarget.setBodyColor(fishSource.getBodyColor());
		}
	}
}
