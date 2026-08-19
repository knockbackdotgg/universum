package org.tainorazum.universum.redis.listeners.network

import com.google.gson.Gson
import org.tainorazum.universum.network.NetworkPlayerEvent
import org.tainorazum.universum.network.manager.NetworkPlayerManager
import org.tainorazum.universum.redis.RedisSubscriber
import org.tainorazum.universum.redis.listeners.RM_Channel

class RM_Online : RM_Channel(NetworkPlayerManager.CHANNEL) {

    private val gson = Gson()

    init {
        RedisSubscriber.register(channel) { message ->
            try {
                val event = gson.fromJson(message, NetworkPlayerEvent::class.java)
                NetworkPlayerManager.handleRemoteEvent(event)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}