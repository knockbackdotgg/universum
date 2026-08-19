package org.tainorazum.universum.redis.listeners.network

import com.google.gson.Gson
import org.tainorazum.universum.network.manager.NetworkPlayerManager
import org.tainorazum.universum.network.manager.ServerMessage
import org.tainorazum.universum.redis.RedisSubscriber
import org.tainorazum.universum.redis.listeners.RM_Channel

class RM_ServerMessage : RM_Channel("server_message") {

    private val gson = Gson()

    init {
        RedisSubscriber.register(channel) { message ->
            try {
                val serverMessage = gson.fromJson(message, ServerMessage::class.java)
                NetworkPlayerManager.serverMessage(serverMessage)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}