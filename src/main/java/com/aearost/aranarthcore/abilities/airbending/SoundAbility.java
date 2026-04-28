package com.aearost.aranarthcore.abilities.airbending;

import com.projectkorra.projectkorra.Element;
import com.projectkorra.projectkorra.ability.AirAbility;
import com.projectkorra.projectkorra.ability.SubAbility;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public abstract class SoundAbility extends AirAbility implements SubAbility {

	public static Element.SubElement SOUND;

	public SoundAbility(final Player player) {
		super(player);
	}

	@Override
	public Class<? extends com.projectkorra.projectkorra.ability.Ability> getParentAbility() {
		return AirAbility.class;
	}

	@Override
	public Element getElement() {
		return SOUND;
	}

	/**
	 * Applies the standard Sound bending debuff: Slowness I for 3 seconds and
	 * Darkness for 2 seconds. Intended for use by all Sound bending abilities.
	 * @param entity The entity to apply the effects to.
	 */
	protected static void applySoundDebuff(LivingEntity entity) {
		entity.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 0, false, true));
		entity.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 40, 0, false, true));
	}

}
