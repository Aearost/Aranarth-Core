package com.aearost.aranarthcore.event.listener.misc;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.objects.Dominion;
import com.aearost.aranarthcore.objects.DominionPermission;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.DominionUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;


public class ArmorStandInteractListener implements Listener {

    // Pose cycle:
    //  0 = No Arms (default)
    //  1 = No Pose  (arms visible)
    //  2 = Solemn
    //  3 = Athena
    //  4 = Athena Mirrored
    //  5 = Brandish
    //  6 = Brandish Mirrored
    //  7 = Honor
    //  8 = Entertainment
    //  9 = Salute
    // 10 = Heroic
    // 11 = Riposte
    // 12 = Riposte Mirrored
    // 13 = Zombie
    // 14 = Cancan
    // 15 = Cancan Mirrored
    // 16 = En Garde
    // 17 = En Garde Mirrored
    // 18 = Attention
    private static final String POSE_PERMISSION_PREFIX = "aranarthcore.armorstand.";

    private static final int TOTAL_POSES = 19;
    private static final String[] POSE_NAMES = {
        "Arms Hidden",
        "No Pose",
        "Solemn",
        "Athena",
        "Athena Mirrored",
        "Brandish",
        "Brandish Mirrored",
        "Honor",
        "Entertainment",
        "Salute",
        "Heroic",
        "Riposte",
        "Riposte Mirrored",
        "Zombie",
        "Cancan",
        "Cancan Mirrored",
        "En Garde",
        "En Garde Mirrored",
        "Attention"
    };

    private final NamespacedKey poseKey;
    private final NamespacedKey lockedKey;

    public ArmorStandInteractListener(AranarthCore plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
        this.poseKey = new NamespacedKey(plugin, "armor_stand_pose");
        this.lockedKey = new NamespacedKey(plugin, "armor_stand_locked");
    }

    /**
     * Re-applies the locked state (canMove=false, gravity=false) for any armor stand
     * that has the locked PDC key when its chunk is loaded after a server restart.
     */
    @EventHandler
    public void onEntitiesLoad(final EntitiesLoadEvent e) {
        for (Entity entity : e.getEntities()) {
            if (!(entity instanceof ArmorStand armorStand)) {
                continue;
            }
            if (armorStand.getPersistentDataContainer().has(lockedKey, PersistentDataType.BYTE)) {
                armorStand.setCanMove(false);
                armorStand.setGravity(false);
            }
        }
    }

    /**
     * Cycles the armor stand through all armor stand poses on each right-click.
     * Arms will only be hidden (pose 0) if the armor stand's hands are also empty.
     */
    @EventHandler
    public void onArmorStandClick(final PlayerInteractAtEntityEvent e) {
        if (!(e.getRightClicked() instanceof ArmorStand armorStand)) {
            return;
        }

        Player player = e.getPlayer();
        if (player.isSneaking()) {
            return;
        }

        // Lock the armor stand if the player is holding an iron block
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand.getType() == Material.IRON_BLOCK) {
            if (!player.hasPermission("aranarth.armorstand.lock")) {
                player.sendMessage(ChatUtils.chatMessage("&cYou do not have permission to lock armor stands"));
                e.setCancelled(true);
                return;
            }
            Dominion dominion = DominionUtils.getDominionOfChunk(armorStand.getLocation().getChunk());
            if (dominion != null && !DominionUtils.hasPermission(player, dominion, DominionPermission.ARMOR_STAND)) {
                player.sendMessage(ChatUtils.chatMessage("&cYou do not have permission to do this in &e" + dominion.getName()));
                e.setCancelled(true);
                return;
            }
            PersistentDataContainer pdc = armorStand.getPersistentDataContainer();
            if (pdc.has(lockedKey, PersistentDataType.BYTE)) {
                player.sendMessage(ChatUtils.chatMessage("&cThis armor stand has already been locked"));
                e.setCancelled(true);
                return;
            }
            if (hand.getAmount() == 1) {
                player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
            } else {
                hand.setAmount(hand.getAmount() - 1);
            }
            pdc.set(lockedKey, PersistentDataType.BYTE, (byte) 1);
            armorStand.setCanMove(false);
            armorStand.setGravity(false);
            player.playSound(armorStand.getLocation(), Sound.BLOCK_ANVIL_PLACE, 1.0F, 1.0F);
            player.sendMessage(ChatUtils.chatMessage("&7This armor stand has been locked"));
            e.setCancelled(true);
            return;
        }

        // If the player is holding something, allow it to be equipped via vanilla logic
        if (hand.getType() != Material.AIR) {
            return;
        }
        // The clicked slot on the stand has an item, allow it to be taken via vanilla logic
        if (clickedSlotHasItem(armorStand, e.getClickedPosition())) {
            return;
        }

        e.setCancelled(true);

        PersistentDataContainer pdc = armorStand.getPersistentDataContainer();
        int currentPose = pdc.getOrDefault(poseKey, PersistentDataType.INTEGER, 0);

