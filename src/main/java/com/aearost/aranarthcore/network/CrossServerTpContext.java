package com.aearost.aranarthcore.network;

import java.util.UUID;

/**
 * Holds the context for a cross-server teleport request received from another server.
 * Stored in NetworkManager keyed by the local player who received the request, so that
 * /tpaccept and /tpdeny know how to handle cross-server flows.
 *
 * @param remotePlayerUuid The UUID of the player on the other server who initiated the request.
 * @param remotePlayerNickname The nickname of the remote player (for messages).
 * @param remoteServer The server name the remote player is on.
 * @param isTpHere If true the remote player used /tphere (local player should travel to them).
 *                 If false the remote player used /tp (they should come here to local player).
 */
public record CrossServerTpContext(UUID remotePlayerUuid, String remotePlayerNickname,
                                   String remoteServer, boolean isTpHere) {}
