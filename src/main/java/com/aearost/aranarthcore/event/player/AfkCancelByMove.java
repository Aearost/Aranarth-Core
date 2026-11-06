package com.aearost.aranarthcore.event.player;

import org.bukkit.Input;
import org.bukkit.Location;
import org.bukkit.event.player.PlayerMoveEvent;

/**
 * Handles canceling a player's AFK status, or resetting their AFK timer, if they move or look around.
 */
public class AfkCancelByMove {

    public void execute(PlayerMoveEvent e) {
        Location from = e.getFrom();
        Location to = e.getTo();
        if (to == null) {
            return;
        }
        boolean hasTurned = from.getYaw() != to.getYaw() || from.getPitch() != to.getPitch();
        Input input = e.getPlayer().getCurrentInput();
        boolean isPressingMovement = input.isForward() || input.isBackward() || input.isLeft() || input.isRight()
                || input.isJump() || input.isSneak() || input.isSprint();
        if (!hasTurned && !isPressingMovement) {
            return;
        }
        new AfkCancelByInteract().execute(e.getPlayer());
    }
}
