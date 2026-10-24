package com.aearost.aranarthcore.integration;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.event.extent.EditSessionEvent;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.extent.AbstractDelegateExtent;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.util.eventbus.Subscribe;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import org.bukkit.Bukkit;

/**
 * Prevents players from using WorldEdit on the survival worlds. Players get WorldEdit in the creative
 * worlds, and without this a selection or a world switch could reach survival from there.
 */
public class WorldEditWorldGuard {

    private static boolean registered = false;

    /**
     * Registers the guard. Safe to call more than once.
     */
    public static void enable() {
        if (registered) {
            return;
        }
        WorldEdit.getInstance().getEventBus().register(new WorldEditWorldGuard());
        registered = true;
        Bukkit.getLogger().info(AranarthCore.LOG_PREFIX + "WorldEdit survival world guard enabled");
    }

    /**
     * Blocks every change a non-staff player makes in a survival world.
     */
    @Subscribe
    public void onEditSession(EditSessionEvent event) {
        if (event.getStage() != EditSession.Stage.BEFORE_CHANGE || event.getWorld() == null
                || !AranarthUtils.isSurvivalWorld(event.getWorld().getName())) {
            return;
        }
        Actor actor = event.getActor();
        if (actor == null || !actor.isPlayer()) {
            return;
        }
        AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(actor.getUniqueId());
        if (aranarthPlayer != null && (aranarthPlayer.getCouncilRank() > 0 || aranarthPlayer.getArchitectRank() > 0)) {
            return;
        }

        event.setExtent(new AbstractDelegateExtent(event.getExtent()) {
            @Override
            public <T extends BlockStateHolder<T>> boolean setBlock(BlockVector3 location, T block) {
                return false;
            }

            @Override
            public boolean setBiome(BlockVector3 position, BiomeType biome) {
                return false;
            }

            @Override
            public Entity createEntity(Location location, BaseEntity entity) {
                return null;
            }
        });
    }
}
