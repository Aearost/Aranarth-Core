package com.aearost.aranarthcore.objects;

import com.aearost.aranarthcore.enums.PrefixStyle;
import com.aearost.aranarthcore.enums.Pronouns;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.google.gson.JsonObject;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.OfflinePlayer;

/**
 * A snapshot of everything needed to render a player's chat prefix, so that each viewer can see it
 * in their own chosen prefix styles, including viewers on the other server.
 */
public class ChatPrefix {

    private final int councilRank;
    private final boolean isArchitect;
    private final int saintRank;
    private final boolean isAvatar;
    private final int rank;
    private final boolean isFemale;
    private final String nickname;
    private final String username;

    public ChatPrefix(int councilRank, boolean isArchitect, int saintRank, boolean isAvatar,
                      int rank, boolean isFemale, String nickname, String username) {
        this.councilRank = councilRank;
        this.isArchitect = isArchitect;
        this.saintRank = saintRank;
        this.isAvatar = isAvatar;
        this.rank = rank;
        this.isFemale = isFemale;
        this.nickname = nickname != null ? nickname : "";
        this.username = username != null ? username : "";
    }

    /**
     * Creates the prefix of the player from their current ranks.
     *
     * @param player The player whose prefix is being created.
     * @return The prefix of the player.
     */
    public static ChatPrefix of(OfflinePlayer player) {
        AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
        String name = player.getName() != null ? player.getName() : aranarthPlayer.getUsername();
        return new ChatPrefix(aranarthPlayer.getCouncilRank(), !AranarthUtils.getArchitectRank(aranarthPlayer).isEmpty(),
                aranarthPlayer.getSaintRank(), !AranarthUtils.getAvatarRank(aranarthPlayer).isEmpty(),
                aranarthPlayer.getRank(), aranarthPlayer.getPronouns() != Pronouns.MALE,
                aranarthPlayer.getNickname(), name);
    }

    /**
     * Provides the prefix elements in the given styles, without any trailing space.
     */
    private String getElements(PrefixStyle[] styles) {
        return AranarthUtils.buildPrefixElements(councilRank, styles[0], isArchitect,
                saintRank, styles[1], isAvatar, rank, isFemale, styles[2]);
    }

    /**
     * Formats the full chat prefix, including the ⊰⊱ brackets.
     *
     * @param styles The council, saint, and rank styles of the viewer.
     * @return The formatted chat prefix.
     */
    public String format(PrefixStyle[] styles) {
        String name = !nickname.isEmpty() ? nickname + "&r" : username;
        return ChatUtils.translateToColor("&l⊰&r" + getElements(styles) + " " + name + "&l⊱ &r");
    }

    /**
     * Formats the prefix elements followed by the name, without the ⊰⊱ brackets.
     *
     * @param styles The council, saint, and rank styles of the viewer.
     * @return The formatted prefix elements and name.
     */
    public String formatName(PrefixStyle[] styles) {
        String name = !nickname.isEmpty() ? nickname : username;
        return ChatUtils.translateToColor(getElements(styles) + " " + name);
    }

    /**
     * Builds the chat prefix as a component which suggests /info for the player when clicked.
     *
     * @param styles The council, saint, and rank styles of the viewer.
     * @return The clickable chat prefix.
     */
    public Component toComponent(PrefixStyle[] styles) {
        String hoverMsg = ChatUtils.translateToColor("&7Click to view &e" + nickname + "&e's &7info");
        // Deserialize with legacySection() since format() has already translated & to §
        Component prefixComponent = LegacyComponentSerializer.legacySection().deserialize(format(styles));
        return ChatUtils.clickableCommand(prefixComponent, hoverMsg, "/info " + username, true);
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("councilRank", councilRank);
        json.addProperty("architect", isArchitect);
        json.addProperty("saintRank", saintRank);
        json.addProperty("avatar", isAvatar);
        json.addProperty("rank", rank);
        json.addProperty("female", isFemale);
        json.addProperty("nickname", nickname);
        json.addProperty("username", username);
        return json;
    }

    public static ChatPrefix fromJson(JsonObject json) {
        return new ChatPrefix(json.get("councilRank").getAsInt(), json.get("architect").getAsBoolean(),
                json.get("saintRank").getAsInt(), json.get("avatar").getAsBoolean(),
                json.get("rank").getAsInt(), json.get("female").getAsBoolean(),
                json.get("nickname").getAsString(), json.get("username").getAsString());
    }
}
