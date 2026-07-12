package com.aearost.aranarthsmp.network;

import java.util.UUID;

/**
 * Context for a cross-server teleport request received from the survival server.
 */
public record CrossServerTpContext(UUID remotePlayerUuid, String remotePlayerNickname,
                                   String remoteServer, boolean isTpHere) {}
