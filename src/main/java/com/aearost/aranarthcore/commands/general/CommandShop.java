package com.aearost.aranarthcore.commands.general;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.enums.Pronouns;
import com.aearost.aranarthcore.gui.GuiShopLocation;
import com.aearost.aranarthcore.network.NetworkManager;
import com.aearost.aranarthcore.network.PendingTeleport;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.*;
import com.aearost.aranarthcore.utils.Lang;
import org.bukkit.*;
import org.bukkit.block.Biome;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Allows players to create, manage, and visit player shop islands.
 */
public class CommandShop implements CommandExecutor {

    /**
     * Deletes the shop island, all associated shop signs, holograms, and location data for the given UUID.
     *
     * @param targetUuid The UUID of the shop owner whose island is being deleted.
     * @param executor   The player who ran the delete command (used for feedback messages), or null for automated deletion.
     */
    public static void deleteShop(UUID targetUuid, Player executor) {
        AranarthPlayer targetAranarthPlayer = AranarthUtils.getPlayer(targetUuid);
        String targetName = targetAranarthPlayer != null ? targetAranarthPlayer.getNickname() : targetUuid.toString();

        Location safeSpot = new Location(Bukkit.getWorld("spawn"), 0.5, 100, 0.5, 180, 0);
        safeSpot = AranarthUtils.getSafeTeleportLocation(safeSpot);

        // Teleport all players on this island off it
        int[] center = AranarthUtils.getShopIslandCenters().get(targetUuid);
        boolean executorWasOnIsland = false;
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (!online.getWorld().getName().equals(ShopIslandUtils.SHOPS_WORLD)) {
                continue;
            }
            if (center == null || !ShopIslandUtils.isWithinPlotBoundary(online.getLocation(), center[0], center[1])) {
                continue;
            }
            if (executor != null && online.getUniqueId().equals(executor.getUniqueId())) {
                executorWasOnIsland = true;
            }
            online.teleport(safeSpot);
            int tpVol = AranarthUtils.getPlayer(online.getUniqueId()).getTeleportSoundVolume();
            if (tpVol > 0) {
                online.playSound(online, Sound.ENTITY_ENDERMAN_TELEPORT, tpVol / 100f, 0.9F);
            }
            if (online.getUniqueId().equals(targetUuid)) {
                online.sendMessage(ChatUtils.chatMessage(Lang.get("shop.deleted_teleport_spawn")));
            } else {
                online.sendMessage(ChatUtils.chatMessage(Lang.get("shop.other_deleted_teleport_spawn")));
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

        // Remove location data, collaborators, and custom name
        AranarthUtils.deleteShopLocation(targetUuid);
        PersistenceUtils.deleteShopLocationFromDatabase(targetUuid);
        AranarthUtils.removeShopIslandCenter(targetUuid);
        AranarthUtils.removeAllShopCollaborators(targetUuid);
        AranarthUtils.removeShopName(targetUuid);

        // Only notify the executor if they weren't already messaged by the loop above
        if (executor != null && !executorWasOnIsland) {
            if (executor.getUniqueId().equals(targetUuid)) {
                executor.sendMessage(ChatUtils.chatMessage(Lang.get("shop.self_deleted")));
            } else {
                executor.sendMessage(ChatUtils.chatMessage(Lang.get("shop.other_deleted", "player", targetName)));
            }
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatUtils.chatMessage(Lang.get("general.no_permission_console")));
            return true;
        }

        AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());

        if (args.length == 0) {
            GuiShopLocation.open(player, 0);
            return true;
        }

