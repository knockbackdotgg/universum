package org.tainorazum.universum.player

import org.bukkit.entity.Player
import org.tainorazum.universum.Universum
import org.tainorazum.universum.utility.permissions.LuckpermsUtil
import java.util.UUID
import java.util.concurrent.CompletableFuture

class PlayerManager {

    val PERM_LOBBY_BYPASS = "universum.bypass.lobby"

    fun allowBypassLobby(player: Player, allow: Boolean): CompletableFuture<Boolean> {
        return if(allow) {
            LuckpermsUtil.givePermission(player.uniqueId, PERM_LOBBY_BYPASS)
        } else {
            LuckpermsUtil.removePermission(player.uniqueId, PERM_LOBBY_BYPASS)
        }
    }

    fun allowBypassLobby(uuid: UUID, allow: Boolean): CompletableFuture<Boolean> {
        return LuckpermsUtil.givePermission(uuid, PERM_LOBBY_BYPASS, value = allow)
    }


    fun allowBypassLobby(player: Player): Boolean {
        return player.hasPermission(PERM_LOBBY_BYPASS)
    }

    fun allowJoin(uuid: UUID) : CompletableFuture<String> {
        val future = CompletableFuture<String>()

        val whitelistedFuture = Universum.whitelistManager.whitelisted()
        val isWhitelistedFuture = Universum.whitelistManager.isWhitelisted(uuid)

        //val isBannedFuture = Universum.punishmentManager.isBanned(uuid)

        CompletableFuture.allOf(whitelistedFuture, isWhitelistedFuture, /*isBannedFuture*/).thenAccept {
            val whitelisted = whitelistedFuture.join()
            val isWhitelisted = isWhitelistedFuture.join()
            //val isBanned = isBannedFuture.join()

            if(whitelisted) {
                if(isWhitelisted) {
                    future.complete("ALLOWED")
                } else {
                    future.complete("WHITELISTED")
                }
            }
            //else if(isBanned != null) {
            //    future.complete("BANNED")
            //}
            else future.complete("ALLOWED")

        }

        return future

    }

}