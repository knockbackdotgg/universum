package org.tainorazum.universum.redis.listeners.games

import org.bukkit.Bukkit
import org.tainorazum.universum.Universum
import org.tainorazum.universum.redis.RedisSubscriber
import org.tainorazum.universum.redis.listeners.RM_Channel

class RM_GameRequest : RM_Channel("game_request") {

    init {
        RedisSubscriber.register(channel) { requestId ->
            Universum.gameManager.claimRequest(requestId).thenAccept { request ->
                if (request != null) {
                    Bukkit.getScheduler().runTask(Universum.instance, Runnable {
                        Universum.gameManager.createGame(request)
                    })
                }
            }
        }
    }
}