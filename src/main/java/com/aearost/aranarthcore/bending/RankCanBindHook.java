package com.aearost.aranarthcore.bending;

import com.aearost.aranarthcore.utils.AranarthUtils;
import com.projectkorra.projectkorra.BendingPlayer;
import com.projectkorra.projectkorra.ability.CoreAbility;
import com.projectkorra.projectkorra.hooks.CanBindHook;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Enforces per-ability rank requirements at bind time.
 */
public class RankCanBindHook implements CanBindHook {

    private static final Map<String, Integer> ABILITY_MIN_RANKS = new HashMap<>();

    static {
        // Knight
        ABILITY_MIN_RANKS.put("airsnipe", 2);
        ABILITY_MIN_RANKS.put("acrobatstance", 2);
        ABILITY_MIN_RANKS.put("warriorstance", 2);

        // Baron
        ABILITY_MIN_RANKS.put("waterarms", 3);
        ABILITY_MIN_RANKS.put("vinewhip", 3);
        ABILITY_MIN_RANKS.put("cableslash", 3);
        ABILITY_MIN_RANKS.put("sandstorm", 3);
        ABILITY_MIN_RANKS.put("burial", 3);
        ABILITY_MIN_RANKS.put("toxicspores", 3);
        ABILITY_MIN_RANKS.put("leafscythe", 3);
        ABILITY_MIN_RANKS.put("rootsnare", 3);
        ABILITY_MIN_RANKS.put("metalblade", 3);
        ABILITY_MIN_RANKS.put("cablethrash", 3);
        ABILITY_MIN_RANKS.put("metalfragments", 3);

        // Count
        ABILITY_MIN_RANKS.put("iceshards", 4);
        ABILITY_MIN_RANKS.put("angeredspirits", 4);
        ABILITY_MIN_RANKS.put("daggervolley", 4);
        ABILITY_MIN_RANKS.put("metalshots", 4);
        ABILITY_MIN_RANKS.put("metalstrips", 4);
        ABILITY_MIN_RANKS.put("metalshred", 4);
        ABILITY_MIN_RANKS.put("healinghelix", 4);

        // Duke
        ABILITY_MIN_RANKS.put("astralprojection", 5);
        ABILITY_MIN_RANKS.put("astralshot", 5);
        ABILITY_MIN_RANKS.put("suffocate", 5);

        // Prince
        ABILITY_MIN_RANKS.put("sonicpulse", 6);
        ABILITY_MIN_RANKS.put("energyburst", 6);
        ABILITY_MIN_RANKS.put("corruptinghelix", 6);
        ABILITY_MIN_RANKS.put("lavadisc", 6);
        ABILITY_MIN_RANKS.put("magmablast", 6);

        // King
        ABILITY_MIN_RANKS.put("sonicboom", 7);
        ABILITY_MIN_RANKS.put("deafeningscream", 7);
        ABILITY_MIN_RANKS.put("magmaglaives", 7);
        ABILITY_MIN_RANKS.put("moltenblast", 7);
        ABILITY_MIN_RANKS.put("magmawave", 7);
        ABILITY_MIN_RANKS.put("electricstrike", 7);
        ABILITY_MIN_RANKS.put("jetbolt", 7);
        ABILITY_MIN_RANKS.put("lightningburst", 7);
        ABILITY_MIN_RANKS.put("bolt", 7);
        ABILITY_MIN_RANKS.put("bloodfreeze", 7);
        ABILITY_MIN_RANKS.put("firecomet", 7);
        ABILITY_MIN_RANKS.put("lavaflux", 7);

        // Emperor
        ABILITY_MIN_RANKS.put("barrage", 8);
        ABILITY_MIN_RANKS.put("combustionstrike", 8);
        ABILITY_MIN_RANKS.put("eruption", 8);
        ABILITY_MIN_RANKS.put("disalignment", 8);
        ABILITY_MIN_RANKS.put("liferip", 8);
        ABILITY_MIN_RANKS.put("fissure", 8);
        ABILITY_MIN_RANKS.put("combustion", 8);
    }

    @Override
    public @NotNull Optional<Boolean> canBind(@NotNull BendingPlayer bPlayer, @NotNull CoreAbility ability) {
        Integer minRank = ABILITY_MIN_RANKS.get(ability.getName().toLowerCase());
        if (minRank == null) {
            return Optional.empty();
        }
        Player player = bPlayer.getPlayer();
        if (player == null) {
            return Optional.of(false);
        }
        int rank = AranarthUtils.getPlayer(player.getUniqueId()).getRank();
        return rank >= minRank ? Optional.empty() : Optional.of(false);
    }
}
