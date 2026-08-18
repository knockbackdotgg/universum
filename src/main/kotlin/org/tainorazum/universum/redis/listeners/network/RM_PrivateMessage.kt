package org.tainorazum.universum.redis.listeners.network

import com.google.gson.Gson
import org.tainorazum.universum.Universum
import org.tainorazum.universum.chat.ChatMessage
import org.tainorazum.universum.redis.RedisSubscriber
import org.tainorazum.universum.redis.listeners.RM_Channel

class RM_PrivateMessage : RM_Channel("private_message") {

    private val gson = Gson()

    init {
        RedisSubscriber.register(channel) { message ->
            try {

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}