        // Build the list of poses this player is allowed to use
        // Pose 0 (Arms Hidden) is always accessible, all others require a permission.
        List<Integer> accessiblePoses = new ArrayList<>();
        for (int i = 0; i < TOTAL_POSES; i++) {
            if (i == 0 || player.hasPermission(posePermission(i))) {
                accessiblePoses.add(i);
            }
        }

        // Only Arms Hidden is available, no cycling needed
        if (accessiblePoses.size() <= 1) {
            return;
        }

        // Advance to the next accessible pose
        int currentIndex = accessiblePoses.indexOf(currentPose);
        if (currentIndex == -1) {
            currentIndex = 0; // Permission was lost, reset to start
        }
        int nextPose = accessiblePoses.get((currentIndex + 1) % accessiblePoses.size());

        // Arms can only be hidden if the stand's hands are both empty
        if (nextPose == 0) {
            boolean mainHandEmpty = armorStand.getEquipment().getItemInMainHand().getType() == Material.AIR;
            boolean offHandEmpty = armorStand.getEquipment().getItemInOffHand().getType() == Material.AIR;
            if (!mainHandEmpty || !offHandEmpty) {
                return;
            }
        }

        applyPose(armorStand, nextPose);
        pdc.set(poseKey, PersistentDataType.INTEGER, nextPose);
        player.playSound(player, Sound.ENTITY_WITHER_BREAK_BLOCK, 0.2F, 1.8F);
        player.sendActionBar(Component.text(POSE_NAMES[nextPose], NamedTextColor.YELLOW));
    }

    /**
     * Approximates which armor slot a player clicked based on the y-position of the click,
     * then returns whether that slot.
     * The thresholds mirror vanilla's slot-selection logic for a standard armor stand.
     */
    private boolean clickedSlotHasItem(ArmorStand stand, Vector clickPos) {
        EntityEquipment eq = stand.getEquipment();
        double y = clickPos.getY();

        // Armor slots, ordered top to bottom
        ItemStack slotItem;
        if (y >= 1.4) {
            slotItem = eq.getHelmet();
        } else if (y >= 0.9) {
            slotItem = eq.getChestplate();
        } else if (y >= 0.45) {
            slotItem = eq.getLeggings();
        } else {
            slotItem = eq.getBoots();
        }

        if (slotItem != null && slotItem.getType() != Material.AIR) {
            return true;
        }

        // If the stand has visible arms and the click is in the arm-height range,
        // also check hand slots so held items are protected too
        if (stand.hasArms() && y >= 0.7 && y <= 1.4) {
            if (eq.getItemInMainHand().getType() != Material.AIR) {
                return true;
            }
            if (eq.getItemInOffHand().getType() != Material.AIR) {
                return true;
            }
        }

        return false;
    }

    /**
     * Returns the permission for a given pose index, derived from its name.
     */
    private static String posePermission(int poseIndex) {
        // Always is accessible
        if (poseIndex == 0) {
            return null;
        }
        String node = POSE_NAMES[poseIndex].toLowerCase().replaceAll("[^a-z0-9]", "");
        return POSE_PERMISSION_PREFIX + node;
    }

    /**
     * Applies the pose to the armor stand.
     * @param stand The armor stand.
     * @param pose The pose to be applied.
     */
    private void applyPose(ArmorStand stand, int pose) {
        if (pose == 0) {
            stand.setArms(false);
            stand.setHeadPose(EulerAngle.ZERO);
            stand.setBodyPose(EulerAngle.ZERO);
            stand.setRightLegPose(EulerAngle.ZERO);
            stand.setLeftLegPose(EulerAngle.ZERO);
            return;
        }

        // All posed states have arms visible - reset limbs to neutral before applying overrides
        stand.setArms(true);
        stand.setHeadPose(EulerAngle.ZERO);
        stand.setBodyPose(EulerAngle.ZERO);
        stand.setRightArmPose(rad(-10, 0, -10));
        stand.setLeftArmPose(rad(-10, 0, 10));
        stand.setRightLegPose(EulerAngle.ZERO);
        stand.setLeftLegPose(EulerAngle.ZERO);

        switch (pose) {
            case 1 -> {
                // No Pose — neutral arm defaults already applied above
            }
            case 2 -> {
                // Solemn
                stand.setHeadPose(rad(10, 0, 0));
                stand.setRightArmPose(rad(10, 0, -5));
                stand.setLeftArmPose(rad(10, 0, 5));
            }
            case 3 -> {
                // Athena
                stand.setHeadPose(rad(-5, 0, 0));
                stand.setRightArmPose(rad(-105, 0, 0));
                stand.setLeftArmPose(rad(10, 0, -20));
                stand.setLeftLegPose(rad(5, 0, 0));
            }
            case 4 -> {
                // Athena Mirrored
                stand.setHeadPose(rad(-5, 0, 0));
                stand.setRightArmPose(rad(10, 0, 20));
                stand.setLeftArmPose(rad(-105, 0, 0));
                stand.setRightLegPose(rad(5, 0, 0));
            }
            case 5 -> {
                // Brandish
                stand.setHeadPose(rad(-15, 0, 0));
                stand.setRightArmPose(rad(-157.5, 0, 0));
                stand.setLeftArmPose(rad(-15, 0, -10));
                stand.setLeftLegPose(rad(5, 0, 0));
            }
            case 6 -> {
                // Brandish Mirrored
                stand.setHeadPose(rad(-15, 0, 0));
                stand.setRightArmPose(rad(-15, 0, 10));
                stand.setLeftArmPose(rad(-157.5, 0, 0));
                stand.setRightLegPose(rad(5, 0, 0));
            }
            case 7 -> {
                // Honor
                stand.setHeadPose(rad(-20, 0, 0));
                stand.setRightArmPose(rad(-15, -70, 0));
                stand.setLeftArmPose(rad(-15, 70, 0));
            }
            case 8 -> {
                // Entertainment
                stand.setHeadPose(rad(-15, 0, 0));
                stand.setRightArmPose(rad(-15, 0, -85));
                stand.setLeftArmPose(rad(-15, 0, 85));
            }
            case 9 -> {
                // Salute
                stand.setHeadPose(rad(-5, 0, 0));
                stand.setRightArmPose(rad(-45, 0, -90));
                stand.setLeftArmPose(rad(-10, 0, 10));
            }
            case 10 -> {
                // Heroic
                stand.setHeadPose(rad(-20, 0, 0));
                stand.setBodyPose(rad(-5, 0, 0));
                stand.setRightArmPose(rad(-50, 0, -20));
                stand.setLeftArmPose(rad(-50, 0, 20));
                stand.setRightLegPose(rad(-10, 0, 0));
                stand.setLeftLegPose(rad(10, 0, 0));
            }
            case 11 -> {
                // Riposte
                stand.setHeadPose(rad(0, -15, 0));
                stand.setBodyPose(rad(0, -10, 0));
                stand.setRightArmPose(rad(-85, 0, 0));
                stand.setLeftArmPose(rad(-60, 0, 10));
                stand.setRightLegPose(rad(-15, 0, -5));
                stand.setLeftLegPose(rad(5, 15, 0));
            }
            case 12 -> {
                // Riposte Mirrored
                stand.setHeadPose(rad(0, 15, 0));
                stand.setBodyPose(rad(0, 10, 0));
                stand.setRightArmPose(rad(-60, 0, -10));
                stand.setLeftArmPose(rad(-85, 0, 0));
                stand.setRightLegPose(rad(5, -15, 0));
                stand.setLeftLegPose(rad(-15, 0, 5));
            }
            case 13 -> {
                // Zombie
                stand.setHeadPose(rad(-10, 0, 0));
                stand.setRightArmPose(rad(-90, 0, 0));
                stand.setLeftArmPose(rad(-90, 0, 0));
            }
            case 14 -> {
                // Cancan (right leg raised)
                stand.setHeadPose(rad(-10, 0, 0));
                stand.setRightArmPose(rad(-10, 0, -130));
                stand.setLeftArmPose(rad(-10, 0, 130));
                stand.setRightLegPose(rad(-135, 0, 0));
                stand.setLeftLegPose(rad(30, 0, 0));
            }
            case 15 -> {
                // Cancan Mirrored (left leg raised)
                stand.setHeadPose(rad(-10, 0, 0));
                stand.setRightArmPose(rad(-10, 0, -130));
                stand.setLeftArmPose(rad(-10, 0, 130));
                stand.setRightLegPose(rad(30, 0, 0));
                stand.setLeftLegPose(rad(-135, 0, 0));
            }
            case 16 -> {
                // En Garde
                stand.setHeadPose(rad(0, -15, 0));
                stand.setBodyPose(rad(0, -15, 0));
                stand.setRightArmPose(rad(-60, 0, 0));
                stand.setLeftArmPose(rad(-15, 0, 15));
                stand.setRightLegPose(rad(-15, 0, -5));
                stand.setLeftLegPose(rad(5, 5, 0));
            }
            case 17 -> {
                // En Garde Mirrored
                stand.setHeadPose(rad(0, 15, 0));
                stand.setBodyPose(rad(0, 15, 0));
                stand.setRightArmPose(rad(-15, 0, -15));
                stand.setLeftArmPose(rad(-60, 0, 0));
                stand.setRightLegPose(rad(5, -5, 0));
                stand.setLeftLegPose(rad(-15, 0, 5));
            }
            case 18 -> {
                // Attention
                stand.setHeadPose(rad(5, 0, 0));
                stand.setRightArmPose(rad(-5, 0, -5));
                stand.setLeftArmPose(rad(-5, 0, 5));
                stand.setRightLegPose(rad(0, 0, -3));
                stand.setLeftLegPose(rad(0, 0, 3));
            }
        }
    }

    private static EulerAngle rad(double x, double y, double z) {
        return new EulerAngle(Math.toRadians(x), Math.toRadians(y), Math.toRadians(z));
    }
}
