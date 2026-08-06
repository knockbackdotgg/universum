package org.tainorazum.universum.whitelist

import org.tainorazum.universum.Universum
import org.tainorazum.universum.server.server.DB_Server
import org.tainorazum.universum.utility.permissions.LuckpermsUtil
import java.time.Duration
import java.util.UUID
import java.util.concurrent.CompletableFuture

class WhitelistManager {

    val perm = "universum.whitelist"

    fun isWhitelisted(uuid: UUID): CompletableFuture<Boolean> {
        return LuckpermsUtil.hasPermission(uuid, perm)
    }

    fun setWhitelisted(bool: Boolean): CompletableFuture<Boolean> {
        return DB_Server.setPublic(Universum.server.identifier, bool)
    }

    fun whitelisted(): CompletableFuture<Boolean> {
        return Universum.server.public
    }

    fun whitelist(uuid: UUID, duration: Duration?): CompletableFuture<Boolean> {
        return LuckpermsUtil.givePermission(uuid, perm, duration)
    }

    fun unWhitelist(uuid: UUID): CompletableFuture<Boolean> {
        return LuckpermsUtil.removePermission(uuid, perm)
    }

    fun getWhitelisted() : CompletableFuture<List<UUID>> {
        return LuckpermsUtil.getPlayersWithPermission(perm)
    }

}