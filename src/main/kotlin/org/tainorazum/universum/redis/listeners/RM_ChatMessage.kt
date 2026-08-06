package org.tainorazum.universum.redis.listeners

import com.google.gson.Gson
import org.tainorazum.universum.Universum
import org.tainorazum.universum.chat.ChatMessage
import org.tainorazum.universum.chat.manager.ChatManager
import org.tainorazum.universum.network.NetworkPlayerEvent
import org.tainorazum.universum.network.manager.NetworkPlayerManager
import org.tainorazum.universum.redis.RedisSubscriber

class RM_ChatMessage : RM_Channel("chat_message") {

    private val gson = Gson()

    init {
        RedisSubscriber.register(channel) { message ->
            try {
                val chatMessage = gson.fromJson(message, ChatMessage::class.java)
                Universum.chatManager.sendMessage(chatMessage)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}