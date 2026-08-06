package org.tainorazum.universum.network

import java.util.UUID

data class NetworkPlayer(
    val uuid: UUID,
    val name: String,
    val serverName: String
)

data class NetworkPlayerEvent(
    val player: NetworkPlayer,
    val type: Type
) {
    enum class Type { JOIN, QUIT, BANNED, MUTED, KICKED }
}