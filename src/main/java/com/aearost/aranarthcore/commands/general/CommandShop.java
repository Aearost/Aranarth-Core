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
            sender.sendMessage(ChatUtils.chatMessage("&cThis command can only be executed in-game"));
            return true;
        }

        AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());

        if (args.length == 0) {
            GuiShopLocation.open(player, 0);
            return true;
        }

        if (args[0].equalsIgnoreCase("create")) {
            if (aranarthPlayer.getRank() < 3 && !aranarthPlayer.isInAdminMode()) {
                String suffix = aranarthPlayer.getPronouns() == Pronouns.FEMALE ? "ess" : "";
                player.sendMessage(ChatUtils.chatMessage("&cYou must be a &5&lBaron" + suffix + " &cor higher to create a shop"));
                return true;
            }
            if (AranarthUtils.getShopLocations().containsKey(player.getUniqueId())) {
                player.sendMessage(ChatUtils.chatMessage("&cYou already have a shop. Use &e/shop home"));
                return true;
            }
            if (AranarthUtils.isCollaboratorOnAnyShop(player.getUniqueId())) {
                player.sendMessage(ChatUtils.chatMessage("&cYou are already a collaborator on another player's shop. Use &e/shop leave &cfirst"));
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
                    player.sendMessage(ChatUtils.chatMessage("&7Your shop island has been created"));
                    player.sendMessage(ChatUtils.chatMessage("&7Use &e/shop sethome &7to update the shop's home location"));
                } else {
                    player.sendMessage(ChatUtils.chatMessage("&cYou could not teleport to your shop..."));
                }
            });
            return true;
        }

        if (args[0].equalsIgnoreCase("home")) {
            Location shopHome = AranarthUtils.getShopLocations().get(player.getUniqueId());
            if (shopHome == null) {
                player.sendMessage(ChatUtils.chatMessage("&cYou do not have a shop. Create one with &e/shop create"));
                return true;
            }
            AranarthUtils.teleportPlayer(player, player.getLocation(), shopHome, aranarthPlayer.isInAdminMode(), success -> {
                if (success) {
                    player.sendMessage(ChatUtils.chatMessage("&7You have teleported to your shop"));
                } else {
                    player.sendMessage(ChatUtils.chatMessage("&cYou could not teleport to your shop"));
                }
            });
            return true;
        }

        if (args[0].equalsIgnoreCase("sethome")) {
            if (!AranarthUtils.getShopLocations().containsKey(player.getUniqueId())) {
                player.sendMessage(ChatUtils.chatMessage("&cYou do not have a shop"));
                return true;
            }
            // Ensure they are on their own island
            UUID ownerAtLocation = ShopIslandUtils.getIslandOwnerAtLocation(player.getLocation());
            if (!player.getWorld().getName().equals(ShopIslandUtils.SHOPS_WORLD) || !player.getUniqueId().equals(ownerAtLocation)) {
                player.sendMessage(ChatUtils.chatMessage("&cYou must be on your shop island to update your home"));
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
                    player.sendMessage(ChatUtils.chatMessage("&cYou do not have permission to delete another player's shop"));
                    return true;
                }

                targetUuid = AranarthUtils.getUUIDFromUsername(args[1]);
                targetName = args[1];
                if (targetUuid == null) {
                    player.sendMessage(ChatUtils.chatMessage("&e" + args[1] + " &ccould not be found"));
                    return true;
                }
            } else {
                // Player deletes their own shop
                targetUuid = player.getUniqueId();
                targetName = player.getName();
            }

            if (!AranarthUtils.getShopLocations().containsKey(targetUuid)) {
                player.sendMessage(ChatUtils.chatMessage("&e" + targetName + " &cdoes not have a shop"));
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
                                player.sendMessage(ChatUtils.chatMessage("&7You have teleported to &e" + shopOwner.getNickname() + "'s &7shop"));
                            } else {
                                player.sendMessage(ChatUtils.chatMessage("&cYou could not teleport to &e" + shopOwner.getNickname() + "'s &cshop"));
                            }
                        });
                    }
                    break;
                }
            }
            if (!wasShopFound) {
                player.sendMessage(ChatUtils.chatMessage("&cThis player does not have a shop"));
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("invite")) {
            if (!AranarthUtils.getShopLocations().containsKey(player.getUniqueId())) {
                player.sendMessage(ChatUtils.chatMessage("&cYou do not have a shop"));
                return true;
            }
            if (args.length < 2) {
                player.sendMessage(ChatUtils.chatMessage("&cInvalid syntax: &e/shop invite <username>"));
                return true;
            }
            if (args[1].equalsIgnoreCase(player.getName())) {
                player.sendMessage(ChatUtils.chatMessage("&cYou cannot invite yourself"));
                return true;
            }
            UUID targetUuid = AranarthUtils.getUUIDFromUsername(args[1]);
            if (targetUuid == null) {
                player.sendMessage(ChatUtils.chatMessage("&e" + args[1] + " &ccould not be found"));
                return true;
            }
            if (AranarthUtils.isShopCollaborator(player.getUniqueId(), targetUuid)) {
                player.sendMessage(ChatUtils.chatMessage("&e" + args[1] + " &cis already a collaborator on your shop"));
                return true;
            }
            if (AranarthUtils.getShopLocations().containsKey(targetUuid)) {
                player.sendMessage(ChatUtils.chatMessage("&e" + args[1] + " &calready has their own shop"));
                return true;
            }
            if (AranarthUtils.isCollaboratorOnAnyShop(targetUuid)) {
                player.sendMessage(ChatUtils.chatMessage("&e" + args[1] + " &cis already a collaborator on another shop"));
                return true;
            }

            // Send the invite — the target must accept it
            AranarthUtils.setPendingShopInvite(targetUuid, player.getUniqueId());
            AranarthPlayer ownerAranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
            player.sendMessage(ChatUtils.chatMessage("&7A shop invitation has been sent to &e" + args[1]));
            Player target = Bukkit.getPlayer(targetUuid);
            if (target != null) {
                target.sendMessage(ChatUtils.chatMessage("&e" + ownerAranarthPlayer.getNickname() + " &7has invited you to collaborate on their shop"));
                target.sendMessage(ChatUtils.chatMessage("&7Use &e/shop accept &7to join or &e/shop decline &7to refuse"));
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("accept")) {
            UUID ownerUuid = AranarthUtils.getPendingShopInvite(player.getUniqueId());
            if (ownerUuid == null) {
                player.sendMessage(ChatUtils.chatMessage("&cYou do not have a pending shop invitation"));
                return true;
            }
            // Check they haven't gained a shop or become a collaborator since the invite was sent
            if (AranarthUtils.getShopLocations().containsKey(player.getUniqueId())) {
                AranarthUtils.removePendingShopInvite(player.getUniqueId());
                player.sendMessage(ChatUtils.chatMessage("&cYou now own a shop and cannot accept a collaborator invitation"));
                return true;
            }
            if (AranarthUtils.isCollaboratorOnAnyShop(player.getUniqueId())) {
                AranarthUtils.removePendingShopInvite(player.getUniqueId());
                player.sendMessage(ChatUtils.chatMessage("&cYou are already a collaborator on another shop"));
                return true;
            }
            // Check the inviting shop still exists
            if (!AranarthUtils.getShopLocations().containsKey(ownerUuid)) {
                AranarthUtils.removePendingShopInvite(player.getUniqueId());
                player.sendMessage(ChatUtils.chatMessage("&cThe shop you were invited to no longer exists"));
                return true;
            }

            AranarthUtils.removePendingShopInvite(player.getUniqueId());
            AranarthUtils.addShopCollaborator(ownerUuid, player.getUniqueId());

            AranarthPlayer ownerAranarthPlayer = AranarthUtils.getPlayer(ownerUuid);
            String ownerName = ownerAranarthPlayer != null ? ownerAranarthPlayer.getNickname() : ownerUuid.toString();
            player.sendMessage(ChatUtils.chatMessage("&7You are now a collaborator on &e" + ownerName + "'s &7shop"));
            Player owner = Bukkit.getPlayer(ownerUuid);
            if (owner != null) {
                AranarthPlayer playerAranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
                owner.sendMessage(ChatUtils.chatMessage("&e" + playerAranarthPlayer.getNickname() + " &7is now a collaborator to your shop"));
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("decline")) {
            UUID ownerUuid = AranarthUtils.getPendingShopInvite(player.getUniqueId());
            if (ownerUuid == null) {
                player.sendMessage(ChatUtils.chatMessage("&cYou do not have a pending shop invitation"));
                return true;
            }
            AranarthUtils.removePendingShopInvite(player.getUniqueId());
            player.sendMessage(ChatUtils.chatMessage("&7You have declined the shop invitation"));
            Player owner = Bukkit.getPlayer(ownerUuid);
            if (owner != null) {
                AranarthPlayer playerAranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
                owner.sendMessage(ChatUtils.chatMessage("&e" + playerAranarthPlayer.getNickname() + " &7has declined your shop invitation"));
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("leave")) {
            UUID ownerUuid = AranarthUtils.getCollaboratorShopOwner(player.getUniqueId());
            if (ownerUuid == null) {
                player.sendMessage(ChatUtils.chatMessage("&cYou are not a collaborator to any shop"));
                return true;
            }

            // Teleport the collaborator off the island if they are currently on it
            if (player.getWorld().getName().equals(ShopIslandUtils.SHOPS_WORLD)) {
                Location safeSpot = new Location(Bukkit.getWorld("spawn"), 0.5, 100, 0.5, 180, 0);
                safeSpot = AranarthUtils.getSafeTeleportLocation(safeSpot);
                player.teleport(safeSpot);
                player.playSound(player, Sound.ENTITY_ENDERMAN_TELEPORT, 1F, 0.9F);
            }

            AranarthUtils.removeShopCollaborator(ownerUuid, player.getUniqueId());
            String ownerNickname = AranarthUtils.getPlayer(ownerUuid).getNickname();
            player.sendMessage(ChatUtils.chatMessage("&7You are no longer a collaborator to &e" + ownerNickname + "&e's &7shop"));
            Player owner = Bukkit.getPlayer(ownerUuid);
            if (owner != null) {
                AranarthPlayer playerAranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
                owner.sendMessage(ChatUtils.chatMessage("&e" + playerAranarthPlayer.getNickname() + " &7is no longer a collaborator to your shop"));
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("remove")) {
            if (!AranarthUtils.getShopLocations().containsKey(player.getUniqueId())) {
                player.sendMessage(ChatUtils.chatMessage("&cYou do not have a shop"));
                return true;
            }
            if (args.length < 2) {
                player.sendMessage(ChatUtils.chatMessage("&cInvalid syntax: &e/shop remove <username>"));
                return true;
            }
            UUID targetUuid = AranarthUtils.getUUIDFromUsername(args[1]);
            if (targetUuid == null) {
                player.sendMessage(ChatUtils.chatMessage("&e" + args[1] + " &ccould not be found"));
                return true;
            }
            boolean removed = AranarthUtils.removeShopCollaborator(player.getUniqueId(), targetUuid);
            if (!removed) {
                player.sendMessage(ChatUtils.chatMessage("&e" + args[1] + " &cis not a collaborator on your shop"));
                return true;
            }

            // Teleport the removed player off the island if they are currently on it
            AranarthPlayer ownerAranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
            Player target = Bukkit.getPlayer(targetUuid);
            if (target != null && target.getWorld().getName().equals(ShopIslandUtils.SHOPS_WORLD)) {
                Location safeSpot = new Location(Bukkit.getWorld("spawn"), 0.5, 100, 0.5, 180, 0);
                safeSpot = AranarthUtils.getSafeTeleportLocation(safeSpot);
                target.teleport(safeSpot);
                target.playSound(target, Sound.ENTITY_ENDERMAN_TELEPORT, 1F, 0.9F);
                target.sendMessage(ChatUtils.chatMessage("&7You have been removed as a collaborator from &e" + ownerAranarthPlayer.getNickname() + "'s &7shop"));
            } else if (target != null) {
                target.sendMessage(ChatUtils.chatMessage("&7You have been removed as a collaborator from &e" + ownerAranarthPlayer.getNickname() + "'s &7shop"));
            }

            player.sendMessage(ChatUtils.chatMessage("&e" + args[1] + " &7has been removed as a collaborator from your shop"));
            return true;
        }

        if (args[0].equalsIgnoreCase("biome")) {
            if (aranarthPlayer.getSaintRank() < 1 && !aranarthPlayer.isInAdminMode()) {
                player.sendMessage(ChatUtils.chatMessage("&cYou must be a &6&lSaint &cor higher to change your island's biome"));
                return true;
            }
            if (!AranarthUtils.getShopIslandCenters().containsKey(player.getUniqueId())) {
                player.sendMessage(ChatUtils.chatMessage("&cYou do not have a shop"));
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

        player.sendMessage(ChatUtils.chatMessage("&cInvalid syntax: &e/shop <subcommand>"));
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

        Location safeSpot = new Location(Bukkit.getWorld("spawn"), 0.5, 100, 0.5, 180, 0);
        safeSpot = AranarthUtils.getSafeTeleportLocation(safeSpot);

        // Teleport all players on this island off it
        int[] center = AranarthUtils.getShopIslandCenters().get(targetUuid);
        boolean playerWasOnIsland = false;
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (!online.getWorld().getName().equals(ShopIslandUtils.SHOPS_WORLD)) {
                continue;
            }
            if (center == null || !ShopIslandUtils.isWithinPlotBoundary(online.getLocation(), center[0], center[1])) {
                continue;
            }
            if (online.getUniqueId().equals(player.getUniqueId())) {
                playerWasOnIsland = true;
            }
            online.teleport(safeSpot);
            online.playSound(online, Sound.ENTITY_ENDERMAN_TELEPORT, 1F, 0.9F);
            if (online.getUniqueId().equals(targetUuid)) {
                online.sendMessage(ChatUtils.chatMessage("&cYour shop has been deleted. You have been teleported to Spawn"));
            } else {
                online.sendMessage(ChatUtils.chatMessage("&cThis shop has been deleted. You have been teleported to Spawn"));
            }
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
        World shopsWorld = Bukkit.getWorld(ShopIslandUtils.SHOPS_WORLD);
        if (center != null && shopsWorld != null) {
            ShopIslandUtils.deleteShopIsland(shopsWorld, center[0], center[1]);
        }

        // Remove location data and collaborators
        AranarthUtils.deleteShopLocation(targetUuid);
        AranarthUtils.removeShopIslandCenter(targetUuid);
        AranarthUtils.removeAllShopCollaborators(targetUuid);

        // Only notify the player if they weren't already messaged by the loop above
        if (!playerWasOnIsland) {
            if (player.getUniqueId().equals(targetUuid)) {
                player.sendMessage(ChatUtils.chatMessage("&7Your shop has been deleted"));
            } else {
                player.sendMessage(ChatUtils.chatMessage("&e" + targetName + "'s &7shop has been deleted"));
            }
        }
    }
}
