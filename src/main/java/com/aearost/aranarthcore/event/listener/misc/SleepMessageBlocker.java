package com.aearost.aranarthcore.event.listener.misc;

import com.aearost.aranarthcore.AranarthCore;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;

/**
 * Overrides vanilla sleep messages.
 */
public class SleepMessageBlocker implements Listener {

	public SleepMessageBlocker(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);

		ProtocolManager pm = ProtocolLibrary.getProtocolManager();
		pm.addPacketListener(new PacketAdapter(
				AranarthCore.getInstance(),
				ListenerPriority.HIGHEST,
				PacketType.Play.Server.SET_TITLE_TEXT,
				PacketType.Play.Server.SET_SUBTITLE_TEXT,
				PacketType.Play.Server.SET_ACTION_BAR_TEXT,
				PacketType.Play.Server.SET_TITLES_ANIMATION
		) {
			@Override
			public void onPacketSending(PacketEvent event) {
				String text = "";

				// Extract text safely
				try {
					text = event.getPacket()
							.getChatComponents().read(0)
							.getJson();
				} catch (Exception ignored) {}

				if (text == null) return;

				// Look for sleeping messages
				if (text.contains("sleep") || text.contains("Sleeping")) {
					Bukkit.getLogger().info("Canceling message");
					event.setCancelled(true);
				}
			}
		});
	}
	
}
