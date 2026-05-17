package com.aearost.aranarthcore.commands.general;

import com.aearost.aranarthcore.enums.Pronouns;
import com.aearost.aranarthcore.gui.GuiShopLocation;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.ShopIslandUtils;
import com.aearost.aranarthcore.utils.ShopUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

/**
 * Allows players to create, manage, and visit player shop islands.
 */
public class CommandShop implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatUtils.chatMessage("&cThis command can only be executed in-game!"));
            return true;
        }

        AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());

        if (args.length == 0) {
            GuiShopLocation gui = new GuiShopLocation(player, 0);
            gui.openGui();
            return true;
        }

        if (args[0].equalsIgnoreCase("create")) {
            if (aranarthPlayer.getRank() < 3 && !aranarthPlayer.isInAdminMode()) {
                String suffix = aranarthPlayer.getPronouns() == Pronouns.FEMALE ? "ess" : "";
                player.sendMessage(ChatUtils.chatMessage("&cYou must be a &5&lBaron" + suffix + " &cor higher to create a shop!"));
                return true;
            }
            if (AranarthUtils.getShopLocations().containsKey(player.getUniqueId())) {
                player.sendMessage(ChatUtils.chatMessage("&cYou already have a shop! Use &e/shop home"));
                return true;
            }

            World shopsWorld = Bukkit.getWorld(ShopIslandUtils.SHOPS_WORLD);
            if (shopsWorld == null) {
                player.sendMessage(ChatUtils.chatMessage("&cSomething went wrong with the shops world"));
                return true;
            }

            // Claim the next island index and compute its center
            int index = AranarthUtils.claimNextShopIslandIndex();
            int[] center = ShopIslandUtils.getIslandCenter(index);
            int centerX = center[0];
            int centerZ = center[1];

            // Generate the island
            ShopIslandUtils.generateShopIsland(shopsWorld, centerX, centerZ);
            Location homeLocation = new Location(shopsWorld, centerX + 0.5, ShopIslandUtils.ISLAND_TOP_Y + 1.0, centerZ + 0.5, 0, 0);
            AranarthUtils.createShopLocation(player.getUniqueId(), homeLocation);
            AranarthUtils.addShopIslandCenter(player.getUniqueId(), centerX, centerZ);

            // Teleport the player immediately
            AranarthUtils.teleportPlayer(player, player.getLocation(), homeLocation, true, success -> {
                if (success) {
                    player.sendMessage(ChatUtils.chatMessage("&7Your shop island has been created!"));
                    player.sendMessage(ChatUtils.chatMessage("&7Use &e/shop sethome &7to update the shop's home location!"));
                } else {
                    player.sendMessage(ChatUtils.chatMessage("&cYou could not teleport to your shop..."));
                }
            });
            return true;
        }

        if (args[0].equalsIgnoreCase("home")) {
            Location shopHome = AranarthUtils.getShopLocations().get(player.getUniqueId());
            if (shopHome == null) {
                player.sendMessage(ChatUtils.chatMessage("&cYou do not have a shop! Create one with &e/shop create&c."));
                return true;
            }
            AranarthUtils.teleportPlayer(player, player.getLocation(), shopHome, aranarthPlayer.isInAdminMode(), success -> {
                if (success) {
                    player.sendMessage(ChatUtils.chatMessage("&7You have teleported to your shop!"));
                } else {
                    player.sendMessage(ChatUtils.chatMessage("&cYou could not teleport to your shop!"));
                }
            });
            return true;
        }

        if (args[0].equalsIgnoreCase("sethome")) {
            if (!AranarthUtils.getShopLocations().containsKey(player.getUniqueId())) {
                player.sendMessage(ChatUtils.chatMessage("&cYou do not have a shop!"));
                return true;
            }
            // Ensure they are on their own island
            UUID ownerAtLocation = ShopIslandUtils.getIslandOwnerAtLocation(player.getLocation());
            if (!player.getWorld().getName().equals(ShopIslandUtils.SHOPS_WORLD) || !player.getUniqueId().equals(ownerAtLocation)) {
                player.sendMessage(ChatUtils.chatMessage("&cYou must be on your shop island to update your home!"));
                return true;
            }

            Location newHome = player.getLocation();
            newHome.setX(newHome.getBlockX() + 0.5);
            newHome.setZ(newHome.getBlockZ() + 0.5);
            AranarthUtils.createShopLocation(player.getUniqueId(), newHome);
            player.sendMessage(ChatUtils.chatMessage("&7Your shop home has been updated to your current location"));
            return true;
        }

        if (args[0].equalsIgnoreCase("delete")) {
            UUID targetUuid;
            String targetName;

            if (args.length >= 2) {
                // Only admins in admin mode may delete another player's shop
                if (!aranarthPlayer.isInAdminMode()) {
                    player.sendMessage(ChatUtils.chatMessage("&cYou do not have permission to delete another player's shop!"));
                    return true;
                }

                targetUuid = AranarthUtils.getUUIDFromUsername(args[1]);
                targetName = args[1];
                if (targetUuid == null) {
                    player.sendMessage(ChatUtils.chatMessage("&e" + args[1] + " &ccould not be found!"));
                    return true;
                }
            } else {
                // Player deletes their own shop
                targetUuid = player.getUniqueId();
                targetName = player.getName();
            }

            if (!AranarthUtils.getShopLocations().containsKey(targetUuid)) {
                player.sendMessage(ChatUtils.chatMessage("&e" + targetName + " &cdoes not have a shop!"));
                return true;
            }

            deleteShop(targetUuid, player);
            return true;
        }

        if (args[0].equalsIgnoreCase("tp")) {
            if (args.length < 2) {
                player.sendMessage(ChatUtils.chatMessage("&cInvalid syntax: &e/shop tp <username>"));
                return true;
            }
            HashMap<UUID, Location> shopLocations = AranarthUtils.getShopLocations();
            boolean wasShopFound = false;
            for (UUID uuid : shopLocations.keySet()) {
                String username = AranarthUtils.getUsername(Bukkit.getOfflinePlayer(uuid));
                if (args[1].equalsIgnoreCase(username)) {
                    AranarthPlayer shopOwner = AranarthUtils.getPlayer(uuid);
                    if (shopLocations.get(uuid) != null) {
                        wasShopFound = true;
                        AranarthUtils.teleportPlayer(player, player.getLocation(), shopLocations.get(uuid), aranarthPlayer.isInAdminMode(), success -> {
                            if (success) {
                                player.sendMessage(ChatUtils.chatMessage("&7You have teleported to &e" + shopOwner.getNickname() + "'s &7shop!"));
                            } else {
                                player.sendMessage(ChatUtils.chatMessage("&cYou could not teleport to &e" + shopOwner.getNickname() + "'s &cshop!"));
                            }
                        });
                    }
                    break;
                }
            }
            if (!wasShopFound) {
                player.sendMessage(ChatUtils.chatMessage("&cThis player does not have a shop!"));
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("biome")) {
            if (aranarthPlayer.getSaintRank() < 1 && !aranarthPlayer.isInAdminMode()) {
                player.sendMessage(ChatUtils.chatMessage("&cYou must be a &6&lSaint &cor higher to change your island's biome!"));
                return true;
            }
            if (!AranarthUtils.getShopIslandCenters().containsKey(player.getUniqueId())) {
                player.sendMessage(ChatUtils.chatMessage("&cYou do not have a shop!"));
                return true;
            }
            if (args.length < 2) {
                player.sendMessage(ChatUtils.chatMessage("&cInvalid syntax: &e/shop biome <biome>"));
                return true;
            }
            Biome biome = Registry.BIOME.get(NamespacedKey.minecraft(args[1].toLowerCase()));
            if (biome == null) {
                player.sendMessage(ChatUtils.chatMessage("&cThis biome could not be found"));
                return true;
            }
            World shopsWorld = Bukkit.getWorld(ShopIslandUtils.SHOPS_WORLD);
            if (shopsWorld == null) {
                player.sendMessage(ChatUtils.chatMessage("&cSomething went wrong with the shops world"));
                return true;
            }
            int[] center = AranarthUtils.getShopIslandCenters().get(player.getUniqueId());
            ShopIslandUtils.setIslandBiome(shopsWorld, center[0], center[1], biome);
            player.sendMessage(ChatUtils.chatMessage("&7Your island's biome has been set to &e" + args[1].toLowerCase()));
            player.sendMessage(ChatUtils.chatMessage("&7Try relogging if the biome doesn't update"));
            return true;
        }

        player.sendMessage(ChatUtils.chatMessage("&cInvalid syntax: &e/shop <create|home|sethome|delete|tp|biome>"));
        return true;
    }

    /**
     * Deletes the shop island, all associated shop signs, holograms, and location data for the given UUID.
     *
     * @param targetUuid The UUID of the shop owner whose island is being deleted.
     * @param player     The player who ran the delete command (used for feedback messages).
     */
    private void deleteShop(UUID targetUuid, Player player) {
        AranarthPlayer targetAranarthPlayer = AranarthUtils.getPlayer(targetUuid);
        String targetName = targetAranarthPlayer != null ? targetAranarthPlayer.getNickname() : targetUuid.toString();

        // Teleport the owner off their island if they are currently on it
        Player onlineTarget = Bukkit.getPlayer(targetUuid);
        if (onlineTarget != null && onlineTarget.getWorld().getName().equals(ShopIslandUtils.SHOPS_WORLD)) {
            Location safeSpot = new Location(Bukkit.getWorld("spawn"), 0.5, 100, 0.5, 180, 0);
            safeSpot = AranarthUtils.getSafeTeleportLocation(safeSpot);
            onlineTarget.teleport(safeSpot);
            onlineTarget.playSound(onlineTarget, Sound.ENTITY_ENDERMAN_TELEPORT, 1F, 0.9F);
            onlineTarget.sendMessage(ChatUtils.chatMessage("&cYour shop has been deleted. You have been teleported to Spawn."));
        }

        // Remove all trading shop holograms and data belonging to this player
        List<com.aearost.aranarthcore.objects.Shop> playerShops = ShopUtils.getShops().get(targetUuid);
        if (playerShops != null) {
            for (com.aearost.aranarthcore.objects.Shop shop : List.copyOf(playerShops)) {
                ShopUtils.removeShopHologram(shop);
            }
            ShopUtils.getShops().remove(targetUuid);
        }

        // Delete the island blocks
        int[] center = AranarthUtils.getShopIslandCenters().get(targetUuid);
        World shopsWorld = Bukkit.getWorld(ShopIslandUtils.SHOPS_WORLD);
        if (center != null && shopsWorld != null) {
            ShopIslandUtils.deleteShopIsland(shopsWorld, center[0], center[1]);
        }

        // Remove location data
        AranarthUtils.deleteShopLocation(targetUuid);
        AranarthUtils.removeShopIslandCenter(targetUuid);

        if (player.getUniqueId().equals(targetUuid)) {
            player.sendMessage(ChatUtils.chatMessage("&7Your shop has been deleted."));
        } else {
            player.sendMessage(ChatUtils.chatMessage("&e" + targetName + "'s &7shop has been deleted."));
        }
    }
}