        if (args[0].equalsIgnoreCase("create")) {
            if (!aranarthPlayer.isInAdminMode()) {
                player.sendMessage(ChatUtils.chatMessage(Lang.get("shop.creation_disabled")));
                return true;
            }
            if (aranarthPlayer.getRank() < 3 && !aranarthPlayer.isInAdminMode()) {
                String suffix = aranarthPlayer.getPronouns() == Pronouns.FEMALE ? "ess" : "";
                player.sendMessage(ChatUtils.chatMessage(Lang.get("shop.rank_required", "suffix", suffix)));
                return true;
            }
            if (AranarthUtils.getShopLocations().containsKey(player.getUniqueId())) {
                player.sendMessage(ChatUtils.chatMessage(Lang.get("shop.already_have")));
                return true;
            }
            if (AranarthUtils.isCollaboratorOnAnyShop(player.getUniqueId())) {
                player.sendMessage(ChatUtils.chatMessage(Lang.get("shop.already_collaborator_other")));
                return true;
            }

            // Shops only exist on Survival - transfer the player there and re-run the command on arrival.
            if (AranarthCore.isSmpServer() && NetworkManager.isActive()) {
                String survivalServerName = AranarthCore.getInstance().getConfig()
                        .getString("network.servers.survival", "survival");
                AranarthUtils.teleportPlayer(player, player.getLocation(), player.getLocation(),
                        aranarthPlayer.isInAdminMode(), "&e&lYour Shop", "&7Creating your shop...", success -> {
                            if (success) {
                                PendingTeleport pt = PendingTeleport.forCommand(
                                        "shop create", "&e&lYour Shop", "&7Your shop island has been created");
                                NetworkManager.getInstance().saveInventoryAndTransfer(player, survivalServerName, pt);
                            }
                        });
                return true;
            }

            World shopsWorld = Bukkit.getWorld(ShopIslandUtils.SHOPS_WORLD);
            if (shopsWorld == null) {
                player.sendMessage(ChatUtils.chatMessage(Lang.get("shop.world_error")));
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
            AranarthUtils.teleportPlayer(player, player.getLocation(), homeLocation, true, "&e&lYour Shop", "&7Your shop island has been created", success -> {
                if (success) {
                    player.sendMessage(ChatUtils.chatMessage(Lang.get("shop.island_created")));
                    player.sendMessage(ChatUtils.chatMessage(Lang.get("shop.set_home_prompt")));
                } else {
                    player.sendMessage(ChatUtils.chatMessage(Lang.get("shop.could_not_teleport")));
                }
            });
            return true;
        }

        if (args[0].equalsIgnoreCase("home")) {
            Location shopHome = AranarthUtils.getShopLocations().get(player.getUniqueId());
            if (shopHome == null) {
                player.sendMessage(ChatUtils.chatMessage(Lang.get("shop.no_shop_create")));
                return true;
            }
            // Shops world only exists on Survival - transfer the player there if on SMP.
            if (AranarthCore.isSmpServer() && NetworkManager.isActive() && shopHome.getWorld() == null) {
                String survivalServerName = AranarthCore.getInstance().getConfig()
                        .getString("network.servers.survival", "survival");
                AranarthUtils.teleportPlayer(player, player.getLocation(), player.getLocation(),
                        aranarthPlayer.isInAdminMode(), "&e&lYour Shop", "&7Transferring to shop...", success -> {
                            if (success) {
                                PendingTeleport pt = new PendingTeleport(
                                        ShopIslandUtils.SHOPS_WORLD,
                                        shopHome.getX(), shopHome.getY(), shopHome.getZ(),
                                        shopHome.getYaw(), shopHome.getPitch(),
                                        "&e&lYour Shop", "&7You have teleported to your shop");
                                NetworkManager.getInstance().saveInventoryAndTransfer(player, survivalServerName, pt);
                            }
                        });
                return true;
            }
            AranarthUtils.teleportPlayer(player, player.getLocation(), shopHome, aranarthPlayer.isInAdminMode(), "&e&lYour Shop", "&7You have teleported to your shop", success -> {
                if (success) {
                    player.sendMessage(ChatUtils.chatMessage(Lang.get("shop.teleport_success")));
                } else {
                    player.sendMessage(ChatUtils.chatMessage(Lang.get("shop.could_not_teleport")));
                }
            });
            return true;
        }

        if (args[0].equalsIgnoreCase("sethome")) {
            if (!AranarthUtils.getShopLocations().containsKey(player.getUniqueId())) {
                player.sendMessage(ChatUtils.chatMessage(Lang.get("shop.no_shop")));
                return true;
            }
            // Ensure they are on their own island
            UUID ownerAtLocation = ShopIslandUtils.getIslandOwnerAtLocation(player.getLocation());
            if (!player.getWorld().getName().equals(ShopIslandUtils.SHOPS_WORLD) || !player.getUniqueId().equals(ownerAtLocation)) {
                player.sendMessage(ChatUtils.chatMessage(Lang.get("shop.must_be_on_island")));
                return true;
            }

            Location newHome = player.getLocation();
            newHome.setX(newHome.getBlockX() + 0.5);
            newHome.setZ(newHome.getBlockZ() + 0.5);
            AranarthUtils.createShopLocation(player.getUniqueId(), newHome);
            player.sendMessage(ChatUtils.chatMessage(Lang.get("shop.home_set")));
            return true;
        }

        if (args[0].equalsIgnoreCase("rename")) {
            if (!AranarthUtils.getShopLocations().containsKey(player.getUniqueId())) {
                player.sendMessage(ChatUtils.chatMessage(Lang.get("shop.no_shop")));
                return true;
            }
            if (args.length < 2) {
                player.sendMessage(ChatUtils.chatMessage(Lang.get("general.invalid_syntax", "usage", "shop rename <name>")));
                return true;
            }
            String newName = String.join(" ", Arrays.copyOfRange(args, 1, args.length)).trim();
            if (newName.length() > 32) {
                player.sendMessage(ChatUtils.chatMessage(Lang.get("shop.name_too_long")));
                return true;
            }
            if (ChatUtils.stripColorFormatting(newName).contains("|")) {
                player.sendMessage(ChatUtils.chatMessage(Lang.get("shop.name_invalid_char")));
                return true;
            }
            AranarthUtils.setShopName(player.getUniqueId(), newName);
            player.sendMessage(ChatUtils.chatMessage(Lang.get("shop.renamed", "name", newName)));
            return true;
        }

        if (args[0].equalsIgnoreCase("delete")) {
            UUID targetUuid;
            String targetName;

            if (args.length >= 2) {
                // Only admins in admin mode may delete another player's shop
                if (!aranarthPlayer.isInAdminMode()) {
                    player.sendMessage(ChatUtils.chatMessage(Lang.get("shop.cannot_destroy_no_permission")));
                    return true;
                }

                targetUuid = AranarthUtils.getUUIDFromUsername(args[1]);
                targetName = args[1];
                if (targetUuid == null) {
                    player.sendMessage(ChatUtils.chatMessage(Lang.get("player.not_found", "name", args[1])));
                    return true;
                }
            } else {
                // Player deletes their own shop
                targetUuid = player.getUniqueId();
                targetName = player.getName();
            }

            if (!AranarthUtils.getShopLocations().containsKey(targetUuid)) {
                player.sendMessage(ChatUtils.chatMessage(Lang.get("shop.other_no_shop", "player", targetName)));
                return true;
            }

            CommandShop.deleteShop(targetUuid, player);
            return true;
        }

        if (args[0].equalsIgnoreCase("invite")) {
            if (!AranarthUtils.getShopLocations().containsKey(player.getUniqueId())) {
                player.sendMessage(ChatUtils.chatMessage(Lang.get("shop.no_shop")));
                return true;
            }
            if (args.length < 2) {
                player.sendMessage(ChatUtils.chatMessage(Lang.get("general.invalid_syntax", "usage", "shop invite <username>")));
                return true;
            }
            if (args[1].equalsIgnoreCase(player.getName())) {
                player.sendMessage(ChatUtils.chatMessage(Lang.get("shop.invite_self")));
                return true;
            }
            UUID targetUuid = AranarthUtils.getUUIDFromUsername(args[1]);
            if (targetUuid == null) {
                player.sendMessage(ChatUtils.chatMessage(Lang.get("player.not_found", "name", args[1])));
                return true;
            }
            if (AranarthUtils.isShopCollaborator(player.getUniqueId(), targetUuid)) {
                player.sendMessage(ChatUtils.chatMessage(Lang.get("shop.already_collaborator_this", "player", args[1])));
                return true;
            }
            if (AranarthUtils.getShopLocations().containsKey(targetUuid)) {
                player.sendMessage(ChatUtils.chatMessage(Lang.get("shop.invite_target_has_shop", "player", args[1])));
                return true;
            }
            if (AranarthUtils.isCollaboratorOnAnyShop(targetUuid)) {
                player.sendMessage(ChatUtils.chatMessage(Lang.get("shop.invite_target_collaborator", "player", args[1])));
                return true;
            }

            // Send the invite - the target must accept it
            AranarthUtils.setPendingShopInvite(targetUuid, player.getUniqueId());
            AranarthPlayer ownerAranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
            player.sendMessage(ChatUtils.chatMessage(Lang.get("shop.invite_sent", "player", args[1])));
            Player target = Bukkit.getPlayer(targetUuid);
            if (target != null) {
                target.sendMessage(ChatUtils.chatMessage(Lang.get("shop.invite_received", "player", ownerAranarthPlayer.getNickname())));
                target.sendMessage(ChatUtils.chatMessage(Lang.get("shop.invite_instructions")));
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("accept")) {
            UUID ownerUuid = AranarthUtils.getPendingShopInvite(player.getUniqueId());
            if (ownerUuid == null) {
                player.sendMessage(ChatUtils.chatMessage(Lang.get("shop.no_pending_invite")));
                return true;
            }
            // Check they haven't gained a shop or become a collaborator since the invite was sent
            if (AranarthUtils.getShopLocations().containsKey(player.getUniqueId())) {
                AranarthUtils.removePendingShopInvite(player.getUniqueId());
                player.sendMessage(ChatUtils.chatMessage(Lang.get("shop.accept_own_shop")));
                return true;
            }
            if (AranarthUtils.isCollaboratorOnAnyShop(player.getUniqueId())) {
                AranarthUtils.removePendingShopInvite(player.getUniqueId());
                player.sendMessage(ChatUtils.chatMessage(Lang.get("shop.already_collaborator_other")));
                return true;
            }
            // Check the inviting shop still exists
            if (!AranarthUtils.getShopLocations().containsKey(ownerUuid)) {
                AranarthUtils.removePendingShopInvite(player.getUniqueId());
                player.sendMessage(ChatUtils.chatMessage(Lang.get("shop.invite_shop_gone")));
                return true;
            }

            AranarthUtils.removePendingShopInvite(player.getUniqueId());
            AranarthUtils.addShopCollaborator(ownerUuid, player.getUniqueId());

            AranarthPlayer ownerAranarthPlayer = AranarthUtils.getPlayer(ownerUuid);
            String ownerName = ownerAranarthPlayer != null ? ownerAranarthPlayer.getNickname() : ownerUuid.toString();
            player.sendMessage(ChatUtils.chatMessage(Lang.get("shop.accepted_collaborator", "player", ownerName)));
            Player owner = Bukkit.getPlayer(ownerUuid);
            if (owner != null) {
                AranarthPlayer playerAranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
                owner.sendMessage(ChatUtils.chatMessage(Lang.get("shop.collaborator_joined", "player", playerAranarthPlayer.getNickname())));
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("decline")) {
            UUID ownerUuid = AranarthUtils.getPendingShopInvite(player.getUniqueId());
            if (ownerUuid == null) {
                player.sendMessage(ChatUtils.chatMessage(Lang.get("shop.no_pending_invite")));
                return true;
            }
            AranarthUtils.removePendingShopInvite(player.getUniqueId());
            player.sendMessage(ChatUtils.chatMessage(Lang.get("shop.invite_declined")));
            Player owner = Bukkit.getPlayer(ownerUuid);
            if (owner != null) {
                AranarthPlayer playerAranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
                owner.sendMessage(ChatUtils.chatMessage(Lang.get("shop.invite_declined_notify", "player", playerAranarthPlayer.getNickname())));
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("leave")) {
            UUID ownerUuid = AranarthUtils.getCollaboratorShopOwner(player.getUniqueId());
            if (ownerUuid == null) {
                player.sendMessage(ChatUtils.chatMessage(Lang.get("shop.not_collaborator")));
                return true;
            }

            // Teleport the collaborator off the island if they are currently on it
            if (player.getWorld().getName().equals(ShopIslandUtils.SHOPS_WORLD)) {
                Location safeSpot = new Location(Bukkit.getWorld("spawn"), 0.5, 100, 0.5, 180, 0);
                safeSpot = AranarthUtils.getSafeTeleportLocation(safeSpot);
                player.teleport(safeSpot);
                int tpVol = AranarthUtils.getPlayer(player.getUniqueId()).getTeleportSoundVolume();
                if (tpVol > 0) {
                    player.playSound(player, Sound.ENTITY_ENDERMAN_TELEPORT, tpVol / 100f, 0.9F);
                }
            }

            AranarthUtils.removeShopCollaborator(ownerUuid, player.getUniqueId());
            String ownerNickname = AranarthUtils.getPlayer(ownerUuid).getNickname();
            player.sendMessage(ChatUtils.chatMessage(Lang.get("shop.left_shop", "player", ownerNickname)));
            Player owner = Bukkit.getPlayer(ownerUuid);
            if (owner != null) {
                AranarthPlayer playerAranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
                owner.sendMessage(ChatUtils.chatMessage(Lang.get("shop.collaborator_left", "player", playerAranarthPlayer.getNickname())));
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("remove")) {
            if (!AranarthUtils.getShopLocations().containsKey(player.getUniqueId())) {
                player.sendMessage(ChatUtils.chatMessage(Lang.get("shop.no_shop")));
                return true;
            }
            if (args.length < 2) {
                player.sendMessage(ChatUtils.chatMessage(Lang.get("general.invalid_syntax", "usage", "shop remove <username>")));
                return true;
            }
            UUID targetUuid = AranarthUtils.getUUIDFromUsername(args[1]);
            if (targetUuid == null) {
                player.sendMessage(ChatUtils.chatMessage(Lang.get("player.not_found", "name", args[1])));
                return true;
            }
            boolean removed = AranarthUtils.removeShopCollaborator(player.getUniqueId(), targetUuid);
            if (!removed) {
                player.sendMessage(ChatUtils.chatMessage(Lang.get("shop.not_collaborator_on_shop", "player", args[1])));
                return true;
            }

            // Teleport the removed player off the island if they are currently on it
            AranarthPlayer ownerAranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
            Player target = Bukkit.getPlayer(targetUuid);
            if (target != null && target.getWorld().getName().equals(ShopIslandUtils.SHOPS_WORLD)) {
                Location safeSpot = new Location(Bukkit.getWorld("spawn"), 0.5, 100, 0.5, 180, 0);
                safeSpot = AranarthUtils.getSafeTeleportLocation(safeSpot);
                target.teleport(safeSpot);
                int tpVol = AranarthUtils.getPlayer(target.getUniqueId()).getTeleportSoundVolume();
                if (tpVol > 0) {
                    target.playSound(target, Sound.ENTITY_ENDERMAN_TELEPORT, tpVol / 100f, 0.9F);
                }
                target.sendMessage(ChatUtils.chatMessage(Lang.get("shop.removed_from_shop", "player", ownerAranarthPlayer.getNickname())));
            } else if (target != null) {
                target.sendMessage(ChatUtils.chatMessage(Lang.get("shop.removed_from_shop", "player", ownerAranarthPlayer.getNickname())));
            }

            player.sendMessage(ChatUtils.chatMessage(Lang.get("shop.collaborator_removed", "player", args[1])));
            return true;
        }

        if (args[0].equalsIgnoreCase("biome")) {
            if (aranarthPlayer.getSaintRank() < 1 && aranarthPlayer.getCouncilRank() < 1 && !aranarthPlayer.isInAdminMode()) {
                player.sendMessage(ChatUtils.chatMessage(Lang.get("shop.biome_rank_required")));
                return true;
            }
            if (!AranarthUtils.getShopIslandCenters().containsKey(player.getUniqueId())) {
                player.sendMessage(ChatUtils.chatMessage(Lang.get("shop.no_shop")));
                return true;
            }
            if (args.length < 2) {
                player.sendMessage(ChatUtils.chatMessage(Lang.get("general.invalid_syntax", "usage", "shop biome <biome>")));
                return true;
            }
            Biome biome = Registry.BIOME.get(NamespacedKey.minecraft(args[1].toLowerCase()));
            if (biome == null) {
                player.sendMessage(ChatUtils.chatMessage(Lang.get("shop.biome_not_found")));
                return true;
            }
            World shopsWorld = Bukkit.getWorld(ShopIslandUtils.SHOPS_WORLD);
            if (shopsWorld == null) {
                player.sendMessage(ChatUtils.chatMessage(Lang.get("shop.world_error")));
                return true;
            }
            int[] center = AranarthUtils.getShopIslandCenters().get(player.getUniqueId());
            ShopIslandUtils.setIslandBiome(shopsWorld, center[0], center[1], biome);
            player.sendMessage(ChatUtils.chatMessage(Lang.get("shop.biome_set", "biome", args[1].toLowerCase())));
            player.sendMessage(ChatUtils.chatMessage(Lang.get("shop.biome_relog_hint")));
            return true;
        }

        player.sendMessage(ChatUtils.chatMessage(Lang.get("general.invalid_syntax", "usage", "shop <subcommand>")));
        return true;
    }
}
