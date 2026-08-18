package org.tainorazum.universum.redis.listeners.network

import com.google.gson.Gson
import org.tainorazum.universum.Universum
import org.tainorazum.universum.chat.ChatMessage
import org.tainorazum.universum.redis.RedisSubscriber
import org.tainorazum.universum.redis.listeners.RM_Channel

class RM_ChatMessage : RM_Channel("chat_message") {

    private val gson = Gson()

    init {
        RedisSubscriber.register(channel) { message ->
            try {
                val chatMessage = gson.fromJson(message, ChatMessage::class.java)
                if(chatMessage.uuid == null) {
                    Universum.chatManager.sendMessage(chatMessage)
                }
                else if(chatMessage.server != Universum.server) Universum.chatManager.sendMessage(chatMessage)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}