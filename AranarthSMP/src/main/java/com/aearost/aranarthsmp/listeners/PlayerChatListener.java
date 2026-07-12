package com.aearost.aranarthsmp.listeners;

import com.aearost.aranarthsmp.AranarthSMP;
import com.aearost.aranarthsmp.network.NetworkPlayer;
import com.aearost.aranarthsmp.network.SMPNetworkManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class PlayerChatListener implements Listener {

    public PlayerChatListener(AranarthSMP plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent e) {
        SMPNetworkManager net = SMPNetworkManager.getInstance();
        if (net == null) return;

        Player player  = e.getPlayer();
        String message = e.getMessage();

        e.setCancelled(true);

        // Build a simplified prefix using the player's rank data from the network roster
        NetworkPlayer np = net.getPlayer(player.getUniqueId());
        String prefix = buildPrefix(player, np);

        // Send to all SMP players
        String formatted = prefix + message;
        for (Player recipient : Bukkit.getOnlinePlayers()) {
            recipient.sendMessage(formatted);
        }
        Bukkit.getConsoleSender().sendMessage(formatted);

        // Relay to survival server via Redis
        net.publishChat(prefix, message);
    }

    private String buildPrefix(Player player, NetworkPlayer np) {
        if (np == null) {
            return "§8[§7Peasant§8] §7" + player.getName() + "§7: ";
        }
        String rankLabel = switch (np.getCouncilRank()) {
            case 3 -> "§4Admin";
            case 2 -> "§cMod";
            case 1 -> "§eHelper";
            default -> switch (np.getArchitectRank() > 0 ? 99 : np.getRank()) {
                case 99 -> "§6Arch";
                case 8  -> "§5Emperor";
                case 7  -> "§dKing";
                case 6  -> "§bPrince";
                case 5  -> "§3Duke";
                case 4  -> "§2Count";
                case 3  -> "§aBaron";
                case 2  -> "§fKnight";
                case 1  -> "§7Esquire";
                default -> "§8Peasant";
            };
        };
        String nickname = np.getNickname().isEmpty() ? player.getName() : np.getNickname();
        return "§8[" + rankLabel + "§8] §7" + nickname + "§7: ";
    }
}
