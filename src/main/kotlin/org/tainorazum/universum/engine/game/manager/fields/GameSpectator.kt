package org.tainorazum.universum.engine.game.manager.fields

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.UUID

data class GameSpectator(
    val uuid: UUID,
    val isInGame: Boolean,
) {

    fun player() = Bukkit.getPlayer(uuid)

}
