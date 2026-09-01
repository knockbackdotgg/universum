package org.tainorazum.universum.player

import org.bukkit.entity.Player
import org.tainorazum.universum.Universum
import org.tainorazum.universum.server.server.DB_Server
import org.tainorazum.universum.utility.permissions.LuckpermsUtil
import java.util.UUID
import java.util.concurrent.CompletableFuture

class PlayerManager {

    val PERM_LOBBY_BYPASS = "universum.bypass.lobby"
    val PERM_CHAT_FILTER_BYPASS = "universum.bypass.chatfilter"

    fun allowBypassChatFilter(player: Player, allow: Boolean): CompletableFuture<Boolean> {
        return if(allow) {
            LuckpermsUtil.givePermission(player.uniqueId, PERM_CHAT_FILTER_BYPASS)
        } else {
            LuckpermsUtil.removePermission(player.uniqueId, PERM_CHAT_FILTER_BYPASS)
        }
    }

    fun allowBypassChatFilter(uuid: UUID, allow: Boolean): CompletableFuture<Boolean> {
        return LuckpermsUtil.givePermission(uuid, PERM_CHAT_FILTER_BYPASS, value = allow)
    }


    fun allowBypassChatFilter(player: Player): Boolean {
        return player.hasPermission(PERM_CHAT_FILTER_BYPASS)
    }

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
        val cachedPublic = DB_Server.currentPublic()
        if (cachedPublic == true) {
            return CompletableFuture.completedFuture("ALLOWED")
        }

        val whitelistedFuture = if (cachedPublic == false) {
            CompletableFuture.completedFuture(true)
        } else {
            Universum.whitelistManager.whitelisted()
        }

        return whitelistedFuture.thenCompose { whitelisted ->
            if (!whitelisted) {
                CompletableFuture.completedFuture("ALLOWED")
            } else {
                LuckpermsUtil.hasPermission(uuid, Universum.whitelistManager.perm)
                    .thenApply { hasPermission -> if (hasPermission) "ALLOWED" else "WHITELISTED" }
            }
        }.exceptionally {
            "ALLOWED"
        }
    }

}
