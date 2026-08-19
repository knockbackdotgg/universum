package org.tainorazum.universum.utility.player

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.tainorazum.universum.Universum
import java.util.UUID

object PlayerIsolationUtility {

    private val isolatedPlayers = mutableSetOf<UUID>() // players who should NOT see anyone else

    fun isIsolated(player: Player): Boolean = isolatedPlayers.contains(player.uniqueId)

    fun isolatePlayer(target: Player) {
        isolatedPlayers.add(target.uniqueId)

        for (other in Bukkit.getOnlinePlayers()) {
            if (other.uniqueId != target.uniqueId) {
                target.hidePlayer(Universum.instance, other)
            }
        }
    }

    fun unisolatePlayer(target: Player) {
        isolatedPlayers.remove(target.uniqueId)

        for (other in Bukkit.getOnlinePlayers()) {
            if (other.uniqueId != target.uniqueId) {
                target.showPlayer(Universum.instance, other)
            }
        }
    }

    fun handleNewPlayerJoin(newPlayer: Player) {
        for (isolatedUuid in isolatedPlayers) {
            val isolatedPlayer = Bukkit.getPlayer(isolatedUuid) ?: continue
            if (isolatedPlayer.isOnline && isolatedPlayer.uniqueId != newPlayer.uniqueId) {
                isolatedPlayer.hidePlayer(Universum.instance, newPlayer)
            }
        }
    }

    fun handlePlayerQuit(player: Player) {
        isolatedPlayers.remove(player.uniqueId)
    }

}