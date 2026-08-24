package org.tainorazum.universum.redis.listeners.games

import com.google.gson.Gson
import org.bukkit.Bukkit
import org.tainorazum.universum.Universum
import org.tainorazum.universum.redis.RedisSubscriber
import org.tainorazum.universum.redis.listeners.RM_Channel

class RM_GameRequest : RM_Channel("game_request") {

    private val gson = Gson()

    init {
        /*RedisSubscriber.register(channel) { message ->
            runCatching {
                val req = gson.fromJson(message, GameRequest::class.java) ?: return@register

                Bukkit.getScheduler().runTask(Universum.instance, Runnable {
                    Universum.gameManager.processGameRequest(req)
                })
            }
        }*/
    }
}