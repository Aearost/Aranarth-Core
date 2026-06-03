package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.commands.general.CommandMctop;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.gmail.nossr50.datatypes.skills.PrimarySkillType;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;

/**
 * Handles the logic for navigating the mcMMO top skill GUI.
 */
public class GuiMctopClick {

    public void execute(InventoryClickEvent e) {
        String title = ChatUtils.stripColorFormatting(e.getView().getTitle());
        if (!title.startsWith("Top ")) {
            return;
        }

        // Resolve the skill from the GUI title (null means overall power level)
        String skillPart = title.substring("Top ".length()).toUpperCase();
        PrimarySkillType skill;
        if (skillPart.equals("OVERALL")) {
            skill = null;
        } else {
            try {
                skill = PrimarySkillType.valueOf(skillPart);
            } catch (IllegalArgumentException ex) {
                return;
            }
        }

        if (e.getClickedInventory() == null) {
            return;
        }

        if (e.getWhoClicked() instanceof Player player) {
            AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());

            if (e.getClickedInventory().getType() == InventoryType.CHEST) {
                e.setCancelled(true);

                // Previous
                if (e.getSlot() == 45) {
                    int currentPage = aranarthPlayer.getCurrentGuiPageNum();
                    if (currentPage > 0) {
                        player.playSound(player, Sound.UI_BUTTON_CLICK, 0.25F, 1);
                        CommandMctop.openGui(player, skill, currentPage - 1);
                    }
                }
                // Exit
                else if (e.getSlot() == 49) {
                    player.closeInventory();
                    player.playSound(player, Sound.UI_BUTTON_CLICK, 0.25F, 1);
                }
                // Next
                else if (e.getSlot() == 53) {
                    int currentPage = aranarthPlayer.getCurrentGuiPageNum();
                    player.playSound(player, Sound.UI_BUTTON_CLICK, 0.25F, 1);
                    CommandMctop.openGui(player, skill, currentPage + 1);
                }
            }
        }
    }

}
