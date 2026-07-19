package org.tainorazum.universum.server.allowed

import org.bukkit.entity.Player
import java.util.concurrent.CompletableFuture

class ServerAllowedManager {

    fun allowed(player: Player): CompletableFuture<Boolean> {
        val future = CompletableFuture<Boolean>()

        return future
    }

}